package com.example.mydailyroutine.features.planning.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.SheetState
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.mydailyroutine.R
import com.example.mydailyroutine.core.designsystem.components.ActionRow
import com.example.mydailyroutine.core.designsystem.components.CategoryTabs
import com.example.mydailyroutine.core.designsystem.components.RoutineLabel
import com.example.mydailyroutine.core.designsystem.components.RoutineSheetListScaffold
import com.example.mydailyroutine.core.designsystem.components.RoutineText
import com.example.mydailyroutine.core.designsystem.components.RoutineTextDefaults
import com.example.mydailyroutine.core.designsystem.components.SheetPrimaryButton
import com.example.mydailyroutine.core.designsystem.components.SettingRow
import com.example.mydailyroutine.core.designsystem.haptics.LocalRoutineHaptics
import com.example.mydailyroutine.core.designsystem.theme.RoutineColors
import com.example.mydailyroutine.core.designsystem.theme.RoutineMetrics
import com.example.mydailyroutine.core.designsystem.theme.RoutineShapes
import com.example.mydailyroutine.core.designsystem.theme.RoutineSpacing
import com.example.mydailyroutine.core.presentation.TimelineAction
import com.example.mydailyroutine.core.presentation.TimelineUiState
import com.example.mydailyroutine.core.presentation.durationLabel
import com.example.mydailyroutine.core.presentation.RoutineDate
import com.example.mydailyroutine.domain.learning.StudyTopic
import com.example.mydailyroutine.domain.model.Milestone
import com.example.mydailyroutine.domain.model.RoutineCategory
import com.example.mydailyroutine.domain.planning.BacklogEntry
import com.example.mydailyroutine.domain.planning.PreparationStage
import com.example.mydailyroutine.features.entry.presentation.AppDatePicker
import java.time.LocalDate


/** The planning sheet is split into three short lists instead of one endless scroll. */
internal enum class PlanningTab { BACKLOG, TOPICS, MARKERS }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanningSheet(state: TimelineUiState, onAction: (TimelineAction) -> Unit, sheetState: SheetState) {
    val context = LocalContext.current
    val busy = state.panels.isSaving
    var tab by rememberSaveable { mutableStateOf(PlanningTab.BACKLOG) }
    var backlogDateId by rememberSaveable { mutableStateOf<Long?>(null) }
    var deleteBacklogId by rememberSaveable { mutableStateOf<Long?>(null) }
    var deleteTopicId by rememberSaveable { mutableStateOf<Long?>(null) }
    val planning = state.planning
    ModalBottomSheet(
        onDismissRequest = { onAction(TimelineAction.ClosePlanning) },
        shape = RoutineShapes.Sheet,
        containerColor = RoutineColors.SheetSurface,
        tonalElevation = 0.dp,
        sheetState = sheetState,
    ) {
        RoutineSheetListScaffold(
            title = stringResource(R.string.planning_title),
            subtitle = stringResource(R.string.model_disclaimer),
            closeLabel = stringResource(R.string.close),
            onClose = { onAction(TimelineAction.ClosePlanning) },
            modifier = Modifier.testTag("planning-sheet"),
        ) {
            item(key = "planning-tabs") {
                CategoryTabs(
                    entries = PlanningTab.entries.toList(),
                    selected = tab,
                    label = { entry -> planningTabLabel(entry) },
                    onSelect = { entry -> tab = entry },
                    tagPrefix = "planning-tab",
                    enabled = !busy,
                )
            }
            when (tab) {
                PlanningTab.BACKLOG -> backlogTab(planning.backlog.toList(), busy,
                    onAddReserve = {
                        onAction(TimelineAction.AddReserve(state.content.date, context.getString(R.string.reserve_title)))
                    },
                    onSchedule = { backlogDateId = it },
                    onRequestDelete = { deleteBacklogId = it })
                PlanningTab.TOPICS -> topicsTab(planning.topics.toList(), busy,
                    onNewTopic = { onAction(TimelineAction.NewTopic) },
                    onRequestDelete = { deleteTopicId = it })
                PlanningTab.MARKERS -> markersTab(planning.milestones.toList().filter { !it.isCompleted }, busy,
                    onPlan = { milestone, synthesis ->
                        val stageNames = if (synthesis) {
                            listOf(R.string.stage_research, R.string.stage_draft, R.string.stage_revision)
                        } else {
                            listOf(R.string.stage_concepts, R.string.stage_practice, R.string.stage_check)
                        }
                        onAction(
                            TimelineAction.PlanMilestone(
                                milestone.id,
                                maxOf(LocalDate.now(), state.content.date),
                                context.getString(R.string.preparation_title_pattern),
                                context.getString(R.string.reserve_title),
                                if (synthesis) RoutineCategory.FOCUS_SYNTHESIZING else RoutineCategory.FOCUS_ANALYTICAL,
                                stageNames.zip(listOf(30, 50, 20)).map { (name, weight) ->
                                    PreparationStage(context.getString(name), weight)
                                },
                            ),
                        )
                    })
            }
        }
    }
    backlogDateId?.let { id ->
        AppDatePicker(
            maxOf(LocalDate.now(), state.content.date),
            onDismiss = { backlogDateId = null },
            onDate = {
                backlogDateId = null
                onAction(TimelineAction.ScheduleBacklog(id, it))
            },
        )
    }
    planning.backlog.firstOrNull { it.id == deleteBacklogId }?.let { entry ->
        AlertDialog(
            onDismissRequest = { deleteBacklogId = null },
            title = { RoutineText(stringResource(R.string.delete_entry_title), style = MaterialTheme.typography.headlineSmall) },
            text = { RoutineText(stringResource(R.string.delete_entry_body, entry.title), maxLines = RoutineTextDefaults.Paragraph) },
            confirmButton = {
                TextButton(enabled = !busy, onClick = { onAction(TimelineAction.DeleteBacklog(entry.id)); deleteBacklogId = null }) {
                    RoutineLabel(stringResource(R.string.delete), style = MaterialTheme.typography.labelLarge, color = RoutineColors.Crimson)
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteBacklogId = null }) {
                    RoutineLabel(stringResource(R.string.keep), style = MaterialTheme.typography.labelLarge)
                }
            },
        )
    }
    deleteTopicId?.let { id ->
        AlertDialog(
            onDismissRequest = { deleteTopicId = null },
            title = { RoutineText(stringResource(R.string.delete_topic_title), style = MaterialTheme.typography.headlineSmall) },
            text = { RoutineText(stringResource(R.string.delete_topic_body), maxLines = RoutineTextDefaults.Paragraph) },
            confirmButton = {
                TextButton(enabled = !busy, onClick = { onAction(TimelineAction.DeleteTopic(id)); deleteTopicId = null }) {
                    RoutineLabel(stringResource(R.string.delete), style = MaterialTheme.typography.labelLarge, color = RoutineColors.Crimson)
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteTopicId = null }) {
                    RoutineLabel(stringResource(R.string.cancel), style = MaterialTheme.typography.labelLarge)
                }
            },
        )
    }
}

@Composable
private fun planningTabLabel(tab: PlanningTab): String = when (tab) {
    PlanningTab.BACKLOG -> stringResource(R.string.planning_tab_backlog)
    PlanningTab.TOPICS -> stringResource(R.string.planning_tab_topics)
    PlanningTab.MARKERS -> stringResource(R.string.planning_tab_markers)
}

private fun LazyListScope.backlogTab(
    backlog: List<BacklogEntry>,
    busy: Boolean,
    onAddReserve: () -> Unit,
    onSchedule: (Long) -> Unit,
    onRequestDelete: (Long) -> Unit,
) {
    item(key = "backlog-add") { SheetPrimaryButton(stringResource(R.string.add_reserve), onAddReserve, enabled = !busy) }
    if (backlog.isEmpty()) {
        item(key = "backlog-empty") {
            RoutineText(stringResource(R.string.backlog_empty), style = MaterialTheme.typography.bodyMedium,
                color = RoutineColors.TextSecondary, maxLines = RoutineTextDefaults.Paragraph)
        }
        return
    }
    items(backlog, key = { "backlog:${it.id}" }) { entry ->
        OutlinedCard(shape = RoutineShapes.Card, border = BorderStroke(1.dp, RoutineColors.Border),
            modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.fillMaxWidth().padding(RoutineSpacing.md),
                verticalArrangement = Arrangement.spacedBy(RoutineSpacing.sm)) {
                RoutineText(entry.title, style = MaterialTheme.typography.titleMedium, maxLines = RoutineTextDefaults.Body)
                RoutineLabel(durationLabel(entry.durationMinutes), style = MaterialTheme.typography.labelSmall,
                    color = RoutineColors.TextSecondary)
                ActionRow {
                    TextButton(enabled = !busy, onClick = { onSchedule(entry.id) }) {
                        RoutineLabel(stringResource(R.string.backlog_schedule), style = MaterialTheme.typography.labelLarge)
                    }
                    TextButton(enabled = !busy, onClick = { onRequestDelete(entry.id) }) {
                        RoutineLabel(stringResource(R.string.delete), style = MaterialTheme.typography.labelLarge,
                            color = RoutineColors.Crimson)
                    }
                }
            }
        }
    }
}

private fun LazyListScope.topicsTab(
    topics: List<StudyTopic>,
    busy: Boolean,
    onNewTopic: () -> Unit,
    onRequestDelete: (Long) -> Unit,
) {
    item(key = "topics-new") { SheetPrimaryButton(stringResource(R.string.new_topic), onNewTopic, enabled = !busy) }
    if (topics.isEmpty()) {
        item(key = "topics-empty") {
            RoutineText(stringResource(R.string.planning_topics_empty), style = MaterialTheme.typography.bodyMedium,
                color = RoutineColors.TextSecondary, maxLines = RoutineTextDefaults.Paragraph)
        }
        return
    }
    items(topics, key = { "topic:${it.id}" }) { topic ->
        OutlinedCard(shape = RoutineShapes.Card, border = BorderStroke(1.dp, RoutineColors.Border),
            modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.fillMaxWidth().padding(RoutineSpacing.md),
                verticalArrangement = Arrangement.spacedBy(RoutineSpacing.sm)) {
                RoutineText(topic.title, style = MaterialTheme.typography.titleMedium, maxLines = RoutineTextDefaults.Body)
                RoutineLabel(stringResource(R.string.planning_topic_due, RoutineDate.withWeekdayYear(topic.finalDate)),
                    style = MaterialTheme.typography.labelSmall, color = RoutineColors.TextSecondary)
                ActionRow {
                    TextButton(enabled = !busy, onClick = { onRequestDelete(topic.id) }) {
                        RoutineLabel(stringResource(R.string.delete), style = MaterialTheme.typography.labelLarge,
                            color = RoutineColors.Crimson)
                    }
                }
            }
        }
    }
}

private fun LazyListScope.markersTab(
    milestones: List<Milestone>,
    busy: Boolean,
    onPlan: (Milestone, Boolean) -> Unit,
) {
    if (milestones.isEmpty()) {
        item(key = "markers-empty") {
            RoutineText(stringResource(R.string.planning_milestones_empty), style = MaterialTheme.typography.bodyMedium,
                color = RoutineColors.TextSecondary, maxLines = RoutineTextDefaults.Paragraph)
        }
        return
    }
    items(milestones, key = { "goal:${it.id}" }) { milestone ->
        OutlinedCard(shape = RoutineShapes.Card, border = BorderStroke(1.dp, RoutineColors.Border),
            modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.fillMaxWidth().padding(RoutineSpacing.md),
                verticalArrangement = Arrangement.spacedBy(RoutineSpacing.sm)) {
                var synthesis by rememberSaveable(milestone.id) { mutableStateOf(false) }
                RoutineText(milestone.title, style = MaterialTheme.typography.titleMedium, maxLines = RoutineTextDefaults.Body)
                RoutineLabel(RoutineDate.withWeekdayYear(milestone.dueDate), style = MaterialTheme.typography.labelMedium,
                    color = if (milestone.dueDate.isBefore(LocalDate.now())) RoutineColors.Crimson else RoutineColors.Violet)
                SettingRow(
                    title = stringResource(R.string.planning_synthesis_toggle),
                    control = {
                        Checkbox(
                            checked = synthesis,
                            onCheckedChange = { synthesis = it },
                            enabled = !busy,
                            modifier = Modifier.size(RoutineMetrics.ActionMinWidth),
                        )
                    },
                )
                val stageNames = if (synthesis) {
                    listOf(R.string.stage_research, R.string.stage_draft, R.string.stage_revision)
                } else {
                    listOf(R.string.stage_concepts, R.string.stage_practice, R.string.stage_check)
                }
                Row(verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(RoutineSpacing.sm)) {
                    Box(Modifier.size(6.dp).clip(CircleShape).background(RoutineColors.Violet))
                    RoutineText(
                        text = stringResource(
                            R.string.stages_hint,
                            stringResource(stageNames[0]),
                            stringResource(stageNames[1]),
                            stringResource(stageNames[2]),
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = RoutineColors.TextSecondary,
                        maxLines = RoutineTextDefaults.Paragraph,
                    )
                }
                if (milestone.estimatedEffortHours > 0) {
                    SheetPrimaryButton(
                        label = stringResource(R.string.plan_preparation),
                        onClick = { onPlan(milestone, synthesis) },
                        enabled = !busy && milestone.dueDate >= LocalDate.now(),
                    )
                } else {
                    RoutineText(stringResource(R.string.no_effort), style = MaterialTheme.typography.bodySmall,
                        color = RoutineColors.TextSecondary, maxLines = RoutineTextDefaults.Paragraph)
                }
            }
        }
    }
}
