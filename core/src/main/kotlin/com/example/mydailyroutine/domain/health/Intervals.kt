package com.example.mydailyroutine.domain.health

/** Half-open minute intervals; touching appointments do not overlap. */
data class MinuteInterval(val start: Int, val end: Int) {
    init { require(start < end) }
    val duration: Int get() = end - start
    fun intersects(other: MinuteInterval): Boolean = start < other.end && end > other.start
}

object Intervals {
    fun union(intervals: List<MinuteInterval>): List<MinuteInterval> {
        val sorted = intervals.sortedWith(compareBy<MinuteInterval> { it.start }.thenBy { it.end })
        if (sorted.isEmpty()) return emptyList()
        val result = mutableListOf<MinuteInterval>()
        var current = sorted.first()
        for (next in sorted.drop(1)) {
            if (next.start <= current.end) current = MinuteInterval(current.start, maxOf(current.end, next.end))
            else { result += current; current = next }
        }
        result += current
        return result
    }

    fun minutes(intervals: List<MinuteInterval>): Int = union(intervals).sumOf { it.duration }

    fun subtract(source: List<MinuteInterval>, blocked: List<MinuteInterval>): List<MinuteInterval> {
        val blockers = union(blocked)
        return union(source).flatMap { interval ->
            val pieces = mutableListOf<MinuteInterval>()
            var cursor = interval.start
            for (block in blockers) {
                if (block.end <= cursor) continue
                if (block.start >= interval.end) break
                if (block.start > cursor) pieces += MinuteInterval(cursor, minOf(block.start, interval.end))
                cursor = maxOf(cursor, block.end)
                if (cursor >= interval.end) break
            }
            if (cursor < interval.end) pieces += MinuteInterval(cursor, interval.end)
            pieces
        }
    }

    fun clusters(
        periods: List<MinuteInterval>, recovery: List<MinuteInterval>, minimumRecovery: Int,
    ): List<List<MinuteInterval>> {
        val merged = union(periods)
        if (merged.isEmpty()) return emptyList()
        val resets = recovery.filter { it.duration >= minimumRecovery }
        val result = mutableListOf<MutableList<MinuteInterval>>()
        var current = mutableListOf(merged.first())
        result += current
        for (next in merged.drop(1)) {
            val previous = current.last()
            if (resets.any { it.start >= previous.end && it.end <= next.start }) {
                current = mutableListOf()
                result += current
            }
            current += next
        }
        return result
    }
}
