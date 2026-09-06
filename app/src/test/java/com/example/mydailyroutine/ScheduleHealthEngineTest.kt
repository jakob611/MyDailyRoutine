package com.example.mydailyroutine

import com.example.mydailyroutine.domain.ResolvedTimelineItem
import com.example.mydailyroutine.domain.RoutineCategory
import com.example.mydailyroutine.domain.ScheduleHealthEngine
import com.example.mydailyroutine.domain.TimelineItemKind
import com.example.mydailyroutine.domain.WarningType
import java.time.LocalTime
import org.junit.Assert.assertTrue
import org.junit.Test

class ScheduleHealthEngineTest {
    private val engine = ScheduleHealthEngine()

    @Test
    fun longFocusAndDeepWorkCeilingAreFlagged() {
        val items = listOf(
            routine("focus-1", RoutineCategory.FOCUS_STUDY, 8, 0, 9, 31),
            routine("focus-2", RoutineCategory.FOCUS_STUDY, 10, 0, 14, 0),
        )

        val types = engine.evaluate(items).map { it.type }

        assertTrue(WarningType.CONCENTRATION_LIMIT in types)
        assertTrue(WarningType.BURNOUT_RISK in types)
    }

    @Test
    fun explicitTwentyMinuteBreakResetsCognitiveLoad() {
        val items = listOf(
            routine("school", RoutineCategory.SCHOOL, 8, 0, 10, 0),
            routine("break", RoutineCategory.REST_BREAK, 10, 0, 10, 20),
            routine("study", RoutineCategory.FOCUS_STUDY, 10, 20, 11, 30),
        )

        val types = engine.evaluate(items).map { it.type }

        assertTrue(WarningType.HIGH_COGNITIVE_LOAD !in types)
    }

    private fun routine(id: String, category: RoutineCategory, startHour: Int, startMinute: Int, endHour: Int, endMinute: Int) =
        ResolvedTimelineItem(
            stableId = id,
            kind = TimelineItemKind.ROUTINE,
            title = id,
            category = category,
            startTime = LocalTime.of(startHour, startMinute),
            endTime = LocalTime.of(endHour, endMinute),
        )
}
