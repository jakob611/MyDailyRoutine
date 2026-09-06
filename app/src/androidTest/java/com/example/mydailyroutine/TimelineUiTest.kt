package com.example.mydailyroutine

import android.content.pm.PackageManager
import android.content.Intent
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

@RunWith(AndroidJUnit4::class)
class TimelineUiTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private fun text(@StringRes id: Int) = compose.activity.getString(id)
    private fun click(@StringRes id: Int) = compose.onNodeWithText(text(id)).performClick()

    @Test fun allFourSlovenianViewsAndFastAddAreReachable() {
        compose.onNodeWithText(text(R.string.app_name)).assertIsDisplayed()
        click(R.string.nav_week); awaitText(R.string.week_heading)
        click(R.string.nav_month); awaitText(R.string.month_heading)
        click(R.string.nav_year); awaitText(R.string.year_big_picture)
        click(R.string.nav_day); compose.onNodeWithTag("fast-add").performClick(); awaitText(R.string.fast_add_title)
        compose.onNodeWithText(text(R.string.preset_deep_work)).assertIsDisplayed()
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
    private fun awaitText(@StringRes id: Int) {
        compose.waitUntil(10000) { compose.onAllNodesWithText(text(id)).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText(text(id)).assertIsDisplayed()
    }
}
