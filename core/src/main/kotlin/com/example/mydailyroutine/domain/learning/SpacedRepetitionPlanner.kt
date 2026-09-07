package com.example.mydailyroutine.domain.learning

import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.pow
import kotlin.math.roundToLong

/** Geometric 1.8 spacing, exact 20% daily cap, and a deterministic ±15% ISI congestion search. */
class SpacedRepetitionPlanner {
    fun plan(request: ReviewRequest, capacity: List<ReviewDayCapacity>): List<ReviewAllocation> {
        require(request.count in 1..52 && request.durationMinutes in 1..120)
        require(request.finalEpochDay >= request.initialEpochDay && request.finalEpochDay - request.initialEpochDay <= 366)
        require(request.priorityWeight.isFinite() && request.priorityWeight > 0)
        require(capacity.map { it.epochDay }.toSet().size == capacity.size)
        require(capacity.all { it.studyCapacityMinutes >= 0 && it.studyMinutes >= 0 && it.reviewMinutes >= 0 && it.freeMinutes >= 0 })
        val days = capacity.associateBy { it.epochDay }
        val added = mutableMapOf<Long, Int>()
        val span = request.finalEpochDay - request.initialEpochDay
        var previousIdeal = request.initialEpochDay
        var previousActual = request.initialEpochDay
        return (1..request.count).map { k ->
            val calculated = request.initialEpochDay + (span * (k.toDouble() / request.count).pow(1.8)).roundToLong()
            val ideal = maxOf(calculated, previousIdeal + 1)
            val isi = ideal - previousIdeal
            val jitter = floor(isi * 0.15).toInt()
            val lower = maxOf(request.initialEpochDay + 1, previousActual + 1, ideal - jitter)
            val upper = minOf(request.finalEpochDay, ideal + jitter)
            val chosen = if (ideal > request.finalEpochDay || lower > upper) null else (lower..upper).mapNotNull { days[it] }
                .filter { day ->
                    val extra = (added[day.epochDay] ?: 0).toLong()
                    day.reviewMinutes + extra + request.durationMinutes <= day.studyCapacityMinutes / 5 &&
                        day.studyMinutes + extra + request.durationMinutes <= day.studyCapacityMinutes &&
                        day.freeMinutes - extra >= request.durationMinutes
                }.minWithOrNull(compareBy<ReviewDayCapacity> {
                    (it.studyMinutes + (added[it.epochDay] ?: 0)).toDouble() / it.studyCapacityMinutes.coerceAtLeast(1)
                }.thenBy { abs(it.epochDay - ideal) }.thenBy { it.epochDay })?.epochDay
            if (chosen != null) { added[chosen] = (added[chosen] ?: 0) + request.durationMinutes; previousActual = chosen }
            previousIdeal = ideal
            ReviewAllocation(k, minOf(ideal, request.finalEpochDay), chosen, request.durationMinutes, jitter)
        }
    }
}
