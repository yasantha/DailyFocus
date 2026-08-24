package com.myday.dailyfocus.service

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.myday.dailyfocus.DailyFocusApplication
import com.myday.dailyfocus.MainActivity
import com.myday.dailyfocus.R
import kotlinx.coroutines.flow.first
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

class DailyResetWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as DailyFocusApplication
        val prefsStore = app.userPrefsStore

        val prefs = prefsStore.userPrefs.first()
        val today = LocalDate.now().toString()
        val yesterday = LocalDate.now().minusDays(1).toString()

        if (prefs.lastCompletedDate.isNotEmpty() &&
            prefs.lastCompletedDate != today &&
            prefs.lastCompletedDate != yesterday
        ) {
            prefsStore.resetStreak()
        }

        showReminderNotification()
        return Result.success()
    }

    private fun showReminderNotification() {
        val context = applicationContext
        val channelId = "daily_focus_reminders"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                context.getString(R.string.notif_daily_reminder_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager = context.getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.notif_daily_reminder_title))
            .setContentText(context.getString(R.string.notif_daily_reminder_text))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
    }

    companion object {
        private const val NOTIFICATION_ID = 1001
        private const val WORK_NAME = "daily_reset_worker"

        fun schedule(context: Context) {
            val now = LocalDateTime.now()
            val nextRun = now.toLocalDate().plusDays(1).atTime(8, 0)
            val initialDelaySeconds = Duration.between(now, nextRun).seconds.coerceAtLeast(60)

            val request = PeriodicWorkRequestBuilder<DailyResetWorker>(24, TimeUnit.HOURS)
                .setInitialDelay(initialDelaySeconds, TimeUnit.SECONDS)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request
            )
        }
    }
}
