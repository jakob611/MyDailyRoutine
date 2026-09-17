package com.example.mydailyroutine.scheduling

import android.annotation.SuppressLint
import android.Manifest
import android.app.Notification
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
import com.example.mydailyroutine.core.presentation.labelRes
import com.example.mydailyroutine.domain.model.RoutineCategory
import com.example.mydailyroutine.domain.model.SchedulePreferences
import com.example.mydailyroutine.core.presentation.RoutineDate
import com.example.mydailyroutine.domain.scheduling.AlarmKind
import com.example.mydailyroutine.domain.scheduling.OccurrenceTimes
import com.example.mydailyroutine.domain.scheduling.PlannedAlarm
import java.time.Duration
import java.time.Instant
import java.time.ZoneId

class ScheduleNotifier(private val context: Context) {
    private val manager = NotificationManagerCompat.from(context)

    /** The notification accent is the category's own hue, the same one the timeline spine uses. */
    private val categoryAccent = mapOf(
        RoutineCategory.SCHOOL to R.color.routine_cobalt,
        RoutineCategory.FOCUS to R.color.routine_amber,
        RoutineCategory.REST_BUFFER to R.color.routine_sage,
        RoutineCategory.EXAM to R.color.routine_crimson,
        RoutineCategory.PROJECT to R.color.routine_violet,
        RoutineCategory.PERSONAL to R.color.routine_neutral,
    )

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
        val startLabel = RoutineDate.clock(window.start.atZone(zone))
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
            .setSubText(context.getString(alarm.block.category.labelRes()))
            .setColor(ContextCompat.getColor(context, categoryAccent.getValue(alarm.block.category)))
            // One group per day. Without it the shade stacks a pile of near-identical banners,
            // which is exactly how a helpful reminder app starts feeling like spam.
            .setGroup(alarm.block.occurrenceDate.toString())
            .setGroupAlertBehavior(NotificationCompat.GROUP_ALERT_SUMMARY)
            .setSortKey(window.start.toString())
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
            manager.notify("${alarm.block.occurrenceDate}:${alarm.deliveryKey}", 0, notification)
            refreshSummary(alarm.block.occurrenceDate.toString(), quiet, now, zone)
        } catch (_: SecurityException) {
            // Runtime notification permission was revoked after the eligibility check.
        }
    }

    /**
     * Rebuilds the day's summary row from what is actually in the shade, so the collapsed group
     * always lists exactly the blocks still pending. Android composes a group summary itself only
     * as a last resort, as a bare counter; an explicit InboxStyle summary is the documented pattern
     * and the one that reads as designed. Children stay silent (GROUP_ALERT_SUMMARY), so a batch of
     * three blocks pings once, through this row.
     */
    /**
     * Drops previews whose moment has passed — a "starts at 08:00" row still sitting in the shade
     * at 10:00 is noise, not a reminder — and rebuilds the affected day summaries. "Already started"
     * rows keep their own timeout at the end of the block, which is exactly when they go stale.
     * Runs on every coordinator pass: alarm fires, day boundaries, boot, and app resume.
     */
    @SuppressLint("MissingPermission")
    fun prune(now: Instant, zone: ZoneId) {
        val platform = context.getSystemService(NotificationManager::class.java) ?: return
        val cutoff = now.toEpochMilli() - 60_000
        val stale = platform.activeNotifications.filter { entry ->
            (entry.tag?.endsWith(AlarmKind.BLOCK_PREVIEW.name) == true ||
                entry.tag?.endsWith(AlarmKind.RECOVERY_START.name) == true) &&
                entry.notification.`when` in 1..cutoff
        }
        if (stale.isEmpty()) return
        val days = stale.mapNotNull { it.tag?.substringBefore(':') }.distinct()
        stale.forEach { manager.cancel(it.tag, 0) }
        days.forEach { day ->
            // The summary inherits the channel of the children still in the shade, so a quiet day
            // never reacquires a sound through its own summary row.
            val quiet = platform.activeNotifications
                .firstOrNull { it.tag?.startsWith("$day:") == true && it.tag != "$day:summary" }
                ?.notification?.channelId == QUIET_CHANNEL
            refreshSummary(day, quiet, now, zone)
        }
    }

    @SuppressLint("MissingPermission")
    private fun refreshSummary(day: String, quiet: Boolean, now: Instant, zone: ZoneId) {
        val platform = context.getSystemService(NotificationManager::class.java) ?: return
        val active = platform.activeNotifications
            .filter { it.tag == day || (it.tag?.startsWith("$day:") == true) }
            .filterNot { it.tag == "$day:summary" }
            .sortedBy { it.notification.`when` }
        if (active.isEmpty()) {
            manager.cancel("$day:summary", 0)
            return
        }
        val lines = active.map { entry ->
            val title = entry.notification.extras.getString(Notification.EXTRA_TITLE).orEmpty()
            val text = entry.notification.extras.getString(Notification.EXTRA_TEXT).orEmpty()
            val prefix = text.substringBefore(" · ", "")
            if (prefix.isNotEmpty() && prefix != text) "$prefix · $title" else title
        }
        val count = active.size
        val title = context.resources.getQuantityString(R.plurals.notification_group_title, count, count)
        val intent = MainActivity.openDayIntent(context, java.time.LocalDate.parse(day))
        val contentIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val endOfDay = java.time.LocalDate.parse(day).plusDays(1).atStartOfDay(zone).toInstant()
        val style = NotificationCompat.InboxStyle()
            .setBigContentTitle(title)
            .setSummaryText(context.getString(R.string.notification_group_summary))
        lines.take(7).forEach(style::addLine)
        val builder = NotificationCompat.Builder(context, if (quiet) QUIET_CHANNEL else NORMAL_CHANNEL)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(lines.first())
            .setStyle(style)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setColor(ContextCompat.getColor(context, R.color.routine_amber))
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .setOnlyAlertOnce(true)
            .setWhen(now.toEpochMilli())
            .setShowWhen(true)
            .setTimeoutAfter(Duration.between(now, endOfDay).toMillis().coerceAtLeast(1))
            .setGroup(day)
            .setGroupSummary(true)
            .apply {
                if (quiet) { setSilent(true); setSound(null); setVibrate(longArrayOf(0L)) }
            }
        try {
            manager.notify("$day:summary", 0, builder.build())
        } catch (_: SecurityException) {
        }
    }

    companion object {
        const val NORMAL_CHANNEL = "routine_reminders_v1"
        const val QUIET_CHANNEL = "routine_school_quiet_v1"
    }
}
