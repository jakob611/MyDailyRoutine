package com.example.mydailyroutine

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import com.example.mydailyroutine.core.designsystem.components.swipeToShift
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class SwipeCancellationTest {
    @get:Rule val compose = createComposeRule()

    @Test fun cancelledArmedSwipeDoesNotNavigateAndNextSwipeStillWorks() {
        val shifts = mutableListOf<Int>()
        compose.setContent {
            Box(Modifier.size(300.dp).testTag("swipe")
                .swipeToShift(enabled = true, threshold = 10.dp, onShift = { shifts.add(it) }))
        }
        val node = compose.onNodeWithTag("swipe")
        node.performTouchInput {
            down(Offset(width * 0.9f, height * 0.5f))
            moveTo(Offset(width * 0.7f, height * 0.5f))
            moveTo(Offset(width * 0.2f, height * 0.5f))
            cancel()
        }
        compose.runOnIdle { assertEquals(emptyList<Int>(), shifts) }
        node.performTouchInput {
            down(Offset(width * 0.9f, height * 0.5f))
            moveTo(Offset(width * 0.7f, height * 0.5f))
            moveTo(Offset(width * 0.2f, height * 0.5f))
            up()
        }
        compose.runOnIdle { assertEquals(listOf(1), shifts) }
    }
}
