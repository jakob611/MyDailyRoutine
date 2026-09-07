package com.example.mydailyroutine.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.example.mydailyroutine.R
import com.example.mydailyroutine.domain.health.HealthConfig
import com.example.mydailyroutine.domain.planning.CircadianPenalty
import com.example.mydailyroutine.domain.model.ResolvedTimelineItem
import com.example.mydailyroutine.domain.model.RoutineCategory
import com.example.mydailyroutine.domain.scheduling.OccurrenceTimes
import com.example.mydailyroutine.platform.Slovenian
import com.example.mydailyroutine.ui.feedback.LocalRoutineHaptics
import com.example.mydailyroutine.ui.theme.*
import com.example.mydailyroutine.ui.timeline.*
import java.time.Duration
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

/** Port of agent3's gutter/card/actions composition, adapted to agent4's occurrence identity and UDF. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TimelineBlockCard(
    block: ResolvedTimelineItem.Block, warnings: List<WarningUi>, config: HealthConfig,
    now: ZonedDateTime, busy: Boolean, showNow: Boolean, overlaps: Boolean,
    modifier: Modifier = Modifier, onAction: (TimelineAction) -> Unit,
) {
    var expanded by rememberSaveable(block.key) { mutableStateOf(false) }
    var dragY by remember(block.key) { mutableFloatStateOf(0f) }
    var dragging by remember(block.key) { mutableStateOf(false) }
    var heightPx by remember(block.key) { mutableIntStateOf(0) }
    val currentAction by rememberUpdatedState(onAction)
    val haptics = LocalRoutineHaptics.current
    val context = LocalContext.current
    val density = LocalDensity.current
    val dragStepPx = with(density) { 24.dp.toPx() }
    val style = categoryStyle(block.category, block.subject?.colorHex)
    val window = remember(block, now.zone) { OccurrenceTimes.window(block, now.zone) }
    val active = !block.isSuppressed && !block.isCompleted && now.toInstant() >= window.start && now.toInstant() < window.end
    val past = now.toInstant() >= window.end || block.isCompleted
    val activeAmount by animateFloatAsState(if (active) 1f else 0f, SnappySpring, label = "active-border")
    val scale by animateFloatAsState(if (dragging) 1.02f else 1f, PopSpring, label = "drag-lift")
    val barColor = block.subject?.let { androidx.compose.ui.graphics.Color(it.colorHex.toInt()) } ?: style.accent
    val toggleDescription = stringResource(if (block.isCompleted) R.string.mark_not_done else R.string.mark_done)
    val dragHint = stringResource(R.string.drag_hint)

    Box(modifier.fillMaxWidth().zIndex(if (dragging) 1f else 0f).onSizeChanged { heightPx = it.height }) {
        Row(Modifier.fillMaxWidth().drawBehind {
            val x = if (layoutDirection == LayoutDirection.Rtl) size.width - 53.dp.toPx() else 53.dp.toPx()
            drawLine(if (active) style.accent.copy(alpha = 0.6f) else RoutineColors.Spine,
                Offset(x, 0f), Offset(x, size.height), 1.5.dp.toPx())
        }) {
            TimeGutter(block.startMinute, block.endMinute, active, past)
            Card(
                onClick = { haptics.tap(); expanded = !expanded },
                modifier = Modifier.weight(1f).graphicsLayer { translationY = dragY; scaleX = scale; scaleY = scale }
                    .animateContentSize(spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow))
                    .semantics { contentDescription = dragHint }
                    .pointerInput(block.key, block.startsAt, block.endsAt, busy) {
                        if (!busy && !block.isCompleted && !block.isSuppressed) detectDragGesturesAfterLongPress(
                            onDragStart = { dragging = true; haptics.dragStart() },
                            onDrag = { change, amount -> change.consume(); dragY += amount.y },
                            onDragCancel = { dragY = 0f; dragging = false },
                            onDragEnd = {
                                val start = block.startsAt.toLocalTime().toSecondOfDay() / 60
                                val delta = ((dragY / dragStepPx).roundToInt() * 15).coerceIn(-start, 1439 - start)
                                if (delta != 0) currentAction(TimelineAction.SaveBlockEdit(block, block.title,
                                    block.startsAt.toLocalTime().plusMinutes(delta.toLong()), block.endsAt.toLocalTime().plusMinutes(delta.toLong()), false))
                                dragY = 0f; dragging = false
                            },
                        )
                    },
                shape = RoutineShapes.Card,
                colors = CardDefaults.cardColors(containerColor = RoutineColors.Surface1),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp, pressedElevation = 0.dp),
                border = if (activeAmount > 0.01f) BorderStroke(1.5.dp, style.accent.copy(alpha = 0.6f * activeAmount))
                    else BorderStroke(1.dp, RoutineColors.CardBorder),
            ) {
                Column(Modifier.fillMaxWidth().heightIn(min = maxOf(112.dp, 2.dp * block.durationMinutes))
                    .drawBehind {
                        val x = if (layoutDirection == LayoutDirection.Rtl) size.width - 4.dp.toPx() else 0f
                        drawRect(barColor.copy(alpha = if (past || block.isSuppressed) 0.35f else 1f), Offset(x, 0f), androidx.compose.ui.geometry.Size(4.dp.toPx(), size.height))
                    }.padding(start = 16.dp, end = 12.dp, top = 12.dp, bottom = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(block.title, style = MaterialTheme.typography.titleMedium, maxLines = if (expanded) 6 else 2, overflow = TextOverflow.Ellipsis,
                                color = if (past || block.isSuppressed) RoutineColors.TextMuted else RoutineColors.TextPrimary,
                                textDecoration = if (block.isCompleted || block.isSuppressed) TextDecoration.LineThrough else null)
                            Text(stringResource(R.string.duration_minutes, block.durationMinutes), style = MaterialTheme.typography.bodySmall, color = RoutineColors.TextSecondary)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Checkbox(block.isCompleted, enabled = !busy && !block.isSuppressed,
                                onCheckedChange = { onAction(TimelineAction.ToggleComplete(block)) },
                                modifier = Modifier.size(40.dp).semantics { contentDescription = toggleDescription })
                            warnings.firstOrNull()?.let { warning -> WarningBadge(warning, busy) {
                                onAction(TimelineAction.InsertRecovery(warning.type, block.key,
                                    context.getString(R.string.auto_recovery_title), context.getString(R.string.continuation_suffix)))
                            } }
                        }
                    }
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        MetaChip(block.category.label(), style)
                        if (block.reviewId != null) MetaChip(stringResource(R.string.review_badge), RoutineColors.School)
                        if (block.category.isDeepWork && CircadianPenalty().kernel(block.startMinute + block.durationMinutes / 2.0) > 0.45)
                            MetaChip(stringResource(R.string.circadian_hint), RoutineColors.Recovery)
                        if (active) MetaChip(stringResource(R.string.now), style)
                        if (block.hasOverride) MetaChip(stringResource(R.string.moved_today))
                        if (!block.isNotificationEnabled) MetaChip(stringResource(R.string.notifications_off))
                    }
                    if (dragging) Text(stringResource(R.string.drag_minutes, (dragY / dragStepPx).roundToInt() * 15), style = MaterialTheme.typography.labelMedium)
                    if (block.isCarryIn) Text(stringResource(R.string.carry_in, block.occurrenceDate.format(DateTimeFormatter.ofPattern("d. M.", Slovenian))), style = MaterialTheme.typography.bodySmall)
                    if (block.isSuppressed) Text(stringResource(R.string.school_inactive, block.holidayTitle.orEmpty()), style = MaterialTheme.typography.bodySmall, color = RoutineColors.TextMuted)
                    if (overlaps) Text(stringResource(R.string.overlap_notice), style = MaterialTheme.typography.labelSmall, color = RoutineColors.Warning)
                    if (window.start.atZone(now.zone).toLocalDateTime() != block.startsAt || Duration.between(window.start, window.end) != Duration.between(block.startsAt, block.endsAt)) {
                        Text(stringResource(R.string.clock_change, window.start.atZone(now.zone).toLocalTime().clockLabel(), durationLabel(Duration.between(window.start, window.end).toMinutes().toInt())), style = MaterialTheme.typography.bodySmall)
                    }
                    AnimatedVisibility(expanded, enter = fadeIn(tween(TransitionMillis)) + slideInVertically(tween(TransitionMillis)) { -it / 4 }, exit = fadeOut(tween(TransitionMillis))) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            HorizontalDivider(color = RoutineColors.Border)
                            Text(if (block.isOneOff) stringResource(R.string.one_off_block) else stringResource(R.string.weekly_blueprint,
                                block.occurrenceDate.format(DateTimeFormatter.ofPattern("EEEE", Slovenian))), style = MaterialTheme.typography.bodySmall)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(stringResource(if (block.category == RoutineCategory.REST_BUFFER) R.string.reminder_at_recovery else R.string.reminder_before), style = MaterialTheme.typography.bodySmall)
                                    if (!block.isOneOff) Text(stringResource(R.string.applies_every_week), style = MaterialTheme.typography.labelSmall)
                                }
                                Switch(block.isNotificationEnabled, { onAction(TimelineAction.SetReminder(block.routineBlockId, it)) }, enabled = !busy)
                            }
                            warnings.forEach { warning ->
                                HealthWarningCard(warning, config, busy) {
                                    onAction(TimelineAction.InsertRecovery(warning.type, block.key, context.getString(R.string.auto_recovery_title), context.getString(R.string.continuation_suffix)))
                                }
                            }
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                TextButton(enabled = !busy, onClick = { onAction(TimelineAction.Edit(block)) }) { Text(stringResource(R.string.move_rename)) }
                                TextButton(enabled = !busy, onClick = { onAction(TimelineAction.Skip(block)) }) { Text(stringResource(R.string.skip_date)) }
                                if (block.hasOverride) TextButton(enabled = !busy, onClick = { onAction(TimelineAction.ResetOverride(block)) }) { Text(stringResource(R.string.reset_date)) }
                                TextButton(enabled = !busy, onClick = { onAction(TimelineAction.RequestDelete(block)) }) { Text(stringResource(if (block.isOneOff) R.string.delete_block else R.string.delete_routine), color = RoutineColors.Crimson) }
                            }
                        }
                    }
                }
            }
        }
        if (showNow && !dragging && heightPx > 0) {
            val progress = (Duration.between(window.start, now.toInstant()).seconds.toFloat() / Duration.between(window.start, window.end).seconds.coerceAtLeast(1)).coerceIn(0f, 1f)
            val offset = with(density) { (heightPx * progress).coerceIn(12.dp.toPx(), heightPx - 12.dp.toPx()).toDp() }
            NowMarker(now.toLocalTime().clockLabel(), Modifier.offset(y = offset).fillMaxWidth())
        }
    }
}

@Composable
private fun TimeGutter(start: Int, end: Int, active: Boolean, past: Boolean) {
    Column(Modifier.width(60.dp).padding(top = 12.dp, end = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(minuteLabel(start), style = MaterialTheme.typography.bodySmall, color = if (active) RoutineColors.TextPrimary else if (past) RoutineColors.TextMuted else RoutineColors.TextSecondary)
        Text(minuteLabel(end), style = MaterialTheme.typography.bodySmall, color = RoutineColors.TextMuted)
    }
}

@Composable
fun MetaChip(text: String, style: CategoryStyle = RoutineColors.Personal) {
    Surface(shape = RoutineShapes.Chip, color = style.container, contentColor = style.content) {
        Text(text, Modifier.padding(horizontal = 7.dp, vertical = 3.dp), style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
fun WarningBadge(warning: WarningUi, busy: Boolean, onInsert: () -> Unit) {
    val pulse = pulseAlpha()
    val description = stringResource(R.string.insert_break_description, warning.recoveryMinutes)
    Surface(onClick = onInsert, enabled = !busy, shape = RoutineShapes.Chip, color = RoutineColors.WarningContainer,
        modifier = Modifier.semantics { contentDescription = description }) {
        Row(Modifier.padding(6.dp), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Psychology, null, Modifier.size(16.dp).alpha(pulse), tint = RoutineColors.Warning)
            Text(stringResource(R.string.insert_break, warning.recoveryMinutes), style = MaterialTheme.typography.labelSmall, color = RoutineColors.Warning)
        }
    }
}

@Composable
fun NowMarker(time: String, modifier: Modifier = Modifier) {
    val pulse = pulseAlpha()
    Row(modifier.height(12.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(8.dp).alpha(pulse).background(RoutineColors.Crimson, androidx.compose.foundation.shape.CircleShape))
        Box(Modifier.weight(1f).height(1.dp).background(RoutineColors.Crimson))
        Text(time, Modifier.background(RoutineColors.Background).padding(horizontal = 4.dp), style = MaterialTheme.typography.labelSmall, color = RoutineColors.Crimson)
    }
}

@Composable
private fun pulseAlpha(): Float {
    val transition = rememberInfiniteTransition(label = "gentle-indicator")
    val alpha by transition.animateFloat(0.68f, 1f, infiniteRepeatable(tween(200), RepeatMode.Reverse), label = "indicator-alpha")
    return alpha
}

@Composable
fun MilestoneCard(item: ResolvedTimelineItem.Milestone, busy: Boolean, onAction: (TimelineAction) -> Unit, modifier: Modifier = Modifier) {
    var expanded by rememberSaveable(item.key) { mutableStateOf(false) }
    val haptics = LocalRoutineHaptics.current
    Row(modifier.fillMaxWidth()) {
        Text(item.dueTime?.clockLabel() ?: stringResource(R.string.all_day), Modifier.width(60.dp).padding(top = 14.dp), style = MaterialTheme.typography.bodySmall, color = RoutineColors.TextSecondary)
        Card(onClick = { haptics.tap(); expanded = !expanded }, modifier = Modifier.weight(1f).animateContentSize(), shape = RoutineShapes.Card,
            border = BorderStroke(1.dp, RoutineColors.CardBorder), elevation = CardDefaults.cardElevation(defaultElevation = 0.dp, pressedElevation = 0.dp),
            colors = CardDefaults.cardColors(containerColor = RoutineColors.Surface1)) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Outlined.Flag, null, tint = RoutineColors.Crimson)
                    Text(item.title, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, textDecoration = if (item.isCompleted) TextDecoration.LineThrough else null)
                    Checkbox(item.isCompleted, { onAction(TimelineAction.ToggleComplete(item)) }, enabled = !busy)
                }
                MetaChip(stringResource(if (item.isExam) R.string.category_exam else R.string.category_milestone), RoutineColors.Exam)
                Text(item.dueTime?.let { stringResource(R.string.milestone_due, it.clockLabel()) } ?: stringResource(R.string.marker_all_day), style = MaterialTheme.typography.bodySmall)
                item.subject?.let { Text(it.name, style = MaterialTheme.typography.bodySmall, color = RoutineColors.TextSecondary) }
                AnimatedVisibility(expanded, enter = fadeIn(tween(TransitionMillis)), exit = fadeOut(tween(TransitionMillis))) {
                    Row {
                        TextButton(enabled = !busy, onClick = { onAction(TimelineAction.Edit(item)) }) { Text(stringResource(R.string.edit)) }
                        TextButton(enabled = !busy, onClick = { onAction(TimelineAction.RequestDelete(item)) }) { Text(stringResource(R.string.delete), color = RoutineColors.Crimson) }
                    }
                }
            }
        }
    }
}
