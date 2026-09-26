package com.example.mydailyroutine.core.designsystem.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.example.mydailyroutine.core.designsystem.glass.GlassRole
import com.example.mydailyroutine.core.designsystem.glass.LocalGlassTilt
import com.example.mydailyroutine.core.designsystem.glass.LocalInsideGlass
import com.example.mydailyroutine.core.designsystem.glass.LocalRoutineBackdrop
import com.example.mydailyroutine.core.designsystem.glass.rememberGlassTouch
import com.example.mydailyroutine.core.designsystem.glass.routineGlass
import com.example.mydailyroutine.core.designsystem.glass.routineGlassTouch
import com.example.mydailyroutine.core.designsystem.motion.LocalReduceMotion
import com.example.mydailyroutine.core.designsystem.motion.glassMorphSpec
import com.example.mydailyroutine.core.designsystem.motion.glassTouchSpec
import com.example.mydailyroutine.core.designsystem.theme.PopSpring
import com.example.mydailyroutine.core.designsystem.theme.RoutineColors
import com.example.mydailyroutine.core.designsystem.theme.RoutineMetrics
import com.example.mydailyroutine.core.designsystem.theme.RoutineShapes
import com.example.mydailyroutine.core.designsystem.theme.RoutineSpacing
import kotlinx.coroutines.launch

/**
 * The liquid controls of the design brief of 2026-09-19, built the way Kyant0's Backdrop catalog
 * builds its own components (the library is deliberately low level — the toggle, the slider and the
 * button are patterns, not widgets): a position that follows the finger, a damped spring that snaps
 * past a threshold, and a squash on press so the control reads as a soft body of glass rather than
 * as two states cut from cardboard.
 *
 * Two rules carried over from the glass layer:
 *
 * * The knob and the thumb stay **solid**. A 24 dp piece of glass over a moving list refracts into
 *   noise, and inside a bottom sheet there is no backdrop to sample at all; Apple paints its switch
 *   knob solid white for the same reason.
 * * Everything honours [LocalReduceMotion]: springs become snaps, the squash becomes nothing.
 */

private val SwitchWidth = 52.dp
private val SwitchHeight = 32.dp
private val SwitchKnob = 24.dp
private val SwitchPad = 4.dp

/**
 * Replacement for the Material switch: a capsule track that fills with the brand turquoise, a white
 * knob that can be dragged or tapped, and a horizontal squash on press (Kyant0's LiquidToggle uses
 * the same velocity squash). Semantics stay `Role.Switch` + toggleable, so TalkBack and the device
 * tests keep working unchanged.
 */
@Composable
fun RoutineSwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val reduceMotion = LocalReduceMotion.current
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val position = remember { Animatable(if (checked) 1f else 0f) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(checked, reduceMotion) {
        if (reduceMotion) position.snapTo(if (checked) 1f else 0f)
        else position.animateTo(if (checked) 1f else 0f, PopSpring)
    }
    val squash by animateFloatAsState(
        if (pressed) 1f else 0f,
        glassTouchSpec<Float>(reduceMotion),
        label = "switch-squash",
    )
    val density = LocalDensity.current
    val travelPx = with(density) { (SwitchWidth - SwitchKnob - SwitchPad * 2).toPx() }

    Box(
        modifier
            // 52 x 32 dp of glass, 52 x 48 dp of finger: the switch is the one control in a
            // settings list where a miss does not just fail, it flips a different setting.
            .defaultMinSize(minWidth = RoutineMetrics.TouchTarget, minHeight = RoutineMetrics.TouchTarget)
            .graphicsLayer { alpha = if (enabled) 1f else 0.38f }
            .toggleable(
                value = checked,
                enabled = enabled,
                role = Role.Switch,
                interactionSource = interaction,
                indication = null,
                onValueChange = { onCheckedChange?.invoke(it) },
            ),
        contentAlignment = Alignment.Center,
    ) {
        // The visible pane keeps the 52 x 32 dp of a switch; the drag lives on it, so a horizontal
        // drag anywhere on the pane still rides the knob.
        Box(
            Modifier
                .size(SwitchWidth, SwitchHeight)
                .pointerInput(enabled, reduceMotion, travelPx) {
                    if (!enabled) return@pointerInput
                    detectHorizontalDragGestures(
                        onHorizontalDrag = { change, dragAmount ->
                            val target = (position.value + dragAmount / travelPx).coerceIn(0f, 1f)
                            scope.launch { position.snapTo(target) }
                            change.consume()
                        },
                        onDragEnd = {
                            val target = position.value >= 0.5f
                            if (target != checked) onCheckedChange?.invoke(target)
                            scope.launch {
                                if (reduceMotion) position.snapTo(if (target) 1f else 0f)
                                else position.animateTo(if (target) 1f else 0f, PopSpring)
                            }
                        },
                        onDragCancel = {
                            scope.launch {
                                if (reduceMotion) position.snapTo(if (checked) 1f else 0f)
                                else position.animateTo(if (checked) 1f else 0f, PopSpring)
                            }
                        },
                    )
                },
        contentAlignment = Alignment.CenterStart,
    ) {
        val track = RoundedCornerShape(percent = 50)
        Box(
            Modifier.fillMaxSize()
                .clip(track)
                .background(lerp(RoutineColors.Surface4, RoutineColors.SwitchOn, position.value))
                // One hairline of light along the top of the track: the same lit-edge idea as the
                // glass rim, at capsule scale.
                .drawWithContent {
                    drawContent()
                    drawRect(
                        Brush.verticalGradient(
                            0f to RoutineColors.GlassRim.copy(alpha = 0.10f * (1f - position.value * 0.6f)),
                            0.5f to Color.Transparent,
                        )
                    )
                }
                .border(
                    1.dp,
                    RoutineColors.GlassRim.copy(alpha = 0.10f * (1f - position.value)),
                    track,
                ),
        )
        Box(
            Modifier
                .size(SwitchKnob)
                .graphicsLayer {
                    // Draw-phase translation: the knob rides the spring without re-laying out the
                    // track, and Kyant0's squash stretches it along the direction of travel,
                    // anchored at the leading edge, never more than a fifth of itself.
                    translationX = with(density) { SwitchPad.toPx() } + position.value * travelPx
                    val stretch = squash * 0.18f
                    scaleX = 1f + stretch
                    transformOrigin = TransformOrigin(if (position.value < 0.5f) 1f else 0f, 0.5f)
                }
                .clip(CircleShape)
                .background(RoutineColors.TextPrimary)
                .border(1.dp, RoutineColors.CardBorder.copy(alpha = 0.25f), CircleShape),
        )
    }
    }
}

private val SliderThumb = 20.dp
private val SliderTrack = 6.dp

/**
 * The catalog's LiquidSlider reduced to what the entry editor needs: a capsule track, a turquoise
 * fill and a solid thumb that grows while the finger is on it. The caller owns the value; every
 * drag delta is published through [onValueChange] immediately, which is how the whole app saves now.
 */
@Composable
fun LiquidSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
) {
    val reduceMotion = LocalReduceMotion.current
    var dragging by remember { mutableStateOf(false) }
    val span = (valueRange.endInclusive - valueRange.start).coerceAtLeast(0.0001f)
    val fraction = ((value - valueRange.start) / span).coerceIn(0f, 1f)
    val shown by animateFloatAsState(fraction, glassMorphSpec<Float>(reduceMotion), label = "slider-fill")
    val thumbScale by animateFloatAsState(
        if (dragging) 1.25f else 1f,
        glassTouchSpec<Float>(reduceMotion),
        label = "slider-thumb",
    )
    var widthPx by remember { mutableStateOf(0f) }
    val density = LocalDensity.current
    val thumbPx = with(density) { SliderThumb.toPx() }

    fun valueAt(x: Float): Float {
        val usable = (widthPx - thumbPx).coerceAtLeast(1f)
        val f = ((x - thumbPx / 2f) / usable).coerceIn(0f, 1f)
        return valueRange.start + f * span
    }

    Box(
        modifier
            .fillMaxWidth()
            // The track is 6 dp and the thumb 20; the row the finger meets is a full touch target,
            // because a slider that is hard to grab is a setting the reader gives up on.
            .height(RoutineMetrics.TouchTarget)
            .graphicsLayer { alpha = if (enabled) 1f else 0.38f }
            .onSizeChanged { widthPx = it.width.toFloat() }
            .pointerInput(enabled, valueRange, widthPx) {
                if (!enabled || widthPx <= 0f) return@pointerInput
                detectTapGestures(onTap = { onValueChange(valueAt(it.x)) })
            }
            .pointerInput(enabled, valueRange, widthPx) {
                if (!enabled || widthPx <= 0f) return@pointerInput
                detectHorizontalDragGestures(
                    onDragStart = { offset ->
                        dragging = true
                        onValueChange(valueAt(offset.x))
                    },
                    onHorizontalDrag = { change, _ ->
                        onValueChange(valueAt(change.position.x))
                        change.consume()
                    },
                    onDragEnd = { dragging = false },
                    onDragCancel = { dragging = false },
                )
            },
        contentAlignment = Alignment.CenterStart,
    ) {
        val capsule = RoundedCornerShape(percent = 50)
        Box(
            Modifier.fillMaxWidth().height(SliderTrack)
                .clip(capsule)
                .background(RoutineColors.Surface4)
                .border(1.dp, RoutineColors.CardBorder.copy(alpha = 0.2f), capsule),
        )
        Box(
            Modifier.fillMaxWidth(shown).height(SliderTrack)
                .clip(capsule)
                .background(RoutineColors.Primary),
        )
        Box(
            Modifier
                .size(SliderThumb)
                .graphicsLayer {
                    translationX = thumbPx / 2f + shown * (widthPx - thumbPx).coerceAtLeast(0f)
                    scaleX = thumbScale
                    scaleY = thumbScale
                }
                .clip(CircleShape)
                .background(RoutineColors.TextPrimary)
                .border(1.dp, RoutineColors.CardBorder.copy(alpha = 0.25f), CircleShape),
        )
    }
}

/**
 * The liquid glass buttons, in the literal sense: standing on its own, the control is **itself** a
 * pane of glass — the same blur, lens, rim and touch behaviour as the floating chrome — instead of
 * a transparent clickable painted on top of a glass frame.
 *
 * Three rules from the glass layer carry over:
 *
 * * A standalone pane samples the *window's* backdrop through [LocalRoutineBackdrop], never a
 *   backdrop of its own. The pane is a sibling of the content layer it refracts, so what it shows
 *   is what is actually scrolling under the chrome it lives in, not the frame it sits on.
 * * **Inside a glass surface there is no pane at all** ([LocalInsideGlass]). Glass does not nest:
 *   a second pane would sample the same backdrop as the bar around it, show the content without
 *   the bar's tint, and read as a hole punched through the chrome. The icon sits on the bar and
 *   the bar stays one piece of glass.
 * * [rememberGlassTouch] gives the control the interactive-glass behaviour of Apple's
 *   `.interactive()`: a few percent of scale under the finger, a light bloom at the touch point, a
 *   sprung release. That behaviour is `routineGlassTouch`, not the pane, so it is identical in both
 *   cases. The Material ripple is switched off — the scale *is* the state layer, and a second
 *   answer on top of it would contradict the first.
 */
@Composable
fun GlassIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: CornerBasedShape = RoutineShapes.GlassChip,
    role: GlassRole = GlassRole.Chip,
    icon: @Composable () -> Unit,
) {
    val backdrop = LocalRoutineBackdrop.current
    // Inside a glass bar the icon is just an icon. Its own pane would sample the same backdrop the
    // bar samples and so would show the content without the bar's tint — a hole through the chrome.
    val insideGlass = LocalInsideGlass.current
    val touch = rememberGlassTouch(LocalReduceMotion.current)
    // The gesture lives on the larger box, the glass keeps its own size: Compose derives the pointer
    // area from the node that owns the gesture, so a 40 dp pane can still be a 48 dp button. Both
    // boxes share one interaction source, which is what keeps the press state (squash, glow) on the
    // pane the finger cannot quite see.
    Box(
        modifier
            .size(RoutineMetrics.TouchTarget)
            .clickable(interactionSource = touch.source, indication = null, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier.size(RoutineMetrics.GlassControlSize)
                // Kept in both cases: the squash and the bloom at the touch point are this
                // modifier's, not the pane's, so the plain button answers the finger exactly as
                // the glass one does.
                .routineGlassTouch(touch, shape)
                .then(
                    if (insideGlass) Modifier
                    else Modifier.routineGlass(backdrop, shape, role, specular = true, tilt = LocalGlassTilt.current),
                )
                .clip(shape),
            contentAlignment = Alignment.Center,
        ) { icon() }
    }
}

/** A text control as its own glass pane: the "Danes" style of button, one raised piece of glass. */
@Composable
fun GlassChipButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: CornerBasedShape = RoutineShapes.GlassChip,
    role: GlassRole = GlassRole.Chip,
    contentColor: Color = RoutineColors.TextPrimary,
) {
    val backdrop = LocalRoutineBackdrop.current
    val touch = rememberGlassTouch(LocalReduceMotion.current)
    Box(
        modifier
            .routineGlassTouch(touch, shape)
            .routineGlass(backdrop, shape, role, specular = true, tilt = LocalGlassTilt.current)
            .clip(shape)
            .defaultMinSize(minHeight = RoutineMetrics.TouchTarget)
            .clickable(interactionSource = touch.source, indication = null, role = Role.Button, onClick = onClick)
            .padding(horizontal = RoutineSpacing.lg, vertical = RoutineSpacing.sm),
        contentAlignment = Alignment.Center,
    ) {
        RoutineLabel(label, style = MaterialTheme.typography.labelLarge, color = contentColor)
    }
}

/**
 * A glass pane of arbitrary width carrying its own content — the wide date chip of the period
 * navigator. The pane sizes to its content; the content owns its padding.
 */
@Composable
fun GlassContentChip(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: CornerBasedShape = RoutineShapes.GlassChip,
    role: GlassRole = GlassRole.Chip,
    label: String? = null,
    content: @Composable () -> Unit,
) {
    val backdrop = LocalRoutineBackdrop.current
    val touch = rememberGlassTouch(LocalReduceMotion.current)
    Box(
        modifier
            .routineGlassTouch(touch, shape)
            .routineGlass(backdrop, shape, role, specular = true, tilt = LocalGlassTilt.current)
            .clip(shape)
            .defaultMinSize(minHeight = RoutineMetrics.TouchTarget)
            .clickable(
                onClickLabel = label,
                interactionSource = touch.source,
                indication = null,
                role = Role.Button,
                onClick = onClick,
            ),
    ) { content() }
}
