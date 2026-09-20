package com.example.mydailyroutine.core.designsystem.sound

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import com.example.mydailyroutine.R

val LocalRoutineSounds = staticCompositionLocalOf<RoutineSounds> { error("RoutineSounds provider is required") }

/**
 * The app's audio vocabulary: three interface sounds, picked from the brand's fifteen-sound
 * catalog and paired with the haptic vocabulary in [com.example.mydailyroutine.core.designsystem.haptics.RoutineHaptics].
 *
 * Sound is the louder sibling of a vibration, so it has to earn its place even more carefully:
 * haptics answer every step and tick, but a blip on each of those would turn the app into a toy.
 * The three sounds therefore sit only on the moments that already carry a *semantic* haptic —
 * an outcome the reader should notice without looking — and fire together with it, so ear and
 * palm tell the same story at the same instant:
 *
 * * **confirm** — catalog sound 2, with [com.example.mydailyroutine.core.designsystem.haptics.RoutineHaptics.confirm]:
 *   a change was committed, a block was completed.
 * * **reject** — catalog sound 3, with [com.example.mydailyroutine.core.designsystem.haptics.RoutineHaptics.reject]:
 *   something was deleted or refused.
 * * **open** — catalog sound 8, with [com.example.mydailyroutine.core.designsystem.haptics.RoutineHaptics.press]:
 *   a main surface took over the screen.
 *
 * The pool plays on the **media** stream at full scale: the sonification stream looked correct on
 * paper but on real hardware it is owned by the system-sound volume (often zero, never touched by
 * the volume keys the reader actually presses), which made the sounds inaudible in practice. On
 * the media stream the hardware keys the reader reaches for are exactly the ones that control
 * these sounds, and full scale means the sounds arrive as loud as the reader chose. The app's own
 * preference is the master switch; [preview] lets the settings screen prove audibility the moment
 * the switch flips on.
 */
@Stable
class RoutineSounds internal constructor(context: Context, private val enabled: State<Boolean>) {
    private val loaded = mutableSetOf<Int>()
    private val pool = SoundPool.Builder()
        .setMaxStreams(MAX_CONCURRENT)
        .setAudioAttributes(feedbackAudio)
        .build()
        .also { pool -> pool.setOnLoadCompleteListener { _, sampleId, status -> if (status == 0) loaded += sampleId } }

    private val confirmId = pool.load(context, R.raw.sound_2, 1)
    private val rejectId = pool.load(context, R.raw.sound_3, 1)
    private val openId = pool.load(context, R.raw.sound_8, 1)

    /** A change was committed or a block completed. Catalog sound 2, paired with the success haptic. */
    fun confirm() = play(confirmId)

    /** Something was deleted or refused. Catalog sound 3, paired with the error haptic. */
    fun reject() = play(rejectId)

    /** A main surface opened. Catalog sound 8, paired with the medium press haptic. */
    fun open() = play(openId)

    /**
     * Plays the confirm sound regardless of the preference, for the settings screen: flipping the
     * switch on should answer with the sound itself, before the preference write has propagated.
     */
    fun preview() = play(confirmId, force = true)

    private fun play(sampleId: Int, force: Boolean = false) {
        if (!force && !enabled.value) return
        if (!loaded.contains(sampleId)) return
        pool.play(sampleId, VOLUME, VOLUME, PRIORITY, 0, RATE)
    }

    internal fun release() = pool.release()

    private companion object {
        /** Full scale: the system media volume the reader sets is the loudness knob. */
        const val VOLUME = 1f
        const val MAX_CONCURRENT = 3
        const val PRIORITY = 1
        const val RATE = 1f

        // Media usage on purpose: it is the one stream whose volume the hardware keys control on
        // every device, so "turn it up" actually turns these sounds up.
        val feedbackAudio = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
    }
}

@Composable
fun rememberRoutineSounds(enabled: Boolean): RoutineSounds {
    val context = LocalContext.current.applicationContext
    val currentEnabled = rememberUpdatedState(enabled)
    val sounds = remember(context) { RoutineSounds(context, currentEnabled) }
    DisposableEffect(sounds) { onDispose { sounds.release() } }
    return sounds
}
