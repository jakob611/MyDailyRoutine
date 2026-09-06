package com.example.mydailyroutine.domain

import androidx.compose.runtime.Immutable
import com.example.mydailyroutine.data.local.EventOverrideEntity
import com.example.mydailyroutine.data.local.MilestoneEntity
import com.example.mydailyroutine.data.local.RoutineBlockEntity
import com.example.mydailyroutine.data.local.SchoolCalendarEntryEntity
import com.example.mydailyroutine.data.local.SubjectEntity
import java.time.LocalDate
import java.time.LocalTime

/** Categories intentionally remain small and stable because they are persisted as names in SQLite. */
enum class RoutineCategory {
    SCHOOL,
    FOCUS_STUDY,
    REST_BREAK,
    PROJECT,
    PERSONAL,
}

enum class TimelineItemKind {
    ROUTINE,
    MILESTONE,
}

@Immutable
data class ResolvedTimelineItem(
    val stableId: String,
    val kind: TimelineItemKind,
    val routineBlockId: Long? = null,
    val milestoneId: Long? = null,
    val subjectId: Long? = null,
    val title: String,
    val category: RoutineCategory? = null,
    val startTime: LocalTime,
    val endTime: LocalTime,
    val isAllDay: Boolean = false,
    val isExam: Boolean = false,
    val subjectColorHex: Long? = null,
    val isNotificationEnabled: Boolean = false,
    val isSchoolDayOff: Boolean = false,
    val schoolDayOffTitle: String? = null,
) {
    val durationMinutes: Int
        get() = if (isAllDay) 0 else {
            val minutes = java.time.Duration.between(startTime, endTime).toMinutes()
            if (minutes > 0) minutes.toInt() else 0
        }
}

@Immutable
data class TimelineSnapshot(
    val routines: List<RoutineBlockEntity>,
    val overrides: List<EventOverrideEntity>,
    val calendarEntry: SchoolCalendarEntryEntity?,
    val milestones: List<MilestoneEntity>,
    val subjects: List<SubjectEntity>,
)

@Immutable
data class MonthMarkers(
    val workFreeDates: Set<LocalDate>,
    val milestoneDates: Set<LocalDate>,
    val examDates: Set<LocalDate>,
)

@Immutable
data class YearOverview(
    val schoolYearStart: LocalDate,
    val schoolYearEnd: LocalDate,
    val workFreeDays: Int,
    val milestoneCount: Int,
    val upcomingMilestones: List<MilestoneEntity>,
    val breakDistribution: List<BreakSummary> = emptyList(),
)

@Immutable
data class BreakSummary(val title: String, val days: Int)
