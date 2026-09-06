package com.example.mydailyroutine.ui.timeline

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
import com.example.mydailyroutine.platform.Slovenian
import com.example.mydailyroutine.ui.theme.categoryStyle
import java.time.LocalTime
import java.time.format.DateTimeFormatter

val clockFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm", Slovenian)
fun LocalTime.clockLabel(): String = format(clockFormat)
fun minuteLabel(minute: Int): String = String.format(Slovenian, "%02d:%02d", minute / 60, minute % 60)
@Composable fun durationLabel(minutes: Int): String = when {
    minutes < 60 -> stringResource(R.string.duration_minutes, minutes)
    minutes % 60 == 0 -> stringResource(R.string.duration_hours, minutes / 60)
    else -> stringResource(R.string.duration_hours_minutes, minutes / 60, minutes % 60)
}
@StringRes fun RoutineCategory.labelRes(): Int = when (this) {
    RoutineCategory.SCHOOL -> R.string.category_school
    RoutineCategory.FOCUS_STUDY -> R.string.category_focus
    RoutineCategory.REST_BREAK -> R.string.category_recovery
    RoutineCategory.PROJECT -> R.string.category_project
    RoutineCategory.PERSONAL -> R.string.category_personal
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
        else -> R.string.title_exam
    })
}
