package com.example.mydailyroutine.core.designsystem.glass

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import com.example.mydailyroutine.core.designsystem.motion.LocalReduceMotion
import kotlin.math.round
import android.os.Build
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import com.example.mydailyroutine.core.designsystem.motion.AppleMotion
import com.example.mydailyroutine.core.designsystem.motion.effectSpec
import com.example.mydailyroutine.core.designsystem.motion.glassTouchSpec
import com.example.mydailyroutine.core.designsystem.theme.RoutineColors
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.effects.lens

/**
 * Liquid glass for the floating chrome of the app (Kyant0's Backdrop, `io.github.kyant0:backdrop`).
 *
 * Three rules, taken from the library's own documentation and from where glass is supposed to live:
 *
 * 1. **Glass is only for the navigation layer** — the top bar, sticky list headers, sheet headers and
 *    footers, floating buttons. Content (lists, cards, grids) never gets glass; it is what the glass
 *    refracts.
 * 2. **A glass element must be a sibling of the layer it samples.** A node carrying both
 *    `layerBackdrop(b)` and `drawBackdrop(b)` draws itself into itself and kills the render thread
 *    with SIGSEGV, so [routineBackdropLayer] goes on the content and [routineGlass] only on chrome
 *    drawn outside it. Nested glass (a button inside a glass sheet) gets its own backdrop.
 * 3. **Readability is never traded for the effect.** Every panel paints a tinted surface over the
 *    refracted backdrop so small text stays above 4.5:1, and below Android 12 — where `RenderEffect`
 *    does not exist — the same panel is drawn as the matching solid surface.
 *
 * Effects use the pinned Backdrop 1.0.0 API: vibrancy, then blur, then lens. This is our
 * chosen chain, not a universal requirement of the library. Glass stays on floating chrome;
 * reading cards are opaque, avoiding repeated GPU effects and self-sampling layers.
 *
 * The modifier helpers are deliberately *not* `@Composable`: Compose lint requires composable
 * functions that return a value to be PascalCase, and `Modifier.routineGlass()` has to stay a normal
 * modifier so it composes with the rest of the chain.
 */

/** Backdrop of the window the glass samples. `null` means "no glass here" and callers fall back. */
val LocalRoutineBackdrop = staticCompositionLocalOf<Backdrop?> { null }

/** Blur is `RenderEffect` (Android 12+), the lens is AGSL (Android 13+). The library skips whatever
 *  the platform cannot do, so these flags only decide whether to paint the solid fallback. */
val glassSupported: Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
val lensSupported: Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

/** Device tilt in -1..1 on both axes, quantised: the raw rotation vector jitters every sample, and
 *  repaint-triggering noise is exactly how a specular highlight becomes a shimmer defect. */
@Immutable
data class GlassTilt(val x: Float = 0f, val y: Float = 0f)

val LocalGlassTilt = staticCompositionLocalOf { GlassTilt() }

/**
 * Reads the rotation vector while the host is alive and nothing else: one listener at UI rate,
 * unregistered on dispose, and under remove-animations the chrome simply keeps its static rim.
 * Phones without a rotation-vector sensor (or without the feature) keep GlassTilt() and the
 * highlight never appears — glass degrades to optics-only, never to a crash.
 */
@Composable
fun rememberGlassTilt(): GlassTilt {
    val context = LocalContext.current
    var tilt by remember { mutableStateOf(GlassTilt()) }
    if (LocalReduceMotion.current) return GlassTilt()
    DisposableEffect(Unit) {
        val manager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        val sensor = manager?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        if (manager == null || sensor == null) return@DisposableEffect onDispose {}
        val listener = object : SensorEventListener {
            private val matrix = FloatArray(9)
            private val orientation = FloatArray(3)
            override fun onSensorChanged(event: SensorEvent) {
                SensorManager.getRotationMatrixFromVector(matrix, event.values)
                SensorManager.getOrientation(matrix, orientation)
                val x = (orientation[2] / 0.5f).coerceIn(-1f, 1f).let { round(it * 20) / 20f }
                val y = (orientation[1] / 0.5f).coerceIn(-1f, 1f).let { round(it * 20) / 20f }
                if (x != tilt.x || y != tilt.y) tilt = GlassTilt(x, y)
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }
        manager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_UI)
        onDispose { manager.unregisterListener(listener) }
    }
    return tilt
}

/** Rim hairline. Drawn centred on the shape outline, so the clip leaves half of it: ~0.5 dp of light. */
private val RimWidth = GlassStyles.RimWidth

/** Creates the window backdrop and publishes it to the subtree; the ambient wash is drawn into it. */
@Composable
fun RoutineBackdropProvider(content: @Composable () -> Unit) {
    val backdrop = rememberLayerBackdrop {
        drawRect(RoutineColors.Background)
        drawContent()
    }
    CompositionLocalProvider(LocalRoutineBackdrop provides backdrop) { content() }
}

/** Marks the *content* node whose pixels the chrome refracts. Sibling of every glass element. */
fun Modifier.routineBackdropLayer(backdrop: Backdrop?): Modifier {
    val layer = backdrop as? LayerBackdrop ?: return this
    return this.layerBackdrop(layer)
}

/**
 * The specular edge: bright at the top-left, falling to almost nothing at the bottom-right, which is
 * how a lit pane of glass actually reads. One gradient for both the effect path and the fallback path
 * so a device without `RenderEffect` still gets the same edge.
 */
/**
 * Apple's specular edge: one hairline along the top of the pane, brightest at the leading corner and
 * gone by the trailing one, because a lit edge is never evenly lit. The diagonal [rimBrush] gives the
 * panel its rim; this gives it a light source. Panels whose top edge is the screen edge (the folding
 * top bar) opt out — there is nothing above them to catch the light, and a bright line under the
 * status bar reads as a rendering defect.
 */
private fun DrawScope.drawSpecular(role: GlassRole) {
    val peak = GlassStyles.Specular * role.style.rim
    val width = RimWidth.toPx()
    drawLine(
        brush = Brush.horizontalGradient(
            0f to RoutineColors.GlassRim.copy(alpha = peak),
            0.6f to RoutineColors.GlassRim.copy(alpha = peak * GlassStyles.SpecularFall),
            1f to Color.Transparent,
        ),
        start = Offset(0f, width / 2f),
        end = Offset(size.width, width / 2f),
        strokeWidth = width,
    )
}

private fun rimBrush(strength: Float, start: Offset = Offset.Zero, end: Offset = Offset.Unspecified) =
    Brush.linearGradient(
        0f to RoutineColors.GlassRim.copy(alpha = strength),
        0.42f to RoutineColors.GlassRim.copy(alpha = strength * GlassStyles.RimWaist),
        1f to RoutineColors.GlassRim.copy(alpha = strength * GlassStyles.RimTail),
        start = start,
        end = end,
    )

/**
 * The glass treatment: chosen effect chain (vibrancy ⇒ blur ⇒ lens),
 * finished with a tinted surface for legibility. Falls back to a plain solid surface when there is no
 * backdrop to sample or the platform cannot render effects.
 *
 * Pixel conversion happens inside the `effects` block because `BackdropEffectScope` is a `Density`,
 * which keeps this modifier free of any composable state.
 */
fun Modifier.routineGlass(
    backdrop: Backdrop?,
    shape: CornerBasedShape,
    role: GlassRole = GlassRole.Bar,
    specular: Boolean = true,
    tilt: GlassTilt = GlassTilt(),
): Modifier {
    if (backdrop == null || !glassSupported) {
        val fallback = role.surface
        // No RenderEffect, no lens — but the rim stays, because the edge is what tells the reader the
        // panel is a surface and not a stain on the background.
        return this.clip(shape).background(fallback)
            .border(RimWidth / 2f, rimBrush(role.style.rim), shape)
            .then(if (specular) Modifier.drawWithContent { drawContent(); drawSpecular(role) } else Modifier)
    }
    val surface: DrawScope.() -> Unit = {
        drawRect(role.surface.copy(alpha = role.style.surfaceAlpha))
        if (specular && (tilt.x != 0f || tilt.y != 0f)) {
            // The one highlight that cannot be faked with a static gradient: it follows the phone.
            val center = Offset(size.width * (0.5f + tilt.x * 0.45f), size.height * (0.5f + tilt.y * 0.45f))
            drawRect(
                Brush.radialGradient(
                    0f to RoutineColors.GlassRim.copy(alpha = GlassStyles.TiltGlow * role.style.rim * 3f),
                    1f to Color.Transparent,
                    center = center,
                    radius = size.width * 0.7f,
                ),
            )
        }
        // Specular rim, drawn last so it sits on top of the wash and survives the clip as a hairline.
        drawOutline(
            outline = shape.createOutline(size, layoutDirection, this),
            brush = rimBrush(role.style.rim, Offset.Zero, Offset(size.width, size.height)),
            style = Stroke(width = RimWidth.toPx()),
        )
        if (specular) drawSpecular(role)
    }
    val glassShape = shape
    return this.drawBackdrop(
        backdrop = backdrop,
        shape = { shape },
        effects = {
            vibrancy()
            blur(role.style.blur.toPx())
            // Refraction height must not exceed any visible corner's radius.
            val corners = listOf(glassShape.topStart, glassShape.topEnd, glassShape.bottomStart, glassShape.bottomEnd)
                .map { it.toPx(size, this) }.filter { it > 0f }
            val height = minOf(role.style.lensHeight.toPx(), corners.minOrNull() ?: 0f, size.minDimension / 2f)
            lens(refractionHeight = height, refractionAmount = role.style.lensAmount.toPx(),
                depthEffect = role.style.depth, chromaticAberration = role.style.dispersion)
        },
        onDrawSurface = surface,
    )
}

/**
 * A glass panel with content inside: the standard way to build a floating bar, header or button, so
 * clipping, padding and the fallback surface are identical everywhere.
 */
@Composable
fun RoutineGlassSurface(
    modifier: Modifier = Modifier,
    shape: CornerBasedShape,
    role: GlassRole = GlassRole.Bar,
    specular: Boolean = true,
    content: @Composable () -> Unit,
) {
    val backdrop = LocalRoutineBackdrop.current
    Box(modifier.routineGlass(backdrop, shape, role, specular, tilt = LocalGlassTilt.current)
        .clip(shape)) { content() }
}

/**
 * What Apple's `.interactive()` glass does under a finger, in Compose terms.
 *
 * `glassEffect(.regular.interactive())` gives a control four behaviours. Two are worth having here
 * and two are not, and the reasoning matters more than the result:
 *
 * * **Scale on press** — yes, on Apple's own release spring (`response 0.3, dampingFraction 0.6`),
 *   which overshoots by a few percent on the way back. That overshoot is the "gel" in liquid glass:
 *   a critically damped return reads as a widget, an underdamped one reads as a material.
 * * **Touch-point illumination** — yes, as a soft highlight that appears where the finger went down
 *   and fades on release. Apple radiates it into neighbouring glass; this app has one interactive
 *   control on screen at a time, so the highlight stays local.
 * * **Shimmer** — no. It is a continuous animation on a surface that already refracts and blurs
 *   every frame, and Apple's own guidance is to let glass rest in steady states.
 * * **Morphing between controls** (`GlassEffectContainer` + `glassEffectID`) — no. Morphing needs one
 *   shared sampling region for several glass elements; kyant0's backdrop gives each layer its own,
 *   and glass sampling glass is the one thing that library documents as fatal.
 */
@Immutable
class GlassTouch internal constructor(
    /** Hand this to `clickable(interactionSource = …, indication = null)`: the scale *is* the
     *  feedback, so a Material ripple on top of it would be a second, contradicting answer. */
    val source: MutableInteractionSource,
    /** The control's scale right now: [AppleMotion.PressScale] while pressed, 1 at rest. */
    val press: Float,
    /** 0..1 strength of the touch-point highlight. */
    val glow: Float,
    /** Where the finger went down, in the control's own coordinates; null once it lifts. */
    val point: Offset?,
)

/** Owns the press animation for one interactive glass control. */
@Composable
fun rememberGlassTouch(reduceMotion: Boolean, scale: Float = AppleMotion.PressScale): GlassTouch {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    // Movement on the glass spring (it may overshoot); the highlight on an effect tween (it may not).
    val fraction by animateFloatAsState(if (pressed) 1f else 0f, glassTouchSpec<Float>(reduceMotion),
        label = "glass-press")
    val glow by animateFloatAsState(if (pressed) 1f else 0f, effectSpec<Float>(reduceMotion, 120),
        label = "glass-glow")
    var point by remember { mutableStateOf<Offset?>(null) }
    LaunchedEffect(source) {
        source.interactions.collect { interaction ->
            when (interaction) {
                is PressInteraction.Press -> point = interaction.pressPosition
                is PressInteraction.Release -> point = null
                is PressInteraction.Cancel -> point = null
            }
        }
    }
    return GlassTouch(source, 1f - (1f - scale) * fraction, glow, point)
}

/**
 * Applies [GlassTouch] to a glass control. Non-composable on purpose (see the note at the top of the
 * file): the animation lives in [rememberGlassTouch], this only reads it. Put it **first** in the
 * modifier chain, before [routineGlass], so the scale wraps the whole panel — glass, rim and content
 * together — instead of fighting the panel's own clip.
 */
fun Modifier.routineGlassTouch(touch: GlassTouch, shape: CornerBasedShape): Modifier = this
    .graphicsLayer {
        scaleX = touch.press
        scaleY = touch.press
    }
    .drawWithContent {
        drawContent()
        val point = touch.point ?: return@drawWithContent
        if (touch.glow <= 0f) return@drawWithContent
        // Filled through the outline rather than drawn as a circle, because this modifier sits
        // outside the panel's clip: a raw circle would spill past the rounded corners.
        drawOutline(
            outline = shape.createOutline(size, layoutDirection, this),
            brush = Brush.radialGradient(
                colors = listOf(
                    RoutineColors.GlassRim.copy(alpha = GlassStyles.TouchGlow * touch.glow),
                    Color.Transparent,
                ),
                center = point,
                radius = (size.minDimension * 1.15f).coerceAtLeast(1f),
            ),
        )
    }

/** Local, low-opacity blue/violet light under the content, never a turquoise card fill. */
@Composable
fun RoutineAmbientBackground(modifier: Modifier = Modifier) {
    Box(
        modifier.drawBehind {
            drawRect(RoutineColors.Background)
            drawRect(
                Brush.radialGradient(
                    colors = listOf(
                        RoutineColors.AmbientTop.copy(alpha = RoutineColors.AmbientTopAlpha),
                        Color.Transparent,
                    ),
                    center = Offset(size.width * 0.25f, size.height * 0.2f),
                    radius = (size.minDimension * 0.65f).coerceAtLeast(1f),
                )
            )
            drawRect(
                Brush.radialGradient(
                    colors = listOf(
                        RoutineColors.AmbientBottom.copy(alpha = RoutineColors.AmbientBottomAlpha),
                        Color.Transparent,
                    ),
                    center = Offset(size.width * 0.8f, size.height * 0.75f),
                    radius = (size.minDimension * 0.75f).coerceAtLeast(1f),
                )
            )
        }
    )
}

/**
 * The light-leak under the floating cards of the design brief of 2026-09-19: a radial wash of the
 * card's own accent at 4 % alpha, centred just below the card, as if the glass pane bent a little
 * of the ambient light around its bottom edge. It goes *first* in the modifier chain — before any
 * clip — so the glow can fall outside the card's bounds; the card then paints over the part that
 * would otherwise sit under it.
 */
fun Modifier.liquidUnderGlow(accent: Color, alpha: Float = 0.04f): Modifier = this.drawBehind {
    val center = Offset(size.width * 0.5f, size.height * 1.04f)
    val radius = (size.width * 0.72f).coerceAtLeast(1f)
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(accent.copy(alpha = alpha), Color.Transparent),
            center = center,
            radius = radius,
        ),
        radius = radius,
        center = center,
    )
}
