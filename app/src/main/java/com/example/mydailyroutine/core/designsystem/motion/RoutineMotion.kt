package com.example.mydailyroutine.core.designsystem.motion

import android.provider.Settings
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import com.example.mydailyroutine.core.designsystem.theme.TransitionMillis

/**
 * The app's motion, in one place.
 *
 * Two specs, because Material 3 splits motion into two kinds and they need opposite tuning:
 *
 * * **Spatial** ([spatialSpec]) moves things — position, size, shape. It is a *spring*, not a
 *   duration: a spring can be retargeted mid-gesture, so interrupting a screen change with another
 *   one stays smooth instead of jumping. Damping is "no bouncy" on purpose; this is a planning tool,
 *   and M3's own guidance is that the calm `standard` scheme fits utilitarian apps while the bouncy
 *   `expressive` scheme is for hero moments. The one place the app allows a bounce is the overdue
 *   badge (`PopSpring`), which *is* a hero moment.
 * * **Effect** ([effectSpec]) changes colour and opacity. It is a plain tween at 180 ms — inside the
 *   200-300 ms window Material recommends for touch-driven transitions, and never overshooting,
 *   because an opacity that goes past 1 is not a flourish, it is a flicker.
 *
 * Everything obeys the system's **Remove animations** setting. When the user turns animations off
 * (accessibility → remove animations, which sets the animator/window/transition scales to 0), every
 * transition in the app resolves with [snap]: no slides, no fades, no rotating chevrons. Motion
 * sensitivity is a vestibular condition, not a taste, so this is not optional polish.
 */
val LocalReduceMotion = staticCompositionLocalOf(false)

/**
 * Reads the system animation scales once per composition host. Any of the three being zero means the
 * user asked for no animation; `ANIMATOR_DURATION_SCALE` is the one the "Remove animations" toggle
 * writes, the other two cover OEMs and adb.
 */
@Composable
fun rememberReduceMotion(): Boolean {
    val resolver = LocalContext.current.contentResolver
    return remember(resolver) {
        runCatching {
            listOf(Settings.Global.ANIMATOR_DURATION_SCALE, Settings.Global.TRANSITION_ANIMATION_SCALE,
                Settings.Global.WINDOW_ANIMATION_SCALE).any { Settings.Global.getFloat(resolver, it, 1f) == 0f }
        }.getOrDefault(false)
    }
}

/** Movement: a calm spring, or nothing at all when the system asks for no animation. */
fun <T> spatialSpec(reduceMotion: Boolean): FiniteAnimationSpec<T> =
    if (reduceMotion) snap() else spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium)

/** Colour and opacity: a short tween that cannot overshoot, or nothing at all. */
fun <T> effectSpec(reduceMotion: Boolean, millis: Int = TransitionMillis): FiniteAnimationSpec<T> =
    if (reduceMotion) snap() else tween(millis)
