package com.example.mydailyroutine.domain.repository

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
    private val cancelledByDate = snapshot.overrides.filter { it.isCancelled }.groupBy { it.overrideDate }
    private val overrides = snapshot.overrides.associateBy { it.routineBlockId to it.overrideDate }
    private val completions = snapshot.completions.map { it.routineBlockId to it.date }.toSet()
    private val holidays = snapshot.calendar.filter { it.isWorkFreeDay }.groupBy { it.date }
    private val milestones = snapshot.milestones.groupBy { it.dueDate }

    /** A cancelled overnight carry-in can be restored from either visible day. */
    fun cancelledForDate(date: LocalDate): List<CancelledOccurrence> = buildList {
        for (origin in listOf(date.minusDays(1), date)) {
            for (exception in cancelledByDate[origin].orEmpty()) {
                val base = routinesById[exception.routineBlockId] ?: continue
                if (!base.occursOn(origin)) continue
                val start = exception.customStartTime ?: base.startTime
                val end = exception.customEndTime ?: base.endTime
                val startsAt = origin.atTime(start)
                val endsAt = origin.plusDays(if (end <= start) 1 else 0).atTime(end)
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
        for (origin in listOf(date.minusDays(1), date)) {
            for (base in byWeekday[origin.dayOfWeek].orEmpty()) {
                if (!base.occursOn(origin)) continue
                val exception = overrides[base.id to origin]
                if (exception?.isCancelled == true) continue
                val startTime = exception?.customStartTime ?: base.startTime
                val endTime = exception?.customEndTime ?: base.endTime
                val start = origin.atTime(startTime)
                val end = origin.plusDays(if (endTime <= startTime) 1 else 0).atTime(endTime)
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
                        isCompleted = (base.id to origin) in completions,
                        holidayTitle = holiday,
                        hasOverride = exception != null,
                        isOneOff = base.validFrom != null && base.validFrom == base.validUntil,
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
                    isCompleted = it.isCompleted,
                ),
            )
        }
    }.sortedWith(
        compareBy<ResolvedTimelineItem> { it.startMinute }
            .thenBy { if (it is ResolvedTimelineItem.Milestone) 0 else 1 }
            .thenBy { it.key },
    )
}
