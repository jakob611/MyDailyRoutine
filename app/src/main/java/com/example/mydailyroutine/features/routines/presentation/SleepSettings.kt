package com.example.mydailyroutine.features.routines.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
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
import com.example.mydailyroutine.core.designsystem.components.RoutineLabel
import com.example.mydailyroutine.core.designsystem.components.RoutineText
import com.example.mydailyroutine.core.designsystem.components.RoutineTextDefaults
import com.example.mydailyroutine.core.designsystem.components.SheetSecondaryButton
import com.example.mydailyroutine.core.designsystem.components.SettingRow
import com.example.mydailyroutine.core.designsystem.haptics.LocalRoutineHaptics
import com.example.mydailyroutine.core.designsystem.theme.RoutineColors
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
    Column(verticalArrangement = Arrangement.spacedBy(RoutineSpacing.sm)) {
        RoutineText(stringResource(R.string.sleep_heading), style = MaterialTheme.typography.titleLarge,
            maxLines = RoutineTextDefaults.Body)
        RoutineText(stringResource(R.string.sleep_hint), style = MaterialTheme.typography.bodySmall,
            color = RoutineColors.TextSecondary, maxLines = RoutineTextDefaults.Paragraph)
        SettingRow(
            title = stringResource(R.string.sleep_enable),
            control = { Switch(enabled, { enabled = it; haptics.tap() }, enabled = !busy) },
        )
        Row(horizontalArrangement = Arrangement.spacedBy(RoutineSpacing.sm)) {
            OutlinedTextField(
                value = bed,
                onValueChange = { bed = it; invalid = false },
                label = { RoutineText(stringResource(R.string.sleep_bedtime)) },
                leadingIcon = { Icon(Icons.Outlined.Bedtime, null) },
                singleLine = true,
                enabled = !busy,
                modifier = Modifier.weight(1f),
            )
            OutlinedTextField(
                value = wake,
                onValueChange = { wake = it; invalid = false },
                label = { RoutineText(stringResource(R.string.sleep_wake)) },
                leadingIcon = { Icon(Icons.Outlined.WbSunny, null) },
                singleLine = true,
                enabled = !busy,
                modifier = Modifier.weight(1f),
            )
        }
        val start = ScheduleValidation.parseTime(bed)
        val end = ScheduleValidation.parseTime(wake)
        if (start != null && end != null && start != end) {
            RoutineText(
                text = stringResource(R.string.sleep_planned_duration, durationLabel(nominalMinutes(start, end))),
                style = MaterialTheme.typography.labelMedium,
                color = RoutineColors.Sage,
                maxLines = RoutineTextDefaults.Body,
            )
        }
        SettingRow(
            title = stringResource(R.string.sleep_weekend_mode),
            control = { Switch(weekend, { weekend = it; haptics.tap() }, enabled = !busy) },
        )
        if (weekend) {
            Row(horizontalArrangement = Arrangement.spacedBy(RoutineSpacing.sm)) {
                OutlinedTextField(
                    value = weekendBed,
                    onValueChange = { weekendBed = it; invalid = false },
                    label = { RoutineText(stringResource(R.string.sleep_weekend_bedtime)) },
                    singleLine = true,
                    enabled = !busy,
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = weekendWake,
                    onValueChange = { weekendWake = it; invalid = false },
                    label = { RoutineText(stringResource(R.string.sleep_weekend_wake)) },
                    singleLine = true,
                    enabled = !busy,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        RoutineText(stringResource(R.string.sleep_days), style = MaterialTheme.typography.titleSmall,
            maxLines = RoutineTextDefaults.Body)
        WeekdayPicker(days, !busy) { days = it }
        ActionRow {
            TextButton(
                enabled = !busy,
                onClick = { haptics.tap(); days = Weekdays.shifted(Weekdays.WORKDAYS, -1) },
            ) {
                RoutineLabel(stringResource(R.string.sleep_before_workdays), style = MaterialTheme.typography.labelLarge)
            }
        }
        RoutineText(stringResource(R.string.sleep_days_hint), style = MaterialTheme.typography.bodySmall,
            color = RoutineColors.TextSecondary, maxLines = RoutineTextDefaults.Paragraph)
        OutlinedTextField(
            value = morning,
            onValueChange = { morning = it.filter(Char::isDigit).take(3) },
            label = { RoutineText(stringResource(R.string.sleep_morning)) },
            singleLine = true,
            enabled = !busy,
            modifier = Modifier.fillMaxWidth(),
        )
        RoutineText(stringResource(R.string.sleep_morning_hint), style = MaterialTheme.typography.bodySmall,
            color = RoutineColors.TextSecondary, maxLines = RoutineTextDefaults.Paragraph)
        if (invalid) {
            RoutineText(stringResource(R.string.sleep_invalid), style = MaterialTheme.typography.bodySmall,
                color = RoutineColors.Warning, maxLines = RoutineTextDefaults.Paragraph)
        }
        SheetSecondaryButton(
            label = stringResource(R.string.sleep_save),
            enabled = !busy,
            onClick = {
                val weekendStart = if (weekend) ScheduleValidation.parseTime(weekendBed) else LocalTime.of(0, 30)
                val weekendEnd = if (weekend) ScheduleValidation.parseTime(weekendWake) else LocalTime.of(9, 30)
                val value = runCatching {
                    SleepSchedule(
                        enabled, requireNotNull(start), requireNotNull(end), days, morning.toInt(), weekend,
                        requireNotNull(weekendStart), requireNotNull(weekendEnd),
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
            },
        )
    }
}
