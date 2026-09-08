package com.example.mydailyroutine

import android.content.pm.PackageManager
import android.content.Intent
import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import java.io.File
import androidx.lifecycle.Lifecycle
import androidx.test.platform.app.InstrumentationRegistry
import androidx.annotation.StringRes
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
class TimelineUiTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private fun text(@StringRes id: Int) = compose.activity.getString(id)
    private fun click(@StringRes id: Int) = compose.onNodeWithText(text(id)).performClick()

    @Test fun allFourSlovenianViewsAndFastAddAreReachable() {
        compose.onNodeWithText(text(R.string.app_name)).assertIsDisplayed()
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
    @Test fun settingsExposeAdvancedRulesAndOptInDemo() {
        compose.onNodeWithContentDescription(text(R.string.settings)).performClick()
        awaitText(R.string.settings_title)
        compose.onNodeWithText(text(R.string.advanced_settings)).performScrollTo().performClick()
        compose.onNodeWithText(text(R.string.threshold_focus)).performScrollTo().assertIsDisplayed()
        compose.onNodeWithText(text(R.string.demo_heading)).performScrollTo().assertIsDisplayed()
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
        assertEquals("sl", compose.activity.resources.configuration.locales[0].language)
        @Suppress("DEPRECATION") val permissions = compose.activity.packageManager.getPackageInfo(compose.activity.packageName, PackageManager.GET_PERMISSIONS).requestedPermissions.orEmpty()
        assertFalse("android.permission.INTERNET" in permissions)
        assertFalse("android.permission.ACCESS_NETWORK_STATE" in permissions)
    }
    @Test fun changingSchoolStartAutomaticallyMaintainsItsDuration() {
        compose.onNodeWithTag("fast-add").performClick()
        awaitText(R.string.fast_add_title)
        compose.onNodeWithText(text(R.string.category_school)).performScrollTo().performClick()
        compose.onNodeWithTag("entry-start").performScrollTo().performTextReplacement("08:00")
        compose.onNodeWithTag("entry-end").assertTextContains("08:45")
        compose.onNodeWithTag("entry-end").performTextReplacement("08:50")
        compose.onNodeWithTag("entry-start").performTextReplacement("09:00")
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
    private fun capture(name: String, editor: Boolean = false) {
        val directory = File(compose.activity.getExternalFilesDir(null), "ui-audit").apply { mkdirs() }
        val image = if (editor) compose.onNodeWithTag("entry-editor",useUnmergedTree=true).captureToImage()
            else compose.onRoot(useUnmergedTree=true).captureToImage()
        File(directory,"$name.png").outputStream().use { image.asAndroidBitmap().compress(Bitmap.CompressFormat.PNG,100,it) }
    }
    private fun awaitText(@StringRes id: Int) {
        compose.waitUntil(10000) { compose.onAllNodesWithText(text(id)).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText(text(id)).assertIsDisplayed()
    }
}
