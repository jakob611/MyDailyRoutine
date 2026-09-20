package com.example.mydailyroutine.core.designsystem.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.AnnotatedString
import com.example.mydailyroutine.R
import com.example.mydailyroutine.core.designsystem.theme.RoutineColors

/**
 * A time entry that opens an iOS-style wheel instead of asking for digits. Typing "07:45" into a
 * text field is the one place the app felt like a form from 2009; a drum you spin is faster one-
 * handed, needs no keyboard, and cannot produce "7:4". The field stays readable as a field —
 * value, label, trailing clock — it simply answers a tap anywhere on it with the wheel.
 */
@Composable
fun RoutineTimeField(
    value: String,
    onPick: (String) -> Unit,
    label: String,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    supporting: String? = null,
    wheelTag: String = "",
) {
    var picking by remember { mutableStateOf(false) }
    // One merged semantics node: the tag, the label and the shown time travel together, so tests
    // and TalkBack read the field as a single value instead of a box with loose children.
    Box(
        modifier.semantics(mergeDescendants = true) {
            // A read-only field keeps its value out of EditableText, so the node states the time
            // itself: tests and TalkBack read one merged value instead of an empty box.
            text = AnnotatedString(value)
        },
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            label = { RoutineText(label) },
            singleLine = true,
            enabled = enabled,
            trailingIcon = {
                Icon(
                    Icons.Outlined.Schedule,
                    contentDescription = stringResource(R.string.time_pick_title),
                )
            },
            isError = supporting != null,
            supportingText = supporting?.let {
                { RoutineText(it, style = MaterialTheme.typography.bodySmall, color = RoutineColors.Error) }
            },
            // The field, not the tap layer, measures the box: two matchParentSize children would
            // collapse it to zero height and leave the wheel unreachable.
            modifier = Modifier.fillMaxWidth(),
            colors = RoutineTextFieldColors(),
        )
        // The read-only field consumes nothing, so one transparent layer owns the whole tap target.
        Box(
            Modifier.matchParentSize()
                .clickable(enabled = enabled, onClickLabel = stringResource(R.string.time_pick_title)) { picking = true },
        )
    }
    if (picking) {
        val parts = value.split(':')
        TimeWheelDialog(
            initialHour = parts.getOrNull(0)?.toIntOrNull()?.coerceIn(0, 23) ?: 12,
            initialMinute = parts.getOrNull(1)?.toIntOrNull()?.coerceIn(0, 59) ?: 0,
            wheelTag = wheelTag.ifEmpty { "time-wheel" },
            onConfirm = { picked -> onPick(picked); picking = false },
            onDismiss = { picking = false },
        )
    }
}
