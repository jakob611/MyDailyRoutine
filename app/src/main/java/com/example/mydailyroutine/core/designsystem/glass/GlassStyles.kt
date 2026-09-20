package com.example.mydailyroutine.core.designsystem.glass

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.mydailyroutine.core.designsystem.theme.RoutineColors

/** Optical parameters are independent of the palette and the renderer. */
@Immutable
data class GlassStyle(
    val surfaceAlpha: Float,
    val blur: Dp,
    val lensHeight: Dp,
    val lensAmount: Dp,
    val rim: Float,
    val depth: Boolean = false,
    val dispersion: Boolean = false,
)

object GlassStyles {
    // Local/static backdrops may use the brief's lighter materials. Scrolling chrome must also
    // pass AA when white text or a user-picked white subject stripe moves underneath it.
    val Card = GlassStyle(0.52f, 6.dp, 18.dp, 32.dp, 0.14f)
    val Compact = GlassStyle(0.58f, 4.dp, 14.dp, 24.dp, 0.12f)
    val Bar = GlassStyle(0.82f, 6.dp, 24.dp, 44.dp, 0.14f, dispersion = true)
    val Sheet = GlassStyle(0.82f, 10.dp, 24.dp, 44.dp, 0.14f, depth = true)
    val Control = GlassStyle(0.84f, 4.dp, 14.dp, 24.dp, 0.12f)

    val RimWidth = 1.dp
    const val RimWaist = 0.65f
    const val RimTail = 0.30f
    const val Specular = 1.0f
    const val SpecularFall = 0.18f
    const val TouchGlow = 0.02f
    const val TiltGlow = 0.015f
}

/** Neutral surfaces only. The fallback is opaque, including on Android 7–11. */
enum class GlassRole(val style: GlassStyle, val surface: Color) {
    Bar(GlassStyles.Bar, RoutineColors.SurfaceLow),
    Sheet(GlassStyles.Sheet, RoutineColors.SurfaceLow),
    Chip(GlassStyles.Compact, RoutineColors.SurfaceHigh),
    Control(GlassStyles.Control, RoutineColors.SurfaceLow),
}
