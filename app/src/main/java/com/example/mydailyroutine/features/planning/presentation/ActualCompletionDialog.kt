package com.example.mydailyroutine.features.planning.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
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
import com.example.mydailyroutine.core.designsystem.components.RoutineLabel
import com.example.mydailyroutine.core.designsystem.components.RoutineText
import com.example.mydailyroutine.core.designsystem.components.RoutineTextDefaults
import com.example.mydailyroutine.core.designsystem.haptics.LocalRoutineHaptics
import com.example.mydailyroutine.core.designsystem.theme.RoutineColors
import com.example.mydailyroutine.core.designsystem.theme.RoutineShapes
import com.example.mydailyroutine.core.designsystem.theme.RoutineSpacing
import com.example.mydailyroutine.core.presentation.TimelineAction
import com.example.mydailyroutine.domain.model.ResolvedTimelineItem
import java.time.Duration

/**
 * Record how long a block actually took. Common durations are one-tap chips; the field stays for the
 * exact number, so nothing has to be typed in the usual case.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ActualCompletionDialog(block: ResolvedTimelineItem.Block, busy: Boolean, onAction: (TimelineAction) -> Unit) {
    val planned = Duration.between(block.startsAt, block.endsAt).toMinutes().toInt().coerceIn(1, 10080)
    var minutes by rememberSaveable(block.key) { mutableStateOf(planned.toString()) }
    var invalid by rememberSaveable { mutableStateOf(false) }
    val haptics = LocalRoutineHaptics.current
    val value = minutes.toIntOrNull()
    AlertDialog(
        onDismissRequest = { onAction(TimelineAction.CloseActual) },
        title = {
            RoutineText(stringResource(R.string.actual_title), style = MaterialTheme.typography.headlineSmall,
                maxLines = RoutineTextDefaults.Body)
        },
        text = {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(RoutineSpacing.sm)) {
                RoutineText(block.title, style = MaterialTheme.typography.titleMedium,
                    maxLines = RoutineTextDefaults.Body)
                RoutineText(stringResource(R.string.actual_hint), style = MaterialTheme.typography.bodySmall,
                    color = RoutineColors.TextSecondary, maxLines = RoutineTextDefaults.Paragraph)
                OutlinedTextField(
                    value = minutes,
                    onValueChange = { minutes = it.filter(Char::isDigit).take(5); invalid = false },
                    label = { RoutineText(stringResource(R.string.actual_minutes)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    enabled = !busy,
                    isError = invalid,
                    modifier = Modifier.fillMaxWidth(),
                    colors = RoutineTextFieldColors(),
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(RoutineSpacing.sm),
                    verticalArrangement = Arrangement.spacedBy(RoutineSpacing.xs),
                ) {
                    listOf(15, 30, 45, 60, 90, planned).distinct().sorted().forEach { option ->
                        FilterChip(
                            selected = value == option,
                            onClick = { haptics.selection(); minutes = option.toString(); invalid = false },
                            enabled = !busy,
                            shape = RoutineShapes.Chip,
                            label = { RoutineLabel(stringResource(R.string.duration_minutes, option)) },
                        )
                    }
                }
                if (invalid) {
                    RoutineText(stringResource(R.string.actual_invalid), style = MaterialTheme.typography.bodySmall,
                        color = RoutineColors.Error, maxLines = RoutineTextDefaults.Paragraph)
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = !busy,
                onClick = {
                    val parsed = minutes.toIntOrNull()
                    invalid = parsed == null || parsed !in 1..10080
                    if (invalid) haptics.warning() else onAction(TimelineAction.RecordActual(block, parsed!!))
                },
            ) { RoutineLabel(stringResource(R.string.actual_save), style = MaterialTheme.typography.labelLarge) }
        },
        dismissButton = {
            TextButton(onClick = { onAction(TimelineAction.CloseActual) }) {
                RoutineLabel(stringResource(R.string.cancel), style = MaterialTheme.typography.labelLarge)
            }
        },
    )
}
