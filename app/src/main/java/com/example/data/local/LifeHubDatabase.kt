package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        UserProfileEntity::class,
        TransactionEntity::class,
        HabitEntity::class,
        SavingsGoalEntity::class,
        TaskEntity::class,
        ReminderEntity::class,
        AchievementEntity::class,
        SyncMetadataEntity::class,
        PendingSyncEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class LifeHubDatabase : RoomDatabase() {
    abstract fun profileDao(): ProfileDao
    abstract fun transactionDao(): TransactionDao
    abstract fun habitDao(): HabitDao
    abstract fun goalDao(): GoalDao
    abstract fun taskDao(): TaskDao
    abstract fun reminderDao(): ReminderDao
    abstract fun achievementDao(): AchievementDao
    abstract fun syncMetadataDao(): SyncMetadataDao
    abstract fun pendingSyncDao(): PendingSyncDao

    companion object {
        @Volatile
        private var instance: LifeHubDatabase? = null

        fun getInstance(context: Context): LifeHubDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    LifeHubDatabase::class.java,
                    "lifehub_offline_cache.db"
                ).fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                    .also { instance = it }
            }
        }
    }
}
