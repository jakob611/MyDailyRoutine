package com.example.mydailyroutine.domain.planning

import com.example.mydailyroutine.domain.model.RoutineCategory
import kotlin.math.abs
import kotlin.math.exp

/** Midpoint Gaussian cost, not a diagnosis or a measured personal circadian phase. */
class CircadianPenalty(private val centerMinutes: Int = 855, private val sigmaMinutes: Int = 45) {
    init { require(centerMinutes in 0..1439 && sigmaMinutes > 0) }
    fun kernel(minute: Double): Double {
        require(minute.isFinite())
        val local = ((minute % 1440.0) + 1440.0) % 1440.0
        val distance = abs(local - centerMinutes).let { minOf(it, 1440.0 - it) }
        return exp(-0.5 * (distance / sigmaMinutes) * (distance / sigmaMinutes))
    }
    fun cost(category: RoutineCategory, startMinutes: Int, durationMinutes: Int, priorityWeight: Double = 1.0): Double {
        require(durationMinutes >= 0 && priorityWeight.isFinite() && priorityWeight > 0)
        val weight = when (category) {
            RoutineCategory.FOCUS_ANALYTICAL -> 1.0
            RoutineCategory.FOCUS_SYNTHESIZING -> 0.60
            RoutineCategory.ADMIN -> 0.05
            else -> 0.0
        }
        return weight * priorityWeight * durationMinutes * kernel(startMinutes.toDouble() + durationMinutes / 2.0)
    }
}
