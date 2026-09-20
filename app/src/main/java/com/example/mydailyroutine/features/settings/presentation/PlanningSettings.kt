package com.example.mydailyroutine.features.settings.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import com.example.mydailyroutine.core.designsystem.components.RoutineTextFieldColors
import com.example.mydailyroutine.R
import com.example.mydailyroutine.core.designsystem.components.RoutineText
import com.example.mydailyroutine.core.designsystem.components.RoutineTextDefaults
import com.example.mydailyroutine.core.designsystem.components.SheetSecondaryButton
import com.example.mydailyroutine.core.designsystem.haptics.LocalRoutineHaptics
import com.example.mydailyroutine.core.designsystem.theme.RoutineColors
import com.example.mydailyroutine.core.designsystem.theme.RoutineSpacing
import com.example.mydailyroutine.core.presentation.minuteLabel
import com.example.mydailyroutine.domain.model.ScheduleValidation
import com.example.mydailyroutine.domain.planning.PlanningConfig

/** Capacity window and circadian dip used by the planner heuristics. */
@Composable
fun PlanningSettings(config: PlanningConfig, busy: Boolean, onSave: (PlanningConfig) -> Unit) {
    var capacity by rememberSaveable(config) { mutableStateOf(config.dailyStudyCapacityMinutes.toString()) }
    var start by rememberSaveable(config) { mutableStateOf(minuteLabel(config.studyStartMinutes)) }
    var end by rememberSaveable(config) { mutableStateOf(minuteLabel(config.studyEndMinutes)) }
    var center by rememberSaveable(config) { mutableStateOf(minuteLabel(config.dipCenterMinutes)) }
    var sigma by rememberSaveable(config) { mutableStateOf(config.dipSigmaMinutes.toString()) }
    var slip by rememberSaveable(config) { mutableStateOf(config.defaultSlipMinutes.toString()) }
    var focus by rememberSaveable(config) { mutableStateOf(config.targetFocusMinutes.toString()) }
    var transition by rememberSaveable(config) { mutableStateOf(config.postSchoolRecoveryMinutes.toString()) }
    var error by rememberSaveable { mutableStateOf(false) }
    val haptics = LocalRoutineHaptics.current
    Column(verticalArrangement = Arrangement.spacedBy(RoutineSpacing.sm)) {
        RoutineText(stringResource(R.string.planning_settings_title), style = MaterialTheme.typography.titleLarge,
            maxLines = RoutineTextDefaults.Body)
        RoutineText(stringResource(R.string.model_disclaimer), style = MaterialTheme.typography.bodySmall,
            color = RoutineColors.TextSecondary, maxLines = RoutineTextDefaults.Paragraph)
        OutlinedTextField(
            value = capacity,
            onValueChange = { capacity = it.filter(Char::isDigit).take(4); error = false },
            label = { RoutineText(stringResource(R.string.planning_capacity)) },
            enabled = !busy,
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
            colors = RoutineTextFieldColors(),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(RoutineSpacing.sm)) {
            OutlinedTextField(
                value = start,
                onValueChange = { start = it; error = false },
                label = { RoutineText(stringResource(R.string.planning_start)) },
                enabled = !busy,
                singleLine = true,
                modifier = Modifier.weight(1f),
                colors = RoutineTextFieldColors(),
            )
            OutlinedTextField(
                value = end,
                onValueChange = { end = it; error = false },
                label = { RoutineText(stringResource(R.string.planning_end)) },
                enabled = !busy,
                singleLine = true,
                modifier = Modifier.weight(1f),
                colors = RoutineTextFieldColors(),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(RoutineSpacing.sm)) {
            OutlinedTextField(
                value = center,
                onValueChange = { center = it; error = false },
                label = { RoutineText(stringResource(R.string.planning_dip)) },
                enabled = !busy,
                singleLine = true,
                modifier = Modifier.weight(1f),
                colors = RoutineTextFieldColors(),
            )
            OutlinedTextField(
                value = sigma,
                onValueChange = { sigma = it.filter(Char::isDigit).take(4); error = false },
                label = { RoutineText(stringResource(R.string.planning_sigma)) },
                enabled = !busy,
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f),
                colors = RoutineTextFieldColors(),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(RoutineSpacing.sm)) {
            OutlinedTextField(
                value = slip,
                onValueChange = { slip = it.filter(Char::isDigit).take(4); error = false },
                label = { RoutineText(stringResource(R.string.planning_slip)) },
                enabled = !busy,
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f),
                colors = RoutineTextFieldColors(),
            )
            OutlinedTextField(
                value = focus,
                onValueChange = { focus = it.filter(Char::isDigit).take(4); error = false },
                label = { RoutineText(stringResource(R.string.planning_focus)) },
                enabled = !busy,
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f),
                colors = RoutineTextFieldColors(),
            )
        }
        OutlinedTextField(
            value = transition,
            onValueChange = { transition = it.filter(Char::isDigit).take(4); error = false },
            label = { RoutineText(stringResource(R.string.planning_transition)) },
            enabled = !busy,
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
            colors = RoutineTextFieldColors(),
        )
        RoutineText(stringResource(R.string.planning_limit_hint), style = MaterialTheme.typography.bodySmall,
            color = RoutineColors.TextSecondary, maxLines = RoutineTextDefaults.Paragraph)
        if (error) {
            RoutineText(stringResource(R.string.planning_invalid), style = MaterialTheme.typography.bodySmall,
                color = RoutineColors.Error, maxLines = RoutineTextDefaults.Paragraph)
        }
        SheetSecondaryButton(
            label = stringResource(R.string.planning_save),
            enabled = !busy,
            onClick = {
                val value = runCatching {
                    PlanningConfig(
                        capacity.toInt(),
                        requireNotNull(ScheduleValidation.parseTime(start)).toSecondOfDay() / 60,
                        if (end == "24:00") 1440 else requireNotNull(ScheduleValidation.parseTime(end)).toSecondOfDay() / 60,
                        requireNotNull(ScheduleValidation.parseTime(center)).toSecondOfDay() / 60,
                        sigma.toInt(),
                        slip.toInt(),
                        focus.toInt(),
                        transition.toInt(),
                    )
                }.getOrNull()
                error = value == null
                if (error) haptics.warning()
                value?.let(onSave)
            },
        )
    }
}
