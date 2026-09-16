package com.example.mydailyroutine.features.entry.presentation

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
import com.example.mydailyroutine.R
import com.example.mydailyroutine.core.designsystem.components.RoutineText
import com.example.mydailyroutine.core.designsystem.components.RoutineTextDefaults
import com.example.mydailyroutine.core.designsystem.components.SheetSecondaryButton
import com.example.mydailyroutine.core.designsystem.haptics.LocalRoutineHaptics
import com.example.mydailyroutine.core.designsystem.theme.RoutineColors
import com.example.mydailyroutine.core.designsystem.theme.RoutineSpacing
import com.example.mydailyroutine.domain.routines.EntryDefaults

/** Default lesson length and break, used by every new school block. */
@Composable
fun EntryDefaultsSettings(defaults: EntryDefaults, busy: Boolean, onSave: (EntryDefaults) -> Unit) {
    var duration by rememberSaveable(defaults) { mutableStateOf(defaults.lessonDurationMinutes.toString()) }
    var pause by rememberSaveable(defaults) { mutableStateOf(defaults.lessonBreakMinutes.toString()) }
    var error by rememberSaveable { mutableStateOf(false) }
    val haptics = LocalRoutineHaptics.current
    Column(verticalArrangement = Arrangement.spacedBy(RoutineSpacing.sm)) {
        RoutineText(stringResource(R.string.entry_defaults_title), style = MaterialTheme.typography.titleLarge,
            maxLines = RoutineTextDefaults.Body)
        RoutineText(stringResource(R.string.entry_defaults_hint), style = MaterialTheme.typography.bodySmall,
            color = RoutineColors.TextSecondary, maxLines = RoutineTextDefaults.Paragraph)
        Row(horizontalArrangement = Arrangement.spacedBy(RoutineSpacing.sm)) {
            OutlinedTextField(
                value = duration,
                onValueChange = { duration = it.filter(Char::isDigit).take(3); error = false },
                label = { RoutineText(stringResource(R.string.default_lesson_duration)) },
                enabled = !busy,
                isError = error,
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f),
            )
            OutlinedTextField(
                value = pause,
                onValueChange = { pause = it.filter(Char::isDigit).take(2); error = false },
                label = { RoutineText(stringResource(R.string.default_lesson_break)) },
                enabled = !busy,
                isError = error,
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f),
            )
        }
        if (error) {
            RoutineText(stringResource(R.string.entry_defaults_invalid), style = MaterialTheme.typography.bodySmall,
                color = RoutineColors.Warning, maxLines = RoutineTextDefaults.Paragraph)
        }
        SheetSecondaryButton(
            label = stringResource(R.string.entry_defaults_save),
            enabled = !busy,
            onClick = {
                val value = runCatching { EntryDefaults(duration.toInt(), pause.toInt()) }.getOrNull()
                error = value == null
                if (error) haptics.warning()
                value?.let(onSave)
            },
        )
    }
}
