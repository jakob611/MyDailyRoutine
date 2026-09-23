package com.example.mydailyroutine.features.subjects.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.mydailyroutine.R
import com.example.mydailyroutine.core.designsystem.components.RoutineLabel
import com.example.mydailyroutine.core.designsystem.components.RoutineText
import com.example.mydailyroutine.core.designsystem.components.RoutineTextDefaults
import com.example.mydailyroutine.core.designsystem.haptics.LocalRoutineHaptics
import com.example.mydailyroutine.core.designsystem.theme.RoutineColors
import com.example.mydailyroutine.core.designsystem.theme.RoutineMetrics
import com.example.mydailyroutine.core.designsystem.theme.RoutineSpacing
import com.example.mydailyroutine.domain.model.Subject
import com.example.mydailyroutine.domain.model.SubjectPalette

/**
 * One name per swatch, in the same order as [SubjectPalette.swatches]. The pairing is checked in CI
 * (tools/check_presentation.py), because a picker whose labels drift one place out of step with its
 * colours is a picker that lies about what the reader just chose.
 */
private val colorLabels = listOf(
    R.string.color_cyan, R.string.color_teal, R.string.color_emerald, R.string.color_green,
    R.string.color_lime, R.string.color_yellow, R.string.color_amber, R.string.color_orange,
    R.string.color_red, R.string.color_rose, R.string.color_pink, R.string.color_fuchsia,
    R.string.color_violet, R.string.color_indigo, R.string.color_blue, R.string.color_slate,
)

/** "#RRGGBB" of an ARGB long, for the custom-colour field. */
private fun hexOf(color: Long): String = "%06X".format(color and 0xFFFFFFL)

/**
 * Create or edit a subject. Deleting lives in the body as a full-width row, so the dialog buttons stay
 * a single unambiguous pair (save / cancel) instead of three labels fighting for one narrow slot.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SubjectEditorDialog(
    subject: Subject,
    busy: Boolean,
    onDismiss: () -> Unit,
    onSave: (Subject) -> Unit,
    onDelete: (() -> Unit)? = null,
    /** Every colour already in use, so a new subject arrives with one of its own instead of a twin. */
    takenColors: List<Long> = emptyList(),
) {
    var name by rememberSaveable(subject.id) { mutableStateOf(subject.name) }
    var duration by rememberSaveable(subject.id) { mutableStateOf(subject.defaultDurationMinutes.toString()) }
    var color by rememberSaveable(subject.id) {
        mutableLongStateOf(
            if (subject.id == 0L && subject.colorHex == SubjectPalette.Default && takenColors.isNotEmpty())
                SubjectPalette.firstFree(takenColors)
            else subject.colorHex,
        )
    }
    // The field is empty while a shelf colour is selected; typing in it takes over.
    var hex by rememberSaveable(subject.id) {
        mutableStateOf(if (subject.colorHex in SubjectPalette.swatches) "" else hexOf(subject.colorHex))
    }
    var confirmingDelete by rememberSaveable { mutableStateOf(false) }
    val haptics = LocalRoutineHaptics.current
    val validDuration = duration.toIntOrNull()?.takeIf { it in 1..1439 }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            RoutineText(
                text = stringResource(if (subject.id == 0L) R.string.new_subject_title else R.string.edit_subject_title),
                style = MaterialTheme.typography.headlineSmall,
                maxLines = RoutineTextDefaults.Body,
            )
        },
        text = {
            Column(
                Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(RoutineSpacing.md),
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.take(120) },
                    label = { RoutineText(stringResource(R.string.subject_name)) },
                    singleLine = true,
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = duration,
                    onValueChange = { duration = it.filter(Char::isDigit).take(4) },
                    label = { RoutineText(stringResource(R.string.subject_duration)) },
                    supportingText = {
                        RoutineLabel(stringResource(R.string.subject_duration_hint),
                            style = MaterialTheme.typography.labelSmall)
                    },
                    singleLine = true,
                    enabled = !busy,
                    isError = validDuration == null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
                RoutineText(stringResource(R.string.subject_color), style = MaterialTheme.typography.titleSmall,
                    maxLines = RoutineTextDefaults.Body)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(RoutineSpacing.sm),
                    verticalArrangement = Arrangement.spacedBy(RoutineSpacing.sm),
                ) {
                    RoutineColors.subjectSwatches.forEachIndexed { index, value ->
                        val label = stringResource(colorLabels[index])
                        val description = if (color == value) stringResource(R.string.selected_color, label) else label
                        Box(
                            Modifier.size(RoutineMetrics.SwatchSize)
                                .border(
                                    BorderStroke(if (color == value) 3.dp else 1.dp,
                                        if (color == value) RoutineColors.TextPrimary else RoutineColors.Border),
                                    CircleShape,
                                )
                                .padding(5.dp)
                                .background(Color(value.toInt()), CircleShape)
                                .clickable(enabled = !busy) {
                                    color = value
                                    hex = ""
                                    haptics.selection()
                                }
                                .semantics { contentDescription = description },
                        )
                    }
                }
                // Anything the shelf does not carry: the school's own colour, a subject that must
                // match a printed timetable, or simply a preference. Hex, because a wheel that is
                // easy to nudge and hard to land on is worse than typing six characters.
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(RoutineSpacing.sm),
                ) {
                    val custom = color !in RoutineColors.subjectSwatches
                    Box(
                        Modifier.size(RoutineMetrics.SwatchSmall)
                            .border(
                                BorderStroke(if (custom) 3.dp else 1.dp,
                                    if (custom) RoutineColors.TextPrimary else RoutineColors.Border),
                                CircleShape,
                            )
                            .padding(3.dp)
                            .background(Color(color.toInt()), CircleShape),
                    )
                    OutlinedTextField(
                        value = hex,
                        onValueChange = { raw ->
                            val cleaned = raw.filter { it.isLetterOrDigit() }.take(6).uppercase()
                            hex = cleaned
                            if (cleaned.length == 6) cleaned.toLongOrNull(16)?.let { color = 0xFF000000L or it }
                        },
                        label = { RoutineText(stringResource(R.string.subject_color_custom)) },
                        supportingText = {
                            RoutineLabel(stringResource(R.string.subject_color_custom_hint),
                                style = MaterialTheme.typography.labelSmall)
                        },
                        singleLine = true,
                        enabled = !busy,
                        isError = hex.isNotEmpty() && hex.length != 6,
                        // Hex only: a suggestion bar inserting a space in the middle of a colour would be a bug.
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
                        modifier = Modifier.weight(1f),
                    )
                }
                if (subject.id > 0L && onDelete != null) {
                    TextButton(
                        enabled = !busy,
                        onClick = { haptics.warning(); confirmingDelete = true },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        RoutineLabel(stringResource(R.string.delete_subject),
                            style = MaterialTheme.typography.labelLarge, color = RoutineColors.Error)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = !busy && name.isNotBlank() && validDuration != null,
                onClick = {
                    validDuration?.let {
                        onSave(subject.copy(name = name.trim(), colorHex = color, defaultDurationMinutes = it))
                    }
                },
            ) {
                RoutineLabel(stringResource(if (busy) R.string.saving else R.string.save_subject),
                    style = MaterialTheme.typography.labelLarge)
            }
        },
        dismissButton = {
            TextButton(enabled = !busy, onClick = onDismiss) {
                RoutineLabel(stringResource(R.string.cancel), style = MaterialTheme.typography.labelLarge)
            }
        },
    )
    if (confirmingDelete) {
        AlertDialog(
            onDismissRequest = { confirmingDelete = false },
            title = {
                RoutineText(stringResource(R.string.delete_subject_title, subject.name),
                    style = MaterialTheme.typography.headlineSmall, maxLines = RoutineTextDefaults.Body)
            },
            text = {
                RoutineText(stringResource(R.string.delete_subject_body), maxLines = RoutineTextDefaults.Paragraph)
            },
            confirmButton = {
                TextButton(enabled = !busy, onClick = { confirmingDelete = false; onDelete?.invoke() }) {
                    RoutineLabel(stringResource(R.string.delete_subject), style = MaterialTheme.typography.labelLarge,
                        color = RoutineColors.Error)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmingDelete = false }) {
                    RoutineLabel(stringResource(R.string.cancel), style = MaterialTheme.typography.labelLarge)
                }
            },
        )
    }
}
