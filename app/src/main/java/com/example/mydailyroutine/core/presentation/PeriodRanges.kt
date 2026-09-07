package com.example.mydailyroutine.core.presentation

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.TemporalAdjusters

object PeriodRanges {
    fun range(date: LocalDate, mode: TimelineMode): Pair<LocalDate, LocalDate> = when(mode) {
        TimelineMode.DAY -> date to date
        TimelineMode.WEEK -> date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).let { it to it.plusDays(6) }
        TimelineMode.MONTH -> YearMonth.from(date).atDay(1).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).let { it to it.plusDays(41) }
        TimelineMode.YEAR -> LocalDate.of(if (date.monthValue >= 9) date.year else date.year - 1, 9, 1).let { it to it.plusYears(1).minusDays(1) }
    }
}
