package com.example.mydailyroutine.domain.execution

import com.example.mydailyroutine.domain.model.ResolvedTimelineItem
import com.example.mydailyroutine.domain.model.RoutineCategory
import com.example.mydailyroutine.domain.planning.PlanningConfig
import com.example.mydailyroutine.domain.scheduling.OccurrenceTimes
import java.time.Instant
import java.time.ZoneId

/** One protection policy shared by start/stop logic, including unallocated school recovery. */
data class ProtectedWindow(val start: Instant, val end: Instant) {
    fun contains(time: Instant): Boolean = time >= start && time < end
}
object ExecutionProtection {
    fun windows(items: List<ResolvedTimelineItem>, config: PlanningConfig, zone: ZoneId, excludedKey: String): List<ProtectedWindow> {
        val blocks = items.filterIsInstance<ResolvedTimelineItem.Block>().filter { !it.isSuppressed && it.occurrenceKey != excludedKey }
            .distinctBy { it.occurrenceKey }
        val fixed = blocks.filter { it.isFixedCommitment || it.category == RoutineCategory.SCHOOL || it.category == RoutineCategory.REST_BUFFER }
            .map { OccurrenceTimes.window(it, zone).let { window -> ProtectedWindow(window.start, window.end) } }
        val recovery = blocks.filter { it.category == RoutineCategory.SCHOOL }.groupBy { OccurrenceTimes.window(it, zone).end.atZone(zone).toLocalDate() }.map { (_, lessons) ->
            val end = lessons.maxOf { OccurrenceTimes.window(it, zone).end }
            ProtectedWindow(end, end.plusSeconds(config.postSchoolRecoveryMinutes * 60L))
        }
        val exams = items.filterIsInstance<ResolvedTimelineItem.Milestone>().filter { (it.isExam || it.isTerminalExam) && !it.isCompleted && it.dueTime != null }
            .distinctBy { it.key }.map { val start = it.date.atTime(it.dueTime).atZone(zone).toInstant(); ProtectedWindow(start, start.plusSeconds(60)) }
        return (fixed + recovery + exams).sortedBy { it.start }
    }
    fun stopBoundary(windows: List<ProtectedWindow>, startedAt: Instant, now: Instant): Instant {
        // A retrospectively inserted conflicting event cannot erase work already performed.
        if (windows.any { it.start < startedAt && it.contains(now) }) return now
        return windows.filter { it.start >= startedAt }.minOfOrNull { it.start } ?: startedAt.plusSeconds(24 * 3600)
    }
}
