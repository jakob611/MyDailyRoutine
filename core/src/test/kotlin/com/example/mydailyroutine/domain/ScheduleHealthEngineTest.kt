package com.example.mydailyroutine.domain

import com.example.mydailyroutine.domain.health.ScheduleHealthEngine
import com.example.mydailyroutine.domain.health.WarningType
import com.example.mydailyroutine.domain.model.ResolvedTimelineItem
import com.example.mydailyroutine.domain.model.RoutineCategory.*
import org.junit.Assert.*
import org.junit.Test

class ScheduleHealthEngineTest {
    private val engine = ScheduleHealthEngine()
    private fun types(vararg blocks: ResolvedTimelineItem) = engine.evaluate(blocks.toList()).map { it.type }.toSet()

    @Test fun `ninety is allowed but ninety one warns`() {
        assertFalse(WarningType.CONCENTRATION_LIMIT in types(block(480, 570)))
        assertTrue(WarningType.CONCENTRATION_LIMIT in types(block(480, 571)))
    }

    @Test fun `180 minutes is boundary and 181 minutes warns across categories`() {
        assertFalse(WarningType.HIGH_COGNITIVE_LOAD in types(block(480, 570, SCHOOL), block(570, 660)))
        assertTrue(WarningType.HIGH_COGNITIVE_LOAD in types(block(480, 570, SCHOOL), block(570, 661)))
    }

    @Test fun `nineteen minute recovery is insufficient but twenty resets cognitive load`() {
        assertTrue(WarningType.HIGH_COGNITIVE_LOAD in types(
            block(480, 580, SCHOOL), block(580, 599, REST_BUFFER), block(599, 699),
        ))
        assertFalse(WarningType.HIGH_COGNITIVE_LOAD in types(
            block(480, 580, SCHOOL), block(580, 600, REST_BUFFER), block(600, 700),
        ))
    }

    @Test fun `adjacent rest blocks can form a twenty minute recovery`() {
        assertFalse(WarningType.HIGH_COGNITIVE_LOAD in types(
            block(480, 580), block(580, 590, REST_BUFFER), block(590, 600, REST_BUFFER), block(600, 700),
        ))
    }

    @Test fun `real unallocated recovery resets but a project is not a break`() {
        assertFalse(WarningType.HIGH_COGNITIVE_LOAD in types(block(480, 580), block(600, 700)))
        assertTrue(WarningType.HIGH_COGNITIVE_LOAD in types(
            block(480, 580), block(580, 640, FOCUS_SYNTHESIZING), block(640, 740),
        ))
    }

    @Test fun `rest overlapping work cannot reset fatigue`() {
        assertTrue(WarningType.HIGH_COGNITIVE_LOAD in types(
            block(480, 580), block(570, 600, REST_BUFFER), block(580, 690, SCHOOL),
        ))
    }

    @Test fun `transition is strict and detects conflicting school overlap`() {
        assertTrue(WarningType.INSUFFICIENT_TRANSITION in types(block(480, 600, SCHOOL), block(644, 690)))
        assertFalse(WarningType.INSUFFICIENT_TRANSITION in types(block(480, 600, SCHOOL), block(645, 690)))
        assertTrue(WarningType.INSUFFICIENT_TRANSITION in types(block(480, 610, SCHOOL), block(600, 660)))
    }

    @Test fun `270 daily focus minutes is boundary and 271 warns`() {
        assertFalse(WarningType.BURNOUT_RISK in types(block(480, 750)))
        assertTrue(WarningType.BURNOUT_RISK in types(block(480, 751)))
    }

    @Test fun `overlap never inflates focus totals or cognitive load`() {
        val overlaps = arrayOf(block(480, 650), block(500, 650, id = 2))
        assertFalse(WarningType.HIGH_COGNITIVE_LOAD in types(*overlaps))
        assertFalse(WarningType.BURNOUT_RISK in types(*overlaps))
    }

    @Test fun `sedentary threshold is strict and projects count`() {
        assertFalse(WarningType.PHYSICAL_RESET in types(block(480, 600, FOCUS_SYNTHESIZING)))
        assertTrue(WarningType.PHYSICAL_RESET in types(block(480, 601, FOCUS_SYNTHESIZING)))
        assertFalse(WarningType.PHYSICAL_RESET in types(block(480, 1000, ADMIN)))
    }

    @Test fun `five minute walk resets deskwork span`() {
        assertFalse(WarningType.PHYSICAL_RESET in types(
            block(480, 550), block(550, 555, REST_BUFFER), block(555, 625),
        ))
    }

    @Test fun `fragmentation includes boundaries only when unallocated`() {
        for (gap in listOf(45, 90)) assertTrue(WarningType.FRAGMENTED_TIME in types(block(480, 540), block(540 + gap, 690)))
        for (gap in listOf(44, 91)) assertFalse(WarningType.FRAGMENTED_TIME in types(block(480, 540), block(540 + gap, 690)))
        assertFalse(WarningType.FRAGMENTED_TIME in types(block(480, 540), block(540, 600, REST_BUFFER), block(600, 660)))
        assertFalse(WarningType.FRAGMENTED_TIME in types(block(480, 540), block(570, 580, ADMIN), block(600, 660)))
    }

    @Test fun `holiday school and zero duration milestones never add cognitive load`() {
        val milestone = ResolvedTimelineItem.Milestone(1, monday, "Exam", null, null, true, false)
        assertEquals(emptySet<WarningType>(), types(block(480, 900, SCHOOL, suppressed = true), milestone))
    }

    @Test fun `completed focus still counts toward planned load`() {
        assertTrue(WarningType.CONCENTRATION_LIMIT in types(block(480, 600).copy(isCompleted = true)))
    }

    @Test fun `empty is safe and input order does not change warning order`() {
        assertTrue(engine.evaluate(emptyList()).isEmpty())
        val blocks = listOf(block(480, 580, SCHOOL), block(580, 780), block(840, 950))
        assertEquals(engine.evaluate(blocks), engine.evaluate(blocks.reversed()))
    }

    @Test fun `warnings carry stable facts for localized Android copy`() {
        val input = block(480, 571)
        val warning = engine.evaluate(listOf(input)).single { it.type == WarningType.CONCENTRATION_LIMIT }
        assertEquals(WarningType.CONCENTRATION_LIMIT, warning.type)
        assertEquals(570, warning.atMinute)
        assertEquals(setOf(input.key), warning.relatedItemKeys)
    }
}
