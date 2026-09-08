package com.example.mydailyroutine.scheduling

import android.app.AlarmManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.mydailyroutine.app.di.AppGraph
import com.example.mydailyroutine.app.di.appGraph
import java.time.Instant
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ScheduleAlarmScheduler.ACTION_REMINDER -> {
                val epoch = intent.getLongExtra(ScheduleAlarmScheduler.EXTRA_TRIGGER_AT, -1)
                if (epoch < 0) return
                bounded(context) { coordinator.onAlarm(Instant.ofEpochMilli(epoch)) }
            }
            ScheduleAlarmScheduler.ACTION_REFRESH -> bounded(context) { coordinator.refresh() }
        }
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action in supportedActions) bounded(context) { coordinator.refresh() }
    }

    companion object {
        private val supportedActions = setOf(
            Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIME_CHANGED, Intent.ACTION_TIMEZONE_CHANGED,
            AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED,
        )
    }
}

/** Bounded goAsync work, always finished; no foreground service or app-owned CPU wakelock. */
private fun BroadcastReceiver.bounded(context: Context, action: suspend AppGraph.() -> Unit) {
    val pending = goAsync()
    val graph = context.appGraph
    graph.scope.launch {
        try {
            graph.scheduler.armRetry()
            withTimeout(8_000) { action(graph) }
        }
        catch (_: Exception) { Log.w("ScheduleReceiver", "Schedule dispatch interrupted; a non-wakeup retry is armed") }
        finally { pending.finish() }
    }
}
