package com.example.mydailyroutine

import android.content.ContentValues
import android.content.pm.PackageManager
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import java.io.File
import androidx.lifecycle.Lifecycle
import com.example.mydailyroutine.core.platform.uiLocale
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.test.platform.app.InstrumentationRegistry
import androidx.annotation.StringRes
import androidx.compose.ui.test.*
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
class TimelineUiTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    @Before fun answerFirstRunIfShown() = compose.passOnboarding()

    private fun text(@StringRes id: Int) = compose.activity.getString(id)
    private fun click(@StringRes id: Int) = compose.onNodeWithText(text(id)).performClick()

    @Test fun allFourSlovenianViewsAndFastAddAreReachable() {
        // The expanded top bar carries the LockIn logo as an image, so the brand is asserted by
        // its content description instead of a text node.
        compose.onNodeWithContentDescription(text(R.string.app_name)).assertIsDisplayed()
        awaitText(R.string.day_heading); capture("01-day")
        click(R.string.nav_week); awaitText(R.string.week_heading); capture("02-week")
        click(R.string.nav_month); awaitText(R.string.month_heading); capture("03-month")
        click(R.string.nav_year); awaitText(R.string.year_big_picture); capture("04-year")
        click(R.string.nav_day); compose.onNodeWithTag("fast-add").performClick(); awaitText(R.string.fast_add_title)
        compose.onNodeWithText(text(R.string.preset_deep_work)).assertIsDisplayed()
        compose.onNodeWithText(text(R.string.entry_save)).assertIsDisplayed()
        compose.onNodeWithText(text(R.string.elasticity)).assertDoesNotExist()
        capture("05-quick-add",true)
    }
    @Test fun settingsTabsExposeRulesAndOptInDemo() {
        compose.onNodeWithContentDescription(text(R.string.settings)).performClick()
        awaitText(R.string.settings_title)
        // The landing tab is rhythm: sleep and lesson defaults, not a 40-item scroll.
        compose.onNodeWithText(text(R.string.sleep_heading)).performScrollTo().assertIsDisplayed()
        compose.onNodeWithText(text(R.string.demo_heading)).assertDoesNotExist()
        // The tab strip scrolls sideways since N10 (five chips no longer fit a phone in one line, and
        // they must not wrap into a 3+2 block). A test therefore has to bring the chip into view before
        // it taps it — exactly what a finger does — instead of clicking a coordinate off the screen.
        compose.onNodeWithTag("settings-tab-rules").performScrollTo().performClick()
        // Health thresholds are no longer hidden behind an "advanced" expander.
        compose.onNodeWithText(text(R.string.threshold_focus)).performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("settings-tab-data").performScrollTo().performClick()
        compose.onNodeWithText(text(R.string.demo_heading)).performScrollTo().assertIsDisplayed()
        compose.onNodeWithText(text(R.string.threshold_focus)).assertDoesNotExist()
        captureTag("06-settings-data", "settings-sheet")
        // The whole sheet, from the tab strip down: the five tabs are the thing the audit reads.
        captureTag("12-settings", "settings-sheet")
    }
    @Test fun menusAreSplitIntoTabsInsteadOfOneLongScroll() {
        compose.onNodeWithContentDescription(text(R.string.planning_open)).performClick()
        awaitText(R.string.planning_title)
        compose.onNodeWithText(text(R.string.add_reserve)).assertIsDisplayed()
        compose.onNodeWithText(text(R.string.new_topic)).assertDoesNotExist()
        compose.onNodeWithTag("planning-tab-topics").performClick()
        compose.onNodeWithText(text(R.string.new_topic)).assertIsDisplayed()
        compose.onNodeWithText(text(R.string.add_reserve)).assertDoesNotExist()
        compose.onNodeWithTag("planning-tab-markers").performClick()
        compose.onNodeWithText(text(R.string.new_topic)).assertDoesNotExist()
        captureTag("07-planning-tabs", "planning-sheet")
    }
    @Test fun longSlovenianButtonLabelsStayOnOneLine() {
        compose.onNodeWithTag("fast-add").performClick()
        awaitText(R.string.fast_add_title)
        compose.onNodeWithText(text(R.string.category_school)).performScrollTo().performClick()
        compose.onNodeWithTag("repeat-weekly").performScrollTo().performClick()
        compose.onNodeWithTag("save-next-lesson").assertIsDisplayed()
        // A 52dp pill whose label wrapped would grow past one text line; catch that regression here.
        // boundsInRoot is in pixels, so the limits are scaled by the device density.
        val density = compose.activity.resources.displayMetrics.density
        val sticky = compose.onNodeWithTag("save-next-lesson").fetchSemanticsNode().boundsInRoot
        assertTrue("sticky footer button grew to ${'$'}{sticky.height}px", sticky.height <= 56f * density)
        val label = compose.onNodeWithText(text(R.string.save_next_lesson), useUnmergedTree = true)
            .fetchSemanticsNode().boundsInRoot
        assertTrue("button label wrapped onto ${'$'}{label.height}px", label.height <= 32f * density)
    }
    @Test fun warmWidgetQuickAddDismissesSettingsAndOpensOneFreshSheet() {
        val activity = compose.activity
        val originalIntent = Intent(activity.intent)
        compose.onNodeWithContentDescription(text(R.string.settings)).performClick()
        awaitText(R.string.settings_title)
        try {
            compose.activityRule.scenario.onActivity { it.startActivity(MainActivity.fastAddIntent(it)) }
            awaitText(R.string.fast_add_title)
            compose.onNodeWithText(text(R.string.settings_title)).assertDoesNotExist()
            compose.runOnIdle { assertEquals(Lifecycle.State.RESUMED, activity.lifecycle.currentState) }
        } finally {
            // ActivityScenario filters lifecycle callbacks by the original action/data. A correct
            // onNewIntent calls setIntent, so restore only the harness intent before rule teardown.
            // Do not remove production setIntent or weaken the actual route/lifecycle assertions.
            InstrumentationRegistry.getInstrumentation().runOnMainSync { activity.intent = originalIntent }
        }
    }
    @Test fun languageAndMergedPrivacyPermissionsAreCorrect() {
        // The app speaks the reader's chosen language, or the device's language where no choice
        // exists — English device means English, everything else Slovenian, because Slovenian is
        // the resource set that ships as the default. What is asserted here is that the activity
        // really runs under the language the app resolved — the completeness of both translations
        // is what the translation gate checks on every build.
        val expected = uiLocale().language
        assertTrue("unexpected interface language: $expected", expected in setOf("sl", "en"))
        assertEquals(expected, compose.activity.resources.configuration.locales[0].language)
        @Suppress("DEPRECATION") val permissions = compose.activity.packageManager.getPackageInfo(compose.activity.packageName, PackageManager.GET_PERMISSIONS).requestedPermissions.orEmpty()
        assertFalse("android.permission.INTERNET" in permissions)
        assertFalse("android.permission.ACCESS_NETWORK_STATE" in permissions)
    }
    /** Spins an iOS-style time drum to an exact value: open the wheel, centre both rows, confirm. */
    private fun setWheel(tag: String, hour: Int, minute: Int) {
        compose.onNodeWithTag(tag).performScrollTo().performClick()
        compose.onNodeWithTag("$tag-hour").performScrollToNode(hasTestTag("$tag-hour-$hour"))
        compose.onNodeWithTag("$tag-hour-$hour").performClick()
        compose.onNodeWithTag("$tag-minute").performScrollToNode(hasTestTag("$tag-minute-$minute"))
        compose.onNodeWithTag("$tag-minute-$minute").performClick()
        compose.onNodeWithTag("$tag-confirm").performClick()
    }
    @Test fun changingSchoolStartAutomaticallyMaintainsItsDuration() {
        compose.onNodeWithTag("fast-add").performClick()
        awaitText(R.string.fast_add_title)
        compose.onNodeWithText(text(R.string.category_school)).performScrollTo().performClick()
        setWheel("entry-start", 8, 0)
        compose.onNodeWithTag("entry-end").assertTextContains("08:45")
        setWheel("entry-end", 8, 50)
        setWheel("entry-start", 9, 0)
        compose.onNodeWithTag("entry-end").assertTextContains("09:50")
    }
    @Test fun multipleWeekdaysAndLessonBreakAreSelectable() {
        compose.onNodeWithTag("fast-add").performClick()
        awaitText(R.string.fast_add_title)
        compose.onNodeWithText(text(R.string.category_school)).performScrollTo().performClick()
        compose.onNodeWithTag("lesson-break-toggle").performScrollTo().performClick()
        compose.onNodeWithTag("lesson-break-toggle").assertIsOn()
        compose.onNodeWithTag("repeat-weekly").performScrollTo().performClick()
        compose.onNodeWithText(text(R.string.weekdays_workdays)).performScrollTo().performClick()
        for(day in 1..5) compose.onNodeWithTag("weekday-$day").assertIsSelected()
        compose.onNodeWithTag("weekday-6").assertIsNotSelected()
        compose.onNodeWithTag("save-next-lesson").assertIsDisplayed()
    }
    /**
     * The floating chrome has to overlap what it refracts. A Scaffold body starts below its top bar,
     * so this fails the moment the window goes back to being inset instead of full-bleed.
     */
    @Test fun glassChromeOverlaysTheContentInsteadOfPushingItDown() {
        awaitText(R.string.day_heading)
        compose.onNodeWithTag("app-top-bar").assertIsDisplayed()
        val bar = compose.onNodeWithTag("app-top-bar").fetchSemanticsNode().boundsInRoot
        val list = compose.onNodeWithTag("day-list").fetchSemanticsNode().boundsInRoot
        assertTrue("top bar collapsed to ${'$'}{bar.height}px", bar.height > 0f)
        // The container still runs under the glass, which is the point of the floating island: a list
        // that stopped below the bar would leave a visible band of nothing behind it.
        assertTrue("day list starts at ${'$'}{list.top}px, below the bar at ${'$'}{bar.bottom}px", list.top < bar.bottom)
        // What must *not* be under the glass is the reading. The bar is measured from the outside of
        // its insets now, so the first line of the day sits below its bottom edge even while the
        // island is open — this is the assertion that fails if the inset is measured inside the
        // status bar padding again.
        val firstLine = compose.onNodeWithText(text(R.string.day_heading)).fetchSemanticsNode().boundsInRoot
        assertTrue(
            "first line top at ${'$'}{firstLine.top}px is under the open island (bottom ${'$'}{bar.bottom}px)",
            firstLine.top >= bar.bottom,
        )
        capture("05-glass-day")
    }
    /** One project selector plus four short tabs, instead of a single thousand-dp scroll. */
    @Test fun goalsAreSplitIntoTabsInsteadOfOneLongScroll() {
        compose.onNodeWithContentDescription(text(R.string.goals_open)).performClick()
        compose.waitUntil(10000) {
            nodeCount(hasTestTagPrefix("goal-seed-")) > 0 || nodeCount(hasTestTag("goal-project-all")) > 0 ||
                nodeCount(hasText(text(R.string.goals_empty_body))) > 0
        }
        // A store with no plan in it shows both starters at once: that screenshot is what the audit
        // reads for the goals entry. Later in the same run the screen is already past this state, and
        // the state it is in is a correct one — so it is read, never demanded.
        if (nodeCount(hasText(text(R.string.goals_empty_body))) > 0) {
            compose.onNodeWithText(text(R.string.goals_seed_cas)).assertIsDisplayed()
            compose.onNodeWithText(text(R.string.goals_seed_ee)).assertIsDisplayed()
            capture("08-goals")
        }
        // The starter that is still missing stays on offer after the first plan exists — the chip sits
        // in the project row, so a student who began with CAS can add EE a week later. The two coexist.
        seedBothPlansOnGoalsScreen()
        compose.onNodeWithTag("goal-seed-CAS").assertDoesNotExist()
        compose.onNodeWithTag("goal-seed-EE").assertDoesNotExist()
        compose.onNodeWithTag("goal-project-all").assertIsDisplayed()
        clickGoalTab("goal-tab-activities")
        compose.onNodeWithText(text(R.string.goals_add_activity)).performScrollTo().assertIsDisplayed()
        compose.onNodeWithText(text(R.string.goals_gantt)).assertDoesNotExist()
        clickGoalTab("goal-tab-progress")
        compose.onNodeWithText(text(R.string.goals_progress_log)).assertIsDisplayed()
        compose.onNodeWithText(text(R.string.goals_add_activity)).assertDoesNotExist()
        captureTag("09-goals-tabs", "goal-tab-body")
    }
    /**
     * CAS and EE are two plans that run at the same time for two years, so the screen must be able
     * to show both at once: one status card and one Gantt lane per plan, milestones from both in one
     * list, each row saying which plan it belongs to. The chip is the reader's way in.
     */
    @Test fun bothPlansAreVisibleAtOnce() {
        compose.onNodeWithContentDescription(text(R.string.goals_open)).performClick()
        seedBothPlansOnGoalsScreen()
        // Both plans exist: the "all plans" chip is offered, and it is what the screen opens on.
        compose.onNodeWithTag("goal-project-all").performScrollTo().assertIsDisplayed().assertIsSelected()
        // Overview: one status card per plan in the same list. Each is reached by scrolling the list,
        // which is what makes this a proof that the two live together instead of a lucky viewport.
        compose.onNodeWithTag("goal-tab-body").performScrollToNode(hasTestTagPrefix("goal-status-CAS-"))
        assertTrue("no status card for CAS", nodeCount(hasTestTagPrefix("goal-status-CAS-")) > 0)
        compose.onNodeWithTag("goal-tab-body").performScrollToNode(hasTestTagPrefix("goal-status-EE-"))
        assertTrue("no status card for EE", nodeCount(hasTestTagPrefix("goal-status-EE-")) > 0)
        captureTag("13-goals-both", "goal-tab-body")
        // Milestones from both plans in one list, each row prefixed with its plan's name.
        clickGoalTab("goal-tab-milestones")
        val list = compose.onNodeWithTag("goal-tab-body")
        list.performScrollToNode(hasText(text(R.string.goals_cas_meeting_1), substring = true))
        compose.onNodeWithText(text(R.string.goals_cas_meeting_1), substring = true).assertIsDisplayed()
        list.performScrollToNode(hasText(text(R.string.ee_milestone_1), substring = true))
        compose.onNodeWithText(text(R.string.ee_milestone_1), substring = true).assertIsDisplayed()
        captureTag("14-goals-both-milestones", "goal-tab-body")
    }
    /** A new activity can be pointed at the other plan from inside the editor, and the picker says so. */
    @Test fun aNewActivityCanBeFiledUnderTheOtherPlan() {
        compose.onNodeWithContentDescription(text(R.string.goals_open)).performClick()
        seedBothPlansOnGoalsScreen()
        clickGoalTab("goal-tab-activities")
        compose.onNodeWithText(text(R.string.goals_add_activity)).performScrollTo().performClick()
        awaitText(R.string.goals_new_activity)
        // The sheet says where the row will land and lets the reader point it at the other plan.
        compose.onNodeWithText(text(R.string.goals_project_label)).assertIsDisplayed()
        compose.onNodeWithTag("goal-target-CAS").performScrollTo().performClick()
        // A name of its own, so a rerun of this test on the same device cannot match two rows.
        val title = "CAS nastop " + System.currentTimeMillis() % 100000
        compose.onNodeWithTag("activity-title").performTextInput(title)
        compose.onNodeWithText(text(R.string.save)).performClick()
        // Saved into CAS, and listed among the activities of the plan the chip row names.
        compose.onNodeWithTag("goal-tab-body").performScrollToNode(hasText(title))
        compose.onNodeWithText(title).assertIsDisplayed()
        captureTag("15-goals-activity-plan", "goal-tab-body")
    }
    /**
     * The system back button walks the stack the reader built, innermost layer first: Goals closes
     * before the day does, and the day returns to the scale it was drilled into instead of leaving
     * the app. Sheets and dialogs are not tested here — they answer from their own window.
     */
    @Test fun systemBackClosesGoalsAndWalksOutOfTheDay() {
        awaitText(R.string.day_heading)
        click(R.string.nav_week); awaitText(R.string.week_heading)
        // The heading lands before the grid does: the day columns are a lazily composed item, so
        // waiting for them is what keeps this from indexing an empty collection mid-transition.
        compose.waitUntil(10000) { compose.onAllNodesWithTag("week-day-column").fetchSemanticsNodes().isNotEmpty() }
        compose.onAllNodesWithTag("week-day-column")[0].performClick()
        awaitText(R.string.day_heading)
        compose.onNodeWithContentDescription(text(R.string.goals_open)).performClick()
        // Not the empty state: `goalsAreSplitIntoTabsInsteadOfOneLongScroll` runs fourth in this
        // class and seeds a CAS project, and the Room database outlives the activity every rule
        // recreates, so by the time this test runs Goals has a project in it. What this test is
        // about is the layer, not its contents — and the top bar says which layer is up either way.
        awaitAnyText(R.string.goals_title)
        // First back: Goals closes and the day underneath is reachable again.
        pressBack()
        awaitText(R.string.day_heading)
        // Second back: the day unwinds to the week it was drilled into, not to the launcher.
        pressBack()
        awaitText(R.string.week_heading)
        capture("11-back-stack")
    }
    /** The year view keeps its countdown and folds the three long panels away. */
    @Test fun yearOverviewFoldsItsLongSections() {
        click(R.string.nav_year); awaitText(R.string.year_big_picture)
        compose.onNodeWithTag("year-month-grid").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("milestone-radar-chart").assertDoesNotExist()
        compose.onNodeWithTag("year-balance-toggle").performScrollTo().performClick()
        compose.onNodeWithTag("year-month-grid").assertDoesNotExist()
        compose.onNodeWithTag("milestone-radar-toggle").performScrollTo().performClick()
        compose.onNodeWithTag("milestone-radar-chart").assertIsDisplayed()
        capture("10-year-folds")
    }
    /**
     * Presses the system back button.
     *
     * The same call the system makes when the reader swipes back, sent through the activity's own
     * dispatcher: an Espresso key event needs a window with focus, and a headless emulator cannot
     * promise one, which made this the only test that failed for a reason that was not the app's.
     */
    private fun pressBack() {
        compose.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
        compose.waitForIdle()
    }

    /**
     * Opens a tab on the goals screen.
     *
     * The tab strip is disabled while a change is being saved, and the click right before this one is
     * a seed — a project being written — so a bare click can land in that window and go nowhere at
     * all. The test therefore waits for the tab to accept input, which is the honest form: the
     * disabled tab is correct behaviour, not a bug to click through.
     */
    /** Matches every test tag that starts with [prefix]; used to count repeated cards without ids. */
    private fun hasTestTagPrefix(prefix: String) = SemanticsMatcher("test tag starts with " + prefix) { node ->
        node.config.getOrNull(SemanticsProperties.TestTag)?.startsWith(prefix) == true
    }

    private fun nodeCount(matcher: SemanticsMatcher) = compose.onAllNodes(matcher).fetchSemanticsNodes().size

    /**
     * Leaves the goals screen with both starter plans in it, whatever the suite has built before.
     * The instrumentation run shares one app database, so the entry state is read rather than
     * demanded: the empty screen offers both starters as buttons, and a used one offers the chip for
     * whichever kind is still missing. Either way this ends with two plans and the "all plans" chip.
     */
    private fun seedBothPlansOnGoalsScreen() {
        val idle = {
            nodeCount(hasTestTagPrefix("goal-seed-")) > 0 || nodeCount(hasTestTag("goal-project-all")) > 0 ||
                nodeCount(hasText(text(R.string.goals_empty_body))) > 0
        }
        compose.waitUntil(10000) { idle() }
        if (nodeCount(hasTestTagPrefix("goal-seed-")) == 0 && nodeCount(hasTestTag("goal-project-all")) == 0) {
            compose.onNodeWithText(text(R.string.goals_seed_cas)).performClick()
        }
        // The chip for the missing kind appears once the write lands: waiting here is what makes the
        // coexistence a checked fact rather than a race the test happened to win.
        compose.waitUntil(10000) {
            nodeCount(hasTestTag("goal-project-all")) > 0 || nodeCount(hasTestTagPrefix("goal-seed-")) > 0
        }
        if (nodeCount(hasTestTag("goal-project-all")) == 0) {
            val missing = if (nodeCount(hasTestTag("goal-seed-CAS")) > 0) "goal-seed-CAS" else "goal-seed-EE"
            compose.onNodeWithTag(missing).performScrollTo().performClick()
        }
        compose.waitUntil(10000) { nodeCount(hasTestTag("goal-project-all")) > 0 }
    }

    private fun clickGoalTab(tag: String) {
        compose.waitUntil(10000) {
            runCatching { compose.onNodeWithTag(tag).assertIsEnabled() }.isSuccess
        }
        compose.onNodeWithTag(tag).performClick()
    }

    /** Screenshots a tagged node; used for sheets, which live in their own window. */
    private fun captureTag(name: String, tag: String) {
        save(name, compose.onNodeWithTag(tag, useUnmergedTree = true).captureToImage())
    }
    private fun capture(name: String, editor: Boolean = false) {
        val image = if (editor) compose.onNodeWithTag("entry-editor",useUnmergedTree=true).captureToImage()
            else compose.onRoot(useUnmergedTree=true).captureToImage()
        save(name, image)
    }
    /** Persists a screenshot where CI can still find it after the run; see [saveUiAudit]. */
    private fun save(name: String, image: ImageBitmap) = saveUiAudit(compose.activity, name, image)

    /**
     * Waits for a string to be **on screen**, not merely composed. During a transition the node
     * exists while it is still sliding in or fading up, and asserting `isDisplayed` the instant it
     * appears is what made these tests flaky on a slow emulator; retrying the assertion itself until
     * the transition settles is the honest form. Every string waited for this way is unique in the
     * tree — for anything that legitimately repeats, use [awaitAnyText].
     */
    private fun awaitText(@StringRes id: Int) {
        compose.waitUntil(10000) {
            compose.onAllNodesWithText(text(id)).fetchSemanticsNodes().isNotEmpty() &&
                runCatching { compose.onNodeWithText(text(id)).assertIsDisplayed() }.isSuccess
        }
    }

    /** Waits for a string that may appear more than once, such as a top bar title echoed in a heading. */
    private fun awaitAnyText(@StringRes id: Int) {
        compose.waitUntil(10000) { compose.onAllNodesWithText(text(id)).fetchSemanticsNodes().isNotEmpty() }
    }
}
