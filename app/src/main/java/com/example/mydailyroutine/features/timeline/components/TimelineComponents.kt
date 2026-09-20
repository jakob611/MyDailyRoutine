package com.example.mydailyroutine.features.timeline.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import com.example.mydailyroutine.core.designsystem.components.RoutineCompletionCheckbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.example.mydailyroutine.R
import com.example.mydailyroutine.core.designsystem.components.ActionRow
import com.example.mydailyroutine.core.designsystem.components.MetaChip
import com.example.mydailyroutine.core.designsystem.components.NowBand
import com.example.mydailyroutine.core.designsystem.components.RoutineLabel
import com.example.mydailyroutine.core.designsystem.components.RoutineSwitch
import com.example.mydailyroutine.core.designsystem.components.RoutineText
import com.example.mydailyroutine.core.designsystem.components.RoutineTextDefaults
import com.example.mydailyroutine.core.designsystem.components.TimeGutter
import com.example.mydailyroutine.core.designsystem.components.categoryBar
import com.example.mydailyroutine.core.designsystem.components.categoryIcon
import com.example.mydailyroutine.core.designsystem.components.timelineRail
import com.example.mydailyroutine.core.designsystem.glass.liquidUnderGlow
import com.example.mydailyroutine.core.designsystem.haptics.LocalRoutineHaptics
import com.example.mydailyroutine.core.designsystem.theme.RoutineColors
import com.example.mydailyroutine.core.designsystem.theme.RoutineMetrics
import com.example.mydailyroutine.core.designsystem.theme.RoutineShapes
import com.example.mydailyroutine.core.designsystem.theme.RoutineSpacing
import com.example.mydailyroutine.core.designsystem.motion.LocalReduceMotion
import com.example.mydailyroutine.core.designsystem.motion.spatialSpec
import com.example.mydailyroutine.core.designsystem.theme.TransitionMillis
import com.example.mydailyroutine.core.designsystem.theme.categoryStyle
import com.example.mydailyroutine.core.platform.Slovenian
import com.example.mydailyroutine.core.presentation.TimelineAction
import com.example.mydailyroutine.core.presentation.WarningUi
import com.example.mydailyroutine.core.presentation.clockLabel
import com.example.mydailyroutine.core.presentation.durationLabel
import com.example.mydailyroutine.core.presentation.label
import com.example.mydailyroutine.core.presentation.minuteLabel
import com.example.mydailyroutine.core.presentation.RoutineDate
import com.example.mydailyroutine.domain.health.HealthConfig
import com.example.mydailyroutine.domain.model.ResolvedTimelineItem
import com.example.mydailyroutine.domain.model.RoutineCategory
import com.example.mydailyroutine.domain.planning.CircadianPenalty
import com.example.mydailyroutine.domain.scheduling.OccurrenceTimes
import java.time.Duration
import java.time.ZonedDateTime
import java.time.format.TextStyle
import kotlin.math.roundToInt

/**
 * Day-view block card.
 *
 * Layout rules enforced here: the hour column comes from the shared [TimeGutter], the rail from
 * [timelineRail], the NOW indicator from [NowBand] (measured and placed, never offset), titles are
 * bounded by [RoutineText] and every action lives in an [ActionRow], so a long Slovenian label moves
 * to the next line at full size instead of shrinking into a vertical strip or an empty ellipsis.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TimelineBlockCard(
    block: ResolvedTimelineItem.Block,
    warnings: List<WarningUi>,
    config: HealthConfig,
    now: ZonedDateTime,
    busy: Boolean,
    showNow: Boolean,
    overlaps: Boolean,
    modifier: Modifier = Modifier,
    canStart: Boolean = false,
    onAction: (TimelineAction) -> Unit,
) {
    var expanded by rememberSaveable(block.key) { mutableStateOf(false) }
    var dragY by remember(block.key) { mutableFloatStateOf(0f) }
    val dragScope = rememberCoroutineScope()
    var dragging by remember(block.key) { mutableStateOf(false) }
    // Release springs the card home. A bare `dragY = 0f` teleports it in one frame, and the eye
    // reads that teleport as the card jumping, even when the commit itself reflows correctly.
    val settleDrag: () -> Unit = {
        dragging = false
        val from = dragY
        dragScope.launch {
            Animatable(from).animateTo(0f, spring(dampingRatio = 0.55f, stiffness = 320f)) { dragY = value }
        }
    }
    val currentAction by rememberUpdatedState(onAction)
    val haptics = LocalRoutineHaptics.current
    val context = LocalContext.current
    val density = LocalDensity.current
    val dragStepPx = with(density) { 30.dp.toPx() } // 15 min x 2 dp/min, the same scale as the timeline.
    val style = categoryStyle(block.category, block.subject?.colorHex)
    val window = remember(block, now.zone) { OccurrenceTimes.window(block, now.zone) }
    val active = !block.isSuppressed && !block.isCompleted &&
        now.toInstant() >= window.start && now.toInstant() < window.end
    val past = now.toInstant() >= window.end || block.isCompleted
    val activeAmount by animateFloatAsState(if (active) 1f else 0f, spatialSpec<Float>(LocalReduceMotion.current), label = "active-border")
    val barColor = block.subject?.let { Color(it.colorHex.toInt()) } ?: style.accent
    val toggleDescription = stringResource(if (block.isCompleted) R.string.mark_not_done else R.string.mark_done)
    val expandLabel = stringResource(if (expanded) R.string.collapse_block else R.string.expand_block)
    val dragHint = stringResource(R.string.drag_hint)
    val durationText = stringResource(R.string.duration_minutes, block.durationMinutes)
    val rangeText = stringResource(
        R.string.time_range,
        window.start.atZone(now.zone).toLocalTime().clockLabel(),
        window.end.atZone(now.zone).toLocalTime().clockLabel(),
    )

    Box(
        modifier.fillMaxWidth()
            .liquidUnderGlow(style.accent, if (past || block.isSuppressed) 0f else 0.10f)
            .zIndex(if (dragging) 1f else 0f),
    ) {
        Row(Modifier.fillMaxWidth().timelineRail(if (active) RoutineColors.Timer.copy(alpha = 0.6f) else RoutineColors.Spine)) {
            val recordedZone = block.actualTiming?.zoneId?.let(java.time.ZoneId::of) ?: now.zone
            TimeGutter(
                start = block.actualTiming?.startedAt?.atZone(recordedZone)?.toLocalTime()?.clockLabel()
                    ?: minuteLabel(block.startMinute),
                end = block.actualTiming?.endedAt?.atZone(recordedZone)?.toLocalTime()?.clockLabel()
                    ?: minuteLabel(block.endMinute),
                emphasized = active,
                muted = past && !active,
            )
            Card(
                onClick = { haptics.tap(); expanded = !expanded },
                modifier = Modifier.weight(1f)
                    // Dragging translates the card only; it never scales, so the lifted card stays
                    // inside its own opaque bounds and cannot mix its text with a neighbour's.
                    .graphicsLayer { translationY = dragY }
                    .animateContentSize(spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow))
                    .pointerInput(block.key, block.startsAt, block.endsAt, busy) {
                        if (!busy && !block.isCompleted && !block.isSuppressed && !block.isFixedCommitment && !block.category.isBuffer) {
                            // One tick per detent the drag passes, the way an iOS picker rail feels:
                            // the finger hears the fifteen-minute steps it cannot see on a dense day.
                            var detent = 0
                            detectDragGesturesAfterLongPress(
                                onDragStart = { dragging = true; detent = 0; haptics.dragStart() },
                                onDrag = { change, amount ->
                                    change.consume()
                                    dragY += amount.y
                                    val step = (dragY / dragStepPx).roundToInt()
                                    if (step != detent) {
                                        detent = step
                                        haptics.dragThreshold()
                                    }
                                },
                                // Cancelling commits nothing, so the release haptic belongs here; a
                                // completed drop is answered by the wrapper's success haptic instead.
                                onDragCancel = { settleDrag(); haptics.dragEnd() },
                                onDragEnd = {
                                    val start = block.startsAt.toLocalTime().toSecondOfDay() / 60
                                    val delta = ((dragY / dragStepPx).roundToInt() * 15).coerceIn(-start, 1439 - start)
                                    if (delta != 0) {
                                        currentAction(
                                            TimelineAction.SaveBlockEdit(
                                                block, block.title,
                                                block.startsAt.toLocalTime().plusMinutes(delta.toLong()),
                                                block.endsAt.toLocalTime().plusMinutes(delta.toLong()), false,
                                            ),
                                        )
                                    }
                                    settleDrag()
                                },
                            )
                        }
                    },
                shape = RoutineShapes.Card,
                colors = CardDefaults.cardColors(containerColor = RoutineColors.cardSurface(style.accent)),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp, pressedElevation = 0.dp),
                border = if (activeAmount > 0.01f) BorderStroke(1.5.dp, RoutineColors.Timer.copy(alpha = 0.20f * activeAmount))
                else BorderStroke(1.dp, RoutineColors.CardBorder),
            ) {
                Box(Modifier.fillMaxWidth()) {
                    Column(
                        Modifier.fillMaxWidth()
                            .defaultMinSize(minHeight = RoutineMetrics.CardMinHeight)
                            .categoryBar(barColor.copy(alpha = if (past || block.isSuppressed) 0.35f else 1f))
                            .padding(
                                start = RoutineMetrics.CardContentStart,
                                end = RoutineSpacing.md,
                                top = RoutineSpacing.md,
                                bottom = RoutineSpacing.md,
                            ),
                        verticalArrangement = Arrangement.spacedBy(RoutineSpacing.sm),
                    ) {
                        Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(RoutineSpacing.sm)) {
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(RoutineSpacing.xs)) {
                                RoutineText(
                                    text = block.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    maxLines = if (expanded) RoutineTextDefaults.Paragraph else RoutineTextDefaults.Body,
                                    color = if (past || block.isSuppressed) RoutineColors.TextSecondary else RoutineColors.TextPrimary,
                                    textDecoration = if (block.isCompleted || block.isSuppressed) TextDecoration.LineThrough else null,
                                )
                                RoutineLabel(
                                    text = "$rangeText · $durationText",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = RoutineColors.TextSecondary,
                                )
                            }
                            Icon(
                                Icons.Outlined.ExpandMore,
                                contentDescription = expandLabel,
                                modifier = Modifier.size(18.dp).rotate(if (expanded) 180f else 0f),
                                tint = RoutineColors.TextSecondary,
                            )
                            if (canStart && !block.isCompleted && !block.isSuppressed && !block.isFixedCommitment && !block.category.isBuffer) {
                                IconButton(enabled = !busy, onClick = { onAction(TimelineAction.StartExecution(block)) }) {
                                    Icon(Icons.Outlined.PlayArrow, stringResource(R.string.execution_start), tint = style.accent)
                                }
                            }
                            if (block.lessonAutoCompleted) {
                                Icon(
                                    Icons.Default.Check,
                                    stringResource(R.string.lesson_auto_done),
                                    Modifier.size(RoutineMetrics.ActionMinWidth).padding(RoutineSpacing.md),
                                    tint = RoutineColors.Success,
                                )
                            } else {
                                RoutineCompletionCheckbox(
                                    checked = block.isCompleted,
                                    onCheckedChange = { onAction(TimelineAction.ToggleComplete(block)) },
                                    enabled = !busy && !block.isSuppressed,
                                    modifier = Modifier.size(RoutineMetrics.ActionMinWidth)
                                        .semantics { contentDescription = toggleDescription },
                                )
                            }
                        }
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(RoutineSpacing.xs),
                            verticalArrangement = Arrangement.spacedBy(RoutineSpacing.xs),
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(RoutineSpacing.xs),
                            ) {
                                Icon(categoryIcon(block.category), null, Modifier.size(16.dp), tint = style.content)
                                MetaChip(block.category.label(), style = style)
                            }
                            if (block.reviewId != null) MetaChip(stringResource(R.string.review_badge), style = RoutineColors.School)
                            if (block.category.isDeepWork && CircadianPenalty().kernel(block.startMinute + block.durationMinutes / 2.0) > 0.45) {
                                MetaChip(stringResource(R.string.circadian_hint), style = RoutineColors.Recovery)
                            }
                            if (active) MetaChip(stringResource(R.string.now), style = style)
                            if (block.hasOverride) MetaChip(stringResource(R.string.moved_today))
                            if (!block.isNotificationEnabled) MetaChip(stringResource(R.string.notifications_off))
                            warnings.firstOrNull()?.let { warning ->
                                WarningBadge(warning, busy) {
                                    onAction(
                                        TimelineAction.InsertRecovery(
                                            warning.type, block.key,
                                            context.getString(R.string.auto_recovery_title),
                                            context.getString(R.string.continuation_suffix),
                                        ),
                                    )
                                }
                            }
                        }
                        if (dragging) {
                            RoutineLabel(
                                text = stringResource(R.string.drag_minutes, (dragY / dragStepPx).roundToInt() * 15),
                                style = MaterialTheme.typography.labelMedium,
                                color = RoutineColors.TextPrimary,
                            )
                        }
                        if (block.isCarryIn) {
                            RoutineText(
                                text = stringResource(
                                    R.string.carry_in,
                                    RoutineDate.tight(block.occurrenceDate),
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = RoutineTextDefaults.Body,
                            )
                        }
                        if (block.isSuppressed) {
                            RoutineText(
                                text = stringResource(R.string.school_inactive, block.holidayTitle.orEmpty()),
                                style = MaterialTheme.typography.bodySmall,
                                color = RoutineColors.TextSecondary,
                                maxLines = RoutineTextDefaults.Body,
                            )
                        }
                        if (overlaps) {
                            RoutineText(
                                text = stringResource(R.string.overlap_notice),
                                style = MaterialTheme.typography.labelSmall,
                                color = RoutineColors.Warning,
                                maxLines = RoutineTextDefaults.Body,
                            )
                        }
                        val clockChanged = if (block.actualTiming != null) {
                            window.start.atZone(recordedZone).offset != window.end.atZone(recordedZone).offset
                        } else {
                            window.start.atZone(now.zone).toLocalDateTime() != block.startsAt ||
                                Duration.between(window.start, window.end) != Duration.between(block.startsAt, block.endsAt)
                        }
                        if (clockChanged) {
                            RoutineText(
                                text = stringResource(
                                    R.string.clock_change,
                                    window.start.atZone(now.zone).toLocalTime().clockLabel(),
                                    durationLabel(Duration.between(window.start, window.end).toMinutes().toInt()),
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = RoutineTextDefaults.Body,
                            )
                        }
                        AnimatedVisibility(
                            visible = expanded,
                            enter = fadeIn(tween(TransitionMillis)) + slideInVertically(tween(TransitionMillis)) { -it / 4 },
                            exit = fadeOut(tween(TransitionMillis)),
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(RoutineSpacing.sm)) {
                                HorizontalDivider(color = RoutineColors.Border)
                                if (block.seriesDays.size > 1) {
                                    RoutineText(
                                        text = stringResource(
                                            R.string.repeat_days_summary,
                                            block.seriesDays.sortedBy { it.value }
                                                .joinToString(", ") { it.getDisplayName(TextStyle.SHORT_STANDALONE, Slovenian) },
                                        ),
                                        style = MaterialTheme.typography.bodySmall,
                                        maxLines = RoutineTextDefaults.Body,
                                    )
                                }
                                RoutineText(
                                    text = if (block.isOneOff) stringResource(R.string.one_off_block)
                                    else stringResource(
                                        R.string.weekly_blueprint,
                                        RoutineDate.weekdayName(block.occurrenceDate),
                                    ),
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = RoutineTextDefaults.Body,
                                )
                                // Actual minutes without a live execution: the reminder said the
                                // lesson happened, the reader knows how long it took - one button,
                                // no execution session required.
                                if (block.actualTiming == null && !block.isSuppressed) {
                                    TextButton(onClick = { onAction(TimelineAction.RequestActual(block)) }, enabled = !busy) {
                                        RoutineLabel(stringResource(R.string.record_actual),
                                            style = MaterialTheme.typography.labelLarge)
                                    }
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Column(Modifier.weight(1f)) {
                                        RoutineText(
                                            text = stringResource(
                                                if (block.category == RoutineCategory.REST_BUFFER) R.string.reminder_at_recovery
                                                else R.string.reminder_before,
                                            ),
                                            style = MaterialTheme.typography.bodySmall,
                                            maxLines = RoutineTextDefaults.Body,
                                        )
                                        if (!block.isOneOff) {
                                            RoutineLabel(
                                                text = stringResource(R.string.applies_every_week),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = RoutineColors.TextSecondary,
                                            )
                                        }
                                    }
                                    RoutineSwitch(
                                        checked = block.isNotificationEnabled,
                                        onCheckedChange = { onAction(TimelineAction.SetReminder(block.routineBlockId, it)) },
                                        enabled = !busy,
                                    )
                                }
                                warnings.forEach { warning ->
                                    HealthWarningCard(warning, config, busy) {
                                        onAction(
                                            TimelineAction.InsertRecovery(
                                                warning.type, block.key,
                                                context.getString(R.string.auto_recovery_title),
                                                context.getString(R.string.continuation_suffix),
                                            ),
                                        )
                                    }
                                }
                                RoutineText(
                                    text = dragHint,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = RoutineColors.TextSecondary,
                                    maxLines = RoutineTextDefaults.Paragraph,
                                )
                                ActionRow {
                                    TextButton(enabled = !busy, onClick = { onAction(TimelineAction.Edit(block)) }) {
                                        RoutineLabel(stringResource(R.string.move_rename), style = MaterialTheme.typography.labelLarge)
                                    }
                                    TextButton(enabled = !busy, onClick = { onAction(TimelineAction.Skip(block)) }) {
                                        RoutineLabel(stringResource(R.string.skip_date), style = MaterialTheme.typography.labelLarge)
                                    }
                                    if (block.hasOverride) {
                                        TextButton(enabled = !busy, onClick = { onAction(TimelineAction.ResetOverride(block)) }) {
                                            RoutineLabel(stringResource(R.string.reset_date), style = MaterialTheme.typography.labelLarge)
                                        }
                                    }
                                    TextButton(enabled = !busy, onClick = { onAction(TimelineAction.RequestDelete(block)) }) {
                                        RoutineLabel(
                                            stringResource(if (block.isOneOff) R.string.delete_block else R.string.delete_routine),
                                            style = MaterialTheme.typography.labelLarge,
                                            color = RoutineColors.Error,
                                        )
                                    }
                                }
                            }
                        }
                    }
                    if (showNow && !dragging) {
                        val total = Duration.between(window.start, window.end).seconds.coerceAtLeast(1)
                        val progress = (Duration.between(window.start, now.toInstant()).seconds.toFloat() / total).coerceIn(0f, 1f)
                        NowBand(
                            time = now.toLocalTime().clockLabel(),
                            progress = progress,
                            modifier = Modifier.matchParentSize(),
                            pulse = pulseAlpha(),
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WarningBadge(warning: WarningUi, busy: Boolean, onInsert: () -> Unit) {
    val pulse = pulseAlpha()
    val description = stringResource(R.string.insert_break_description, warning.recoveryMinutes)
    Surface(
        onClick = onInsert,
        enabled = !busy,
        shape = RoutineShapes.Chip,
        color = RoutineColors.WarningContainer,
        modifier = Modifier.defaultMinSize(minHeight = 36.dp).semantics { contentDescription = description },
    ) {
        Row(
            Modifier.padding(horizontal = RoutineSpacing.sm, vertical = RoutineSpacing.xs),
            horizontalArrangement = Arrangement.spacedBy(RoutineSpacing.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Outlined.Psychology, null, Modifier.size(16.dp).alpha(pulse), tint = RoutineColors.Warning)
            RoutineLabel(
                text = stringResource(R.string.insert_break, warning.recoveryMinutes),
                style = MaterialTheme.typography.labelSmall,
                color = RoutineColors.Warning,
            )
        }
    }
}

/**
 * Standalone NOW row used between two list items, where nothing is underneath it. Over a card the
 * measured [NowBand] is used instead.
 */
@Composable
fun NowMarker(time: String, modifier: Modifier = Modifier, pulse: Float = pulseAlpha()) {
    Surface(color = RoutineColors.Background, modifier = modifier) {
        Row(
            Modifier.fillMaxWidth().heightIn(min = RoutineMetrics.NowBandHeight),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier.size(RoutineMetrics.NowDotSize).alpha(pulse)
                    .background(RoutineColors.Timer, CircleShape),
            )
            Box(Modifier.weight(1f).height(RoutineMetrics.SpineWidth).background(RoutineColors.Timer))
            RoutineLabel(
                text = time,
                modifier = Modifier.alpha(pulse).padding(horizontal = RoutineSpacing.xs),
                style = MaterialTheme.typography.labelSmall,
                color = RoutineColors.Timer,
            )
        }
    }
}

@Composable
fun pulseAlpha(): Float {
    // A breathing indicator is the one animation here the reader never asked for, so it is the first
    // to go: under remove-animations it holds its brightest value and the loop never starts.
    if (LocalReduceMotion.current) return 1f
    val transition = rememberInfiniteTransition(label = "gentle-indicator")
    // A 200 ms reverse loop is a 2.5 Hz strobe, which reads as an alarm, not as "now". iOS Calendar's
    // indicator does not blink at all; the closest honest analogue is one slow sine breath per ~3 s,
    // shallow enough to notice only when you look for it.
    val alpha by transition.animateFloat(
        0.78f, 1f,
        infiniteRepeatable(tween(2800, easing = CubicBezierEasing(0.37f, 0f, 0.63f, 1f)), RepeatMode.Reverse),
        label = "indicator-alpha",
    )
    return alpha
}

@Composable
fun MilestoneCard(
    item: ResolvedTimelineItem.Milestone,
    busy: Boolean,
    onAction: (TimelineAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by rememberSaveable(item.key) { mutableStateOf(false) }
    val haptics = LocalRoutineHaptics.current
    val expandLabel = stringResource(if (expanded) R.string.collapse_block else R.string.expand_block)
    Box(modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().timelineRail(RoutineColors.Spine)) {
            TimeGutter(
                start = item.dueTime?.clockLabel() ?: stringResource(R.string.all_day),
                topPadding = RoutineSpacing.md,
                muted = true,
            )
            Card(
                onClick = { haptics.tap(); expanded = !expanded },
                modifier = Modifier.weight(1f).animateContentSize(),
                shape = RoutineShapes.Card,
                border = BorderStroke(1.dp, RoutineColors.CardBorder),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp, pressedElevation = 0.dp),
                colors = CardDefaults.cardColors(containerColor = RoutineColors.SurfaceContainer),
            ) {
                Column(
                    Modifier.fillMaxWidth().padding(RoutineSpacing.md),
                    verticalArrangement = Arrangement.spacedBy(RoutineSpacing.sm),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(RoutineSpacing.sm)) {
                        Icon(Icons.Outlined.Flag, null, tint = RoutineColors.Error)
                        RoutineText(
                            text = item.title,
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = if (expanded) RoutineTextDefaults.Paragraph else RoutineTextDefaults.Title,
                            textDecoration = if (item.isCompleted) TextDecoration.LineThrough else null,
                        )
                        Icon(
                            Icons.Outlined.ExpandMore,
                            contentDescription = expandLabel,
                            modifier = Modifier.size(18.dp).rotate(if (expanded) 180f else 0f),
                            tint = RoutineColors.TextSecondary,
                        )
                        RoutineCompletionCheckbox(
                            checked = item.isCompleted,
                            onCheckedChange = { onAction(TimelineAction.ToggleComplete(item)) },
                            enabled = !busy,
                            modifier = Modifier.size(RoutineMetrics.ActionMinWidth),
                        )
                    }
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(RoutineSpacing.xs),
                        verticalArrangement = Arrangement.spacedBy(RoutineSpacing.xs),
                    ) {
                        MetaChip(
                            stringResource(if (item.isExam) R.string.category_exam else R.string.category_milestone),
                            style = RoutineColors.Exam,
                        )
                        item.subject?.let { MetaChip(it.name, style = RoutineColors.Personal) }
                    }
                    RoutineText(
                        text = item.dueTime?.let { stringResource(R.string.milestone_due, it.clockLabel()) }
                            ?: stringResource(R.string.marker_all_day),
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = RoutineTextDefaults.Body,
                    )
                    AnimatedVisibility(
                        visible = expanded,
                        enter = fadeIn(tween(TransitionMillis)),
                        exit = fadeOut(tween(TransitionMillis)),
                    ) {
                        ActionRow {
                            TextButton(enabled = !busy, onClick = { onAction(TimelineAction.Edit(item)) }) {
                                RoutineLabel(stringResource(R.string.edit), style = MaterialTheme.typography.labelLarge)
                            }
                            TextButton(enabled = !busy, onClick = { onAction(TimelineAction.RequestDelete(item)) }) {
                                RoutineLabel(stringResource(R.string.delete), style = MaterialTheme.typography.labelLarge, color = RoutineColors.Error)
                            }
                        }
                    }
                }
            }
        }
    }
}
