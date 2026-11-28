package com.example.data.repository

import com.example.data.local.AchievementEntity
import com.example.data.local.HabitEntity
import com.example.data.local.LifeHubDatabase
import com.example.data.local.PendingSyncEntity
import com.example.data.local.PreferencesManager
import com.example.data.local.ReminderEntity
import com.example.data.local.SavingsGoalEntity
import com.example.data.local.SyncMetadataEntity
import com.example.data.local.TaskEntity
import com.example.data.local.TransactionEntity
import com.example.data.local.UserProfileEntity
import com.example.data.remote.FirestoreAchievement
import com.example.data.remote.FirestoreGoal
import com.example.data.remote.FirestoreHabit
import com.example.data.remote.FirestoreProfile
import com.example.data.remote.FirestoreReminder
import com.example.data.remote.FirestoreSettings
import com.example.data.remote.FirestoreTask
import com.example.data.remote.FirestoreTransaction
import com.example.data.remote.NetworkMonitor
import com.example.data.remote.OperationType
import com.example.data.remote.handleFirestoreError
import com.example.domain.model.ThemeMode
import com.example.domain.model.UserSettings
import com.example.domain.util.DateAndFormatUtils
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await
import org.json.JSONArray
import org.json.JSONObject
import java.util.Date

/**
 * Typed JSON serializer for offline Room outbox payloads.
 * Preserves Long vs Double vs Boolean vs String vs List<String> so Firestore security rules
 * type assertions (`is int`, `is number`, `is bool`, `is list`) always match 1:1 on retry.
 */
internal object SyncPayloadCodec {
    private const val TYPE_LONG = "L"
    private const val TYPE_DOUBLE = "D"
    private const val TYPE_BOOL = "B"
    private const val TYPE_STRING = "S"
    private const val TYPE_STRING_LIST = "SL"

    fun encode(fields: Map<String, Any?>): String {
        val root = JSONObject()
        fields.forEach { (key, value) ->
            if (value == null) return@forEach
            val entry = JSONObject()
            when (value) {
                is Long -> {
                    entry.put("t", TYPE_LONG)
                    entry.put("v", value)
                }
                is Int -> {
                    entry.put("t", TYPE_LONG)
                    entry.put("v", value.toLong())
                }
                is Double -> {
                    entry.put("t", TYPE_DOUBLE)
                    entry.put("v", value)
                }
                is Float -> {
                    entry.put("t", TYPE_DOUBLE)
                    entry.put("v", value.toDouble())
                }
                is Boolean -> {
                    entry.put("t", TYPE_BOOL)
                    entry.put("v", value)
                }
                is String -> {
                    entry.put("t", TYPE_STRING)
                    entry.put("v", value)
                }
                is List<*> -> {
                    val arr = JSONArray()
                    value.forEach { item -> if (item != null) arr.put(item.toString()) }
                    entry.put("t", TYPE_STRING_LIST)
                    entry.put("v", arr)
                }
            }
            if (entry.has("t")) root.put(key, entry)
        }
        return root.toString()
    }

    fun decode(json: String): MutableMap<String, Any> {
        val result = mutableMapOf<String, Any>()
        if (json.isBlank()) return result
        val root = JSONObject(json)
        val keys = root.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            val entry = root.optJSONObject(key) ?: continue
            when (entry.optString("t")) {
                TYPE_LONG -> result[key] = entry.getLong("v")
                TYPE_DOUBLE -> result[key] = entry.getDouble("v")
                TYPE_BOOL -> result[key] = entry.getBoolean("v")
                TYPE_STRING -> result[key] = entry.getString("v")
                TYPE_STRING_LIST -> {
                    val arr = entry.optJSONArray("v") ?: JSONArray()
                    val list = mutableListOf<String>()
                    for (i in 0 until arr.length()) {
                        list.add(arr.getString(i))
                    }
                    result[key] = list
                }
            }
        }
        return result
    }

    fun mergeUpdateIntoCreate(existingCreateJson: String, updateFields: Map<String, Any?>): String {
        val merged = decode(existingCreateJson)
        updateFields.forEach { (k, v) ->
            if (v != null) merged[k] = v
        }
        return encode(merged)
    }
}

/**
 * Spark-plan-optimized Firestore synchronization engine:
 * - Persists sync watermarks & cooldowns in Room (`sync_metadata`) across app restarts.
 * - Uses single-field indexed incremental delta queries (`updatedAt > cursor`) instead of full collection downloads.
 * - Batches offline & background writes via Room outbox (`pending_sync_ops`) + Firestore `WriteBatch`.
 * - Single-flight Mutex prevents duplicate concurrent network requests.
 * - Strictly user-scoped (`users/$uid/...`) and acyclic (no Room <-> Firestore infinite sync loops).
 */
internal class FirestoreSyncEngine(
    private val db: FirebaseFirestore,
    private val localDb: LifeHubDatabase,
    private val preferencesManager: PreferencesManager,
    private val networkMonitor: NetworkMonitor,
    private val auth: FirebaseAuth
) {
    private val syncMutex = Mutex()
    private val flushMutex = Mutex()
    private val lastCursorByMonth = mutableMapOf<String, DocumentSnapshot?>()

    private fun userDoc(uid: String) = db.collection("users").document(uid)

    // --- OFFLINE-FIRST OUTBOX ENQUEUE & BATCH FLUSH ---

    suspend fun enqueueOperation(
        userId: String,
        collectionName: String,
        docId: String,
        operation: String,
        fields: Map<String, Any?> = emptyMap()
    ) {
        val queueKey = "$userId:$collectionName:$docId"
        val dao = localDb.pendingSyncDao()
        val existing = dao.getByKey(queueKey)

        when (operation) {
            OP_CREATE -> {
                dao.upsert(
                    PendingSyncEntity(
                        queueKey = queueKey,
                        userId = userId,
                        collectionName = collectionName,
                        docId = docId,
                        operation = OP_CREATE,
                        payloadJson = SyncPayloadCodec.encode(fields),
                        createdAtMs = System.currentTimeMillis()
                    )
                )
            }
            OP_UPDATE -> {
                if (existing != null && existing.operation == OP_CREATE) {
                    // Merge local edit directly into unsent CREATE so cloud gets 1 single write
                    dao.upsert(
                        existing.copy(
                            payloadJson = SyncPayloadCodec.mergeUpdateIntoCreate(existing.payloadJson, fields)
                        )
                    )
                } else if (existing != null && existing.operation == OP_UPDATE) {
                    dao.upsert(
                        existing.copy(
                            payloadJson = SyncPayloadCodec.mergeUpdateIntoCreate(existing.payloadJson, fields)
                        )
                    )
                } else {
                    dao.upsert(
                        PendingSyncEntity(
                            queueKey = queueKey,
                            userId = userId,
                            collectionName = collectionName,
                            docId = docId,
                            operation = OP_UPDATE,
                            payloadJson = SyncPayloadCodec.encode(fields),
                            createdAtMs = System.currentTimeMillis()
                        )
                    )
                }
            }
            OP_DELETE -> {
                if (existing != null && existing.operation == OP_CREATE) {
                    // Created and deleted before ever reaching cloud -> cancel with 0 cloud writes!
                    dao.deleteByKey(queueKey)
                } else {
                    dao.upsert(
                        PendingSyncEntity(
                            queueKey = queueKey,
                            userId = userId,
                            collectionName = collectionName,
                            docId = docId,
                            operation = OP_DELETE,
                            payloadJson = "",
                            createdAtMs = System.currentTimeMillis()
                        )
                    )
                }
            }
        }
    }

    /**
     * Flushes all queued local writes for the currently authenticated user using Firestore WriteBatch.
     * Never syncs another user's records. Never triggers a read/download loop.
     */
    suspend fun flushPendingQueue(uid: String): Result<Int> = flushMutex.withLock {
        if (auth.currentUser?.uid != uid) {
            return@withLock Result.success(0)
        }
        if (!networkMonitor.isCurrentlyOnline()) {
            return@withLock Result.success(0)
        }
        val pending = localDb.pendingSyncDao().getPendingForUser(uid)
        if (pending.isEmpty()) return@withLock Result.success(0)

        var flushedCount = 0
        for (chunk in pending.chunked(MAX_BATCH_OPS)) {
            val batchResult = commitChunkBatch(uid, chunk)
            if (batchResult.isSuccess) {
                localDb.pendingSyncDao().deleteByKeys(chunk.map { it.queueKey })
                flushedCount += chunk.size
            } else {
                // Fall back to individual writes so one bad/not-found doc does not block valid ops
                flushedCount += commitChunkIndividually(uid, chunk)
            }
        }
        Result.success(flushedCount)
    }

    private suspend fun commitChunkBatch(uid: String, chunk: List<PendingSyncEntity>): Result<Unit> {
        return try {
            val batch = db.batch()
            for (op in chunk) {
                val ref = userDoc(uid).collection(op.collectionName).document(op.docId)
                when (op.operation) {
                    OP_CREATE -> {
                        val payload = SyncPayloadCodec.decode(op.payloadJson).apply {
                            put("createdAt", FieldValue.serverTimestamp())
                            put("updatedAt", FieldValue.serverTimestamp())
                        }
                        batch.set(ref, payload)
                    }
                    OP_UPDATE -> {
                        val payload = SyncPayloadCodec.decode(op.payloadJson).apply {
                            put("updatedAt", FieldValue.serverTimestamp())
                        }
                        batch.update(ref, payload)
                    }
                    OP_DELETE -> {
                        batch.delete(ref)
                    }
                }
            }
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "users/$uid/batch(${chunk.size})")
            Result.failure(e)
        }
    }

    private suspend fun commitChunkIndividually(uid: String, chunk: List<PendingSyncEntity>): Int {
        var successCount = 0
        for (op in chunk) {
            val ref = userDoc(uid).collection(op.collectionName).document(op.docId)
            val path = "users/$uid/${op.collectionName}/${op.docId}"
            try {
                when (op.operation) {
                    OP_CREATE -> {
                        val payload = SyncPayloadCodec.decode(op.payloadJson).apply {
                            put("createdAt", FieldValue.serverTimestamp())
                            put("updatedAt", FieldValue.serverTimestamp())
                        }
                        try {
                            ref.set(payload).await()
                        } catch (permErr: FirebaseFirestoreException) {
                            if (permErr.code == FirebaseFirestoreException.Code.PERMISSION_DENIED &&
                                op.collectionName == "settings"
                            ) {
                                // Doc already existed remotely (createdAt immutable on update)
                                val updatePayload = payload.toMutableMap().apply {
                                    remove("userId")
                                    remove("createdAt")
                                }
                                ref.update(updatePayload).await()
                            } else {
                                throw permErr
                            }
                        }
                    }
                    OP_UPDATE -> {
                        val payload = SyncPayloadCodec.decode(op.payloadJson).apply {
                            put("updatedAt", FieldValue.serverTimestamp())
                        }
                        try {
                            ref.update(payload).await()
                        } catch (notFound: FirebaseFirestoreException) {
                            if (notFound.code == FirebaseFirestoreException.Code.NOT_FOUND &&
                                op.collectionName == "settings"
                            ) {
                                // First-time settings write when preferences doc was never created
                                val createPayload = payload.toMutableMap().apply {
                                    put("userId", uid)
                                    put("createdAt", FieldValue.serverTimestamp())
                                }
                                ref.set(createPayload).await()
                            } else if (notFound.code == FirebaseFirestoreException.Code.NOT_FOUND) {
                                // Target doc no longer exists remotely; drop stale update
                            } else {
                                throw notFound
                            }
                        }
                    }
                    OP_DELETE -> {
                        ref.delete().await()
                    }
                }
                localDb.pendingSyncDao().deleteByKey(op.queueKey)
                successCount++
            } catch (e: CancellationException) {
                throw e
            } catch (e: FirebaseFirestoreException) {
                handleFirestoreError(e, OperationType.WRITE, path)
                if (e.code == FirebaseFirestoreException.Code.PERMISSION_DENIED ||
                    e.code == FirebaseFirestoreException.Code.INVALID_ARGUMENT
                ) {
                    // Drop permanently invalid op after logging to avoid infinite retry storm
                    localDb.pendingSyncDao().deleteByKey(op.queueKey)
                } else {
                    localDb.pendingSyncDao().upsert(op.copy(retryCount = op.retryCount + 1))
                }
            } catch (e: Exception) {
                handleFirestoreError(e, OperationType.WRITE, path)
                localDb.pendingSyncDao().upsert(op.copy(retryCount = op.retryCount + 1))
            }
        }
        return successCount
    }

    // --- SMART INCREMENTAL DELTA SYNC (ZERO FULL DOWNLOADS AFTER BOOTSTRAP) ---

    suspend fun syncProfileAndSettingsSmart(
        uid: String,
        forceRefresh: Boolean
    ): Result<Unit> = syncMutex.withLock {
        if (auth.currentUser?.uid != uid) return@withLock Result.success(Unit)
        if (!networkMonitor.isCurrentlyOnline()) return@withLock Result.success(Unit)
        val now = System.currentTimeMillis()
        val meta = localDb.syncMetadataDao().get(uid, KEY_PROFILE_SETTINGS)
        val cachedProfile = localDb.profileDao().getProfileOnce(uid)

        if (!forceRefresh &&
            meta != null &&
            cachedProfile != null &&
            (now - meta.lastSyncedWallMs) < PROFILE_SYNC_COOLDOWN_MS
        ) {
            return@withLock Result.success(Unit)
        }

        val profilePath = "users/$uid/profile/main"
        val settingsPath = "users/$uid/settings/preferences"
        return@withLock try {
            val profileSnap = userDoc(uid).collection("profile").document("main").get().await()
            if (profileSnap.exists()) {
                val remote = profileSnap.toObject(
                    FirestoreProfile::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
                if (remote != null) {
                    localDb.profileDao().upsert(
                        UserProfileEntity(
                            userId = uid,
                            displayName = remote.displayName.ifBlank { "LifeHub Member" },
                            email = remote.email ?: "",
                            photoUrl = remote.photoUrl ?: "",
                            monthlyBudget = remote.monthlyBudget,
                            currencySymbol = remote.currencySymbol.ifBlank { "₹" },
                            totalXp = remote.totalXp.toInt(),
                            updatedAtMs = now
                        )
                    )
                }
            } else {
                val user = auth.currentUser
                val name = (user?.displayName?.takeIf { it.isNotBlank() } ?: "LifeHub Member").take(60)
                val email = user?.email?.take(120)
                val photo = user?.photoUrl?.toString()?.take(500)?.takeIf { it.startsWith("https://") }
                val payload = mapOf(
                    "userId" to uid,
                    "displayName" to name,
                    "email" to email,
                    "photoUrl" to photo,
                    "monthlyBudget" to 25000.0,
                    "currencySymbol" to "₹",
                    "totalXp" to 0L,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                ).filterValues { it != null }
                userDoc(uid).collection("profile").document("main").set(payload).await()
                localDb.profileDao().upsert(
                    UserProfileEntity(
                        userId = uid,
                        displayName = name,
                        email = email ?: "",
                        photoUrl = photo ?: "",
                        monthlyBudget = 25000.0,
                        currencySymbol = "₹",
                        totalXp = 0,
                        updatedAtMs = now
                    )
                )
            }

            val settingsSnap = userDoc(uid).collection("settings").document("preferences").get().await()
            val settingsExists = settingsSnap.exists()
            if (settingsExists) {
                val remote = settingsSnap.toObject(
                    FirestoreSettings::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
                if (remote != null) {
                    preferencesManager.applyRemoteSettings(
                        UserSettings(
                            themeMode = ThemeMode.fromRaw(remote.themeMode),
                            notificationsEnabled = remote.notificationsEnabled,
                            currencySymbol = remote.currencySymbol.ifBlank { "₹" },
                            monthlyBudget = remote.monthlyBudget,
                            separateTripEventLedger = remote.separateTripEventLedger
                        )
                    )
                }
            }

            localDb.syncMetadataDao().upsert(
                SyncMetadataEntity(
                    userId = uid,
                    syncKey = KEY_PROFILE_SETTINGS,
                    lastSyncedWallMs = now,
                    lastServerUpdatedAtMs = now,
                    remoteInitialized = settingsExists
                )
            )
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, "$profilePath / $settingsPath")
            Result.failure(e)
        }
    }

    /**
     * Syncs transactions for the selected month using a two-tier strategy:
     * 1. If the month has NEVER been bootstrapped locally, fetches up to 50 docs for that month once.
     * 2. If already bootstrapped locally, skips network within cooldown, or runs ONE incremental
     *    delta query (`updatedAt > watermark`) fetching ONLY newly added/edited transactions.
     */
    suspend fun syncTransactionsSmart(
        uid: String,
        monthKey: String,
        manualRefresh: Boolean = false,
        fullReconcile: Boolean = false
    ): Result<TransactionPageResult> = syncMutex.withLock {
        if (auth.currentUser?.uid != uid) {
            return@withLock Result.success(TransactionPageResult(0, false))
        }
        if (!networkMonitor.isCurrentlyOnline()) {
            val meta = localDb.syncMetadataDao().get(uid, monthMetaKey(monthKey))
            return@withLock Result.success(TransactionPageResult(0, meta?.hasMore ?: false))
        }

        val now = System.currentTimeMillis()
        val monthMeta = localDb.syncMetadataDao().get(uid, monthMetaKey(monthKey))
        val deltaMeta = localDb.syncMetadataDao().get(uid, KEY_TX_DELTA)
        val path = "users/$uid/transactions"

        return@withLock try {
            if (fullReconcile || monthMeta == null) {
                // Initial bootstrap (or explicit full reconciliation) for this specific month
                val (startMs, endMs) = DateAndFormatUtils.monthBoundsEpochMs(monthKey)
                val snapshot = userDoc(uid).collection("transactions")
                    .whereGreaterThanOrEqualTo("timestampEpochMs", startMs)
                    .whereLessThan("timestampEpochMs", endMs)
                    .orderBy("timestampEpochMs", Query.Direction.DESCENDING)
                    .limit(LifeHubRepository.TRANSACTION_PAGE_SIZE)
                    .get()
                    .await()

                val docs = snapshot.documents
                val entities = docs.mapNotNull { doc -> mapTransactionDoc(uid, monthKey, doc) }
                if (fullReconcile) {
                    localDb.transactionDao().clearMonth(uid, monthKey)
                }
                if (entities.isNotEmpty()) {
                    localDb.transactionDao().upsertAll(entities)
                }

                lastCursorByMonth["$uid:$monthKey"] = docs.lastOrNull()
                val hasMore = docs.size.toLong() == LifeHubRepository.TRANSACTION_PAGE_SIZE
                val maxServerTs = extractMaxUpdatedAtMs(docs)

                localDb.syncMetadataDao().upsert(
                    SyncMetadataEntity(
                        userId = uid,
                        syncKey = monthMetaKey(monthKey),
                        lastSyncedWallMs = now,
                        lastServerUpdatedAtMs = maxServerTs ?: (now - CLOCK_SKEW_BUFFER_MS).coerceAtLeast(1L),
                        hasMore = hasMore,
                        remoteInitialized = true
                    )
                )

                val prevDeltaCursor = deltaMeta?.lastServerUpdatedAtMs ?: 0L
                val nextDeltaCursor = when {
                    maxServerTs != null -> maxOf(prevDeltaCursor, maxServerTs + 1L)
                    prevDeltaCursor > 0L -> prevDeltaCursor
                    else -> (now - CLOCK_SKEW_BUFFER_MS).coerceAtLeast(1L)
                }
                localDb.syncMetadataDao().upsert(
                    SyncMetadataEntity(
                        userId = uid,
                        syncKey = KEY_TX_DELTA,
                        lastSyncedWallMs = now,
                        lastServerUpdatedAtMs = nextDeltaCursor,
                        remoteInitialized = true
                    )
                )
                Result.success(TransactionPageResult(entities.size, hasMore))
            } else {
                // Month is already cached in Room!
                val lastDeltaWall = deltaMeta?.lastSyncedWallMs ?: monthMeta.lastSyncedWallMs
                if (!manualRefresh && (now - lastDeltaWall) < TX_SYNC_COOLDOWN_MS) {
                    // Within cooldown -> 0 Firestore reads!
                    return@withLock Result.success(TransactionPageResult(0, monthMeta.hasMore))
                }

                // Single incremental delta query across transactions (only modified/new docs!)
                val cursorMs = (deltaMeta?.lastServerUpdatedAtMs ?: monthMeta.lastServerUpdatedAtMs)
                    .coerceAtLeast(1L)
                val snapshot = userDoc(uid).collection("transactions")
                    .whereGreaterThan("updatedAt", Timestamp(Date(cursorMs)))
                    .orderBy("updatedAt", Query.Direction.ASCENDING)
                    .limit(DELTA_PAGE_SIZE)
                    .get()
                    .await()

                val docs = snapshot.documents
                val entities = docs.mapNotNull { doc -> mapTransactionDoc(uid, monthKey, doc) }
                if (entities.isNotEmpty()) {
                    localDb.transactionDao().upsertAll(entities)
                }
                val maxServerTs = extractMaxUpdatedAtMs(docs)
                val nextCursor = if (maxServerTs != null) maxOf(cursorMs, maxServerTs + 1L) else cursorMs

                localDb.syncMetadataDao().upsert(
                    SyncMetadataEntity(
                        userId = uid,
                        syncKey = KEY_TX_DELTA,
                        lastSyncedWallMs = now,
                        lastServerUpdatedAtMs = nextCursor,
                        remoteInitialized = true
                    )
                )
                Result.success(TransactionPageResult(entities.size, monthMeta.hasMore))
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, path)
            Result.failure(e)
        }
    }

    /**
     * Cursor-based pagination for older transactions in a month.
     * Supports both in-memory DocumentSnapshot cursor AND cold-start timestamp cursor from Room
     * so restarting the app never forces re-fetching the first 50 items.
     */
    suspend fun loadNextTransactionsPage(
        uid: String,
        monthKey: String
    ): Result<TransactionPageResult> = syncMutex.withLock {
        if (auth.currentUser?.uid != uid) {
            return@withLock Result.success(TransactionPageResult(0, false))
        }
        if (!networkMonitor.isCurrentlyOnline()) {
            return@withLock Result.success(TransactionPageResult(0, false))
        }
        val monthMeta = localDb.syncMetadataDao().get(uid, monthMetaKey(monthKey))
        if (monthMeta != null && !monthMeta.hasMore) {
            return@withLock Result.success(TransactionPageResult(0, false))
        }

        val path = "users/$uid/transactions"
        return@withLock try {
            val (startMs, endMs) = DateAndFormatUtils.monthBoundsEpochMs(monthKey)
            val memCursor = lastCursorByMonth["$uid:$monthKey"]
            val query = if (memCursor != null) {
                userDoc(uid).collection("transactions")
                    .whereGreaterThanOrEqualTo("timestampEpochMs", startMs)
                    .whereLessThan("timestampEpochMs", endMs)
                    .orderBy("timestampEpochMs", Query.Direction.DESCENDING)
                    .startAfter(memCursor)
                    .limit(LifeHubRepository.TRANSACTION_PAGE_SIZE)
            } else {
                val oldestLocal = localDb.transactionDao().getOldestTimestampForMonth(uid, monthKey)
                    ?: return@withLock Result.success(TransactionPageResult(0, false))
                userDoc(uid).collection("transactions")
                    .whereGreaterThanOrEqualTo("timestampEpochMs", startMs)
                    .whereLessThan("timestampEpochMs", oldestLocal)
                    .orderBy("timestampEpochMs", Query.Direction.DESCENDING)
                    .limit(LifeHubRepository.TRANSACTION_PAGE_SIZE)
            }

            val snapshot = query.get().await()
            val docs = snapshot.documents
            val entities = docs.mapNotNull { doc -> mapTransactionDoc(uid, monthKey, doc) }
            if (entities.isNotEmpty()) {
                localDb.transactionDao().upsertAll(entities)
            }
            if (docs.isNotEmpty()) {
                lastCursorByMonth["$uid:$monthKey"] = docs.last()
            }
            val hasMore = docs.size.toLong() == LifeHubRepository.TRANSACTION_PAGE_SIZE
            localDb.syncMetadataDao().upsert(
                (monthMeta ?: SyncMetadataEntity(userId = uid, syncKey = monthMetaKey(monthKey))).copy(
                    lastSyncedWallMs = System.currentTimeMillis(),
                    hasMore = hasMore,
                    remoteInitialized = true
                )
            )
            Result.success(TransactionPageResult(entities.size, hasMore))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, path)
            Result.failure(e)
        }
    }

    /**
     * Syncs Habits, Goals, Tasks, Reminders, and Achievements using persisted delta watermarks:
     * - Skips completely within cooldown (0 reads on repeated launches).
     * - After initial bootstrap, fetches ONLY documents where `updatedAt > cursor`.
     */
    suspend fun syncCoreModulesSmart(
        uid: String,
        manualRefresh: Boolean = false,
        fullReconcile: Boolean = false
    ): Result<Unit> = syncMutex.withLock {
        if (auth.currentUser?.uid != uid) return@withLock Result.success(Unit)
        if (!networkMonitor.isCurrentlyOnline()) return@withLock Result.success(Unit)
        val now = System.currentTimeMillis()
        val meta = localDb.syncMetadataDao().get(uid, KEY_CORE_MODULES)

        if (!manualRefresh &&
            !fullReconcile &&
            meta != null &&
            (now - meta.lastSyncedWallMs) < CORE_SYNC_COOLDOWN_MS
        ) {
            return@withLock Result.success(Unit)
        }

        val useDelta = !fullReconcile && meta != null && meta.lastServerUpdatedAtMs > 0L
        val cursorTs = if (useDelta) Timestamp(Date(meta!!.lastServerUpdatedAtMs)) else null

        return@withLock try {
            var maxSeenTs = meta?.lastServerUpdatedAtMs ?: 0L

            // 1. Habits
            val habitQuery = if (cursorTs != null) {
                userDoc(uid).collection("habits")
                    .whereGreaterThan("updatedAt", cursorTs)
                    .orderBy("updatedAt", Query.Direction.ASCENDING)
                    .limit(50)
            } else {
                userDoc(uid).collection("habits").limit(50)
            }
            val habitDocs = habitQuery.get().await().documents
            val habits = habitDocs.mapNotNull { doc ->
                val h = doc.toObject(FirestoreHabit::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    ?: return@mapNotNull null
                HabitEntity(
                    id = h.id.ifBlank { doc.id },
                    userId = uid,
                    title = h.title,
                    iconKey = h.iconKey,
                    colorHex = h.colorHex,
                    targetDaysPerWeek = h.targetDaysPerWeek.toInt().coerceIn(1, 7),
                    currentStreak = h.currentStreak.toInt(),
                    longestStreak = h.longestStreak.toInt(),
                    completedDatesCsv = h.completedDates.joinToString(",")
                )
            }
            if (habits.isNotEmpty()) localDb.habitDao().upsertAll(habits)
            extractMaxUpdatedAtMs(habitDocs)?.let { maxSeenTs = maxOf(maxSeenTs, it + 1L) }

            // 2. Goals
            val goalQuery = if (cursorTs != null) {
                userDoc(uid).collection("goals")
                    .whereGreaterThan("updatedAt", cursorTs)
                    .orderBy("updatedAt", Query.Direction.ASCENDING)
                    .limit(50)
            } else {
                userDoc(uid).collection("goals").limit(50)
            }
            val goalDocs = goalQuery.get().await().documents
            val goals = goalDocs.mapNotNull { doc ->
                val g = doc.toObject(FirestoreGoal::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    ?: return@mapNotNull null
                SavingsGoalEntity(
                    id = g.id.ifBlank { doc.id },
                    userId = uid,
                    title = g.title,
                    category = g.category,
                    targetAmount = g.targetAmount,
                    savedAmount = g.savedAmount,
                    completed = g.completed,
                    targetMonth = g.targetMonth
                )
            }
            if (goals.isNotEmpty()) localDb.goalDao().upsertAll(goals)
            extractMaxUpdatedAtMs(goalDocs)?.let { maxSeenTs = maxOf(maxSeenTs, it + 1L) }

            // 3. Tasks
            val taskQuery = if (cursorTs != null) {
                userDoc(uid).collection("tasks")
                    .whereGreaterThan("updatedAt", cursorTs)
                    .orderBy("updatedAt", Query.Direction.ASCENDING)
                    .limit(100)
            } else {
                userDoc(uid).collection("tasks").limit(100)
            }
            val taskDocs = taskQuery.get().await().documents
            val tasks = taskDocs.mapNotNull { doc ->
                val t = doc.toObject(FirestoreTask::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    ?: return@mapNotNull null
                TaskEntity(
                    id = t.id.ifBlank { doc.id },
                    userId = uid,
                    title = t.title,
                    cadence = t.cadence,
                    priority = t.priority,
                    status = t.status,
                    dueDateEpochMs = t.dueDateEpochMs,
                    recurring = t.recurring,
                    notes = t.notes ?: ""
                )
            }
            if (tasks.isNotEmpty()) localDb.taskDao().upsertAll(tasks)
            extractMaxUpdatedAtMs(taskDocs)?.let { maxSeenTs = maxOf(maxSeenTs, it + 1L) }

            // 4. Reminders
            val remQuery = if (cursorTs != null) {
                userDoc(uid).collection("reminders")
                    .whereGreaterThan("updatedAt", cursorTs)
                    .orderBy("updatedAt", Query.Direction.ASCENDING)
                    .limit(50)
            } else {
                userDoc(uid).collection("reminders").limit(50)
            }
            val remDocs = remQuery.get().await().documents
            val reminders = remDocs.mapNotNull { doc ->
                val r = doc.toObject(FirestoreReminder::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    ?: return@mapNotNull null
                ReminderEntity(
                    id = r.id.ifBlank { doc.id },
                    userId = uid,
                    title = r.title,
                    category = r.category,
                    amount = r.amount,
                    dueDateEpochMs = r.dueDateEpochMs,
                    recurringMonthly = r.recurringMonthly,
                    paid = r.paid
                )
            }
            if (reminders.isNotEmpty()) localDb.reminderDao().upsertAll(reminders)
            extractMaxUpdatedAtMs(remDocs)?.let { maxSeenTs = maxOf(maxSeenTs, it + 1L) }

            // 5. Achievements
            val achQuery = if (cursorTs != null) {
                userDoc(uid).collection("achievements")
                    .whereGreaterThan("updatedAt", cursorTs)
                    .orderBy("updatedAt", Query.Direction.ASCENDING)
                    .limit(50)
            } else {
                userDoc(uid).collection("achievements").limit(50)
            }
            val achDocs = achQuery.get().await().documents
            val achievements = achDocs.mapNotNull { doc ->
                val a = doc.toObject(FirestoreAchievement::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    ?: return@mapNotNull null
                AchievementEntity(
                    id = a.id.ifBlank { doc.id },
                    userId = uid,
                    code = a.code,
                    xpAwarded = a.xpAwarded.toInt(),
                    unlocked = a.unlocked
                )
            }
            if (achievements.isNotEmpty()) localDb.achievementDao().upsertAll(achievements)
            extractMaxUpdatedAtMs(achDocs)?.let { maxSeenTs = maxOf(maxSeenTs, it + 1L) }

            val finalWatermark = if (maxSeenTs > 0L) maxSeenTs else (now - CLOCK_SKEW_BUFFER_MS).coerceAtLeast(1L)
            localDb.syncMetadataDao().upsert(
                SyncMetadataEntity(
                    userId = uid,
                    syncKey = KEY_CORE_MODULES,
                    lastSyncedWallMs = now,
                    lastServerUpdatedAtMs = finalWatermark,
                    remoteInitialized = true
                )
            )
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, "users/$uid/*")
            Result.failure(e)
        }
    }

    suspend fun hasMoreForMonth(uid: String, monthKey: String): Boolean {
        return localDb.syncMetadataDao().get(uid, monthMetaKey(monthKey))?.hasMore ?: false
    }

    suspend fun isSettingsInitializedRemotely(uid: String): Boolean {
        return localDb.syncMetadataDao().get(uid, KEY_PROFILE_SETTINGS)?.remoteInitialized ?: false
    }

    suspend fun markSettingsInitializedRemotely(uid: String) {
        val existing = localDb.syncMetadataDao().get(uid, KEY_PROFILE_SETTINGS)
        localDb.syncMetadataDao().upsert(
            (existing ?: SyncMetadataEntity(userId = uid, syncKey = KEY_PROFILE_SETTINGS)).copy(
                remoteInitialized = true
            )
        )
    }

    fun clearMemoryCursors() {
        lastCursorByMonth.clear()
    }

    private fun mapTransactionDoc(uid: String, fallbackMonthKey: String, doc: DocumentSnapshot): TransactionEntity? {
        val item = doc.toObject(
            FirestoreTransaction::class.java,
            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
        ) ?: return null
        val resolvedMonth = item.monthKey.ifBlank {
            if (item.timestampEpochMs > 0L) DateAndFormatUtils.monthKeyFromEpoch(item.timestampEpochMs)
            else fallbackMonthKey
        }
        return TransactionEntity(
            id = item.id.ifBlank { doc.id },
            userId = uid,
            title = item.title,
            amount = item.amount,
            type = item.type,
            category = item.category,
            monthKey = resolvedMonth,
            timestampEpochMs = item.timestampEpochMs,
            note = item.note ?: "",
            isTripOrEvent = item.isTripOrEvent,
            eventName = item.eventName ?: ""
        )
    }

    private fun extractMaxUpdatedAtMs(docs: List<DocumentSnapshot>): Long? {
        return docs.mapNotNull { doc ->
            doc.getTimestamp("updatedAt", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                ?.toDate()
                ?.time
        }.maxOrNull()
    }

    companion object {
        const val OP_CREATE = "CREATE"
        const val OP_UPDATE = "UPDATE"
        const val OP_DELETE = "DELETE"

        const val KEY_PROFILE_SETTINGS = "profile_settings"
        const val KEY_CORE_MODULES = "core_modules"
        const val KEY_TX_DELTA = "tx_delta"

        fun monthMetaKey(monthKey: String) = "tx_month_$monthKey"

        private const val TX_SYNC_COOLDOWN_MS = 5 * 60_000L       // 5 minutes
        private const val CORE_SYNC_COOLDOWN_MS = 10 * 60_000L    // 10 minutes
        private const val PROFILE_SYNC_COOLDOWN_MS = 30 * 60_000L // 30 minutes
        private const val CLOCK_SKEW_BUFFER_MS = 30_000L
        private const val DELTA_PAGE_SIZE = 100L
        private const val MAX_BATCH_OPS = 400
    }
}
