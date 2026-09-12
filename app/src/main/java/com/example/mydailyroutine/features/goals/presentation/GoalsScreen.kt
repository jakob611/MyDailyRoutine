package com.example.mydailyroutine.features.goals.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.mydailyroutine.R
import com.example.mydailyroutine.core.designsystem.theme.*
import com.example.mydailyroutine.core.platform.Slovenian
import com.example.mydailyroutine.core.presentation.*
import com.example.mydailyroutine.domain.model.GoalActivity
import com.example.mydailyroutine.domain.model.GoalMilestone
import com.example.mydailyroutine.domain.model.GoalProgress
import com.example.mydailyroutine.domain.model.GoalsProject
import com.example.mydailyroutine.features.entry.presentation.AppDatePicker
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import kotlin.math.roundToInt

internal val GoalDateFormat = DateTimeFormatter.ofPattern("d. MMM yyyy", Slovenian)
internal val GoalMonthFormat = DateTimeFormatter.ofPattern("LLL yy", Slovenian)
internal val GoalShortFormat = DateTimeFormatter.ofPattern("d. MMM", Slovenian)

/** Full-screen long-term planner for CAS/EE: status, Gantt strip, activities, milestones. */

@Composable
fun GoalsScreen(goals: GoalsUiState, busy: Boolean, onAction: (TimelineAction) -> Unit) {
    var selectedId by rememberSaveable { mutableStateOf<Long?>(null) }
    var editingActivity by remember { mutableStateOf<GoalActivity?>(null) }
    var addingActivity by remember { mutableStateOf(false) }
    var editingMilestone by remember { mutableStateOf<GoalMilestone?>(null) }
    var addingMilestone by remember { mutableStateOf(false) }
    var editingProject by remember { mutableStateOf<GoalsProject?>(null) }
    var addingProject by remember { mutableStateOf(false) }
    val project = goals.projects.firstOrNull { it.id == selectedId } ?: goals.projects.lastOrNull()
    if (project == null && !addingProject) {
        GoalEmptyState(busy, onAction) { addingProject = true }
        return
    }
    Column(Modifier.fillMaxSize()) {
        if (goals.projects.size > 1) {
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                goals.projects.forEach { candidate ->
                    if (project?.id == candidate.id) FilledTonalButton(onClick = { selectedId = candidate.id }) { Text(candidate.name, maxLines = 1) }
                    else OutlinedButton(onClick = { selectedId = candidate.id }) { Text(candidate.name, maxLines = 1) }
                }
            }
        }
        if (project != null) {
            val activities = goals.activities.filter { it.projectId == project.id }
            val milestones = goals.milestones.filter { it.projectId == project.id }.sortedBy { it.dueDate }
            val progress = goals.progress.filter { it.projectId == project.id }
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                StatusCard(project, activities, milestones, progress, busy, onEdit = { editingProject = project }, onAction = onAction)
                Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = RoutineColors.Surface1), shape = RoutineShapes.Card) {
                    Column(Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(stringResource(R.string.goals_gantt), style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(horizontal = 14.dp))
                        GoalGantt(project, activities, milestones) { editingActivity = it }
                    }
                }
                ActivityList(activities, progress, busy, onAction, onAdd = { addingActivity = true }, onEdit = { editingActivity = it })
                MilestoneList(milestones, busy, onAction, onAdd = { addingMilestone = true }, onEdit = { editingMilestone = it })
            }
        }
    }
    if (project != null && (addingActivity || editingActivity != null)) {
        ActivityEditorSheet(project, editingActivity, busy, goals.progress,
            onDismiss = { addingActivity = false; editingActivity = null }, onAction = onAction)
    }
    if (project != null && (addingMilestone || editingMilestone != null)) {
        MilestoneEditorSheet(project, editingMilestone, busy,
            onDismiss = { addingMilestone = false; editingMilestone = null }, onAction = onAction)
    }
    if (addingProject || editingProject != null) {
        ProjectEditorSheet(editingProject, busy, onDismiss = { addingProject = false; editingProject = null }, onAction = onAction)
    }
}

@Composable
private fun GoalEmptyState(busy: Boolean, onAction: (TimelineAction) -> Unit, onNewCustom: () -> Unit) {
    val context = LocalContext.current
    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.weight(1f))
        Text(stringResource(R.string.goals_title), style = MaterialTheme.typography.headlineMedium)
        Text(stringResource(R.string.goals_empty_body), style = MaterialTheme.typography.bodyMedium, color = RoutineColors.TextSecondary)
        FilledTonalButton(enabled = !busy, modifier = Modifier.fillMaxWidth(), onClick = {
            onAction(TimelineAction.SeedGoalProject("CAS", context.getString(R.string.goals_seed_cas_name), emptyList(),
                listOf(R.string.goals_cas_meeting_1, R.string.goals_cas_meeting_2, R.string.goals_cas_meeting_3, R.string.goals_cas_statement).map { context.getString(it) }))
        }) { Text(stringResource(R.string.goals_seed_cas)) }
        FilledTonalButton(enabled = !busy, modifier = Modifier.fillMaxWidth(), onClick = {
            onAction(TimelineAction.SeedGoalProject("EE", context.getString(R.string.goals_seed_ee_name),
                listOf(R.string.ee_stage_1, R.string.ee_stage_2, R.string.ee_stage_3, R.string.ee_stage_4, R.string.ee_stage_5, R.string.ee_stage_6).map { context.getString(it) },
                listOf(R.string.ee_milestone_1, R.string.ee_milestone_2, R.string.ee_milestone_3, R.string.ee_milestone_4, R.string.ee_milestone_5).map { context.getString(it) }))
        }) { Text(stringResource(R.string.goals_seed_ee)) }
        OutlinedButton(enabled = !busy, modifier = Modifier.fillMaxWidth(), onClick = onNewCustom) { Text(stringResource(R.string.goals_new_project)) }
        Spacer(Modifier.weight(1f))
    }
}

@Composable
private fun StatusCard(project: GoalsProject, activities: List<GoalActivity>, milestones: List<GoalMilestone>,
                       progress: List<GoalProgress>, busy: Boolean, onEdit: () -> Unit, onAction: (TimelineAction) -> Unit) {
    val hours = progress.filter { it.kind == "hour" }.sumOf { it.amount }
    val words = progress.filter { it.kind == "word" }.sumOf { it.amount.toInt() }
    val next = milestones.filter { !it.isDone }.minByOrNull { it.dueDate }
    val categoryHours = progress.filter { it.kind == "hour" }.groupBy { entry -> activities.firstOrNull { it.id == entry.activityId }?.category }
    OutlinedCard(Modifier.fillMaxWidth(), shape = RoutineShapes.Card, border = BorderStroke(1.dp, RoutineColors.Border)) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(project.name, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                TextButton(enabled = !busy, onClick = onEdit) { Text(stringResource(R.string.edit)) }
            }
            Text(stringResource(R.string.date_range, project.start.format(GoalDateFormat), project.end.format(GoalDateFormat)),
                style = MaterialTheme.typography.bodySmall, color = RoutineColors.TextSecondary)
            val hourTarget = project.targetHours
            if (hourTarget != null) {
                GoalBar((hours / hourTarget).toFloat(), RoutineColors.Amber)
                Text(stringResource(R.string.goals_hours_total, hours.roundToInt(), hourTarget.roundToInt()), style = MaterialTheme.typography.labelMedium)
            }
            if (project.kind == "CAS") {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    listOf("CREATIVITY" to R.string.goals_category_creativity, "ACTIVITY" to R.string.goals_category_activity, "SERVICE" to R.string.goals_category_service).forEach { (category, res) ->
                        Column(Modifier.weight(1f)) {
                            Text(stringResource(res), style = MaterialTheme.typography.labelSmall, color = categoryColor(category), maxLines = 1)
                            Text(stringResource(R.string.goals_hours_item, categoryHours[category]?.sumOf { it.amount }?.roundToInt() ?: 0),
                                style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
                if (listOf("CREATIVITY", "ACTIVITY", "SERVICE").any { (categoryHours[it]?.sumOf { entry -> entry.amount } ?: 0.0) == 0.0 })
                    Text(stringResource(R.string.goals_balance_missing), style = MaterialTheme.typography.labelSmall, color = RoutineColors.Warning)
            }
            val wordTarget = project.targetWords
            if (project.kind == "EE" && wordTarget != null) {
                GoalBar((words.toFloat() / wordTarget), RoutineColors.Cobalt)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.goals_words_total, words, wordTarget), style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(1f))
                    listOf(100, 250, 500).forEach { step ->
                        TextButton(enabled = !busy, onClick = {
                            onAction(TimelineAction.AddGoalProgress(GoalProgress(projectId = project.id, activityId = null, kind = "word", amount = step.toDouble(), date = LocalDate.now())))
                        }) { Text(stringResource(R.string.goals_hours_step, step)) }
                    }
                }
            }
            if (next != null) {
                val days = ChronoUnit.DAYS.between(LocalDate.now(), next.dueDate)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(Modifier.size(8.dp).clip(CircleShape).background(when { days < 0 -> RoutineColors.Crimson; days <= 14 -> RoutineColors.Amber; else -> RoutineColors.Sage }))
                    Text(stringResource(R.string.goals_next_milestone), style = MaterialTheme.typography.labelMedium, color = RoutineColors.TextSecondary)
                    Text(next.title, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f), maxLines = 1)
                    Text(goalRelative(next.dueDate), style = MaterialTheme.typography.labelMedium,
                        color = if (days < 0) RoutineColors.Crimson else RoutineColors.TextSecondary)
                }
            } else if (milestones.isEmpty()) {
                Text(stringResource(R.string.goals_milestones_empty), style = MaterialTheme.typography.bodySmall, color = RoutineColors.TextSecondary)
            }
            Text(stringResource(R.string.goals_activity_progress, activities.count { it.isDone }, activities.size),
                style = MaterialTheme.typography.labelMedium, color = RoutineColors.TextSecondary)
            if (project.kind == "EE") Text(stringResource(R.string.goals_period_note), style = MaterialTheme.typography.labelSmall, color = RoutineColors.Warning)
        }
    }
}

@Composable
private fun ActivityList(activities: List<GoalActivity>, progress: List<GoalProgress>,
                         busy: Boolean, onAction: (TimelineAction) -> Unit, onAdd: () -> Unit, onEdit: (GoalActivity) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.goals_activities), style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
            FilledTonalButton(enabled = !busy, onClick = onAdd) { Text(stringResource(R.string.goals_add_activity)) }
        }
        if (activities.isEmpty()) Text(stringResource(R.string.goals_activities_empty), style = MaterialTheme.typography.bodySmall, color = RoutineColors.TextSecondary)
        activities.forEach { activity ->
            val hours = progress.filter { it.kind == "hour" && it.activityId == activity.id }.sumOf { it.amount }
            val details = listOfNotNull(
                stringResource(R.string.date_range, activity.start.format(GoalShortFormat), activity.end.format(GoalShortFormat)),
                categoryLabel(activity.category),
                if (hours > 0.0) stringResource(R.string.goals_hours_item, hours.roundToInt()) else null,
                if (activity.isScheduled) stringResource(R.string.goals_scheduled) else null,
                if (activity.isCasProject) stringResource(R.string.goals_cas_project_short) else null,
                if (activity.isDone) stringResource(R.string.goals_activity_done) else null,
            ).joinToString(" · ")
            OutlinedCard(onClick = { onEdit(activity) }, modifier = Modifier.fillMaxWidth(), shape = RoutineShapes.Card, border = BorderStroke(1.dp, RoutineColors.Border)) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Checkbox(activity.isDone, { if (!busy) onAction(TimelineAction.SaveGoalActivity(activity.copy(isDone = !activity.isDone))) }, enabled = !busy)
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(activity.title, style = MaterialTheme.typography.titleSmall, maxLines = 2)
                        Text(details, style = MaterialTheme.typography.labelSmall, color = RoutineColors.TextSecondary, maxLines = 2)
                    }
                }
            }
        }
    }
}

@Composable
private fun MilestoneList(milestones: List<GoalMilestone>, busy: Boolean, onAction: (TimelineAction) -> Unit, onAdd: () -> Unit, onEdit: (GoalMilestone) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.goals_milestones), style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
            FilledTonalButton(enabled = !busy, onClick = onAdd) { Text(stringResource(R.string.goals_add_milestone)) }
        }
        if (milestones.isEmpty()) Text(stringResource(R.string.goals_milestones_empty), style = MaterialTheme.typography.bodySmall, color = RoutineColors.TextSecondary)
        milestones.forEach { milestone ->
            OutlinedCard(shape = RoutineShapes.Card, border = BorderStroke(1.dp, RoutineColors.Border)) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Checkbox(milestone.isDone, { if (!busy) onAction(TimelineAction.ToggleGoalMilestone(milestone.id)) }, enabled = !busy)
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(milestone.title, style = MaterialTheme.typography.titleSmall, maxLines = 2)
                        Text("${milestone.dueDate.format(GoalShortFormat)} · ${goalRelative(milestone.dueDate)}", style = MaterialTheme.typography.labelSmall, color = RoutineColors.TextSecondary, maxLines = 1)
                    }
                    TextButton(enabled = !busy, onClick = { onEdit(milestone) }) { Text(stringResource(R.string.edit)) }
                }
            }
        }
    }
}

@Composable
private fun goalRelative(date: LocalDate): String {
    val days = ChronoUnit.DAYS.between(LocalDate.now(), date)
    return when {
        days == 0L -> stringResource(R.string.tasks_today_short)
        days == 1L -> stringResource(R.string.tasks_tomorrow)
        days > 0 -> stringResource(R.string.tasks_in_days, days.toInt())
        else -> stringResource(R.string.tasks_ago_days, (-days).toInt())
    }
}

internal fun categoryColor(category: String?): Color = when (category) {
    "CREATIVITY" -> RoutineColors.Amber
    "ACTIVITY" -> RoutineColors.Sage
    "SERVICE" -> RoutineColors.Violet
    "STAGE" -> RoutineColors.Cobalt
    else -> RoutineColors.TextSecondary
}

@Composable
private fun categoryLabel(category: String?): String? = when (category) {
    "CREATIVITY" -> stringResource(R.string.goals_category_creativity)
    "ACTIVITY" -> stringResource(R.string.goals_category_activity)
    "SERVICE" -> stringResource(R.string.goals_category_service)
    "STAGE" -> stringResource(R.string.goals_category_stage)
    else -> null
}
