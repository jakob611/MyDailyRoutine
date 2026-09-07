package com.example.mydailyroutine.domain.learning

import kotlin.math.ceil
import java.math.BigInteger

/** Bounded historical sum(actual)/sum(raw planned); never repeatedly multiply an already-calibrated duration. */
class VelocityCalibrator(history: List<HistoricalVelocity>) {
    private val fractions = mutableMapOf<String, Pair<Long,Long>>()
    private val summaries: Map<String, SubjectVelocity> = history.filter {
        it.subjectId != null && it.plannedDurationMinutes > 0 && it.actualDurationMinutes > 0
    }.groupBy { it.subjectId!! }.mapValues { (id, all) ->
        val samples = all.sortedWith(compareByDescending<HistoricalVelocity> { it.timestamp }.thenByDescending { it.id }).take(120)
        val planned = samples.sumOf { it.plannedDurationMinutes.toLong() }
        val actual = samples.sumOf { it.actualDurationMinutes.toLong() }
        fractions[id] = when {
            actual * 2 < planned -> 1L to 2L
            actual * 2 > planned * 5 -> 5L to 2L
            else -> actual to planned
        }
        val ratio = (actual.toDouble() / planned).coerceIn(0.5, 2.5)
        val sorted = samples.map { it.actualDurationMinutes.toDouble() / it.plannedDurationMinutes }.sorted()
        val p90 = sorted[(ceil(sorted.size * 0.9).toInt() - 1).coerceAtLeast(0)].coerceIn(0.5, 3.0)
        SubjectVelocity(id, ratio, samples.size, maxOf(ratio, p90))
    }
    fun getCalibratedDuration(rawMinutes: Int, subjectId: String): Int {
        require(rawMinutes > 0)
        val (numerator, denominator) = fractions[subjectId] ?: (1L to 1L)
        val divisor = BigInteger.valueOf(denominator)
        // Integer ceiling prevents 90 * 1.1 becoming 100 due to binary floating-point noise.
        return BigInteger.valueOf(rawMinutes.toLong()).multiply(BigInteger.valueOf(numerator))
            .add(divisor.subtract(BigInteger.ONE)).divide(divisor).min(BigInteger.valueOf(1439)).toInt().coerceAtLeast(1)
    }
    fun multiplier(subjectId: String): Double = summaries[subjectId]?.multiplier ?: 1.0
    fun summaries(): List<SubjectVelocity> = summaries.values.sortedBy { it.subjectId }
    fun conservativeMultiplier(subjectId: String): Double = summaries[subjectId]?.conservativeMultiplier ?: 1.5
}
