package com.example.mydailyroutine.domain.execution

import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import com.example.mydailyroutine.domain.planning.PlanningConfig

/** Explicitly started work; no passive monitoring, foreground service or background ticking. */
data class ActiveExecution(val routineBlockId: Long, val occurrenceDate: LocalDate, val startedAt: Instant,
    val expectedEnd: Instant, val lastHealedEnd: Instant, val stoppedAt: Instant? = null) {
    fun elapsedMinutes(now: Instant): Int = (((((stoppedAt ?: now).toEpochMilli() - startedAt.toEpochMilli()).coerceAtLeast(0)) + 59_999) / 60_000)
        .coerceIn(1, 10080).toInt()
}
interface ExecutionRepository {
    val active: Flow<ActiveExecution?>
    suspend fun start(routineId: Long, date: LocalDate, config: PlanningConfig, automatic: Boolean)
    suspend fun synchronize(config: PlanningConfig, automatic: Boolean): Boolean
    suspend fun finish(config: PlanningConfig, automatic: Boolean): Int
    suspend fun record(block: com.example.mydailyroutine.domain.model.ResolvedTimelineItem.Block, actualMinutes: Int, config: PlanningConfig, automatic: Boolean)
    suspend fun cancel()
}
