package com.example.mydailyroutine.core.designsystem.motion

import android.provider.Settings
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import com.example.mydailyroutine.core.designsystem.theme.TransitionMillis
import kotlin.math.PI
import kotlin.math.pow

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
val LocalReduceMotion = staticCompositionLocalOf { false }

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

/**
 * How a row travels when a list it belongs to is added to, removed from or reordered.
 *
 * Lists whose contents change are the one place where a missing animation reads as a bug: a task
 * ticked off, a subject deleted or a backlog entry scheduled makes everything below it jump to a
 * new place with no explanation of where it came from. The spring is calm and retargetable
 * mid-flight, and under remove-animations the row simply lands.
 *
 * Only for lists that actually mutate. A picker wheel or a fixed set of options has nothing to
 * animate and would only wobble.
 */
@Composable
fun listPlacementSpec(): FiniteAnimationSpec<IntOffset> = spatialSpec(LocalReduceMotion.current)

/** Movement: a calm spring, or nothing at all when the system asks for no animation. */
fun <T> spatialSpec(reduceMotion: Boolean): FiniteAnimationSpec<T> =
    if (reduceMotion) snap<T>()
    else spring<T>(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium)

/** Colour and opacity: a short tween that cannot overshoot, or nothing at all. */
fun <T> effectSpec(reduceMotion: Boolean, millis: Int = TransitionMillis): FiniteAnimationSpec<T> =
    if (reduceMotion) snap<T>() else tween<T>(millis)

/**
 * Apple's spring vocabulary, converted exactly rather than approximated.
 *
 * WWDC23 *Animate with springs* defines Apple's two parameters in terms of the classical ones:
 *
 * ```
 * mass      = 1
 * stiffness = (2π ÷ duration)²
 * damping   = (1 − bounce) × 4π ÷ duration        (bounce ≥ 0)
 * ```
 *
 * Compose spells the same physics as `stiffness` at unit mass plus a **damping ratio**
 * ζ = damping ÷ (2·√(stiffness·mass)), and substituting Apple's expressions cancels cleanly to
 * **ζ = 1 − bounce**. So an Apple spring is a Compose spring with
 *
 * ```
 * stiffness = (2π ÷ duration)²        dampingRatio = 1 − bounce
 * ```
 *
 * Apple's `duration` is the *perceptual* duration, not the settling time, which is why their numbers
 * look slow next to Material's: `.smooth`, `.snappy` and `.bouncy` all default to 0.5 s
 * (stiffness 158), `.bouncy(duration: 0.4)` is stiffness 247, and the drag-release spring in Apple's
 * own Liquid Glass samples (`response: 0.3, dampingFraction: 0.6`) is stiffness 439 at ζ 0.6.
 * Compose's `StiffnessMedium` (1500) is a 0.16 s spring — about three times quicker. Both are right
 * for their platform; the mistake is mixing them inside a single interaction, so the two families
 * are kept apart here: [spatialSpec] moves *content* the Material way, and the glass specs below
 * move *chrome* the Apple way.
 */
fun <T> appleSpring(duration: Float, bounce: Float): FiniteAnimationSpec<T> = spring<T>(
    dampingRatio = 1f - bounce,
    stiffness = (2f * PI.toFloat() / duration).pow(2),
)

/**
 * The numbers behind [appleSpring], kept in one place with their provenance so nobody has to guess
 * whether a value was invented or read.
 */
object AppleMotion {
    /** `.smooth` / `.snappy` / `.bouncy` all default to half a second of perceptual duration. */
    const val PresetDuration = 0.5f

    /** `.snappy`: brisk with a long tail rather than a bounce. */
    const val SnappyBounce = 0.15f

    /** `.bouncy`: visibly springy. Apple's own guidance stops at 0.4 — beyond that a UI element
     *  reads as exaggerated rather than as fluid. */
    const val BouncyBounce = 0.3f

    /** The drag-release spring in Apple's Liquid Glass samples: `response 0.3, dampingFraction 0.6`. */
    const val GlassTouchDuration = 0.3f
    const val GlassTouchBounce = 0.4f

    /** `.bouncy(duration: 0.4)`, which the same samples use when a glass cluster morphs open. */
    const val GlassMorphDuration = 0.4f

    /** How far an interactive glass control shrinks under the finger. Apple does not publish the
     *  value; measurements of `.glassEffect(.regular.interactive())` put it a few percent below 1. */
    const val PressScale = 0.96f

    /** Apple's draggable-glass sample lifts the element to 1.1 while it is being dragged. */
    const val DragLiftScale = 1.1f
}

/** Touch response for glass: Apple's own release spring, with its slight overshoot. */
fun <T> glassTouchSpec(reduceMotion: Boolean): FiniteAnimationSpec<T> =
    if (reduceMotion) snap<T>()
    else appleSpring<T>(AppleMotion.GlassTouchDuration, AppleMotion.GlassTouchBounce)

/** Morphing and expansion of glass: SwiftUI `.bouncy(duration: 0.4)`. */
fun <T> glassMorphSpec(reduceMotion: Boolean): FiniteAnimationSpec<T> =
    if (reduceMotion) snap<T>()
    else appleSpring<T>(AppleMotion.GlassMorphDuration, AppleMotion.BouncyBounce)

/**
 * The app's one breathing indicator.
 *
 * The NOW bands and the health badges used to each spin their own `rememberInfiniteTransition`,
 * so a busy day view carried a dozen overlapping loops, every one writing its own state every
 * frame and restarted or stopped on every card that scrolled in and out. During a fast fling the
 * churn spent the frame budget and read as stutter. One clock serves every indicator — they all
 * breathe in phase, which is what a live "now" should do anyway — and under the system's
 * remove-animations setting the loop never starts and the value holds its brightest state.
 */
private val SteadyPulse: State<Float> = mutableStateOf(1f)

val LocalPulse = staticCompositionLocalOf { SteadyPulse }

/**
 * The pulse as a [State], deliberately, and never as a `Float`.
 *
 * An infinite transition changes every frame. Read as a value it is read *during composition*, and
 * because [LocalPulse] is a static local — the kind that does not track its readers — providing
 * that value at the root recomposed the entire application on every frame of the loop, forever, on
 * a screen that refreshes 120 times a second. The animation is three dots breathing.
 *
 * Handing out the state object instead means the local's value never changes: the reference is
 * stable and the frames land in `.value`, which [pulsing] reads in the draw phase where a changing
 * number costs a repaint of one node and nothing else.
 */
@Composable
fun rememberAppPulse(reduceMotion: Boolean = LocalReduceMotion.current): State<Float> {
    if (reduceMotion) return SteadyPulse
    val transition = rememberInfiniteTransition(label = "app-pulse")
    return transition.animateFloat(
        0.78f, 1f,
        infiniteRepeatable(tween(2800, easing = CubicBezierEasing(0.37f, 0f, 0.63f, 1f)), RepeatMode.Reverse),
        label = "app-pulse-value",
    )
}

/** Breathes a node's opacity. `alpha(pulse.value)` would read the frame during composition. */
fun Modifier.pulsing(pulse: State<Float>): Modifier = graphicsLayer { alpha = pulse.value }
