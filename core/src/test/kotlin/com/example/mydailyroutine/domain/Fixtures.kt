package com.example.mydailyroutine.domain

import com.example.mydailyroutine.domain.model.ResolvedTimelineItem
import com.example.mydailyroutine.domain.model.RoutineCategory
import java.time.LocalDate

internal val monday: LocalDate = LocalDate.of(2026, 9, 7)
internal fun block(
    start: Int, end: Int, category: RoutineCategory = RoutineCategory.FOCUS_STUDY,
    id: Long = start.toLong() + 1, suppressed: Boolean = false,
): ResolvedTimelineItem.Block = ResolvedTimelineItem.Block(
    routineBlockId = id, occurrenceDate = monday, date = monday, title = "Block $id",
    category = category, subject = null,
    startsAt = monday.atStartOfDay().plusMinutes(start.toLong()),
    endsAt = monday.atStartOfDay().plusMinutes(end.toLong()),
    startMinute = start, endMinute = end, isNotificationEnabled = true, isCompleted = false,
    holidayTitle = if (suppressed) "Holiday" else null, hasOverride = false, isOneOff = false,
)
