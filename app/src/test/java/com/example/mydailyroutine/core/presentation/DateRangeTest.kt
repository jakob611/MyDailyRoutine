package com.example.mydailyroutine.core.presentation

import java.time.LocalDate
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * When a period may be written with one month name. The header of the week view has room for four
 * short pieces, not five, so "21. sep – 27. sep" has to become "21.–27. sep" exactly when both ends
 * share a month — never across a month or a year, where the name would then lie.
 */
class DateRangeTest {
    @Test fun aWeekInsideOneMonthSharesItsName() {
        assertTrue(sharesOneMonth(LocalDate.of(2026, 9, 21), LocalDate.of(2026, 9, 27)))
    }

    @Test fun aWeekThatCrossesAMonthKeepsBothNames() {
        assertFalse(sharesOneMonth(LocalDate.of(2026, 9, 28), LocalDate.of(2026, 10, 4)))
    }

    @Test fun theSameMonthInAnotherYearIsStillTwoDates() {
        // A range from December to December of the next year must not lose the second year's name.
        assertFalse(sharesOneMonth(LocalDate.of(2026, 12, 28), LocalDate.of(2027, 1, 3)))
        assertFalse(sharesOneMonth(LocalDate.of(2026, 12, 1), LocalDate.of(2027, 12, 31)))
    }
}
