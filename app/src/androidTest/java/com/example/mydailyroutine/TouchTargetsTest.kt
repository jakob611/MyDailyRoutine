package com.example.mydailyroutine

import androidx.annotation.StringRes
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Every control the finger meets has to be at least 48 dp in **both** directions, and the test
 * measures the touch bounds the framework actually uses, not the size the pane is painted at.
 *
 * This is the reason the glass buttons have a 48 dp invisible box around a 40 dp pane: a control
 * that is drawn small may be hit generously, but a control that is drawn at 32 dp *and* hit at
 * 32 dp is the failure mode this locks out — in the settings list a missed tap lands on the next
 * row and flips a different switch.
 */
@RunWith(AndroidJUnit4::class)
class TouchTargetsTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Before fun answerFirstRunIfShown() = compose.passOnboarding()

    private fun text(@StringRes id: Int) = compose.activity.getString(id)

    /** Fails with the measured size in dp, so a regression report says "40x40 dp" instead of "false". */
    private fun assertTouchTarget(what: String, node: SemanticsNode) {
        val density = compose.activity.resources.displayMetrics.density
        val width = node.touchBoundsInRoot.width / density
        val height = node.touchBoundsInRoot.height / density
        assertTrue("$what is ${width}x$height dp, expected at least 48x48 dp", width >= 47.5f && height >= 47.5f)
    }

    @Test fun theChromeControlsAndTheModeTabsAreHittable() {
        compose.waitUntil(10000) { compose.onAllNodesWithTag("fast-add").fetchSemanticsNodes().isNotEmpty() }
        listOf(R.string.planning_open, R.string.tasks_open, R.string.goals_open, R.string.settings).forEach { id ->
            assertTouchTarget("top bar button \"${text(id)}\"", compose.onNodeWithContentDescription(text(id)).fetchSemanticsNode())
        }
        assertTouchTarget("the add pill", compose.onNodeWithTag("fast-add").fetchSemanticsNode())
        listOf(R.string.nav_day, R.string.nav_week, R.string.nav_month, R.string.nav_year).forEach { id ->
            assertTouchTarget("the \"${text(id)}\" tab", compose.onNodeWithText(text(id)).fetchSemanticsNode())
        }
    }

    @Test fun everyTargetInTheLowerHalfOfTheScreenIsHittable() {
        // The thumb's half of the screen is where a miss is most expensive: the day's own cards, the
        // add pill and the actions inside a card all live there. Every clickable node whose centre is
        // below the middle of the window is measured — and printed, so the CI log carries the numbers
        // that were measured rather than only the one that failed (N18).
        compose.waitUntil(10000) { compose.onAllNodesWithTag("fast-add").fetchSemanticsNodes().isNotEmpty() }
        val window = compose.onRoot().fetchSemanticsNode().boundsInRoot
        val density = compose.activity.resources.displayMetrics.density
        val lowerHalf = window.top + window.height / 2f
        val clickable = compose.onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsActions.OnClick)).fetchSemanticsNodes()
        val lower = clickable.filter {
            it.touchBoundsInRoot.height > 0f && it.touchBoundsInRoot.center.y >= lowerHalf
        }
        assertTrue("nothing tappable in the lower half of the day: ${clickable.size} clickable nodes", lower.isNotEmpty())
        val measured = lower.joinToString(" · ") { node ->
            val width = node.touchBoundsInRoot.width / density
            val height = node.touchBoundsInRoot.height / density
            "%.0fx%.0f".format(width, height)
        }
        println("ui-audit: lower-half touch targets (dp): $measured")
        lower.forEachIndexed { index, node -> assertTouchTarget("lower-half target #${index + 1}", node) }
    }

    @Test fun everySettingsSwitchIsAFullTouchTarget() {
        compose.onNodeWithContentDescription(text(R.string.settings)).performClick()
        compose.waitUntil(10000) { compose.onAllNodes(isToggleable()).fetchSemanticsNodes().isNotEmpty() }
        val switches = compose.onAllNodes(isToggleable()).fetchSemanticsNodes()
        assertTrue("the rhythm tab should offer switches", switches.size >= 2)
        switches.forEachIndexed { index, node -> assertTouchTarget("switch #${index + 1}", node) }
    }
}
