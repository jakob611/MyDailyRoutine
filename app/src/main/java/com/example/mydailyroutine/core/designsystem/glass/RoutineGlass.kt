package com.example.mydailyroutine.core.designsystem.glass

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.mydailyroutine.core.designsystem.theme.RoutineColors
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy

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
    val tintAlpha: Float,
    val fallback: Color,
) {
    /** Top bar, segmented control row, any header floating over scrolling content. */
    Bar(8.dp, 10.dp, 20.dp, depth = false, tintAlpha = RoutineColors.GlassTintAlpha, fallback = RoutineColors.GlassFallbackStrong),
    /** Sticky header and footer inside a bottom sheet. */
    Sheet(10.dp, 12.dp, 24.dp, depth = true, tintAlpha = RoutineColors.GlassTintStrongAlpha, fallback = RoutineColors.GlassFallbackStrong),
    /** Filter chips, tabs, small pill buttons. */
    Chip(3.dp, 8.dp, 16.dp, depth = false, tintAlpha = RoutineColors.GlassTintAlpha, fallback = RoutineColors.GlassFallback),
    /** Floating action button and other floating primary controls. */
    Control(4.dp, 12.dp, 24.dp, depth = true, tintAlpha = 0f, fallback = RoutineColors.GlassFallback),
}

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
): Modifier {
    if (backdrop == null || !glassSupported) {
        val fallback = if (hue) tint else role.fallback
        return this.clip(shape).background(fallback)
    }
    // A coloured control is tinted the way the library documents: hue-blend first so the refracted
    // backdrop keeps its own shading, then a translucent wash of the accent on top. Neutral chrome
    // only gets the wash, because its job is to make text readable, not to carry meaning.
    val surface: (DrawScope.() -> Unit)? = when {
        hue -> {
            drawRect(tint, blendMode = BlendMode.Hue)
            drawRect(tint.copy(alpha = RoutineColors.GlassTintStrongAlpha))
        }
        role.tintAlpha > 0f -> { drawRect(tint.copy(alpha = role.tintAlpha)) }
        else -> null
    }
    return this.drawBackdrop(
        backdrop = backdrop,
        shape = { shape },
        effects = {
            vibrancy()
            blur(role.blur.toPx())
            lens(role.lensHeight.toPx(), role.lensAmount.toPx(), role.depth)
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
    content: @Composable () -> Unit,
) {
    val backdrop = LocalRoutineBackdrop.current
    Box(modifier.routineGlass(backdrop, shape, role, tint, hue).clip(shape)) { content() }
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
