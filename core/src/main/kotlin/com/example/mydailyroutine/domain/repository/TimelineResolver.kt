package com.example.mydailyroutine.domain.repository

import com.example.mydailyroutine.domain.model.*
import com.example.mydailyroutine.domain.routines.RoutineOrigin
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

/** One resolver for the app, planners, reminders and widget, including managed companion timing. */
class TimelineResolver {
    fun resolve(date: LocalDate, snapshot: ScheduleSnapshot): List<ResolvedTimelineItem> = prepare(snapshot).forDate(date)
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
    private val seriesDays = snapshot.routines.filter { it.seriesKey != null && it.parentRoutineId == null }
        .groupBy { it.seriesKey }.mapValues { (_, rows) -> rows.map { it.dayOfWeek }.toSet() }
    private data class Window(val start: LocalDateTime, val end: LocalDateTime, val holiday: String?, val inheritedOverride: Boolean = false)

    private fun rootWindow(base: RoutineBlueprint, origin: LocalDate, includeCancelled: Boolean = false): Window? {
        if (!base.isEnabled || !base.occursOn(origin)) return null
        val exception = overrides[base.id to origin]
        if (!includeCancelled && exception?.isCancelled == true) return null
        val startTime = exception?.customStartTime ?: base.startTime
        val endTime = exception?.customEndTime ?: base.endTime
        val shifted = origin.plusDays((exception?.dayShift ?: 0).toLong())
        val completion = completions[base.id to origin]
        val start = completion?.actualStartedAt ?: shifted.atTime(startTime)
        val end = completion?.actualMinutes?.let { start.plusMinutes(it.toLong()) }
            ?: shifted.plusDays(if (endTime <= startTime) 1 else 0).atTime(endTime)
        val holiday = if (base.category == RoutineCategory.SCHOOL)
            holidays[origin]?.map { it.title }?.distinct()?.sorted()?.joinToString(" · ") else null
        return Window(start, end, holiday, exception != null)
    }
    private fun window(base: RoutineBlueprint, origin: LocalDate, includeCancelled: Boolean = false): Window? {
        if (!base.isEnabled || !base.occursOn(origin)) return null
        if (!includeCancelled && overrides[base.id to origin]?.isCancelled == true) return null
        val parentId = base.parentRoutineId ?: return rootWindow(base,origin,includeCancelled)
        val parent = routinesById[parentId] ?: return null
        if (parent.parentRoutineId != null) return null // Defensive against malformed non-Room inputs/cycles.
        val parentWindow = rootWindow(parent,origin) ?: return null
        val actual = completions[parentId to origin]?.actualTiming
        val start = actual?.endedAt?.atZone(ZoneId.of(actual.zoneId))?.toLocalDateTime() ?: parentWindow.end
        return Window(start,start.plusMinutes(nominalMinutes(base.startTime,base.endTime).toLong()),parentWindow.holiday,parentWindow.inheritedOverride)
    }
    fun cancelledForDate(date: LocalDate): List<CancelledOccurrence> = buildList {
        for (offset in (MAX_OCCURRENCE_SHIFT_DAYS + 8) downTo 0) {
            val origin = date.minusDays(offset.toLong())
            for (exception in cancelledByDate[origin].orEmpty()) {
                val base = routinesById[exception.routineBlockId] ?: continue
                val window = window(base,origin,includeCancelled=true) ?: continue
                if (window.start < date.plusDays(1).atStartOfDay() && window.end > date.atStartOfDay())
                    add(CancelledOccurrence(base.id,origin,exception.customTitle ?: base.title))
            }
        }
    }.sortedWith(compareBy<CancelledOccurrence> { it.occurrenceDate }.thenBy { it.routineBlockId })

    fun forDate(date: LocalDate): List<ResolvedTimelineItem> {
        val dayStart = date.atStartOfDay(); val dayEnd = date.plusDays(1).atStartOfDay()
        val blocks = buildList {
            for (offset in (MAX_OCCURRENCE_SHIFT_DAYS + 8) downTo 0) {
                val origin = date.minusDays(offset.toLong())
                for (base in byWeekday[origin.dayOfWeek].orEmpty()) {
                    val window = window(base,origin) ?: continue
                    if (window.start >= dayEnd || window.end <= dayStart) continue
                    val exception = overrides[base.id to origin]
                    val completion = completions[base.id to origin]
                    add(ResolvedTimelineItem.Block(
                        routineBlockId=base.id,occurrenceDate=origin,date=date,title=exception?.customTitle ?: base.title,
                        category=base.category,subject=subjects[base.subjectId],startsAt=window.start,endsAt=window.end,
                        startMinute=Duration.between(dayStart,maxOf(window.start,dayStart)).toMinutes().toInt(),
                        endMinute=Duration.between(dayStart,minOf(window.end,dayEnd)).toMinutes().toInt(),
                        isNotificationEnabled=base.isNotificationEnabled,isCompleted=completion != null,holidayTitle=window.holiday,
                        hasOverride=exception != null || window.inheritedOverride,isOneOff=base.validFrom != null && base.validFrom==base.validUntil,
                        minDurationMinutes=minOf(base.minDurationMinutes,Duration.between(window.start,window.end).toMinutes().toInt()),
                        elasticity=base.elasticity,priorityWeight=base.priorityWeight,isFixedCommitment=base.isFixedCommitment,
                        completedActualMinutes=completion?.actualMinutes,rawDurationMinutes=base.rawDurationMinutes,
                        topicId=base.topicId,milestoneId=base.milestoneId,reviewId=reviews[base.id]?.id,stageOrder=base.stageOrder,
                        actualTiming=completion?.actualTiming,seriesKey=base.seriesKey,seriesDays=seriesDays[base.seriesKey].orEmpty(),
                        parentRoutineId=base.parentRoutineId,origin=base.origin,
                    ))
                }
            }
        }
        val markers = milestones[date].orEmpty().map {
            ResolvedTimelineItem.Milestone(it.id,it.dueDate,it.title,subjects[it.subjectId],it.dueTime,it.isExam,it.isCompleted,it.estimatedEffortHours,it.isTerminalExam)
        }
        val explicit = blocks.filter { it.parentRoutineId == null && !it.isSuppressed }
        val acceptedCompanions = mutableListOf<ResolvedTimelineItem.Block>()
        val companions = blocks.filter { it.parentRoutineId != null }.sortedWith(
            compareBy<ResolvedTimelineItem.Block> { if(it.origin==RoutineOrigin.MORNING_BUFFER)0 else 1 }.thenBy { it.startMinute }.thenBy { it.key })
            .associate { child ->
                val conflict = !child.isSuppressed && ((explicit + acceptedCompanions).any { other ->
                    other.startsAt < child.endsAt && other.endsAt > child.startsAt
                } || markers.any { it.isExam && !it.isCompleted && it.dueTime != null && it.date.atTime(it.dueTime) >= child.startsAt && it.date.atTime(it.dueTime) < child.endsAt })
                val resolved = child.copy(companionConflict=conflict)
                if (!resolved.isSuppressed) acceptedCompanions += resolved
                child.key to resolved
            }
        return (blocks.map { companions[it.key] ?: it } + markers).sortedWith(
            compareBy<ResolvedTimelineItem> { it.startMinute }.thenBy { if(it is ResolvedTimelineItem.Milestone)0 else 1 }.thenBy { it.key })
    }
}
