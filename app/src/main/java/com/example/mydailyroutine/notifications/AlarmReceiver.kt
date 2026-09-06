package com.example.mydailyroutine.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.mydailyroutine.MainActivity
import com.example.mydailyroutine.R
import java.time.LocalTime

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ScheduleAlarmScheduler.ACTION_BLOCK_ALARM) return

        val title = intent.getStringExtra(ScheduleAlarmScheduler.EXTRA_TITLE) ?: "Upcoming routine"
        val category = intent.getStringExtra(ScheduleAlarmScheduler.EXTRA_CATEGORY).orEmpty()
        val kind = intent.getStringExtra(ScheduleAlarmScheduler.EXTRA_KIND).orEmpty()
        val muteDuringSchoolHours = NotificationPreferences.muteDuringSchoolHours(context)
        val schoolStart = NotificationPreferences.schoolStartMinutes(context)
        val schoolEnd = NotificationPreferences.schoolEndMinutes(context)
        val quiet = muteDuringSchoolHours && isWithinWindow(LocalTime.now().toSecondOfDay() / 60, schoolStart, schoolEnd)

        createChannels(context)
        val channel = if (quiet) QUIET_CHANNEL_ID else NORMAL_CHANNEL_ID
        val body = if (kind == ScheduleAlarmScheduler.KIND_RECOVERY) {
            "Recovery break starts now"
        } else {
            "Starts in 5 minutes · ${category.replace('_', ' ').lowercase().replaceFirstChar { it.uppercase() }}"
        }
        val openApp = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentIntent = android.app.PendingIntent.getActivity(
            context,
            title.hashCode(),
            openApp,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, channel)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle(title)
            .setContentText(body)
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .setPriority(if (quiet) NotificationCompat.PRIORITY_LOW else NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setSilent(quiet)
            .build()

        if (Build.VERSION.SDK_INT < 33 || context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            NotificationManagerCompat.from(context).notify(intent.getIntExtra(ScheduleAlarmScheduler.EXTRA_REQUEST_ID, title.hashCode()), notification)
        }
    }

    private fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(NORMAL_CHANNEL_ID, "Routine reminders", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "Upcoming blocks and recovery starts"
            },
        )
        manager.createNotificationChannel(
            NotificationChannel(QUIET_CHANNEL_ID, "Quiet school reminders", NotificationManager.IMPORTANCE_LOW).apply {
                description = "Silent reminders during school hours"
                setSound(null, null)
                enableVibration(false)
            },
        )
    }

    private fun isWithinWindow(now: Int, start: Int, end: Int): Boolean =
        if (start <= end) now in start..end else now >= start || now <= end

    companion object {
        private const val NORMAL_CHANNEL_ID = "routine_reminders"
        private const val QUIET_CHANNEL_ID = "quiet_school_reminders"
    }
}
