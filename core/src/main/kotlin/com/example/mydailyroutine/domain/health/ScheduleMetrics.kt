package com.example.mydailyroutine.domain.health

import com.example.mydailyroutine.domain.model.ResolvedTimelineItem
import com.example.mydailyroutine.domain.model.RoutineCategory

data class DailyMetrics(
    val focusMinutes: Int,
    val schoolMinutes: Int,
    val recoveryMinutes: Int,
    val occupiedMinutes: Int,
    val completedCount: Int,
    val blockCount: Int,
    val milestoneCount: Int,
    val examCount: Int,
)

object ScheduleMetrics {
    fun forDay(items: List<ResolvedTimelineItem>): DailyMetrics {
        val blocks = items.filterIsInstance<ResolvedTimelineItem.Block>().filterNot { it.isSuppressed }
        fun minutes(category: RoutineCategory? = null): Int = Intervals.minutes(
            blocks.filter { category == null || it.category == category }.map { MinuteInterval(it.startMinute, it.endMinute) },
        )
        val milestones = items.filterIsInstance<ResolvedTimelineItem.Milestone>()
        return DailyMetrics(
            focusMinutes = minutes(RoutineCategory.FOCUS_STUDY),
            schoolMinutes = minutes(RoutineCategory.SCHOOL),
            recoveryMinutes = minutes(RoutineCategory.REST_BREAK),
            occupiedMinutes = minutes(),
            completedCount = blocks.count { it.isCompleted },
            blockCount = blocks.size,
            milestoneCount = milestones.count { !it.isCompleted },
            examCount = milestones.count { it.isExam && !it.isCompleted },
        )
    }
}

/** Greedy interval coloring prevents overlapping weekly cards from hiding one another. */
data class PositionedBlock(val block: ResolvedTimelineItem.Block, val lane: Int, val laneCount: Int)

object WeeklyLayout {
    fun position(blocks: List<ResolvedTimelineItem.Block>): List<PositionedBlock> {
        val sorted = blocks.sortedWith(compareBy<ResolvedTimelineItem.Block> { it.startMinute }.thenBy { it.key })
        val result = mutableListOf<PositionedBlock>()
        val group = mutableListOf<Pair<ResolvedTimelineItem.Block, Int>>()
        val laneEnds = mutableListOf<Int>()
        var groupEnd = -1
        fun flush() {
            result += group.map { (block, lane) -> PositionedBlock(block, lane, laneEnds.size) }
            group.clear()
            laneEnds.clear()
        }
        sorted.forEach { block ->
            if (block.startMinute >= groupEnd && group.isNotEmpty()) flush()
            var lane = laneEnds.indexOfFirst { it <= block.startMinute }
            if (lane < 0) { lane = laneEnds.size; laneEnds += block.endMinute }
            else laneEnds[lane] = block.endMinute
            group += block to lane
            groupEnd = maxOf(groupEnd, block.endMinute)
        }
        if (group.isNotEmpty()) flush()
        return result
    }
}
