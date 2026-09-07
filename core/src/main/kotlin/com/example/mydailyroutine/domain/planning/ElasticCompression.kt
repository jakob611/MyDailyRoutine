package com.example.mydailyroutine.domain.planning

import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.sqrt

/** Active-set water filling minimizes sum(x_i² / (2 e_i)); integer largest remainders remove drift. */
object ElasticCompression {
    fun reductions(blocks: List<TimeBlock>, requestedMinutes: Int): Map<String, Int> {
        require(requestedMinutes >= 0 && blocks.map { it.id }.distinct().size == blocks.size)
        val eligible = blocks.filter { !it.isFixed && !it.category.isBuffer && it.elasticity > 0 && it.durationMinutes > it.minDurationMinutes }
        var remaining = minOf(requestedMinutes.toLong(), eligible.sumOf { (it.durationMinutes - it.minDurationMinutes).toLong() }).toInt()
        val result = linkedMapOf<String, Int>()
        val active = eligible.toMutableList()
        while (remaining > 0 && active.isNotEmpty()) {
            val scale = active.maxOf { it.elasticity }
            val total = active.sumOf { it.elasticity / scale }
            val saturated = active.filter { remaining * (it.elasticity / scale) / total >= it.durationMinutes - it.minDurationMinutes }
            if (saturated.isNotEmpty()) {
                saturated.forEach { block ->
                    val capacity = block.durationMinutes - block.minDurationMinutes
                    result[block.id] = capacity
                    remaining -= capacity
                }
                active.removeAll(saturated.toSet())
                continue
            }
            val shares = active.map { block -> block to (remaining * (block.elasticity / scale) / total) }
            val floors = shares.associate { (block, amount) -> block.id to floor(amount).toInt().coerceIn(0, block.durationMinutes - block.minDurationMinutes) }.toMutableMap()
            var remainder = remaining - floors.values.sum()
            shares.sortedWith(compareByDescending<Pair<TimeBlock, Double>> { it.second - floor(it.second) }.thenBy { it.first.id }).forEach { (block, _) ->
                if (remainder > 0 && floors.getValue(block.id) < block.durationMinutes - block.minDurationMinutes) {
                    floors[block.id] = floors.getValue(block.id) + 1
                    remainder--
                }
            }
            check(remainder == 0) { "Integer compression allocation drift" }
            result.putAll(floors)
            remaining = 0
        }
        return result
    }
}

data class DurationEstimate(val medianMinutes: Int, val conservativeMinutes: Int)
object RsemBufferSizer {
    fun minutes(estimates: List<DurationEstimate>): Int {
        require(estimates.all { it.medianMinutes >= 0 && it.conservativeMinutes >= it.medianMinutes })
        val squares = estimates.sumOf { val delta = it.conservativeMinutes.toDouble() - it.medianMinutes; delta * delta }
        return ceil(sqrt(squares)).coerceIn(0.0, Int.MAX_VALUE.toDouble()).toInt()
    }
}
