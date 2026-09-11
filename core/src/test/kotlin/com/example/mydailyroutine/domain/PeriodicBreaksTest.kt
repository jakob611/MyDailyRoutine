package com.example.mydailyroutine.domain.health

import org.junit.Assert.assertEquals
import org.junit.Test

class PeriodicBreaksTest {

    @Test
    fun `block shorter than the interval needs no breaks`() {
        assertEquals(emptyList<MinuteInterval>(), PeriodicBreaks.plan(MinuteInterval(0, 50), everyMinutes = 60, breakMinutes = 5))
    }

    @Test
    fun `single break inserted inside a two hour block`() {
        val breaks = PeriodicBreaks.plan(MinuteInterval(0, 120), everyMinutes = 60, breakMinutes = 5)
        assertEquals(listOf(MinuteInterval(60, 65)), breaks)
    }

    @Test
    fun `respects remaining room before the block end`() {
        // 09:00-11:00 (120 min), break every 60 with 15 min: first at 10:00-10:15,
        // the next candidate (11:15) would spill past the end, so it is dropped.
        val breaks = PeriodicBreaks.plan(MinuteInterval(540, 660), everyMinutes = 60, breakMinutes = 15)
        assertEquals(listOf(MinuteInterval(600, 615)), breaks)
    }

    @Test
    fun `two breaks when there is room`() {
        // 09:00-12:00 (180 min), break every 60 with 5 min: 10:00-10:05, 11:05-11:10.
        val breaks = PeriodicBreaks.plan(MinuteInterval(540, 720), everyMinutes = 60, breakMinutes = 5)
        assertEquals(
            listOf(MinuteInterval(600, 605), MinuteInterval(665, 670)),
            breaks,
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun `rejects an out-of-range interval`() {
        PeriodicBreaks.plan(MinuteInterval(0, 120), everyMinutes = 10, breakMinutes = 5)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `rejects an out-of-range break length`() {
        PeriodicBreaks.plan(MinuteInterval(0, 240), everyMinutes = 60, breakMinutes = 90)
    }
}
