package com.example.mydailyroutine.scheduling

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.util.Log
import androidx.room.withTransaction
import com.example.mydailyroutine.core.database.AlarmDeliveryEntity
import com.example.mydailyroutine.core.database.RoutineDatabase
import com.example.mydailyroutine.domain.repository.PreferencesRepository
import com.example.mydailyroutine.domain.repository.TimelineRepository
import com.example.mydailyroutine.domain.scheduling.AlarmPlanner
import com.example.mydailyroutine.widget.refreshAgendaWidgets
import com.example.mydailyroutine.widget.AgendaWidgetReceiver
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Serializes edits, reboot recovery, alarm dispatch, permission changes, and widget refresh requests. */
class ScheduleCoordinator(
    private val context: Context,
    private val database: RoutineDatabase,
    private val repository: TimelineRepository,
    private val preferences: PreferencesRepository,
    private val scheduler: ScheduleAlarmScheduler,
    private val notifier: ScheduleNotifier,
    private val clock: Clock = Clock.systemUTC(),
) {
    private val mutex = Mutex()
    private val planner = AlarmPlanner()

    suspend fun refresh() = mutex.withLock { refreshLocked() }

    suspend fun onAlarm(scheduledFor: Instant) = mutex.withLock {
        val now = clock.instant()
        val zone = ZoneId.systemDefault() // Re-read after ACTION_TIMEZONE_CHANGED, never cache a device zone.
        val day = scheduledFor.atZone(zone).toLocalDate()
        val prefs = preferences.preferences.first()
        val due = if (notifier.canNotify()) database.withTransaction {
            // Re-read and claim in ONE write transaction, linearizing concurrent skip/delete/completion edits.
            val snapshot = repository.snapshot(day.minusDays(1), day.plusDays(1))
            planner.due(snapshot, scheduledFor, now, zone).filter { alarm ->
                database.alarmDeliveries().claim(AlarmDeliveryEntity(
                    alarm.block.routineBlockId, alarm.block.occurrenceDate, alarm.kind.name, now.toEpochMilli(),
                )) != -1L
            }
        } else emptyList()
        // Arm the next reminder before notification rendering / widget work. Process death cannot break the chain here.
        refreshLocked(updateWidget = false)
        due.forEach { notifier.post(it, prefs, now, zone) }
        updateWidgets()
    }

    private suspend fun refreshLocked(updateWidget: Boolean = true) {
        val now = clock.instant()
        val zone = ZoneId.systemDefault()
        val today = now.atZone(zone).toLocalDate()
        val snapshot = repository.snapshot(today, today.plusDays(370))
        val allowed = notifier.canNotify()
        val next = if (allowed) planner.next(snapshot, now, zone) else null
        scheduler.armNotification(next)
        val widgetPresent = hasWidgets()
        val retryFutureRoutine = allowed && next == null && database.routines().hasUpcomingNotificationRoutines(today)
        scheduler.armRefresh(when {
            widgetPresent -> planner.nextWidgetBoundary(snapshot, now, zone)
            retryFutureRoutine -> today.plusDays(1).atStartOfDay(zone).toInstant()
            else -> null
        })
        database.alarmDeliveries().prune(today.minusDays(30))
        if (updateWidget && widgetPresent) updateWidgets()
    }

    private fun hasWidgets(): Boolean = AppWidgetManager.getInstance(context)
        .getAppWidgetIds(ComponentName(context, AgendaWidgetReceiver::class.java)).isNotEmpty()

    private suspend fun updateWidgets() {
        if (!hasWidgets()) return
        try { refreshAgendaWidgets(context) }
        catch (error: CancellationException) { throw error }
        catch (_: Exception) { Log.w("ScheduleCoordinator", "Widget refresh could not complete; next boundary will retry") }
    }
}
