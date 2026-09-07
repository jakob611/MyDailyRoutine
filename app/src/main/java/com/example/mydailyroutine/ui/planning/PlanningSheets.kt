package com.example.mydailyroutine.ui.planning

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
import com.example.mydailyroutine.domain.model.*
import com.example.mydailyroutine.ui.editor.AppDatePicker
import com.example.mydailyroutine.ui.theme.*
import com.example.mydailyroutine.ui.timeline.*
import java.time.Duration
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanningSheet(state: TimelineUiState, onAction: (TimelineAction) -> Unit) {
    val context = LocalContext.current
    val busy = state.panels.isSaving
    var backlogDateId by rememberSaveable { mutableStateOf<Long?>(null) }
    var deleteTopicId by rememberSaveable { mutableStateOf<Long?>(null) }
    ModalBottomSheet(onDismissRequest = { onAction(TimelineAction.ClosePlanning) }, shape = RoutineShapes.Sheet,
        containerColor = RoutineColors.Surface1, tonalElevation = 0.dp, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        LazyColumn(Modifier.fillMaxWidth(), contentPadding = PaddingValues(24.dp, 0.dp, 24.dp, 32.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            item {
                Text(stringResource(R.string.planning_title), style = MaterialTheme.typography.headlineSmall)
                Text(stringResource(R.string.model_disclaimer), style = MaterialTheme.typography.bodySmall, color = RoutineColors.TextSecondary)
                OutlinedButton(enabled = !busy, onClick = { onAction(TimelineAction.AddReserve(state.content.date, context.getString(R.string.reserve_title))) }) { Text(stringResource(R.string.add_reserve)) }
            }
            item { Text(stringResource(R.string.backlog_heading), style = MaterialTheme.typography.titleLarge) }
            if (state.planning.backlog.isEmpty()) item { Text(stringResource(R.string.backlog_empty), color = RoutineColors.TextSecondary) }
            items(state.planning.backlog, key = { "backlog:${it.id}" }) { entry ->
                OutlinedCard(shape = RoutineShapes.Card, border = BorderStroke(1.dp, RoutineColors.Border)) {
                    Column(Modifier.fillMaxWidth().padding(14.dp)) {
                        Text(entry.title, style = MaterialTheme.typography.titleMedium)
                        Text(durationLabel(entry.durationMinutes), style = MaterialTheme.typography.bodySmall, color = RoutineColors.TextSecondary)
                        Row {
                            TextButton(enabled = !busy, onClick = { backlogDateId = entry.id }) { Text(stringResource(R.string.backlog_schedule)) }
                            TextButton(enabled = !busy, onClick = { onAction(TimelineAction.DeleteBacklog(entry.id)) }) { Text(stringResource(R.string.delete)) }
                        }
                    }
                }
            }
            item {
                Text(stringResource(R.string.learning_topics), style = MaterialTheme.typography.titleLarge)
                FilledTonalButton(enabled = !busy, onClick = { onAction(TimelineAction.NewTopic) }) { Text(stringResource(R.string.new_topic)) }
            }
            items(state.planning.topics, key = { "topic:${it.id}" }) { topic ->
                ListItem(headlineContent = { Text(topic.title) }, supportingContent = { Text(topic.finalDate.toString(), style = MaterialTheme.typography.bodySmall) },
                    trailingContent = { TextButton(enabled = !busy, onClick = { deleteTopicId = topic.id }) { Text(stringResource(R.string.delete)) } })
            }
            item { Text(stringResource(R.string.upcoming_milestones), style = MaterialTheme.typography.titleLarge) }
            items(state.planning.milestones.filter { !it.isCompleted }, key = { "goal:${it.id}" }) { milestone ->
                OutlinedCard(shape = RoutineShapes.Card, border = BorderStroke(1.dp, RoutineColors.Border)) {
                    Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        var synthesis by rememberSaveable(milestone.id) { mutableStateOf(false) }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(synthesis, { synthesis = it }, enabled = !busy)
                            Text(stringResource(R.string.category_project))
                        }
                        Text(milestone.title, style = MaterialTheme.typography.titleMedium)
                        Text(milestone.dueDate.toString(), style = MaterialTheme.typography.bodySmall, color = RoutineColors.TextSecondary)
                        if (milestone.estimatedEffortHours > 0) TextButton(enabled = !busy && milestone.dueDate >= LocalDate.now(), onClick = {
                            onAction(TimelineAction.PlanMilestone(milestone.id, maxOf(LocalDate.now(), state.content.date),
                                context.getString(R.string.preparation_title_pattern), context.getString(R.string.reserve_title),
                                if (synthesis) RoutineCategory.FOCUS_SYNTHESIZING else RoutineCategory.FOCUS_ANALYTICAL))
                        }) { Text(stringResource(R.string.plan_preparation)) }
                        else Text(stringResource(R.string.no_effort), style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
    backlogDateId?.let { id -> AppDatePicker(maxOf(LocalDate.now(), state.content.date), onDismiss = { backlogDateId = null }, onDate = {
        backlogDateId = null; onAction(TimelineAction.ScheduleBacklog(id, it))
    }) }
    deleteTopicId?.let { id -> AlertDialog(onDismissRequest = { deleteTopicId = null }, title = { Text(stringResource(R.string.delete_topic_title)) },
        text = { Text(stringResource(R.string.delete_topic_body)) }, confirmButton = { TextButton(enabled = !busy, onClick = { onAction(TimelineAction.DeleteTopic(id)); deleteTopicId = null }) { Text(stringResource(R.string.delete)) } },
        dismissButton = { TextButton(onClick = { deleteTopicId = null }) { Text(stringResource(R.string.cancel)) } }) }
}

@Composable
fun ActualCompletionDialog(block: ResolvedTimelineItem.Block, busy: Boolean, onAction: (TimelineAction) -> Unit) {
    var minutes by rememberSaveable(block.key) { mutableStateOf(Duration.between(block.startsAt, block.endsAt).toMinutes().toString()) }
    var invalid by rememberSaveable { mutableStateOf(false) }
    AlertDialog(onDismissRequest = { onAction(TimelineAction.CloseActual) }, title = { Text(stringResource(R.string.actual_title)) }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(block.title, style = MaterialTheme.typography.titleMedium)
            Text(stringResource(R.string.actual_hint), style = MaterialTheme.typography.bodySmall)
            OutlinedTextField(minutes, { minutes = it.filter(Char::isDigit).take(5) }, label = { Text(stringResource(R.string.actual_minutes)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, enabled = !busy)
            if (invalid) Text(stringResource(R.string.actual_invalid), color = RoutineColors.Warning)
        }
    }, confirmButton = { TextButton(enabled = !busy, onClick = {
        val value = minutes.toIntOrNull(); invalid = value == null || value !in 1..10080
        if (!invalid) onAction(TimelineAction.RecordActual(block, value!!))
    }) { Text(stringResource(R.string.actual_save)) } }, dismissButton = { TextButton(onClick = { onAction(TimelineAction.CloseActual) }) { Text(stringResource(R.string.cancel)) } })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopicEditorSheet(state: TimelineUiState, onAction: (TimelineAction) -> Unit) {
    val context = LocalContext.current
    val today = maxOf(LocalDate.now(), state.content.date)
    var title by rememberSaveable { mutableStateOf("") }
    var initial by rememberSaveable { mutableStateOf(today.toString()) }
    var final by rememberSaveable { mutableStateOf(today.plusDays(30).toString()) }
    var count by rememberSaveable { mutableStateOf("4") }
    var duration by rememberSaveable { mutableStateOf("15") }
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
                    final = item.dueDate.minusDays(if (item.isTerminalExam) 1 else 0).toString()
                }, enabled = !busy, label = { Text(item.title) })
            } }
            OutlinedTextField(initial, { initial = it }, label = { Text(stringResource(R.string.topic_initial)) }, enabled = !busy, singleLine = true)
            OutlinedTextField(final, { final = it }, label = { Text(stringResource(R.string.topic_final)) }, enabled = !busy, singleLine = true)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(count, { count = it.filter(Char::isDigit).take(2) }, label = { Text(stringResource(R.string.topic_count)) }, enabled = !busy,
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
