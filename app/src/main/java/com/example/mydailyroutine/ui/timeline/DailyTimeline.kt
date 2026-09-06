package com.example.mydailyroutine.ui.timeline

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.EventAvailable
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Spa
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.LayoutDirection
import com.example.mydailyroutine.domain.calendar.SlovenianAcademicCalendar
import com.example.mydailyroutine.domain.model.ResolvedTimelineItem
import com.example.mydailyroutine.domain.model.RoutineCategory
import java.time.ZonedDateTime
import java.time.Duration
import com.example.mydailyroutine.domain.scheduling.OccurrenceTimes
import java.time.format.DateTimeFormatter

@Composable
fun DailyTimeline(day: DayUi, now: ZonedDateTime, busy: Boolean, onAction: (TimelineAction) -> Unit) {
    val lineColor = MaterialTheme.colorScheme.outlineVariant
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 108.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item(key = "summary") { DaySummary(day) }
        if (!SlovenianAcademicCalendar.covers(day.date)) item(key = "coverage") {
            Text("Holiday dates are bundled for 2026/27 only. Check school days outside that cycle.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(vertical = 8.dp))
        }
        if (day.calendar.isNotEmpty()) item(key = "calendar") {
            Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = RoundedCornerShape(16.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(if (day.calendar.any { it.isWorkFreeDay }) "A little room to breathe" else "On the school calendar",
                        style = MaterialTheme.typography.titleSmall)
                    day.calendar.forEach { Text(it.title, style = MaterialTheme.typography.bodySmall) }
                    if (day.calendar.any { it.isWorkFreeDay }) Text("School blocks stay visible, but inactive today.", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        if (day.items.isEmpty()) item(key = "empty") {
            Card(Modifier.fillMaxWidth().padding(vertical = 20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Outlined.Spa, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text("Make space for what matters.", style = MaterialTheme.typography.headlineSmall)
                    Text("Start with a lesson, a focused study block, or a proper break. Your schedule stays on this device.")
                    FilledTonalButton(onClick = { onAction(TimelineAction.OpenAdd) }) { Text("Plan your first block") }
                }
            }
        }
        var occupiedUntil: Int? = null
        day.items.forEach { entry ->
            val before = occupiedUntil
            if (entry is ResolvedTimelineItem.Block && !entry.isSuppressed) {
                if (before != null && entry.startMinute - before >= 15) {
                    val gap = entry.startMinute - before
                    item(key = "gap:${entry.key}") {
                        Row(Modifier.fillMaxWidth().height((gap / 2).coerceIn(28, 72).dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.width(58.dp))
                            Box(Modifier.width(1.dp).fillMaxHeight().background(lineColor))
                            Text("${durationLabel(gap)} unallocated", Modifier.padding(start = 18.dp),
                                style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                occupiedUntil = maxOf(before ?: 0, entry.endMinute)
            }
            item(key = entry.key, contentType = if (entry is ResolvedTimelineItem.Block) "block" else "milestone") {
                Row(
                    modifier = Modifier.fillMaxWidth().animateItem(placementSpec = spring(stiffness = Spring.StiffnessMediumLow))
                        .drawBehind {
                            val x = if (layoutDirection == LayoutDirection.Rtl) size.width - 58.dp.toPx() else 58.dp.toPx()
                            drawLine(lineColor, Offset(x, 0f), Offset(x, size.height), 1.dp.toPx())
                            drawCircle(lineColor, 4.dp.toPx(), Offset(x, 25.dp.toPx()))
                        },
                ) {
                    Column(Modifier.width(74.dp).padding(top = 16.dp)) {
                        Text(if (entry is ResolvedTimelineItem.Milestone && entry.dueTime == null) "All day" else entry.startTime.clockLabel(),
                            style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                    }
                    when (entry) {
                        is ResolvedTimelineItem.Block -> BlockCard(
                            entry, day.warnings.filter { entry.key in it.itemKeys }, now, busy,
                            overlaps = before != null && entry.startMinute < before && !entry.isSuppressed,
                            modifier = Modifier.weight(1f), onAction = onAction,
                        )
                        is ResolvedTimelineItem.Milestone -> MilestoneCard(entry, busy, Modifier.weight(1f), onAction)
                    }
                }
            }
        }
        if (day.cancelled.isNotEmpty()) item(key = "cancelled") {
            OutlinedCard(Modifier.fillMaxWidth().padding(top = 12.dp)) {
                Column(Modifier.padding(16.dp)) {
                    Text("Skipped on this date", style = MaterialTheme.typography.titleSmall)
                    day.cancelled.forEach { cancelled ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(cancelled.title, style = MaterialTheme.typography.bodyMedium)
                                if (cancelled.date != day.date) Text("Begins ${cancelled.date}", style = MaterialTheme.typography.labelSmall)
                            }
                            TextButton(enabled = !busy, onClick = { onAction(TimelineAction.Restore(cancelled.routineId, cancelled.date)) }) { Text("Restore") }
                        }
                    }
                }
            }
        }
        item(key = "health-note") {
            Text("Planning guidance, not medical advice. Thresholds are useful prompts, not universal human limits. Overlapping blocks are counted once.",
                modifier = Modifier.padding(top = 16.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun DaySummary(day: DayUi) {
    Column(Modifier.padding(bottom = 8.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("A balanced day starts here.", style = MaterialTheme.typography.titleMedium)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MetricTile("Deep work", durationLabel(day.metrics.focusMinutes), Modifier.weight(1f))
            MetricTile("Recovery", durationLabel(day.metrics.recoveryMinutes), Modifier.weight(1f))
            MetricTile("Completed", "${day.metrics.completedCount}/${day.metrics.blockCount}", Modifier.weight(1f))
        }
        AnimatedVisibility(visible = day.warnings.isNotEmpty(), enter = slideInVertically { -it / 2 } + fadeIn()) {
            Text("${day.warnings.size} balance ${if (day.warnings.size == 1) "suggestion" else "suggestions"} · tap a block to explore",
                style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.tertiary)
        }
    }
}

@Composable
private fun MetricTile(label: String, value: String, modifier: Modifier) {
    Surface(modifier, shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceContainerLow) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(value, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
            Text(label, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BlockCard(
    block: ResolvedTimelineItem.Block, warnings: List<WarningUi>, now: ZonedDateTime, busy: Boolean,
    overlaps: Boolean, modifier: Modifier, onAction: (TimelineAction) -> Unit,
) {
    var expanded by rememberSaveable(block.key) { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current
    val accent = categoryColor(block.category, block.subject?.colorHex)
    val actualWindow = remember(block, now.zone) { OccurrenceTimes.window(block, now.zone) }
    val active = !block.isSuppressed && !block.isCompleted && now.toInstant() >= actualWindow.start && now.toInstant() < actualWindow.end
    val container = when {
        block.isSuppressed || block.category == RoutineCategory.REST_BREAK -> MaterialTheme.colorScheme.surfaceContainerLow
        else -> accent.copy(alpha = if (active) 0.18f else 0.09f)
    }
    Card(
        onClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); expanded = !expanded },
        modifier = modifier.animateContentSize(spring(stiffness = Spring.StiffnessMediumLow)),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = container),
        border = if (block.category == RoutineCategory.FOCUS_STUDY || active) BorderStroke(if (active) 2.dp else 1.dp, accent.copy(alpha = 0.7f)) else null,
        elevation = CardDefaults.cardElevation(defaultElevation = if (active) 3.dp else 0.dp),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(if (active) "NOW · ${block.category.label()}" else block.category.label(),
                    Modifier.weight(1f), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Checkbox(checked = block.isCompleted, enabled = !busy && !block.isSuppressed,
                    onCheckedChange = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onAction(TimelineAction.ToggleComplete(block))
                    }, modifier = Modifier.size(32.dp))
            }
            Text(block.title, style = MaterialTheme.typography.titleMedium,
                textDecoration = if (block.isCompleted || block.isSuppressed) TextDecoration.LineThrough else null)
            Text("${minuteLabel(block.startMinute)}–${minuteLabel(block.endMinute)} · ${durationLabel(block.durationMinutes)}",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (actualWindow.start.atZone(now.zone).toLocalDateTime() != block.startsAt ||
                Duration.between(actualWindow.start, actualWindow.end) != Duration.between(block.startsAt, block.endsAt)) {
                Text("Clock change · actual start ${actualWindow.start.atZone(now.zone).toLocalTime().clockLabel()}, ${durationLabel(Duration.between(actualWindow.start, actualWindow.end).toMinutes().toInt())} elapsed",
                    style = MaterialTheme.typography.labelSmall)
            }
            if (block.isCarryIn) Text("Continues from ${block.occurrenceDate.format(DateTimeFormatter.ofPattern("d MMM"))}", style = MaterialTheme.typography.labelSmall)
            if (block.isSuppressed) Text("No school · ${block.holidayTitle}", style = MaterialTheme.typography.bodySmall)
            if (overlaps) Text("Overlaps another block", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
            if (warnings.isNotEmpty()) FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                warnings.forEach { warning ->
                    SuggestionChip(onClick = { expanded = true; haptic.performHapticFeedback(HapticFeedbackType.LongPress) },
                        label = { Text(warning.type.label(), style = MaterialTheme.typography.labelSmall) })
                }
            }
            AnimatedVisibility(visible = expanded, enter = slideInVertically { -it / 4 } + fadeIn()) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    HorizontalDivider()
                    Text(if (block.isOneOff) "One-off block" else "Weekly blueprint · ${block.occurrenceDate.dayOfWeek.name.lowercase().replaceFirstChar { it.uppercase() }}",
                        style = MaterialTheme.typography.bodySmall)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(if (block.category == RoutineCategory.REST_BREAK) "Remind at recovery start" else "Remind 5 minutes before", style = MaterialTheme.typography.bodySmall)
                            if (!block.isOneOff) Text("Applies to every week", style = MaterialTheme.typography.labelSmall)
                        }
                        Switch(checked = block.isNotificationEnabled, enabled = !busy, onCheckedChange = {
                            onAction(TimelineAction.SetReminder(block.routineBlockId, it))
                        })
                    }
                    warnings.forEach { Text(it.message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        TextButton(enabled = !busy, onClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); onAction(TimelineAction.Edit(block)) }) { Text("Move / rename") }
                        TextButton(enabled = !busy, onClick = { onAction(TimelineAction.Skip(block)) }) { Text("Skip this date") }
                        if (block.hasOverride) TextButton(enabled = !busy, onClick = { onAction(TimelineAction.ResetOverride(block)) }) { Text("Reset this date") }
                        TextButton(enabled = !busy, onClick = { onAction(TimelineAction.RequestDelete(block)) }) { Text(if (block.isOneOff) "Delete block" else "Delete routine", color = MaterialTheme.colorScheme.error) }
                    }
                }
            }
        }
    }
}

@Composable
private fun MilestoneCard(item: ResolvedTimelineItem.Milestone, busy: Boolean, modifier: Modifier, onAction: (TimelineAction) -> Unit) {
    var expanded by rememberSaveable(item.key) { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current
    Card(onClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); expanded = !expanded },
        modifier = modifier.animateContentSize(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer), shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(if (item.isExam) Icons.Outlined.EventAvailable else Icons.Outlined.Flag, contentDescription = null)
                Text(if (item.isExam) "Exam" else "Milestone", Modifier.weight(1f), style = MaterialTheme.typography.labelMedium)
                Checkbox(checked = item.isCompleted, enabled = !busy, modifier = Modifier.size(32.dp), onCheckedChange = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress); onAction(TimelineAction.ToggleComplete(item))
                })
            }
            Text(item.title, style = MaterialTheme.typography.titleMedium, textDecoration = if (item.isCompleted) TextDecoration.LineThrough else null)
            Text(item.dueTime?.let { "Due ${it.clockLabel()}" } ?: "All-day marker · no time reserved", style = MaterialTheme.typography.bodySmall)
            item.subject?.let { Text(it.name, style = MaterialTheme.typography.bodySmall) }
            AnimatedVisibility(expanded, enter = fadeIn() + slideInVertically { -it / 3 }) {
                Row {
                    TextButton(enabled = !busy, onClick = { onAction(TimelineAction.Edit(item)) }) { Text("Edit") }
                    TextButton(enabled = !busy, onClick = { onAction(TimelineAction.RequestDelete(item)) }) { Text("Delete") }
                }
            }
        }
    }
}
