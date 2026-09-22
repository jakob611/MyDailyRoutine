package com.example.mydailyroutine.features.onboarding.presentation

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.mydailyroutine.R
import com.example.mydailyroutine.core.designsystem.components.RoutineLabel
import com.example.mydailyroutine.core.designsystem.components.RoutineText
import com.example.mydailyroutine.core.designsystem.components.RoutineTextDefaults
import com.example.mydailyroutine.core.designsystem.components.RoutineTimeField
import com.example.mydailyroutine.core.designsystem.theme.RoutineColors
import com.example.mydailyroutine.core.designsystem.theme.RoutineShapes
import com.example.mydailyroutine.core.designsystem.theme.RoutineSpacing
import com.example.mydailyroutine.core.presentation.TimelineAction
import java.time.LocalTime

/**
 * The first run, in four short steps: who the reader is, when school is, what the screens mean, and
 * a way to have a day with a block in it before the second minute is over.
 *
 * Three rules shaped it:
 *
 * * **Nothing here is required.** The name may be left empty, and the whole flow can be skipped; the
 *   defaults it would have set are already the ones the app ships with. A first screen that demands
 *   answers before it shows anything is the fastest way to lose the reader on a phone.
 * * **The last step produces a day, not a sentence.** "Load the IB example" fills today with blocks
 *   that can be ticked off straight away, which is the whole point of installing a routine app.
 * * **It explains, it does not sell.** The third step is a legend — the four scales, the add pill,
 *   the colours — because the interface already works, and a reader who knows what a colour means
 *   stops being afraid of tapping it.
 *
 * No cloud question is asked: there is no account and no server, so offering a "free backup" here
 * would be a promise the app cannot keep. When that changes, the step belongs between the third and
 * the fourth, and the flag in `SchedulePreferences` already exists to gate it.
 */
@Composable
fun OnboardingScreen(
    userName: String,
    schoolStart: LocalTime,
    schoolEnd: LocalTime,
    onAction: (TimelineAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    var step by rememberSaveable { mutableIntStateOf(0) }
    var name by rememberSaveable { mutableStateOf(userName) }
    var start by rememberSaveable { mutableStateOf(clock(schoolStart)) }
    var end by rememberSaveable { mutableStateOf(clock(schoolEnd)) }
    val lastStep = Step.entries.lastIndex
    val windowInvalid = start == end

    fun finish(loadExample: Boolean) {
        onAction(
            TimelineAction.FinishOnboarding(
                userName = name.trim(),
                schoolStart = LocalTime.parse(start),
                schoolEnd = LocalTime.parse(end),
                loadExample = loadExample,
            ),
        )
    }

    Surface(modifier.fillMaxSize().testTag("onboarding"), color = RoutineColors.Background) {
        Column(
            Modifier.fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = RoutineSpacing.xl, vertical = RoutineSpacing.lg),
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
                when (Step.entries[step]) {
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
                    }
                    Step.RHYTHM -> {
                        Heading(stringResource(R.string.onboarding_rhythm_title))
                        Body(stringResource(R.string.onboarding_rhythm_body))
                        RoutineTimeField(
                            value = start,
                            onPick = { start = it },
                            label = stringResource(R.string.onboarding_school_start),
                            modifier = Modifier.fillMaxWidth(),
                            wheelTag = "onboarding-start",
                        )
                        RoutineTimeField(
                            value = end,
                            onPick = { end = it },
                            label = stringResource(R.string.onboarding_school_end),
                            modifier = Modifier.fillMaxWidth(),
                            supporting = if (windowInvalid) stringResource(R.string.onboarding_rhythm_error) else null,
                            wheelTag = "onboarding-end",
                        )
                    }
                    Step.MEANING -> {
                        Heading(stringResource(R.string.onboarding_meaning_title))
                        Meaning(RoutineColors.Primary, stringResource(R.string.onboarding_meaning_scales))
                        Meaning(RoutineColors.Timer, stringResource(R.string.onboarding_meaning_swipe))
                        Meaning(RoutineColors.Warning, stringResource(R.string.onboarding_meaning_add))
                        Meaning(RoutineColors.FocusAccent, stringResource(R.string.onboarding_meaning_colors))
                        Meaning(RoutineColors.Success, stringResource(R.string.onboarding_meaning_quiet))
                    }
                    Step.START -> {
                        Heading(stringResource(R.string.onboarding_start_title))
                        Body(stringResource(R.string.onboarding_start_body))
                    }
                }
            }
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(RoutineSpacing.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (step > 0) {
                    OutlinedButton(
                        onClick = { step -= 1 },
                        shape = RoutineShapes.Pill,
                        modifier = Modifier.testTag("onboarding-back"),
                    ) {
                        RoutineLabel(stringResource(R.string.onboarding_back), style = MaterialTheme.typography.labelLarge)
                    }
                }
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
                            enabled = !(Step.entries[step] == Step.RHYTHM && windowInvalid),
                            shape = RoutineShapes.Pill,
                            modifier = Modifier.weight(1f).testTag("onboarding-next"),
                        ) {
                            RoutineLabel(stringResource(R.string.onboarding_next), style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }
            if (Step.entries[step] == Step.START) {
                OutlinedButton(
                    onClick = { finish(loadExample = false) },
                    shape = RoutineShapes.Pill,
                    modifier = Modifier.fillMaxWidth().testTag("onboarding-empty"),
                ) {
                    RoutineLabel(stringResource(R.string.onboarding_start_empty), style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

/** Which screen of the flow is showing; the order here is the order the reader walks. */
private enum class Step { WELCOME, RHYTHM, MEANING, START }

/** `07:45`, the shape every time field in the app speaks. */
private fun clock(time: LocalTime): String = "%02d:%02d".format(time.hour, time.minute)

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

/** One line of the legend: a colour, a name for it, and what it does. */
@Composable
private fun Meaning(color: Color, text: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(RoutineSpacing.md), verticalAlignment = Alignment.Top) {
        Box(
            Modifier.padding(top = RoutineSpacing.sm).size(10.dp).clip(CircleShape).background(color),
        )
        Body(text)
    }
}

/** Four dots, one per step: where the reader is, without a number that sounds like a form. */
@Composable
private fun StepDots(step: Int, count: Int, modifier: Modifier = Modifier) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(RoutineSpacing.sm), verticalAlignment = Alignment.CenterVertically) {
        for (index in 0 until count) {
            Box(
                Modifier.size(if (index == step) 10.dp else 6.dp)
                    .clip(CircleShape)
                    .background(if (index == step) RoutineColors.Primary else RoutineColors.TextDisabled),
            )
        }
        Spacer(Modifier.size(RoutineSpacing.xs))
    }
}
