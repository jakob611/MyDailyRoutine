package com.example.mydailyroutine.features.routines.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.FilterChip
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.example.mydailyroutine.R
import com.example.mydailyroutine.core.designsystem.components.ActionRow
import com.example.mydailyroutine.core.designsystem.components.RoutineSwitch
import com.example.mydailyroutine.core.designsystem.components.RoutineTimeField
import com.example.mydailyroutine.core.designsystem.components.RoutineLabel
import com.example.mydailyroutine.core.designsystem.components.RoutineText
import com.example.mydailyroutine.core.designsystem.components.RoutineTextDefaults
import com.example.mydailyroutine.core.designsystem.components.SheetSecondaryButton
import com.example.mydailyroutine.core.designsystem.components.SettingRow
import com.example.mydailyroutine.core.designsystem.haptics.LocalRoutineHaptics
import com.example.mydailyroutine.core.designsystem.theme.RoutineColors
import com.example.mydailyroutine.core.designsystem.theme.RoutineShapes
import com.example.mydailyroutine.core.designsystem.theme.RoutineSpacing
import com.example.mydailyroutine.core.presentation.TimelineAction
import com.example.mydailyroutine.core.presentation.clockLabel
import com.example.mydailyroutine.core.presentation.durationLabel
import com.example.mydailyroutine.domain.model.ScheduleValidation
import com.example.mydailyroutine.domain.model.nominalMinutes
import com.example.mydailyroutine.domain.routines.SleepSchedule
import com.example.mydailyroutine.domain.routines.Weekdays
import java.time.LocalTime

/** Sleep rhythm section: one card, one save button, weekday picker that reflows instead of clipping. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SleepSettings(schedule: SleepSchedule, busy: Boolean, onAction: (TimelineAction) -> Unit) {
    var enabled by rememberSaveable(schedule) { mutableStateOf(schedule.enabled) }
    var bed by rememberSaveable(schedule) { mutableStateOf(schedule.bedtime.clockLabel()) }
    var wake by rememberSaveable(schedule) { mutableStateOf(schedule.wakeTime.clockLabel()) }
    var days by rememberSaveable(schedule) { mutableIntStateOf(schedule.weekdaysMask) }
    var weekend by rememberSaveable(schedule) { mutableStateOf(schedule.weekendEnabled) }
    var weekendBed by rememberSaveable(schedule) { mutableStateOf(schedule.weekendBedtime.clockLabel()) }
    var weekendWake by rememberSaveable(schedule) { mutableStateOf(schedule.weekendWakeTime.clockLabel()) }
    var morning by rememberSaveable(schedule) { mutableStateOf(schedule.morningBufferMinutes.toString()) }
    var invalid by rememberSaveable { mutableStateOf(false) }
    val context = LocalContext.current
    val haptics = LocalRoutineHaptics.current
    // Every control commits the whole rhythm at once: a wheel confirmation, a chip or a switch is
    // the save. A separate button after already-confirmed wheels only taught doubt.
    fun commit() {
        val value = runCatching {
            SleepSchedule(
                enabled,
                requireNotNull(ScheduleValidation.parseTime(bed)),
                requireNotNull(ScheduleValidation.parseTime(wake)),
                days, morning.toIntOrNull() ?: 30, weekend,
                requireNotNull(ScheduleValidation.parseTime(weekendBed)),
                requireNotNull(ScheduleValidation.parseTime(weekendWake)),
            )
        }.getOrNull()
        invalid = value == null
        if (invalid) haptics.warning()
        value?.let {
            onAction(
                TimelineAction.SaveSleep(
                    it,
                    context.getString(R.string.sleep_title),
                    context.getString(R.string.sleep_morning_title),
                ),
            )
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(RoutineSpacing.sm)) {
        RoutineText(stringResource(R.string.sleep_heading), style = MaterialTheme.typography.titleLarge,
            maxLines = RoutineTextDefaults.Body)
        RoutineText(stringResource(R.string.sleep_hint), style = MaterialTheme.typography.bodySmall,
            color = RoutineColors.TextSecondary, maxLines = RoutineTextDefaults.Paragraph)
        SettingRow(
            title = stringResource(R.string.sleep_enable),
            control = { RoutineSwitch(enabled, { enabled = it; haptics.toggle(it); commit() }, enabled = !busy) },
        )
        Row(horizontalArrangement = Arrangement.spacedBy(RoutineSpacing.sm)) {
            RoutineTimeField(
                value = bed,
                onPick = { bed = it; invalid = false; commit() },
                label = stringResource(R.string.sleep_bedtime),
                enabled = !busy,
                modifier = Modifier.weight(1f),
                wheelTag = "sleep-bed",
            )
            RoutineTimeField(
                value = wake,
                onPick = { wake = it; invalid = false; commit() },
                label = stringResource(R.string.sleep_wake),
                enabled = !busy,
                modifier = Modifier.weight(1f),
                wheelTag = "sleep-wake",
            )
        }
        val start = ScheduleValidation.parseTime(bed)
        val end = ScheduleValidation.parseTime(wake)
        if (start != null && end != null && start != end) {
            RoutineText(
                text = stringResource(R.string.sleep_planned_duration, durationLabel(nominalMinutes(start, end))),
                style = MaterialTheme.typography.labelMedium,
                color = RoutineColors.Success,
                maxLines = RoutineTextDefaults.Body,
            )
        }
        SettingRow(
            title = stringResource(R.string.sleep_weekend_mode),
            control = { RoutineSwitch(weekend, { weekend = it; haptics.toggle(it); commit() }, enabled = !busy) },
        )
        if (weekend) {
            Row(horizontalArrangement = Arrangement.spacedBy(RoutineSpacing.sm)) {
                RoutineTimeField(
                    value = weekendBed,
                    onPick = { weekendBed = it; invalid = false; commit() },
                    label = stringResource(R.string.sleep_weekend_bedtime),
                    enabled = !busy,
                    modifier = Modifier.weight(1f),
                    wheelTag = "sleep-weekend-bed",
                )
                RoutineTimeField(
                    value = weekendWake,
                    onPick = { weekendWake = it; invalid = false; commit() },
                    label = stringResource(R.string.sleep_weekend_wake),
                    enabled = !busy,
                    modifier = Modifier.weight(1f),
                    wheelTag = "sleep-weekend-wake",
                )
            }
        }
        RoutineText(stringResource(R.string.sleep_days), style = MaterialTheme.typography.titleSmall,
            maxLines = RoutineTextDefaults.Body)
        WeekdayPicker(days, !busy) { days = it; commit() }
        ActionRow {
            TextButton(
                enabled = !busy,
                onClick = { haptics.selection(); days = Weekdays.shifted(Weekdays.WORKDAYS, -1) },
            ) {
                RoutineLabel(stringResource(R.string.sleep_before_workdays), style = MaterialTheme.typography.labelLarge)
            }
        }
        RoutineText(stringResource(R.string.sleep_days_hint), style = MaterialTheme.typography.bodySmall,
            color = RoutineColors.TextSecondary, maxLines = RoutineTextDefaults.Paragraph)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(RoutineSpacing.sm), verticalArrangement = Arrangement.spacedBy(RoutineSpacing.xs)) {
            listOf(0, 15, 30, 45, 60, 90).forEach { minutes ->
                FilterChip(
                    selected = morning == minutes.toString(),
                    onClick = { morning = minutes.toString(); invalid = false; haptics.selection(); commit() },
                    enabled = !busy,
                    label = { RoutineLabel(stringResource(R.string.sleep_minutes_format, minutes)) },
                    shape = RoutineShapes.Chip,
                )
            }
        }
        RoutineText(stringResource(R.string.sleep_morning_hint), style = MaterialTheme.typography.bodySmall,
            color = RoutineColors.TextSecondary, maxLines = RoutineTextDefaults.Paragraph)
        if (invalid) {
            RoutineText(stringResource(R.string.sleep_invalid), style = MaterialTheme.typography.bodySmall,
                color = RoutineColors.Warning, maxLines = RoutineTextDefaults.Paragraph)
        }
    }
}
