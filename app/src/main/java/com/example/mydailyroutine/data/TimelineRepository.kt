package com.example.mydailyroutine.data

import com.example.mydailyroutine.data.local.EventOverrideDao
import com.example.mydailyroutine.data.local.EventOverrideEntity
import com.example.mydailyroutine.data.local.MilestoneEntity
import com.example.mydailyroutine.data.local.RoutineBlockEntity
import com.example.mydailyroutine.data.local.RoutineDatabase
import com.example.mydailyroutine.data.local.SubjectEntity
import com.example.mydailyroutine.data.seed.DefaultDataSeeder
import com.example.mydailyroutine.data.seed.SchoolYearBounds
import com.example.mydailyroutine.domain.BreakSummary
import com.example.mydailyroutine.domain.MonthMarkers
import com.example.mydailyroutine.domain.ResolvedTimelineItem
import com.example.mydailyroutine.domain.RoutineCategory
import com.example.mydailyroutine.domain.TimelineItemKind
import com.example.mydailyroutine.domain.YearOverview
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first

/**
 * The only class that knows how a weekly blueprint becomes a date-specific timeline.
 * Each DAO source stays reactive, so editing any related table redraws the day immediately.
 */
class TimelineRepository(private val database: RoutineDatabase) {
    private val routineDao = database.routineBlockDao()
    private val overrideDao: EventOverrideDao = database.eventOverrideDao()
    private val calendarDao = database.schoolCalendarDao()
    private val milestoneDao = database.milestoneDao()
    private val subjectDao = database.subjectDao()

    suspend fun initialize() = DefaultDataSeeder.seedIfNeeded(database)

    fun observeSubjects(): Flow<List<SubjectEntity>> = subjectDao.observeAll()

    fun getTimelineForDate(date: LocalDate): Flow<List<ResolvedTimelineItem>> = combine(
        routineDao.observeForDay(date.dayOfWeek),
        overrideDao.observeForDate(date),
        calendarDao.observeForDate(date),
        milestoneDao.observeForDate(date),
        subjectDao.observeAll(),
    ) { routines, overrides, calendarEntry, milestones, subjects ->
        resolve(date, routines, overrides, calendarEntry, milestones, subjects)
    }.map { items ->
        items.sortedWith(compareBy<ResolvedTimelineItem> { it.startTime }.thenBy { it.kind })
    }.distinctUntilChanged()

    suspend fun getTimelineForDateOnce(date: LocalDate): List<ResolvedTimelineItem> =
        getTimelineForDate(date).firstValue()

    fun observeMonthMarkers(date: LocalDate): Flow<MonthMarkers> {
        val first = date.withDayOfMonth(1)
        val last = first.withDayOfMonth(first.lengthOfMonth())
        return combine(
            calendarDao.observeInRange(first, last),
            milestoneDao.observeInRange(first, last),
        ) { calendar, milestones ->
            MonthMarkers(
                workFreeDates = calendar.filter { it.isWorkFreeDay }.map { it.date }.toSet(),
                milestoneDates = milestones.map { it.dueDate }.toSet(),
                examDates = milestones.filter { it.isExam }.map { it.dueDate }.toSet(),
            )
        }
    }

    fun observeYearOverview(date: LocalDate): Flow<YearOverview> {
        val bounds = SchoolYearBounds.forDate(date)
        return combine(
            calendarDao.observeInRange(bounds.start, bounds.end),
            milestoneDao.observeInRange(bounds.start, bounds.end),
        ) { calendar, milestones ->
            YearOverview(
                schoolYearStart = bounds.start,
                schoolYearEnd = bounds.end,
                workFreeDays = calendar.count { it.isWorkFreeDay },
                milestoneCount = milestones.size,
                upcomingMilestones = milestones.filter { !it.dueDate.isBefore(date) }.take(8),
                breakDistribution = calendar
                    .filter { it.isWorkFreeDay }
                    .groupingBy { it.title }
                    .eachCount()
                    .map { (title, days) -> BreakSummary(title, days) }
                    .sortedByDescending(BreakSummary::days),
            )
        }
    }

    suspend fun addRoutineBlock(block: RoutineBlockEntity): Long = routineDao.insert(block)

    suspend fun addMilestone(milestone: MilestoneEntity): Long = milestoneDao.insert(milestone)

    suspend fun saveOverride(eventOverride: EventOverrideEntity): Long = overrideDao.insert(eventOverride)

    private fun resolve(
        date: LocalDate,
        routines: List<RoutineBlockEntity>,
        overrides: List<EventOverrideEntity>,
        calendarEntry: com.example.mydailyroutine.data.local.SchoolCalendarEntryEntity?,
        milestones: List<MilestoneEntity>,
        subjects: List<SubjectEntity>,
    ): List<ResolvedTimelineItem> {
        val subjectColors = subjects.associateBy(SubjectEntity::id)
        val overridesByBlock = overrides.associateBy { it.routineBlockId }
        val routineItems = routines.mapNotNull { block ->
            val override = overridesByBlock[block.id]
            if (override?.isCancelled == true) return@mapNotNull null
            val start = override?.customStartTime ?: block.startTime
            val end = override?.customEndTime ?: block.endTime
            val subject = block.subjectId?.let(subjectColors::get)
            ResolvedTimelineItem(
                stableId = "routine:${block.id}:$date",
                kind = TimelineItemKind.ROUTINE,
                routineBlockId = block.id,
                subjectId = block.subjectId,
                title = override?.customTitle?.takeIf(String::isNotBlank) ?: block.title,
                category = block.category,
                startTime = start,
                endTime = end,
                subjectColorHex = subject?.colorHex,
                isNotificationEnabled = block.isNotificationEnabled,
                isSchoolDayOff = calendarEntry?.isWorkFreeDay == true && block.category == RoutineCategory.SCHOOL,
                schoolDayOffTitle = calendarEntry?.title,
            )
        }
        val milestoneItems = milestones.map { milestone ->
            val subject = milestone.subjectId?.let(subjectColors::get)
            val dueTime = milestone.dueTime
            ResolvedTimelineItem(
                stableId = "milestone:${milestone.id}:$date",
                kind = TimelineItemKind.MILESTONE,
                milestoneId = milestone.id,
                subjectId = milestone.subjectId,
                title = milestone.title,
                startTime = dueTime ?: LocalTime.MIDNIGHT,
                endTime = dueTime ?: LocalTime.MIDNIGHT,
                isAllDay = dueTime == null,
                isExam = milestone.isExam,
                subjectColorHex = subject?.colorHex,
            )
        }
        return routineItems + milestoneItems
    }
}

private suspend fun <T> Flow<T>.firstValue(): T = first()
