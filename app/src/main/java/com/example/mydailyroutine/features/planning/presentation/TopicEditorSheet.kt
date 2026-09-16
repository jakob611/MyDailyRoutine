package com.example.mydailyroutine.features.planning.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.mydailyroutine.R
import com.example.mydailyroutine.core.designsystem.components.RoutineSheetScaffold
import com.example.mydailyroutine.core.designsystem.components.RoutineText
import com.example.mydailyroutine.core.designsystem.components.RoutineTextDefaults
import com.example.mydailyroutine.core.designsystem.components.SheetPrimaryButton
import com.example.mydailyroutine.core.designsystem.haptics.LocalRoutineHaptics
import com.example.mydailyroutine.core.designsystem.theme.RoutineColors
import com.example.mydailyroutine.core.designsystem.theme.RoutineShapes
import com.example.mydailyroutine.core.designsystem.theme.RoutineSpacing
import com.example.mydailyroutine.core.presentation.TimelineAction
import com.example.mydailyroutine.core.presentation.TimelineUiState
import com.example.mydailyroutine.domain.learning.SpacedRepetitionPlanner
import com.example.mydailyroutine.domain.learning.StudyTopic
import com.example.mydailyroutine.domain.model.ScheduleValidation
import com.example.mydailyroutine.features.entry.presentation.AppDatePicker
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * New study topic: spaced-repetition window, review count and duration.
 *
 * Subject and milestone choices are flow rows of chips — everything is visible at once, nothing hides
 * behind a horizontal swipe — and both dates also open a picker.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TopicEditorSheet(state: TimelineUiState, onAction: (TimelineAction) -> Unit, sheetState: SheetState) {
    val context = LocalContext.current
    val haptics = LocalRoutineHaptics.current
    val today = maxOf(LocalDate.now(), state.content.date)
    var title by rememberSaveable { mutableStateOf("") }
    var initialText by rememberSaveable { mutableStateOf(today.toString()) }
    var finalText by rememberSaveable { mutableStateOf(today.plusDays(30).toString()) }
    var count by rememberSaveable { mutableStateOf(SpacedRepetitionPlanner.suggestedCount(30).toString()) }
    var manualCount by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(initialText, finalText) {
        if (!manualCount) {
            val from = ScheduleValidation.parseDate(initialText)
            val until = ScheduleValidation.parseDate(finalText)
            if (from != null && until != null && until >= from) {
                count = SpacedRepetitionPlanner.suggestedCount(ChronoUnit.DAYS.between(from, until)).toString()
            }
        }
    }
    var duration by rememberSaveable { mutableStateOf("45") }
    var subject by rememberSaveable { mutableStateOf<Long?>(null) }
    var milestone by rememberSaveable { mutableStateOf<Long?>(null) }
    var error by rememberSaveable { mutableStateOf(false) }
    var pickingInitial by rememberSaveable { mutableStateOf(false) }
    var pickingFinal by rememberSaveable { mutableStateOf(false) }
    val busy = state.panels.isSaving
    val milestones = state.planning.milestones.filter { it.dueDate >= today }
    ModalBottomSheet(
        onDismissRequest = { onAction(TimelineAction.CloseTopic) },
        shape = RoutineShapes.Sheet,
        containerColor = RoutineColors.Surface1,
        tonalElevation = 0.dp,
        sheetState = sheetState,
    ) {
        RoutineSheetScaffold(
            title = stringResource(R.string.new_topic),
            subtitle = stringResource(R.string.topic_hint),
            closeLabel = stringResource(R.string.close),
            onClose = { onAction(TimelineAction.CloseTopic) },
            modifier = Modifier.testTag("topic-editor"),
            footer = {
                if (error) {
                    RoutineText(stringResource(R.string.topic_invalid), style = MaterialTheme.typography.bodySmall,
                        color = RoutineColors.Warning, maxLines = RoutineTextDefaults.Paragraph)
                }
                SheetPrimaryButton(
                    label = stringResource(if (busy) R.string.saving else R.string.topic_save),
                    enabled = !busy,
                    onClick = {
                        val from = ScheduleValidation.parseDate(initialText)
                        val to = ScheduleValidation.parseDate(finalText)
                        val repetitions = count.toIntOrNull()
                        val size = duration.toIntOrNull()
                        error = title.isBlank() || from == null || to == null || from < LocalDate.now() || to < from ||
                            ChronoUnit.DAYS.between(from, to) > 366 || repetitions == null || repetitions !in 1..52 ||
                            size == null || size !in 1..120
                        if (error) {
                            haptics.warning()
                        } else {
                            onAction(
                                TimelineAction.SaveTopic(
                                    StudyTopic(
                                        title = title.trim(),
                                        subjectId = subject,
                                        initialDate = from!!,
                                        finalDate = to!!,
                                        reviewCount = repetitions!!,
                                        reviewDurationMinutes = size!!,
                                        milestoneId = milestone,
                                    ),
                                    context.getString(R.string.review_title_pattern),
                                ),
                            )
                        }
                    },
                )
            },
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it.take(120); error = false },
                label = { RoutineText(stringResource(R.string.topic_title)) },
                enabled = !busy,
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            if (state.content.subjects.isNotEmpty()) {
                RoutineText(stringResource(R.string.saved_subjects), style = MaterialTheme.typography.titleSmall,
                    maxLines = RoutineTextDefaults.Body)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(RoutineSpacing.sm),
                    verticalArrangement = Arrangement.spacedBy(RoutineSpacing.xs),
                ) {
                    state.content.subjects.forEach { item ->
                        FilterChip(
                            selected = subject == item.id,
                            onClick = { subject = item.id; haptics.tap() },
                            enabled = !busy,
                            shape = RoutineShapes.Chip,
                            label = { RoutineText(item.name, maxLines = 1, softWrap = false) },
                        )
                    }
                }
            }
            if (milestones.isNotEmpty()) {
                RoutineText(stringResource(R.string.topic_milestone), style = MaterialTheme.typography.titleSmall,
                    maxLines = RoutineTextDefaults.Body)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(RoutineSpacing.sm),
                    verticalArrangement = Arrangement.spacedBy(RoutineSpacing.xs),
                ) {
                    milestones.forEach { item ->
                        FilterChip(
                            selected = milestone == item.id,
                            onClick = {
                                haptics.tap()
                                milestone = item.id
                                subject = item.subjectId
                                finalText = item.dueDate.minusDays(if (item.isExam || item.isTerminalExam) 1 else 0).toString()
                            },
                            enabled = !busy,
                            shape = RoutineShapes.Chip,
                            label = { RoutineText(item.title, maxLines = 1, softWrap = false) },
                        )
                    }
                }
            }
            OutlinedTextField(
                value = initialText,
                onValueChange = { initialText = it; error = false },
                label = { RoutineText(stringResource(R.string.topic_initial)) },
                enabled = !busy,
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                trailingIcon = {
                    IconButton(enabled = !busy, onClick = { pickingInitial = true }) {
                        Icon(Icons.Outlined.CalendarMonth, stringResource(R.string.choose_date))
                    }
                },
            )
            OutlinedTextField(
                value = finalText,
                onValueChange = { finalText = it; error = false },
                label = { RoutineText(stringResource(R.string.topic_final)) },
                enabled = !busy,
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                trailingIcon = {
                    IconButton(enabled = !busy, onClick = { pickingFinal = true }) {
                        Icon(Icons.Outlined.CalendarMonth, stringResource(R.string.choose_date))
                    }
                },
            )
            Row(horizontalArrangement = Arrangement.spacedBy(RoutineSpacing.sm)) {
                OutlinedTextField(
                    value = count,
                    onValueChange = { count = it.filter(Char::isDigit).take(2); manualCount = true; error = false },
                    label = { RoutineText(stringResource(R.string.topic_count)) },
                    enabled = !busy,
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
                OutlinedTextField(
                    value = duration,
                    onValueChange = { duration = it.filter(Char::isDigit).take(3); error = false },
                    label = { RoutineText(stringResource(R.string.topic_duration)) },
                    enabled = !busy,
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
            }
        }
    }
    if (pickingInitial) {
        AppDatePicker(
            ScheduleValidation.parseDate(initialText) ?: today,
            onDismiss = { pickingInitial = false },
            onDate = { initialText = it.toString(); pickingInitial = false },
        )
    }
    if (pickingFinal) {
        AppDatePicker(
            ScheduleValidation.parseDate(finalText) ?: today.plusDays(30),
            onDismiss = { pickingFinal = false },
            onDate = { finalText = it.toString(); pickingFinal = false },
        )
    }
}
