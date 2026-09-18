package com.example.mydailyroutine.core.designsystem

import com.example.mydailyroutine.core.designsystem.components.wheelCenterIndex
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Geometry of the iOS-style time drum: five 44 px rows in a 220 px viewport, so the selection row is
 * the one whose centre meets the viewport centre. Pinned here because the picker confirms exactly
 * what this function reports — if it ever picks a neighbour, the wheel silently lies.
 */
class WheelMathTest {
    private val sizes = listOf(44, 44, 44, 44, 44)

    @Test fun theRowInTheMiddleWindowIsSelected() {
        // rows 6..10 at offsets 0,44,88,132,176; viewport centre 110 -> row 8 (centre 110).
        assertEquals(8, wheelCenterIndex(listOf(0, 44, 88, 132, 176), sizes, listOf(6, 7, 8, 9, 10), 110))
    }

    @Test fun aDragSettlingHalfwayPicksTheNearerRow() {
        // ten pixels short of resting on row 9: centre 100 beats centre 144 for the 110 window.
        assertEquals(9, wheelCenterIndex(listOf(-10, 34, 78, 122, 166), sizes, listOf(7, 8, 9, 10, 11), 110))
    }

    @Test fun tiesGoToTheEarlierRow() {
        // centres 22,66,110,154,198; viewport centre 88 sits 22 px from both 66 and 110 -> earlier row.
        assertEquals(4, wheelCenterIndex(listOf(0, 44, 88, 132, 176), sizes, listOf(3, 4, 5, 6, 7), 88))
    }

    @Test fun anEmptyDrumSelectsNothing() {
        assertNull(wheelCenterIndex(emptyList(), emptyList(), emptyList(), 110))
    }
}
