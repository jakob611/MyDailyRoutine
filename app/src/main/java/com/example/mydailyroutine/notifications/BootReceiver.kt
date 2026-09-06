package com.example.mydailyroutine.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.mydailyroutine.data.TimelineRepository
import com.example.mydailyroutine.data.local.RoutineDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action !in setOf(Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_TIME_SET, Intent.ACTION_TIMEZONE_CHANGED)) return
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val database = RoutineDatabase.getInstance(context.applicationContext)
                val repository = TimelineRepository(database)
                repository.initialize()
                ScheduleAlarmScheduler(context.applicationContext, repository).scheduleUpcoming()
            } finally {
                pendingResult.finish()
            }
        }
    }
}
