package com.example.domain.engine

import com.example.domain.model.AchievementItem
import com.example.domain.model.HabitItem
import com.example.domain.model.SavingsGoalItem
import com.example.domain.model.TaskItem
import com.example.domain.model.TaskStatus
import com.example.domain.model.TransactionItem
import com.example.domain.model.TransactionType

object AchievementEngine {

    fun evaluate(
        transactions: List<TransactionItem>,
        habits: List<HabitItem>,
        goals: List<SavingsGoalItem>,
        tasks: List<TaskItem>,
        monthlyBudget: Double,
        moneyScore: Int,
        persistedCodes: Set<String> = emptySet()
    ): List<AchievementItem> {
        val expenseCount = transactions.count { it.type == TransactionType.EXPENSE }
        val totalExpense = transactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
        val totalIncome = transactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
        val totalGoalSaved = goals.sumOf { it.savedAmount }
        val netMonthlySavings = (totalIncome - totalExpense).coerceAtLeast(0.0)
        val effectiveSaved = maxOf(totalGoalSaved, netMonthlySavings)
        val maxStreak = habits.maxOfOrNull { maxOf(it.currentStreak, it.longestStreak) } ?: 0
        val completedGoals = goals.count { it.completed }
        val completedTasks = tasks.count { it.status == TaskStatus.DONE }

        return listOf(
            AchievementItem(
                code = "first_expense",
                title = "First Expense Added",
                description = "Log your very first expense in LifeHub.",
                xpReward = 100,
                unlocked = expenseCount >= 1 || "first_expense" in persistedCodes,
                progressLabel = if (expenseCount >= 1) "Unlocked" else "0 / 1 expense"
            ),
            AchievementItem(
                code = "streak_7",
                title = "7 Day Streak",
                description = "Keep any daily habit alive for 7 straight days.",
                xpReward = 250,
                unlocked = maxStreak >= 7 || "streak_7" in persistedCodes,
                progressLabel = "${maxStreak.coerceAtMost(7)} / 7 days"
            ),
            AchievementItem(
                code = "streak_30",
                title = "30 Day Streak",
                description = "Unstoppable! Hit a 30-day habit streak.",
                xpReward = 750,
                unlocked = maxStreak >= 30 || "streak_30" in persistedCodes,
                progressLabel = "${maxStreak.coerceAtMost(30)} / 30 days"
            ),
            AchievementItem(
                code = "first_goal",
                title = "First Goal Completed",
                description = "Fully fund any savings goal to 100%.",
                xpReward = 500,
                unlocked = completedGoals >= 1 || "first_goal" in persistedCodes,
                progressLabel = "${completedGoals.coerceAtMost(1)} / 1 goal"
            ),
            AchievementItem(
                code = "saved_10k",
                title = "Saved ₹10,000",
                description = "Accumulate ₹10,000 across goals or monthly savings.",
                xpReward = 600,
                unlocked = effectiveSaved >= 10_000.0 || "saved_10k" in persistedCodes,
                progressLabel = "${effectiveSaved.toInt().coerceAtMost(10_000)} / 10,000"
            ),
            AchievementItem(
                code = "budget_guardian",
                title = "Budget Guardian",
                description = "Log expenses while staying under your monthly budget.",
                xpReward = 300,
                unlocked = (expenseCount >= 3 && monthlyBudget > 0.0 && totalExpense <= monthlyBudget) ||
                    "budget_guardian" in persistedCodes,
                progressLabel = if (expenseCount >= 3 && totalExpense <= monthlyBudget) "On Track" else "$expenseCount / 3 logs"
            ),
            AchievementItem(
                code = "task_crusher",
                title = "Task Crusher",
                description = "Move 5 tasks to Done on your Kanban board.",
                xpReward = 200,
                unlocked = completedTasks >= 5 || "task_crusher" in persistedCodes,
                progressLabel = "${completedTasks.coerceAtMost(5)} / 5 done"
            ),
            AchievementItem(
                code = "money_legend",
                title = "Money Legend",
                description = "Reach an Elite Money Score of 80 or above.",
                xpReward = 500,
                unlocked = moneyScore >= 80 || "money_legend" in persistedCodes,
                progressLabel = "$moneyScore / 80 score"
            )
        )
    }

    fun calculateXp(achievements: List<AchievementItem>): Int =
        achievements.filter { it.unlocked }.sumOf { it.xpReward }

    fun calculateLevel(totalXp: Int): Int = (totalXp / 500) + 1

    fun xpIntoCurrentLevel(totalXp: Int): Int = totalXp % 500
}
