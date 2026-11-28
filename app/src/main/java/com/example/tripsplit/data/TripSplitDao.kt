package com.example.tripsplit.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TripSplitDao {

    @Query("SELECT * FROM ts_trips ORDER BY createdAt DESC")
    fun observeTrips(): Flow<List<TripEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTrip(trip: TripEntity)

    @Query("DELETE FROM ts_trips WHERE id = :tripId")
    suspend fun deleteTrip(tripId: String)

    @Query("SELECT * FROM ts_members WHERE tripId = :tripId ORDER BY isSelf DESC, name ASC")
    fun observeMembers(tripId: String): Flow<List<TripMemberEntity>>

    @Query("SELECT * FROM ts_members ORDER BY isSelf DESC, name ASC")
    fun observeAllMembers(): Flow<List<TripMemberEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMember(member: TripMemberEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMembers(members: List<TripMemberEntity>)

    @Query("DELETE FROM ts_members WHERE id = :memberId")
    suspend fun deleteMember(memberId: String)

    @Query("SELECT * FROM ts_expenses WHERE tripId = :tripId ORDER BY dateMillis DESC")
    fun observeExpenses(tripId: String): Flow<List<TripExpenseEntity>>

    @Query("SELECT * FROM ts_expenses ORDER BY dateMillis DESC")
    fun observeAllExpenses(): Flow<List<TripExpenseEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertExpense(expense: TripExpenseEntity)

    @Query("DELETE FROM ts_expenses WHERE id = :expenseId")
    suspend fun deleteExpense(expenseId: String)

    @Query("SELECT * FROM ts_wallet_entries WHERE tripId = :tripId ORDER BY createdAt DESC")
    fun observeWalletEntries(tripId: String): Flow<List<WalletEntryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertWalletEntry(entry: WalletEntryEntity)

    @Query("DELETE FROM ts_wallet_entries WHERE id = :entryId")
    suspend fun deleteWalletEntry(entryId: String)

    @Query("SELECT * FROM ts_settlements WHERE tripId = :tripId ORDER BY createdAt DESC")
    fun observeSettlements(tripId: String): Flow<List<SettlementEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSettlement(settlement: SettlementEntity)

    @Query("DELETE FROM ts_settlements WHERE id = :settlementId")
    suspend fun deleteSettlement(settlementId: String)

    @Query("DELETE FROM ts_settlements")
    suspend fun clearSettlements()

    @Query("DELETE FROM ts_wallet_entries")
    suspend fun clearWalletEntries()

    @Query("DELETE FROM ts_expenses")
    suspend fun clearExpenses()

    @Query("DELETE FROM ts_members")
    suspend fun clearMembers()

    @Query("DELETE FROM ts_trips")
    suspend fun clearTrips()

    @androidx.room.Transaction
    suspend fun clearAllData() {
        clearSettlements()
        clearWalletEntries()
        clearExpenses()
        clearMembers()
        clearTrips()
    }
}
