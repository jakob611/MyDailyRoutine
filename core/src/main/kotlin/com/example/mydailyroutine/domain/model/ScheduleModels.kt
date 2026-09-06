package com.example.mydailyroutine.domain.model

import com.example.mydailyroutine.domain.health.HealthConfig
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/** All schedule times are local wall-clock times, at minute precision. */
enum class RoutineCategory { SCHOOL, FOCUS_STUDY, REST_BREAK, PROJECT, PERSONAL }

data class Subject(
    val id: Long = 0,
    val name: String,
    val colorHex: Long,
    val defaultDurationMinutes: Int,
)

data class RoutineBlueprint(
    val id: Long = 0,
    val subjectId: Long?,
    val title: String,
    val category: RoutineCategory,
    val dayOfWeek: DayOfWeek,
    val startTime: LocalTime,
    val endTime: LocalTime,
    val isNotificationEnabled: Boolean,
    // A bounded template supports a genuinely one-off fast-add without fake cancellations.
    val validFrom: LocalDate? = null,
    val validUntil: LocalDate? = null,
) {
    fun occursOn(date: LocalDate): Boolean = date.dayOfWeek == dayOfWeek &&
        (validFrom == null || !date.isBefore(validFrom)) &&
        (validUntil == null || !date.isAfter(validUntil))
}

data class EventOverride(
    val id: Long = 0,
    val routineBlockId: Long,
    val overrideDate: LocalDate,
    val isCancelled: Boolean = false,
    val customStartTime: LocalTime? = null,
    val customEndTime: LocalTime? = null,
    val customTitle: String? = null,
)

data class CalendarEntry(
    val id: Long = 0,
    val date: LocalDate,
    val title: String,
    val isWorkFreeDay: Boolean,
)

data class Milestone(
    val id: Long = 0,
    val subjectId: Long?,
    val title: String,
    val dueDate: LocalDate,
    val dueTime: LocalTime?,
    val isExam: Boolean,
    val isCompleted: Boolean = false,
)

data class OccurrenceCompletion(val routineBlockId: Long, val date: LocalDate)
data class CancelledOccurrence(val routineBlockId: Long, val occurrenceDate: LocalDate, val title: String)

/** A transactionally consistent database read, not a composition of independent DAO emissions. */
data class ScheduleSnapshot(
    val from: LocalDate,
    val through: LocalDate,
    val subjects: List<Subject> = emptyList(),
    val routines: List<RoutineBlueprint> = emptyList(),
    val overrides: List<EventOverride> = emptyList(),
    val calendar: List<CalendarEntry> = emptyList(),
    val milestones: List<Milestone> = emptyList(),
    val completions: List<OccurrenceCompletion> = emptyList(),
)

sealed interface ResolvedTimelineItem {
    val key: String
    val date: LocalDate
    val title: String
    val startMinute: Int
    val startTime: LocalTime
    val subject: Subject?
    val isCompleted: Boolean

    data class Block(
        val routineBlockId: Long,
        /** Ownership date, including for the following day's overnight carry-in. */
        val occurrenceDate: LocalDate,
        override val date: LocalDate,
        override val title: String,
        val category: RoutineCategory,
        override val subject: Subject?,
        val startsAt: LocalDateTime,
        val endsAt: LocalDateTime,
        /** Clipped half-open interval [startMinute, endMinute) on the display date. */
        override val startMinute: Int,
        val endMinute: Int,
        val isNotificationEnabled: Boolean,
        override val isCompleted: Boolean,
        val holidayTitle: String?,
        val hasOverride: Boolean,
        val isOneOff: Boolean,
    ) : ResolvedTimelineItem {
        override val key: String get() = "block:$routineBlockId:$occurrenceDate:$date"
        val occurrenceKey: String get() = "block:$routineBlockId:$occurrenceDate"
        override val startTime: LocalTime get() = LocalTime.ofSecondOfDay(startMinute * 60L)
        val durationMinutes: Int get() = endMinute - startMinute
        val isSuppressed: Boolean get() = category == RoutineCategory.SCHOOL && holidayTitle != null
        val isCarryIn: Boolean get() = occurrenceDate != date
    }

    data class Milestone(
        val milestoneId: Long,
        override val date: LocalDate,
        override val title: String,
        override val subject: Subject?,
        val dueTime: LocalTime?,
        val isExam: Boolean,
        override val isCompleted: Boolean,
    ) : ResolvedTimelineItem {
        override val key: String get() = "milestone:$milestoneId"
        // All-day markers are deliberately sorted at midnight, not assigned a fictitious deadline.
        override val startTime: LocalTime get() = dueTime ?: LocalTime.MIDNIGHT
        override val startMinute: Int get() = startTime.toSecondOfDay() / 60
    }
}

data class SchedulePreferences(
    val muteDuringSchoolHours: Boolean = true,
    val schoolStart: LocalTime = LocalTime.of(7, 45),
    val schoolEnd: LocalTime = LocalTime.of(14, 30),
    val teachingEndDate: LocalDate = LocalDate.of(2027, 6, 24),
    val hapticsEnabled: Boolean = true,
    val health: HealthConfig = HealthConfig(),
) {
    /** Half-open window. Overnight quiet windows are supported; equal endpoints are rejected. */
    fun isQuietAt(time: LocalTime): Boolean = muteDuringSchoolHours && when {
        schoolStart < schoolEnd -> time >= schoolStart && time < schoolEnd
        schoolStart > schoolEnd -> time >= schoolStart || time < schoolEnd
        else -> false
    }
}
