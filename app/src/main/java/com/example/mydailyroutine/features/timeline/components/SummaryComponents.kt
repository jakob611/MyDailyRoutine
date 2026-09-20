package com.example.mydailyroutine.features.timeline.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.mydailyroutine.R
import com.example.mydailyroutine.core.designsystem.components.ActionRow
import com.example.mydailyroutine.core.designsystem.components.RoutineLabel
import com.example.mydailyroutine.core.designsystem.components.RoutineText
import com.example.mydailyroutine.core.designsystem.components.RoutineTextDefaults
import com.example.mydailyroutine.core.designsystem.theme.RoutineColors
import com.example.mydailyroutine.core.designsystem.theme.RoutineShapes
import com.example.mydailyroutine.core.designsystem.theme.RoutineSpacing
import com.example.mydailyroutine.core.designsystem.theme.categoryStyle
import com.example.mydailyroutine.core.presentation.*
import com.example.mydailyroutine.domain.health.HealthConfig
import com.example.mydailyroutine.domain.health.categoryAllocation
import com.example.mydailyroutine.domain.model.CalendarEntry
import com.example.mydailyroutine.domain.model.ResolvedTimelineItem

/** Adapted from agent3's component library; uses the canonical core model and shared OLED palette. */
@Composable
fun DateNavigator(
    title: String,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onToday: () -> Unit,
    onPick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val pickLabel = stringResource(R.string.choose_date)
    Row(
        modifier.fillMaxWidth().padding(horizontal = RoutineSpacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(RoutineSpacing.xs),
    ) {
        IconButton(onClick = onPrevious) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, stringResource(R.string.previous_period))
        }
        // The title is the date picker: one control instead of a fourth icon competing for width.
        Row(
            Modifier.weight(1f).clip(RoutineShapes.Chip).clickable(onClickLabel = pickLabel, onClick = onPick)
                .padding(vertical = RoutineSpacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(RoutineSpacing.xs),
        ) {
            RoutineText(
                text = title,
                modifier = Modifier.weight(1f, fill = false),
                style = MaterialTheme.typography.titleMedium,
                maxLines = RoutineTextDefaults.Body,
            )
            Icon(Icons.Outlined.CalendarMonth, null, Modifier.size(16.dp), tint = RoutineColors.TextSecondary)
        }
        TextButton(onClick = onToday) { RoutineLabel(stringResource(R.string.today), style = MaterialTheme.typography.labelLarge) }
        IconButton(onClick = onNext) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, stringResource(R.string.next_period))
        }
    }
}

@Composable
fun DayLoadBar(items: List<ResolvedTimelineItem>, modifier: Modifier = Modifier) {
    val allocation = remember(items) { categoryAllocation(items).filter { it.minutes > 0 } }
    val description = stringResource(R.string.day_load_description)
    Row(
        modifier.fillMaxWidth().height(RoutineSpacing.sm).clip(RoutineShapes.Pill)
            .background(RoutineColors.SurfaceLowest).semantics { contentDescription = description },
    ) {
        allocation.forEach { slice ->
            Box(Modifier.weight(slice.minutes.toFloat()).fillMaxHeight().background(categoryStyle(slice.category).accent))
        }
    }
}

@Composable
fun HealthWarningCard(warning: WarningUi, config: HealthConfig, busy: Boolean, onInsert: () -> Unit) {
    Surface(
        shape = RoutineShapes.Card,
        color = RoutineColors.WarningContainer,
        border = BorderStroke(1.dp, RoutineColors.Warning.copy(alpha = 0.18f)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(RoutineSpacing.md),
            horizontalArrangement = Arrangement.spacedBy(RoutineSpacing.md),
        ) {
            Icon(Icons.Outlined.Psychology, null, tint = RoutineColors.Warning)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(RoutineSpacing.xs)) {
                RoutineText(warning.type.label(), style = MaterialTheme.typography.titleSmall,
                    color = RoutineColors.Warning, maxLines = RoutineTextDefaults.Body)
                RoutineText(warningBody(warning.type, config), style = MaterialTheme.typography.bodySmall,
                    maxLines = RoutineTextDefaults.Paragraph)
                ActionRow {
                    TextButton(onClick = onInsert, enabled = !busy) {
                        RoutineLabel(stringResource(R.string.insert_break, warning.recoveryMinutes),
                            style = MaterialTheme.typography.labelLarge, color = RoutineColors.Warning)
                    }
                }
            }
        }
    }
}

@Composable
fun CalendarNoticeCard(entries: List<CalendarEntry>) {
    if (entries.isEmpty()) return
    val off = entries.any { it.isWorkFreeDay }
    val style = if (off) RoutineColors.Recovery else RoutineColors.School
    Surface(
        color = style.container,
        contentColor = style.content,
        shape = RoutineShapes.Card,
        border = BorderStroke(1.dp, RoutineColors.Border),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(RoutineSpacing.md),
            horizontalArrangement = Arrangement.spacedBy(RoutineSpacing.md),
        ) {
            Icon(Icons.Outlined.CalendarMonth, null, tint = style.content)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(RoutineSpacing.xs)) {
                RoutineText(stringResource(if (off) R.string.calendar_off_title else R.string.calendar_notice_title),
                    style = MaterialTheme.typography.titleSmall, color = style.content,
                    maxLines = RoutineTextDefaults.Body)
                entries.forEach {
                    RoutineText(it.title, style = MaterialTheme.typography.bodySmall,
                        color = style.content, maxLines = RoutineTextDefaults.Body)
                }
                if (off) {
                    RoutineText(stringResource(R.string.calendar_school_inactive),
                        style = MaterialTheme.typography.bodySmall, color = RoutineColors.TextSecondary,
                        maxLines = RoutineTextDefaults.Paragraph)
                }
            }
        }
    }
}

/**
 * One metric: value on top, label below. Values may wrap onto a second line — with
 * `Modifier.height(IntrinsicSize.Min)` on the row every tile keeps the same height, so a long
 * duration can neither truncate nor make the row ragged.
 */
@Composable
fun MetricTile(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        modifier.fillMaxHeight(),
        shape = RoutineShapes.Card,
        color = RoutineColors.SurfaceContainer,
        border = BorderStroke(1.dp, RoutineColors.CardBorder),
    ) {
        Column(
            Modifier.fillMaxWidth().padding(RoutineSpacing.md),
            verticalArrangement = Arrangement.spacedBy(RoutineSpacing.xs),
        ) {
            RoutineText(value, style = MaterialTheme.typography.titleMedium, color = RoutineColors.TextPrimary,
                maxLines = RoutineTextDefaults.Body)
            RoutineLabel(label, style = MaterialTheme.typography.labelMedium, color = RoutineColors.TextSecondary)
        }
    }
}

@Composable
fun Legend(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(RoutineSpacing.sm)) {
        Box(Modifier.size(6.dp).background(color, CircleShape))
        RoutineLabel(label, style = MaterialTheme.typography.labelSmall, color = RoutineColors.TextSecondary)
    }
}
