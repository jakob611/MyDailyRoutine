package com.example.mydailyroutine.domain.planning

import com.example.mydailyroutine.domain.health.Intervals
import com.example.mydailyroutine.domain.health.MinuteInterval
import com.example.mydailyroutine.domain.model.ResolvedTimelineItem
import com.example.mydailyroutine.domain.model.RoutineCategory
import java.time.LocalDate

/** A bounded minute grid avoids repeated overlap scans while placing tasks (1440 is constant). */
class DailyCapacityPlanner(
    val date: LocalDate,
    items: List<ResolvedTimelineItem>,
    private val config: PlanningConfig,
    private val earliestMinute: Int = config.studyStartMinutes,
    private val latestMinute: Int = config.studyEndMinutes,
) {
    private val occupied = BooleanArray(1440)
    private var cachedGaps: List<MinuteInterval>? = null
    var studyMinutes: Int = 0; private set
    var reviewMinutes: Int = 0; private set
    private var studyPotentialMinutes = 0
    val effectiveStudyCapacity: Int get() = minOf(config.dailyStudyCapacityMinutes, studyPotentialMinutes)
    val effectiveReviewCapacity: Int get() = effectiveStudyCapacity / 5
    private val circadian = CircadianPenalty(config.dipCenterMinutes, config.dipSigmaMinutes)
    init {
        val blocks = items.filterIsInstance<ResolvedTimelineItem.Block>().filterNot { it.isSuppressed }
        blocks.forEach { mark(it.startMinute, it.endMinute) }
        studyMinutes = Intervals.minutes(blocks.filter { it.category.isDeepWork }.map { MinuteInterval(it.startMinute, it.endMinute) })
        reviewMinutes = Intervals.minutes(blocks.filter { it.reviewId != null }.map { MinuteInterval(it.startMinute, it.endMinute) })
        blocks.filter { it.category == RoutineCategory.SCHOOL }.maxOfOrNull { it.endMinute }?.let {
            mark(it, (it + config.postSchoolRecoveryMinutes).coerceAtMost(1440))
        }
        items.filterIsInstance<ResolvedTimelineItem.Milestone>().filter { it.isExam && !it.isCompleted && it.dueTime != null }.forEach {
            mark(it.startMinute, it.startMinute + 1)
        }
        studyPotentialMinutes = studyMinutes + freeMinutes()
    }
    private fun mark(start: Int, end: Int) {
        for (m in start.coerceIn(0,1440) until end.coerceIn(0,1440)) occupied[m] = true
        cachedGaps = null
    }
    private fun gaps(): List<MinuteInterval> {
        cachedGaps?.let { return it }
        val result = mutableListOf<MinuteInterval>()
        val end = minOf(config.studyEndMinutes, latestMinute).coerceIn(0,1440)
        var cursor = maxOf(config.studyStartMinutes, earliestMinute).coerceIn(0,1440)
        while (cursor < end) {
            if (occupied[cursor]) { cursor++; continue }
            val start = cursor
            while (cursor < end && !occupied[cursor]) cursor++
            result += MinuteInterval(start, cursor)
        }
        cachedGaps = result
        return result
    }
    fun freeMinutes(): Int = (maxOf(config.studyStartMinutes, earliestMinute).coerceIn(0,1440) until minOf(config.studyEndMinutes, latestMinute).coerceIn(0,1440)).count { !occupied[it] }
    fun remainingStudy(): Int = (effectiveStudyCapacity - studyMinutes).coerceAtLeast(0)
    fun longestFreeSlot(): Int = gaps().maxOfOrNull { it.duration } ?: 0
    fun candidate(duration: Int, category: RoutineCategory, priority: Double, review: Boolean = false, preferLate: Boolean = false, latestAllowedMinute: Int = 1440): Int? {
        if (duration <= 0 || duration > 1439 || (category.isDeepWork && duration > remainingStudy())) return null
        if (review && reviewMinutes + duration > effectiveReviewCapacity) return null
        if (!category.isDeepWork) {
            val after = minOf(config.dailyStudyCapacityMinutes, studyPotentialMinutes - duration)
            if (after < studyMinutes || after / 5 < reviewMinutes) return null
        }
        var best: Int? = null
        var bestCost = Double.POSITIVE_INFINITY
        var bestOutsidePreferred = Int.MAX_VALUE
        val phaseShift = config.dipCenterMinutes - 855
        val preferred = when (category) {
            RoutineCategory.FOCUS_ANALYTICAL -> (540 + phaseShift)..(750 + phaseShift)
            RoutineCategory.FOCUS_SYNTHESIZING -> (990 + phaseShift)..(1170 + phaseShift)
            else -> null
        }
        gaps().mapNotNull { gap -> val end = minOf(gap.end, latestAllowedMinute)
            if (end - gap.start >= duration) MinuteInterval(gap.start,end) else null
        }.forEach { gap ->
            // A Gaussian is minimized at a feasible endpoint or the opposite circadian phase.
            // Checking these integer points is exact for this objective, not an expensive minute scan.
            val opposite = kotlin.math.floor(((config.dipCenterMinutes + 720) % 1440) - duration / 2.0).toInt()
            val candidates = listOfNotNull(gap.start, gap.end - duration, opposite, opposite + 1,
                preferred?.first, preferred?.let { it.last - duration }).distinct()
            candidates.filter { it in gap.start..(gap.end - duration) }.forEach { start ->
                val cost = circadian.cost(category, start, duration, priority)
                val outside = preferred?.let { duration - (minOf(start + duration, it.last) - maxOf(start, it.first)).coerceAtLeast(0) } ?: 0
                if (outside < bestOutsidePreferred || (outside == bestOutsidePreferred &&
                    (cost < bestCost - 1e-9 || (kotlin.math.abs(cost - bestCost) <= 1e-9 && (best == null || if (preferLate) start > best!! else start < best!!))))) {
                    best = start; bestCost = cost; bestOutsidePreferred = outside
                }
            }
        }
        return best
    }
    fun allocate(start: Int, duration: Int, category: RoutineCategory, review: Boolean = false) {
        require(start >= 0 && start + duration <= 1440 && duration > 0)
        require((start until start + duration).none { occupied[it] })
        require(!category.isDeepWork || studyMinutes + duration <= effectiveStudyCapacity)
        require(!review || reviewMinutes + duration <= effectiveReviewCapacity)
        mark(start, start + duration)
        if (category.isDeepWork) studyMinutes += duration
        if (review) reviewMinutes += duration
        if (!category.isDeepWork) studyPotentialMinutes -= duration
    }
}
