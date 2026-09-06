package com.example.mydailyroutine.di

import android.content.Context
import android.util.Log
import com.example.mydailyroutine.RoutineApplication
import com.example.mydailyroutine.data.local.RoutineDatabase
import com.example.mydailyroutine.data.seed.DemoDataSeeder
import com.example.mydailyroutine.data.preferences.DataStorePreferencesRepository
import com.example.mydailyroutine.data.repository.RoomTimelineRepository
import com.example.mydailyroutine.scheduling.ScheduleAlarmScheduler
import com.example.mydailyroutine.scheduling.ScheduleCoordinator
import com.example.mydailyroutine.scheduling.ScheduleNotifier
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch

/** Small explicit composition root; no service locator inside the domain and no DI reflection. */
class AppGraph(context: Context) {
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val refreshRequests = Channel<Unit>(Channel.CONFLATED)
    val database = RoutineDatabase.create(context)
    val repository = RoomTimelineRepository(database, ::requestRefresh)
    val preferences = DataStorePreferencesRepository(context, ::requestRefresh)
    val exampleData = DemoDataSeeder(context, database, preferences, ::requestRefresh)
    val scheduler = ScheduleAlarmScheduler(context)
    val notifier = ScheduleNotifier(context)
    val coordinator = ScheduleCoordinator(context, database, repository, preferences, scheduler, notifier)

    init {
        notifier.ensureChannels()
        scope.launch {
            for (request in refreshRequests) {
                try { coordinator.refresh() }
                catch (error: CancellationException) { throw error }
                catch (_: Exception) {
                    scheduler.armRetry()
                    Log.w("DailyRoutine", "Local schedule refresh failed; retry armed")
                }
            }
        }
    }

    fun requestRefresh() { refreshRequests.trySend(Unit) }
}

val Context.appGraph: AppGraph get() = (applicationContext as RoutineApplication).graph
