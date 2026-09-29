package com.example.mydailyroutine.features.onboarding.presentation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.DpVector
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.example.mydailyroutine.R
import com.example.mydailyroutine.core.designsystem.components.LanguageSelector
import com.example.mydailyroutine.core.designsystem.components.RoutineLabel
import com.example.mydailyroutine.core.designsystem.components.RoutineText
import com.example.mydailyroutine.core.designsystem.components.RoutineTextDefaults
import com.example.mydailyroutine.core.designsystem.motion.LocalReduceMotion
import com.example.mydailyroutine.core.designsystem.motion.effectSpec
import com.example.mydailyroutine.core.designsystem.motion.spatialSpec
import com.example.mydailyroutine.core.designsystem.theme.RoutineColors
import com.example.mydailyroutine.core.designsystem.theme.RoutineShapes
import com.example.mydailyroutine.core.designsystem.theme.RoutineSpacing
import com.example.mydailyroutine.core.presentation.TimelineAction
import java.time.LocalTime

/**
 * The first run, in **two** steps: who the reader is, and what should be on today when they arrive.
 *
 * It used to be four, and two of them were the app explaining itself: a step about school hours and a
 * step that was a legend of colours, tabs and gestures. Both read as a manual handed over before the
 * reader has seen anything, and both were answering questions nobody had asked yet — the colour
 * legend even described the calendar the way it worked two versions ago. What is left is the two
 * things the app genuinely cannot guess:
 *
 * * **A name, which may be left empty.** It appears in one greeting on an empty day; skipping it
 *   changes nothing else. Nothing here is required, and the whole flow can still be skipped.
 * * **A first day.** Loading the IB example fills the timeline with blocks that can be ticked off
 *   immediately, which is the only way to understand this app: it is a day, not a description of one.
 *
 * School hours moved to Settings → Ritem, where they already had a home and where a reader who knows
 * their timetable will look for them. No cloud question is asked: there is no account and no server,
 * so offering a "free backup" here would be a promise the app cannot keep.
 */
@Composable
fun OnboardingScreen(
    userName: String,
    schoolStart: LocalTime,
    schoolEnd: LocalTime,
    /**
     * The interface language as it is stored right now (`null` = follows the device). Shown on
     * the first screen, because a reader who does not read Slovenian should be able to leave the
     * whole flow in English — and the change applies at once, so step two is already theirs.
     */
    appLanguage: String?,
    onAction: (TimelineAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    var step by rememberSaveable { mutableIntStateOf(0) }
    var name by rememberSaveable { mutableStateOf(userName) }
    val reduceMotion = LocalReduceMotion.current

    // School hours are carried through unchanged: the flow no longer asks for them, but the
    // preferences the app ships with still have to be written with the values it started from.
    fun finish(loadExample: Boolean) {
        onAction(
            TimelineAction.FinishOnboarding(
                userName = name.trim(),
                schoolStart = schoolStart,
                schoolEnd = schoolEnd,
                loadExample = loadExample,
            ),
        )
    }

    Surface(modifier.fillMaxSize().testTag("onboarding"), color = RoutineColors.Background) {
        Column(
            Modifier.fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = RoutineSpacing.lg, vertical = RoutineSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(RoutineSpacing.lg),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                StepDots(step = step, count = Step.entries.size, modifier = Modifier.weight(1f))
                TextButton(
                    onClick = { finish(loadExample = false) },
                    modifier = Modifier.testTag("onboarding-skip"),
                ) {
                    RoutineLabel(stringResource(R.string.onboarding_skip), style = MaterialTheme.typography.labelLarge)
                }
            }
            Column(
                Modifier.weight(1f).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(RoutineSpacing.md),
            ) {
                // Leaving a step is a move, not a blink: the step the reader is going to comes in
                // from the direction they are travelling and the one they left goes out the other
                // side, on the same horizontal axis the timeline's own period switch uses. The size
                // follows on the spatial spring, so a tall first step folding into a short second one
                // shrinks instead of snapping — and both answer the system's remove-animations
                // setting, which a bare swap never had to.
                AnimatedContent(
                    targetState = step,
                    label = "onboarding-step",
                    transitionSpec = {
                        val forward = if (targetState >= initialState) 1 else -1
                        val spatial = spatialSpec<IntOffset>(reduceMotion)
                        val effect = effectSpec<Float>(reduceMotion)
                        (slideInHorizontally(spatial) { it / 4 * forward } + fadeIn(effect)) togetherWith
                            (slideOutHorizontally(spatial) { -it / 4 * forward } + fadeOut(effect))
                    },
                    sizeTransform = SizeTransform(sizeAnimationSpec = { _, _ -> spatialSpec<IntSize>(reduceMotion) }),
                ) { shown ->
                    // A column of its own, with the outer column's arrangement: the steps' children
                    // were siblings of a `spacedBy` column, and inside an AnimatedContent they would
                    // otherwise lose the space between them.
                    Column(verticalArrangement = Arrangement.spacedBy(RoutineSpacing.md)) {
                        when (Step.entries[shown]) {
                            Step.WELCOME -> {
                                Heading(stringResource(R.string.onboarding_welcome_title))
                                Body(stringResource(R.string.onboarding_welcome_body))
                                OutlinedTextField(
                                    value = name,
                                    onValueChange = { value -> name = value.take(40) },
                                    label = { RoutineText(stringResource(R.string.onboarding_name_label)) },
                                    placeholder = { RoutineText(stringResource(R.string.onboarding_name_hint)) },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth().testTag("onboarding-name"),
                                )
                                Spacer(Modifier.size(RoutineSpacing.md))
                                RoutineText(stringResource(R.string.onboarding_language_title),
                                    style = MaterialTheme.typography.titleSmall, maxLines = RoutineTextDefaults.Body,
                                    modifier = Modifier.semantics { heading() })
                                LanguageSelector(
                                    selected = appLanguage,
                                    onSelect = { language -> onAction(TimelineAction.SetAppLanguage(language)) },
                                    tagPrefix = "onboarding-language",
                                )
                                RoutineText(stringResource(R.string.onboarding_language_hint),
                                    style = MaterialTheme.typography.bodySmall, color = RoutineColors.TextSecondary,
                                    maxLines = RoutineTextDefaults.Paragraph)
                            }
                            Step.START -> {
                                Heading(stringResource(R.string.onboarding_start_title))
                                Body(stringResource(R.string.onboarding_start_body))
                                Body(stringResource(R.string.onboarding_rhythm_note))
                            }
                        }
                    }
                }
            }
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(RoutineSpacing.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                when (Step.entries[step]) {
                    Step.START -> {
                        Button(
                            onClick = { finish(loadExample = true) },
                            shape = RoutineShapes.Pill,
                            modifier = Modifier.weight(1f).testTag("onboarding-load-example"),
                        ) {
                            RoutineLabel(stringResource(R.string.onboarding_start_demo), style = MaterialTheme.typography.labelLarge)
                        }
                    }
                    else -> {
                        Button(
                            onClick = { step += 1 },
                            shape = RoutineShapes.Pill,
                            modifier = Modifier.weight(1f).testTag("onboarding-next"),
                        ) {
                            RoutineLabel(stringResource(R.string.onboarding_next), style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }
            if (Step.entries[step] == Step.START) {
                // The reader's own timetable is the best possible starting state, so it is offered
                // here rather than being something to go and find later. It finishes the flow
                // first: the import sheet is an overlay of the main screen and is composed only
                // once the first run is done, so the flow hands over and the sheet is already
                // open when the app appears.
                OutlinedButton(
                    onClick = { finish(loadExample = false); onAction(TimelineAction.ShowTimetableImport) },
                    shape = RoutineShapes.Pill,
                    modifier = Modifier.fillMaxWidth().testTag("onboarding-timetable"),
                ) {
                    RoutineLabel(stringResource(R.string.onboarding_start_timetable), style = MaterialTheme.typography.labelLarge)
                }
                OutlinedButton(
                    onClick = { finish(loadExample = false) },
                    shape = RoutineShapes.Pill,
                    modifier = Modifier.fillMaxWidth().testTag("onboarding-empty"),
                ) {
                    RoutineLabel(stringResource(R.string.onboarding_start_empty), style = MaterialTheme.typography.labelLarge)
                }
            }
            if (step > 0) {
                TextButton(
                    onClick = { step -= 1 },
                    modifier = Modifier.align(Alignment.CenterHorizontally).testTag("onboarding-back"),
                ) {
                    RoutineLabel(stringResource(R.string.onboarding_back), style = MaterialTheme.typography.labelLarge,
                        color = RoutineColors.TextSecondary)
                }
            }
        }
    }
}

/** Which screen of the flow is showing; the order here is the order the reader walks. */
private enum class Step { WELCOME, START }

@Composable
private fun Heading(text: String) {
    RoutineText(
        text = text,
        style = MaterialTheme.typography.headlineSmall,
        maxLines = RoutineTextDefaults.Title,
        modifier = Modifier.semantics { heading() },
    )
}

@Composable
private fun Body(text: String) {
    RoutineText(
        text = text,
        color = RoutineColors.TextSecondary,
        maxLines = RoutineTextDefaults.Paragraph,
    )
}

/** The dot the reader is on is the larger one; both sizes are the ones the indicator has always had. */
private val DotActive = 10.dp
private val DotInactive = 6.dp

/** Four dots, one per step: where the reader is, without a number that sounds like a form. */
@Composable
private fun StepDots(step: Int, count: Int, modifier: Modifier = Modifier) {
    val reduceMotion = LocalReduceMotion.current
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(RoutineSpacing.sm), verticalAlignment = Alignment.CenterVertically) {
        for (index in 0 until count) {
            // Grown and tinted rather than swapped: a step indicator that jumps to its new state
            // says the app skipped something. Both animate inside this scope and no wider — two dots
            // and a spacer is what recomposes while they move.
            val size by animateDpAsState(if (index == step) DotActive else DotInactive,
                spatialSpec<DpVector>(reduceMotion), label = "step-dot-size")
            val tint by animateColorAsState(if (index == step) RoutineColors.Primary else RoutineColors.TextDisabled,
                effectSpec(reduceMotion), label = "step-dot-tint")
            Box(Modifier.size(size).clip(CircleShape).background(tint))
        }
        Spacer(Modifier.size(RoutineSpacing.xs))
    }
}
