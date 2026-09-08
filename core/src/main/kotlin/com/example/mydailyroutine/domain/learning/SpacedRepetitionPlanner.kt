package com.example.mydailyroutine.domain.learning

import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.pow
import kotlin.math.roundToLong

/** PDF pp.12–15: geometric targets, strict capacity, bounded jitter, then shorter retrieval format. */
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
            val ideal = request.initialEpochDay + (span * (k.toDouble() / request.count).pow(1.8)).roundToLong()
            val isi = (ideal - previousIdeal).coerceAtLeast(0)
            val jitter = floor(isi * 0.15).toInt()
            val lower = maxOf(request.initialEpochDay + 1, previousActual + 1, ideal - jitter)
            val upper = minOf(request.finalEpochDay, ideal + jitter)
            fun remaining(day: ReviewDayCapacity): Int {
                val extra = (added[day.epochDay] ?: 0).toLong()
                return minOf(day.studyCapacityMinutes / 5L - day.reviewMinutes - extra,
                    day.studyCapacityMinutes.toLong() - day.studyMinutes - extra, day.freeMinutes.toLong() - extra,
                    day.longestFreeSlotMinutes.toLong()).coerceAtLeast(0).toInt()
            }
            val eligible = if (lower > upper) emptyList() else (lower..upper).mapNotNull(days::get)
                .sortedWith(compareBy<ReviewDayCapacity> { (it.studyMinutes.toLong() + (added[it.epochDay] ?: 0)).toDouble() / it.studyCapacityMinutes.coerceAtLeast(1) }
                    .thenBy { abs(it.epochDay - ideal) }.thenBy { it.epochDay })
            val full = eligible.firstOrNull { remaining(it) >= request.durationMinutes }
            val minimum = request.minimumDurationMinutes
            val chosen = full ?: eligible.filter { remaining(it) >= minimum }
                .maxWithOrNull(compareBy<ReviewDayCapacity> { remaining(it) }.thenBy { -abs(it.epochDay - ideal) }.thenBy { -it.epochDay })
            val duration = chosen?.let { minOf(request.durationMinutes, remaining(it)) } ?: request.durationMinutes
            if (chosen != null) {
                added[chosen.epochDay] = (added[chosen.epochDay] ?: 0) + duration
                previousActual = chosen.epochDay
            }
            previousIdeal = ideal
            ReviewAllocation(k, ideal, chosen?.epochDay, duration, jitter, duration < request.durationMinutes)
        }
    }
    companion object {
        /** The paper's defaults are a suggestion; explicit user counts are still supported. */
        fun suggestedCount(horizonDays: Long): Int = when { horizonDays > 180 -> 5; horizonDays > 60 -> 4; horizonDays > 21 -> 3; else -> 2 }
    }
}
