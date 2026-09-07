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
import com.example.mydailyroutine.domain.model.*
import com.example.mydailyroutine.domain.planning.PreparationStage
import com.example.mydailyroutine.features.entry.presentation.AppDatePicker
import com.example.mydailyroutine.core.designsystem.theme.*
import com.example.mydailyroutine.core.presentation.*
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
                        val stageNames = if (synthesis) listOf(R.string.stage_research,R.string.stage_draft,R.string.stage_revision) else listOf(R.string.stage_concepts,R.string.stage_practice,R.string.stage_check)
                        Text(stringResource(R.string.stages_hint, stringResource(stageNames[0]), stringResource(stageNames[1]), stringResource(stageNames[2])), style = MaterialTheme.typography.bodySmall, color = RoutineColors.TextSecondary)
                        if (milestone.estimatedEffortHours > 0) TextButton(enabled = !busy && milestone.dueDate >= LocalDate.now(), onClick = {
                            onAction(TimelineAction.PlanMilestone(milestone.id, maxOf(LocalDate.now(), state.content.date),
                                context.getString(R.string.preparation_title_pattern), context.getString(R.string.reserve_title),
                                if (synthesis) RoutineCategory.FOCUS_SYNTHESIZING else RoutineCategory.FOCUS_ANALYTICAL,
                                stageNames.zip(listOf(30,50,20)).map { (name,weight) -> PreparationStage(context.getString(name),weight) }))
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
