package com.example.mydailyroutine.scheduling

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import com.example.mydailyroutine.domain.scheduling.AlarmBatch
import java.time.Instant

/** Only two stable PendingIntent identities: next reminder batch and non-wakeup widget/maintenance. */
class ScheduleAlarmScheduler(private val context: Context) {
    private val manager = context.getSystemService(AlarmManager::class.java)

    fun canScheduleExactAlarms(): Boolean = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || manager.canScheduleExactAlarms()

    fun armNotification(batch: AlarmBatch?) {
        val intent = alarmIntent(ACTION_REMINDER).apply {
            if (batch != null) putExtra(EXTRA_TRIGGER_AT, batch.triggerAt.toEpochMilli())
        }
        val pending = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        if (batch == null) {
            manager.cancel(pending)
            return
        }
        val trigger = batch.triggerAt.toEpochMilli()
        if (canScheduleExactAlarms()) {
            try {
                manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, pending)
                return
            } catch (_: SecurityException) {
                // Permission can be revoked between the check and setExactAndAllowWhileIdle.
            }
        }
        // Explicitly approximate when exact access is unavailable; never crash or request broad USE_EXACT_ALARM.
        manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, pending)
    }

    fun armRefresh(at: Instant?) {
        val pending = PendingIntent.getBroadcast(context, 0, alarmIntent(ACTION_REFRESH),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        if (at == null) manager.cancel(pending)
        else manager.set(AlarmManager.RTC, at.toEpochMilli(), pending) // Non-wakeup, intentionally inexact.
    }

    /** One non-wakeup retry survives a transient storage error or interrupted broadcast. */
    fun armRetry() = armRefresh(Instant.now().plusSeconds(15 * 60))

    private fun alarmIntent(action: String) = Intent(context, AlarmReceiver::class.java)
        .setAction(action).setData(Uri.parse("mydailyroutine://internal/$action"))

    companion object {
        const val ACTION_REMINDER = "com.example.mydailyroutine.REMINDER"
        const val ACTION_REFRESH = "com.example.mydailyroutine.REFRESH"
        const val EXTRA_TRIGGER_AT = "trigger_at_epoch_millis"
    }
}
