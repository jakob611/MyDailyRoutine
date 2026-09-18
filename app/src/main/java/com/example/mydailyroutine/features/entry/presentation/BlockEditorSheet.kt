package com.example.mydailyroutine.features.entry.presentation

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.mydailyroutine.R
import com.example.mydailyroutine.core.designsystem.components.RoutineSheetScaffold
import com.example.mydailyroutine.core.designsystem.components.RoutineText
import com.example.mydailyroutine.core.designsystem.components.RoutineTimeField
import com.example.mydailyroutine.core.designsystem.components.RoutineTextDefaults
import com.example.mydailyroutine.core.designsystem.components.SettingRow
import com.example.mydailyroutine.core.designsystem.components.SheetPrimaryButton
import com.example.mydailyroutine.core.designsystem.components.SheetSecondaryButton
import com.example.mydailyroutine.core.designsystem.haptics.LocalRoutineHaptics
import com.example.mydailyroutine.core.designsystem.theme.RoutineColors
import com.example.mydailyroutine.core.designsystem.theme.RoutineMetrics
import com.example.mydailyroutine.core.designsystem.theme.RoutineShapes
import com.example.mydailyroutine.core.designsystem.theme.RoutineSpacing
import com.example.mydailyroutine.core.presentation.TimelineAction
import com.example.mydailyroutine.core.presentation.RoutineDate
import com.example.mydailyroutine.domain.model.ResolvedTimelineItem
import com.example.mydailyroutine.domain.model.ScheduleValidation
import com.example.mydailyroutine.domain.model.nominalMinutes
import com.example.mydailyroutine.domain.routines.TimeEntryState


/**
 * Edit one occurrence (or the whole weekly template) of a routine block.
 *
 * Rendered as a bottom sheet rather than a dialog: the five fields plus two scope toggles need the
 * full width, and the save/cancel pair lives in a sticky footer where its labels never wrap.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BlockEditorSheet(
    block: ResolvedTimelineItem.Block,
    busy: Boolean,
    onDismiss: () -> Unit,
    onSave: (TimelineAction.SaveBlockEdit) -> Unit,
) {
    var title by rememberSaveable(block.key) { mutableStateOf(block.title) }
    var times by rememberSaveable(block.key, stateSaver = TimeEntrySaver) {
        mutableStateOf(
            TimeEntryState.at(
                block.startsAt.toLocalTime(),
                nominalMinutes(block.startsAt.toLocalTime(), block.endsAt.toLocalTime()).coerceIn(1, 1439),
            ),
        )
    }
    val start = times.startText
    val end = times.endText
    var allDays by rememberSaveable(block.key) { mutableStateOf(true) }
    var whole by rememberSaveable(block.key) { mutableStateOf(false) }
    var error by rememberSaveable { mutableStateOf<Int?>(null) }
    val haptic = LocalRoutineHaptics.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoutineShapes.Sheet,
        containerColor = RoutineColors.SheetSurface,
        tonalElevation = 0.dp,
    ) {
        RoutineSheetScaffold(
            title = stringResource(R.string.edit_block_title),
            subtitle = stringResource(R.string.edit_occurrence, RoutineDate.spoken(block.occurrenceDate)),
            closeLabel = stringResource(R.string.close),
            onClose = onDismiss,
            modifier = Modifier.testTag("block-editor"),
            footer = {
                error?.let {
                    RoutineText(stringResource(it), style = MaterialTheme.typography.bodySmall,
                        color = RoutineColors.Warning, maxLines = RoutineTextDefaults.Paragraph)
                }
                SheetPrimaryButton(
                    label = stringResource(if (busy) R.string.saving else R.string.save_changes),
                    enabled = !busy,
                    onClick = {
                        val parsedStart = ScheduleValidation.parseTime(start)
                        val parsedEnd = ScheduleValidation.parseTime(end)
                        if (title.isBlank() || parsedStart == null || parsedEnd == null || parsedStart == parsedEnd) {
                            haptic.warning()
                            error = R.string.error_block_edit
                        } else {
                            onSave(
                                TimelineAction.SaveBlockEdit(
                                    block, title.trim(), parsedStart, parsedEnd, whole,
                                    allDays && block.seriesKey != null,
                                ),
                            )
                        }
                    },
                )
                SheetSecondaryButton(
                    label = stringResource(R.string.cancel),
                    enabled = !busy,
                    onClick = onDismiss,
                )
            },
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it.take(120); error = null },
                label = { RoutineText(stringResource(R.string.entry_title)) },
                singleLine = true,
                enabled = !busy,
                modifier = Modifier.fillMaxWidth(),
            )
            RoutineTimeField(
                value = start,
                onPick = { times = times.withStart(it); error = null },
                label = stringResource(R.string.entry_start),
                enabled = !busy,
                modifier = Modifier.fillMaxWidth(),
                wheelTag = "editor-start",
            )
            RoutineTimeField(
                value = end,
                onPick = { times = times.withEnd(it); error = null },
                label = stringResource(R.string.entry_end),
                enabled = !busy,
                modifier = Modifier.fillMaxWidth(),
                wheelTag = "editor-end",
            )
            RoutineText(stringResource(R.string.edit_times_hint), style = MaterialTheme.typography.bodySmall,
                color = RoutineColors.TextSecondary, maxLines = RoutineTextDefaults.Paragraph)
            if (!block.isOneOff) {
                SettingRow(
                    title = stringResource(R.string.edit_whole_template),
                    description = stringResource(if (whole) R.string.edit_whole_hint else R.string.edit_once_hint),
                    control = {
                        Checkbox(whole, { whole = it; haptic.tap() }, enabled = !busy,
                            modifier = Modifier.size(RoutineMetrics.ActionMinWidth))
                    },
                )
                if (whole && block.seriesKey != null && block.seriesDays.size > 1) {
                    SettingRow(
                        title = stringResource(R.string.edit_all_repeat_days),
                        control = {
                            Checkbox(allDays, { allDays = it; haptic.tap() }, enabled = !busy,
                                modifier = Modifier.size(RoutineMetrics.ActionMinWidth))
                        },
                    )
                }
            } else {
                RoutineText(stringResource(R.string.edit_once_hint), style = MaterialTheme.typography.bodySmall,
                    color = RoutineColors.TextSecondary, maxLines = RoutineTextDefaults.Paragraph)
            }
        }
    }
}
