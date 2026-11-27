package com.example.tripsplit.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.tripsplit.domain.DestinationCover
import com.example.tripsplit.domain.ExpenseShare
import com.example.tripsplit.domain.SettlementRecord
import com.example.tripsplit.domain.SplitMode
import com.example.tripsplit.domain.Trip
import com.example.tripsplit.domain.TripCategory
import com.example.tripsplit.domain.TripExpense
import com.example.tripsplit.domain.TripMember
import com.example.tripsplit.domain.WalletEntry
import com.example.tripsplit.domain.WalletEntryType

@Entity(tableName = "ts_trips")
data class TripEntity(
    @PrimaryKey val id: String,
    val title: String,
    val destination: String,
    val origin: String,
    val startDateLabel: String,
    val endDateLabel: String,
    val totalBudget: Double,
    val currencySymbol: String,
    val coverName: String,
    val waypointsEncoded: String,
    val createdAt: Long
)

@Entity(
    tableName = "ts_members",
    foreignKeys = [
        ForeignKey(
            entity = TripEntity::class,
            parentColumns = ["id"],
            childColumns = ["tripId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("tripId")]
)
data class TripMemberEntity(
    @PrimaryKey val id: String,
    val tripId: String,
    val name: String,
    val colorIndex: Int,
    val isSelf: Boolean
)

@Entity(
    tableName = "ts_expenses",
    foreignKeys = [
        ForeignKey(
            entity = TripEntity::class,
            parentColumns = ["id"],
            childColumns = ["tripId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("tripId")]
)
data class TripExpenseEntity(
    @PrimaryKey val id: String,
    val tripId: String,
    val title: String,
    val amount: Double,
    val categoryName: String,
    val paidByMemberId: String?,
    val paidFromWallet: Boolean,
    val splitModeName: String,
    val sharesEncoded: String,
    val dateMillis: Long,
    val locationNote: String
)

@Entity(
    tableName = "ts_wallet_entries",
    foreignKeys = [
        ForeignKey(
            entity = TripEntity::class,
            parentColumns = ["id"],
            childColumns = ["tripId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("tripId")]
)
data class WalletEntryEntity(
    @PrimaryKey val id: String,
    val tripId: String,
    val memberId: String,
    val amount: Double,
    val typeName: String,
    val note: String,
    val createdAt: Long
)

@Entity(
    tableName = "ts_settlements",
    foreignKeys = [
        ForeignKey(
            entity = TripEntity::class,
            parentColumns = ["id"],
            childColumns = ["tripId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("tripId")]
)
data class SettlementEntity(
    @PrimaryKey val id: String,
    val tripId: String,
    val fromMemberId: String,
    val toMemberId: String,
    val amount: Double,
    val note: String,
    val createdAt: Long
)

fun encodeShares(shares: List<ExpenseShare>): String =
    shares.joinToString(separator = "|") { "${it.memberId}:${it.amount}" }

fun decodeShares(raw: String): List<ExpenseShare> {
    if (raw.isBlank()) return emptyList()
    return raw.split("|").mapNotNull { token ->
        val parts = token.split(":")
        if (parts.size == 2) {
            val amount = parts[1].toDoubleOrNull()
            if (amount != null) ExpenseShare(memberId = parts[0], amount = amount) else null
        } else {
            null
        }
    }
}

fun TripEntity.toDomain(): Trip = Trip(
    id = id,
    title = title,
    destination = destination,
    origin = origin,
    startDateLabel = startDateLabel,
    endDateLabel = endDateLabel,
    totalBudget = totalBudget,
    currencySymbol = currencySymbol,
    cover = DestinationCover.fromName(coverName),
    waypoints = waypointsEncoded.split("|").map { it.trim() }.filter { it.isNotEmpty() },
    createdAt = createdAt
)

fun Trip.toEntity(): TripEntity = TripEntity(
    id = id,
    title = title,
    destination = destination,
    origin = origin,
    startDateLabel = startDateLabel,
    endDateLabel = endDateLabel,
    totalBudget = totalBudget,
    currencySymbol = currencySymbol,
    coverName = cover.name,
    waypointsEncoded = waypoints.joinToString("|"),
    createdAt = createdAt
)

fun TripMemberEntity.toDomain(): TripMember = TripMember(
    id = id,
    tripId = tripId,
    name = name,
    colorIndex = colorIndex,
    isSelf = isSelf
)

fun TripMember.toEntity(): TripMemberEntity = TripMemberEntity(
    id = id,
    tripId = tripId,
    name = name,
    colorIndex = colorIndex,
    isSelf = isSelf
)

fun TripExpenseEntity.toDomain(): TripExpense = TripExpense(
    id = id,
    tripId = tripId,
    title = title,
    amount = amount,
    category = TripCategory.fromName(categoryName),
    paidByMemberId = paidByMemberId,
    paidFromWallet = paidFromWallet,
    splitMode = SplitMode.fromName(splitModeName),
    shares = decodeShares(sharesEncoded),
    dateMillis = dateMillis,
    locationNote = locationNote
)

fun TripExpense.toEntity(): TripExpenseEntity = TripExpenseEntity(
    id = id,
    tripId = tripId,
    title = title,
    amount = amount,
    categoryName = category.name,
    paidByMemberId = paidByMemberId,
    paidFromWallet = paidFromWallet,
    splitModeName = splitMode.name,
    sharesEncoded = encodeShares(shares),
    dateMillis = dateMillis,
    locationNote = locationNote
)

fun WalletEntryEntity.toDomain(): WalletEntry = WalletEntry(
    id = id,
    tripId = tripId,
    memberId = memberId,
    amount = amount,
    type = WalletEntryType.fromName(typeName),
    note = note,
    createdAt = createdAt
)

fun WalletEntry.toEntity(): WalletEntryEntity = WalletEntryEntity(
    id = id,
    tripId = tripId,
    memberId = memberId,
    amount = amount,
    typeName = type.name,
    note = note,
    createdAt = createdAt
)

fun SettlementEntity.toDomain(): SettlementRecord = SettlementRecord(
    id = id,
    tripId = tripId,
    fromMemberId = fromMemberId,
    toMemberId = toMemberId,
    amount = amount,
    note = note,
    createdAt = createdAt
)

fun SettlementRecord.toEntity(): SettlementEntity = SettlementEntity(
    id = id,
    tripId = tripId,
    fromMemberId = fromMemberId,
    toMemberId = toMemberId,
    amount = amount,
    note = note,
    createdAt = createdAt
)
