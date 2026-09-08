package com.example.mydailyroutine.features.timeline.presentation

import com.example.mydailyroutine.core.presentation.*

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Spa
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.mydailyroutine.R
import com.example.mydailyroutine.domain.calendar.SlovenianAcademicCalendar
import com.example.mydailyroutine.domain.health.HealthConfig
import com.example.mydailyroutine.domain.planning.PlanningConfig
import com.example.mydailyroutine.domain.model.ResolvedTimelineItem
import com.example.mydailyroutine.domain.routines.RoutineOrigin
import com.example.mydailyroutine.features.routines.presentation.ManagedRoutineCard
import com.example.mydailyroutine.domain.scheduling.OccurrenceTimes
import com.example.mydailyroutine.features.timeline.components.*
import com.example.mydailyroutine.core.designsystem.theme.*
import java.time.ZonedDateTime

@Composable
fun DailyTimeline(day: DayUi, now: ZonedDateTime, busy: Boolean, health: HealthConfig, planning: PlanningConfig, backlogCount: Int, execution: com.example.mydailyroutine.domain.execution.ActiveExecution?, onAction: (TimelineAction) -> Unit) {
    val today = day.date == now.toLocalDate()
    val nowMinute = now.hour * 60 + now.minute
    val activeKey = remember(day.items, now) {
        if (!today) null else day.items.filterIsInstance<ResolvedTimelineItem.Block>().firstOrNull {
            val window = OccurrenceTimes.window(it, now.zone)
            now.toInstant() >= window.start && now.toInstant() < window.end
        }?.key
    }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp, 12.dp, 16.dp, 112.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item(key = "summary") {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(bottom = 12.dp)) {
                Text(stringResource(R.string.day_heading), style = MaterialTheme.typography.titleLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MetricTile(stringResource(R.string.metric_focus), durationLabel(day.metrics.focusMinutes), Modifier.weight(1f))
                    MetricTile(stringResource(R.string.metric_recovery), durationLabel(day.metrics.recoveryMinutes), Modifier.weight(1f))
                    MetricTile(stringResource(R.string.metric_completed), stringResource(R.string.completed_count, day.metrics.completedCount, day.metrics.blockCount), Modifier.weight(1f))
                }
                DayLoadBar(day.items)
                execution?.let { active ->
                    Surface(color = RoutineColors.Focus.container, shape = RoutineShapes.Card) {
                        Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(stringResource(if (active.stoppedAt == null) R.string.execution_running else R.string.execution_stopped), style = MaterialTheme.typography.titleSmall)
                            Text(stringResource(R.string.execution_elapsed, active.elapsedMinutes(now.toInstant())), style = MaterialTheme.typography.titleLarge)
                            Row {
                                TextButton(enabled = !busy, onClick = { onAction(TimelineAction.FinishExecution) }) { Text(stringResource(R.string.execution_finish)) }
                                TextButton(enabled = !busy, onClick = { onAction(TimelineAction.RequestCancelExecution) }) { Text(stringResource(R.string.execution_cancel)) }
                            }
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    FilledTonalButton(enabled = !busy && day.items.isNotEmpty() && day.date >= java.time.LocalDate.now(), onClick = {
                        val actual = if (today) nowMinute else (day.items.filterIsInstance<ResolvedTimelineItem.Block>().filter { !it.isCompleted && !it.isSuppressed }.minOfOrNull { it.startMinute } ?: 0) + planning.defaultSlipMinutes
                        onAction(TimelineAction.AutoHeal(day.date, actual.coerceIn(0,2879)))
                    }) { Text(stringResource(R.string.auto_heal)) }
                    TextButton(onClick = { onAction(TimelineAction.OpenPlanning) }) { Text(stringResource(R.string.backlog_count, backlogCount)) }
                }
                Text(stringResource(R.string.auto_heal_hint), style = MaterialTheme.typography.bodySmall, color = RoutineColors.TextSecondary)
                Text(stringResource(R.string.reserve_remaining, day.metrics.reserveMinutes), style = MaterialTheme.typography.labelMedium, color = RoutineColors.Sage)
                if (day.warnings.isNotEmpty()) Text(stringResource(R.string.health_suggestion_count, day.warnings.size), style = MaterialTheme.typography.labelMedium, color = RoutineColors.Warning)
            }
        }
        if (!SlovenianAcademicCalendar.covers(day.date)) item(key = "coverage") {
            Text(stringResource(R.string.coverage_warning), style = MaterialTheme.typography.bodySmall, color = RoutineColors.Warning)
        }
        if (day.calendar.isNotEmpty()) item(key = "calendar") { CalendarNoticeCard(day.calendar) }
        if (day.items.isEmpty()) item(key = "empty") {
            Surface(Modifier.fillMaxWidth().padding(vertical = 16.dp), shape = RoutineShapes.Card, color = RoutineColors.Surface1, border = BorderStroke(1.dp, RoutineColors.CardBorder)) {
                Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Icon(Icons.Outlined.Spa, null, tint = RoutineColors.Sage)
                    Text(stringResource(R.string.empty_day_title), style = MaterialTheme.typography.headlineSmall)
                    Text(stringResource(R.string.empty_day_body), color = RoutineColors.TextSecondary)
                    FilledTonalButton(onClick = { onAction(TimelineAction.OpenAdd) }) { Text(stringResource(R.string.plan_first_block)) }
                }
            }
        }
        var previousEnd: Int? = null
        var nowPlaced = activeKey != null
        day.items.forEach { entry ->
            val before = previousEnd
            if (entry is ResolvedTimelineItem.Block && before != null && entry.startMinute > before) {
                val minutes = entry.startMinute - before
                val showNow = today && !nowPlaced && nowMinute in before until entry.startMinute
                if (showNow) nowPlaced = true
                item(key = "gap:${entry.key}") {
                    TimelineGap(minutes, if (showNow) (nowMinute - before).toFloat() / minutes else null, now.toLocalTime().clockLabel())
                }
            }
            if (today && !nowPlaced && nowMinute < entry.startMinute) {
                item(key = "now-before:${entry.key}") { NowMarker(now.toLocalTime().clockLabel(), Modifier.fillMaxWidth().padding(vertical = 8.dp)) }
                nowPlaced = true
            }
            item(key = entry.key, contentType = if (entry is ResolvedTimelineItem.Block) "block" else "milestone") {
                val placement = Modifier.animateItem(placementSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow))
                when (entry) {
                    is ResolvedTimelineItem.Block -> if (entry.origin != RoutineOrigin.USER) ManagedRoutineCard(entry,now,entry.key==activeKey,busy,placement,onAction)
                        else TimelineBlockCard(entry, day.warnings.filter { entry.key in it.itemKeys }, health, now, busy,
                        showNow = entry.key == activeKey, overlaps = before != null && entry.startMinute < before && !entry.isSuppressed,
                        modifier = placement, canStart = execution == null && today, onAction = onAction)
                    is ResolvedTimelineItem.Milestone -> MilestoneCard(entry, busy, onAction, placement)
                }
            }
            if (entry is ResolvedTimelineItem.Block) previousEnd = maxOf(previousEnd ?: 0, entry.endMinute)
        }
        if (today && !nowPlaced) item(key = "now-tail") { NowMarker(now.toLocalTime().clockLabel(), Modifier.fillMaxWidth().padding(vertical = 12.dp)) }
        if (day.cancelled.isNotEmpty()) item(key = "cancelled") {
            OutlinedCard(Modifier.fillMaxWidth().padding(top = 12.dp), shape = RoutineShapes.Card, border = BorderStroke(1.dp, RoutineColors.Border)) {
                Column(Modifier.padding(16.dp)) {
                    Text(stringResource(R.string.skipped_title), style = MaterialTheme.typography.titleSmall)
                    day.cancelled.forEach { cancelled ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(cancelled.title, style = MaterialTheme.typography.bodyMedium)
                                if (cancelled.date != day.date) Text(stringResource(R.string.begins_on, cancelled.date), style = MaterialTheme.typography.labelSmall)
                            }
                            TextButton(enabled = !busy, onClick = { onAction(TimelineAction.Restore(cancelled.routineId, cancelled.date)) }) { Text(stringResource(R.string.restore)) }
                        }
                    }
                }
            }
        }
        item(key = "guidance") {
            Column(Modifier.padding(top = 16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(stringResource(R.string.health_explanation), style = MaterialTheme.typography.bodySmall, color = RoutineColors.TextSecondary)
                Text(stringResource(R.string.recovery_safety), style = MaterialTheme.typography.bodySmall, color = RoutineColors.TextMuted)
            }
        }
    }
}

/** Agent3's proportional gap, with the NOW marker positioned inside genuinely empty time. */
@Composable
private fun TimelineGap(minutes: Int, nowFraction: Float?, clock: String) {
    val height = (minutes * 0.6f).coerceIn(12f, 144f).dp
    Box(Modifier.fillMaxWidth().height(height)) {
        Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
            Spacer(Modifier.width(53.dp))
            Box(Modifier.width(1.5.dp).fillMaxHeight().background(RoutineColors.Spine))
            if (minutes >= 15) Text(stringResource(R.string.unallocated_time, durationLabel(minutes)), Modifier.padding(start = 14.dp), style = MaterialTheme.typography.labelSmall, color = RoutineColors.TextMuted)
        }
        if (nowFraction != null) NowMarker(clock, Modifier.fillMaxWidth().offset(y = (height - 12.dp) * nowFraction.coerceIn(0f, 1f)))
    }
}
