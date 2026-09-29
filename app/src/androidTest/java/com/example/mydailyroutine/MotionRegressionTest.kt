package com.example.mydailyroutine

import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.MotionDurationScale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.mydailyroutine.core.designsystem.components.RoutineSwitch
import com.example.mydailyroutine.core.designsystem.components.LiquidSlider
import com.example.mydailyroutine.core.designsystem.components.swipeToShift
import com.example.mydailyroutine.core.designsystem.glass.GlassTouch
import com.example.mydailyroutine.core.designsystem.glass.rememberGlassTouch
import com.example.mydailyroutine.core.designsystem.glass.routineGlassTouch
import com.example.mydailyroutine.core.designsystem.haptics.LocalRoutineHaptics
import com.example.mydailyroutine.core.designsystem.haptics.rememberRoutineHaptics
import com.example.mydailyroutine.core.designsystem.motion.LocalReduceMotion
import com.example.mydailyroutine.core.designsystem.theme.RoutineShapes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** In-flight behaviour, not just the final screenshot. No app/database/demo data is needed. */
@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
class MotionRegressionTest {
    // CI disables platform animations for functional tests. These tests explicitly need a real clock.
    @get:Rule val compose = createComposeRule(effectContext = object : MotionDurationScale {
        override val scaleFactor = 1f
    })

    @Test fun glassFramesDoNotRecomposeTheCallerAndReleaseGlowCanFade() {
        lateinit var touch: GlassTouch
        var compositions = 0
        compose.setContent {
            touch = rememberGlassTouch(reduceMotion = false)
            SideEffect { compositions++ }
            Box(Modifier.size(80.dp).routineGlassTouch(touch, RoutineShapes.Pill))
        }
        compose.mainClock.autoAdvance = false
        val press = PressInteraction.Press(Offset(20f, 20f))
        compose.runOnIdle { assertTrue(touch.source.tryEmit(press)) }
        compose.mainClock.advanceTimeBy(80)
        var framesStartedAt = 0
        compose.runOnIdle {
            assertTrue("Test must exercise a live animation", touch.press < 1f)
            framesStartedAt = compositions
        }
        compose.mainClock.advanceTimeBy(160)
        compose.runOnIdle {
            assertEquals("Frame values must be read in draw/layer, not composition", framesStartedAt, compositions)
            assertTrue(touch.source.tryEmit(PressInteraction.Release(press)))
        }
        compose.mainClock.advanceTimeBy(48)
        compose.runOnIdle {
            assertEquals(press.pressPosition, touch.point)
            assertTrue("Release must not cut off the glow", touch.glow > 0f && touch.glow < 1f)
        }
        compose.mainClock.advanceTimeBy(300)
        compose.runOnIdle { assertEquals(0f, touch.glow, 0.001f) }
    }

    @Test fun cancelledSwipeDoesNotNavigate() {
        var shifts = 0
        compose.setContent {
            CompositionLocalProvider(LocalRoutineHaptics provides rememberRoutineHaptics(false)) {
                Box(Modifier.fillMaxWidth().height(100.dp).testTag("swipe")
                    .swipeToShift(enabled = true, onShift = { shifts++ }, threshold = 20.dp))
            }
        }
        compose.onNodeWithTag("swipe").performTouchInput {
            down(Offset(width * 0.9f, centerY))
            moveBy(Offset(-width * 0.3f, 0f))
            moveBy(Offset(-width * 0.3f, 0f))
            cancel()
        }
        compose.runOnIdle { assertEquals(0, shifts) }
    }

    @Test fun swipeUsesLatestCallbackWithoutRestartingTheGesture() {
        var version by mutableIntStateOf(0)
        var calledVersion = -1
        compose.setContent {
            val current = version
            CompositionLocalProvider(LocalRoutineHaptics provides rememberRoutineHaptics(false)) {
                Box(Modifier.fillMaxWidth().height(100.dp).testTag("swipe")
                    .swipeToShift(enabled = true, onShift = { calledVersion = current }, threshold = 20.dp))
            }
        }
        compose.onNodeWithTag("swipe").performTouchInput {
            down(Offset(width * 0.9f, centerY))
            moveBy(Offset(-width * 0.3f, 0f))
        }
        compose.runOnIdle { version = 1 }
        compose.onNodeWithTag("swipe").performTouchInput {
            moveBy(Offset(-width * 0.3f, 0f))
            up()
        }
        compose.runOnIdle { assertEquals(1, calledVersion) }
    }

    @Test fun sliderUsesLatestCallbackAndExposesAccessibleProgress() {
        var version by mutableIntStateOf(0)
        var calledVersion = -1
        var progress = 0f
        compose.setContent {
            val current = version
            LiquidSlider(0.5f, { calledVersion = current; progress = it }, Modifier.testTag("slider"))
        }
        compose.runOnIdle { version = 1 }
        compose.onNodeWithTag("slider").performTouchInput { click(center) }
        compose.runOnIdle { assertEquals(1, calledVersion) }
        compose.onNodeWithTag("slider").performSemanticsAction(SemanticsActions.SetProgress) { it(2f) }
        compose.runOnIdle { assertEquals(1f, progress, 0f) }
    }

    @Test fun switchDragUsesLatestCheckedValue() {
        var checked by mutableStateOf(false)
        var result: Boolean? = null
        compose.setContent {
            RoutineSwitch(checked, { result = it }, Modifier.testTag("switch"))
        }
        // The gesture detector was installed while checked was false.
        compose.runOnIdle { checked = true }
        compose.waitForIdle()
        compose.onNodeWithTag("switch").performTouchInput {
            down(Offset(width * 0.8f, centerY))
            moveBy(Offset(-width * 0.3f, 0f))
            moveBy(Offset(-width * 0.5f, 0f))
            up()
        }
        compose.runOnIdle { assertEquals(false, result) }
    }

    @Test fun reducedMotionGlassSettlesWithoutATween() {
        lateinit var touch: GlassTouch
        compose.setContent {
            CompositionLocalProvider(LocalReduceMotion provides true) {
                touch = rememberGlassTouch(LocalReduceMotion.current)
                Box(Modifier.size(80.dp).routineGlassTouch(touch, RoutineShapes.Pill))
            }
        }
        val press = PressInteraction.Press(Offset.Zero)
        compose.runOnIdle { touch.source.tryEmit(press) }
        compose.waitForIdle()
        compose.runOnIdle { assertEquals(0.96f, touch.press, 0.001f) }
        compose.runOnIdle { touch.source.tryEmit(PressInteraction.Release(press)) }
        compose.waitForIdle()
        compose.runOnIdle {
            assertEquals(1f, touch.press, 0.001f)
            assertEquals(0f, touch.glow, 0.001f)
        }
    }
}
