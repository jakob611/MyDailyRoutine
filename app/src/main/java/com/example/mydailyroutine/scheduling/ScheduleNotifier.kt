package com.example.mydailyroutine.scheduling

import android.annotation.SuppressLint
import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.mydailyroutine.MainActivity
import com.example.mydailyroutine.R
import com.example.mydailyroutine.domain.model.SchedulePreferences
import com.example.mydailyroutine.domain.scheduling.AlarmKind
import com.example.mydailyroutine.domain.scheduling.OccurrenceTimes
import com.example.mydailyroutine.domain.scheduling.PlannedAlarm
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class ScheduleNotifier(private val context: Context) {
    private val manager = NotificationManagerCompat.from(context)

    fun ensureChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val normal = NotificationChannel(NORMAL_CHANNEL, context.getString(R.string.notification_channel_normal), NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = context.getString(R.string.notification_channel_normal_description)
                enableVibration(true)
                lockscreenVisibility = NotificationCompat.VISIBILITY_PRIVATE
            }
            val quiet = NotificationChannel(QUIET_CHANNEL, context.getString(R.string.notification_channel_quiet), NotificationManager.IMPORTANCE_LOW).apply {
                description = context.getString(R.string.notification_channel_quiet_description)
                setSound(null, null)
                enableVibration(false)
                lockscreenVisibility = NotificationCompat.VISIBILITY_PRIVATE
            }
            context.getSystemService(NotificationManager::class.java).createNotificationChannels(listOf(normal, quiet))
        }
    }

    fun canNotify(): Boolean {
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return false
        if (!manager.areNotificationsEnabled()) return false
        if (Build.VERSION.SDK_INT >= 26) {
            val platform = context.getSystemService(NotificationManager::class.java)
            return listOf(NORMAL_CHANNEL, QUIET_CHANNEL).any { platform.getNotificationChannel(it)?.importance != NotificationManager.IMPORTANCE_NONE }
        }
        return true
    }

    @SuppressLint("MissingPermission") // canNotify checks POST_NOTIFICATIONS; a revocation race is caught below.
    fun post(alarm: PlannedAlarm, preferences: SchedulePreferences, now: Instant, zone: ZoneId) {
        if (!canNotify()) return
        val quiet = preferences.isQuietAt(now.atZone(zone).toLocalTime())
        val window = OccurrenceTimes.window(alarm.block, zone)
        val startLabel = window.start.atZone(zone).format(DateTimeFormatter.ofPattern("HH:mm"))
        val text = if (alarm.kind == AlarmKind.RECOVERY_START) {
            if (now >= window.start.plusSeconds(60)) context.getString(R.string.notification_recovery_late, startLabel)
            else context.getString(R.string.notification_recovery_now)
        } else context.getString(if (now >= window.start) R.string.notification_started else R.string.notification_starts,
            startLabel, alarm.block.subject?.name ?: context.getString(R.string.notification_subject_fallback))
        val intent = MainActivity.openDayIntent(context, alarm.block.occurrenceDate).apply {
            data = android.net.Uri.parse("mydailyroutine://day/${alarm.block.occurrenceDate}/${alarm.deliveryKey}")
        }
        val contentIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val notification = NotificationCompat.Builder(context, if (quiet) QUIET_CHANNEL else NORMAL_CHANNEL)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(alarm.block.title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .setOnlyAlertOnce(true)
            .setWhen(window.start.toEpochMilli())
            .setShowWhen(true)
            .setTimeoutAfter(Duration.between(now, window.end).toMillis().coerceAtLeast(1))
            .setPriority(if (quiet) NotificationCompat.PRIORITY_LOW else NotificationCompat.PRIORITY_DEFAULT)
            .apply {
                if (quiet) { setSilent(true); setSound(null); setVibrate(longArrayOf(0L)) }
                else setDefaults(NotificationCompat.DEFAULT_SOUND or NotificationCompat.DEFAULT_VIBRATE)
            }.build()
        try {
            // Unique tag, not hashCode/requestCode: unrelated occurrences cannot collide.
            manager.notify(alarm.deliveryKey, 0, notification)
        } catch (_: SecurityException) {
            // Runtime notification permission was revoked after the eligibility check.
        }
    }

    companion object {
        const val NORMAL_CHANNEL = "routine_reminders_v1"
        const val QUIET_CHANNEL = "routine_school_quiet_v1"
    }
}
