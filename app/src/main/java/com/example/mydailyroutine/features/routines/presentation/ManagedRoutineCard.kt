package com.example.mydailyroutine.features.routines.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material.icons.outlined.Spa
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.onClickLabel
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.mydailyroutine.R
import com.example.mydailyroutine.core.designsystem.components.ActionRow
import com.example.mydailyroutine.core.designsystem.components.NowBand
import com.example.mydailyroutine.core.designsystem.components.RoutineLabel
import com.example.mydailyroutine.core.designsystem.components.RoutineText
import com.example.mydailyroutine.core.designsystem.components.RoutineTextDefaults
import com.example.mydailyroutine.core.designsystem.components.TimeGutter
import com.example.mydailyroutine.core.designsystem.components.timelineRail
import com.example.mydailyroutine.core.designsystem.haptics.LocalRoutineHaptics
import com.example.mydailyroutine.core.designsystem.theme.RoutineColors
import com.example.mydailyroutine.core.designsystem.theme.RoutineShapes
import com.example.mydailyroutine.core.designsystem.theme.RoutineSpacing
import com.example.mydailyroutine.core.presentation.TimelineAction
import com.example.mydailyroutine.core.presentation.clockLabel
import com.example.mydailyroutine.core.presentation.minuteLabel
import com.example.mydailyroutine.domain.model.ResolvedTimelineItem
import com.example.mydailyroutine.domain.routines.RoutineOrigin
import com.example.mydailyroutine.features.timeline.components.pulseAlpha
import java.time.ZonedDateTime

/**
 * Compact context cards keep five-minute breaks and overnight sleep from overwhelming the day.
 *
 * They share the day-view geometry: the same [TimeGutter], the same rail, the same measured
 * [NowBand]. Sleep and break titles are bounded to two lines with an ellipsis, and the expanded
 * actions reflow instead of squeezing each other.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ManagedRoutineCard(
    block: ResolvedTimelineItem.Block,
    now: ZonedDateTime,
    showNow: Boolean,
    busy: Boolean,
    modifier: Modifier = Modifier,
    onAction: (TimelineAction) -> Unit,
) {
    var expanded by remember(block.key) { mutableStateOf(false) }
    val haptics = LocalRoutineHaptics.current
    val sleep = block.origin == RoutineOrigin.SLEEP
    val morning = block.origin == RoutineOrigin.MORNING_BUFFER
    val tint = if (block.isSuppressed) RoutineColors.TextMuted else RoutineColors.Sage
    val expandLabel = stringResource(if (expanded) R.string.collapse_block else R.string.expand_block)
    Box(modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().timelineRail(RoutineColors.Spine), verticalAlignment = Alignment.Top) {
            TimeGutter(start = minuteLabel(block.startMinute), end = minuteLabel(block.endMinute), topPadding = RoutineSpacing.md)
            OutlinedCard(
                onClick = { expanded = !expanded; haptics.tap() },
                modifier = Modifier.weight(1f).semantics { onClickLabel = expandLabel },
                shape = RoutineShapes.Card,
                border = BorderStroke(1.dp, RoutineColors.CardBorder),
                colors = CardDefaults.outlinedCardColors(containerColor = RoutineColors.Surface1),
            ) {
                Box(Modifier.fillMaxWidth()) {
                    Column(
                        Modifier.fillMaxWidth().padding(RoutineSpacing.md),
                        verticalArrangement = Arrangement.spacedBy(RoutineSpacing.xs),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(RoutineSpacing.sm),
                        ) {
                            Icon(
                                if (sleep) Icons.Outlined.Bedtime else if (morning) Icons.Outlined.WbSunny else Icons.Outlined.Spa,
                                null,
                                Modifier.size(20.dp),
                                tint = tint,
                            )
                            RoutineText(
                                text = block.title,
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.titleSmall,
                                color = tint,
                                maxLines = RoutineTextDefaults.Body,
                            )
                            if (!sleep) {
                                RoutineLabel(
                                    text = stringResource(R.string.duration_minutes, block.durationMinutes),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = tint,
                                )
                            }
                        }
                        RoutineText(
                            text = when {
                                block.companionConflict -> stringResource(R.string.break_no_room)
                                block.isSuppressed -> stringResource(R.string.school_inactive, block.holidayTitle.orEmpty())
                                sleep -> stringResource(R.string.managed_sleep_notice)
                                morning -> stringResource(R.string.managed_morning_notice)
                                else -> stringResource(R.string.break_follows_lesson)
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = RoutineColors.TextSecondary,
                            maxLines = RoutineTextDefaults.Paragraph,
                        )
                        if (expanded) {
                            ActionRow {
                                TextButton(enabled = !busy, onClick = { onAction(TimelineAction.Skip(block)) }) {
                                    RoutineLabel(stringResource(R.string.skip_date), style = MaterialTheme.typography.labelLarge)
                                }
                                if (sleep || morning) {
                                    TextButton(onClick = { onAction(TimelineAction.OpenSettings) }) {
                                        RoutineLabel(stringResource(R.string.sleep_manage), style = MaterialTheme.typography.labelLarge)
                                    }
                                } else {
                                    TextButton(enabled = !busy, onClick = { onAction(TimelineAction.RequestDelete(block)) }) {
                                        RoutineLabel(stringResource(R.string.delete), style = MaterialTheme.typography.labelLarge,
                                            color = RoutineColors.Crimson)
                                    }
                                }
                            }
                        }
                    }
                    if (showNow) {
                        val nowMinute = now.hour * 60 + now.minute
                        val span = (block.endMinute - block.startMinute).coerceAtLeast(1)
                        val progress = ((nowMinute - block.startMinute).toFloat() / span).coerceIn(0f, 1f)
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
