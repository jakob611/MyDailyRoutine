package com.example.mydailyroutine.core.database

import com.example.mydailyroutine.core.database.entities.*
import com.example.mydailyroutine.core.database.daos.*

import com.example.mydailyroutine.domain.model.*
import com.example.mydailyroutine.domain.learning.*
import com.example.mydailyroutine.domain.planning.*
import java.time.LocalDate
import java.time.LocalTime

internal fun SubjectEntity.domain() = Subject(id, name, colorHex, defaultDurationMinutes)
internal fun Subject.entity() = SubjectEntity(id, name, colorHex, defaultDurationMinutes)
internal fun TimeBlockEntity.domain(): RoutineBlueprint {
    val start = LocalTime.ofSecondOfDay(startMinutes * 60L)
    return RoutineBlueprint(id, subjectId, title, category, dayOfWeek, start, start.plusMinutes(durationMinutes.toLong()),
        isNotificationEnabled, validFrom, validUntil, minDurationMinutes, elasticity, priorityWeight, isFixedCommitment,
        completedActualMinutes, rawDurationMinutes, topicId, milestoneId, stageOrder, seriesKey, parentRoutineId, origin, isEnabled)
}
internal fun RoutineBlueprint.entity() = TimeBlockEntity(id, subjectId, title, category, dayOfWeek,
    startTime.toSecondOfDay() / 60, nominalMinutes(startTime, endTime), isNotificationEnabled, validFrom, validUntil,
    minDurationMinutes, elasticity, priorityWeight, isFixedCommitment, completedActualMinutes, rawDurationMinutes, topicId, milestoneId, stageOrder, seriesKey, parentRoutineId, origin, isEnabled)
internal fun EventOverrideEntity.domain() = EventOverride(id, routineBlockId, overrideDate, isCancelled,
    customStartTime, customEndTime, customTitle, dayShift, cancellationReason)
internal fun EventOverride.entity() = EventOverrideEntity(id, routineBlockId, overrideDate, isCancelled,
    customStartTime, customEndTime, customTitle, dayShift, cancellationReason)
internal fun SchoolCalendarEntryEntity.domain() = CalendarEntry(id, date, title, isWorkFreeDay)
internal fun MilestoneEntity.domain() = Milestone(id, subjectId, title, LocalDate.ofEpochDay(targetDeadlineEpochDay), dueTime, isExam, isCompleted, estimatedEffortHours, isTerminalExam)
internal fun Milestone.entity() = MilestoneEntity(id, subjectId, title, dueDate.toEpochDay(), dueTime, isExam, isCompleted, estimatedEffortHours, isTerminalExam)
internal fun HistoricalVelocityEntity.domain() = HistoricalVelocity(subjectId?.toString(), plannedDurationMinutes, actualDurationMinutes, timestamp, id)
internal fun StudyTopicEntity.domain() = StudyTopic(id, title, subjectId, LocalDate.ofEpochDay(initialEpochDay), LocalDate.ofEpochDay(finalEpochDay), reviewCount, reviewDurationMinutes, priorityWeight, milestoneId)
internal fun StudyTopic.entity() = StudyTopicEntity(id, title, subjectId, initialDate.toEpochDay(), finalDate.toEpochDay(), reviewCount, reviewDurationMinutes, priorityWeight, milestoneId)
internal fun SpacedReviewEntity.domain() = SpacedReview(id, topicId, scheduledEpochDay, durationMinutes, priorityWeight, ordinal, timeBlockId, isCompleted)
internal fun BacklogEntryEntity.domain() = BacklogEntry(id, title, category, durationMinutes, minDurationMinutes, elasticity, priorityWeight,
    subjectId, sourceRoutineId, occurrenceDate, milestoneId, topicId, reviewId, BacklogReason.valueOf(reason), rawDurationMinutes, stageOrder)
internal fun BacklogEntry.entity() = BacklogEntryEntity(id, title, category, durationMinutes, minDurationMinutes, elasticity, priorityWeight,
    subjectId, sourceRoutineId, occurrenceDate, milestoneId, topicId, reviewId, reason.name, rawDurationMinutes, stageOrder)
