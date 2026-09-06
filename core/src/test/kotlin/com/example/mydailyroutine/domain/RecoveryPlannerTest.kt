package com.example.mydailyroutine.domain

import com.example.mydailyroutine.domain.health.*
import com.example.mydailyroutine.domain.model.ResolvedTimelineItem
import com.example.mydailyroutine.domain.model.RoutineCategory
import java.time.Duration
import java.time.LocalTime
import org.junit.Assert.*
import org.junit.Test

class RecoveryPlannerTest {
    private val planner = RecoveryPlanner()
    private val engine = ScheduleHealthEngine()
    private fun warning(items: List<ResolvedTimelineItem>, type: WarningType, config: HealthConfig = HealthConfig()) = engine.evaluate(items, config).first { it.type == type }
    @Test fun `long focus is split without changing total study minutes`() {
        val focus = block(480, 600)
        val plan = (planner.plan(monday, listOf(focus), warning(listOf(focus), WarningType.CONCENTRATION_LIMIT), HealthConfig()) as RecoveryDecision.Insert).plan
        assertEquals(monday.atTime(9, 30), plan.start)
        assertEquals(15, plan.minutes)
        assertEquals(monday.atTime(9, 45), plan.continuation!!.start)
        assertEquals(monday.atTime(10, 15), plan.continuation.end)
        assertEquals(120L, Duration.between(plan.change!!.start, plan.change.end).toMinutes() + Duration.between(plan.continuation.start, plan.continuation.end).toMinutes())
    }
    @Test fun `custom focus threshold also controls split location`() {
        val config = HealthConfig(focusLimitMinutes = 60)
        val focus = block(480, 560)
        val plan = (planner.plan(monday, listOf(focus), warning(listOf(focus), WarningType.CONCENTRATION_LIMIT, config), config) as RecoveryDecision.Insert).plan
        assertEquals(monday.atTime(9, 0), plan.start)
    }
    @Test fun `school transition shifts only focus and preserves its duration`() {
        val items = listOf(block(480, 600, RoutineCategory.SCHOOL), block(610, 700))
        val plan = (planner.plan(monday, items, warning(items, WarningType.INSUFFICIENT_TRANSITION), HealthConfig()) as RecoveryDecision.Insert).plan
        assertEquals(monday.atTime(10, 0), plan.start)
        assertEquals(monday.atTime(10, 30), plan.change!!.start)
        assertEquals(90L, Duration.between(plan.change.start, plan.change.end).toMinutes())
        assertEquals(items[1], plan.change.original)
    }
    @Test fun `fixed commitments cannot be displaced by extending a focus block`() {
        val items = listOf(block(480, 600), block(600, 660, RoutineCategory.PERSONAL))
        val plan = (planner.plan(monday, items, warning(items, WarningType.CONCENTRATION_LIMIT), HealthConfig()) as RecoveryDecision.Insert).plan
        assertNull(plan.change)
        assertTrue(plan.start >= monday.atTime(11, 0))
    }
    @Test fun `timed exams are obstacles even though milestones have no duration`() {
        val items = listOf(block(480, 600), ResolvedTimelineItem.Milestone(1, monday, "Test", null, LocalTime.of(10, 5), true, false))
        val plan = (planner.plan(monday, items, warning(items, WarningType.CONCENTRATION_LIMIT), HealthConfig()) as RecoveryDecision.Insert).plan
        assertNull(plan.change)
        assertTrue(plan.start >= monday.atTime(10, 6))
    }
    @Test fun `fragmented window becomes intentional recovery`() {
        val items = listOf(block(480, 540), block(600, 660))
        val plan = (planner.plan(monday, items, warning(items, WarningType.FRAGMENTED_TIME), HealthConfig()) as RecoveryDecision.Insert).plan
        assertEquals(monday.atTime(9, 0), plan.start)
        assertEquals(60, plan.minutes)
        assertNull(plan.change)
    }
    @Test fun `existing rest is not duplicated`() {
        val items = listOf(block(480, 700, RoutineCategory.SCHOOL), block(700, 720, RoutineCategory.REST_BREAK))
        assertEquals(RecoveryDecision.AlreadyHandled, planner.plan(monday, items, warning(items, WarningType.HIGH_COGNITIVE_LOAD), HealthConfig()))
    }
    @Test fun `full fixed day has no safe mutation`() {
        val items = listOf(block(0, 720, RoutineCategory.SCHOOL), block(720, 1440, RoutineCategory.SCHOOL))
        assertEquals(RecoveryDecision.NoSpace, planner.plan(monday, items, warning(items, WarningType.HIGH_COGNITIVE_LOAD), HealthConfig()))
    }
    @Test fun `completed history is never split`() {
        val items = listOf(block(480, 600).copy(isCompleted = true))
        val plan = (planner.plan(monday, items, warning(items, WarningType.CONCENTRATION_LIMIT), HealthConfig()) as RecoveryDecision.Insert).plan
        assertNull(plan.change)
    }
    @Test fun `overnight carry chooses the visible identity rather than its duplicate`() {
        val carry = block(0, 180).copy(occurrenceDate = monday.minusDays(1), startsAt = monday.minusDays(1).atTime(23, 0), endsAt = monday.atTime(3, 0))
        val previous = carry.copy(date = monday.minusDays(1), startMinute = 1380, endMinute = 1440)
        val plan = (planner.plan(monday, listOf(previous, carry), warning(listOf(carry), WarningType.CONCENTRATION_LIMIT), HealthConfig()) as RecoveryDecision.Insert).plan
        assertEquals(monday.atTime(0, 30), plan.start)
        assertEquals(carry.key, plan.change!!.original.key)
    }
}
