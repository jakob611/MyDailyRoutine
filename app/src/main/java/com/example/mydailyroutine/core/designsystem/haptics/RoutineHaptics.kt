package com.example.mydailyroutine.core.designsystem.haptics

import android.content.Context
import android.media.AudioAttributes
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalView

val LocalRoutineHaptics = staticCompositionLocalOf<RoutineHaptics> { error("RoutineHaptics provider is required") }

/**
 * The app's touch vocabulary, mapped one to one onto Apple's feedback generators.
 *
 * iOS does not have "a vibration". It has three semantic families, and the difference between them
 * is what makes an iPhone feel precise:
 *
 * * **Impact** (`UIImpactFeedbackGenerator`) — a collision, in weights `light`, `medium`, `heavy`,
 *   `soft` and `rigid`. Used when a control takes the finger and when something snaps into place.
 * * **Selection** (`UISelectionFeedbackGenerator`) — the lightest tick there is, fired on every
 *   discrete step: a picker wheel, a segment, a day cell. It is what makes scrolling through values
 *   feel like a detent rail rather than a smooth slide.
 * * **Notification** (`UINotificationFeedbackGenerator`) — an *outcome*: `.success` is two light
 *   taps, `.warning` is one medium tap, `.error` is three taps of rising strength. Users read the
 *   pattern without looking, which is the whole point.
 *
 * Android's equivalent vocabulary is `VibrationEffect.Composition`: hardware primitives
 * (`PRIMITIVE_TICK`, `PRIMITIVE_CLICK`, `PRIMITIVE_THUD`, `PRIMITIVE_LOW_TICK`) that can be scaled
 * by intensity and chained into a pattern — the same building blocks Core Haptics uses. Two honest
 * differences:
 *
 * * Apple exposes **sharpness** as a second continuous parameter; Android's public API exposes only
 *   the intensity scale, and the sharpness is baked into the primitive (a `TICK` is short and crisp,
 *   a `THUD` is low and soft). Choosing the primitive *is* choosing the sharpness.
 * * Motors differ far more across Android hardware than Taptic Engines do across iPhones, so every
 *   method here is three tiers deep: a composed primitive with an explicit intensity (API 31+ and
 *   only if the motor reports it can render primitives), a predefined effect (API 29+), and finally
 *   the platform constant via `View.performHapticFeedback`, which every OEM tunes for its own
 *   hardware. A device that cannot do the fancy thing still gets the right *kind* of feedback.
 *
 * Everything is gated by the app's own preference, the system's touch-haptic setting and the
 * presence of a motor, so turning haptics off anywhere turns them off everywhere.
 */
@Stable
class RoutineHaptics internal constructor(private val view: View, private val enabled: State<Boolean>) {
    private val context = view.context.applicationContext
    private val vibrator: Vibrator = systemVibrator(context)

    private fun allowed(): Boolean = enabled.value && Settings.System.getInt(context.contentResolver,
        Settings.System.HAPTIC_FEEDBACK_ENABLED, 1) != 0

    // ---- impact --------------------------------------------------------------------------------

    /** A control was touched. Apple: `UIImpactFeedbackGenerator(style: .light)`. */
    fun tap() {
        if (!allowed()) return
        if (Build.VERSION.SDK_INT >= 31 && primitivesSupported()) {
            vibrate(primitive(VibrationEffect.Composition.PRIMITIVE_TICK, 0.55f))
        } else if (Build.VERSION.SDK_INT >= 29) {
            vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK))
        } else {
            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
        }
    }

    /**
     * A glass control took the finger down — the touch-down half of Apple's
     * `.glassEffect(.regular.interactive())`, whose visual scale-down and haptic arrive together.
     * Apple: impact `.medium`.
     */
    fun press() {
        if (!allowed()) return
        if (Build.VERSION.SDK_INT >= 31 && primitivesSupported()) {
            vibrate(primitive(VibrationEffect.Composition.PRIMITIVE_CLICK, 0.8f))
        } else if (Build.VERSION.SDK_INT >= 29) {
            vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
        } else {
            view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
        }
    }

    /**
     * Something heavy snapped into place: a drag landed, a block dropped onto the timeline, a long
     * press armed a mode. Apple: impact `.heavy` / `.rigid`.
     */
    fun impact() {
        if (!allowed()) return
        if (Build.VERSION.SDK_INT >= 31 && primitivesSupported()) {
            vibrate(primitive(VibrationEffect.Composition.PRIMITIVE_THUD, 1f))
        } else if (Build.VERSION.SDK_INT >= 29) {
            vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK))
        } else {
            view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
        }
    }

    // ---- selection -----------------------------------------------------------------------------

    /**
     * One discrete step under the finger: a segment, a day cell, a minute on a stepper, a chip in a
     * filter row. Apple: `UISelectionFeedbackGenerator.selectionChanged()` — deliberately the
     * quietest thing in the vocabulary, because it fires often.
     */
    fun selection() {
        if (!allowed()) return
        if (Build.VERSION.SDK_INT >= 31 && primitivesSupported()) {
            vibrate(primitive(VibrationEffect.Composition.PRIMITIVE_LOW_TICK, 0.5f))
        } else if (Build.VERSION.SDK_INT >= 29) {
            vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK))
        } else {
            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
        }
    }

    /** A drag crossed into a new state — a detent, a snap point, a threshold. */
    fun dragThreshold() {
        if (!allowed()) return
        if (Build.VERSION.SDK_INT >= 31 && primitivesSupported()) {
            vibrate(primitive(VibrationEffect.Composition.PRIMITIVE_TICK, 0.7f))
        } else {
            view.performHapticFeedback(
                if (Build.VERSION.SDK_INT >= 30) HapticFeedbackConstants.SEGMENT_TICK
                else HapticFeedbackConstants.CLOCK_TICK)
        }
    }

    // ---- notification --------------------------------------------------------------------------

    /**
     * A change was committed and accepted. Apple: notification `.success` — two light taps, which
     * is exactly `PRIMITIVE_TICK` into `PRIMITIVE_CLICK` (and `EFFECT_DOUBLE_CLICK` below API 31).
     */
    fun confirm() {
        if (!allowed()) return
        if (Build.VERSION.SDK_INT >= 31 && primitivesSupported()) {
            vibrate(pattern(VibrationEffect.Composition.PRIMITIVE_TICK to 0.5f,
                VibrationEffect.Composition.PRIMITIVE_CLICK to 0.85f))
        } else if (Build.VERSION.SDK_INT >= 29) {
            vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_DOUBLE_CLICK))
        } else {
            waveform(longArrayOf(0, 25, 60, 30), intArrayOf(0, 70, 0, 110))
        }
    }

    /**
     * A change was refused or destroyed. Apple: notification `.error` — three taps of rising
     * strength, which the reader recognises without looking at the screen.
     */
    fun reject() {
        if (!allowed()) return
        if (Build.VERSION.SDK_INT >= 31 && primitivesSupported()) {
            vibrate(pattern(VibrationEffect.Composition.PRIMITIVE_TICK to 0.4f,
                VibrationEffect.Composition.PRIMITIVE_CLICK to 0.7f,
                VibrationEffect.Composition.PRIMITIVE_THUD to 1f))
        } else if (Build.VERSION.SDK_INT >= 29) {
            vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK))
        } else {
            waveform(longArrayOf(0, 20, 50, 25, 50, 35), intArrayOf(0, 60, 0, 110, 0, 170))
        }
    }

    /**
     * Something needs attention but nothing broke. Apple: notification `.warning` — a single medium
     * tap. (It used to be a double click here, which is the *success* pattern; the two outcomes now
     * feel different, as they do on iOS.)
     */
    fun warning() {
        if (!allowed()) return
        if (Build.VERSION.SDK_INT >= 31 && primitivesSupported()) {
            vibrate(primitive(VibrationEffect.Composition.PRIMITIVE_CLICK, 1f))
        } else if (Build.VERSION.SDK_INT >= 29) {
            vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
        } else {
            waveform(longArrayOf(0, 20, 45, 20), intArrayOf(0, 50, 0, 40))
        }
    }

    /** Work finished. Kept as its own name because timeline effects call it; same pattern as [confirm]. */
    fun complete() = confirm()

    // ---- gesture ---------------------------------------------------------------------------------

    /** A drag began: the platform's own gesture-start feedback, which OEMs tune per device. */
    fun dragStart() {
        if (!allowed()) return
        view.performHapticFeedback(
            if (Build.VERSION.SDK_INT >= 30) HapticFeedbackConstants.GESTURE_START
            else HapticFeedbackConstants.LONG_PRESS)
    }

    /** A drag ended. */
    fun dragEnd() {
        if (!allowed()) return
        view.performHapticFeedback(
            if (Build.VERSION.SDK_INT >= 30) HapticFeedbackConstants.GESTURE_END
            else HapticFeedbackConstants.VIRTUAL_KEY)
    }

    /** A switch changed side. Android has had distinct on/off haptics since API 34, as iOS does. */
    fun toggle(on: Boolean) {
        if (!allowed()) return
        if (Build.VERSION.SDK_INT >= 34) {
            view.performHapticFeedback(
                if (on) HapticFeedbackConstants.TOGGLE_ON else HapticFeedbackConstants.TOGGLE_OFF)
        } else if (on) confirm() else tap()
    }

    // ---- tiers -----------------------------------------------------------------------------------

    /**
     * Primitives exist from API 30 but are optional hardware: a motor that cannot render them says
     * so, and the caller drops a tier. The version check is duplicated at every call site on purpose
     * — that is what lint reads to prove the API is guarded.
     */
    private fun primitivesSupported(): Boolean =
        Build.VERSION.SDK_INT >= 31 && runCatching { vibrator.arePrimitivesSupported() }.getOrDefault(false)

    @RequiresApi(31)
    private fun primitive(id: Int, scale: Float): VibrationEffect =
        VibrationEffect.startComposition().addPrimitive(id, scale).compose()

    @RequiresApi(31)
    private fun pattern(vararg steps: Pair<Int, Float>): VibrationEffect {
        val composition = VibrationEffect.startComposition()
        for (step in steps) composition.addPrimitive(step.first, step.second)
        return composition.compose()
    }

    @RequiresApi(29)
    private fun vibrate(effect: VibrationEffect) {
        if (!vibrator.hasVibrator()) return
        vibrator.vibrate(effect, touchAudio)
    }

    @Suppress("DEPRECATION") // the pre-API-26 overload below is the only one those devices have
    private fun waveform(timings: LongArray, amplitudes: IntArray) {
        if (!vibrator.hasVibrator()) return
        if (Build.VERSION.SDK_INT >= 26) {
            val pattern = if (vibrator.hasAmplitudeControl()) VibrationEffect.createWaveform(timings, amplitudes, -1)
            else VibrationEffect.createWaveform(timings, -1)
            vibrator.vibrate(pattern, touchAudio)
        } else vibrator.vibrate(timings, -1, touchAudio)
    }

    private val touchAudio = AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build()
}

/**
 * `VibratorManager` from API 31, the deprecated service lookup below it. Both return the same motor;
 * only the way of asking differs.
 */
@Suppress("DEPRECATION")
private fun systemVibrator(context: Context): Vibrator =
    if (Build.VERSION.SDK_INT >= 31) context.getSystemService(VibratorManager::class.java).defaultVibrator
    else context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

@Composable
fun rememberRoutineHaptics(enabled: Boolean): RoutineHaptics {
    val view = LocalView.current
    val currentEnabled = rememberUpdatedState(enabled)
    return remember(view) { RoutineHaptics(view, currentEnabled) }
}
