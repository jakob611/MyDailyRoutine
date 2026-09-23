package com.example.mydailyroutine.core.designsystem.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.mydailyroutine.R
import com.example.mydailyroutine.core.designsystem.theme.RoutineColors
import com.example.mydailyroutine.core.presentation.MonthMark

/**
 * The four meanings of the calendar as shapes: a test is a triangle, a deadline a diamond, a day
 * without lessons a ring, planned focus a dot.
 *
 * Drawn in one place because the grid and the legend must not be able to tell two different stories:
 * before this, both the grid cell and the legend printed the same red dot for a test and for an IB
 * deadline, so the two were indistinguishable even to a reader with ordinary colour vision. Each icon
 * carries its own name for the screen reader, which is what "never colour alone" means in practice.
 */
@Composable
fun MonthMarkIcon(
    mark: MonthMark,
    modifier: Modifier = Modifier,
    size: Dp = 6.dp,
    color: Color = MonthMarkColors.of(mark),
) {
    val shape = stringResource(
        when (mark) {
            MonthMark.Ring -> R.string.mark_shape_ring
            MonthMark.Triangle -> R.string.mark_shape_triangle
            MonthMark.Diamond -> R.string.mark_shape_diamond
            MonthMark.Dot -> R.string.mark_shape_dot
        },
    )
    Canvas(modifier.size(size).semantics { contentDescription = shape }) {
        // A fixed hairline, not a fraction of the box: the ring must look like a ring at 6 dp and at
        // any font scale, and `size` inside this lambda is the canvas, not the parameter.
        val stroke = 1.5.dp.toPx()
        val side = this.size.minDimension
        val radius = side / 2f
        when (mark) {
            MonthMark.Dot -> drawCircle(color, radius = radius)
            // The ring is stroked inside its own box so a ring and a dot still measure the same.
            MonthMark.Ring -> drawCircle(color, radius = radius - stroke / 2f, style = Stroke(stroke))
            MonthMark.Triangle -> drawPath(trianglePath(side), color)
            MonthMark.Diamond -> drawPath(diamondPath(side), color)
        }
    }
}

private fun trianglePath(side: Float): Path = Path().apply {
    moveTo(side / 2f, 0f)
    lineTo(side, side)
    lineTo(0f, side)
    close()
}

private fun diamondPath(side: Float): Path = Path().apply {
    moveTo(side / 2f, 0f)
    lineTo(side, side / 2f)
    lineTo(side / 2f, side)
    lineTo(0f, side / 2f)
    close()
}

/** The palette the marks are allowed to use. No new colours are introduced for the calendar. */
object MonthMarkColors {
    fun of(mark: MonthMark): Color = when (mark) {
        MonthMark.Ring -> RoutineColors.Recovery.accent
        MonthMark.Triangle, MonthMark.Diamond -> RoutineColors.Error
        MonthMark.Dot -> RoutineColors.Primary
    }
}
