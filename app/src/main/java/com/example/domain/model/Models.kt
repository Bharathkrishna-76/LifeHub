package com.example.domain.model

import androidx.compose.ui.graphics.Color
import com.example.ui.theme.CatBills
import com.example.ui.theme.CatEducation
import com.example.ui.theme.CatEntertainment
import com.example.ui.theme.CatFood
import com.example.ui.theme.CatHealth
import com.example.ui.theme.CatIncome
import com.example.ui.theme.CatOthers
import com.example.ui.theme.CatShopping
import com.example.ui.theme.CatTravel

enum class ThemeMode {
    SYSTEM, DARK, LIGHT;

    companion object {
        fun fromRaw(raw: String?): ThemeMode =
            entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: DARK
    }
}

enum class TransactionType {
    EXPENSE, INCOME;

    companion object {
        fun fromRaw(raw: String?): TransactionType =
            entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: EXPENSE
    }
}

/**
 * Static local category catalog so category metadata never triggers redundant Firestore reads.
 */
enum class ExpenseCategory(
    val label: String,
    val isIncomeCategory: Boolean,
    val accentColor: Color
) {
    FOOD("Food", false, CatFood),
    TRAVEL("Travel", false, CatTravel),
    SHOPPING("Shopping", false, CatShopping),
    BILLS("Bills", false, CatBills),
    ENTERTAINMENT("Entertainment", false, CatEntertainment),
    HEALTH("Health", false, CatHealth),
    EDUCATION("Education", false, CatEducation),
    OTHERS("Others", false, CatOthers),
    SALARY("Salary", true, CatIncome),
    FREELANCE("Freelance", true, Color(0xFF22D3EE)),
    INVESTMENT("Investment", true, Color(0xFF8B5CF6));

    companion object {
        val expenseCategories: List<ExpenseCategory> = entries.filter { !it.isIncomeCategory }
        val incomeCategories: List<ExpenseCategory> = entries.filter { it.isIncomeCategory }

        fun fromRaw(raw: String?): ExpenseCategory =
            entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: OTHERS
    }
}

enum class TaskCadence(val label: String) {
    DAILY("Daily"),
    WEEKLY("Weekly");

    companion object {
        fun fromRaw(raw: String?): TaskCadence =
            entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: DAILY
    }
}

enum class TaskPriority(val label: String) {
    HIGH("High"),
    MEDIUM("Medium"),
    LOW("Low");

    companion object {
        fun fromRaw(raw: String?): TaskPriority =
            entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: MEDIUM
    }
}

enum class TaskStatus(val label: String) {
    TODO("To Do"),
    IN_PROGRESS("In Progress"),
    DONE("Done");

    companion object {
        fun fromRaw(raw: String?): TaskStatus =
            entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: TODO
    }
}

enum class ReminderCategory(val label: String) {
    BILL("Bill"),
    EMI("EMI"),
    RENT("Rent"),
    SUBSCRIPTION("Subscription");

    companion object {
        fun fromRaw(raw: String?): ReminderCategory =
            entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: BILL
    }
}

enum class GoalPreset(val label: String, val key: String) {
    PHONE("New Phone", "PHONE"),
    VACATION("Vacation", "VACATION"),
    LAPTOP("Laptop", "LAPTOP"),
    BIKE("Bike", "BIKE"),
    EMERGENCY("Emergency Fund", "EMERGENCY"),
    CUSTOM("Custom Goal", "CUSTOM");

    companion object {
        fun fromRaw(raw: String?): GoalPreset =
            entries.firstOrNull { it.key.equals(raw, ignoreCase = true) } ?: CUSTOM
    }
}

data class UserProfile(
    val userId: String = "",
    val displayName: String = "LifeHub Member",
    val email: String = "",
    val photoUrl: String = "",
    val monthlyBudget: Double = 25000.0,
    val currencySymbol: String = "₹",
    val totalXp: Int = 0
)

data class TransactionItem(
    val id: String,
    val userId: String,
    val title: String,
    val amount: Double,
    val type: TransactionType,
    val category: ExpenseCategory,
    val monthKey: String,
    val timestampEpochMs: Long,
    val note: String = "",
    val isTripOrEvent: Boolean = false,
    val eventName: String = ""
)

data class HabitItem(
    val id: String,
    val userId: String,
    val title: String,
    val iconKey: String,
    val colorHex: String,
    val targetDaysPerWeek: Int,
    val currentStreak: Int,
    val longestStreak: Int,
    val completedDates: List<String>
) {
    fun isDoneOn(dateKey: String): Boolean = completedDates.contains(dateKey)
}

data class SavingsGoalItem(
    val id: String,
    val userId: String,
    val title: String,
    val category: GoalPreset,
    val targetAmount: Double,
    val savedAmount: Double,
    val completed: Boolean,
    val targetMonth: String
) {
    val progressFraction: Float
        get() = if (targetAmount <= 0.0) 0f else (savedAmount / targetAmount).toFloat().coerceIn(0f, 1f)
}

data class TaskItem(
    val id: String,
    val userId: String,
    val title: String,
    val cadence: TaskCadence,
    val priority: TaskPriority,
    val status: TaskStatus,
    val dueDateEpochMs: Long,
    val recurring: Boolean,
    val notes: String = ""
)

data class ReminderItem(
    val id: String,
    val userId: String,
    val title: String,
    val category: ReminderCategory,
    val amount: Double,
    val dueDateEpochMs: Long,
    val recurringMonthly: Boolean,
    val paid: Boolean
)

data class AchievementItem(
    val code: String,
    val title: String,
    val description: String,
    val xpReward: Int,
    val unlocked: Boolean,
    val progressLabel: String
)

data class UserSettings(
    val themeMode: ThemeMode = ThemeMode.DARK,
    val notificationsEnabled: Boolean = true,
    val currencySymbol: String = "₹",
    val monthlyBudget: Double = 25000.0,
    val separateTripEventLedger: Boolean = true,
    val parentalControlEnabled: Boolean = false,
    val parentalPinHash: String = ""
)

data class MoneyScoreFactor(
    val name: String,
    val score: Int,
    val maxScore: Int,
    val insight: String
)

data class MoneyScoreBreakdown(
    val totalScore: Int,
    val tierLabel: String,
    val summary: String,
    val factors: List<MoneyScoreFactor>
)

data class MonthlyWrappedSummary(
    val monthLabel: String,
    val totalSpending: Double,
    val totalIncome: Double,
    val totalSavings: Double,
    val topCategory: ExpenseCategory?,
    val topCategoryAmount: Double,
    val bestSavingMonthLabel: String,
    val bestSavingAmount: Double,
    val longestStreak: Int,
    val moneyScore: Int,
    val moneyTier: String,
    val unlockedAchievementCount: Int,
    val totalXp: Int
)

data class MonthlyTrendPoint(
    val monthKey: String,
    val shortLabel: String,
    val income: Double,
    val expense: Double
)

data class YearlyReportSummary(
    val year: Int,
    val totalIncome: Double,
    val totalExpense: Double,
    val netSavings: Double,
    val cachedMonthsCount: Int,
    val transactionCount: Int,
    val topCategory: ExpenseCategory?,
    val topCategoryAmount: Double,
    val categoryTotals: List<Triple<ExpenseCategory, Double, Float>>,
    val monthlyBreakdown: List<MonthlyTrendPoint>
)

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}
