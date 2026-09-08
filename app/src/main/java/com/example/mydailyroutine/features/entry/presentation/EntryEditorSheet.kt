package com.example.mydailyroutine.features.entry.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Close
import com.example.mydailyroutine.core.designsystem.components.categoryIcon
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.mydailyroutine.R
import com.example.mydailyroutine.domain.model.ResolvedTimelineItem
import com.example.mydailyroutine.domain.model.RoutineCategory
import com.example.mydailyroutine.domain.model.ScheduleValidation
import com.example.mydailyroutine.domain.model.Subject
import com.example.mydailyroutine.domain.presets.*
import com.example.mydailyroutine.domain.routines.*
import com.example.mydailyroutine.features.routines.presentation.WeekdayPicker
import com.example.mydailyroutine.domain.learning.HistoricalVelocity
import com.example.mydailyroutine.domain.learning.VelocityCalibrator
import com.example.mydailyroutine.domain.model.nominalMinutes
import com.example.mydailyroutine.core.designsystem.haptics.LocalRoutineHaptics
import com.example.mydailyroutine.core.designsystem.theme.*
import com.example.mydailyroutine.core.presentation.*
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EntryEditorSheet(
    selectedDate: LocalDate, subjects: List<Subject>, subjectPresets: List<QuickAddPreset>, history: List<HistoricalVelocity>,
    editing: ResolvedTimelineItem.Milestone?, busy: Boolean,
    onDismiss: () -> Unit, onSave: (EntryDraft) -> Unit, onNewSubject: () -> Unit,
    defaults: EntryDefaults = EntryDefaults(), continuation: EntryContinuation? = null,
) {
    val initial = remember(selectedDate, editing?.key, continuation?.start) {
        val now = LocalDateTime.now()
        if (continuation != null) continuation.start
        else if (editing != null) editing.date.atTime(editing.dueTime ?: LocalTime.of(16, 0))
        else if (selectedDate == now.toLocalDate()) now.plusMinutes((15 - now.minute % 15).toLong()).withSecond(0).withNano(0)
        else selectedDate.atTime(16, 0)
    }
    var title by rememberSaveable(editing?.key) { mutableStateOf(editing?.title ?: "") }
    var dateText by rememberSaveable(editing?.key) { mutableStateOf(initial.toLocalDate().toString()) }
    var times by rememberSaveable(editing?.key, stateSaver = TimeEntrySaver) {
        mutableStateOf(TimeEntryState.at(initial.toLocalTime(), continuation?.durationMinutes ?: 90))
    }
    val startText = times.startText
    val endText = times.endText
    var category by rememberSaveable { mutableStateOf(if (continuation != null) RoutineCategory.SCHOOL else RoutineCategory.FOCUS_ANALYTICAL) }
    var kind by rememberSaveable(editing?.key) { mutableStateOf(if (editing == null) EntryKind.BLOCK else if (editing.isExam) EntryKind.EXAM else EntryKind.DEADLINE) }
    var subjectId by rememberSaveable(editing?.key) { mutableStateOf(editing?.subject?.id) }
    var weekly by rememberSaveable { mutableStateOf(continuation?.weekly ?: false) }
    var repeatDays by rememberSaveable { mutableIntStateOf(continuation?.weekdaysMask ?: Weekdays.mask(setOf(initial.dayOfWeek))) }
    var daysTouched by rememberSaveable { mutableStateOf(continuation != null) }
    var includeBreak by rememberSaveable { mutableStateOf((continuation?.breakMinutes ?: 0) > 0) }
    var breakMinutes by rememberSaveable { mutableStateOf((continuation?.breakMinutes?.takeIf { it > 0 } ?: defaults.lessonBreakMinutes).toString()) }
    var notifications by rememberSaveable { mutableStateOf(true) }
    var allDay by rememberSaveable(editing?.key) { mutableStateOf(editing != null && editing.dueTime == null) }
    var error by rememberSaveable { mutableStateOf<Int?>(null) }
    var pickingDate by rememberSaveable { mutableStateOf(false) }
    var advanced by rememberSaveable { mutableStateOf(false) }
    var minimum by rememberSaveable { mutableStateOf("") }
    var elasticity by rememberSaveable { mutableStateOf("1.0") }
    var priority by rememberSaveable { mutableStateOf("3.0") }
    var fixed by rememberSaveable { mutableStateOf(false) }
    var calibrate by rememberSaveable { mutableStateOf(true) }
    var effort by rememberSaveable(editing?.key) { mutableStateOf(editing?.estimatedEffortHours?.takeIf { it > 0 }?.toString().orEmpty()) }
    var terminal by rememberSaveable(editing?.key) { mutableStateOf(editing?.isTerminalExam ?: false) }
    val velocity = remember(history) { VelocityCalibrator(history) }
    val haptics = LocalRoutineHaptics.current
    val context = LocalContext.current
    val standardPresets = remember { PresetFactory.standard() }

    fun applyPreset(preset: QuickAddPreset) {
        haptics.tap()
        title = preset.title(context)
        kind = if (preset.isExam) EntryKind.EXAM else EntryKind.BLOCK
        category = preset.category
        subjectId = preset.subjectId ?: subjectId.takeUnless { preset.kind in setOf(PresetKind.WALK, PresetKind.LUNCH, PresetKind.SNACK, PresetKind.RESERVE) }
        val start = ScheduleValidation.parseTime(startText) ?: initial.toLocalTime()
        times = TimeEntryState.at(start,preset.durationMinutes)
        allDay = false
        minimum = ""; elasticity = "1.0"; priority = if (preset.category.isBuffer) "1.0" else "3.0"; fixed = false
        if (preset.kind == PresetKind.LUNCH || preset.kind == PresetKind.SNACK) {
            fixed = true; weekly = true; repeatDays = Weekdays.WORKDAYS; daysTouched = true
        }
        error = null
    }
    fun save(preset: QuickAddPreset? = null, keepOpen: Boolean = false) {
        val chosenKind = if (preset?.isExam == true) EntryKind.EXAM else kind
        val chosenTitle = preset?.title(context) ?: title.trim()
        val date = ScheduleValidation.parseDate(dateText)
        val start = if (chosenKind != EntryKind.BLOCK && allDay) null else ScheduleValidation.parseTime(startText)
        val end = if (chosenKind == EntryKind.BLOCK) ScheduleValidation.parseTime(endText) else null
        val afterBreak = if (chosenKind == EntryKind.BLOCK && category == RoutineCategory.SCHOOL && includeBreak) breakMinutes.toIntOrNull() else 0
        val minimumValue = if (minimum.isBlank()) null else minimum.toIntOrNull()
        val elasticityValue = elasticity.replace(',', '.').toDoubleOrNull()
        val priorityValue = priority.replace(',', '.').toDoubleOrNull()
        val effortValue = if (effort.isBlank()) 0.0 else effort.replace(',', '.').toDoubleOrNull()
        val raw = if (start != null && end != null) nominalMinutes(start, end) else 0
        error = when {
            chosenTitle.isBlank() -> R.string.error_title
            date == null -> R.string.error_date
            (chosenKind == EntryKind.BLOCK || !allDay) && start == null -> R.string.error_time
            chosenKind == EntryKind.BLOCK && (end == null || end == start) -> R.string.error_time_range
            chosenKind == EntryKind.BLOCK && weekly && repeatDays == 0 -> R.string.repeat_days_required
            afterBreak == null || afterBreak !in 0..60 || (includeBreak && category == RoutineCategory.SCHOOL && chosenKind == EntryKind.BLOCK && afterBreak == 0) -> R.string.lesson_break_invalid
            chosenKind == EntryKind.BLOCK && ((minimum.isNotBlank() && (minimumValue == null || minimumValue !in (if (category.isBuffer) 0 else 1)..raw)) ||
                elasticityValue == null || !elasticityValue.isFinite() || elasticityValue !in 0.0..1000000.0 ||
                priorityValue == null || !priorityValue.isFinite() || priorityValue <= 0 || priorityValue > 1000000) -> R.string.elastic_invalid
            chosenKind != EntryKind.BLOCK && (effortValue == null || !effortValue.isFinite() || effortValue !in 0.0..1000.0) -> R.string.effort_invalid
            else -> null
        }
        if (error == null && date != null) {
            val chosenSubject = preset?.subjectId ?: subjectId
            onSave(EntryDraft(chosenTitle, chosenSubject?.takeIf { id -> subjects.any { it.id == id } }, chosenKind, date, start, end,
                preset?.category ?: category, weekly, notifications, editing?.milestoneId ?: 0, editing?.isCompleted ?: false,
                minimumValue, elasticityValue ?: 1.0, priorityValue ?: 3.0, fixed, calibrate, effortValue ?: 0.0, terminal && chosenKind == EntryKind.EXAM,
                repeatDays, afterBreak ?: 0, context.getString(R.string.lesson_break_title), keepOpen))
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        shape = RoutineShapes.Sheet, containerColor = RoutineColors.Surface1, tonalElevation = 0.dp) {
        Column(Modifier.fillMaxWidth().fillMaxHeight(0.92f).imePadding().testTag("entry-editor")) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(if (editing == null) R.string.fast_add_title else R.string.entry_edit_milestone), Modifier.weight(1f), style = MaterialTheme.typography.headlineSmall)
                IconButton(onClick = onDismiss, enabled = !busy) { Icon(Icons.Outlined.Close, stringResource(R.string.close)) }
            }
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 24.dp).padding(bottom = 12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (editing == null) LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(standardPresets, key = { it.key }) { preset ->
                        SuggestionChip(onClick = { applyPreset(preset) }, enabled = !busy, shape = RoutineShapes.Chip, label = { Text(preset.label(context)) })
                    }
                }
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    val choices = if (editing == null) EntryKind.entries else listOf(EntryKind.DEADLINE, EntryKind.EXAM)
                    choices.forEachIndexed { index, choice ->
                        SegmentedButton(selected = kind == choice, enabled = !busy, onClick = { haptics.tap(); kind = choice; error = null }, shape = SegmentedButtonDefaults.itemShape(index, choices.size)) {
                            Text(stringResource(when (choice) { EntryKind.BLOCK -> R.string.entry_block; EntryKind.DEADLINE -> R.string.entry_deadline; EntryKind.EXAM -> R.string.entry_exam }))
                        }
                    }
                }
                Text(stringResource(R.string.saved_subjects), style = MaterialTheme.typography.titleSmall)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item { FilterChip(subjectId == null, onClick = { subjectId = null; haptics.tap() }, enabled = !busy, label = { Text(stringResource(R.string.subject_all)) }, shape = RoutineShapes.Chip) }
                    items(subjects, key = { it.id }) { subject ->
                        FilterChip(selected = subjectId == subject.id, enabled = !busy, shape = RoutineShapes.Chip,
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Color(subject.colorHex.toInt()).copy(alpha = 0.2f)),
                            onClick = {
                                subjectId = subject.id
                                if (editing != null || kind == EntryKind.DEADLINE) {
                                    // Associating a subject must not turn an EE/IA deadline into an exam,
                                    // or overwrite a title/date/time in an existing milestone editor.
                                    haptics.tap()
                                } else {
                                    val desired = if (kind == EntryKind.BLOCK) PresetKind.SUBJECT_LESSON else PresetKind.SUBJECT_TEST
                                    subjectPresets.firstOrNull { it.subjectId == subject.id && it.kind == desired }?.let(::applyPreset)
                                }
                            }, label = { Text(subject.name) })
                    }
                    item { SuggestionChip(onClick = onNewSubject, enabled = !busy, label = { Text(stringResource(R.string.new_subject)) }, shape = RoutineShapes.Chip) }
                }
                if (subjects.isEmpty()) Text(stringResource(R.string.no_subjects_hint), style = MaterialTheme.typography.bodySmall)
                else {
                    Text(stringResource(R.string.subject_presets), style = MaterialTheme.typography.titleSmall)
                    val visible = subjectPresets.filter { (subjectId == null || it.subjectId == subjectId) && (editing == null || it.isExam) }
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(visible, key = { it.key }) { preset ->
                            val color = preset.colorHex?.let { Color(it.toInt()) } ?: RoutineColors.Cobalt
                            SuggestionChip(onClick = { applyPreset(preset) }, enabled = !busy, shape = RoutineShapes.Chip,
                                border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.5f)), label = { Text(preset.label(context), color = color) })
                        }
                    }
                }
                OutlinedTextField(title, { title = it.take(120); error = null }, label = { Text(stringResource(R.string.entry_title)) }, singleLine = true, modifier = Modifier.fillMaxWidth(), enabled = !busy)
                OutlinedTextField(dateText, { dateText = it; error = null
                    if (!daysTouched) ScheduleValidation.parseDate(it)?.let { date -> repeatDays = Weekdays.mask(setOf(date.dayOfWeek)) }
                }, label = { Text(stringResource(R.string.entry_date)) }, singleLine = true, modifier = Modifier.fillMaxWidth(), enabled = !busy,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii), trailingIcon = {
                        IconButton(onClick = { pickingDate = true; haptics.tap() }, enabled = !busy) { Icon(Icons.Outlined.CalendarMonth, stringResource(R.string.choose_date)) }
                    })
                if (kind != EntryKind.BLOCK) Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(allDay, { allDay = it; haptics.tap() }, enabled = !busy)
                    Text(stringResource(R.string.entry_all_day))
                }
                AnimatedVisibility(kind == EntryKind.BLOCK || !allDay) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(startText, { times = times.withStart(it); error = null }, label = { Text(stringResource(if (kind == EntryKind.BLOCK) R.string.entry_start else R.string.entry_due)) },
                            singleLine = true, modifier = Modifier.weight(1f).testTag("entry-start"), enabled = !busy, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii))
                        if (kind == EntryKind.BLOCK) OutlinedTextField(endText, { times = times.withEnd(it); error = null }, label = { Text(stringResource(R.string.entry_end)) },
                            singleLine = true, modifier = Modifier.weight(1f).testTag("entry-end"), enabled = !busy, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii))
                    }
                }
                if (kind == EntryKind.BLOCK) {
                    Text(stringResource(R.string.duration_follows_start, times.durationMinutes), style = MaterialTheme.typography.bodySmall, color = RoutineColors.TextSecondary)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(listOf(30,45,60,90,times.durationMinutes).distinct().sorted(), key={it}) { minutes ->
                            FilterChip(selected=times.durationMinutes==minutes, onClick={ times=times.withDuration(minutes); haptics.tap(); error=null },
                                enabled=!busy, label={ Text(stringResource(R.string.duration_minutes,minutes)) }, shape=RoutineShapes.Chip)
                        }
                    }
                    Text(stringResource(R.string.entry_overnight_hint), style = MaterialTheme.typography.bodySmall)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(RoutineCategory.entries, key = { it.name }) { option -> FilterChip(category == option, {
                            category = option; haptics.tap()
                            if (option == RoutineCategory.SCHOOL) times = times.withDuration(subjects.firstOrNull { it.id == subjectId }?.defaultDurationMinutes ?: defaults.lessonDurationMinutes)
                        }, enabled = !busy,
                            label = { Text(option.label()) }, leadingIcon = { Icon(categoryIcon(option), null, Modifier.size(16.dp)) }, shape = RoutineShapes.Chip) }
                    }
                    if (category == RoutineCategory.SCHOOL) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(includeBreak,{ includeBreak=it;haptics.tap() },enabled=!busy,modifier=Modifier.testTag("lesson-break-toggle"))
                            Text(stringResource(R.string.lesson_break_option),Modifier.weight(1f))
                            if (includeBreak) OutlinedTextField(breakMinutes,{ breakMinutes=it.filter(Char::isDigit).take(2) },
                                label={ Text(stringResource(R.string.minutes_short)) },singleLine=true,enabled=!busy,modifier=Modifier.width(82.dp),keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number))
                        }
                        if (includeBreak) Text(stringResource(R.string.lesson_break_hint),style=MaterialTheme.typography.bodySmall,color=RoutineColors.TextSecondary)
                    }
                    TextButton(onClick = { advanced = !advanced; haptics.tap() }) {
                        Icon(Icons.Outlined.ExpandMore, null)
                        Text(stringResource(R.string.entry_more_options))
                    }
                    AnimatedVisibility(advanced) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(stringResource(R.string.elastic_settings), style = MaterialTheme.typography.titleSmall)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(fixed || category == RoutineCategory.SCHOOL, { fixed = it }, enabled = !busy && category != RoutineCategory.SCHOOL)
                                Text(stringResource(R.string.fixed_commitment))
                            }
                            Text(stringResource(R.string.fixed_hint), style = MaterialTheme.typography.bodySmall)
                            if (!fixed && category != RoutineCategory.SCHOOL) {
                                OutlinedTextField(minimum, { minimum = it.filter(Char::isDigit).take(4) }, label = { Text(stringResource(R.string.minimum_duration)) }, singleLine = true, enabled = !busy)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(elasticity, { elasticity = it }, label = { Text(stringResource(R.string.elasticity)) }, modifier = Modifier.weight(1f), singleLine = true, enabled = !busy)
                                    OutlinedTextField(priority, { priority = it }, label = { Text(stringResource(R.string.priority_weight)) }, modifier = Modifier.weight(1f), singleLine = true, enabled = !busy)
                                }
                                Text(stringResource(R.string.elastic_hint), style = MaterialTheme.typography.bodySmall)
                            }
                            if (category.isDeepWork && subjectId != null && !fixed) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Checkbox(calibrate, { calibrate = it }, enabled = !busy)
                                    Text(stringResource(R.string.calibrate_duration))
                                }
                                val from = ScheduleValidation.parseTime(startText)
                                val until = ScheduleValidation.parseTime(endText)
                                if (from != null && until != null && from != until) {
                                    val raw = nominalMinutes(from, until)
                                    Text(stringResource(R.string.velocity_preview, raw, if (calibrate) velocity.getCalibratedDuration(raw, subjectId.toString()) else raw), style = MaterialTheme.typography.bodySmall, color = RoutineColors.Sage)
                                }
                            }
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(stringResource(R.string.entry_repeat), style = MaterialTheme.typography.titleSmall)
                            Text(stringResource(if (weekly) R.string.entry_repeat_hint else R.string.entry_once_hint), style = MaterialTheme.typography.bodySmall)
                        }
                        Switch(weekly, { weekly = it; haptics.tap() }, enabled = !busy, modifier = Modifier.testTag("repeat-weekly"))
                    }
                    if (weekly) {
                        Text(stringResource(R.string.repeat_days_label),style=MaterialTheme.typography.titleSmall)
                        WeekdayPicker(repeatDays,!busy) { repeatDays=it;daysTouched=true }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(stringResource(R.string.entry_reminder), style = MaterialTheme.typography.titleSmall)
                            Text(stringResource(if (category == RoutineCategory.REST_BUFFER) R.string.reminder_at_recovery else R.string.reminder_before), style = MaterialTheme.typography.bodySmall)
                        }
                        Switch(notifications, { notifications = it; haptics.tap() }, enabled = !busy)
                    }
                } else {
                    Text(stringResource(R.string.marker_hint), style = MaterialTheme.typography.bodySmall)
                    OutlinedTextField(effort, { effort = it }, label = { Text(stringResource(R.string.milestone_effort)) }, singleLine = true, enabled = !busy)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(terminal, { terminal = it; if (it) kind = EntryKind.EXAM }, enabled = !busy)
                        Text(stringResource(R.string.terminal_exam))
                    }
                    Text(stringResource(R.string.effort_hint), style = MaterialTheme.typography.bodySmall)
                    if (editing == null && subjects.isNotEmpty()) {
                        Text(stringResource(R.string.scheduled_test_hint), style = MaterialTheme.typography.bodySmall)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(subjectPresets.filter { it.kind == PresetKind.SUBJECT_TEST }, key = { it.key }) { test ->
                                OutlinedButton(enabled = !busy, shape = RoutineShapes.Pill, onClick = { save(test) }) {
                                    Text(stringResource(R.string.scheduled_test, test.subjectName.orEmpty()), color = RoutineColors.Exam.content)
                                }
                            }
                        }
                    }
                }
            }
            error?.let { Text(stringResource(it), Modifier.padding(horizontal = 24.dp), style = MaterialTheme.typography.bodySmall, color = RoutineColors.Warning) }
            HorizontalDivider(color = RoutineColors.Border)
            if (editing == null && kind == EntryKind.BLOCK && category == RoutineCategory.SCHOOL) TextButton(enabled=!busy,
                modifier=Modifier.fillMaxWidth().testTag("save-next-lesson"),onClick={ save(keepOpen=true) }) { Text(stringResource(R.string.save_next_lesson)) }
            Button(enabled = !busy, shape = RoutineShapes.Pill, modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp).heightIn(min = 52.dp), onClick = { save() }) {
                Text(stringResource(if (busy) R.string.saving else if (editing == null) R.string.entry_save else R.string.entry_save_milestone))
            }
        }
    }
    if (pickingDate) AppDatePicker(ScheduleValidation.parseDate(dateText) ?: selectedDate,
        onDismiss = { pickingDate = false }, onDate = { dateText = it.toString(); if (!daysTouched) repeatDays=Weekdays.mask(setOf(it.dayOfWeek)); pickingDate = false })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppDatePicker(date: LocalDate, onDismiss: () -> Unit, onDate: (LocalDate) -> Unit) {
    val state = rememberDatePickerState(initialSelectedDateMillis = date.toEpochDay() * 86_400_000L)
    DatePickerDialog(onDismissRequest = onDismiss,
        confirmButton = { TextButton(enabled = state.selectedDateMillis != null, onClick = {
            state.selectedDateMillis?.let { onDate(LocalDate.ofEpochDay(Math.floorDiv(it, 86_400_000L))) }
        }) { Text(stringResource(R.string.choose)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    ) { DatePicker(state) }
}
