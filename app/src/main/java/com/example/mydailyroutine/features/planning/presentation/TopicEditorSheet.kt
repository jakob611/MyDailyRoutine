package com.example.mydailyroutine.features.planning.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.mydailyroutine.R
import com.example.mydailyroutine.domain.learning.StudyTopic
import com.example.mydailyroutine.domain.learning.SpacedRepetitionPlanner
import com.example.mydailyroutine.domain.model.*
import com.example.mydailyroutine.features.entry.presentation.AppDatePicker
import com.example.mydailyroutine.core.designsystem.theme.*
import com.example.mydailyroutine.core.presentation.*
import java.time.Duration
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopicEditorSheet(state: TimelineUiState, onAction: (TimelineAction) -> Unit) {
    val context = LocalContext.current
    val today = maxOf(LocalDate.now(), state.content.date)
    var title by rememberSaveable { mutableStateOf("") }
    var initial by rememberSaveable { mutableStateOf(today.toString()) }
    var final by rememberSaveable { mutableStateOf(today.plusDays(30).toString()) }
    var count by rememberSaveable { mutableStateOf(SpacedRepetitionPlanner.suggestedCount(30).toString()) }
    var manualCount by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(initial, final) {
        if (!manualCount) {
            val from = ScheduleValidation.parseDate(initial); val until = ScheduleValidation.parseDate(final)
            if (from != null && until != null && until >= from) count = SpacedRepetitionPlanner.suggestedCount(java.time.temporal.ChronoUnit.DAYS.between(from,until)).toString()
        }
    }
    var duration by rememberSaveable { mutableStateOf("45") }
    var subject by rememberSaveable { mutableStateOf<Long?>(null) }
    var milestone by rememberSaveable { mutableStateOf<Long?>(null) }
    var error by rememberSaveable { mutableStateOf(false) }
    val busy = state.panels.isSaving
    ModalBottomSheet(onDismissRequest = { onAction(TimelineAction.CloseTopic) }, shape = RoutineShapes.Sheet, containerColor = RoutineColors.Surface1,
        tonalElevation = 0.dp, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.fillMaxWidth().imePadding().verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.new_topic), style = MaterialTheme.typography.headlineSmall)
            Text(stringResource(R.string.topic_hint), style = MaterialTheme.typography.bodySmall)
            OutlinedTextField(title, { title = it.take(120) }, label = { Text(stringResource(R.string.topic_title)) }, enabled = !busy, modifier = Modifier.fillMaxWidth())
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { items(state.content.subjects, key = { it.id }) { item ->
                FilterChip(subject == item.id, { subject = item.id }, enabled = !busy, label = { Text(item.name) })
            } }
            Text(stringResource(R.string.topic_milestone), style = MaterialTheme.typography.titleSmall)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { items(state.planning.milestones.filter { it.dueDate >= today }, key = { it.id }) { item ->
                FilterChip(milestone == item.id, {
                    milestone = item.id; subject = item.subjectId
                    final = item.dueDate.minusDays(if (item.isExam || item.isTerminalExam) 1 else 0).toString()
                }, enabled = !busy, label = { Text(item.title) })
            } }
            OutlinedTextField(initial, { initial = it }, label = { Text(stringResource(R.string.topic_initial)) }, enabled = !busy, singleLine = true)
            OutlinedTextField(final, { final = it }, label = { Text(stringResource(R.string.topic_final)) }, enabled = !busy, singleLine = true)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(count, { count = it.filter(Char::isDigit).take(2); manualCount = true }, label = { Text(stringResource(R.string.topic_count)) }, enabled = !busy,
                    modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                OutlinedTextField(duration, { duration = it.filter(Char::isDigit).take(3) }, label = { Text(stringResource(R.string.topic_duration)) }, enabled = !busy,
                    modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
            }
            if (error) Text(stringResource(R.string.topic_invalid), color = RoutineColors.Warning)
            Button(enabled = !busy, onClick = {
                val from = ScheduleValidation.parseDate(initial); val to = ScheduleValidation.parseDate(final)
                val repetitions = count.toIntOrNull(); val size = duration.toIntOrNull()
                error = title.isBlank() || from == null || to == null || from < LocalDate.now() || to < from ||
                    java.time.temporal.ChronoUnit.DAYS.between(from, to) > 366 || repetitions == null || repetitions !in 1..52 || size == null || size !in 1..120
                if (!error) onAction(TimelineAction.SaveTopic(StudyTopic(title = title.trim(), subjectId = subject, initialDate = from!!,
                    finalDate = to!!, reviewCount = repetitions!!, reviewDurationMinutes = size!!, milestoneId = milestone), context.getString(R.string.review_title_pattern)))
            }) { Text(stringResource(R.string.topic_save)) }
        }
    }
}
