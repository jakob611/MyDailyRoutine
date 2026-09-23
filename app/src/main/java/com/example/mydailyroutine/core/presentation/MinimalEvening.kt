package com.example.mydailyroutine.core.presentation

import com.example.mydailyroutine.domain.model.ResolvedTimelineItem

/**
 * The minimal-state protocol (N15) and the neutral postponement it belongs to (N6).
 *
 * A day can fall apart: two blocks in a row end without a mark, the hour is late, and everything that
 * was planned for the afternoon is still waiting. The behavioural study that suggested this called it
 * an evening amnesty — delete what was skipped, push the rest quietly. This app does not delete a
 * reader's plan, so the protocol does something narrower and reversible:
 *
 * * it takes **elastic focus work that has not started** off the timeline for that evening, so the
 *   evening stops being a backlog;
 * * it says so in one line, with a way back ("Pokaži vse"), and remembers that answer for that date;
 * * it never touches a fixed commitment, planned rest, a lesson, a milestone, or anything already
 *   completed — those are the day's skeleton, not its elasticity;
 * * it does not engage at all when tomorrow holds an exam or a deadline. The plan for the last evening
 *   before an exam is the one plan nobody should touch, and keeping it whole is simpler and safer than
 *   synthesising a single 30-minute anchor at a guessed hour.
 *
 * The decisions live here, as pure functions over an already resolved day, so they can be reasoned
 * about and tested without a clock, a database or a device.
 */
object MinimalEvening {

    /** The protocol only speaks in the evening: before this hour the day is still being lived. */
    const val EveningHour = 18

    /** After this hour the day itself offers to put the skipped blocks aside (N6). */
    const val AmnestyHour = 20

    /** True when two blocks in a row have already ended without a mark and without being set aside. */
    fun hasTwoMissedInARow(blocks: List<ResolvedTimelineItem.Block>, nowMinute: Int): Boolean {
        var run = 0
        blocks.filter { !it.isSuppressed && it.endMinute <= nowMinute }
            .sortedBy { it.startMinute }
            .forEach { block ->
                run = if (block.isCompleted) 0 else run + 1
                if (run >= 2) return true
            }
        return false
    }

    /**
     * What the protocol takes off the timeline: elastic focus work that has not started yet. Anything
     * fixed, resting, completed, set aside, or already running stays exactly where it is.
     */
    fun hiddenKeys(
        blocks: List<ResolvedTimelineItem.Block>,
        nowMinute: Int,
        runningKey: String? = null,
    ): Set<String> = blocks.filter { it.hidesIt(nowMinute) && it.key != runningKey }.map { it.key }.toSet()

    private fun ResolvedTimelineItem.Block.hidesIt(nowMinute: Int): Boolean = category.isDeepWork &&
        elasticity > 0.0 && !isFixedCommitment && !isCompleted && !isSuppressed && endMinute > nowMinute

    /**
     * Whether the evening runs in minimal state at all. [requestedFull] is the reader's own answer to
     * "Pokaži vse" for this date; [tomorrowHasDeadline] keeps the night before an exam untouched.
     */
    fun engages(
        blocks: List<ResolvedTimelineItem.Block>,
        nowMinute: Int,
        isToday: Boolean,
        tomorrowHasDeadline: Boolean,
        requestedFull: Boolean,
    ): Boolean = isToday && !requestedFull && !tomorrowHasDeadline &&
        nowMinute >= EveningHour * 60 &&
        hasTwoMissedInARow(blocks, nowMinute) &&
        hiddenKeys(blocks, nowMinute).isNotEmpty()
}
