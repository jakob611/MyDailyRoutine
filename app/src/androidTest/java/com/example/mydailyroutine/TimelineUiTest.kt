package com.example.mydailyroutine

import android.content.pm.PackageManager
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
        compose.onNodeWithContentDescription(text(R.string.settings)).performClick()
        awaitText(R.string.settings_title)
        compose.activityRule.scenario.onActivity { activity -> activity.startActivity(MainActivity.fastAddIntent(activity)) }
        awaitText(R.string.fast_add_title)
        compose.onNodeWithText(text(R.string.settings_title)).assertDoesNotExist()
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
