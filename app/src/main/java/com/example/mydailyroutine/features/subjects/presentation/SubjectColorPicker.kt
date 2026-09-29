package com.example.mydailyroutine.features.subjects.presentation

import android.graphics.Color as AndroidColor
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.mydailyroutine.R
import com.example.mydailyroutine.core.designsystem.components.LiquidSlider
import com.example.mydailyroutine.core.designsystem.components.RoutineLabel
import com.example.mydailyroutine.core.designsystem.haptics.LocalRoutineHaptics
import com.example.mydailyroutine.core.designsystem.theme.RoutineColors
import com.example.mydailyroutine.core.designsystem.theme.RoutineMetrics
import com.example.mydailyroutine.core.designsystem.theme.RoutineSpacing
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

/**
 * The custom colour of a subject, chosen by feel instead of by typing six hex digits.
 *
 * One wheel, one slider:
 *
 * * the **wheel** is hue around the circle and saturation from the centre to the rim — a single
 *   drag reaches any of the millions of hues the shelf's sixteen swatches do not carry;
 * * the **slider** below is brightness, because a wheel at full value cannot darken without
 *   lying about what the reader just touched.
 *
 * The wheel is two gradients, not a bitmap: a sweep of the six primaries is *exactly* the HSV hue
 * circle at full saturation (HSV → RGB is piecewise linear between them), and a white radial
 * gradient over it is exactly the saturation ramp. Two GPU draws that scale to any size, instead
 * of 65 536 pixels computed on the composition thread and then drawn at the wrong size.
 * The preview swatch and the hex readout stay, because "it looks about right" is not the same as
 * "it is #4F8CFF" — the readout is how a reader who needs the exact code finds it.
 */
@Composable
fun SubjectColorPicker(
    /** The colour the wheel and slider are showing (the editor's custom colour). */
    customColor: Long,
    /**
     * Whether the editor's effective colour is this custom one or a shelf swatch. An inactive
     * picker still works — touching it takes over — but it reads as not-the-current choice.
     */
    active: Boolean,
    onColor: (Long) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val haptics = LocalRoutineHaptics.current
    val hsv = remember(customColor) { hsvOf(customColor) }
    val value = hsv[2].coerceIn(MinValue, 1f)

    Column(
        modifier.fillMaxWidth()
            .graphicsLayer { alpha = if (active && enabled) 1f else 0.38f },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(RoutineSpacing.md),
    ) {
        // Preview: what the wheel is actually producing, with the exact code beside it.
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(RoutineSpacing.sm),
        ) {
            Box(
                Modifier.size(RoutineMetrics.SwatchSmall)
                    .border(
                        if (active) 3.dp else 1.dp,
                        if (active) RoutineColors.TextPrimary else RoutineColors.Border,
                        CircleShape,
                    )
                    .padding(3.dp)
                    .background(Color(customColor.toInt()), CircleShape),
            )
            RoutineLabel(
                text = hexOf(customColor),
                style = MaterialTheme.typography.labelSmall,
                color = RoutineColors.TextSecondary,
            )
            Spacer(Modifier.weight(1f))
        }
        ColorWheel(
            color = customColor,
            onColor = onColor,
            value = value,
            onHaptic = { haptics.selection() },
            modifier = Modifier.size(WheelSize),
            enabled = enabled,
        )
        Column(verticalArrangement = Arrangement.spacedBy(RoutineSpacing.xs)) {
            RoutineLabel(
                stringResource(R.string.subject_color_value),
                style = MaterialTheme.typography.labelSmall,
                color = RoutineColors.TextSecondary,
            )
            LiquidSlider(
                value = value,
                // The floor is not a rounding detail: at brightness zero every colour is the same
                // black and the hue the reader just picked is gone from the stored ARGB for good.
                onValueChange = { v -> onColor(Color.hsv(hsv[0], hsv[1], v.coerceIn(MinValue, 1f)).toColorValue()) },
                modifier = Modifier.fillMaxWidth(),
                enabled = enabled,
                valueRange = MinValue..1f,
            )
        }
    }
}

/** The wheel's on-screen size. */
private val WheelSize = 180.dp

/** The thumb the finger sees: the colour itself under one bright hairline, never glass. */
private val ThumbSize = 22.dp

/**
 * The darkest the slider goes. Below this a colour is indistinguishable from the OLED background
 * anyway, and the round trip through ARGB would throw the hue away.
 */
private const val MinValue = 0.06f

@Composable
private fun ColorWheel(
    color: Long,
    onColor: (Long) -> Unit,
    value: Float,
    onHaptic: () -> Unit,
    modifier: Modifier,
    enabled: Boolean,
) {
    val density = LocalDensity.current
    var widthPx by remember { mutableFloatStateOf(0f) }
    val hsv = remember(color) { hsvOf(color) }
    val thumbPx = with(density) { ThumbSize.toPx() }
    val wheelDescription = stringResource(R.string.subject_color_wheel)

    // The gesture detectors below are keyed on `enabled` alone. Keying them on the brightness too
    // would restart them — and cancel the drag in progress — the moment a picked colour rounds to
    // a different value, so what the detectors need is the *latest* brightness, not a new detector.
    val latestValue by rememberUpdatedState(value)
    val latestOnColor by rememberUpdatedState(onColor)
    val pick: (Float, Float) -> Unit = { x, y ->
        val centre = widthPx / 2f
        if (centre > 0f) {
            val dx = x - centre
            val dy = y - centre
            val hue = (Math.toDegrees(atan2(dy, dx).toDouble()).toFloat() + 360f) % 360f
            val saturation = (hypot(dx, dy) / centre).coerceIn(0f, 1f)
            latestOnColor(Color.hsv(hue, saturation, latestValue).toColorValue())
        }
    }

    Box(
        modifier
            .clip(CircleShape)
            .onSizeChanged { widthPx = it.width.toFloat() }
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                detectTapGestures { position ->
                    onHaptic()
                    pick(position.x, position.y)
                }
            }
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                detectDragGestures(
                    onDragStart = { position ->
                        onHaptic()
                        pick(position.x, position.y)
                    },
                    onDrag = { change, _ ->
                        pick(change.position.x, change.position.y)
                        change.consume()
                    },
                )
            }
            .semantics { contentDescription = wheelDescription },
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val radius = size.minDimension / 2f
            drawCircle(Brush.sweepGradient(RoutineColors.HueWheel, center), radius = radius, center = center)
            drawCircle(
                // White fading to *transparent white*: `Color.Transparent` is transparent black,
                // and interpolating towards it would grey the middle of the ramp.
                Brush.radialGradient(
                    listOf(RoutineColors.HueWheelCentre, RoutineColors.HueWheelCentre.copy(alpha = 0f)),
                    center = center,
                    radius = radius,
                ),
                radius = radius,
                center = center,
            )
            // The wheel darkens with the slider, so the circle always shows the colours the finger
            // would actually land on rather than a brighter promise of them.
            if (value < 1f) {
                drawCircle(RoutineColors.HueWheelShade.copy(alpha = 1f - value), radius = radius, center = center)
            }
        }
        // The thumb rides a draw-phase translation over the static wheel: moving it never
        // re-lays out the circle, so the finger and the thumb move in the same frame.
        val centre = widthPx / 2f
        val angle = Math.toRadians(hsv[0].toDouble()).toFloat()
        val distance = hsv[1] * centre
        Box(
            Modifier.size(ThumbSize)
                .graphicsLayer {
                    translationX = centre + distance * cos(angle) - thumbPx / 2f
                    translationY = centre + distance * sin(angle) - thumbPx / 2f
                }
                .clip(CircleShape)
                .background(Color(color.toInt()))
                .border(2.dp, RoutineColors.TextPrimary, CircleShape),
        )
    }
}

/**
 * The HSV triple of a stored ARGB colour. Compose can build a [Color] *from* HSV but cannot read
 * HSV back out of one, so the platform's own conversion does that half.
 */
private fun hsvOf(argb: Long): FloatArray =
    FloatArray(3).also { AndroidColor.colorToHSV(argb.toInt(), it) }

/**
 * A colour in the form the database and the shelf use: a positive `0xAARRGGBB` long. The mask
 * matters — `toArgb().toLong()` alone is negative for any opaque colour, and the editor compares
 * this value against [com.example.mydailyroutine.core.designsystem.theme.RoutineColors.subjectSwatches].
 */
private fun Color.toColorValue(): Long = toArgb().toLong() and 0xFFFFFFFFL

/** "#RRGGBB" of an ARGB long, for the readout beside the wheel. */
private fun hexOf(color: Long): String = "%06X".format(color and 0xFFFFFFL)
