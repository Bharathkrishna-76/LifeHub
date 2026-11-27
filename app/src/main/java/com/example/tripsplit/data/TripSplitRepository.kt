package com.example.tripsplit.data

import com.example.tripsplit.domain.SettlementRecord
import com.example.tripsplit.domain.Trip
import com.example.tripsplit.domain.TripExpense
import com.example.tripsplit.domain.TripMember
import com.example.tripsplit.domain.WalletEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class TripSplitRepository(private val dao: TripSplitDao) {

    fun observeTrips(): Flow<List<Trip>> =
        dao.observeTrips().map { list -> list.map { it.toDomain() } }

    fun observeAllMembers(): Flow<List<TripMember>> =
        dao.observeAllMembers().map { list -> list.map { it.toDomain() } }

    fun observeAllExpenses(): Flow<List<TripExpense>> =
        dao.observeAllExpenses().map { list -> list.map { it.toDomain() } }

    fun observeMembers(tripId: String): Flow<List<TripMember>> =
        dao.observeMembers(tripId).map { list -> list.map { it.toDomain() } }

    fun observeExpenses(tripId: String): Flow<List<TripExpense>> =
        dao.observeExpenses(tripId).map { list -> list.map { it.toDomain() } }

    fun observeWalletEntries(tripId: String): Flow<List<WalletEntry>> =
        dao.observeWalletEntries(tripId).map { list -> list.map { it.toDomain() } }

    fun observeSettlements(tripId: String): Flow<List<SettlementRecord>> =
        dao.observeSettlements(tripId).map { list -> list.map { it.toDomain() } }

    suspend fun createTripWithMembers(trip: Trip, members: List<TripMember>) {
        dao.upsertTrip(trip.toEntity())
        if (members.isNotEmpty()) {
            dao.upsertMembers(members.map { it.toEntity() })
        }
    }

    suspend fun updateTrip(trip: Trip) {
        dao.upsertTrip(trip.toEntity())
    }

    suspend fun deleteTrip(tripId: String) {
        dao.deleteTrip(tripId)
    }

    suspend fun upsertMember(member: TripMember) {
        dao.upsertMember(member.toEntity())
    }

    suspend fun deleteMember(memberId: String) {
        dao.deleteMember(memberId)
    }

    suspend fun upsertExpense(expense: TripExpense) {
        dao.upsertExpense(expense.toEntity())
    }

    suspend fun deleteExpense(expenseId: String) {
        dao.deleteExpense(expenseId)
    }

    suspend fun upsertWalletEntry(entry: WalletEntry) {
        dao.upsertWalletEntry(entry.toEntity())
    }

    suspend fun deleteWalletEntry(entryId: String) {
        dao.deleteWalletEntry(entryId)
    }

    suspend fun upsertSettlement(settlement: SettlementRecord) {
        dao.upsertSettlement(settlement.toEntity())
    }

    suspend fun deleteSettlement(settlementId: String) {
        dao.deleteSettlement(settlementId)
    }
}
