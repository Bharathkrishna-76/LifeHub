package com.example

import com.example.domain.util.DateAndFormatUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DateAndStreakUtilsTest {

    @Test
    fun `month shifting handles year boundaries cleanly`() {
        assertEquals("2027-01", DateAndFormatUtils.shiftMonthKey("2026-12", 1))
        assertEquals("2025-12", DateAndFormatUtils.shiftMonthKey("2026-01", -1))
    }

    @Test
    fun `month bounds produce strictly increasing range`() {
        val (start, end) = DateAndFormatUtils.monthBoundsEpochMs("2026-11")
        assertTrue(end > start)
        val days = (end - start) / 86_400_000L
        assertEquals(30L, days)
    }

    @Test
    fun `streak calculator computes current and longest streaks accurately`() {
        val dates = listOf(
            "2026-11-10", "2026-11-11", "2026-11-12", "2026-11-13",
            "2026-11-23", "2026-11-24", "2026-11-25"
        )
        val (current, longest) = DateAndFormatUtils.calculateStreaks(dates, todayKey = "2026-11-25")
        assertEquals(3, current)
        assertEquals(4, longest)
    }
}
