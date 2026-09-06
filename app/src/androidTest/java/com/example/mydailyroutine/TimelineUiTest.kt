package com.example.mydailyroutine

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TimelineUiTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun allFourViewsAndFastAddAreReachable() {
        compose.onNodeWithText("My Daily Routine").assertIsDisplayed()
        compose.onNodeWithText("Week").performClick()
        awaitText("Your week, at a glance")
        compose.onNodeWithText("Month").performClick()
        awaitText("Build a rhythm, not a streak.")
        compose.onNodeWithText("Year").performClick()
        awaitText("THE BIG PICTURE")
        compose.onNodeWithText("Day").performClick()
        compose.onNodeWithText("Add block").performClick()
        awaitText("Make a little space.")
        compose.onNodeWithText("90 min Deep Work").assertIsDisplayed()
    }

    @Test fun settingsAreAvailableWithoutGrantingAnyPermission() {
        compose.onNodeWithContentDescription("Settings").performClick()
        awaitText("Your rhythm")
        compose.onNodeWithText("Entirely offline. No account, network access, analytics, or cloud backup.").assertIsDisplayed()
    }

    private fun awaitText(text: String) {
        compose.waitUntil(timeoutMillis = 10000) { compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText(text).assertIsDisplayed()
    }
}
