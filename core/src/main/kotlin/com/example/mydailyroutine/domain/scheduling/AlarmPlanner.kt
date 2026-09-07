package com.example.mydailyroutine.domain.scheduling

import com.example.mydailyroutine.domain.model.ResolvedTimelineItem
import com.example.mydailyroutine.domain.model.RoutineCategory
import com.example.mydailyroutine.domain.model.ScheduleSnapshot
import com.example.mydailyroutine.domain.repository.TimelineResolver
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

enum class AlarmKind { BLOCK_PREVIEW, RECOVERY_START }

data class PlannedAlarm(
    val block: ResolvedTimelineItem.Block,
    val kind: AlarmKind,
    val triggerAt: Instant,
) {
    val deliveryKey: String get() = "${block.occurrenceKey}:${kind.name}"
}

data class AlarmBatch(val triggerAt: Instant, val alarms: List<PlannedAlarm>)
data class OccurrenceWindow(val start: Instant, val end: Instant)

/**
 * DST policy: ambiguous local times use the first offset. A nonexistent start shifts the entire
 * occurrence forward by the DST gap. Pre-alert subtraction happens on the instant, not wall time.
 */
object OccurrenceTimes {
    fun window(block: ResolvedTimelineItem.Block, zone: ZoneId): OccurrenceWindow {
        val start = block.startsAt.atZone(zone)
        val gapShift = Duration.between(block.startsAt, start.toLocalDateTime())
        val end = block.endsAt.plus(gapShift).atZone(zone).toInstant()
        return OccurrenceWindow(start.toInstant(), end)
    }
}

/** One upcoming notification batch keeps AlarmManager usage O(1), even for a very dense timetable. */
class AlarmPlanner(private val resolver: TimelineResolver = TimelineResolver()) {
    fun forOccurrence(block: ResolvedTimelineItem.Block, zone: ZoneId): PlannedAlarm? {
        if (block.category == RoutineCategory.EMERGENCY_RESERVE || block.isCarryIn || block.isSuppressed || block.isCompleted || !block.isNotificationEnabled) return null
        val window = OccurrenceTimes.window(block, zone)
        if (window.end <= window.start) return null
        val kind = if (block.category == RoutineCategory.REST_BUFFER) AlarmKind.RECOVERY_START else AlarmKind.BLOCK_PREVIEW
        val trigger = if (kind == AlarmKind.RECOVERY_START) window.start else window.start.minusSeconds(5 * 60)
        return PlannedAlarm(block, kind, trigger)
    }

    fun next(snapshot: ScheduleSnapshot, now: Instant, zone: ZoneId): AlarmBatch? {
        val schedule = resolver.prepare(snapshot)
        var date = maxOf(snapshot.from, now.atZone(zone).toLocalDate())
        var earliest: Instant? = null
        val alarms = mutableListOf<PlannedAlarm>()
        while (date <= snapshot.through) {
            schedule.forDate(date).filterIsInstance<ResolvedTimelineItem.Block>().forEach { block ->
                val alarm = forOccurrence(block, zone)
                if (alarm != null && alarm.triggerAt > now) {
                    val previous = earliest
                    when {
                        previous == null || alarm.triggerAt < previous -> {
                            earliest = alarm.triggerAt
                            alarms.clear()
                            alarms += alarm
                        }
                        alarm.triggerAt == previous -> alarms += alarm
                    }
                }
            }
            // Tomorrow's 00:00 pre-alert could occur today at 23:55. Do not stop one day too soon.
            val earliestTomorrow = date.plusDays(1).atStartOfDay(zone).toInstant().minusSeconds(5 * 60)
            if (earliest?.let { earliestTomorrow > it } == true) break
            date = date.plusDays(1)
        }
        return earliest?.let { AlarmBatch(it, alarms.distinctBy { alarm -> alarm.deliveryKey }.sortedBy { alarm -> alarm.deliveryKey }) }
    }

    /** Re-resolve at receipt time: edited, skipped, completed, deleted, or holiday blocks cannot leak stale notifications. */
    fun due(snapshot: ScheduleSnapshot, scheduledFor: Instant, now: Instant, zone: ZoneId): List<PlannedAlarm> {
        if (now < scheduledFor || Duration.between(scheduledFor, now) > MAX_LATENESS) return emptyList()
        val schedule = resolver.prepare(snapshot)
        val date = scheduledFor.atZone(zone).toLocalDate()
        return (-1L..1L).flatMap { offset ->
            schedule.forDate(date.plusDays(offset)).filterIsInstance<ResolvedTimelineItem.Block>().mapNotNull { forOccurrence(it, zone) }
        }.filter { it.triggerAt == scheduledFor && OccurrenceTimes.window(it.block, zone).end > now }
            .distinctBy { it.deliveryKey }.sortedBy { it.deliveryKey }
    }

    /** Widget updates never need to wake a sleeping phone. The next local midnight is also a boundary. */
    fun nextWidgetBoundary(snapshot: ScheduleSnapshot, now: Instant, zone: ZoneId): Instant {
        val date = now.atZone(zone).toLocalDate()
        val midnight = date.plusDays(1).atStartOfDay(zone).toInstant()
        return resolver.resolve(date, snapshot).filterIsInstance<ResolvedTimelineItem.Block>()
            .filter { !it.isSuppressed && !it.isCompleted }
            .flatMap { OccurrenceTimes.window(it, zone).let { window -> listOf(window.start, window.end) } }
            .filter { it > now && it < midnight }.minOrNull() ?: midnight
    }

    companion object { val MAX_LATENESS: Duration = Duration.ofMinutes(10) }
}
