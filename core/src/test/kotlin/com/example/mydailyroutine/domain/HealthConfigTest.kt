package com.example.mydailyroutine.domain

import com.example.mydailyroutine.domain.health.*
import com.example.mydailyroutine.domain.model.RoutineCategory
import org.junit.Assert.*
import org.junit.Test

class HealthConfigTest {
    private val engine = ScheduleHealthEngine()
    @Test fun `custom focus limit changes warnings without changing schedule`() {
        val items = listOf(block(480, 555))
        assertFalse(engine.evaluate(items).any { it.type == WarningType.CONCENTRATION_LIMIT })
        assertTrue(engine.evaluate(items, HealthConfig(focusLimitMinutes = 60)).any { it.type == WarningType.CONCENTRATION_LIMIT })
        assertFalse(engine.evaluate(items, HealthConfig(focusLimitMinutes = 75)).any { it.type == WarningType.CONCENTRATION_LIMIT })
    }
    @Test fun `custom cognitive daily sedentary limits preserve strict boundaries`() {
        val input = listOf(block(480, 600))
        assertFalse(engine.evaluate(input, HealthConfig(cognitiveLimitMinutes = 120, dailyFocusLimitMinutes = 120, sedentaryLimitMinutes = 120))
            .any { it.type in setOf(WarningType.HIGH_COGNITIVE_LOAD, WarningType.BURNOUT_RISK, WarningType.PHYSICAL_RESET) })
        val types = engine.evaluate(input, HealthConfig(cognitiveLimitMinutes = 119, dailyFocusLimitMinutes = 119, sedentaryLimitMinutes = 119)).map { it.type }
        assertTrue(types.containsAll(listOf(WarningType.HIGH_COGNITIVE_LOAD, WarningType.BURNOUT_RISK, WarningType.PHYSICAL_RESET)))
    }
    @Test fun `transition and fragmented bounds are configurable`() {
        val transition = listOf(block(480, 540, RoutineCategory.SCHOOL), block(560, 600))
        assertFalse(engine.evaluate(transition, HealthConfig(transitionMinutes = 20)).any { it.type == WarningType.INSUFFICIENT_TRANSITION })
        assertTrue(engine.evaluate(transition, HealthConfig(transitionMinutes = 21)).any { it.type == WarningType.INSUFFICIENT_TRANSITION })
        val gap = listOf(block(480, 540), block(570, 600))
        assertTrue(engine.evaluate(gap, HealthConfig(fragmentedMinMinutes = 30, fragmentedMaxMinutes = 30)).any { it.type == WarningType.FRAGMENTED_TIME })
        assertFalse(engine.evaluate(gap, HealthConfig(fragmentedMinMinutes = 31, fragmentedMaxMinutes = 90)).any { it.type == WarningType.FRAGMENTED_TIME })
    }
    @Test fun `each rule can be disabled independently`() {
        val items = listOf(block(300, 500, RoutineCategory.SCHOOL), block(500, 810), block(870, 930))
        val baseline = engine.evaluate(items).map { it.type }.toSet()
        assertEquals(WarningType.entries.toSet(), baseline)
        val disabled = listOf(
            WarningType.CONCENTRATION_LIMIT to HealthConfig(concentrationEnabled = false),
            WarningType.HIGH_COGNITIVE_LOAD to HealthConfig(cognitiveEnabled = false),
            WarningType.INSUFFICIENT_TRANSITION to HealthConfig(transitionEnabled = false),
            WarningType.BURNOUT_RISK to HealthConfig(dailyFocusEnabled = false),
            WarningType.PHYSICAL_RESET to HealthConfig(sedentaryEnabled = false),
            WarningType.FRAGMENTED_TIME to HealthConfig(fragmentationEnabled = false),
        )
        disabled.forEach { (type, config) -> assertEquals(baseline - type, engine.evaluate(items, config).map { it.type }.toSet()) }
    }
    @Test fun `all switches off emits nothing`() {
        assertTrue(engine.evaluate(listOf(block(0, 900)), HealthConfig(concentrationEnabled = false, cognitiveEnabled = false, transitionEnabled = false,
            dailyFocusEnabled = false, sedentaryEnabled = false, fragmentationEnabled = false)).isEmpty())
    }
    @Test(expected = IllegalArgumentException::class) fun `inverted fragment range is rejected`() { HealthConfig(fragmentedMinMinutes = 90, fragmentedMaxMinutes = 45) }
    @Test(expected = IllegalArgumentException::class) fun `invalid threshold is rejected`() { HealthConfig(focusLimitMinutes = 0) }
    @Test fun `load bar is disjoint across overlapping categories`() {
        val input = listOf(block(480, 600, RoutineCategory.SCHOOL), block(500, 650), block(600, 630, RoutineCategory.REST_BUFFER))
        val allocation = categoryAllocation(input)
        assertEquals(170, allocation.sumOf { it.minutes })
        assertEquals(0, allocation.single { it.category == RoutineCategory.REST_BUFFER }.minutes)
    }
}
