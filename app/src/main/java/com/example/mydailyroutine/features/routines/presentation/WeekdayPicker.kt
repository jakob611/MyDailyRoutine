package com.example.mydailyroutine.features.routines.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.mydailyroutine.R
import com.example.mydailyroutine.core.designsystem.haptics.LocalRoutineHaptics
import com.example.mydailyroutine.core.designsystem.theme.RoutineShapes
import com.example.mydailyroutine.core.platform.Slovenian
import com.example.mydailyroutine.domain.routines.Weekdays
import java.time.DayOfWeek
import java.time.format.TextStyle

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WeekdayPicker(mask: Int, enabled: Boolean, onChange: (Int) -> Unit) {
    val haptics = LocalRoutineHaptics.current
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            DayOfWeek.values().forEach { day ->
                val bit = 1 shl (day.value - 1)
                FilterChip(selected=mask and bit != 0, enabled=enabled, shape=RoutineShapes.Chip,
                    modifier=Modifier.testTag("weekday-${day.value}"),
                    onClick={ haptics.tap(); onChange(mask xor bit) },
                    label={ Text(day.getDisplayName(TextStyle.SHORT_STANDALONE,Slovenian)) })
            }
        }
        Row {
            TextButton(enabled=enabled,onClick={ haptics.tap();onChange(Weekdays.WORKDAYS) }) { Text(stringResource(R.string.weekdays_workdays)) }
            TextButton(enabled=enabled,onClick={ haptics.tap();onChange(Weekdays.ALL) }) { Text(stringResource(R.string.weekdays_every_day)) }
        }
    }
}
