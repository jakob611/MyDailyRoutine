package com.example.mydailyroutine.core.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.example.mydailyroutine.core.designsystem.theme.CategoryStyle
import com.example.mydailyroutine.core.designsystem.theme.RoutineColors
import com.example.mydailyroutine.core.designsystem.theme.RoutineMetrics
import com.example.mydailyroutine.core.designsystem.theme.RoutineShapes
import com.example.mydailyroutine.core.designsystem.theme.RoutineSpacing
import kotlin.math.roundToInt

/**
 * Shared chrome for everything laid out against a clock: the hour gutter, the vertical rail, the
 * category bar, categorical chips and the NOW band.
 *
 * All geometry derives from [RoutineMetrics], so the day list, managed-routine cards, milestone rows
 * and the gaps between blocks finally share one measurement instead of three competing ones (the
 * previous code mixed 48 dp, 53 dp and 60 dp columns, which is why hours and cards never lined up).
 * Text is never positioned absolutely here: the rail, the accent bar and the band line are pure
 * `drawBehind` painting, and the only absolutely placed child (the NOW band) is measured and placed
 * by [NowBand] with an opaque background of its own.
 */

/** Vertical rail between the hour gutter and the cards; centred in the gutter gap. */
fun Modifier.timelineRail(color: Color): Modifier = drawBehind {
    val x = RoutineMetrics.RailX.toPx()
    val railX = if (layoutDirection == LayoutDirection.Rtl) size.width - x else x
    drawLine(color, Offset(railX, 0f), Offset(railX, size.height), RoutineMetrics.SpineWidth.toPx())
}

/** Category bar on the leading edge of a card. */
fun Modifier.categoryBar(color: Color): Modifier = drawBehind {
    val barWidth = RoutineMetrics.CategoryBarWidth.toPx()
    val x = if (layoutDirection == LayoutDirection.Rtl) size.width - barWidth else 0f
    drawRect(color, Offset(x, 0f), Size(barWidth, size.height))
}

/**
 * The hour column: start time, optional end or actual time. Fixed text width so cards next to it
 * start on the same pixel in every row of the day.
 */
@Composable
fun TimeGutter(
    start: String,
    modifier: Modifier = Modifier,
    end: String? = null,
    emphasized: Boolean = false,
    muted: Boolean = false,
    topPadding: Dp = RoutineSpacing.md,
    style: TextStyle = MaterialTheme.typography.bodySmall,
) {
    Column(
        modifier.width(RoutineMetrics.GutterTextWidth)
            .padding(top = topPadding, end = RoutineMetrics.GutterGap),
        verticalArrangement = Arrangement.spacedBy(RoutineSpacing.xs),
    ) {
        RoutineLabel(
            text = start,
            style = style,
            color = when {
                emphasized -> RoutineColors.TextPrimary
                muted -> RoutineColors.TextSecondary
                else -> RoutineColors.TextSecondary
            },
        )
        if (end != null) RoutineLabel(end, style = style, color = RoutineColors.TextSecondary)
    }
}

/** Small categorical badge: one unbreakable line, capped width so it never owns the whole card. */
@Composable
fun MetaChip(
    text: String,
    modifier: Modifier = Modifier,
    style: CategoryStyle = RoutineColors.Personal,
) {
    Surface(shape = RoutineShapes.Chip, color = style.container, contentColor = style.content, modifier = modifier) {
        RoutineLabel(
            text = text,
            modifier = Modifier.widthIn(max = RoutineMetrics.ChipMaxWidth)
                .padding(horizontal = RoutineSpacing.sm, vertical = RoutineSpacing.xs),
            style = MaterialTheme.typography.labelSmall,
        )
    }
}

/**
 * The NOW indicator, drawn over a card without ever mixing with its text.
 *
 * It is a measured [Layout] child: the band is placed at the progress fraction of the parent height
 * and contributes no size of its own, so card layout is unaffected and no `Modifier.offset` is
 * involved. The band is opaque and spans the full width, so it reads as a marker on top of the card
 * rather than as text colliding with text.
 */
@Composable
fun NowBand(
    time: String,
    progress: Float,
    modifier: Modifier = Modifier,
    pulse: Float = 1f,
) {
    Layout(
        modifier = modifier,
        content = {
            Surface(color = RoutineColors.Background) {
                Row(
                    Modifier.height(RoutineMetrics.NowBandHeight),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        Modifier.size(RoutineMetrics.NowDotSize).alpha(pulse)
                            .background(RoutineColors.Timer, CircleShape),
                    )
                    Box(
                        Modifier.weight(1f).height(RoutineMetrics.SpineWidth).background(RoutineColors.Timer),
                    )
                    RoutineLabel(
                        text = time,
                        modifier = Modifier.alpha(pulse).padding(horizontal = RoutineSpacing.xs),
                        style = MaterialTheme.typography.labelSmall,
                        color = RoutineColors.Timer,
                    )
                }
            }
        },
    ) { measurables, constraints ->
        val width = if (constraints.hasBoundedWidth) constraints.maxWidth else 0
        val band = measurables.first().measure(
            if (constraints.hasBoundedWidth) Constraints.fixedWidth(constraints.maxWidth) else constraints,
        )
        val height = if (constraints.hasBoundedHeight) constraints.maxHeight else band.height
        val range = (height - band.height).coerceAtLeast(0)
        val y = (range * progress.coerceIn(0f, 1f)).roundToInt().coerceIn(0, range)
        layout(width, height) { band.placeRelative(0, y) }
    }
}

