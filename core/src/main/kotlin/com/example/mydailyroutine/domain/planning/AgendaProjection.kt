package com.example.mydailyroutine.domain.planning

import com.example.mydailyroutine.domain.model.ResolvedTimelineItem
import com.example.mydailyroutine.domain.model.RoutineCategory
import com.example.mydailyroutine.domain.scheduling.OccurrenceTimes
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import kotlin.math.ceil

data class AgendaProjection(val active: ResolvedTimelineItem.Block?, val progress: Float,
    val reserveRemainingMinutes: Int, val upcoming: List<ResolvedTimelineItem.Block>) {
    companion object {
        fun from(items: List<ResolvedTimelineItem>, now: Instant, zone: ZoneId): AgendaProjection {
            val remaining = items.filterIsInstance<ResolvedTimelineItem.Block>().filter {
                !it.isCompleted && !it.isSuppressed && OccurrenceTimes.window(it, zone).end > now
            }.distinctBy { it.occurrenceKey }
            val active = remaining.filter { OccurrenceTimes.window(it, zone).start <= now }
                .sortedWith(compareByDescending<ResolvedTimelineItem.Block> { it.isFixedCommitment }.thenBy { it.startsAt }.thenBy { it.key }).firstOrNull()
            val progress = active?.let {
                val window = OccurrenceTimes.window(it, zone)
                (Duration.between(window.start, now).toMillis().toDouble() / Duration.between(window.start, window.end).toMillis().coerceAtLeast(1)).coerceIn(0.0,1.0).toFloat()
            } ?: 0f
            val reserve = remaining.filter { it.category == RoutineCategory.EMERGENCY_RESERVE }.sumOf {
                val window = OccurrenceTimes.window(it, zone)
                ceil(Duration.between(maxOf(now, window.start), window.end).seconds.coerceAtLeast(0) / 60.0).toInt()
            }
            val next = remaining.filter { OccurrenceTimes.window(it, zone).start > now }
                .sortedWith(compareBy<ResolvedTimelineItem.Block> { it.startsAt }.thenBy { it.key }).take(2)
            return AgendaProjection(active, progress, reserve, next)
        }
    }
}
