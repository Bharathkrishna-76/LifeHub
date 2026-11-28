package com.example.data.repository

import android.content.Context
import com.example.data.export.ExportAndShareHelper
import com.example.data.local.AchievementEntity
import com.example.data.local.HabitEntity
import com.example.data.local.LifeHubDatabase
import com.example.data.local.PreferencesManager
import com.example.data.local.ReminderEntity
import com.example.data.local.SavingsGoalEntity
import com.example.data.local.TaskEntity
import com.example.data.local.TransactionEntity
import com.example.data.remote.NetworkMonitor
import com.example.data.remote.OperationType
import com.example.data.remote.handleFirestoreError
import com.example.data.worker.ReminderScheduler
import com.example.tripsplit.data.TripSplitDatabase
import com.example.tripsplit.data.TripSplitPreferences
import com.example.domain.model.ExpenseCategory
import com.example.domain.model.GoalPreset
import com.example.domain.model.HabitItem
import com.example.domain.model.ReminderCategory
import com.example.domain.model.ReminderItem
import com.example.domain.model.SavingsGoalItem
import com.example.domain.model.TaskCadence
import com.example.domain.model.TaskItem
import com.example.domain.model.TaskPriority
import com.example.domain.model.TaskStatus
import com.example.domain.model.TransactionItem
import com.example.domain.model.TransactionType
import com.example.domain.model.UserProfile
import com.example.domain.model.UserSettings
import com.example.domain.util.DateAndFormatUtils
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.UUID

data class TransactionPageResult(
    val fetchedCount: Int,
    val hasMore: Boolean
)

/**
 * Offline-first repository using Room SQLite as the primary UI data source
 * and Firebase Firestore as the cloud sync/backup layer via [FirestoreSyncEngine].
 *
 * Key Spark-plan optimizations:
 * - Zero real-time snapshot listeners; all screens observe local Room Flows (0 reads on tab switch/recomposition).
 * - Persisted sync watermarks & cooldowns in Room survive app restarts (0 reads on repeat launches within cooldown).
 * - Incremental delta queries (`updatedAt > cursor`) fetch only modified/new documents instead of full collections.
 * - Instant local Room writes for Add / Edit / Delete + background WriteBatch outbox sync with offline retry.
 * - Zero redundant `.get()` reads before settings or achievement writes.
 */
class LifeHubRepository(
    private val db: FirebaseFirestore,
    private val localDb: LifeHubDatabase,
    private val preferencesManager: PreferencesManager,
    private val reminderScheduler: ReminderScheduler,
    private val networkMonitor: NetworkMonitor,
    private val auth: FirebaseAuth = Firebase.auth
) {
    private val writeMutex = Mutex()
    private val inFlightWriteKeys = mutableSetOf<String>()
    private val backgroundSyncScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val syncEngine = FirestoreSyncEngine(
        db = db,
        localDb = localDb,
        preferencesManager = preferencesManager,
        networkMonitor = networkMonitor,
        auth = auth
    )

    private fun requireUserId(): String {
        val uid = auth.currentUser?.uid
        if (uid.isNullOrBlank()) {
            throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
        }
        return uid
    }

    // --- LOCAL REACTIVE FLOWS (PRIMARY SOURCE OF TRUTH — ZERO NETWORK READS) ---

    fun observeOnlineStatus(): Flow<Boolean> = networkMonitor.isOnline

    fun observeProfile(userId: String): Flow<UserProfile> =
        localDb.profileDao().observeProfile(userId).map { entity ->
            entity?.toDomain() ?: UserProfile(
                userId = userId,
                displayName = auth.currentUser?.displayName?.take(60) ?: "LifeHub Member",
                email = auth.currentUser?.email ?: "",
                photoUrl = auth.currentUser?.photoUrl?.toString() ?: ""
            )
        }

    fun observeSettings(): Flow<UserSettings> = preferencesManager.settingsFlow

    fun observeTransactionsForMonth(userId: String, monthKey: String): Flow<List<TransactionItem>> =
        localDb.transactionDao().observeByMonth(userId, monthKey).map { list ->
            list.map { it.toDomain() }
        }

    /**
     * Observes ALL locally cached transactions for the user across months
     * so Yearly Reports, Multi-Month Charts, Search, and Date Range Filters run with 0 Firebase reads.
     */
    fun observeAllCachedTransactions(userId: String): Flow<List<TransactionItem>> =
        localDb.transactionDao().observeAllForUser(userId).map { list ->
            list.map { it.toDomain() }
        }

    fun observeHabits(userId: String): Flow<List<HabitItem>> =
        localDb.habitDao().observeAll(userId).map { list -> list.map { it.toDomain() } }

    fun observeGoals(userId: String): Flow<List<SavingsGoalItem>> =
        localDb.goalDao().observeAll(userId).map { list -> list.map { it.toDomain() } }

    fun observeTasks(userId: String): Flow<List<TaskItem>> =
        localDb.taskDao().observeAll(userId).map { list -> list.map { it.toDomain() } }

    fun observeReminders(userId: String): Flow<List<ReminderItem>> =
        localDb.reminderDao().observeAll(userId).map { list -> list.map { it.toDomain() } }

    fun observePersistedAchievementCodes(userId: String): Flow<Set<String>> =
        localDb.achievementDao().observeAll(userId).map { list ->
            list.filter { it.unlocked }.map { it.code }.toSet()
        }

    fun observePendingSyncCount(userId: String): Flow<Int> =
        localDb.pendingSyncDao().observePendingCount(userId)

    fun observeLastSyncTime(userId: String): Flow<Long> =
        localDb.syncMetadataDao().observeLastSyncTime(userId).map { it ?: 0L }

    suspend fun getAllCachedTransactions(userId: String): List<TransactionItem> =
        localDb.transactionDao().getAllCachedForUser(userId).map { it.toDomain() }

    suspend fun hasMoreForMonth(monthKey: String): Boolean {
        val uid = auth.currentUser?.uid ?: return false
        return syncEngine.hasMoreForMonth(uid, monthKey)
    }

    // --- SMART SYNCHRONIZATION ---

    suspend fun flushPendingSyncQueue(): Result<Int> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        return syncEngine.flushPendingQueue(uid)
    }

    suspend fun syncProfileAndSettingsSmart(forceRefresh: Boolean = false): Result<Unit> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        return syncEngine.syncProfileAndSettingsSmart(uid, forceRefresh)
    }

    suspend fun syncTransactionsSmart(
        monthKey: String,
        manualRefresh: Boolean = false,
        fullReconcile: Boolean = false
    ): Result<TransactionPageResult> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        // Push pending local edits first so cloud doesn't overwrite local unsynced records
        syncEngine.flushPendingQueue(uid)
        return syncEngine.syncTransactionsSmart(uid, monthKey, manualRefresh, fullReconcile)
    }

    suspend fun loadNextTransactionsPage(monthKey: String): Result<TransactionPageResult> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        return syncEngine.loadNextTransactionsPage(uid, monthKey)
    }

    suspend fun syncCoreModulesSmart(
        manualRefresh: Boolean = false,
        fullReconcile: Boolean = false
    ): Result<Unit> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        return syncEngine.syncCoreModulesSmart(uid, manualRefresh, fullReconcile)
    }

    private fun triggerBackgroundFlush(uid: String) {
        backgroundSyncScope.launch {
            syncEngine.flushPendingQueue(uid)
        }
    }

    // --- INSTANT LOCAL-FIRST MUTATIONS + BACKGROUND BATCH SYNC ---

    private suspend fun <T> withWriteGuard(writeKey: String, block: suspend () -> Result<T>): Result<T> {
        val acquired = writeMutex.withLock {
            if (writeKey in inFlightWriteKeys) {
                false
            } else {
                inFlightWriteKeys.add(writeKey)
                true
            }
        }
        if (!acquired) {
            return Result.failure(DuplicateWriteIgnoredException())
        }
        return try {
            block()
        } finally {
            writeMutex.withLock { inFlightWriteKeys.remove(writeKey) }
        }
    }

    suspend fun addTransaction(
        clientDocId: String,
        title: String,
        amount: Double,
        type: TransactionType,
        category: ExpenseCategory,
        timestampEpochMs: Long,
        note: String,
        isTripOrEvent: Boolean = false,
        eventName: String = ""
    ): Result<Unit> = withWriteGuard("tx_$clientDocId") {
        val cleanTitle = title.trim().take(80)
        if (cleanTitle.isBlank()) return@withWriteGuard Result.failure(IllegalArgumentException("Enter a title"))
        if (!amount.isFinite() || amount <= 0.0 || amount > 100_000_000.0) {
            return@withWriteGuard Result.failure(IllegalArgumentException("Enter a valid amount"))
        }

        val docId = sanitizeDocId(clientDocId)
        val monthKey = DateAndFormatUtils.monthKeyFromEpoch(timestampEpochMs)
        var docPath = "users/unauthenticated/transactions/$docId"
        try {
            val uid = requireUserId()
            docPath = "users/$uid/transactions/$docId"
            val cleanNote = note.trim().take(240).ifBlank { null }
            val cleanEvent = if (isTripOrEvent) eventName.trim().take(60).ifBlank { null } else null

            // 1. Update Room immediately -> UI reflects instantly (0 network wait)
            localDb.transactionDao().upsert(
                TransactionEntity(
                    id = docId,
                    userId = uid,
                    title = cleanTitle,
                    amount = amount,
                    type = type.name,
                    category = category.name,
                    monthKey = monthKey,
                    timestampEpochMs = timestampEpochMs,
                    note = cleanNote ?: "",
                    isTripOrEvent = isTripOrEvent,
                    eventName = cleanEvent ?: ""
                )
            )

            // 2. Enqueue deterministic CREATE in offline-safe outbox & flush in background
            val payload = mapOf<String, Any?>(
                "userId" to uid,
                "title" to cleanTitle,
                "amount" to amount,
                "type" to type.name,
                "category" to category.name,
                "monthKey" to monthKey,
                "timestampEpochMs" to timestampEpochMs,
                "note" to cleanNote,
                "isTripOrEvent" to isTripOrEvent,
                "eventName" to cleanEvent
            ).filterValues { it != null }

            syncEngine.enqueueOperation(
                userId = uid,
                collectionName = "transactions",
                docId = docId,
                operation = FirestoreSyncEngine.OP_CREATE,
                fields = payload
            )
            triggerBackgroundFlush(uid)
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    /**
     * Edits ONLY the affected transaction locally in Room and syncs ONLY that document in background.
     * Preserves original docId, userId, createdAt, and timestampEpochMs (unless changed).
     */
    suspend fun updateTransaction(
        existing: TransactionItem,
        title: String,
        amount: Double,
        type: TransactionType,
        category: ExpenseCategory,
        note: String,
        isTripOrEvent: Boolean = existing.isTripOrEvent,
        eventName: String = existing.eventName
    ): Result<Unit> = withWriteGuard("edit_tx_${existing.id}") {
        val cleanTitle = title.trim().take(80)
        if (cleanTitle.isBlank()) return@withWriteGuard Result.failure(IllegalArgumentException("Enter a title"))
        if (!amount.isFinite() || amount <= 0.0 || amount > 100_000_000.0) {
            return@withWriteGuard Result.failure(IllegalArgumentException("Enter a valid amount"))
        }

        var docPath = "users/unauthenticated/transactions/${existing.id}"
        try {
            val uid = requireUserId()
            docPath = "users/$uid/transactions/${existing.id}"
            val cleanNote = note.trim().take(240)
            val cleanEvent = if (isTripOrEvent) eventName.trim().take(60) else ""

            // Update ONLY the single affected Room record
            localDb.transactionDao().upsert(
                TransactionEntity(
                    id = existing.id,
                    userId = uid,
                    title = cleanTitle,
                    amount = amount,
                    type = type.name,
                    category = category.name,
                    monthKey = existing.monthKey,
                    timestampEpochMs = existing.timestampEpochMs,
                    note = cleanNote,
                    isTripOrEvent = isTripOrEvent,
                    eventName = cleanEvent
                )
            )

            val updateFields = mutableMapOf<String, Any?>(
                "title" to cleanTitle,
                "amount" to amount,
                "type" to type.name,
                "category" to category.name,
                "note" to cleanNote,
                "isTripOrEvent" to isTripOrEvent,
                "eventName" to cleanEvent
            )

            syncEngine.enqueueOperation(
                userId = uid,
                collectionName = "transactions",
                docId = existing.id,
                operation = FirestoreSyncEngine.OP_UPDATE,
                fields = updateFields
            )
            triggerBackgroundFlush(uid)
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteTransaction(transactionId: String): Result<Unit> = withWriteGuard("del_tx_$transactionId") {
        var docPath = "users/unauthenticated/transactions/$transactionId"
        try {
            val uid = requireUserId()
            docPath = "users/$uid/transactions/$transactionId"
            localDb.transactionDao().deleteById(uid, transactionId)
            syncEngine.enqueueOperation(
                userId = uid,
                collectionName = "transactions",
                docId = transactionId,
                operation = FirestoreSyncEngine.OP_DELETE
            )
            triggerBackgroundFlush(uid)
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    suspend fun addHabit(
        clientDocId: String,
        title: String,
        iconKey: String,
        colorHex: String,
        targetDaysPerWeek: Int
    ): Result<Unit> = withWriteGuard("habit_$clientDocId") {
        val cleanTitle = title.trim().take(60)
        if (cleanTitle.isBlank()) return@withWriteGuard Result.failure(IllegalArgumentException("Enter habit name"))
        val docId = sanitizeDocId(clientDocId)
        val safeTarget = targetDaysPerWeek.coerceIn(1, 7)
        val safeColor = sanitizeColorHex(colorHex)
        var docPath = "users/unauthenticated/habits/$docId"
        try {
            val uid = requireUserId()
            docPath = "users/$uid/habits/$docId"
            localDb.habitDao().upsert(
                HabitEntity(
                    id = docId,
                    userId = uid,
                    title = cleanTitle,
                    iconKey = iconKey.take(30).ifBlank { "check" },
                    colorHex = safeColor,
                    targetDaysPerWeek = safeTarget,
                    currentStreak = 0,
                    longestStreak = 0,
                    completedDatesCsv = ""
                )
            )
            val payload = mapOf<String, Any?>(
                "userId" to uid,
                "title" to cleanTitle,
                "iconKey" to iconKey.take(30).ifBlank { "check" },
                "colorHex" to safeColor,
                "targetDaysPerWeek" to safeTarget.toLong(),
                "currentStreak" to 0L,
                "longestStreak" to 0L,
                "completedDates" to emptyList<String>()
            )
            syncEngine.enqueueOperation(uid, "habits", docId, FirestoreSyncEngine.OP_CREATE, payload)
            triggerBackgroundFlush(uid)
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun toggleHabitForDate(habit: HabitItem, dateKey: String): Result<Unit> =
        withWriteGuard("toggle_habit_${habit.id}_$dateKey") {
            var docPath = "users/unauthenticated/habits/${habit.id}"
            try {
                val uid = requireUserId()
                docPath = "users/$uid/habits/${habit.id}"
                val updatedDates = habit.completedDates.toMutableList()
                if (updatedDates.contains(dateKey)) {
                    updatedDates.remove(dateKey)
                } else {
                    updatedDates.add(dateKey)
                }
                val boundedDates = updatedDates.distinct().sorted().takeLast(365)
                val (currentStreak, longestStreak) = DateAndFormatUtils.calculateStreaks(boundedDates)
                val finalLongest = maxOf(longestStreak, habit.longestStreak)

                localDb.habitDao().upsert(
                    HabitEntity(
                        id = habit.id,
                        userId = uid,
                        title = habit.title,
                        iconKey = habit.iconKey,
                        colorHex = habit.colorHex,
                        targetDaysPerWeek = habit.targetDaysPerWeek,
                        currentStreak = currentStreak,
                        longestStreak = finalLongest,
                        completedDatesCsv = boundedDates.joinToString(",")
                    )
                )
                syncEngine.enqueueOperation(
                    userId = uid,
                    collectionName = "habits",
                    docId = habit.id,
                    operation = FirestoreSyncEngine.OP_UPDATE,
                    fields = mapOf(
                        "completedDates" to boundedDates,
                        "currentStreak" to currentStreak.toLong(),
                        "longestStreak" to finalLongest.toLong()
                    )
                )
                triggerBackgroundFlush(uid)
                Result.success(Unit)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                handleFirestoreError(e, OperationType.UPDATE, docPath)
                Result.failure(e)
            }
        }

    suspend fun deleteHabit(habitId: String): Result<Unit> = withWriteGuard("del_habit_$habitId") {
        var docPath = "users/unauthenticated/habits/$habitId"
        try {
            val uid = requireUserId()
            docPath = "users/$uid/habits/$habitId"
            localDb.habitDao().deleteById(uid, habitId)
            syncEngine.enqueueOperation(uid, "habits", habitId, FirestoreSyncEngine.OP_DELETE)
            triggerBackgroundFlush(uid)
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    suspend fun addGoal(
        clientDocId: String,
        title: String,
        category: GoalPreset,
        targetAmount: Double,
        initialSaved: Double,
        targetMonth: String
    ): Result<Unit> = withWriteGuard("goal_$clientDocId") {
        val cleanTitle = title.trim().take(60)
        if (cleanTitle.isBlank()) return@withWriteGuard Result.failure(IllegalArgumentException("Enter goal title"))
        if (!targetAmount.isFinite() || targetAmount <= 0.0 || targetAmount > 1_000_000_000.0) {
            return@withWriteGuard Result.failure(IllegalArgumentException("Target must be positive"))
        }
        val safeSaved = if (initialSaved.isFinite()) initialSaved.coerceIn(0.0, 1_000_000_000.0) else 0.0
        val completed = safeSaved >= targetAmount
        val docId = sanitizeDocId(clientDocId)
        val cleanTargetMonth = targetMonth.trim().take(20)
        var docPath = "users/unauthenticated/goals/$docId"
        try {
            val uid = requireUserId()
            docPath = "users/$uid/goals/$docId"
            localDb.goalDao().upsert(
                SavingsGoalEntity(
                    id = docId,
                    userId = uid,
                    title = cleanTitle,
                    category = category.key,
                    targetAmount = targetAmount,
                    savedAmount = safeSaved,
                    completed = completed,
                    targetMonth = cleanTargetMonth
                )
            )
            val payload = mapOf<String, Any?>(
                "userId" to uid,
                "title" to cleanTitle,
                "category" to category.key,
                "targetAmount" to targetAmount,
                "savedAmount" to safeSaved,
                "completed" to completed,
                "targetMonth" to cleanTargetMonth
            )
            syncEngine.enqueueOperation(uid, "goals", docId, FirestoreSyncEngine.OP_CREATE, payload)
            triggerBackgroundFlush(uid)
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun contributeToGoal(
        goal: SavingsGoalItem,
        contributionAmount: Double
    ): Result<Boolean> = withWriteGuard("contrib_${goal.id}_${contributionAmount}") {
        if (!contributionAmount.isFinite() || contributionAmount <= 0.0 || contributionAmount > 1_000_000_000.0) {
            return@withWriteGuard Result.failure(IllegalArgumentException("Contribution must be positive"))
        }
        var docPath = "users/unauthenticated/goals/${goal.id}"
        try {
            val uid = requireUserId()
            docPath = "users/$uid/goals/${goal.id}"
            val newSaved = (goal.savedAmount + contributionAmount).coerceAtMost(1_000_000_000.0)
            val nowCompleted = newSaved >= goal.targetAmount
            val justCompleted = !goal.completed && nowCompleted

            localDb.goalDao().upsert(
                SavingsGoalEntity(
                    id = goal.id,
                    userId = uid,
                    title = goal.title,
                    category = goal.category.key,
                    targetAmount = goal.targetAmount,
                    savedAmount = newSaved,
                    completed = nowCompleted,
                    targetMonth = goal.targetMonth
                )
            )
            syncEngine.enqueueOperation(
                userId = uid,
                collectionName = "goals",
                docId = goal.id,
                operation = FirestoreSyncEngine.OP_UPDATE,
                fields = mapOf(
                    "savedAmount" to newSaved,
                    "completed" to nowCompleted
                )
            )
            triggerBackgroundFlush(uid)
            Result.success(justCompleted)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteGoal(goalId: String): Result<Unit> = withWriteGuard("del_goal_$goalId") {
        var docPath = "users/unauthenticated/goals/$goalId"
        try {
            val uid = requireUserId()
            docPath = "users/$uid/goals/$goalId"
            localDb.goalDao().deleteById(uid, goalId)
            syncEngine.enqueueOperation(uid, "goals", goalId, FirestoreSyncEngine.OP_DELETE)
            triggerBackgroundFlush(uid)
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    suspend fun addTask(
        clientDocId: String,
        title: String,
        cadence: TaskCadence,
        priority: TaskPriority,
        dueDateEpochMs: Long,
        recurring: Boolean,
        notes: String
    ): Result<Unit> = withWriteGuard("task_$clientDocId") {
        val cleanTitle = title.trim().take(100)
        if (cleanTitle.isBlank()) return@withWriteGuard Result.failure(IllegalArgumentException("Enter task title"))
        val docId = sanitizeDocId(clientDocId)
        val cleanNotes = notes.trim().take(300).ifBlank { null }
        var docPath = "users/unauthenticated/tasks/$docId"
        try {
            val uid = requireUserId()
            docPath = "users/$uid/tasks/$docId"
            localDb.taskDao().upsert(
                TaskEntity(
                    id = docId,
                    userId = uid,
                    title = cleanTitle,
                    cadence = cadence.name,
                    priority = priority.name,
                    status = TaskStatus.TODO.name,
                    dueDateEpochMs = dueDateEpochMs.coerceAtLeast(0L),
                    recurring = recurring,
                    notes = cleanNotes ?: ""
                )
            )
            val payload = mapOf<String, Any?>(
                "userId" to uid,
                "title" to cleanTitle,
                "cadence" to cadence.name,
                "priority" to priority.name,
                "status" to TaskStatus.TODO.name,
                "dueDateEpochMs" to dueDateEpochMs.coerceAtLeast(0L),
                "recurring" to recurring,
                "notes" to cleanNotes
            ).filterValues { it != null }
            syncEngine.enqueueOperation(uid, "tasks", docId, FirestoreSyncEngine.OP_CREATE, payload)
            triggerBackgroundFlush(uid)
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun updateTaskStatus(task: TaskItem, newStatus: TaskStatus): Result<Unit> =
        withWriteGuard("task_status_${task.id}_${newStatus.name}") {
            var docPath = "users/unauthenticated/tasks/${task.id}"
            try {
                val uid = requireUserId()
                docPath = "users/$uid/tasks/${task.id}"
                localDb.taskDao().upsert(
                    TaskEntity(
                        id = task.id,
                        userId = uid,
                        title = task.title,
                        cadence = task.cadence.name,
                        priority = task.priority.name,
                        status = newStatus.name,
                        dueDateEpochMs = task.dueDateEpochMs,
                        recurring = task.recurring,
                        notes = task.notes
                    )
                )
                syncEngine.enqueueOperation(
                    userId = uid,
                    collectionName = "tasks",
                    docId = task.id,
                    operation = FirestoreSyncEngine.OP_UPDATE,
                    fields = mapOf("status" to newStatus.name)
                )
                triggerBackgroundFlush(uid)
                Result.success(Unit)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                handleFirestoreError(e, OperationType.UPDATE, docPath)
                Result.failure(e)
            }
        }

    suspend fun deleteTask(taskId: String): Result<Unit> = withWriteGuard("del_task_$taskId") {
        var docPath = "users/unauthenticated/tasks/$taskId"
        try {
            val uid = requireUserId()
            docPath = "users/$uid/tasks/$taskId"
            localDb.taskDao().deleteById(uid, taskId)
            syncEngine.enqueueOperation(uid, "tasks", taskId, FirestoreSyncEngine.OP_DELETE)
            triggerBackgroundFlush(uid)
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    suspend fun addReminder(
        clientDocId: String,
        title: String,
        category: ReminderCategory,
        amount: Double,
        dueDateEpochMs: Long,
        recurringMonthly: Boolean,
        currencySymbol: String,
        notificationsEnabled: Boolean
    ): Result<Unit> = withWriteGuard("rem_$clientDocId") {
        val cleanTitle = title.trim().take(80)
        if (cleanTitle.isBlank()) return@withWriteGuard Result.failure(IllegalArgumentException("Enter reminder title"))
        val docId = sanitizeDocId(clientDocId)
        val safeAmount = if (amount.isFinite()) amount.coerceIn(0.0, 100_000_000.0) else 0.0
        val safeDue = dueDateEpochMs.coerceAtLeast(System.currentTimeMillis() + 10_000L)
        var docPath = "users/unauthenticated/reminders/$docId"
        try {
            val uid = requireUserId()
            docPath = "users/$uid/reminders/$docId"
            val entity = ReminderEntity(
                id = docId,
                userId = uid,
                title = cleanTitle,
                category = category.name,
                amount = safeAmount,
                dueDateEpochMs = safeDue,
                recurringMonthly = recurringMonthly,
                paid = false
            )
            localDb.reminderDao().upsert(entity)
            if (notificationsEnabled) {
                reminderScheduler.scheduleReminder(entity.toDomain(), currencySymbol)
            }
            val payload = mapOf<String, Any?>(
                "userId" to uid,
                "title" to cleanTitle,
                "category" to category.name,
                "amount" to safeAmount,
                "dueDateEpochMs" to safeDue,
                "recurringMonthly" to recurringMonthly,
                "paid" to false
            )
            syncEngine.enqueueOperation(uid, "reminders", docId, FirestoreSyncEngine.OP_CREATE, payload)
            triggerBackgroundFlush(uid)
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun toggleReminderPaid(
        reminder: ReminderItem,
        currencySymbol: String,
        notificationsEnabled: Boolean
    ): Result<Unit> = withWriteGuard("rem_paid_${reminder.id}") {
        var docPath = "users/unauthenticated/reminders/${reminder.id}"
        try {
            val uid = requireUserId()
            docPath = "users/$uid/reminders/${reminder.id}"
            val newPaid = !reminder.paid
            val updatedEntity = ReminderEntity(
                id = reminder.id,
                userId = uid,
                title = reminder.title,
                category = reminder.category.name,
                amount = reminder.amount,
                dueDateEpochMs = reminder.dueDateEpochMs,
                recurringMonthly = reminder.recurringMonthly,
                paid = newPaid
            )
            localDb.reminderDao().upsert(updatedEntity)
            if (newPaid) {
                reminderScheduler.cancelReminder(reminder.id)
            } else if (notificationsEnabled) {
                reminderScheduler.scheduleReminder(updatedEntity.toDomain(), currencySymbol)
            }
            syncEngine.enqueueOperation(
                userId = uid,
                collectionName = "reminders",
                docId = reminder.id,
                operation = FirestoreSyncEngine.OP_UPDATE,
                fields = mapOf("paid" to newPaid)
            )
            triggerBackgroundFlush(uid)
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteReminder(reminderId: String): Result<Unit> = withWriteGuard("del_rem_$reminderId") {
        var docPath = "users/unauthenticated/reminders/$reminderId"
        try {
            val uid = requireUserId()
            docPath = "users/$uid/reminders/$reminderId"
            localDb.reminderDao().deleteById(uid, reminderId)
            reminderScheduler.cancelReminder(reminderId)
            syncEngine.enqueueOperation(uid, "reminders", reminderId, FirestoreSyncEngine.OP_DELETE)
            triggerBackgroundFlush(uid)
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    /**
     * Persists ONLY genuinely newly unlocked achievements (checked against local Room cache)
     * using the outbox WriteBatch — ZERO Firestore `.get()` reads!
     */
    suspend fun persistNewlyUnlockedAchievements(
        newlyUnlocked: List<Pair<String, Int>>
    ): Result<Unit> {
        if (newlyUnlocked.isEmpty()) return Result.success(Unit)
        return try {
            val uid = requireUserId()
            val alreadyPersisted = localDb.achievementDao().getUnlockedCodes(uid).toSet()
            val trulyNew = newlyUnlocked.filter { (code, _) -> sanitizeDocId(code) !in alreadyPersisted }
            if (trulyNew.isEmpty()) return Result.success(Unit)

            val entities = mutableListOf<AchievementEntity>()
            for ((code, xpAwarded) in trulyNew) {
                val docId = sanitizeDocId(code)
                entities.add(
                    AchievementEntity(
                        id = docId,
                        userId = uid,
                        code = docId,
                        xpAwarded = xpAwarded,
                        unlocked = true
                    )
                )
                syncEngine.enqueueOperation(
                    userId = uid,
                    collectionName = "achievements",
                    docId = docId,
                    operation = FirestoreSyncEngine.OP_CREATE,
                    fields = mapOf(
                        "userId" to uid,
                        "code" to docId,
                        "xpAwarded" to xpAwarded.toLong(),
                        "unlocked" to true
                    )
                )
            }
            localDb.achievementDao().upsertAll(entities)
            triggerBackgroundFlush(uid)
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "users/achievements")
            Result.failure(e)
        }
    }

    /**
     * Saves user settings locally immediately and enqueues a batched Firestore update
     * with ZERO pre-write `.get()` reads!
     */
    suspend fun saveUserSettings(settings: UserSettings): Result<Unit> = withWriteGuard("save_settings") {
        preferencesManager.applyRemoteSettings(settings)
        var docPath = "users/unauthenticated/settings/preferences"
        try {
            val uid = requireUserId()
            docPath = "users/$uid/settings/preferences"

            val settingsInitialized = syncEngine.isSettingsInitializedRemotely(uid)
            if (settingsInitialized) {
                syncEngine.enqueueOperation(
                    userId = uid,
                    collectionName = "settings",
                    docId = "preferences",
                    operation = FirestoreSyncEngine.OP_UPDATE,
                    fields = mapOf(
                        "themeMode" to settings.themeMode.name,
                        "notificationsEnabled" to settings.notificationsEnabled,
                        "currencySymbol" to settings.currencySymbol,
                        "monthlyBudget" to settings.monthlyBudget,
                        "separateTripEventLedger" to settings.separateTripEventLedger
                    )
                )
            } else {
                syncEngine.enqueueOperation(
                    userId = uid,
                    collectionName = "settings",
                    docId = "preferences",
                    operation = FirestoreSyncEngine.OP_CREATE,
                    fields = mapOf(
                        "userId" to uid,
                        "themeMode" to settings.themeMode.name,
                        "notificationsEnabled" to settings.notificationsEnabled,
                        "currencySymbol" to settings.currencySymbol,
                        "monthlyBudget" to settings.monthlyBudget,
                        "separateTripEventLedger" to settings.separateTripEventLedger
                    )
                )
                syncEngine.markSettingsInitializedRemotely(uid)
            }

            // Keep profile budget/currency synced in the same WriteBatch (0 pre-write reads!)
            val existingProfile = localDb.profileDao().getProfileOnce(uid)
            if (existingProfile != null) {
                localDb.profileDao().upsert(
                    existingProfile.copy(
                        monthlyBudget = settings.monthlyBudget,
                        currencySymbol = settings.currencySymbol,
                        updatedAtMs = System.currentTimeMillis()
                    )
                )
                syncEngine.enqueueOperation(
                    userId = uid,
                    collectionName = "profile",
                    docId = "main",
                    operation = FirestoreSyncEngine.OP_UPDATE,
                    fields = mapOf(
                        "monthlyBudget" to settings.monthlyBudget,
                        "currencySymbol" to settings.currencySymbol
                    )
                )
            }
            triggerBackgroundFlush(uid)
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    fun clearSessionCaches() {
        syncEngine.clearMemoryCursors()
    }

    /**
     * Completely wipes all account-specific local caches, Room databases (LifeHub + TripSplit),
     * preferences, scheduled financial reminder notifications, and temporary export/share files.
     * Prevents cross-account data exposure after logout or account switch.
     */
    suspend fun wipeAllLocalAccountData(context: Context) {
        withContext(Dispatchers.IO) {
            syncEngine.clearMemoryCursors()
            reminderScheduler.cancelAllReminders()
            runCatching { localDb.clearAllTables() }
            runCatching { TripSplitDatabase.getInstance(context).tripSplitDao().clearAllData() }
            runCatching { TripSplitPreferences.getInstance(context).clearAll() }
            runCatching { preferencesManager.clearUserPreferences() }
            ExportAndShareHelper.clearExportAndShareCaches(context)
        }
    }

    /**
     * Guarantees strict account isolation if the signed-in Firebase UID changes without a prior
     * manual sign-out tap (e.g., OS credential switch or re-authentication under another account).
     */
    suspend fun ensureAccountIsolation(context: Context, currentUid: String) {
        val lastUid = preferencesManager.getLastSignedInUid()
        if (lastUid != null && lastUid != currentUid) {
            wipeAllLocalAccountData(context)
        }
        preferencesManager.setLastSignedInUid(currentUid)
    }

    private fun sanitizeColorHex(raw: String): String {
        val trimmed = raw.trim()
        return if (Regex("^#[0-9a-fA-F]{6,8}$").matches(trimmed)) trimmed else "#00E59B"
    }

    private fun sanitizeDocId(raw: String): String {
        val cleaned = raw.replace(Regex("[^a-zA-Z0-9_\\-]"), "").take(64)
        return cleaned.ifBlank { UUID.randomUUID().toString().replace("-", "") }
    }

    companion object {
        const val TRANSACTION_PAGE_SIZE = 50L
    }
}

class DuplicateWriteIgnoredException : IllegalStateException("Write already in progress")
