package com.example.mydailyroutine.features.timeline.presentation

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Spa
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.mydailyroutine.R
import com.example.mydailyroutine.core.designsystem.components.ActionRow
import com.example.mydailyroutine.core.designsystem.components.NowBand
import com.example.mydailyroutine.core.designsystem.components.RoutineLabel
import com.example.mydailyroutine.core.designsystem.components.RoutineText
import com.example.mydailyroutine.core.designsystem.components.RoutineTextDefaults
import com.example.mydailyroutine.core.designsystem.components.SectionHeader
import com.example.mydailyroutine.core.designsystem.theme.RoutineColors
import com.example.mydailyroutine.core.designsystem.theme.RoutineMetrics
import com.example.mydailyroutine.core.designsystem.theme.RoutineShapes
import com.example.mydailyroutine.core.designsystem.theme.RoutineSpacing
import com.example.mydailyroutine.core.presentation.DayUi
import com.example.mydailyroutine.core.presentation.MinimalEvening
import com.example.mydailyroutine.core.presentation.TimelineAction
import com.example.mydailyroutine.core.presentation.clockLabel
import com.example.mydailyroutine.core.presentation.durationLabel
import com.example.mydailyroutine.core.presentation.RoutineDate
import com.example.mydailyroutine.domain.calendar.SlovenianAcademicCalendar
import com.example.mydailyroutine.domain.execution.ActiveExecution
import com.example.mydailyroutine.domain.health.HealthConfig
import com.example.mydailyroutine.domain.model.ResolvedTimelineItem
import com.example.mydailyroutine.domain.model.Task
import com.example.mydailyroutine.domain.planning.PlanningConfig
import com.example.mydailyroutine.domain.routines.RoutineOrigin
import com.example.mydailyroutine.domain.scheduling.OccurrenceTimes
import com.example.mydailyroutine.features.routines.presentation.ManagedRoutineCard
import com.example.mydailyroutine.features.timeline.components.CalendarNoticeCard
import com.example.mydailyroutine.features.timeline.components.DayLoadBar
import com.example.mydailyroutine.features.timeline.components.MetricTile
import com.example.mydailyroutine.features.timeline.components.MilestoneCard
import com.example.mydailyroutine.features.timeline.components.NowMarker
import com.example.mydailyroutine.features.timeline.components.TimelineBlockCard
import com.example.mydailyroutine.features.timeline.components.pulseAlpha
import java.time.LocalDate
import java.time.ZonedDateTime

/**
 * The day list: one glance card, the contextual notices, then the schedule.
 *
 * The glance card groups the numbers that used to be six loose lines of different type sizes, the
 * action row reflows instead of squeezing its labels, and gaps share the rail geometry of the cards.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DailyTimeline(
    day: DayUi,
    now: ZonedDateTime,
    busy: Boolean,
    health: HealthConfig,
    planning: PlanningConfig,
    backlogCount: Int,
    execution: ActiveExecution?,
    dueTasks: List<Task>,
    onAction: (TimelineAction) -> Unit,
    topInset: Dp = 0.dp,
    /** Space the floating add control and the navigation bar take; measured, see RoutineApp. */
    bottomInset: Dp = RoutineMetrics.ListBottomInset,
    /** What the first-run flow was told to call the reader; empty when nobody was asked. */
    userName: String = "",
    /** The reader already asked to see this whole evening (N15): the protocol stands down. */
    eveningFull: Boolean = false,
    /** The reader already put the skipped blocks aside for this date (N6). */
    skippedHidden: Boolean = false,
    /** Tomorrow holds an exam or a deadline: the minimal-state protocol never touches that evening. */
    tomorrowHasDeadline: Boolean = false,
) {
    val today = day.date == now.toLocalDate()
    val nowMinute = now.hour * 60 + now.minute
    // A day with nothing in it has no summary: three zeros, an empty load bar, "0 min of reserve"
    // and two buttons that would do nothing are the loudest thing on the screen a reader sees first.
    // The empty-day card below already says everything, and it says it once.
    val hasItems = day.items.isNotEmpty()
    // Healing is offered when the day has actually slipped (a block that should have ended and is
    // neither done nor set aside) or when the queue holds something. On a tidy day both buttons
    // would be no-ops, and a no-op button is worse than no button.
    val slipped = today && day.items.filterIsInstance<ResolvedTimelineItem.Block>().any {
        !it.isCompleted && !it.isSuppressed && it.endMinute <= nowMinute
    }
    val offerHealing = slipped || backlogCount > 0
    val noValue = stringResource(R.string.value_none)
    val completedValue = if (day.metrics.blockCount == 0) noValue
    else stringResource(R.string.completed_count, day.metrics.completedCount, day.metrics.blockCount)
    val completedColor = when {
        day.metrics.blockCount == 0 || day.metrics.completedCount == 0 -> RoutineColors.TextMuted
        day.metrics.completedCount == day.metrics.blockCount -> RoutineColors.Success
        else -> RoutineColors.TextPrimary
    }
    val focusValue = if (day.metrics.focusMinutes == 0) noValue else durationLabel(day.metrics.focusMinutes)
    val recoveryValue = if (day.metrics.recoveryMinutes == 0) noValue else durationLabel(day.metrics.recoveryMinutes)
    val activeKey = remember(day.items, now) {
        if (!today) null else day.items.filterIsInstance<ResolvedTimelineItem.Block>().firstOrNull {
            val window = OccurrenceTimes.window(it, now.zone)
            now.toInstant() >= window.start && now.toInstant() < window.end
        }?.key
    }
    // The minimal-state protocol, decided here because "the hour is late" is only true of a clock,
    // not of a database row. It hides elastic focus work that has not started; everything the day is
    // built on stays, and one line with "Pokaži vse" is what keeps the hiding reversible (N15).
    val blocks = day.items.filterIsInstance<ResolvedTimelineItem.Block>()
    val hiddenKeys = if (!eveningFull) MinimalEvening.hiddenKeys(blocks, nowMinute, runningKey = activeKey) else emptySet()
    val eveningQuiet = MinimalEvening.engages(
        blocks = blocks,
        nowMinute = nowMinute,
        isToday = today,
        tomorrowHasDeadline = tomorrowHasDeadline,
        requestedFull = eveningFull,
    )
    val items = if (eveningQuiet) day.items.filterNot { it.key in hiddenKeys } else day.items
    LazyColumn(
        // Tagged so a test can prove the list starts at the very top of the window and slides
        // under the glass bar instead of stopping below it.
        Modifier.fillMaxSize().testTag("day-list"),
        contentPadding = PaddingValues(RoutineMetrics.ScreenPadding, topInset + RoutineSpacing.md,
            RoutineMetrics.ScreenPadding, bottomInset),
        verticalArrangement = Arrangement.spacedBy(RoutineSpacing.sm),
    ) {
        item(key = "summary") {
            Column(verticalArrangement = Arrangement.spacedBy(RoutineSpacing.md), modifier = Modifier.padding(bottom = RoutineSpacing.md)) {
                RoutineText(
                    text = stringResource(R.string.day_heading),
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = RoutineTextDefaults.Body,
                    // The headline of the screen a reader lands on: a heading is what lets a screen
                    // reader jump straight to the day instead of walking through the summary tiles.
                    heading = true,
                )
                if (hasItems) Surface(
                    Modifier.fillMaxWidth(),
                    shape = RoutineShapes.Card,
                    color = RoutineColors.Surface1,
                    border = BorderStroke(1.dp, RoutineColors.CardBorder),
                ) {
                    Column(
                        Modifier.fillMaxWidth().padding(RoutineSpacing.md),
                        verticalArrangement = Arrangement.spacedBy(RoutineSpacing.md),
                    ) {
                        Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(RoutineSpacing.sm)) {
                            MetricTile(
                                stringResource(R.string.metric_focus),
                                focusValue,
                                Modifier.weight(1f).fillMaxHeight(),
                                valueColor = if (day.metrics.focusMinutes == 0) RoutineColors.TextMuted else Color.Unspecified,
                            )
                            MetricTile(
                                stringResource(R.string.metric_recovery),
                                recoveryValue,
                                Modifier.weight(1f).fillMaxHeight(),
                                valueColor = if (day.metrics.recoveryMinutes == 0) RoutineColors.TextMuted else Color.Unspecified,
                            )
                            MetricTile(
                                stringResource(R.string.metric_completed),
                                completedValue,
                                Modifier.weight(1f).fillMaxHeight(),
                                valueColor = completedColor,
                            )
                        }
                        DayLoadBar(day.items)
                        if (day.metrics.reserveMinutes > 0 || day.warnings.isNotEmpty()) Row(
                            Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(RoutineSpacing.sm),
                        ) {
                            RoutineLabel(
                                text = stringResource(R.string.reserve_remaining, day.metrics.reserveMinutes),
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.labelMedium,
                                color = RoutineColors.Success,
                            )
                            if (day.warnings.isNotEmpty()) {
                                RoutineLabel(
                                    text = stringResource(R.string.health_suggestion_count, day.warnings.size),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = RoutineColors.Warning,
                                )
                            }
                        }
                    }
                }
                if (dueTasks.isNotEmpty()) {
                    OutlinedCard(
                        onClick = { onAction(TimelineAction.OpenTasks) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoutineShapes.Card,
                        colors = CardDefaults.cardColors(containerColor = RoutineColors.Error.copy(alpha = 0.10f)),
                        border = BorderStroke(1.dp, RoutineColors.Error.copy(alpha = 0.35f)),
                    ) {
                        Column(
                            Modifier.fillMaxWidth().padding(RoutineSpacing.md),
                            verticalArrangement = Arrangement.spacedBy(RoutineSpacing.xs),
                        ) {
                            RoutineText(
                                text = stringResource(if (today) R.string.tasks_due_today else R.string.tasks_due_on_day, dueTasks.size),
                                style = MaterialTheme.typography.titleSmall,
                                color = RoutineColors.Error,
                                maxLines = RoutineTextDefaults.Body,
                            )
                            val shown = dueTasks.take(2).joinToString(" · ") { it.title }
                            RoutineText(
                                text = if (dueTasks.size > 2) stringResource(R.string.more_items_suffix, shown, dueTasks.size - 2) else shown,
                                style = MaterialTheme.typography.bodySmall,
                                color = RoutineColors.TextSecondary,
                                maxLines = RoutineTextDefaults.Body,
                            )
                        }
                    }
                }
                execution?.let { active ->
                    Surface(color = RoutineColors.Focus.container, contentColor = RoutineColors.Focus.content,
                        shape = RoutineShapes.Card, modifier = Modifier.fillMaxWidth()) {
                        Column(
                            Modifier.fillMaxWidth().padding(RoutineSpacing.md),
                            verticalArrangement = Arrangement.spacedBy(RoutineSpacing.sm),
                        ) {
                            RoutineLabel(
                                text = stringResource(if (active.stoppedAt == null) R.string.execution_running else R.string.execution_stopped),
                                style = MaterialTheme.typography.titleSmall,
                            )
                            RoutineLabel(
                                text = stringResource(R.string.execution_elapsed_minutes, active.elapsedMinutes(now.toInstant())),
                                style = MaterialTheme.typography.displaySmall,
                            )
                            RoutineLabel(
                                text = stringResource(R.string.execution_elapsed_label),
                                style = MaterialTheme.typography.labelMedium,
                            )
                            ActionRow {
                                TextButton(
                                    enabled = !busy,
                                    onClick = { onAction(TimelineAction.FinishExecution) },
                                    colors = ButtonDefaults.textButtonColors(contentColor = RoutineColors.Focus.content),
                                ) {
                                    RoutineLabel(stringResource(R.string.execution_finish), style = MaterialTheme.typography.labelLarge)
                                }
                                TextButton(enabled = !busy, onClick = { onAction(TimelineAction.RequestCancelExecution) }) {
                                    RoutineLabel(stringResource(R.string.execution_cancel), style = MaterialTheme.typography.labelLarge,
                                        color = RoutineColors.TextSecondary)
                                }
                            }
                        }
                    }
                }
                if (offerHealing) ActionRow {
                    FilledTonalButton(
                        enabled = !busy && day.items.isNotEmpty() && day.date >= LocalDate.now(),
                        onClick = {
                            val actual = if (today) nowMinute
                            else (day.items.filterIsInstance<ResolvedTimelineItem.Block>()
                                .filter { !it.isCompleted && !it.isSuppressed }
                                .minOfOrNull { it.startMinute } ?: 0) + planning.defaultSlipMinutes
                            onAction(TimelineAction.AutoHeal(day.date, actual.coerceIn(0, 2879)))
                        },
                    ) { RoutineLabel(stringResource(R.string.auto_heal), style = MaterialTheme.typography.labelLarge) }
                    OutlinedButton(onClick = { onAction(TimelineAction.OpenPlanning) }) {
                        RoutineLabel(stringResource(R.string.backlog_count, backlogCount), style = MaterialTheme.typography.labelLarge)
                    }
                }
                if (offerHealing) RoutineText(
                    text = stringResource(R.string.auto_heal_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = RoutineColors.TextSecondary,
                    maxLines = RoutineTextDefaults.Paragraph,
                )
                // The line that makes the quiet evening honest: the plan is still there, one tap away,
                // and nothing was deleted on the reader's behalf.
                if (eveningQuiet) Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(RoutineSpacing.sm),
                ) {
                    RoutineText(
                        text = stringResource(R.string.evening_enough),
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyMedium,
                        color = RoutineColors.TextSecondary,
                        maxLines = RoutineTextDefaults.Body,
                    )
                    TextButton(enabled = !busy, onClick = { onAction(TimelineAction.ShowEveningFull(day.date)) }) {
                        RoutineLabel(stringResource(R.string.evening_show_all), style = MaterialTheme.typography.labelLarge)
                    }
                }
                // Neutral postponement (N6): after eight in the evening the day offers to put the skipped
                // blocks aside. It is an offer, never a deletion, and it is not a warning colour.
                if (today && day.cancelled.isNotEmpty() && !skippedHidden && nowMinute >= MinimalEvening.AmnestyHour * 60) Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(RoutineSpacing.sm),
                ) {
                    RoutineText(
                        text = stringResource(R.string.evening_skipped_line),
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyMedium,
                        color = RoutineColors.TextSecondary,
                        maxLines = RoutineTextDefaults.Body,
                    )
                    TextButton(enabled = !busy, onClick = { onAction(TimelineAction.HideSkipped(day.date)) }) {
                        RoutineLabel(stringResource(R.string.evening_hide_skipped), style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
        if (!SlovenianAcademicCalendar.covers(day.date)) {
            item(key = "coverage") {
                RoutineText(
                    text = stringResource(R.string.coverage_warning),
                    style = MaterialTheme.typography.bodySmall,
                    color = RoutineColors.Warning,
                    maxLines = RoutineTextDefaults.Paragraph,
                )
            }
        }
        if (day.calendar.isNotEmpty()) item(key = "calendar") { CalendarNoticeCard(day.calendar) }
        if (day.items.isEmpty()) {
            item(key = "empty") {
                Surface(
                    Modifier.fillMaxWidth().padding(vertical = RoutineSpacing.lg),
                    shape = RoutineShapes.Card,
                    color = RoutineColors.Surface1,
                    border = BorderStroke(1.dp, RoutineColors.CardBorder),
                ) {
                    Column(
                        Modifier.fillMaxWidth().padding(RoutineSpacing.xl),
                        verticalArrangement = Arrangement.spacedBy(RoutineSpacing.md),
                    ) {
                        Icon(Icons.Outlined.Spa, null, tint = RoutineColors.Success)
                        // The tagline used to occupy the top bar of every screen, where it cost
                        // height forever and said nothing the reader needed. Here it is an eyebrow
                        // on the one screen with room for it, and it reads as an invitation.
                        // With a name to use, the eyebrow greets; without one it keeps the tagline,
                        // because an invented "Hi there" is worse than saying nothing personal.
                        if (userName.isBlank()) {
                            RoutineLabel(
                                text = stringResource(R.string.app_tagline),
                                style = MaterialTheme.typography.labelSmall,
                                color = RoutineColors.TextSecondary,
                            )
                        } else {
                            RoutineLabel(
                                text = stringResource(R.string.empty_day_greeting, userName),
                                style = MaterialTheme.typography.labelSmall,
                                color = RoutineColors.Primary,
                            )
                        }
                        RoutineText(
                            text = stringResource(R.string.empty_day_title),
                            style = MaterialTheme.typography.headlineSmall,
                            maxLines = RoutineTextDefaults.Title,
                        )
                        RoutineText(
                            text = stringResource(R.string.empty_day_body),
                            color = RoutineColors.TextSecondary,
                            maxLines = RoutineTextDefaults.Paragraph,
                        )
                        // Two ways out of an empty day, side by side: write the first block yourself, or
                        // fill the day with the IB example and start from something real. The second one
                        // used to live three taps deep in Settings, which is where nobody looks on day one.
                        ActionRow {
                            // One tap, one ready-made block: the editor opens with a 45-minute focus block
                            // at the current time, so the first thing a new reader does is edit a block that
                            // already exists rather than fill in an empty form (N11).
                            FilledTonalButton(onClick = { onAction(TimelineAction.OpenFirstBlock) }) {
                                RoutineLabel(stringResource(R.string.plan_first_block), style = MaterialTheme.typography.labelLarge)
                            }
                            OutlinedButton(
                                onClick = { onAction(TimelineAction.LoadDemo) },
                                shape = RoutineShapes.Pill,
                            ) {
                                RoutineLabel(stringResource(R.string.onboarding_start_demo), style = MaterialTheme.typography.labelLarge)
                            }
                        }
                    }
                }
            }
        }
        var previousEnd: Int? = null
        var nowPlaced = activeKey != null
        items.forEach { entry ->
            val before = previousEnd
            if (entry is ResolvedTimelineItem.Block && before != null && entry.startMinute > before) {
                val minutes = entry.startMinute - before
                val showNow = today && !nowPlaced && nowMinute in before until entry.startMinute
                if (showNow) nowPlaced = true
                item(key = "gap:${entry.key}") {
                    TimelineGap(
                        minutes = minutes,
                        nowFraction = if (showNow) (nowMinute - before).toFloat() / minutes else null,
                        clock = now.toLocalTime().clockLabel(),
                    )
                }
            }
            if (today && !nowPlaced && nowMinute < entry.startMinute) {
                item(key = "now-before:${entry.key}") {
                    NowMarker(now.toLocalTime().clockLabel(), Modifier.fillMaxWidth().padding(vertical = RoutineSpacing.sm))
                }
                nowPlaced = true
            }
            item(key = entry.key, contentType = if (entry is ResolvedTimelineItem.Block) "block" else "milestone") {
                val placement = Modifier.animateItem(placementSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow))
                when (entry) {
                    is ResolvedTimelineItem.Block ->
                        if (entry.origin != RoutineOrigin.USER) {
                            ManagedRoutineCard(entry, now, entry.key == activeKey, busy, placement, onAction)
                        } else {
                            TimelineBlockCard(
                                entry, day.warnings.filter { entry.key in it.itemKeys }, health, now, busy,
                                showNow = entry.key == activeKey,
                                overlaps = before != null && entry.startMinute < before && !entry.isSuppressed,
                                modifier = placement,
                                canStart = execution == null && today,
                                onAction = onAction,
                            )
                        }
                    is ResolvedTimelineItem.Milestone -> MilestoneCard(entry, busy, onAction, placement)
                }
            }
            if (entry is ResolvedTimelineItem.Block) previousEnd = maxOf(previousEnd ?: 0, entry.endMinute)
        }
        if (today && !nowPlaced) {
            item(key = "now-tail") {
                NowMarker(now.toLocalTime().clockLabel(), Modifier.fillMaxWidth().padding(vertical = RoutineSpacing.md))
            }
        }
        if (day.cancelled.isNotEmpty() && !skippedHidden) {
            item(key = "cancelled") {
                OutlinedCard(
                    Modifier.fillMaxWidth().padding(top = RoutineSpacing.md),
                    shape = RoutineShapes.Card,
                    border = BorderStroke(1.dp, RoutineColors.CardBorder),
                ) {
                    Column(Modifier.fillMaxWidth().padding(RoutineSpacing.lg), verticalArrangement = Arrangement.spacedBy(RoutineSpacing.sm)) {
                        SectionHeader(stringResource(R.string.skipped_title), style = MaterialTheme.typography.titleSmall)
                        day.cancelled.forEach { cancelled ->
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(RoutineSpacing.sm)) {
                                Column(Modifier.weight(1f)) {
                                    RoutineText(cancelled.title, style = MaterialTheme.typography.bodyMedium, maxLines = RoutineTextDefaults.Body)
                                    if (cancelled.date != day.date) {
                                        RoutineLabel(
                                            text = stringResource(
                                                R.string.begins_on,
                                                RoutineDate.tight(cancelled.date),
                                            ),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = RoutineColors.TextSecondary,
                                        )
                                    }
                                }
                                TextButton(enabled = !busy, onClick = { onAction(TimelineAction.Restore(cancelled.routineId, cancelled.date)) }) {
                                    RoutineLabel(stringResource(R.string.restore), style = MaterialTheme.typography.labelLarge)
                                }
                            }
                        }
                    }
                }
            }
        }
        item(key = "guidance") {
            Column(Modifier.padding(top = RoutineSpacing.lg), verticalArrangement = Arrangement.spacedBy(RoutineSpacing.sm)) {
                RoutineText(
                    text = stringResource(R.string.health_explanation),
                    style = MaterialTheme.typography.bodySmall,
                    color = RoutineColors.TextSecondary,
                    maxLines = RoutineTextDefaults.Paragraph,
                )
                RoutineText(
                    text = stringResource(R.string.recovery_safety),
                    style = MaterialTheme.typography.bodySmall,
                    color = RoutineColors.TextMuted,
                    maxLines = RoutineTextDefaults.Paragraph,
                )
            }
        }
    }
}

/**
 * Proportional gap with the NOW marker placed inside genuinely empty time. The marker is measured by
 * [NowBand], so it can never land on top of the "unallocated time" label as two overlapping texts:
 * the band is opaque and owns its full width.
 */
@Composable
private fun TimelineGap(minutes: Int, nowFraction: Float?, clock: String) {
    val height = (minutes * 0.6f).coerceIn(12f, 144f).dp
    Box(Modifier.fillMaxWidth().height(height)) {
        Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.width(RoutineMetrics.RailX).fillMaxHeight())
            Box(
                Modifier.width(RoutineMetrics.SpineWidth).fillMaxHeight().background(RoutineColors.Spine),
            )
            if (minutes >= 15) {
                RoutineLabel(
                    text = stringResource(R.string.unallocated_time, durationLabel(minutes)),
                    modifier = Modifier.padding(start = RoutineSpacing.md),
                    style = MaterialTheme.typography.labelSmall,
                    color = RoutineColors.TextMuted,
                )
            }
        }
        if (nowFraction != null) {
            NowBand(
                time = clock,
                progress = nowFraction.coerceIn(0f, 1f),
                modifier = Modifier.matchParentSize(),
                pulse = pulseAlpha(),
            )
        }
    }
}
