package com.example.data.export

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import androidx.core.content.FileProvider
import com.example.domain.model.MonthlyWrappedSummary
import com.example.domain.model.TransactionItem
import com.example.domain.util.DateAndFormatUtils
import java.io.File
import java.io.FileOutputStream

object ExportAndShareHelper {

    fun shareMonthlyWrappedCard(
        context: Context,
        wrapped: MonthlyWrappedSummary,
        currencySymbol: String
    ): Result<Unit> = runCatching {
        val width = 1080
        val height = 1920
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Vibrant Spotify Wrapped style gradient background
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f, 0f, width.toFloat(), height.toFloat(),
                intArrayOf(
                    Color.parseColor("#0B0D14"),
                    Color.parseColor("#2A1354"),
                    Color.parseColor("#0D3B3E"),
                    Color.parseColor("#090B11")
                ),
                floatArrayOf(0f, 0.35f, 0.72f, 1f),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        // Glowing accent orbs
        val orbMint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(55, 0, 229, 155)
        }
        canvas.drawCircle(920f, 300f, 340f, orbMint)

        val orbCoral = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(50, 255, 92, 138)
        }
        canvas.drawCircle(160f, 1460f, 360f, orbCoral)

        // Header badge
        val badgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(45, 255, 255, 255)
        }
        canvas.drawRoundRect(RectF(80f, 120f, 560f, 200f), 40f, 40f, badgePaint)

        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#00E59B")
            textSize = 36f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        canvas.drawText("LIFEHUB WRAPPED", 120f, 173f, labelPaint)

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 92f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        canvas.drawText(wrapped.monthLabel, 80f, 330f, titlePaint)

        val cardBg = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(180, 18, 22, 34)
        }
        val cardBorder = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 3f
            color = Color.argb(75, 255, 255, 255)
        }
        val statLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#9BA6C1")
            textSize = 34f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        }
        val statValuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 68f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        }

        val stats = listOf(
            "TOTAL SPENDING" to DateAndFormatUtils.formatCurrency(wrapped.totalSpending, currencySymbol),
            "TOTAL SAVINGS" to DateAndFormatUtils.formatCurrency(wrapped.totalSavings, currencySymbol),
            "TOP CATEGORY" to (wrapped.topCategory?.label ?: "None Yet"),
            "LONGEST STREAK" to "${wrapped.longestStreak} Days",
            "MONEY SCORE" to "${wrapped.moneyScore}/100 • ${wrapped.moneyTier}"
        )

        var y = 430f
        stats.forEach { (k, v) ->
            val rect = RectF(80f, y, width - 80f, y + 225f)
            canvas.drawRoundRect(rect, 36f, 36f, cardBg)
            canvas.drawRoundRect(rect, 36f, 36f, cardBorder)
            canvas.drawText(k, 130f, y + 78f, statLabelPaint)
            canvas.drawText(v, 130f, y + 170f, statValuePaint)
            y += 260f
        }

        val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#B8FF3B")
            textSize = 38f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        canvas.drawText(
            "${wrapped.unlockedAchievementCount} Badges Unlocked • ${wrapped.totalXp} XP",
            80f,
            1820f,
            footerPaint
        )

        val dir = File(context.cacheDir, "shared").apply { mkdirs() }
        val file = File(dir, "lifehub_wrapped_${System.currentTimeMillis()}.png")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 95, out)
        }
        bitmap.recycle()

        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, "My ${wrapped.monthLabel} LifeHub Wrapped! Money Score: ${wrapped.moneyScore}/100")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Share Monthly Wrapped"))
    }

    fun exportTransactionsCsv(
        context: Context,
        transactions: List<TransactionItem>,
        currencySymbol: String
    ): Result<Unit> = runCatching {
        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        val file = File(dir, "lifehub_transactions_${System.currentTimeMillis()}.csv")
        val sb = StringBuilder()
        sb.appendLine("Date,Month,Title,Type,Category,Amount ($currencySymbol),Note")
        transactions.forEach { tx ->
            val safeTitle = tx.title.replace("\"", "\"\"")
            val safeNote = tx.note.replace("\"", "\"\"")
            val dateStr = DateAndFormatUtils.dateKeyFromEpoch(tx.timestampEpochMs)
            sb.appendLine("$dateStr,${tx.monthKey},\"$safeTitle\",${tx.type.name},${tx.category.label},${tx.amount},\"$safeNote\"")
        }
        file.writeText(sb.toString())

        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "LifeHub Transactions Export")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Export LifeHub Data"))
    }
}
