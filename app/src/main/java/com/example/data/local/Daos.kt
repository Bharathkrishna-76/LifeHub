package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ProfileDao {
    @Query("SELECT * FROM user_profiles WHERE userId = :userId LIMIT 1")
    fun observeProfile(userId: String): Flow<UserProfileEntity?>

    @Query("SELECT * FROM user_profiles WHERE userId = :userId LIMIT 1")
    suspend fun getProfileOnce(userId: String): UserProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(profile: UserProfileEntity)
}

@Dao
interface TransactionDao {
    @Query(
        "SELECT * FROM transactions WHERE userId = :userId AND monthKey = :monthKey ORDER BY timestampEpochMs DESC"
    )
    fun observeByMonth(userId: String, monthKey: String): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE userId = :userId ORDER BY timestampEpochMs DESC")
    fun observeAllForUser(userId: String): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE userId = :userId ORDER BY timestampEpochMs DESC")
    suspend fun getAllCachedForUser(userId: String): List<TransactionEntity>

    @Query("SELECT * FROM transactions WHERE userId = :userId AND id = :id LIMIT 1")
    suspend fun getById(userId: String, id: String): TransactionEntity?

    @Query("SELECT MIN(timestampEpochMs) FROM transactions WHERE userId = :userId AND monthKey = :monthKey")
    suspend fun getOldestTimestampForMonth(userId: String, monthKey: String): Long?

    @Query(
        "SELECT * FROM transactions WHERE userId = :userId AND monthKey = :monthKey ORDER BY timestampEpochMs DESC"
    )
    suspend fun getCachedForMonth(userId: String, monthKey: String): List<TransactionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<TransactionEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: TransactionEntity)

    @Query("DELETE FROM transactions WHERE userId = :userId AND id = :id")
    suspend fun deleteById(userId: String, id: String)

    @Query("DELETE FROM transactions WHERE userId = :userId AND monthKey = :monthKey")
    suspend fun clearMonth(userId: String, monthKey: String)

    @Query("DELETE FROM transactions WHERE userId = :userId")
    suspend fun clearAllForUser(userId: String)
}

@Dao
interface HabitDao {
    @Query("SELECT * FROM habits WHERE userId = :userId ORDER BY currentStreak DESC, title ASC")
    fun observeAll(userId: String): Flow<List<HabitEntity>>

    @Query("SELECT * FROM habits WHERE userId = :userId")
    suspend fun getAllOnce(userId: String): List<HabitEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<HabitEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: HabitEntity)

    @Query("DELETE FROM habits WHERE userId = :userId AND id = :id")
    suspend fun deleteById(userId: String, id: String)

    @Query("DELETE FROM habits WHERE userId = :userId")
    suspend fun clearAllForUser(userId: String)
}

@Dao
interface GoalDao {
    @Query("SELECT * FROM savings_goals WHERE userId = :userId ORDER BY completed ASC, targetAmount DESC")
    fun observeAll(userId: String): Flow<List<SavingsGoalEntity>>

    @Query("SELECT * FROM savings_goals WHERE userId = :userId")
    suspend fun getAllOnce(userId: String): List<SavingsGoalEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<SavingsGoalEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: SavingsGoalEntity)

    @Query("DELETE FROM savings_goals WHERE userId = :userId AND id = :id")
    suspend fun deleteById(userId: String, id: String)

    @Query("DELETE FROM savings_goals WHERE userId = :userId")
    suspend fun clearAllForUser(userId: String)
}

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks WHERE userId = :userId ORDER BY dueDateEpochMs ASC")
    fun observeAll(userId: String): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE userId = :userId")
    suspend fun getAllOnce(userId: String): List<TaskEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<TaskEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: TaskEntity)

    @Query("DELETE FROM tasks WHERE userId = :userId AND id = :id")
    suspend fun deleteById(userId: String, id: String)

    @Query("DELETE FROM tasks WHERE userId = :userId")
    suspend fun clearAllForUser(userId: String)
}

@Dao
interface ReminderDao {
    @Query("SELECT * FROM reminders WHERE userId = :userId ORDER BY paid ASC, dueDateEpochMs ASC")
    fun observeAll(userId: String): Flow<List<ReminderEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<ReminderEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: ReminderEntity)

    @Query("DELETE FROM reminders WHERE userId = :userId AND id = :id")
    suspend fun deleteById(userId: String, id: String)

    @Query("SELECT * FROM reminders WHERE userId = :userId")
    suspend fun getAllOnce(userId: String): List<ReminderEntity>

    @Query("DELETE FROM reminders WHERE userId = :userId")
    suspend fun clearAllForUser(userId: String)
}

@Dao
interface AchievementDao {
    @Query("SELECT * FROM achievements WHERE userId = :userId")
    fun observeAll(userId: String): Flow<List<AchievementEntity>>

    @Query("SELECT code FROM achievements WHERE userId = :userId AND unlocked = 1")
    suspend fun getUnlockedCodes(userId: String): List<String>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<AchievementEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: AchievementEntity)

    @Query("DELETE FROM achievements WHERE userId = :userId")
    suspend fun clearAllForUser(userId: String)
}

@Dao
interface SyncMetadataDao {
    @Query("SELECT * FROM sync_metadata WHERE userId = :userId AND syncKey = :syncKey LIMIT 1")
    suspend fun get(userId: String, syncKey: String): SyncMetadataEntity?

    @Query("SELECT MAX(lastSyncedWallMs) FROM sync_metadata WHERE userId = :userId")
    fun observeLastSyncTime(userId: String): Flow<Long?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(meta: SyncMetadataEntity)

    @Query("DELETE FROM sync_metadata WHERE userId = :userId")
    suspend fun clearForUser(userId: String)
}

@Dao
interface PendingSyncDao {
    @Query("SELECT COUNT(*) FROM pending_sync_ops WHERE userId = :userId")
    fun observePendingCount(userId: String): Flow<Int>

    @Query("SELECT * FROM pending_sync_ops WHERE userId = :userId ORDER BY createdAtMs ASC")
    suspend fun getPendingForUser(userId: String): List<PendingSyncEntity>

    @Query("SELECT * FROM pending_sync_ops WHERE queueKey = :queueKey LIMIT 1")
    suspend fun getByKey(queueKey: String): PendingSyncEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(op: PendingSyncEntity)

    @Query("DELETE FROM pending_sync_ops WHERE queueKey = :queueKey")
    suspend fun deleteByKey(queueKey: String)

    @Query("DELETE FROM pending_sync_ops WHERE queueKey IN (:keys)")
    suspend fun deleteByKeys(keys: List<String>)

    @Query("DELETE FROM pending_sync_ops WHERE userId = :userId")
    suspend fun clearForUser(userId: String)
}
