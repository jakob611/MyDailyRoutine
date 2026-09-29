package com.example.mydailyroutine.core.designsystem.components

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitHorizontalTouchSlopOrCancellation
import androidx.compose.foundation.gestures.horizontalDrag
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.mydailyroutine.core.designsystem.haptics.LocalRoutineHaptics
import com.example.mydailyroutine.core.designsystem.motion.LocalReduceMotion
import kotlin.math.abs

/**
 * How far a finger has to travel before a swipe is worth acting on. One thumb-width: long enough that
 * a stray horizontal wobble while scrolling the list never changes the day, short enough that a
 * deliberate flick goes through without the reader having to think about it.
 */
val SwipeShiftThreshold = 64.dp

/**
 * The arithmetic of a horizontal swipe, deliberately kept out of the gesture so it can be unit
 * tested on the JVM.
 *
 * A drag does three things. It nudges the content by a fraction of the finger's travel ([resistance]),
 * so the surface answers the touch without pretending the page has already moved. It *arms* once the
 * travel passes the threshold — exactly once per gesture, which is what the single haptic tick marks.
 * And on release it commits a direction, or nothing at all. Below the threshold nothing happens: a
 * short, accidental drag must never take the reader to another day.
 */
class SwipeShiftTracker(
    private val thresholdPx: Float,
    private val resistance: Float = 0.32f,
    private val maxOffsetPx: Float = 72f,
) {
    private var travel = 0f

    /** Visual nudge in pixels, read in the draw phase by the layer that owns the gesture. */
    var offset by mutableFloatStateOf(0f)
        private set

    /** True while the drag is past the threshold and would commit if released now. */
    var armed = false
        private set

    /** Feeds one drag delta; returns true on the single delta that crossed the threshold. */
    fun add(deltaPx: Float): Boolean {
        travel += deltaPx
        offset = (travel * resistance).coerceIn(-maxOffsetPx, maxOffsetPx)
        val nowArmed = abs(travel) >= thresholdPx
        val crossed = nowArmed && !armed
        armed = nowArmed
        return crossed
    }

    /** -1 = the period before this one, +1 = the period after, 0 = the swipe was not a command. */
    fun commit(): Int = when {
        // Swiping left moves forward in time, the direction the page slides when it changes.
        travel <= -thresholdPx -> 1
        travel >= thresholdPx -> -1
        else -> 0
    }

    /** Back to rest. Called on release and whenever the gesture is cancelled. */
    fun reset() {
        travel = 0f
        offset = 0f
        armed = false
    }
}

/**
 * Swipe sideways to move one period through time — the gesture every calendar has, and the reason the
 * date arrows can stay small.
 *
 * Two rules keep it from fighting the rest of the screen. It waits for the *horizontal* touch slop,
 * so a list that scrolls vertically keeps its gesture and this modifier simply lets go. And it only
 * applies where the caller enables it: the day, week and month scales, never while Goals owns the
 * screen or a sheet owns the window.
 */
@Composable
fun Modifier.swipeToShift(
    enabled: Boolean,
    onShift: (Int) -> Unit,
    threshold: Dp = SwipeShiftThreshold,
): Modifier {
    val density = LocalDensity.current
    val reduceMotion = LocalReduceMotion.current
    val haptics = LocalRoutineHaptics.current
    val currentOnShift by rememberUpdatedState(onShift)
    val currentHaptics by rememberUpdatedState(haptics)
    val tracker = remember(density, threshold) {
        SwipeShiftTracker(
            thresholdPx = with(density) { threshold.toPx() },
            maxOffsetPx = with(density) { 72.dp.toPx() },
        )
    }
    return this
        // Read in the draw phase: the finger drags the layer, the composition stays still.
        .graphicsLayer { translationX = if (reduceMotion) 0f else tracker.offset }
        .pointerInput(enabled, tracker) {
            if (!enabled) return@pointerInput
            try {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val start = awaitHorizontalTouchSlopOrCancellation(down.id) { change, overSlop ->
                        change.consume()
                        if (tracker.add(overSlop)) currentHaptics.selection()
                    }
                    if (start == null) {
                        tracker.reset()
                        return@awaitEachGesture
                    }
                    // false means another recognizer consumed the gesture, not a completed swipe.
                    val released = horizontalDrag(start.id) { change ->
                        val delta = change.positionChange().x
                        change.consume()
                        if (tracker.add(delta)) currentHaptics.selection()
                    }
                    val direction = if (released) tracker.commit() else 0
                    tracker.reset()
                    if (direction != 0) currentOnShift(direction)
                }
            } finally {
                // Also covers disabling/removing the modifier while the pointer is still down.
                tracker.reset()
            }
        }
}
