package com.example.mydailyroutine.domain.planning

/**
 * Five-phase repair of the operational window before the next fixed boundary. No external solver.
 * Active-set compression + greedy knapsack elimination + one adjacent circadian pass are O(N²).
 * Priority/duration deferral is a deterministic heuristic, not a claim of exact knapsack optimality.
 */
class DeterministicReschedulingEngine(private val circadian: CircadianPenalty = CircadianPenalty()) {
    fun recover(input: List<TimeBlock>, anchorMinutes: Int, delayMinutes: Int, horizonMinutes: Int = 1440): HealingResult {
        require(delayMinutes >= 0 && anchorMinutes in 0..10080 && horizonMinutes in 1..10080)
        require(input.map { it.id }.toSet().size == input.size)
        val sorted = input.sortedWith(compareBy<TimeBlock> { it.startMinutes }.thenBy { if (it.isFixed) 0 else 1 }.thenBy { it.id })
        val boundary = minOf(horizonMinutes, sorted.filter { it.isFixed && (it.startMinutes >= anchorMinutes || it.endMinutes > anchorMinutes) }.minOfOrNull { maxOf(anchorMinutes, it.startMinutes) } ?: horizonMinutes)
        val cursor = (anchorMinutes.toLong() + delayMinutes).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
        val candidates = sorted.filter { !it.isFixed && it.completedActualMinutes == null && it.startMinutes < boundary && it.endMinutes > anchorMinutes }
        val untouched = sorted.filter { it !in candidates }
        val originals = candidates.associateBy { it.id }
        val active = candidates.toMutableList()
        fun forward(blocks: List<TimeBlock>): List<TimeBlock> {
            var next = cursor.toLong()
            return blocks.filter { it.durationMinutes > 0 }.map { block ->
                val start = next
                next = start + block.durationMinutes
                // During feasibility checks starts may be beyond the horizon. Do not construct an
                // invalid public TimeBlock until feasibility has been established below.
                block.copy(startMinutes = start.coerceIn(-10080, 10080).toInt())
            }
        }
        fun finish(blocks: List<TimeBlock>): Long {
            var next = cursor.toLong()
            blocks.filter { it.durationMinutes > 0 }.forEach { next += it.durationMinutes }
            return next
        }
        fun deficit(): Int = (finish(active) - boundary).coerceIn(0, Int.MAX_VALUE.toLong()).toInt()
        val initial = deficit()
        // 1. Existing gaps absorb the part of the slip that does not reach the hard boundary.
        val slackUsed = (delayMinutes.toLong() - initial).coerceIn(0, delayMinutes.toLong()).toInt()
        // 2. Consume explicit pooled reserve first, then only the unprotected portion of REST_BUFFER.
        val buffers = active.filter { it.category.isBuffer && it.elasticity > 0 }.sortedWith(
            compareBy<TimeBlock> { if (it.category == com.example.mydailyroutine.domain.model.RoutineCategory.EMERGENCY_RESERVE) 0 else 1 }
                .thenByDescending { it.startMinutes }.thenBy { it.id })
        var bufferUsed = 0
        buffers.forEach { block ->
            val amount = minOf(deficit(), block.durationMinutes - block.minDurationMinutes)
            if (amount > 0) {
                val index = active.indexOfFirst { it.id == block.id }
                active[index] = block.copy(durationMinutes = block.durationMinutes - amount)
                bufferUsed += amount
            }
        }
        // 3. Quadratic elastic compression with repeated saturation at exact integer minima.
        val reductions = ElasticCompression.reductions(active, deficit())
        val deferred = mutableListOf<TimeBlock>()
        val compressed = active.mapNotNull { block ->
            val duration = block.durationMinutes - (reductions[block.id] ?: 0)
            if (duration == 0 && !block.category.isBuffer) {
                deferred += originals.getValue(block.id)
                null
            } else block.copy(durationMinutes = duration)
        }
        active.clear(); active.addAll(compressed)
        // 4. Density-greedy 0/1 elimination. Re-test feasibility after each removal (O(N²)).
        val elimination = active.filter { it.durationMinutes > 0 }.sortedWith(
            compareBy<TimeBlock> { it.priorityWeight / it.durationMinutes.coerceAtLeast(1) }.thenBy { it.priorityWeight }.thenBy { it.id })
        elimination.forEach { block ->
            if (deficit() > 0) { active.removeAll { it.id == block.id }; deferred += originals.getValue(block.id) }
        }
        // If actual time has passed the boundary, buffers are capacity, not work: remove them
        // rather than turning reserve into an academic backlog or moving a fixed commitment.
        active.removeAll { it.durationMinutes == 0 }
        val blockedStages = deferred.filter { it.precedenceGroup != null && it.stageOrder != null }.groupBy { it.precedenceGroup }
            .mapValues { (_, tasks) -> tasks.minOf { it.stageOrder!! } }
        val dependent = active.filter { task -> task.precedenceGroup?.let { group -> blockedStages[group]?.let { task.stageOrder != null && task.stageOrder > it } } == true && !task.category.isBuffer }
        dependent.forEach { deferred += originals.getValue(it.id) }
        active.removeAll(dependent.toSet())
        // 5. Regenerate and apply a bounded circadian improvement pass only if it remains feasible.
        if (finish(active) <= boundary) {
            fun cost(blocks: List<TimeBlock>): Double = forward(blocks).sumOf {
                circadian.cost(it.category, it.startMinutes, it.durationMinutes, it.priorityWeight)
            }
            for (i in 0 until (active.size - 1).coerceAtLeast(0)) {
                if (active[i].category.isBuffer || active[i + 1].category.isBuffer) continue
                if (active[i].precedenceGroup != null && active[i].precedenceGroup == active[i+1].precedenceGroup && active[i].stageOrder != active[i+1].stageOrder) continue
                val before = cost(active)
                val trial = active.toMutableList().apply { val first = this[i]; this[i] = this[i + 1]; this[i + 1] = first }
                if (finish(trial) <= boundary && cost(trial) + 1e-8 < before) { active.clear(); active.addAll(trial) }
            }
        }
        val regenerated = if (active.isEmpty()) emptyList() else forward(active)
        check(regenerated.all { it.endMinutes <= boundary }) { "Repair crossed a fixed boundary" }
        val compressedMinutes = active.filterNot { it.category.isBuffer }.sumOf { originals.getValue(it.id).durationMinutes - it.durationMinutes }
        return HealingResult((untouched + regenerated).sortedWith(compareBy<TimeBlock> { it.startMinutes }.thenBy { it.id }),
            deferred.sortedBy { it.id }, HealingReport(initial, slackUsed, bufferUsed, compressedMinutes, deferred.size,
                deferred.sumOf { it.durationMinutes }, boundary, cursor > boundary))
    }
}
