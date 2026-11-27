package com.example.data.worker

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.R
import com.example.domain.model.ReminderItem
import com.example.domain.util.DateAndFormatUtils
import java.util.concurrent.TimeUnit

class ReminderNotificationWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val title = inputData.getString(KEY_TITLE) ?: return Result.success()
        val body = inputData.getString(KEY_BODY) ?: "Upcoming payment reminder in LifeHub."
        val notifId = inputData.getInt(KEY_NOTIF_ID, title.hashCode())

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return Result.success()
        }

        ReminderScheduler.ensureChannel(applicationContext)

        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(applicationContext).notify(notifId, notification)
        return Result.success()
    }

    companion object {
        const val CHANNEL_ID = "lifehub_reminders"
        const val KEY_TITLE = "reminder_title"
        const val KEY_BODY = "reminder_body"
        const val KEY_NOTIF_ID = "reminder_notif_id"
    }
}

class ReminderScheduler(private val context: Context) {

    fun scheduleReminder(reminder: ReminderItem, currencySymbol: String) {
        if (reminder.paid) {
            cancelReminder(reminder.id)
            return
        }
        val delayMs = (reminder.dueDateEpochMs - System.currentTimeMillis()).coerceAtLeast(5_000L)
        val amountStr = DateAndFormatUtils.formatCurrency(reminder.amount, currencySymbol)
        val input = Data.Builder()
            .putString(
                ReminderNotificationWorker.KEY_TITLE,
                "${reminder.category.label} Due: ${reminder.title}"
            )
            .putString(
                ReminderNotificationWorker.KEY_BODY,
                "Amount $amountStr • Due ${DateAndFormatUtils.formatShortDate(reminder.dueDateEpochMs)}"
            )
            .putInt(ReminderNotificationWorker.KEY_NOTIF_ID, reminder.id.hashCode())
            .build()

        val request = OneTimeWorkRequestBuilder<ReminderNotificationWorker>()
            .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
            .setInputData(input)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            uniqueWorkName(reminder.id),
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    fun cancelReminder(reminderId: String) {
        WorkManager.getInstance(context).cancelUniqueWork(uniqueWorkName(reminderId))
    }

    private fun uniqueWorkName(reminderId: String): String = "lifehub_reminder_$reminderId"

    companion object {
        fun ensureChannel(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val manager = context.getSystemService(NotificationManager::class.java) ?: return
                val channel = NotificationChannel(
                    ReminderNotificationWorker.CHANNEL_ID,
                    context.getString(R.string.reminder_channel_name),
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = context.getString(R.string.reminder_channel_desc)
                }
                manager.createNotificationChannel(channel)
            }
        }
    }
}
