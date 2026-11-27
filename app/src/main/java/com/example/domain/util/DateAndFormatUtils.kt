package com.example.domain.util

import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.abs
import kotlin.math.roundToLong

object DateAndFormatUtils {

    fun currentMonthKey(nowMs: Long = System.currentTimeMillis()): String {
        val cal = Calendar.getInstance().apply { timeInMillis = nowMs }
        val year = cal.get(Calendar.YEAR)
        val month = cal.get(Calendar.MONTH) + 1
        return String.format(Locale.US, "%04d-%02d", year, month)
    }

    fun currentDateKey(nowMs: Long = System.currentTimeMillis()): String {
        val cal = Calendar.getInstance().apply { timeInMillis = nowMs }
        val year = cal.get(Calendar.YEAR)
        val month = cal.get(Calendar.MONTH) + 1
        val day = cal.get(Calendar.DAY_OF_MONTH)
        return String.format(Locale.US, "%04d-%02d-%02d", year, month, day)
    }

    fun dateKeyFromEpoch(epochMs: Long): String = currentDateKey(epochMs)

    fun monthKeyFromEpoch(epochMs: Long): String = currentMonthKey(epochMs)

    fun shiftMonthKey(monthKey: String, deltaMonths: Int): String {
        val (year, month) = parseMonthKey(monthKey)
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month - 1)
            set(Calendar.DAY_OF_MONTH, 1)
            add(Calendar.MONTH, deltaMonths)
        }
        return String.format(
            Locale.US,
            "%04d-%02d",
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH) + 1
        )
    }

    /**
     * Returns [startInclusiveMs, endExclusiveMs) for the given YYYY-MM month key in local time.
     * Used for single-field indexed Firestore range queries on timestampEpochMs.
     */
    fun monthBoundsEpochMs(monthKey: String): Pair<Long, Long> {
        val (year, month) = parseMonthKey(monthKey)
        val startCal = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month - 1)
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val endCal = (startCal.clone() as Calendar).apply {
            add(Calendar.MONTH, 1)
        }
        return startCal.timeInMillis to endCal.timeInMillis
    }

    fun formatMonthLabel(monthKey: String): String {
        val (year, month) = parseMonthKey(monthKey)
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month - 1)
            set(Calendar.DAY_OF_MONTH, 1)
        }
        return SimpleDateFormat("MMM yyyy", Locale.getDefault()).format(cal.time)
    }

    fun formatShortDate(epochMs: Long): String {
        if (epochMs <= 0L) return "No date"
        return SimpleDateFormat("dd MMM", Locale.getDefault()).format(Date(epochMs))
    }

    fun formatDateTime(epochMs: Long): String {
        if (epochMs <= 0L) return ""
        return SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date(epochMs))
    }

    fun lastNDaysKeys(days: Int, nowMs: Long = System.currentTimeMillis()): List<String> {
        val cal = Calendar.getInstance().apply { timeInMillis = nowMs }
        val result = mutableListOf<String>()
        for (offset in (days - 1) downTo 0) {
            val c = (cal.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -offset) }
            result.add(currentDateKey(c.timeInMillis))
        }
        return result
    }

    fun dayShortLetter(dateKey: String): String {
        val parts = dateKey.split("-")
        if (parts.size != 3) return "?"
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, parts[0].toIntOrNull() ?: 2026)
            set(Calendar.MONTH, (parts[1].toIntOrNull() ?: 1) - 1)
            set(Calendar.DAY_OF_MONTH, parts[2].toIntOrNull() ?: 1)
        }
        return SimpleDateFormat("EEE", Locale.getDefault()).format(cal.time).take(1)
    }

    fun calculateStreaks(completedDates: List<String>, todayKey: String = currentDateKey()): Pair<Int, Int> {
        if (completedDates.isEmpty()) return 0 to 0
        val uniqueDays = completedDates
            .mapNotNull { parseDayIndex(it) }
            .distinct()
            .sorted()
        if (uniqueDays.isEmpty()) return 0 to 0

        var longest = 1
        var run = 1
        for (i in 1 until uniqueDays.size) {
            if (uniqueDays[i] == uniqueDays[i - 1] + 1L) {
                run++
                if (run > longest) longest = run
            } else {
                run = 1
            }
        }

        val todayIdx = parseDayIndex(todayKey) ?: uniqueDays.last()
        val lastCompleted = uniqueDays.last()
        val current = if (lastCompleted == todayIdx || lastCompleted == todayIdx - 1L) {
            var c = 1
            for (i in uniqueDays.lastIndex downTo 1) {
                if (uniqueDays[i] == uniqueDays[i - 1] + 1L) {
                    c++
                } else {
                    break
                }
            }
            c
        } else {
            0
        }
        return current to longest
    }

    private fun parseDayIndex(dateKey: String): Long? {
        val parts = dateKey.split("-")
        if (parts.size != 3) return null
        val y = parts[0].toIntOrNull() ?: return null
        val m = parts[1].toIntOrNull() ?: return null
        val d = parts[2].toIntOrNull() ?: return null
        val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            clear()
            set(y, m - 1, d, 0, 0, 0)
        }
        return cal.timeInMillis / 86_400_000L
    }

    private fun parseMonthKey(monthKey: String): Pair<Int, Int> {
        val parts = monthKey.split("-")
        val year = parts.getOrNull(0)?.toIntOrNull() ?: 2026
        val month = (parts.getOrNull(1)?.toIntOrNull() ?: 1).coerceIn(1, 12)
        return year to month
    }

    fun formatCurrency(amount: Double, symbol: String = "₹", compact: Boolean = false): String {
        val negative = amount < 0
        val absVal = abs(amount)
        val formatted = if (compact && absVal >= 100_000) {
            String.format(Locale.US, "%.1fL", absVal / 100_000.0)
        } else if (compact && absVal >= 1_000) {
            String.format(Locale.US, "%.1fK", absVal / 1_000.0)
        } else {
            val nf = NumberFormat.getNumberInstance(Locale.US).apply {
                maximumFractionDigits = if (absVal % 1.0 == 0.0) 0 else 2
                minimumFractionDigits = 0
            }
            nf.format(absVal)
        }
        return if (negative) "-$symbol$formatted" else "$symbol$formatted"
    }

    fun daysUntil(targetEpochMs: Long, nowMs: Long = System.currentTimeMillis()): Int {
        val diff = targetEpochMs - nowMs
        return (diff.toDouble() / 86_400_000.0).roundToLong().toInt()
    }
}
