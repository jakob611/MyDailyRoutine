package com.example.mydailyroutine.features.routines.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.example.mydailyroutine.R
import com.example.mydailyroutine.core.designsystem.components.ActionRow
import com.example.mydailyroutine.core.designsystem.components.RoutineLabel
import com.example.mydailyroutine.core.designsystem.components.RoutineText
import com.example.mydailyroutine.core.designsystem.haptics.LocalRoutineHaptics
import com.example.mydailyroutine.core.designsystem.theme.RoutineShapes
import com.example.mydailyroutine.core.designsystem.theme.RoutineSpacing
import com.example.mydailyroutine.core.platform.Slovenian
import com.example.mydailyroutine.domain.routines.Weekdays
import java.time.DayOfWeek
import java.time.format.TextStyle

/**
 * Weekday mask picker. Chips flow onto new lines instead of shrinking, and the two shortcuts sit in
 * an action row that reflows rather than squeezing their labels.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WeekdayPicker(mask: Int, enabled: Boolean, onChange: (Int) -> Unit) {
    val haptics = LocalRoutineHaptics.current
    Column(verticalArrangement = Arrangement.spacedBy(RoutineSpacing.xs)) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(RoutineSpacing.xs),
            verticalArrangement = Arrangement.spacedBy(RoutineSpacing.xs),
        ) {
            DayOfWeek.entries.forEach { day ->
                val bit = 1 shl (day.value - 1)
                FilterChip(
                    selected = mask and bit != 0,
                    enabled = enabled,
                    shape = RoutineShapes.Chip,
                    modifier = Modifier.testTag("weekday-${day.value}"),
                    onClick = { haptics.tap(); onChange(mask xor bit) },
                    label = {
                        RoutineText(day.getDisplayName(TextStyle.SHORT_STANDALONE, Slovenian),
                            maxLines = 1, softWrap = false)
                    },
                )
            }
        }
        ActionRow {
            TextButton(enabled = enabled, onClick = { haptics.tap(); onChange(Weekdays.WORKDAYS) }) {
                RoutineLabel(stringResource(R.string.weekdays_workdays), style = MaterialTheme.typography.labelLarge)
            }
            TextButton(enabled = enabled, onClick = { haptics.tap(); onChange(Weekdays.ALL) }) {
                RoutineLabel(stringResource(R.string.weekdays_every_day), style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}
