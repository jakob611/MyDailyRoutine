package com.example.mydailyroutine.features.goals.presentation

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Where two plans share a day, the month plan bands it in crimson.
 *
 * The bands used to be found by walking every day of the span and counting the windows over it —
 * seven hundred days for a two-year extended essay, walked again on every recomposition of the
 * chart, which a tap on any bar causes. The sweep that replaced the walk has one seam worth
 * pinning: a window that closes on the day another opens moves the depth down and up on the same
 * date, and reading those as two moments cuts one band in half exactly where the reader sees one.
 */
class GoalGanttOverlapTest {
    @Test fun windowsThatOnlyTouchShareNoDay() {
        assertEquals(
            noBands,
            overlapRunsOf(listOf(date(9, 1) to date(9, 5), date(9, 6) to date(9, 9))),
        )
    }

    @Test fun oneSharedDayIsItsOwnBand() {
        assertEquals(
            listOf(date(9, 5) to date(9, 5)),
            overlapRunsOf(listOf(date(9, 1) to date(9, 5), date(9, 5) to date(9, 9))),
        )
    }

    @Test fun twoIdenticalPlansBandTheWholeWindow() {
        assertEquals(
            listOf(date(9, 1) to date(9, 5)),
            overlapRunsOf(listOf(date(9, 1) to date(9, 5), date(9, 1) to date(9, 5))),
        )
    }

    @Test fun aHandoverDoesNotCutTheBandInHalf() {
        // The middle plan carries the band across 5-9 September; the third takes over on the sixth,
        // the day after the first ends. One band, not two meeting at a seam.
        assertEquals(
            listOf(date(9, 5) to date(9, 9)),
            overlapRunsOf(
                listOf(
                    date(9, 1) to date(9, 5),
                    date(9, 5) to date(9, 9),
                    date(9, 6) to date(9, 10),
                ),
            ),
        )
    }

    @Test fun aPlanInsideAnotherBandsTheInnerOne() {
        assertEquals(
            listOf(date(10, 1) to date(10, 10)),
            overlapRunsOf(listOf(date(9, 1) to date(12, 20), date(10, 1) to date(10, 10))),
        )
    }

    @Test fun threePlansBandFromTheSecondStartToTheSecondLastEnd() {
        assertEquals(
            listOf(date(9, 5) to date(9, 15)),
            overlapRunsOf(
                listOf(
                    date(9, 1) to date(9, 10),
                    date(9, 5) to date(9, 15),
                    date(9, 8) to date(9, 20),
                ),
            ),
        )
    }

    @Test fun twoBusySeasonsStayTwoBands() {
        assertEquals(
            listOf(date(9, 3) to date(9, 5), date(10, 3) to date(10, 5)),
            overlapRunsOf(
                listOf(
                    date(9, 1) to date(9, 5),
                    date(9, 3) to date(9, 20),
                    date(10, 1) to date(10, 5),
                    date(10, 3) to date(10, 9),
                ),
            ),
        )
    }

    @Test fun onePlanHasNothingToOverlap() {
        assertEquals(noBands, overlapRunsOf(emptyList()))
        assertEquals(noBands, overlapRunsOf(listOf(date(9, 1) to date(9, 30))))
    }

    private companion object {
        val noBands = emptyList<Pair<LocalDate, LocalDate>>()
        fun date(month: Int, day: Int): LocalDate = LocalDate.of(2025, month, day)
    }
}
