package com.example.domain.engine

import com.example.domain.model.ExpenseCategory
import com.example.domain.model.HabitItem
import com.example.domain.model.MoneyScoreBreakdown
import com.example.domain.model.MoneyScoreFactor
import com.example.domain.model.SavingsGoalItem
import com.example.domain.model.TransactionItem
import com.example.domain.model.TransactionType
import com.example.domain.util.DateAndFormatUtils
import kotlin.math.roundToInt

/**
 * Deterministic 0-100 Money Score engine combining:
 * 1. Savings Percentage (0..25)
 * 2. Budget Adherence (0..25)
 * 3. Habit Consistency (0..20)
 * 4. Goal Completion (0..15)
 * 5. Expense Control (0..15)
 */
object MoneyScoreCalculator {

    fun calculate(
        transactions: List<TransactionItem>,
        monthlyBudget: Double,
        habits: List<HabitItem>,
        goals: List<SavingsGoalItem>,
        todayKey: String = DateAndFormatUtils.currentDateKey()
    ): MoneyScoreBreakdown {
        val totalIncome = transactions
            .filter { it.type == TransactionType.INCOME }
            .sumOf { it.amount }
        val totalExpense = transactions
            .filter { it.type == TransactionType.EXPENSE }
            .sumOf { it.amount }
        val netSavings = (totalIncome - totalExpense).coerceAtLeast(0.0)

        // 1. Savings Percentage (0..25): 30%+ savings rate earns full 25 pts
        val savingsRate = if (totalIncome > 0.0) (netSavings / totalIncome).coerceIn(0.0, 1.0) else 0.0
        val savingsScore = if (totalIncome == 0.0 && totalExpense == 0.0) {
            10
        } else {
            ((savingsRate / 0.30).coerceAtMost(1.0) * 25.0).roundToInt()
        }
        val savingsInsight = when {
            totalIncome == 0.0 -> "Add monthly income to unlock your savings ratio."
            savingsRate >= 0.30 -> "Saving ${(savingsRate * 100).roundToInt()}% of income — elite pace!"
            savingsRate >= 0.15 -> "Saving ${(savingsRate * 100).roundToInt()}%. Push toward 30% for max points."
            else -> "Low savings buffer (${(savingsRate * 100).roundToInt()}%). Trim discretionary spends."
        }

        // 2. Budget Adherence (0..25)
        val budgetScore = when {
            monthlyBudget <= 0.0 -> 12
            totalExpense <= monthlyBudget * 0.75 -> 25
            totalExpense <= monthlyBudget -> {
                val ratio = (totalExpense - monthlyBudget * 0.75) / (monthlyBudget * 0.25)
                (25.0 - ratio * 6.0).roundToInt().coerceIn(19, 25)
            }
            else -> {
                val overRatio = ((totalExpense - monthlyBudget) / monthlyBudget).coerceAtMost(1.0)
                (18.0 * (1.0 - overRatio)).roundToInt().coerceIn(0, 18)
            }
        }
        val budgetUsagePct = if (monthlyBudget > 0.0) ((totalExpense / monthlyBudget) * 100).roundToInt() else 0
        val budgetInsight = when {
            monthlyBudget <= 0.0 -> "Set a monthly budget in Profile to track adherence."
            budgetUsagePct <= 80 -> "Using $budgetUsagePct% of budget — great runway."
            budgetUsagePct <= 100 -> "At $budgetUsagePct% of monthly budget — stay alert."
            else -> "Exceeded budget by ${budgetUsagePct - 100}% this month."
        }

        // 3. Habit Consistency (0..20): average 7-day completion rate across habits
        val last7Days = DateAndFormatUtils.lastNDaysKeys(7)
        val habitScore = if (habits.isEmpty()) {
            8
        } else {
            val avgConsistency = habits.map { habit ->
                val doneCount = habit.completedDates.count { it in last7Days || it == todayKey }
                (doneCount.toDouble() / habit.targetDaysPerWeek.coerceAtLeast(1)).coerceAtMost(1.0)
            }.average()
            (avgConsistency * 20.0).roundToInt().coerceIn(0, 20)
        }
        val habitInsight = if (habits.isEmpty()) {
            "Start 1 daily habit to boost consistency points."
        } else {
            val bestStreak = habits.maxOfOrNull { it.currentStreak } ?: 0
            "Active across ${habits.size} habit(s) with a $bestStreak-day streak."
        }

        // 4. Goal Completion (0..15): average progress + completion bonus
        val goalScore = if (goals.isEmpty()) {
            5
        } else {
            val avgProgress = goals.map { it.progressFraction.toDouble() }.average()
            val completedBonus = if (goals.any { it.completed }) 3.0 else 0.0
            (avgProgress * 12.0 + completedBonus).roundToInt().coerceIn(0, 15)
        }
        val goalInsight = if (goals.isEmpty()) {
            "Create a savings goal to earn momentum points."
        } else {
            val avgPct = (goals.map { it.progressFraction }.average() * 100).roundToInt()
            "${goals.count { it.completed }}/${goals.size} completed ($avgPct% avg progress)."
        }

        // 5. Expense Control (0..15): discretionary spend ratio (Shopping + Entertainment + Food)
        val discretionaryCategories = setOf(
            ExpenseCategory.SHOPPING,
            ExpenseCategory.ENTERTAINMENT,
            ExpenseCategory.FOOD
        )
        val discretionarySpend = transactions
            .filter { it.type == TransactionType.EXPENSE && it.category in discretionaryCategories }
            .sumOf { it.amount }
        val discretionaryRatio = if (totalExpense > 0.0) discretionarySpend / totalExpense else 0.0
        val expenseControlScore = when {
            totalExpense == 0.0 -> 10
            discretionaryRatio <= 0.40 -> 15
            discretionaryRatio <= 0.70 -> (15.0 - ((discretionaryRatio - 0.40) / 0.30) * 7.0).roundToInt()
            else -> (8.0 * (1.0 - ((discretionaryRatio - 0.70) / 0.30).coerceAtMost(1.0))).roundToInt().coerceIn(2, 8)
        }
        val expenseControlInsight = if (totalExpense == 0.0) {
            "Log expenses to analyze essential vs lifestyle balance."
        } else {
            "Lifestyle spend is ${(discretionaryRatio * 100).roundToInt()}% of monthly outflow."
        }

        val total = (savingsScore + budgetScore + habitScore + goalScore + expenseControlScore).coerceIn(0, 100)
        val (tierLabel, summary) = when {
            total >= 85 -> "Elite" to "Main-character financial discipline. You're crushing savings & habits."
            total >= 70 -> "Strong" to "Solid momentum across your wallet and daily routines."
            total >= 50 -> "Building" to "Good foundations — dial in budget adherence and habit streaks."
            else -> "Starter" to "Fresh start! Track expenses and check off daily habits to climb fast."
        }

        return MoneyScoreBreakdown(
            totalScore = total,
            tierLabel = tierLabel,
            summary = summary,
            factors = listOf(
                MoneyScoreFactor("Savings Percentage", savingsScore, 25, savingsInsight),
                MoneyScoreFactor("Budget Adherence", budgetScore, 25, budgetInsight),
                MoneyScoreFactor("Habit Consistency", habitScore, 20, habitInsight),
                MoneyScoreFactor("Goal Completion", goalScore, 15, goalInsight),
                MoneyScoreFactor("Expense Control", expenseControlScore, 15, expenseControlInsight)
            )
        )
    }
}
