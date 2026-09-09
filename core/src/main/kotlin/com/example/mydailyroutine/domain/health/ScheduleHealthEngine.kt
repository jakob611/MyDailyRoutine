package com.example.mydailyroutine.domain.health

import com.example.mydailyroutine.domain.model.ResolvedTimelineItem
import com.example.mydailyroutine.domain.model.RoutineCategory
import java.time.Duration

enum class WarningType {
    CONCENTRATION_LIMIT, HIGH_COGNITIVE_LOAD, INSUFFICIENT_TRANSITION,
    BURNOUT_RISK, PHYSICAL_RESET, FRAGMENTED_TIME,
}

data class HealthWarning(
    val type: WarningType,
    val relatedItemKeys: Set<String>,
    val atMinute: Int,
)

/**
 * Deterministic planning heuristics, NOT a diagnosis or scientifically universal human limits.
 *
 * - Per-day totals use clipped, unioned intervals (overlap never counts twice).
 * - Completed work still contributes load; holiday-suppressed school and milestones do not.
 * - >=20 min of explicit REST or genuinely unallocated time resets cognitive accumulation.
 * - >=5 min of the same recovery resets the deskwork span; FOCUS_SYNTHESIZING is assumed deskwork.
 * - A recovery booked over work is NOT recovery. ADMIN isn't presumed sedentary or restorative.
 * - Defaults: strict >90, >180, >300, >120; transition <30; fragmentation 45..90 inclusive.
 *   A HealthConfig customizes thresholds and enables/disables each rule without changing interval accounting.
 * - Warnings carry facts only. All displayed copy is localized by the Android resource layer.
 */
class ScheduleHealthEngine {
    fun evaluate(items: List<ResolvedTimelineItem>, config: HealthConfig = HealthConfig(), periodic: PeriodicBreakConfig? = null): List<HealthWarning> {
        require(items.map { it.date }.distinct().size <= 1) { "Evaluate one display date at a time." }
        val blocks = items.filterIsInstance<ResolvedTimelineItem.Block>()
            .filter { !it.isSuppressed && it.durationMinutes > 0 }
            .distinctBy { it.key }
            .sortedWith(compareBy<ResolvedTimelineItem.Block> { it.startMinute }.thenBy { it.key })
        if (blocks.isEmpty()) return emptyList()
        val focus = blocks.filter { it.category.isDeepWork }
        val school = blocks.filter { it.category == RoutineCategory.SCHOOL }
        val cognitive = blocks.filter { it.category in cognitiveCategories }
        val seated = blocks.filter { it.category in seatedCategories }
        val nonRest = blocks.filter { !it.category.isBuffer }.map { it.interval() }
        val rest = blocks.filter { it.category.isBuffer }.map { it.interval() }
        val free = Intervals.subtract(listOf(MinuteInterval(0, 1440)), blocks.map { it.interval() })
        val recovery = Intervals.union(Intervals.subtract(rest, nonRest) + free)
        val warnings = mutableListOf<HealthWarning>()

        fun warn(type: WarningType, related: List<ResolvedTimelineItem.Block>, minute: Int) {
            if (config.isEnabled(type)) warnings += HealthWarning(type, related.map { it.key }.toSet(), minute)
        }

        focus.filter { Duration.between(it.startsAt, it.endsAt).toMinutes() > config.focusLimitMinutes }.forEach {
            // A >90-minute overnight occurrence must not escape this rule by crossing midnight.
            warn(WarningType.CONCENTRATION_LIMIT, listOf(it), minOf(it.startMinute + config.focusLimitMinutes, it.endMinute))
        }

        Intervals.clusters(cognitive.map { it.interval() }, recovery, minimumRecovery = HealthConfig.COGNITIVE_RECOVERY_MINUTES).forEach { cluster ->
            if (cluster.sumOf { it.duration } > config.cognitiveLimitMinutes) {
                val related = cognitive.filter { block -> cluster.any { it.intersects(block.interval()) } }
                warn(WarningType.HIGH_COGNITIVE_LOAD, related, minuteAtLoad(cluster, config.cognitiveLimitMinutes))
            }
        }

        focus.forEach { study ->
            val lastSchool = school.filter { it.startMinute <= study.startMinute }.maxByOrNull { it.endMinute }
            if (lastSchool != null && study.startMinute - lastSchool.endMinute < config.transitionMinutes) {
                warn(WarningType.INSUFFICIENT_TRANSITION, listOf(lastSchool, study), study.startMinute)
            }
        }

        val focusIntervals = Intervals.union(focus.map { it.interval() })
        if (focusIntervals.sumOf { it.duration } > config.dailyFocusLimitMinutes) {
            warn(WarningType.BURNOUT_RISK, focus, minuteAtLoad(focusIntervals, config.dailyFocusLimitMinutes))
        }

        Intervals.clusters(seated.map { it.interval() }, recovery, minimumRecovery = HealthConfig.PHYSICAL_RECOVERY_MINUTES).forEach { cluster ->
            if (cluster.last().end - cluster.first().start > config.sedentaryLimitMinutes) {
                val related = seated.filter { block -> cluster.any { it.intersects(block.interval()) } }
                warn(WarningType.PHYSICAL_RESET, related, cluster.first().start + config.sedentaryLimitMinutes)
            }
        }

        focusIntervals.zipWithNext().forEach { (before, after) ->
            if (after.start - before.end in config.fragmentedMinMinutes..config.fragmentedMaxMinutes) {
                val gap = MinuteInterval(before.end, after.start)
                // An allocated walk, meal, school block, or other routine makes the gap intentional.
                if (blocks.none { it.interval().intersects(gap) }) {
                    val related = focus.filter { it.endMinute == before.end || it.startMinute == after.start }
                    warn(WarningType.FRAGMENTED_TIME, related, before.end)
                }
            }
        }
        if (periodic != null && periodic.enabled) {
            val every = periodic.everyMinutes
            val len = periodic.breakMinutes
            val buffer = blocks.filter { it.category.isBuffer }.map { it.interval() }
            focus.forEach { block ->
                var t = block.startMinute + every
                while (t + len <= block.endMinute) {
                    val slot = MinuteInterval(t, t + len)
                    val coveredByRest = buffer.any { it.intersects(slot) }
                    val inFreeGap = free.any { it.start <= slot.start && it.end >= slot.end }
                    if (!coveredByRest && !inFreeGap) {
                        warnings += HealthWarning(WarningType.CONCENTRATION_LIMIT, setOf(block.key), t)
                    }
                    t += every + len
                }
            }
        }
        return warnings.sortedWith(compareBy<HealthWarning> { it.atMinute }.thenBy { it.type.ordinal })
    }

    private fun minuteAtLoad(intervals: List<MinuteInterval>, threshold: Int): Int {
        var remaining = threshold
        for (interval in intervals) {
            if (remaining < interval.duration) return interval.start + remaining
            remaining -= interval.duration
        }
        return intervals.last().end
    }

    companion object {
        private val cognitiveCategories = setOf(RoutineCategory.SCHOOL, RoutineCategory.FOCUS_ANALYTICAL, RoutineCategory.FOCUS_SYNTHESIZING)
        private val seatedCategories = cognitiveCategories + RoutineCategory.FOCUS_SYNTHESIZING

    }
}

private fun ResolvedTimelineItem.Block.interval() = MinuteInterval(startMinute, endMinute)
