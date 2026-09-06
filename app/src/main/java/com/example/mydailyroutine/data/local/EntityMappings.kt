package com.example.mydailyroutine.data.local

import com.example.mydailyroutine.domain.model.*

internal fun SubjectEntity.domain() = Subject(id, name, colorHex, defaultDurationMinutes)
internal fun Subject.entity() = SubjectEntity(id, name, colorHex, defaultDurationMinutes)
internal fun RoutineBlockEntity.domain() = RoutineBlueprint(
    id, subjectId, title, category, dayOfWeek, startTime, endTime, isNotificationEnabled, validFrom, validUntil,
)
internal fun RoutineBlueprint.entity() = RoutineBlockEntity(
    id, subjectId, title, category, dayOfWeek, startTime, endTime, isNotificationEnabled, validFrom, validUntil,
)
internal fun EventOverrideEntity.domain() = EventOverride(
    id, routineBlockId, overrideDate, isCancelled, customStartTime, customEndTime, customTitle,
)
internal fun EventOverride.entity() = EventOverrideEntity(
    id, routineBlockId, overrideDate, isCancelled, customStartTime, customEndTime, customTitle,
)
internal fun SchoolCalendarEntryEntity.domain() = CalendarEntry(id, date, title, isWorkFreeDay)
internal fun MilestoneEntity.domain() = Milestone(id, subjectId, title, dueDate, dueTime, isExam, isCompleted)
internal fun Milestone.entity() = MilestoneEntity(id, subjectId, title, dueDate, dueTime, isExam, isCompleted)
