package com.example.mydailyroutine.domain.repository

import com.example.mydailyroutine.domain.model.*
import com.example.mydailyroutine.domain.model.CancelledOccurrence
import com.example.mydailyroutine.domain.model.ResolvedTimelineItem
import com.example.mydailyroutine.domain.model.RoutineCategory
import com.example.mydailyroutine.domain.model.ScheduleSnapshot
import java.time.Duration
import java.time.LocalDate

/** Pure recurrence resolution, shared by the app, notifications, widgets, and JVM tests. */
class TimelineResolver {
    fun resolve(date: LocalDate, snapshot: ScheduleSnapshot): List<ResolvedTimelineItem> =
        prepare(snapshot).forDate(date)

    fun prepare(snapshot: ScheduleSnapshot): ResolvedSchedule = ResolvedSchedule(snapshot)
}

class ResolvedSchedule internal constructor(snapshot: ScheduleSnapshot) {
    private val subjects = snapshot.subjects.associateBy { it.id }
    private val byWeekday = snapshot.routines.groupBy { it.dayOfWeek }
    private val routinesById = snapshot.routines.associateBy { it.id }
    private val cancelledByDate = snapshot.overrides.filter { it.isCancelled && it.cancellationReason == CancellationReason.MANUAL }.groupBy { it.overrideDate }
    private val overrides = snapshot.overrides.associateBy { it.routineBlockId to it.overrideDate }
    private val completions = snapshot.completions.associateBy { it.routineBlockId to it.date }
    private val reviews = snapshot.reviews.associateBy { it.timeBlockId }
    private val holidays = snapshot.calendar.filter { it.isWorkFreeDay }.groupBy { it.date }
    private val milestones = snapshot.milestones.groupBy { it.dueDate }

    /** A cancelled overnight carry-in can be restored from either visible day. */
    fun cancelledForDate(date: LocalDate): List<CancelledOccurrence> = buildList {
        for (offset in (MAX_OCCURRENCE_SHIFT_DAYS + 8) downTo 0) {
            val origin = date.minusDays(offset.toLong())
            for (exception in cancelledByDate[origin].orEmpty()) {
                val base = routinesById[exception.routineBlockId] ?: continue
                if (!base.occursOn(origin)) continue
                val start = exception.customStartTime ?: base.startTime
                val end = exception.customEndTime ?: base.endTime
                val shifted = origin.plusDays(exception.dayShift.toLong())
                val startsAt = shifted.atTime(start)
                val endsAt = shifted.plusDays(if (end <= start) 1 else 0).atTime(end)
                if (startsAt < date.plusDays(1).atStartOfDay() && endsAt > date.atStartOfDay()) {
                    add(CancelledOccurrence(base.id, origin, exception.customTitle ?: base.title))
                }
            }
        }
    }.sortedWith(compareBy<CancelledOccurrence> { it.occurrenceDate }.thenBy { it.routineBlockId })

    fun forDate(date: LocalDate): List<ResolvedTimelineItem> = buildList {
        val dayStart = date.atStartOfDay()
        val dayEnd = date.plusDays(1).atStartOfDay()
        // Include the previous day's occurrences so sleep and late study are never lost at midnight.
        for (offset in (MAX_OCCURRENCE_SHIFT_DAYS + 8) downTo 0) {
            val origin = date.minusDays(offset.toLong())
            for (base in byWeekday[origin.dayOfWeek].orEmpty()) {
                if (!base.occursOn(origin)) continue
                val exception = overrides[base.id to origin]
                if (exception?.isCancelled == true) continue
                val startTime = exception?.customStartTime ?: base.startTime
                val endTime = exception?.customEndTime ?: base.endTime
                val shifted = origin.plusDays((exception?.dayShift ?: 0).toLong())
                val completion = completions[base.id to origin]
                val start = completion?.actualStartedAt ?: shifted.atTime(startTime)
                val end = completion?.actualMinutes?.let { start.plusMinutes(it.toLong()) }
                    ?: shifted.plusDays(if (endTime <= startTime) 1 else 0).atTime(endTime)
                if (start >= dayEnd || end <= dayStart) continue
                val holiday = if (base.category == RoutineCategory.SCHOOL) {
                    holidays[origin]?.map { it.title }?.distinct()?.sorted()?.joinToString(" · ")
                } else null
                add(
                    ResolvedTimelineItem.Block(
                        routineBlockId = base.id,
                        occurrenceDate = origin,
                        date = date,
                        title = exception?.customTitle ?: base.title,
                        category = base.category,
                        subject = subjects[base.subjectId],
                        startsAt = start,
                        endsAt = end,
                        startMinute = Duration.between(dayStart, maxOf(start, dayStart)).toMinutes().toInt(),
                        endMinute = Duration.between(dayStart, minOf(end, dayEnd)).toMinutes().toInt(),
                        isNotificationEnabled = base.isNotificationEnabled,
                        isCompleted = completions.containsKey(base.id to origin),
                        holidayTitle = holiday,
                        hasOverride = exception != null,
                        isOneOff = base.validFrom != null && base.validFrom == base.validUntil,
                        minDurationMinutes = minOf(base.minDurationMinutes, Duration.between(start, end).toMinutes().toInt()),
                        elasticity = base.elasticity, priorityWeight = base.priorityWeight, isFixedCommitment = base.isFixedCommitment,
                        completedActualMinutes = completions[base.id to origin]?.actualMinutes,
                        rawDurationMinutes = base.rawDurationMinutes, topicId = base.topicId, milestoneId = base.milestoneId,
                        reviewId = reviews[base.id]?.id, stageOrder = base.stageOrder, actualTiming = completion?.actualTiming,
                    ),
                )
            }
        }
        milestones[date].orEmpty().forEach {
            add(
                ResolvedTimelineItem.Milestone(
                    milestoneId = it.id,
                    date = it.dueDate,
                    title = it.title,
                    subject = subjects[it.subjectId],
                    dueTime = it.dueTime,
                    isExam = it.isExam,
                    isCompleted = it.isCompleted, estimatedEffortHours = it.estimatedEffortHours, isTerminalExam = it.isTerminalExam,
                ),
            )
        }
    }.sortedWith(
        compareBy<ResolvedTimelineItem> { it.startMinute }
            .thenBy { if (it is ResolvedTimelineItem.Milestone) 0 else 1 }
            .thenBy { it.key },
    )
}
