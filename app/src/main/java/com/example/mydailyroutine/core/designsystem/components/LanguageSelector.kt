package com.example.mydailyroutine.core.designsystem.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.FilterChip
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.example.mydailyroutine.R
import com.example.mydailyroutine.core.designsystem.theme.RoutineShapes
import com.example.mydailyroutine.core.designsystem.theme.RoutineSpacing
import com.example.mydailyroutine.domain.model.AppLanguage

/**
 * The reader's choice of interface language, as three chips.
 *
 * The same control lives in two places on purpose — the first onboarding screen and the settings
 * sheet — because the choice is made once, early, and then forgotten: it applies immediately,
 * persists on the device, and both screens send the same
 * [com.example.mydailyroutine.core.presentation.TimelineAction.SetAppLanguage].
 *
 * Each chip is labelled in the language it selects ("Slovenščina", "English"), the convention for
 * language pickers: a reader who reads neither language can still find the one that is theirs, and
 * the labels never need re-translating when the interface switches around them.
 *
 * [tagPrefix] is empty by default; a caller that wants the chips addressable from a device test
 * passes its own prefix and gets one tag per chip (`<prefix>-system`, `<prefix>-sl`, `<prefix>-en`).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LanguageSelector(
    selected: String?,
    onSelect: (String?) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    tagPrefix: String = "",
) {
    // The selection haptic is owned by the central action wrapper (the same one every other
    // control in the app answers through), so this component only reports the choice.
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(RoutineSpacing.sm),
        verticalArrangement = Arrangement.spacedBy(RoutineSpacing.xs),
    ) {
        chip(null, R.string.language_system, "system", selected, enabled, tagPrefix, onSelect)
        chip(AppLanguage.SLOVENIAN, R.string.language_slovenian, "sl", selected, enabled, tagPrefix, onSelect)
        chip(AppLanguage.ENGLISH, R.string.language_english, "en", selected, enabled, tagPrefix, onSelect)
    }
}

@Composable
private fun chip(
    value: String?,
    labelRes: Int,
    tag: String,
    selected: String?,
    enabled: Boolean,
    tagPrefix: String,
    onSelect: (String?) -> Unit,
) {
    FilterChip(
        selected = selected == value,
        onClick = { if (selected != value) onSelect(value) },
        enabled = enabled,
        label = { RoutineLabel(stringResource(labelRes)) },
        shape = RoutineShapes.Chip,
        modifier = if (tagPrefix.isEmpty()) Modifier else Modifier.testTag("$tagPrefix-$tag"),
    )
}
