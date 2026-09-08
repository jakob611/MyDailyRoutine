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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalView

val LocalRoutineHaptics = staticCompositionLocalOf<RoutineHaptics> { error("RoutineHaptics provider is required") }

/** App preference + system touch-haptic preference + hardware capability are all respected. */
@Stable
class RoutineHaptics internal constructor(private val view: View, private val enabled: State<Boolean>) {
    private val context = view.context.applicationContext
    private val vibrator: Vibrator = if (Build.VERSION.SDK_INT >= 31)
        context.getSystemService(VibratorManager::class.java).defaultVibrator
    else @Suppress("DEPRECATION") (context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator)
    private fun allowed(): Boolean = enabled.value && Settings.System.getInt(context.contentResolver,
        Settings.System.HAPTIC_FEEDBACK_ENABLED, 1) != 0
    fun tap() { if (allowed()) view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK) }
    fun dragStart() {
        if (allowed()) view.performHapticFeedback(if (Build.VERSION.SDK_INT >= 30) HapticFeedbackConstants.GESTURE_START else HapticFeedbackConstants.LONG_PRESS)
    }
    fun complete() = waveform(longArrayOf(0, 50, 40, 70), intArrayOf(0, 90, 0, 50))
    fun warning() {
        if (!allowed() || !vibrator.hasVibrator()) return
        if (Build.VERSION.SDK_INT >= 29) vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_DOUBLE_CLICK), touchAudio)
        else waveform(longArrayOf(0, 20, 45, 20), intArrayOf(0, 50, 0, 40))
    }
    private fun waveform(timings: LongArray, amplitudes: IntArray) {
        if (!allowed() || !vibrator.hasVibrator()) return
        if (Build.VERSION.SDK_INT >= 26) {
            val pattern = if (vibrator.hasAmplitudeControl()) VibrationEffect.createWaveform(timings, amplitudes, -1)
            else VibrationEffect.createWaveform(timings, -1)
            vibrator.vibrate(pattern, touchAudio)
        } else @Suppress("DEPRECATION") vibrator.vibrate(timings, -1, touchAudio)
    }
    private val touchAudio = AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build()
}

@Composable
fun rememberRoutineHaptics(enabled: Boolean): RoutineHaptics {
    val view = LocalView.current
    val currentEnabled = rememberUpdatedState(enabled)
    return remember(view) { RoutineHaptics(view, currentEnabled) }
}
