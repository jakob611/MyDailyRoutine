package com.example.mydailyroutine.domain.model

import com.example.mydailyroutine.domain.health.HealthConfig
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/** All schedule times are local wall-clock times, at minute precision. */
enum class RoutineCategory { SCHOOL, FOCUS_ANALYTICAL, FOCUS_SYNTHESIZING, ADMIN, REST_BUFFER, EMERGENCY_RESERVE;
    val isDeepWork: Boolean get() = this == FOCUS_ANALYTICAL || this == FOCUS_SYNTHESIZING
    val isBuffer: Boolean get() = this == REST_BUFFER || this == EMERGENCY_RESERVE
}
fun nominalMinutes(start: LocalTime, end: LocalTime): Int = Math.floorMod(end.toSecondOfDay() / 60 - start.toSecondOfDay() / 60, 1440)
const val MAX_OCCURRENCE_SHIFT_DAYS = 7
enum class CancellationReason { MANUAL, AUTO_HEAL, BACKLOG, BUFFER_CONSUMED }

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
    val minDurationMinutes: Int = if (category == RoutineCategory.SCHOOL) nominalMinutes(startTime, endTime)
        else if (category == RoutineCategory.EMERGENCY_RESERVE) 0 else minOf(25, nominalMinutes(startTime, endTime)),
    val elasticity: Double = if (category == RoutineCategory.SCHOOL) 0.0 else 1.0,
    val priorityWeight: Double = if (category.isDeepWork) 3.0 else 1.0,
    val isFixedCommitment: Boolean = category == RoutineCategory.SCHOOL,
    val completedActualMinutes: Int? = null,
    val rawDurationMinutes: Int = nominalMinutes(startTime, endTime),
    val topicId: Long? = null,
    val milestoneId: Long? = null,
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
    val dayShift: Int = 0,
    val cancellationReason: CancellationReason = CancellationReason.MANUAL,
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
    val estimatedEffortHours: Double = 0.0,
    val isTerminalExam: Boolean = false,
)

data class OccurrenceCompletion(val routineBlockId: Long, val date: LocalDate, val actualMinutes: Int? = null)
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
    val reviews: List<com.example.mydailyroutine.domain.learning.SpacedReview> = emptyList(),
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
        val minDurationMinutes: Int = minOf(25, endMinute - startMinute),
        val elasticity: Double = if (category == RoutineCategory.SCHOOL) 0.0 else 1.0,
        val priorityWeight: Double = 1.0,
        val isFixedCommitment: Boolean = category == RoutineCategory.SCHOOL,
        val completedActualMinutes: Int? = null,
        val rawDurationMinutes: Int = endMinute - startMinute,
        val topicId: Long? = null,
        val milestoneId: Long? = null,
        val reviewId: Long? = null,
    ) : ResolvedTimelineItem {
        override val key: String get() = "block:$routineBlockId:$occurrenceDate:$date"
        val occurrenceKey: String get() = "block:$routineBlockId:$occurrenceDate"
        override val startTime: LocalTime get() = LocalTime.ofSecondOfDay(startMinute * 60L)
        val durationMinutes: Int get() = endMinute - startMinute
        val isSuppressed: Boolean get() = category == RoutineCategory.SCHOOL && holidayTitle != null
        val isCarryIn: Boolean get() = startsAt.toLocalDate() < date
    }

    data class Milestone(
        val milestoneId: Long,
        override val date: LocalDate,
        override val title: String,
        override val subject: Subject?,
        val dueTime: LocalTime?,
        val isExam: Boolean,
        override val isCompleted: Boolean,
        val estimatedEffortHours: Double = 0.0,
        val isTerminalExam: Boolean = false,
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
    val planning: com.example.mydailyroutine.domain.planning.PlanningConfig = com.example.mydailyroutine.domain.planning.PlanningConfig(),
) {
    /** Half-open window. Overnight quiet windows are supported; equal endpoints are rejected. */
    fun isQuietAt(time: LocalTime): Boolean = muteDuringSchoolHours && when {
        schoolStart < schoolEnd -> time >= schoolStart && time < schoolEnd
        schoolStart > schoolEnd -> time >= schoolStart || time < schoolEnd
        else -> false
    }
}
