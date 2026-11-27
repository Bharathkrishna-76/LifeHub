package com.example.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
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

@Entity(tableName = "user_profiles")
data class UserProfileEntity(
    @PrimaryKey val userId: String,
    val displayName: String,
    val email: String,
    val photoUrl: String,
    val monthlyBudget: Double,
    val currencySymbol: String,
    val totalXp: Int,
    val updatedAtMs: Long
) {
    fun toDomain(): UserProfile = UserProfile(
        userId = userId,
        displayName = displayName,
        email = email,
        photoUrl = photoUrl,
        monthlyBudget = monthlyBudget,
        currencySymbol = currencySymbol,
        totalXp = totalXp
    )
}

@Entity(
    tableName = "transactions",
    indices = [
        Index(value = ["userId", "monthKey", "timestampEpochMs"]),
        Index(value = ["userId", "category"])
    ]
)
data class TransactionEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val title: String,
    val amount: Double,
    val type: String,
    val category: String,
    val monthKey: String,
    val timestampEpochMs: Long,
    val note: String,
    val isTripOrEvent: Boolean = false,
    val eventName: String = ""
) {
    fun toDomain(): TransactionItem = TransactionItem(
        id = id,
        userId = userId,
        title = title,
        amount = amount,
        type = TransactionType.fromRaw(type),
        category = ExpenseCategory.fromRaw(category),
        monthKey = monthKey,
        timestampEpochMs = timestampEpochMs,
        note = note,
        isTripOrEvent = isTripOrEvent,
        eventName = eventName
    )
}

@Entity(
    tableName = "habits",
    indices = [Index(value = ["userId"])]
)
data class HabitEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val title: String,
    val iconKey: String,
    val colorHex: String,
    val targetDaysPerWeek: Int,
    val currentStreak: Int,
    val longestStreak: Int,
    val completedDatesCsv: String
) {
    fun toDomain(): HabitItem = HabitItem(
        id = id,
        userId = userId,
        title = title,
        iconKey = iconKey,
        colorHex = colorHex,
        targetDaysPerWeek = targetDaysPerWeek,
        currentStreak = currentStreak,
        longestStreak = longestStreak,
        completedDates = if (completedDatesCsv.isBlank()) emptyList()
        else completedDatesCsv.split(",").filter { it.isNotBlank() }
    )
}

@Entity(
    tableName = "savings_goals",
    indices = [Index(value = ["userId", "completed"])]
)
data class SavingsGoalEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val title: String,
    val category: String,
    val targetAmount: Double,
    val savedAmount: Double,
    val completed: Boolean,
    val targetMonth: String
) {
    fun toDomain(): SavingsGoalItem = SavingsGoalItem(
        id = id,
        userId = userId,
        title = title,
        category = GoalPreset.fromRaw(category),
        targetAmount = targetAmount,
        savedAmount = savedAmount,
        completed = completed,
        targetMonth = targetMonth
    )
}

@Entity(
    tableName = "tasks",
    indices = [
        Index(value = ["userId", "status"]),
        Index(value = ["userId", "dueDateEpochMs"])
    ]
)
data class TaskEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val title: String,
    val cadence: String,
    val priority: String,
    val status: String,
    val dueDateEpochMs: Long,
    val recurring: Boolean,
    val notes: String
) {
    fun toDomain(): TaskItem = TaskItem(
        id = id,
        userId = userId,
        title = title,
        cadence = TaskCadence.fromRaw(cadence),
        priority = TaskPriority.fromRaw(priority),
        status = TaskStatus.fromRaw(status),
        dueDateEpochMs = dueDateEpochMs,
        recurring = recurring,
        notes = notes
    )
}

@Entity(
    tableName = "reminders",
    indices = [Index(value = ["userId", "dueDateEpochMs"])]
)
data class ReminderEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val title: String,
    val category: String,
    val amount: Double,
    val dueDateEpochMs: Long,
    val recurringMonthly: Boolean,
    val paid: Boolean
) {
    fun toDomain(): ReminderItem = ReminderItem(
        id = id,
        userId = userId,
        title = title,
        category = ReminderCategory.fromRaw(category),
        amount = amount,
        dueDateEpochMs = dueDateEpochMs,
        recurringMonthly = recurringMonthly,
        paid = paid
    )
}

@Entity(
    tableName = "achievements",
    indices = [Index(value = ["userId"])]
)
data class AchievementEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val code: String,
    val xpAwarded: Int,
    val unlocked: Boolean
)

/**
 * Persists per-user Firestore sync cursors, cooldown timestamps, pagination flags,
 * and remote initialization status across app restarts so cold launches avoid full downloads.
 */
@Entity(
    tableName = "sync_metadata",
    primaryKeys = ["userId", "syncKey"],
    indices = [Index(value = ["userId"])]
)
data class SyncMetadataEntity(
    val userId: String,
    val syncKey: String,
    val lastSyncedWallMs: Long = 0L,
    val lastServerUpdatedAtMs: Long = 0L,
    val hasMore: Boolean = false,
    val remoteInitialized: Boolean = false
)

/**
 * Persistent offline-first write outbox scoped strictly by userId.
 * Collapses redundant local mutations on the same document (queueKey = "$userId:$collection:$docId")
 * and flushes to Firestore in WriteBatches whenever connectivity is available.
 */
@Entity(
    tableName = "pending_sync_ops",
    indices = [Index(value = ["userId", "createdAtMs"])]
)
data class PendingSyncEntity(
    @PrimaryKey val queueKey: String,
    val userId: String,
    val collectionName: String,
    val docId: String,
    val operation: String, // "CREATE", "UPDATE", "DELETE"
    val payloadJson: String,
    val createdAtMs: Long,
    val retryCount: Int = 0
)
