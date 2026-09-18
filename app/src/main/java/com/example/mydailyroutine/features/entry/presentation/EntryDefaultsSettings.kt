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
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.FilterChip
import com.example.mydailyroutine.core.designsystem.theme.RoutineShapes
import com.example.mydailyroutine.R
import com.example.mydailyroutine.core.designsystem.components.RoutineText
import com.example.mydailyroutine.core.designsystem.components.RoutineTextDefaults
import com.example.mydailyroutine.core.designsystem.components.SheetSecondaryButton
import com.example.mydailyroutine.core.designsystem.haptics.LocalRoutineHaptics
import com.example.mydailyroutine.core.designsystem.theme.RoutineColors
import com.example.mydailyroutine.core.designsystem.theme.RoutineSpacing
import com.example.mydailyroutine.core.designsystem.components.RoutineLabel
import com.example.mydailyroutine.domain.routines.EntryDefaults

/** Default lesson length and break, used by every new school block. */
@Composable
fun EntryDefaultsSettings(defaults: EntryDefaults, busy: Boolean, onSave: (EntryDefaults) -> Unit) {
    val haptics = LocalRoutineHaptics.current
    Column(verticalArrangement = Arrangement.spacedBy(RoutineSpacing.sm)) {
        RoutineText(stringResource(R.string.entry_defaults_title), style = MaterialTheme.typography.titleLarge,
            maxLines = RoutineTextDefaults.Body)
        RoutineText(stringResource(R.string.entry_defaults_hint), style = MaterialTheme.typography.bodySmall,
            color = RoutineColors.TextSecondary, maxLines = RoutineTextDefaults.Paragraph)
        RoutineText(stringResource(R.string.default_lesson_duration), style = MaterialTheme.typography.titleSmall,
            maxLines = RoutineTextDefaults.Body)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(RoutineSpacing.sm), verticalArrangement = Arrangement.spacedBy(RoutineSpacing.xs)) {
            listOf(30, 40, 45, 50, 60, 90).forEach { minutes ->
                FilterChip(selected = defaults.lessonDurationMinutes == minutes,
                    onClick = { haptics.selection(); onSave(EntryDefaults(minutes, defaults.lessonBreakMinutes)) },
                    enabled = !busy,
                    label = { RoutineLabel(stringResource(R.string.sleep_minutes_format, minutes)) },
                    shape = RoutineShapes.Chip)
            }
        }
        RoutineText(stringResource(R.string.default_lesson_break), style = MaterialTheme.typography.titleSmall,
            maxLines = RoutineTextDefaults.Body)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(RoutineSpacing.sm), verticalArrangement = Arrangement.spacedBy(RoutineSpacing.xs)) {
            listOf(5, 10, 15, 20).forEach { minutes ->
                FilterChip(selected = defaults.lessonBreakMinutes == minutes,
                    onClick = { haptics.selection(); onSave(EntryDefaults(defaults.lessonDurationMinutes, minutes)) },
                    enabled = !busy,
                    label = { RoutineLabel(stringResource(R.string.sleep_minutes_format, minutes)) },
                    shape = RoutineShapes.Chip)
            }
        }
    }
}
