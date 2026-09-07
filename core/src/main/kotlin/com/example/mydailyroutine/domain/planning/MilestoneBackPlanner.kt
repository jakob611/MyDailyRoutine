package com.example.mydailyroutine.domain.planning

import com.example.mydailyroutine.domain.model.RoutineCategory
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.abs
import kotlin.math.ceil

data class PreparationBlock(val date: LocalDate, val startMinutes: Int, val durationMinutes: Int, val rawMinutes: Int, val reserve: Boolean = false)
data class UnplacedStudy(val durationMinutes: Int, val rawMinutes: Int)
data class MilestonePlan(val blocks: List<PreparationBlock>, val unplacedChunks: List<UnplacedStudy>, val unreservedMinutes: Int, val virtualDeadline: LocalDate)

/** Backwards distributed chunks + earlier virtual deadline + a pooled RSEM tail, never an external solver. */
class MilestoneBackPlanner {
    fun plan(initialDate: LocalDate, deadline: LocalDate, estimatedEffortHours: Double, terminal: Boolean,
        category: RoutineCategory, velocity: Double, conservativeVelocity: Double,
        capacity: List<DailyCapacityPlanner>, config: PlanningConfig): MilestonePlan {
        require(deadline >= initialDate && ChronoUnit.DAYS.between(initialDate, deadline) <= 366)
        require(estimatedEffortHours.isFinite() && estimatedEffortHours in 0.0..1000.0)
        require(category.isDeepWork && velocity.isFinite() && velocity > 0 && conservativeVelocity.isFinite() && conservativeVelocity >= velocity)
        val rawTotal = MinuteRounding.ceiling(estimatedEffortHours * 60)
        val total = MinuteRounding.ceiling(rawTotal * velocity.coerceIn(0.5,2.5))
        val span = ChronoUnit.DAYS.between(initialDate, deadline)
        val virtual = if (terminal) initialDate.plusDays((span * 0.85).toLong()) else deadline
        val dates = capacity.filter { it.date in initialDate..virtual }.sortedByDescending { it.date }
        val chunks = chunks(total, config.targetFocusMinutes)
        val result = mutableListOf<PreparationBlock>()
        val missing = mutableListOf<UnplacedStudy>()
        var rawLeft = rawTotal
        var calibratedLeft = total
        // RSEM uses raw t50 and a conservative estimate; the 1.5 fallback is a planning policy,
        // not a claimed measured probability for a student without history.
        var reserve = RsemBufferSizer.minutes(chunks.map { DurationEstimate(it,
            ceil(it * (conservativeVelocity / velocity).coerceAtLeast(1.0)).toInt()) })
        capacity.filter { it.date in virtual..deadline }.sortedByDescending { it.date }.forEach { day ->
            while (reserve > 0) {
                val amount = minOf(60, reserve)
                val start = day.candidate(amount, RoutineCategory.EMERGENCY_RESERVE, 1.0, preferLate = true) ?: break
                day.allocate(start, amount, RoutineCategory.EMERGENCY_RESERVE)
                result += PreparationBlock(day.date, start, amount, amount, true)
                reserve -= amount
            }
        }
        chunks.forEachIndexed { index, duration ->
            val raw = if (calibratedLeft == duration) rawLeft else (rawLeft.toLong() * duration / calibratedLeft.coerceAtLeast(1)).toInt().coerceAtLeast(1)
            rawLeft = (rawLeft - raw).coerceAtLeast(0); calibratedLeft -= duration
            // Distribute backwards across the horizon rather than cramming everything on its last day.
            val idealIndex = if (chunks.size <= 1) 0 else (index.toLong() * (dates.size - 1).coerceAtLeast(0) / (chunks.size - 1)).toInt()
            val choices = dates.mapIndexedNotNull { dayIndex, day ->
                day.candidate(duration, category, if (terminal) 8.0 else 4.0)?.let { Triple(dayIndex, day, it) }
            }
            val selected = choices.minWithOrNull(compareBy<Triple<Int,DailyCapacityPlanner,Int>> { abs(it.first - idealIndex) }
                .thenBy { it.second.studyMinutes }.thenByDescending { it.second.date })
            if (selected == null) missing += UnplacedStudy(duration, raw.coerceAtLeast(1)) else {
                selected.second.allocate(selected.third, duration, category)
                result += PreparationBlock(selected.second.date, selected.third, duration, raw.coerceAtLeast(1))
            }
        }
        return MilestonePlan(result.sortedWith(compareBy<PreparationBlock> { it.date }.thenBy { it.startMinutes }), missing, reserve, virtual)
    }
    private fun chunks(total: Int, target: Int): List<Int> {
        if (total <= 0) return emptyList()
        val count = (total + target - 1) / target
        return List(count) { total / count + if (it < total % count) 1 else 0 }
    }
}
