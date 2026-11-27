package com.example

import com.example.domain.engine.MoneyScoreCalculator
import com.example.domain.model.ExpenseCategory
import com.example.domain.model.GoalPreset
import com.example.domain.model.HabitItem
import com.example.domain.model.SavingsGoalItem
import com.example.domain.model.TransactionItem
import com.example.domain.model.TransactionType
import com.example.domain.util.DateAndFormatUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MoneyScoreCalculatorTest {

    @Test
    fun `high savings, under budget, strong habits and completed goal earn Elite score`() {
        val last7 = DateAndFormatUtils.lastNDaysKeys(7)
        val transactions = listOf(
            TransactionItem("t1", "u1", "Salary", 100_000.0, TransactionType.INCOME, ExpenseCategory.SALARY, "2026-11", 1000L),
            TransactionItem("t2", "u1", "Rent", 20_000.0, TransactionType.EXPENSE, ExpenseCategory.BILLS, "2026-11", 2000L),
            TransactionItem("t3", "u1", "Groceries", 5_000.0, TransactionType.EXPENSE, ExpenseCategory.FOOD, "2026-11", 3000L)
        )
        val habits = listOf(
            HabitItem("h1", "u1", "Workout", "fitness", "#00E59B", 5, 7, 10, last7)
        )
        val goals = listOf(
            SavingsGoalItem("g1", "u1", "New Laptop", GoalPreset.LAPTOP, 50_000.0, 50_000.0, true, "2027-01")
        )

        val result = MoneyScoreCalculator.calculate(
            transactions = transactions,
            monthlyBudget = 40_000.0,
            habits = habits,
            goals = goals
        )

        assertTrue("Expected score >= 85, got ${result.totalScore}", result.totalScore >= 85)
        assertEquals("Elite", result.tierLabel)
        assertEquals(5, result.factors.size)
    }

    @Test
    fun `overspending with zero income drops budget and savings factors`() {
        val transactions = listOf(
            TransactionItem("t1", "u1", "Shopping Spree", 50_000.0, TransactionType.EXPENSE, ExpenseCategory.SHOPPING, "2026-11", 1000L)
        )

        val result = MoneyScoreCalculator.calculate(
            transactions = transactions,
            monthlyBudget = 20_000.0,
            habits = emptyList(),
            goals = emptyList()
        )

        assertTrue("Expected score < 50, got ${result.totalScore}", result.totalScore < 50)
        assertEquals("Starter", result.tierLabel)
    }
}
