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
    val message: String,
    val relatedItemKeys: Set<String>,
    val atMinute: Int,
)

/**
 * Deterministic planning heuristics, NOT a diagnosis or scientifically universal human limits.
 *
 * - Per-day totals use clipped, unioned intervals (overlap never counts twice).
 * - Completed work still contributes load; holiday-suppressed school and milestones do not.
 * - >=20 min of explicit REST or genuinely unallocated time resets cognitive accumulation.
 * - >=5 min of the same recovery resets the deskwork span; PROJECT is assumed deskwork.
 * - A recovery booked over work is NOT recovery. PERSONAL isn't presumed sedentary or restorative.
 * - Thresholds are strict >90, >180, >300, >120; transition is <30; fragmentation is 45..90 inclusive.
 */
class ScheduleHealthEngine {
    fun evaluate(items: List<ResolvedTimelineItem>): List<HealthWarning> {
        require(items.map { it.date }.distinct().size <= 1) { "Evaluate one display date at a time." }
        val blocks = items.filterIsInstance<ResolvedTimelineItem.Block>()
            .filter { !it.isSuppressed && it.durationMinutes > 0 }
            .distinctBy { it.key }
            .sortedWith(compareBy<ResolvedTimelineItem.Block> { it.startMinute }.thenBy { it.key })
        if (blocks.isEmpty()) return emptyList()
        val focus = blocks.filter { it.category == RoutineCategory.FOCUS_STUDY }
        val school = blocks.filter { it.category == RoutineCategory.SCHOOL }
        val cognitive = blocks.filter { it.category in cognitiveCategories }
        val seated = blocks.filter { it.category in seatedCategories }
        val nonRest = blocks.filter { it.category != RoutineCategory.REST_BREAK }.map { it.interval() }
        val rest = blocks.filter { it.category == RoutineCategory.REST_BREAK }.map { it.interval() }
        val free = Intervals.subtract(listOf(MinuteInterval(0, 1440)), blocks.map { it.interval() })
        val recovery = Intervals.union(Intervals.subtract(rest, nonRest) + free)
        val warnings = mutableListOf<HealthWarning>()

        fun warn(type: WarningType, related: List<ResolvedTimelineItem.Block>, minute: Int) {
            warnings += HealthWarning(type, messages.getValue(type), related.map { it.key }.toSet(), minute)
        }

        focus.filter { Duration.between(it.startsAt, it.endsAt).toMinutes() > 90 }.forEach {
            // A >90-minute overnight occurrence must not escape this rule by crossing midnight.
            warn(WarningType.CONCENTRATION_LIMIT, listOf(it), minOf(it.startMinute + 90, it.endMinute))
        }

        Intervals.clusters(cognitive.map { it.interval() }, recovery, minimumRecovery = 20).forEach { cluster ->
            if (cluster.sumOf { it.duration } > 180) {
                val related = cognitive.filter { block -> cluster.any { it.intersects(block.interval()) } }
                warn(WarningType.HIGH_COGNITIVE_LOAD, related, minuteAtLoad(cluster, 180))
            }
        }

        focus.forEach { study ->
            val lastSchool = school.filter { it.startMinute <= study.startMinute }.maxByOrNull { it.endMinute }
            if (lastSchool != null && study.startMinute - lastSchool.endMinute < 30) {
                warn(WarningType.INSUFFICIENT_TRANSITION, listOf(lastSchool, study), study.startMinute)
            }
        }

        val focusIntervals = Intervals.union(focus.map { it.interval() })
        if (focusIntervals.sumOf { it.duration } > 300) {
            warn(WarningType.BURNOUT_RISK, focus, minuteAtLoad(focusIntervals, 300))
        }

        Intervals.clusters(seated.map { it.interval() }, recovery, minimumRecovery = 5).forEach { cluster ->
            if (cluster.last().end - cluster.first().start > 120) {
                val related = seated.filter { block -> cluster.any { it.intersects(block.interval()) } }
                warn(WarningType.PHYSICAL_RESET, related, cluster.first().start + 120)
            }
        }

        focusIntervals.zipWithNext().forEach { (before, after) ->
            if (after.start - before.end in 45..90) {
                val gap = MinuteInterval(before.end, after.start)
                // An allocated walk, meal, school block, or other routine makes the gap intentional.
                if (blocks.none { it.interval().intersects(gap) }) {
                    val related = focus.filter { it.endMinute == before.end || it.startMinute == after.start }
                    warn(WarningType.FRAGMENTED_TIME, related, before.end)
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
        private val cognitiveCategories = setOf(RoutineCategory.SCHOOL, RoutineCategory.FOCUS_STUDY)
        private val seatedCategories = cognitiveCategories + RoutineCategory.PROJECT
        val messages: Map<WarningType, String> = mapOf(
            WarningType.CONCENTRATION_LIMIT to "Continuous deep focus past 90 min causes sharp cognitive decline. Insert a 10-15 min recovery block.",
            WarningType.HIGH_COGNITIVE_LOAD to "Mental stamina exhausted. Schedule a physical or screen-free break.",
            WarningType.INSUFFICIENT_TRANSITION to "No buffer after school. Allow at least 30 minutes for a nutritional and cognitive reset.",
            WarningType.BURNOUT_RISK to "Total daily deep work exceeds human sustained limits (~4-5h). Diminishing returns detected.",
            WarningType.PHYSICAL_RESET to "Take a walk or implement the 20-20-20 visual rule to prevent eye strain and fatigue.",
            WarningType.FRAGMENTED_TIME to "Fragmented dead window. Either consolidate blocks or convert this into intentional recovery.",
        )
    }
}

private fun ResolvedTimelineItem.Block.interval() = MinuteInterval(startMinute, endMinute)
