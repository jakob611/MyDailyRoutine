package com.example.mydailyroutine.domain.routines

import com.example.mydailyroutine.domain.model.*
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.flow.Flow

/** Persistent provenance, not a second category enum: every block still uses the canonical category. */
enum class RoutineOrigin { USER, LESSON_BREAK, SLEEP, MORNING_BUFFER }

object Weekdays {
    const val ALL = 0b1111111
    const val WORKDAYS = 0b0011111
    fun mask(days: Set<DayOfWeek>): Int = days.fold(0) { value, day -> value or (1 shl (day.value - 1)) }
    fun fromMask(mask: Int): Set<DayOfWeek> = DayOfWeek.values().filter { mask and (1 shl (it.value - 1)) != 0 }.toSet()
    fun shifted(bits: Int, days: Int): Int = mask(fromMask(bits).map { it.plus(days.toLong()) }.toSet())
}

/** A valid start edit preserves the last chosen duration; a valid end edit deliberately changes it. */
data class TimeEntryState(val startText: String, val endText: String, val durationMinutes: Int) {
    init { require(durationMinutes in 1..1439) }
    fun withStart(text: String): TimeEntryState {
        val parsed = ScheduleValidation.parseTime(text)
        return copy(startText = text, endText = parsed?.plusMinutes(durationMinutes.toLong())?.format(FORMAT) ?: endText)
    }
    fun withEnd(text: String): TimeEntryState {
        val start = ScheduleValidation.parseTime(startText)
        val end = ScheduleValidation.parseTime(text)
        val duration = if (start != null && end != null && start != end) nominalMinutes(start,end) else durationMinutes
        return copy(endText = text, durationMinutes = duration)
    }
    fun withDuration(minutes: Int): TimeEntryState {
        require(minutes in 1..1439)
        val start = ScheduleValidation.parseTime(startText)
        return copy(durationMinutes = minutes, endText = start?.plusMinutes(minutes.toLong())?.format(FORMAT) ?: endText)
    }
    companion object {
        private val FORMAT = DateTimeFormatter.ofPattern("HH:mm")
        fun at(start: LocalTime, duration: Int): TimeEntryState = TimeEntryState(start.format(FORMAT),start.plusMinutes(duration.toLong()).format(FORMAT),duration)
    }
}

data class EntryDefaults(val lessonDurationMinutes: Int = 45, val lessonBreakMinutes: Int = 5) {
    init { require(lessonDurationMinutes in 1..240 && lessonBreakMinutes in 1..60) }
}
data class SleepSchedule(val enabled: Boolean = false, val bedtime: LocalTime = LocalTime.of(23,0),
    val wakeTime: LocalTime = LocalTime.of(7,0), val weekdaysMask: Int = Weekdays.ALL, val morningBufferMinutes: Int = 30) {
    init {
        ScheduleValidation.times(bedtime,wakeTime)
        require(weekdaysMask in 1..Weekdays.ALL && morningBufferMinutes in 0..120)
    }
    val durationMinutes: Int get() = nominalMinutes(bedtime,wakeTime)
}
data class RoutinePatternRequest(val blueprint: RoutineBlueprint, val weekdays: Set<DayOfWeek>, val weekly: Boolean,
    val afterLessonBreakMinutes: Int = 0, val breakTitle: String = "", val breakNotifications: Boolean = false) {
    init {
        require(weekdays.isNotEmpty() && afterLessonBreakMinutes in 0..60)
        require(afterLessonBreakMinutes == 0 || blueprint.category == RoutineCategory.SCHOOL)
        if (afterLessonBreakMinutes > 0) ScheduleValidation.title(breakTitle)
    }
}
data class RoutinePatternResult(val routineIds: List<Long>, val companionIds: List<Long>)

object RoutinePatternExpander {
    fun expand(request: RoutinePatternRequest, seriesKey: String): List<RoutineBlueprint> {
        val first = requireNotNull(request.blueprint.validFrom)
        val days = if (request.weekly) request.weekdays else setOf(first.dayOfWeek)
        return days.sortedBy { it.value }.map { day -> request.blueprint.copy(id = 0, dayOfWeek = day,
            validFrom = first, validUntil = if (request.weekly) request.blueprint.validUntil else first,
            seriesKey = if (request.weekly) seriesKey else null, parentRoutineId = null, origin = RoutineOrigin.USER, isEnabled = true) }
    }
    fun firstOccurrence(from: LocalDate, days: Set<DayOfWeek>): LocalDate {
        require(days.isNotEmpty())
        return (0L..6L).map(from::plusDays).first { it.dayOfWeek in days }
    }
    fun followingStart(date: LocalDate, start: LocalTime, duration: Int, breakMinutes: Int): LocalDateTime =
        date.atTime(start).plusMinutes(duration.toLong() + breakMinutes)
}
interface RoutinePatternsRepository {
    val sleep: Flow<SleepSchedule>
    suspend fun create(request: RoutinePatternRequest): RoutinePatternResult
    suspend fun editSeries(seriesKey: String, title: String, start: LocalTime, end: LocalTime)
    suspend fun deleteSeries(seriesKey: String)
    suspend fun saveSleep(schedule: SleepSchedule, sleepTitle: String, morningTitle: String)
}
