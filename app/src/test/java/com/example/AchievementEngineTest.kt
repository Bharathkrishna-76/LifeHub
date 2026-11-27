package com.example

import com.example.domain.engine.AchievementEngine
import com.example.domain.model.ExpenseCategory
import com.example.domain.model.GoalPreset
import com.example.domain.model.HabitItem
import com.example.domain.model.SavingsGoalItem
import com.example.domain.model.TaskCadence
import com.example.domain.model.TaskItem
import com.example.domain.model.TaskPriority
import com.example.domain.model.TaskStatus
import com.example.domain.model.TransactionItem
import com.example.domain.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AchievementEngineTest {

    @Test
    fun `unlocks expected badges and computes XP and level accurately`() {
        val transactions = listOf(
            TransactionItem("t1", "u1", "Coffee", 200.0, TransactionType.EXPENSE, ExpenseCategory.FOOD, "2026-11", 1000L),
            TransactionItem("t2", "u1", "Metro", 100.0, TransactionType.EXPENSE, ExpenseCategory.TRAVEL, "2026-11", 2000L),
            TransactionItem("t3", "u1", "Book", 500.0, TransactionType.EXPENSE, ExpenseCategory.EDUCATION, "2026-11", 3000L)
        )
        val habits = listOf(
            HabitItem("h1", "u1", "Run", "fitness", "#00E59B", 5, 7, 7, emptyList())
        )
        val goals = listOf(
            SavingsGoalItem("g1", "u1", "Phone", GoalPreset.PHONE, 15_000.0, 15_000.0, true, "2027-01")
        )
        val tasks = (1..5).map { idx ->
            TaskItem("k$idx", "u1", "Task $idx", TaskCadence.DAILY, TaskPriority.MEDIUM, TaskStatus.DONE, 0L, false)
        }

        val badges = AchievementEngine.evaluate(
            transactions = transactions,
            habits = habits,
            goals = goals,
            tasks = tasks,
            monthlyBudget = 20_000.0,
            moneyScore = 84
        )

        val unlockedCodes = badges.filter { it.unlocked }.map { it.code }.toSet()
        assertTrue("first_expense" in unlockedCodes)
        assertTrue("streak_7" in unlockedCodes)
        assertTrue("first_goal" in unlockedCodes)
        assertTrue("saved_10k" in unlockedCodes)
        assertTrue("budget_guardian" in unlockedCodes)
        assertTrue("task_crusher" in unlockedCodes)
        assertTrue("money_legend" in unlockedCodes)

        val xp = AchievementEngine.calculateXp(badges)
        assertEquals(100 + 250 + 500 + 600 + 300 + 200 + 500, xp)
        assertEquals(5, AchievementEngine.calculateLevel(xp))
    }
}
