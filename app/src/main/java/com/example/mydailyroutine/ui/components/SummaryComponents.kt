package com.example.mydailyroutine.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.mydailyroutine.R
import com.example.mydailyroutine.domain.health.HealthConfig
import com.example.mydailyroutine.domain.health.categoryAllocation
import com.example.mydailyroutine.domain.model.CalendarEntry
import com.example.mydailyroutine.domain.model.ResolvedTimelineItem
import com.example.mydailyroutine.ui.theme.*
import com.example.mydailyroutine.ui.timeline.*

/** Adapted from agent3's component library; uses the canonical core model and shared OLED palette. */
@Composable
fun DateNavigator(title: String, onPrevious: () -> Unit, onNext: () -> Unit, onToday: () -> Unit, onPick: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onPrevious) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, stringResource(R.string.previous_period)) }
        Text(title, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
        TextButton(onClick = onToday) { Text(stringResource(R.string.today)) }
        IconButton(onClick = onPick) { Icon(Icons.Outlined.CalendarMonth, stringResource(R.string.choose_date)) }
        IconButton(onClick = onNext) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, stringResource(R.string.next_period)) }
    }
}

@Composable
fun DayLoadBar(items: List<ResolvedTimelineItem>, modifier: Modifier = Modifier) {
    val allocation = remember(items) { categoryAllocation(items).filter { it.minutes > 0 } }
    val description = stringResource(R.string.day_load_description)
    Row(modifier.fillMaxWidth().height(8.dp).clip(RoutineShapes.Pill).background(RoutineColors.Surface2)
        .semantics { contentDescription = description }) {
        allocation.forEach { slice -> Box(Modifier.weight(slice.minutes.toFloat()).fillMaxHeight().background(categoryStyle(slice.category).accent)) }
    }
}

@Composable
fun HealthWarningCard(warning: WarningUi, config: HealthConfig, busy: Boolean, onInsert: () -> Unit) {
    Surface(shape = RoutineShapes.Card, color = RoutineColors.WarningContainer, border = BorderStroke(1.dp, RoutineColors.Warning.copy(alpha = 0.18f))) {
        Row(Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(Icons.Outlined.Psychology, null, tint = RoutineColors.Warning)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(warning.type.label(), style = MaterialTheme.typography.titleSmall, color = RoutineColors.Warning)
                Text(warningBody(warning.type, config), style = MaterialTheme.typography.bodySmall)
                TextButton(onClick = onInsert, enabled = !busy) { Text(stringResource(R.string.insert_break, warning.recoveryMinutes), color = RoutineColors.Warning) }
            }
        }
    }
}

@Composable
fun CalendarNoticeCard(entries: List<CalendarEntry>) {
    if (entries.isEmpty()) return
    val off = entries.any { it.isWorkFreeDay }
    val style = if (off) RoutineColors.Recovery else RoutineColors.School
    Surface(color = style.container, shape = RoutineShapes.Card, border = BorderStroke(1.dp, RoutineColors.Border)) {
        Row(Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(Icons.Outlined.CalendarMonth, null, tint = style.content)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(stringResource(if (off) R.string.calendar_off_title else R.string.calendar_notice_title), style = MaterialTheme.typography.titleSmall, color = style.content)
                entries.forEach { Text(it.title, style = MaterialTheme.typography.bodySmall) }
                if (off) Text(stringResource(R.string.calendar_school_inactive), style = MaterialTheme.typography.bodySmall, color = RoutineColors.TextSecondary)
            }
        }
    }
}

@Composable
fun MetricTile(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(modifier, shape = RoutineShapes.Card, color = RoutineColors.Surface1, border = BorderStroke(1.dp, RoutineColors.CardBorder)) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(value, style = MaterialTheme.typography.titleLarge, color = RoutineColors.TextPrimary)
            Text(label, style = MaterialTheme.typography.labelMedium, color = RoutineColors.TextSecondary)
        }
    }
}

@Composable
fun Legend(label: String, color: androidx.compose.ui.graphics.Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(Modifier.size(6.dp).background(color, CircleShape))
        Text(label, style = MaterialTheme.typography.labelSmall, color = RoutineColors.TextSecondary)
    }
}
