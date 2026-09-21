package com.example.mydailyroutine

import android.provider.Settings
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runner.RunWith
import org.junit.runners.model.Statement

/**
 * The evidence that the layout survives a small screen with large type: the device is put into that
 * state for real (a 320 dp wide display, font scale 1,5) and the screens are then photographed and
 * measured. Faking the density inside one composable would prove that a composable can be rendered,
 * not that this app is readable on the phone of someone who set their type big.
 *
 * The device is restored in a `finally`, so a failing test cannot leave the emulator in the state it
 * put it in — the later screenshot tests would otherwise all be narrow and huge.
 */
@RunWith(AndroidJUnit4::class)
class LargeFontUiTest {
    private val compose = createAndroidComposeRule<MainActivity>()

    @get:Rule
    val device: TestRule = RuleChain.outerRule(NarrowScreenWithLargeFont()).around(compose)

    private fun text(id: Int) = compose.activity.getString(id)

    @Test fun thePlannerStaysReadableAtLargeTypeOnANarrowScreen() {
        val configuration = compose.activity.resources.configuration
        // Printed so the CI log carries the state this ran in, and asserted so a device that ignored
        // the override fails loudly instead of producing a screenshot of an ordinary phone.
        println("ui-audit: fontScale=${configuration.fontScale} widthDp=${configuration.screenWidthDp} densityDpi=${configuration.densityDpi}")
        assertTrue("fontScale=${configuration.fontScale}", configuration.fontScale >= 1.45f)
        assertTrue("widthDp=${configuration.screenWidthDp}", configuration.screenWidthDp in 300..340)

        compose.waitUntil(10000) { compose.onAllNodesWithTag("fast-add").fetchSemanticsNodes().isNotEmpty() }
        capture("13-font-day")
        click(text(R.string.nav_week)); awaitText(text(R.string.week_heading)); capture("14-font-week")
        click(text(R.string.nav_month)); awaitText(text(R.string.month_heading)); capture("15-font-month")
        click(text(R.string.nav_day)); awaitText(text(R.string.day_heading))

        // Nothing is allowed to grow past the screen: a control that overflows its own width is how
        // large type turns into an unusable screen, and it is invisible in a screenshot's thumbnail.
        val root = compose.onNodeWithTag("app-top-bar").fetchSemanticsNode().boundsInRoot
        listOf(R.string.planning_open, R.string.tasks_open, R.string.goals_open, R.string.settings).forEach { id ->
            val bounds = compose.onNodeWithContentDescription(text(id)).fetchSemanticsNode().boundsInRoot
            assertTrue("${text(id)} starts at ${bounds.left}, the bar at ${root.left}", bounds.left >= root.left - 1f)
            assertTrue("${text(id)} ends at ${bounds.right}, the bar at ${root.right}", bounds.right <= root.right + 1f)
        }
        val pill = compose.onNodeWithTag("fast-add").fetchSemanticsNode().boundsInRoot
        val screenWidth = compose.onRoot().fetchSemanticsNode().boundsInRoot.right
        assertTrue("the add pill ends at ${pill.right}, the screen at $screenWidth", pill.right <= screenWidth + 1f)
    }

    private fun click(label: String) = compose.onNodeWithText(label).performClick()
    private fun capture(name: String) = saveUiAudit(compose.activity, name, compose.onRoot(useUnmergedTree = true).captureToImage())

    /** Waits for a string to be on screen, not merely composed; the transitions here are 200 ms. */
    private fun awaitText(label: String) {
        compose.waitUntil(10000) {
            compose.onAllNodesWithText(label).fetchSemanticsNodes().isNotEmpty() &&
                runCatching { compose.onNodeWithText(label).assertIsDisplayed() }.isSuccess
        }
    }
}

/**
 * Puts the emulator into a 320 dp wide screen at font scale 1,5 before the activity starts, and puts
 * it back afterwards. A 840 px wide display at density 420 is exactly 320 dp — the narrowest phone
 * this app is meant to be used on.
 */
private class NarrowScreenWithLargeFont : TestRule {
    override fun apply(base: Statement, description: Description): Statement = object : Statement() {
        override fun evaluate() {
            val context = InstrumentationRegistry.getInstrumentation().targetContext
            val previousFontScale = Settings.System.getFloat(context.contentResolver, Settings.System.FONT_SCALE, 1f)
            shell("settings put system font_scale 1.5")
            shell("wm size 840x1866")
            shell("wm density 420")
            // The system applies these asynchronously and the activity is launched right after this
            // statement runs; two seconds is the margin this emulator needed in practice.
            Thread.sleep(2000)
            try {
                base.evaluate()
            } finally {
                shell("settings put system font_scale $previousFontScale")
                shell("wm size reset")
                shell("wm density reset")
                Thread.sleep(500)
            }
        }
    }
}
