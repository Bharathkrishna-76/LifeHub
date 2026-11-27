package com.example.tripsplit.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        TripEntity::class,
        TripMemberEntity::class,
        TripExpenseEntity::class,
        WalletEntryEntity::class,
        SettlementEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class TripSplitDatabase : RoomDatabase() {
    abstract fun tripSplitDao(): TripSplitDao

    companion object {
        @Volatile
        private var INSTANCE: TripSplitDatabase? = null

        fun getInstance(context: Context): TripSplitDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    TripSplitDatabase::class.java,
                    "tripsplit_standalone.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
