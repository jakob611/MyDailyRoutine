package com.example.mydailyroutine.core.designsystem.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The swipe has exactly one job: change the day when the reader meant it, and never otherwise. These
 * tests pin the three moments that matter — the nudge before the threshold, the single arming tick,
 * and the release that may or may not be a command.
 */
class SwipeShiftTrackerTest {

    private val threshold = 200f

    @Test fun aShortDragChangesNothing() {
        val tracker = SwipeShiftTracker(threshold)
        repeat(3) { tracker.add(-60f) }
        assertEquals(0, tracker.commit())
    }

    @Test fun aLongDragCommitsTheDirectionItStarted() {
        val tracker = SwipeShiftTracker(threshold)
        repeat(5) { tracker.add(-60f) }
        assertEquals(1, tracker.commit())
    }

    @Test fun draggingRightGoesBackInTime() {
        val tracker = SwipeShiftTracker(threshold)
        tracker.add(250f)
        assertEquals(-1, tracker.commit())
    }

    @Test fun theThresholdIsAnnouncedExactlyOnce() {
        val tracker = SwipeShiftTracker(threshold)
        val crossings = generateSequence(0) { it + 1 }
            .take(10)
            .count { tracker.add(-40f) }
        // 40 px steps: crosses at 200, never again while it stays past the threshold.
        assertEquals(1, crossings)
        assertTrue(tracker.armed)
    }

    @Test fun armsAgainAfterComingBackUnderTheThreshold() {
        val tracker = SwipeShiftTracker(threshold)
        tracker.add(-250f)
        assertTrue(tracker.armed)
        tracker.add(150f)
        assertFalse(tracker.armed)
        assertTrue(tracker.add(-150f))
    }

    @Test fun theNudgeIsDampedAndCapped() {
        val tracker = SwipeShiftTracker(threshold, resistance = 0.3f, maxOffsetPx = 50f)
        tracker.add(-100f)
        assertEquals(-30f, tracker.offset, 0.01f)
        tracker.add(-1000f)
        // The layer never travels further than the cap, however far the finger goes.
        assertEquals(-50f, tracker.offset, 0.01f)
    }

    @Test fun resetPutsEverythingBack() {
        val tracker = SwipeShiftTracker(threshold)
        tracker.add(-250f)
        tracker.reset()
        assertEquals(0f, tracker.offset, 0.01f)
        assertFalse(tracker.armed)
        assertEquals(0, tracker.commit())
    }
}
