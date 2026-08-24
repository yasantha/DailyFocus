package com.myday.dailyfocus.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.myday.dailyfocus.MainActivity
import com.myday.dailyfocus.R

/**
 * Keeps a low-priority "timer running" notification alive while a focus/break countdown is
 * active, so the process is protected from background eviction and the user always has a
 * visible, live countdown even if the app itself gets backgrounded or swiped away.
 */
class TimerForegroundService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val endTimeMillis = intent.getLongExtra(EXTRA_END_TIME_MILLIS, System.currentTimeMillis())
                val modeLabel = intent.getStringExtra(EXTRA_MODE_LABEL) ?: MODE_FOCUS
                ensureChannel()
                val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                } else {
                    0
                }
                startForeground(NOTIFICATION_ID, buildRunningNotification(endTimeMillis, modeLabel), type)
            }
            ACTION_STOP -> {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    private fun buildRunningNotification(endTimeMillis: Long, modeLabel: String): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, openAppIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val title = getString(
            when (modeLabel) {
                MODE_BREAK -> R.string.notif_running_break_title
                MODE_LONG_BREAK -> R.string.notif_running_long_break_title
                else -> R.string.notif_running_focus_title
            }
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(getString(R.string.notif_running_text))
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setUsesChronometer(true)
            .setChronometerCountDown(true)
            .setWhen(endTimeMillis)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .build()
    }

    private fun ensureChannel() {
        val manager = getSystemService(NotificationManager::class.java) ?: return
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.notif_channel_running_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = getString(R.string.notif_channel_running_desc)
            setSound(null, null)
        }
        manager.createNotificationChannel(channel)
    }

    companion object {
        const val ACTION_START = "com.myday.dailyfocus.action.START_TIMER"
        const val ACTION_STOP = "com.myday.dailyfocus.action.STOP_TIMER"
        const val EXTRA_END_TIME_MILLIS = "extra_end_time_millis"
        const val EXTRA_MODE_LABEL = "extra_mode_label"
        const val MODE_FOCUS = "FOCUS"
        const val MODE_BREAK = "BREAK"
        const val MODE_LONG_BREAK = "LONG_BREAK"
        private const val CHANNEL_ID = "timer_running"
        private const val NOTIFICATION_ID = 2001
    }
}
