package com.example.mydailyroutine.domain

import com.example.mydailyroutine.domain.health.*
import org.junit.Assert.*
import org.junit.Test

class IntervalsTest {
    @Test fun `union merges touching nested and overlapping intervals`() {
        assertEquals(listOf(MinuteInterval(0, 30), MinuteInterval(40, 60)), Intervals.union(listOf(
            MinuteInterval(10, 20), MinuteInterval(0, 15), MinuteInterval(20, 30), MinuteInterval(40, 60),
        )))
    }
    @Test fun `subtract handles complete partial and multiple overlaps`() {
        assertEquals(listOf(MinuteInterval(10, 20), MinuteInterval(30, 40)), Intervals.subtract(
            listOf(MinuteInterval(0, 60)), listOf(MinuteInterval(0, 10), MinuteInterval(20, 30), MinuteInterval(40, 70)),
        ))
    }
    @Test fun `weekly overlap lanes are distinct and released at touching boundary`() {
        val positioned = WeeklyLayout.position(listOf(block(480, 600), block(500, 550, id = 2), block(600, 650)))
        assertEquals(2, positioned[0].laneCount)
        assertNotEquals(positioned[0].lane, positioned[1].lane)
        assertEquals(1, positioned[2].laneCount)
        assertEquals(0, positioned[2].lane)
    }
    @Test fun `daily metrics union overlaps`() {
        assertEquals(120, ScheduleMetrics.forDay(listOf(block(480, 600), block(490, 550))).focusMinutes)
    }
}
