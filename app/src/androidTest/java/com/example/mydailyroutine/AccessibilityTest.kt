package com.example.mydailyroutine

import androidx.annotation.StringRes
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsConfiguration
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * What a screen reader finds, not what a screenshot shows.
 *
 * Three things make this app usable without sight, and each of them is a contract rather than a
 * feature: a screen has headings to jump between, a number is announced together with the label it
 * belongs to, and the action a reader most often wants on a block ("done", or move it a quarter of
 * an hour) is reachable from the card itself without opening anything. None of that is visible in a
 * screenshot, which is exactly why it needs a test of its own.
 */
@RunWith(AndroidJUnit4::class)
class AccessibilityTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Before fun answerFirstRunIfShown() = compose.passOnboarding()

    private fun text(@StringRes id: Int) = compose.activity.getString(id)

    private val isHeading = SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading)

    private fun actionableBlocks() = compose.onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsActions.CustomActions))
        .fetchSemanticsNodes()

    /** The custom actions a node exposes, read the way a screen reader reads them. */
    private fun actionsOf(config: SemanticsConfiguration): List<CustomAccessibilityAction> =
        config.getOrElseNullable(SemanticsActions.CustomActions) { null }.orEmpty()

    /** Waits for a piece of text, then insists the node carrying it is a heading and not just bold. */
    private fun awaitHeading(@StringRes id: Int) {
        compose.waitUntil(10000) {
            compose.onAllNodesWithText(text(id)).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText(text(id)).assert(isHeading)
    }

    @Test fun everyScreenHasAHeadingToJumpTo() {
        awaitHeading(R.string.day_heading)
        compose.onNodeWithText(text(R.string.nav_week)).performClick()
        awaitHeading(R.string.week_heading)
        compose.onNodeWithText(text(R.string.nav_month)).performClick()
        awaitHeading(R.string.month_heading)
        // A panel has to say what it is for as well: its title is the first thing a reader hears.
        compose.onNodeWithText(text(R.string.nav_day)).performClick()
        awaitHeading(R.string.day_heading)
        compose.onNodeWithContentDescription(text(R.string.settings)).performClick()
        awaitHeading(R.string.settings_title)
    }

    @Test fun theGoalsScreenOpensOnAHeading() {
        compose.onNodeWithContentDescription(text(R.string.goals_open)).performClick()
        awaitHeading(R.string.goals_title)
    }

    @Test fun aNumberIsAnnouncedWithTheLabelItBelongsTo() {
        // "3/9" on a summary tile means nothing on its own; the tile states "Opravljeno: 3/9"
        // instead, and the same holds for the two durations beside it.
        val stated = compose.onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsProperties.StateDescription))
            .fetchSemanticsNodes()
            .mapNotNull { it.config.getOrElseNullable(SemanticsProperties.StateDescription) { null } }
        listOf(R.string.metric_focus, R.string.metric_recovery, R.string.metric_completed).forEach { id ->
            assertTrue(
                "no tile states the value of ${text(id)} among $stated",
                stated.any { it.contains(text(id)) },
            )
        }
    }

    @Test fun aTimeFieldStatesWhichTimeItHolds() {
        // "08:45" alone does not say which end of the block it is, so the field states the label with
        // the value and the sheet reads as "Začetek: 08:45" rather than as a bare number.
        compose.onNodeWithTag("fast-add").performClick()
        awaitHeading(R.string.fast_add_title)
        val start = compose.onNodeWithTag("entry-start", useUnmergedTree = true).fetchSemanticsNode()
        val description = start.config.getOrElseNullable(SemanticsProperties.StateDescription) { null }
        assertTrue("the start field states \"$description\"", description?.contains(text(R.string.entry_start)) == true)
    }

    @Test fun aBlockOffersDoneAndMoveToAScreenReader() {
        compose.onNodeWithTag("fast-add").performClick()
        awaitHeading(R.string.fast_add_title)
        // A preset is the shortest honest route to a real block: it fills the sheet and saving keeps it.
        compose.onNodeWithText(text(R.string.preset_deep_work)).performClick()
        compose.onNodeWithText(text(R.string.entry_save)).performClick()
        compose.waitUntil(10000) { actionableBlocks().isNotEmpty() }
        val labels = actionableBlocks().flatMap { node -> actionsOf(node.config).map { it.label } }
        assertTrue(
            "a block offered $labels",
            labels.containsAll(
                listOf(text(R.string.mark_done), text(R.string.a11y_move_later), text(R.string.a11y_move_earlier)),
            ),
        )
    }
}
