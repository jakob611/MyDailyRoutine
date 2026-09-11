package com.example.mydailyroutine.domain.health

/**
 * Proactive focus-break splitting.
 *
 * The reactive [RecoveryPlanner] already offers a break when the health engine flags a block that
 * exceeds the ultradian limit. This planner is the *proactive* counterpart: given a single focus
 * block, it returns the break intervals that should be inserted every [everyMinutes] of focused
 * work, each lasting [breakMinutes] — e.g. "insert a 5-minute break after each hour".
 *
 * Conservative and deterministic: it never returns a break that would spill past the block's end,
 * and it stops as soon as there is no room left for a full break before the block finishes. Pure
 * Kotlin with no Android dependency, so it is directly unit-testable on the JVM.
 */
object PeriodicBreaks {
    fun plan(
        block: MinuteInterval,
        everyMinutes: Int,
        breakMinutes: Int,
    ): List<MinuteInterval> {
        require(everyMinutes in 30..240) { "everyMinutes must be 30..240, was $everyMinutes" }
        require(breakMinutes in 1..60) { "breakMinutes must be 1..60, was $breakMinutes" }
        if (block.duration <= everyMinutes) return emptyList()
        val result = mutableListOf<MinuteInterval>()
        var cursor = block.start + everyMinutes
        while (cursor + breakMinutes <= block.end) {
            result += MinuteInterval(cursor, cursor + breakMinutes)
            cursor += everyMinutes + breakMinutes
        }
        return result
    }
}
