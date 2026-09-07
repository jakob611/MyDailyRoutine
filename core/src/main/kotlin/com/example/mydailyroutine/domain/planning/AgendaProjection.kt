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
            val reserveIntervals = remaining.filter { it.category == RoutineCategory.EMERGENCY_RESERVE }.map {
                val window = OccurrenceTimes.window(it, zone)
                maxOf(now, window.start) to window.end
            }.sortedBy { it.first }
            val merged = mutableListOf<Pair<Instant,Instant>>()
            reserveIntervals.forEach { interval ->
                val last = merged.lastOrNull()
                if (last != null && interval.first <= last.second) merged[merged.lastIndex] = last.first to maxOf(last.second, interval.second)
                else merged += interval
            }
            val reserve = merged.sumOf { (start, end) -> ((Duration.between(start, end).toMillis().coerceAtLeast(0) + 59999) / 60000).toInt() }
            val next = remaining.filter { OccurrenceTimes.window(it, zone).start > now }
                .sortedWith(compareBy<ResolvedTimelineItem.Block> { it.startsAt }.thenBy { it.key }).take(2)
            return AgendaProjection(active, progress, reserve, next)
        }
    }
}
