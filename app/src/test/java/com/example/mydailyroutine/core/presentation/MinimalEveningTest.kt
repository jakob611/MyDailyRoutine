package com.example.mydailyroutine.core.presentation

import com.example.mydailyroutine.domain.model.ResolvedTimelineItem
import com.example.mydailyroutine.domain.model.RoutineCategory
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The two promises of the minimal-state protocol: it only ever engages in the evening, after two
 * blocks in a row really did go by unmarked, and it only ever takes elastic focus work that has not
 * started. Fixed commitments, planned rest, finished blocks and the running block stay where they are,
 * because a plan that quietly loses its skeleton is worse than one that stays noisy.
 */
class MinimalEveningTest {
    private val day: LocalDate = LocalDate.of(2026, 9, 7)

    private fun block(
        start: Int,
        end: Int,
        category: RoutineCategory = RoutineCategory.FOCUS_ANALYTICAL,
        completed: Boolean = false,
        elasticity: Double = 1.0,
        fixed: Boolean = category == RoutineCategory.SCHOOL,
        suppressed: Boolean = false,
    ): ResolvedTimelineItem.Block = ResolvedTimelineItem.Block(
        routineBlockId = start.toLong(),
        occurrenceDate = day,
        date = day,
        title = "Block $start",
        category = category,
        subject = null,
        startsAt = day.atStartOfDay().plusMinutes(start.toLong()),
        endsAt = day.atStartOfDay().plusMinutes(end.toLong()),
        startMinute = start,
        endMinute = end,
        isNotificationEnabled = true,
        isCompleted = completed,
        holidayTitle = if (suppressed) "Holiday" else null,
        hasOverride = false,
        isOneOff = false,
        elasticity = elasticity,
        isFixedCommitment = fixed,
    )

    private val twoMissedInTheMorning = listOf(
        block(start = 8 * 60, end = 9 * 60),
        block(start = 9 * 60, end = 10 * 60),
    )

    @Test fun twoMissedBlocksInARowEngageTheProtocolInTheEvening() {
        val evening = listOf(block(start = 19 * 60, end = 20 * 60))
        assertTrue(
            MinimalEvening.engages(twoMissedInTheMorning + evening, nowMinute = 19 * 60, isToday = true, tomorrowHasDeadline = false, requestedFull = false),
        )
    }

    @Test fun theProtocolDoesNotSpeakBeforeSix() {
        val afternoon = listOf(block(start = 17 * 60, end = 18 * 60))
        assertFalse(
            MinimalEvening.engages(twoMissedInTheMorning + afternoon, nowMinute = 15 * 60, isToday = true, tomorrowHasDeadline = false, requestedFull = false),
        )
    }

    @Test fun oneMissedBlockIsNotEnough() {
        val evening = listOf(block(start = 19 * 60, end = 20 * 60))
        assertFalse(
            MinimalEvening.engages(listOf(twoMissedInTheMorning.first()) + evening, nowMinute = 19 * 60, isToday = true, tomorrowHasDeadline = false, requestedFull = false),
        )
    }

    @Test fun aCompletedBlockResetsTheRun() {
        val afternoon = listOf(
            block(start = 13 * 60, end = 14 * 60, completed = true),
            block(start = 15 * 60, end = 16 * 60),
        )
        val evening = listOf(block(start = 19 * 60, end = 20 * 60))
        assertFalse(
            MinimalEvening.engages(afternoon + evening, nowMinute = 19 * 60, isToday = true, tomorrowHasDeadline = false, requestedFull = false),
        )
    }

    @Test fun aHolidaySuppressedBlockIsNotAMissedBlock() {
        val suppressed = listOf(
            block(start = 8 * 60, end = 9 * 60, category = RoutineCategory.SCHOOL, suppressed = true),
            block(start = 9 * 60, end = 10 * 60, category = RoutineCategory.SCHOOL, suppressed = true),
        )
        val evening = listOf(block(start = 19 * 60, end = 20 * 60))
        assertFalse(
            MinimalEvening.engages(suppressed + evening, nowMinute = 19 * 60, isToday = true, tomorrowHasDeadline = false, requestedFull = false),
        )
    }

    @Test fun fixedWorkAndRestAreNeverTakenOffTheTimeline() {
        val blocks = listOf(
            block(start = 19 * 60, end = 20 * 60, category = RoutineCategory.SCHOOL, elasticity = 0.0),
            block(start = 20 * 60, end = 21 * 60, category = RoutineCategory.REST_BUFFER, elasticity = 0.0, fixed = true),
            block(start = 21 * 60, end = 22 * 60),
        )
        val hidden = MinimalEvening.hiddenKeys(blocks, nowMinute = 19 * 60)
        assertEquals(setOf(blocks.last().key), hidden)
    }

    @Test fun whatHasAlreadyEndedOrIsRunningStaysOnTheTimeline() {
        val finished = block(start = 13 * 60, end = 14 * 60)
        val running = block(start = 19 * 60, end = 20 * 60)
        val later = block(start = 21 * 60, end = 22 * 60)
        val hidden = MinimalEvening.hiddenKeys(listOf(finished, running, later), nowMinute = 19 * 60 + 30, runningKey = running.key)
        assertEquals(setOf(later.key), hidden)
    }

    @Test fun theReadersOwnAnswerAndTomorrowsExamBothStandTheProtocolDown() {
        val blocks = twoMissedInTheMorning + block(start = 19 * 60, end = 20 * 60)
        assertFalse(MinimalEvening.engages(blocks, 19 * 60, isToday = true, tomorrowHasDeadline = false, requestedFull = true))
        assertFalse(MinimalEvening.engages(blocks, 19 * 60, isToday = true, tomorrowHasDeadline = true, requestedFull = false))
        assertFalse(MinimalEvening.engages(blocks, 19 * 60, isToday = false, tomorrowHasDeadline = false, requestedFull = false))
    }

    @Test fun aQuietEveningOnlyHappensWhenThereIsSomethingToQuiet() {
        // Two blocks missed, nothing left to hide: the protocol has nothing to offer and says nothing.
        assertFalse(MinimalEvening.engages(twoMissedInTheMorning, 19 * 60, isToday = true, tomorrowHasDeadline = false, requestedFull = false))
    }
}
