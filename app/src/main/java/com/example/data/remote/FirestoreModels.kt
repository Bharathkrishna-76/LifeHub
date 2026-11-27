package com.example.data.remote

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@Keep
@IgnoreExtraProperties
data class FirestoreProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "LifeHub Member",
    val email: String? = null,
    val photoUrl: String? = null,
    val monthlyBudget: Double = 25000.0,
    val currencySymbol: String = "₹",
    val totalXp: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class FirestoreTransaction(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val amount: Double = 0.0,
    val type: String = "EXPENSE",
    val category: String = "OTHERS",
    val monthKey: String = "",
    val timestampEpochMs: Long = 0L,
    val note: String? = null,
    val isTripOrEvent: Boolean = false,
    val eventName: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class FirestoreHabit(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val iconKey: String = "flame",
    val colorHex: String = "#00E59B",
    val targetDaysPerWeek: Long = 7L,
    val currentStreak: Long = 0L,
    val longestStreak: Long = 0L,
    val completedDates: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class FirestoreGoal(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val category: String = "CUSTOM",
    val targetAmount: Double = 1000.0,
    val savedAmount: Double = 0.0,
    val completed: Boolean = false,
    val targetMonth: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class FirestoreTask(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val cadence: String = "DAILY",
    val priority: String = "MEDIUM",
    val status: String = "TODO",
    val dueDateEpochMs: Long = 0L,
    val recurring: Boolean = false,
    val notes: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class FirestoreReminder(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val category: String = "BILL",
    val amount: Double = 0.0,
    val dueDateEpochMs: Long = 0L,
    val recurringMonthly: Boolean = true,
    val paid: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class FirestoreAchievement(
    @DocumentId val id: String = "",
    val userId: String = "",
    val code: String = "",
    val xpAwarded: Long = 0L,
    val unlocked: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class FirestoreSettings(
    @DocumentId val id: String = "",
    val userId: String = "",
    val themeMode: String = "DARK",
    val notificationsEnabled: Boolean = true,
    val currencySymbol: String = "₹",
    val monthlyBudget: Double = 25000.0,
    val separateTripEventLedger: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)
