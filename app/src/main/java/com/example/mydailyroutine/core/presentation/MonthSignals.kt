package com.example.mydailyroutine.core.presentation

/**
 * What a month cell says before it says anything about colour.
 *
 * The previous grid encoded *quantity* as colour intensity (`0.08 + heat * 0.35`), which turned a
 * month into a ranking: the fullest day was the darkest one, and a quiet Tuesday looked like a
 * verdict. The grid now splits the two jobs:
 *
 * * [MonthDayType] is the *kind* of day and is painted as one flat tint — three discrete steps plus
 *   the plain surface, never a ramp, so no day is louder than another just because it is fuller;
 * * [MonthCellSignals.bars] carries the *amount* of planned focus as one to three short marks
 *   (each bar is up to an hour), which stays readable with the colour switched off entirely;
 * * [MonthCellSignals.marks] carries the *events* as shapes, because a triangle and a diamond can be
 *   told apart by someone who cannot tell the two reds apart.
 *
 * The decisions live here, apart from the composable, so the rule "colour is never the only carrier"
 * can be tested without a device.
 */
enum class MonthDayType {
    /** Nothing planned: the surface stays as it is. A day is allowed to be empty. */
    Quiet,
    /** A lesson day: the school accent, flat. */
    School,
    /** A work-free day from the academic calendar: the recovery surface. */
    Free,
    /** Planned deep work and no lessons: the focus accent, flat. */
    Focus,
}

/**
 * The four meanings drawn as shapes in the grid and in the legend.
 *
 * The order of the enum is the order they appear in a cell, which keeps two neighbouring days from
 * swapping their symbols around.
 */
enum class MonthMark {
    /** No lessons: a ring, because "nothing scheduled" should read as a circle, not as a dot. */
    Ring,

    /** A test: a filled triangle. */
    Triangle,

    /** A deadline that is not a test: a filled diamond. */
    Diamond,

    /** Planned focus on a day with nothing else: a filled dot. */
    Dot,
}

/** One month cell reduced to the two things a reader needs: the kind of day and what is on it. */
data class MonthCellSignals(
    val dayType: MonthDayType,
    val marks: List<MonthMark>,
    val bars: Int,
)

object MonthSignals {

    /**
     * One bar per started hour of planned focus, capped at three: the marks answer "was there a
     * little, a fair amount or a lot" and nothing more precise than that is legible at 6 dp wide.
     */
    fun barsFor(focusMinutes: Int): Int = when {
        focusMinutes <= 0 -> 0
        else -> ((focusMinutes + 59) / 60).coerceIn(1, 3)
    }

    /**
     * The day's signals. [deadlineCount] is expected to exclude exams, so a test day shows a triangle
     * and not a triangle plus a diamond for the same event.
     *
     * The dot is only drawn when the day has no other mark: a cell is 44 dp wide, and a fourth symbol
     * would either wrap or push its neighbours out of the column.
     */
    fun of(
        focusMinutes: Int,
        examCount: Int,
        deadlineCount: Int,
        isWorkFree: Boolean,
        hasSchool: Boolean,
    ): MonthCellSignals {
        val dayType = when {
            isWorkFree -> MonthDayType.Free
            hasSchool -> MonthDayType.School
            focusMinutes > 0 -> MonthDayType.Focus
            else -> MonthDayType.Quiet
        }
        val marks = buildList {
            if (isWorkFree) add(MonthMark.Ring)
            if (examCount > 0) add(MonthMark.Triangle)
            if (deadlineCount > 0) add(MonthMark.Diamond)
            if (isEmpty() && focusMinutes > 0) add(MonthMark.Dot)
        }
        return MonthCellSignals(dayType = dayType, marks = marks, bars = barsFor(focusMinutes))
    }
}
