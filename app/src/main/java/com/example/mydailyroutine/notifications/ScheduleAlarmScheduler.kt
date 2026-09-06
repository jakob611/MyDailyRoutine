package com.example.mydailyroutine.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.mydailyroutine.data.TimelineRepository
import com.example.mydailyroutine.domain.ResolvedTimelineItem
import com.example.mydailyroutine.domain.RoutineCategory
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

class ScheduleAlarmScheduler(
    private val context: Context,
    private val repository: TimelineRepository,
) {
    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    suspend fun scheduleUpcoming(daysAhead: Long = 7) = withContext(Dispatchers.IO) {
        val today = LocalDate.now()
        for (offset in 0..daysAhead) {
            val date = today.plusDays(offset)
            val items = repository.getTimelineForDate(date).first()
            items.filter {
                    it.kind == com.example.mydailyroutine.domain.TimelineItemKind.ROUTINE &&
                        it.isNotificationEnabled &&
                        !(it.isSchoolDayOff && it.category == RoutineCategory.SCHOOL)
                }.forEach { item ->
                    scheduleItem(date, item)
                }
        }
    }

    fun scheduleItem(date: LocalDate, item: ResolvedTimelineItem) {
        if (item.routineBlockId == null || item.category == null) return
        val start = date.atTime(item.startTime).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val shouldScheduleRecoveryStart = item.category == RoutineCategory.REST_BREAK
        val notificationTime = if (shouldScheduleRecoveryStart) start else start - FIVE_MINUTES_MS
        if (notificationTime < System.currentTimeMillis()) return

        val kind = if (shouldScheduleRecoveryStart) KIND_RECOVERY else KIND_PRE_START
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = ACTION_BLOCK_ALARM
            putExtra(EXTRA_TITLE, item.title)
            putExtra(EXTRA_CATEGORY, item.category.name)
            putExtra(EXTRA_DATE, date.toString())
            putExtra(EXTRA_KIND, kind)
            putExtra(EXTRA_REQUEST_ID, requestCode(item.stableId, kind))
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode(item.stableId, kind),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        try {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, notificationTime, pendingIntent)
        } catch (_: SecurityException) {
            // Android 12+ can revoke exact-alarm access. A non-exact idle alarm is still useful
            // and avoids a crash; the user can grant SCHEDULE_EXACT_ALARM later.
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, notificationTime, pendingIntent)
        }
    }

    fun cancelItem(stableId: String) {
        listOf(KIND_PRE_START, KIND_RECOVERY).forEach { kind ->
            val intent = Intent(context, AlarmReceiver::class.java).apply { action = ACTION_BLOCK_ALARM }
            PendingIntent.getBroadcast(
                context,
                requestCode(stableId, kind),
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
            )?.let { pending ->
                alarmManager.cancel(pending)
                pending.cancel()
            }
        }
    }

    private fun requestCode(stableId: String, kind: String): Int =
        ("$stableId:$kind".hashCode() and Int.MAX_VALUE).coerceAtLeast(1)

    companion object {
        const val ACTION_BLOCK_ALARM = "com.example.mydailyroutine.action.BLOCK_ALARM"
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_CATEGORY = "extra_category"
        const val EXTRA_DATE = "extra_date"
        const val EXTRA_KIND = "extra_kind"
        const val EXTRA_REQUEST_ID = "extra_request_id"
        const val KIND_PRE_START = "pre_start"
        const val KIND_RECOVERY = "recovery"
        private const val FIVE_MINUTES_MS = 5 * 60 * 1000L
    }
}
