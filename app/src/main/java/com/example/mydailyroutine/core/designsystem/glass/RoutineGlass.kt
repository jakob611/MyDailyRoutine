package com.example.mydailyroutine.core.designsystem.glass

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
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
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
import com.kyant.backdrop.effects.colorControls
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
 * The three things that make this read as *liquid glass* rather than as a grey translucent rectangle
 * are the three things Apple's material does and most Android copies skip:
 *
 * * **A heavy blur** (24-28 dp for bars and sheets, against Apple's ~30-40 px regular material and
 *   40-48 for navigation bars). Heavy blur is also what buys legibility: it removes the high-frequency
 *   detail that competes with text, so the tint can stay at 0.74 instead of 0.82 and the panel stays
 *   see-through.
 * * **A saturation and brightness lift on the blurred content** (`colorControls`, ~160 % saturation —
 *   `vibrancy()` is exactly 150 %). This is what makes glass look lit from behind.
 * * **A specular rim**: one hairline of light along the top-left edge falling away to the bottom-right.
 *   Without an edge, a translucent panel has no boundary and reads as a smudge.
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

/**
 * Where glass is used and how strong it is. One row per role keeps every bar, sheet and button
 * optically identical instead of letting each screen invent its own blur radius.
 *
 * `lensHeight` must stay at or below the smallest corner radius of the shape or the refraction breaks
 * at that corner; the `RoutineShapes.Glass*` shapes are all ≥ 14 dp.
 */
enum class GlassRole(
    val blur: Dp,
    val lensHeight: Dp,
    val lensAmount: Dp,
    val depth: Boolean,
    val dispersion: Boolean,
    val rim: Float,
    val tintAlpha: Float,
    val fallback: Color,
) {
    /** Top bar, segmented control row, any header floating over scrolling content. */
    Bar(24.dp, 14.dp, 22.dp, depth = false, dispersion = true, rim = 0.30f,
        tintAlpha = RoutineColors.GlassTintAlpha, fallback = RoutineColors.GlassFallbackStrong),
    /** Sticky header and footer inside a bottom sheet. Thickest blur: it sits over the most content. */
    Sheet(28.dp, 14.dp, 26.dp, depth = true, dispersion = true, rim = 0.26f,
        tintAlpha = RoutineColors.GlassTintStrongAlpha, fallback = RoutineColors.GlassFallbackStrong),
    /** Filter chips, tabs, small pill buttons. Thinner material, and no dispersion at this size: on a
     *  32 dp chip the colour fringing reads as a printing defect, not as optics. */
    Chip(14.dp, 8.dp, 14.dp, depth = false, dispersion = false, rim = 0.22f,
        tintAlpha = RoutineColors.GlassTintAlpha, fallback = RoutineColors.GlassFallback),
    /** Floating action button and other floating primary controls. */
    Control(18.dp, 16.dp, 26.dp, depth = true, dispersion = true, rim = 0.38f,
        tintAlpha = 0f, fallback = RoutineColors.GlassFallback),
}

/** Rim hairline. Drawn centred on the shape outline, so the clip leaves half of it: ~0.8 dp of light. */
private val RimWidth = 1.6.dp

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
    val peak = RoutineColors.GlassSpecular * role.rim
    val width = RimWidth.toPx()
    drawLine(
        brush = Brush.horizontalGradient(
            0f to RoutineColors.GlassRim.copy(alpha = peak),
            0.6f to RoutineColors.GlassRim.copy(alpha = peak * RoutineColors.GlassSpecularFall),
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
        0.42f to RoutineColors.GlassRim.copy(alpha = strength * RoutineColors.GlassRimWaist),
        1f to RoutineColors.GlassRim.copy(alpha = strength * RoutineColors.GlassRimTail),
        start = start,
        end = end,
    )

/**
 * The glass treatment: effect chain in the order the library requires (colour filter ⇒ blur ⇒ lens),
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
    tint: Color = RoutineColors.GlassTint,
    hue: Boolean = false,
    specular: Boolean = true,
): Modifier {
    if (backdrop == null || !glassSupported) {
        val fallback = if (hue) tint else role.fallback
        // No RenderEffect, no lens — but the rim stays, because the edge is what tells the reader the
        // panel is a surface and not a stain on the background.
        return this.clip(shape).background(fallback)
            .border(RimWidth / 2f, rimBrush(role.rim), shape)
            .then(if (specular) Modifier.drawWithContent { drawContent(); drawSpecular(role) } else Modifier)
    }
    // A coloured control is tinted the way the library documents: hue-blend first so the refracted
    // backdrop keeps its own shading, then a translucent wash of the accent on top. Neutral chrome
    // only gets the wash, because its job is to make text readable, not to carry meaning.
    // Braces after a `when` arrow are the branch body, not a lambda, so the drawing is written as an
    // `if` whose branches are lambdas typed by the declaration above them.
    val wash: (DrawScope.() -> Unit)? = if (hue) {
        {
            drawRect(tint, blendMode = BlendMode.Hue)
            drawRect(tint.copy(alpha = RoutineColors.GlassTintStrongAlpha))
        }
    } else if (role.tintAlpha > 0f) {
        { drawRect(tint.copy(alpha = role.tintAlpha)) }
    } else {
        null
    }
    val surface: DrawScope.() -> Unit = {
        wash?.invoke(this)
        // Specular rim, drawn last so it sits on top of the wash and survives the clip as a hairline.
        drawOutline(
            outline = shape.createOutline(size, layoutDirection, this),
            brush = rimBrush(role.rim, Offset.Zero, Offset(size.width, size.height)),
            style = Stroke(width = RimWidth.toPx()),
        )
        if (specular) drawSpecular(role)
    }
    return this.drawBackdrop(
        backdrop = backdrop,
        shape = { shape },
        effects = {
            // Colour filter, then blur, then lens — the order the library requires, each stage
            // chaining onto the previous RenderEffect. The lift is what makes glass read as lit from
            // behind rather than as a grey rectangle: 160 % saturation (`vibrancy()` is 150 %),
            // a hair of contrast, and +4 % brightness, which Apple's materials also apply.
            colorControls(brightness = 0.04f, contrast = 1.02f, saturation = 1.6f)
            blur(role.blur.toPx())
            lens(role.lensHeight.toPx(), role.lensAmount.toPx(), role.depth, role.dispersion)
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
    tint: Color = RoutineColors.GlassTint,
    hue: Boolean = false,
    specular: Boolean = true,
    content: @Composable () -> Unit,
) {
    val backdrop = LocalRoutineBackdrop.current
    Box(modifier.routineGlass(backdrop, shape, role, tint, hue, specular).clip(shape)) { content() }
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
                    RoutineColors.GlassRim.copy(alpha = RoutineColors.GlassTouchGlow * touch.glow),
                    Color.Transparent,
                ),
                center = point,
                radius = (size.minDimension * 1.15f).coerceAtLeast(1f),
            ),
        )
    }

/**
 * The ambient wash behind every screen: a vertical graphite ramp plus one indigo glow at the top and
 * one amber glow at the bottom, both under 6 % alpha. Its job is twofold — keep elevation readable on
 * an OLED panel and give the glass something to refract where a floating bar sits over empty space.
 */
@Composable
fun RoutineAmbientBackground(modifier: Modifier = Modifier) {
    Box(
        modifier.drawBehind {
            drawRect(
                Brush.verticalGradient(
                    0f to RoutineColors.Background,
                    0.5f to RoutineColors.Surface1,
                    1f to RoutineColors.Background,
                )
            )
            drawRect(
                Brush.radialGradient(
                    colors = listOf(
                        RoutineColors.AmbientTop.copy(alpha = RoutineColors.AmbientTopAlpha),
                        Color.Transparent,
                    ),
                    center = Offset(size.width * 0.16f, size.height * 0.01f),
                    radius = (size.width * 1.15f).coerceAtLeast(1f),
                )
            )
            drawRect(
                Brush.radialGradient(
                    colors = listOf(
                        RoutineColors.AmbientBottom.copy(alpha = RoutineColors.AmbientBottomAlpha),
                        Color.Transparent,
                    ),
                    center = Offset(size.width * 0.94f, size.height * 0.99f),
                    radius = (size.width * 1.25f).coerceAtLeast(1f),
                )
            )
        }
    )
}
