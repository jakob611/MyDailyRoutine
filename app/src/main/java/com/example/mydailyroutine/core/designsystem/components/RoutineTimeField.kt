package com.example.mydailyroutine.core.designsystem.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.example.mydailyroutine.R
import com.example.mydailyroutine.core.designsystem.theme.RoutineColors
import com.example.mydailyroutine.core.designsystem.theme.RoutineSpacing

/**
 * A time entry that opens the platform clock instead of asking for digits. Typing "07:45" into a
 * text field is the one place the app felt like a form from 2009; iOS solves the same moment with a
 * wheel, Material with a clock face, and both beat a keyboard. The field stays readable as a field
 * — value, label, trailing clock — it simply answers a tap with the picker.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoutineTimeField(
    value: String,
    onPick: (String) -> Unit,
    label: String,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    supporting: String? = null,
) {
    var picking by remember { mutableStateOf(false) }
    OutlinedTextField(
        value = value,
        onValueChange = onPick,
        label = { RoutineText(label) },
        singleLine = true,
        enabled = enabled,
        trailingIcon = {
            Icon(
                Icons.Outlined.Schedule,
                contentDescription = stringResource(R.string.time_pick_title),
                modifier = Modifier.clickable(enabled = enabled) { picking = true },
            )
        },
        modifier = modifier,
        isError = supporting != null,
        supportingText = supporting?.let {
            { RoutineText(it, style = MaterialTheme.typography.bodySmall, color = RoutineColors.Warning) }
        },
    )
    if (picking) {
        val parts = value.split(':')
        val state = rememberTimePickerState(
            initialHour = parts.getOrNull(0)?.toIntOrNull()?.coerceIn(0, 23) ?: 12,
            initialMinute = parts.getOrNull(1)?.toIntOrNull()?.coerceIn(0, 59) ?: 0,
            is24Hour = true,
        )
        AlertDialog(
            onDismissRequest = { picking = false },
            confirmButton = {
                TextButton(onClick = {
                    onPick("%02d:%02d".format(state.hour, state.minute))
                    picking = false
                }) { RoutineLabel(stringResource(R.string.time_apply), style = MaterialTheme.typography.labelLarge) }
            },
            dismissButton = {
                TextButton(onClick = { picking = false }) {
                    RoutineLabel(stringResource(R.string.cancel), style = MaterialTheme.typography.labelLarge)
                }
            },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    RoutineText(
                        stringResource(R.string.time_pick_title),
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(bottom = RoutineSpacing.md),
                    )
                    TimePicker(state = state)
                }
            },
        )
    }
}
