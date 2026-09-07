package com.example.mydailyroutine.domain.planning

import com.example.mydailyroutine.domain.model.RoutineCategory
import java.time.LocalDate
import java.time.temporal.ChronoUnit

data class PreparationBlock(val date: LocalDate, val startMinutes: Int, val durationMinutes: Int, val rawMinutes: Int,
    val reserve: Boolean = false, val stageOrder: Int = 0, val stageTitle: String = "")
data class UnplacedStudy(val durationMinutes: Int, val rawMinutes: Int, val stageOrder: Int = 0, val stageTitle: String = "")
data class MilestonePlan(val blocks: List<PreparationBlock>, val unplacedChunks: List<UnplacedStudy>, val unreservedMinutes: Int, val virtualDeadline: LocalDate)

/** PDF p.12: reverse ordered deliverables, each protected by its own RSEM feeding buffer. */
class MilestoneBackPlanner {
    fun plan(initialDate: LocalDate, deadline: LocalDate, estimatedEffortHours: Double, terminal: Boolean,
        category: RoutineCategory, velocity: Double, conservativeVelocity: Double,
        capacity: List<DailyCapacityPlanner>, config: PlanningConfig,
        stages: List<PreparationStage> = emptyList()): MilestonePlan {
        require(deadline >= initialDate && ChronoUnit.DAYS.between(initialDate, deadline) <= 366)
        require(estimatedEffortHours.isFinite() && estimatedEffortHours in 0.0..1000.0)
        require(category.isDeepWork && velocity.isFinite() && velocity > 0 && conservativeVelocity.isFinite() && conservativeVelocity >= velocity)
        require(stages.size <= 8)
        val rawTotal = MinuteRounding.ceiling(estimatedEffortHours * 60)
        val multiplier = velocity.coerceAtLeast(1.0)
        // Whole-plan overflow is explicit backlog; individual persisted blocks are always <= 90 min.
        val total = MinuteRounding.ceiling(rawTotal * multiplier)
        require(total <= 300_000) { "Preparation plan is too large for the bounded offline horizon" }
        val stageList = stages.ifEmpty { listOf(PreparationStage("",1)) }
        val weights = stageList.sumOf { it.weight }
        val rawShares = splitWeighted(rawTotal, stageList.map { it.weight }, weights)
        val shares = splitWeighted(total, stageList.map { it.weight }, weights)
        val span = ChronoUnit.DAYS.between(initialDate, deadline)
        val virtual = if (terminal) initialDate.plusDays((span * 0.85).toLong()) else deadline
        val days = capacity.filter { it.date in initialDate..virtual }.sortedByDescending { it.date }
        val result = mutableListOf<PreparationBlock>()
        val missing = mutableListOf<UnplacedStudy>()
        var missingReserve = 0
        var cursorDate = virtual
        var cursorMinute = config.studyEndMinutes
        fun place(duration: Int, type: RoutineCategory, priority: Double): Pair<DailyCapacityPlanner, Int>? {
            for (day in days) {
                if (day.date > cursorDate) continue
                val bound = if (day.date == cursorDate) cursorMinute else config.studyEndMinutes
                val start = day.candidate(duration,type,priority,preferLate=true,latestAllowedMinute=bound) ?: continue
                day.allocate(start,duration,type)
                cursorDate = day.date; cursorMinute = start
                return day to start
            }
            return null
        }
        for (index in stageList.indices.reversed()) {
            val stage = stageList[index]
            val durations = chunks(shares[index],config.targetFocusMinutes)
            if (durations.isEmpty()) continue
            val raw = splitWeighted(rawShares[index],durations,durations.sum())
            val reserve = RsemBufferSizer.minutes(durations.map { nominal -> DurationEstimate(nominal,
                maxOf(nominal,MinuteRounding.ceiling(nominal * (conservativeVelocity / multiplier).coerceAtLeast(1.0)))) })
            var reserveLeft = reserve
            while (reserveLeft > 0) {
                val amount = minOf(30,reserveLeft)
                val slot = place(amount,RoutineCategory.EMERGENCY_RESERVE,1.0)
                if (slot == null) { missingReserve += reserveLeft; break }
                result += PreparationBlock(slot.first.date,slot.second,amount,amount,true,index,stage.title)
                reserveLeft -= amount
            }
            // A missing buffer is surfaced; it is never represented as a fictitious successful reserve.
            for (part in durations.indices.reversed()) {
                val duration = durations[part]
                val slot = place(duration,category,if(terminal)8.0 else 4.0)
                if (slot == null) missing += UnplacedStudy(duration,raw[part].coerceAtLeast(1),index,stage.title)
                else result += PreparationBlock(slot.first.date,slot.second,duration,raw[part].coerceAtLeast(1),false,index,stage.title)
            }
        }
        return MilestonePlan(result.sortedWith(compareBy<PreparationBlock> { it.date }.thenBy { it.startMinutes }),
            missing.sortedBy { it.stageOrder },missingReserve,virtual)
    }
    private fun chunks(total: Int, target: Int): List<Int> {
        if (total <= 0) return emptyList()
        val count = (total + target - 1) / target
        return List(count) { total / count + if (it < total % count) 1 else 0 }
    }
    private fun splitWeighted(total: Int, weights: List<Int>, denominator: Int): List<Int> {
        if (weights.isEmpty()) return emptyList()
        val values = weights.map { total.toLong() * it / denominator }.map(Long::toInt).toMutableList()
        var rest = total - values.sum()
        weights.indices.sortedWith(compareByDescending<Int> { total.toLong() * weights[it] % denominator }.thenBy { it }).forEach {
            if (rest > 0) { values[it]++; rest-- }
        }
        return values
    }
}
