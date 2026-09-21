package com.example.mydailyroutine.core.presentation

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import com.example.mydailyroutine.R
import com.example.mydailyroutine.domain.health.HealthConfig
import com.example.mydailyroutine.domain.health.WarningType
import com.example.mydailyroutine.domain.model.RoutineCategory
import com.example.mydailyroutine.domain.presets.PresetKind
import com.example.mydailyroutine.domain.presets.QuickAddPreset
import com.example.mydailyroutine.core.platform.uiLocaleFor
import com.example.mydailyroutine.core.designsystem.theme.categoryStyle
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZonedDateTime
import java.time.LocalTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * The language the interface is currently in. Read once per process from the process default, which
 * `RoutineApplication` sets from the same decision the resources use: a language change recreates the
 * process, so nothing here can go stale while a screen is on.
 */
internal val interfaceLocale: Locale = uiLocaleFor(Locale.getDefault().language)

/** True when the interface speaks Slovenian, which is also what decides the date patterns below. */
private val slovenianInterface: Boolean = interfaceLocale.language == "sl"

/**
 * The patterns behind [RoutineDate], one set per language.
 *
 * Slovenian writes dates with trailing dots and a space (`16. sep`), English without them
 * (`16 Sep`); an English interface with Slovenian patterns reads like a translation that stopped
 * halfway, and a Slovenian interface with English patterns reads as broken. Everything else about a
 * date — how short a form is, which one a screen uses — is shared, so there is still exactly one
 * place in the app that decides how a date is written.
 */
private class DatePatterns(
    val tight: String,
    val dayNumber: String,
    val normal: String,
    val withWeekday: String,
    val weekdayTight: String,
    val full: String,
    val spoken: String,
    val monthAndYear: String,
    val monthTight: String,
    val monthYearTight: String,
    val normalYear: String,
    val weekdayNormalYear: String,
    val weekdayName: String,
    val weekdayFull: String,
    val axisDay: String,
    val clock: String,
)

private val slovenianPatterns = DatePatterns(
    tight = "d. M.", dayNumber = "d.", normal = "d. MMM", withWeekday = "EEE, d. MMM",
    weekdayTight = "EEE d", full = "d. MMMM yyyy", spoken = "EEEE, d. MMMM yyyy",
    monthAndYear = "LLLL yyyy", monthTight = "MMM", monthYearTight = "LLL yy",
    normalYear = "d. MMM yyyy", weekdayNormalYear = "EEE, d. MMM yyyy",
    weekdayName = "EEEE", weekdayFull = "EEEE d. MMMM", axisDay = "d/M",
    clock = "HH:mm",
)

private val englishPatterns = DatePatterns(
    tight = "d MMM", dayNumber = "d", normal = "d MMM", withWeekday = "EEE, d MMM",
    weekdayTight = "EEE d", full = "d MMMM yyyy", spoken = "EEEE, d MMMM yyyy",
    monthAndYear = "LLLL yyyy", monthTight = "MMM", monthYearTight = "LLL yy",
    normalYear = "d MMM yyyy", weekdayNormalYear = "EEE, d MMM yyyy",
    weekdayName = "EEEE", weekdayFull = "EEEE d MMMM", axisDay = "d/M",
    clock = "HH:mm",
)

private val patterns: DatePatterns = if (slovenianInterface) slovenianPatterns else englishPatterns

val clockFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm", interfaceLocale)
fun LocalTime.clockLabel(): String = format(clockFormat)
fun minuteLabel(minute: Int): String = String.format(interfaceLocale, "%02d:%02d", minute / 60, minute % 60)

/**
 * The only place in the app that decides how a date is written.
 *
 * Screens must not build their own `DateTimeFormatter`. A screen cannot know how wide the slot it is
 * filling will be on another device or in another language, and 23 hand-written patterns scattered
 * over the UI were exactly what turned dates into `sre, 16. …`. Each helper is named after the space
 * it is meant for, shortest first, and the labels combine with `RoutineLabel`, which shrinks the type
 * before it ever truncates — so a date is either fully readable or visibly smaller, never cut.
 *
 * Slovenian month names are long (`september`, `november`), which is why `full()` and `spoken()`
 * exist as their own steps instead of being improvised per screen.
 */
object RoutineDate {
    private val tightFormat: DateTimeFormatter = DateTimeFormatter.ofPattern(patterns.tight, interfaceLocale)
    private val normalFormat: DateTimeFormatter = DateTimeFormatter.ofPattern(patterns.normal, interfaceLocale)
    private val weekdayNormalFormat: DateTimeFormatter = DateTimeFormatter.ofPattern(patterns.withWeekday, interfaceLocale)
    private val weekdayTightFormat: DateTimeFormatter = DateTimeFormatter.ofPattern(patterns.weekdayTight, interfaceLocale)
    private val fullFormat: DateTimeFormatter = DateTimeFormatter.ofPattern(patterns.full, interfaceLocale)
    private val spokenFormat: DateTimeFormatter = DateTimeFormatter.ofPattern(patterns.spoken, interfaceLocale)
    private val monthAndYearFormat: DateTimeFormatter = DateTimeFormatter.ofPattern(patterns.monthAndYear, interfaceLocale)
    private val monthTightFormat: DateTimeFormatter = DateTimeFormatter.ofPattern(patterns.monthTight, interfaceLocale)
    private val monthYearTightFormat: DateTimeFormatter = DateTimeFormatter.ofPattern(patterns.monthYearTight, interfaceLocale)
    private val dayNumberFormat: DateTimeFormatter = DateTimeFormatter.ofPattern(patterns.dayNumber, interfaceLocale)
    private val normalYearFormat: DateTimeFormatter = DateTimeFormatter.ofPattern(patterns.normalYear, interfaceLocale)
    private val weekdayNormalYearFormat: DateTimeFormatter = DateTimeFormatter.ofPattern(patterns.weekdayNormalYear, interfaceLocale)
    private val weekdayNameFormat: DateTimeFormatter = DateTimeFormatter.ofPattern(patterns.weekdayName, interfaceLocale)
    private val weekdayFullFormat: DateTimeFormatter = DateTimeFormatter.ofPattern(patterns.weekdayFull, interfaceLocale)
    private val axisDayFormat: DateTimeFormatter = DateTimeFormatter.ofPattern(patterns.axisDay, interfaceLocale)
    private val clockFormat: DateTimeFormatter = DateTimeFormatter.ofPattern(patterns.clock, interfaceLocale)

    /** `16. 9.` — grid cells, gutters, chips: the shortest form that is still unambiguous. */
    fun tight(date: LocalDate): String = date.format(tightFormat)
    /** `16.` — month calendars, where the month is already written somewhere else. */
    fun dayNumber(date: LocalDate): String = date.format(dayNumberFormat)
    /** `16. sep` — list rows, milestone rows, progress entries. */
    fun normal(date: LocalDate): String = date.format(normalFormat)
    /** `sre, 16. sep` — screen and sheet headers. */
    fun withWeekday(date: LocalDate): String = date.format(weekdayNormalFormat)
    /** `sre 16` — the week grid, where a column is only a few dp wide. */
    fun weekdayTight(date: LocalDate): String = date.format(weekdayTightFormat)
    /** `16. september 2026` — anything that must read as a full sentence. */
    fun full(date: LocalDate): String = date.format(fullFormat)
    /** `sreda, 16. september 2026` — editors, where the reader is making a decision. */
    fun spoken(date: LocalDate): String = date.format(spokenFormat)
    /** `september 2026` — month mode header. */
    fun monthAndYear(date: LocalDate): String = date.format(monthAndYearFormat)
    /** `sep` — year mode cells and any axis with room for three letters. */
    fun monthTight(date: LocalDate): String = date.format(monthTightFormat)
    /** `sep` — same label when only the month is known, as in the year grid. */
    fun monthTight(month: YearMonth): String = month.format(monthTightFormat)
    /** `sep 26` — the Gantt axis. */
    fun axisLabel(month: YearMonth): String = month.format(monthYearTightFormat)
    /** `16. sep 2026` — anything crossing an academic-year boundary, where the year matters. */
    fun normalYear(date: LocalDate): String = date.format(normalYearFormat)
    /** `sre, 16. sep 2026` — planning rows: weekday plus year in one compact line. */
    fun withWeekdayYear(date: LocalDate): String = date.format(weekdayNormalYearFormat)
    /** `sreda` — a weekday on its own, e.g. beside a time in a weekly row. */
    fun weekdayName(date: LocalDate): String = date.format(weekdayNameFormat)
    /** `sreda 16. september` — overview headings inside a known year. */
    fun weekdayFull(date: LocalDate): String = date.format(weekdayFullFormat)
    /** `16/9` — the narrowest axis tick that still reads as a date. */
    fun axisDay(date: LocalDate): String = date.format(axisDayFormat)
    /** `07:30` — a clock time, tabular and locale-independent. */
    fun clock(dateTime: LocalDateTime): String = dateTime.format(clockFormat)
    /** `07:30` — same clock time from a zoned instant. */
    fun clock(dateTime: ZonedDateTime): String = dateTime.format(clockFormat)

    /** `07:30–09:00` — lesson and block rows; en dash, no spaces, tabular digits. */
    fun timeRange(startMinute: Int, endMinute: Int): String = "${minuteLabel(startMinute)}–${minuteLabel(endMinute)}"

    /** `16. sep – 20. jun` — a period on one line. Short form, so it fits narrow headers. */
    @Composable fun range(first: LocalDate, last: LocalDate): String =
        stringResource(R.string.date_range, normal(first), normal(last))

    /** `16. september 2026 – 20. junij 2027` — a period where a whole line is available. */
    @Composable fun wideRange(first: LocalDate, last: LocalDate): String =
        stringResource(R.string.date_range, full(first), full(last))
}

@Composable fun durationLabel(minutes: Int): String = when {
    minutes < 60 -> stringResource(R.string.duration_minutes, minutes)
    minutes % 60 == 0 -> stringResource(R.string.duration_hours, minutes / 60)
    else -> stringResource(R.string.duration_hours_minutes, minutes / 60, minutes % 60)
}
@StringRes fun RoutineCategory.labelRes(): Int = when (this) {
    RoutineCategory.SCHOOL -> R.string.category_school
    RoutineCategory.FOCUS_ANALYTICAL -> R.string.category_focus
    RoutineCategory.REST_BUFFER -> R.string.category_recovery
    RoutineCategory.EMERGENCY_RESERVE -> R.string.category_reserve
    RoutineCategory.FOCUS_SYNTHESIZING -> R.string.category_project
    RoutineCategory.ADMIN -> R.string.category_personal
}
@Composable fun RoutineCategory.label(): String = stringResource(labelRes())
@StringRes fun WarningType.labelRes(): Int = when (this) {
    WarningType.CONCENTRATION_LIMIT -> R.string.warning_concentration
    WarningType.HIGH_COGNITIVE_LOAD -> R.string.warning_cognitive
    WarningType.INSUFFICIENT_TRANSITION -> R.string.warning_transition
    WarningType.BURNOUT_RISK -> R.string.warning_daily
    WarningType.PHYSICAL_RESET -> R.string.warning_physical
    WarningType.FRAGMENTED_TIME -> R.string.warning_fragmented
}
@Composable fun WarningType.label(): String = stringResource(labelRes())
@Composable fun warningBody(type: WarningType, config: HealthConfig): String = when (type) {
    WarningType.CONCENTRATION_LIMIT -> stringResource(R.string.warning_concentration_body, config.focusLimitMinutes)
    WarningType.HIGH_COGNITIVE_LOAD -> stringResource(R.string.warning_cognitive_body, config.cognitiveLimitMinutes)
    WarningType.INSUFFICIENT_TRANSITION -> stringResource(R.string.warning_transition_body, config.transitionMinutes)
    WarningType.BURNOUT_RISK -> stringResource(R.string.warning_daily_body, config.dailyFocusLimitMinutes)
    WarningType.PHYSICAL_RESET -> stringResource(R.string.warning_physical_body, config.sedentaryLimitMinutes)
    WarningType.FRAGMENTED_TIME -> stringResource(R.string.warning_fragmented_body, config.fragmentedMinMinutes, config.fragmentedMaxMinutes)
}
fun categoryColor(category: RoutineCategory, subjectColor: Long? = null): Color = categoryStyle(category, subjectColor).accent
@StringRes fun PresetKind.labelRes(): Int = when (this) {
    PresetKind.DEEP_WORK -> R.string.preset_deep_work
    PresetKind.POMODORO -> R.string.preset_pomodoro
    PresetKind.WALK -> R.string.preset_walk
    PresetKind.IB_REVISION -> R.string.preset_ib
    PresetKind.EXAM -> R.string.preset_exam
    PresetKind.RESERVE -> R.string.preset_reserve
    PresetKind.LUNCH -> R.string.preset_lunch
    PresetKind.SNACK -> R.string.preset_snack
    PresetKind.SUBJECT_LESSON -> R.string.preset_lessons
    PresetKind.SUBJECT_STUDY -> R.string.preset_study
    PresetKind.SUBJECT_TEST -> R.string.preset_test
}
fun QuickAddPreset.label(context: Context): String = context.getString(kind.labelRes(), subjectName.orEmpty())
fun QuickAddPreset.title(context: Context): String {
    if (subjectId != null) {
        // A valid 120-character subject must still produce a valid stored block/milestone title.
        // Keep the localized purpose suffix and never split a UTF-16 surrogate pair.
        val suffixLength = context.getString(kind.labelRes(), "").length
        val name = subjectName.orEmpty()
        var end = minOf(name.length, (120 - suffixLength).coerceAtLeast(0))
        if (end > 0 && end < name.length && name[end - 1].isHighSurrogate()) end--
        return context.getString(kind.labelRes(), name.substring(0, end).trimEnd())
    }
    return context.getString(when (kind) {
        PresetKind.DEEP_WORK -> R.string.title_deep_work
        PresetKind.POMODORO -> R.string.title_pomodoro
        PresetKind.WALK -> R.string.title_walk
        PresetKind.IB_REVISION -> R.string.title_ib
        PresetKind.RESERVE -> R.string.reserve_title
        PresetKind.LUNCH -> R.string.title_lunch
        PresetKind.SNACK -> R.string.title_snack
        else -> R.string.title_exam
    })
}
