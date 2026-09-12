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

private val GoalDateFormat = DateTimeFormatter.ofPattern("d. MMM yyyy", Slovenian)
private val GoalMonthFormat = DateTimeFormatter.ofPattern("LLL yy", Slovenian)
private val GoalShortFormat = DateTimeFormatter.ofPattern("d. MMM", Slovenian)

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
private fun GoalBar(fraction: Float, color: Color) {
    Box(Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)).background(RoutineColors.Surface2)) {
        Box(Modifier.fillMaxWidth(fraction.coerceIn(0f, 1f)).height(8.dp).clip(RoundedCornerShape(4.dp)).background(color))
    }
}

@Composable
private fun GoalGantt(project: GoalsProject, activities: List<GoalActivity>, milestones: List<GoalMilestone>, onActivity: (GoalActivity) -> Unit) {
    val today = LocalDate.now()
    val spanDays = maxOf(1L, ChronoUnit.DAYS.between(project.start, project.end))
    val monthCount = (spanDays / 31 + 1).toInt().coerceIn(2, 30)
    val cellWidth = 64f
    fun xOf(date: LocalDate): Dp {
        val days = ChronoUnit.DAYS.between(project.start, date).toFloat().coerceIn(0f, spanDays.toFloat())
        return (days / spanDays.toFloat() * monthCount * cellWidth).dp
    }
    Column(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
        Box {
            Column(Modifier.width((monthCount * cellWidth).dp)) {
                Row(Modifier.fillMaxWidth()) {
                    repeat(monthCount) { index ->
                        Box(Modifier.width(64.dp).padding(start = 4.dp)) {
                            Text(YearMonth.from(project.start).plusMonths(index.toLong()).format(GoalMonthFormat),
                                style = MaterialTheme.typography.labelSmall, color = RoutineColors.TextSecondary, maxLines = 1)
                        }
                    }
                }
                Box(Modifier.fillMaxWidth().height(26.dp)) {
                    milestones.forEach { milestone ->
                        Box(Modifier.offset(x = xOf(milestone.dueDate) - 5.dp, y = 7.dp).size(11.dp).rotate(45f)
                            .background(if (milestone.isDone) RoutineColors.TextMuted.copy(alpha = 0.5f) else RoutineColors.Crimson, RoundedCornerShape(2.dp)))
                    }
                }
                activities.forEach { activity ->
                    Box(Modifier.fillMaxWidth().height(42.dp)) {
                        val left = xOf(activity.start)
                        val width = (xOf(activity.end) - left).coerceAtLeast(22.dp) - 4.dp
                        Box(Modifier.offset(x = left, y = 4.dp).width(width).height(34.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(categoryColor(activity.category).copy(alpha = if (activity.isDone) 0.35f else 0.85f))
                            .then(if (activity.isCasProject) Modifier.border(1.5.dp, RoutineColors.Violet, RoundedCornerShape(6.dp)) else Modifier)
                            .clickable { onActivity(activity) }
                            .padding(horizontal = 8.dp, vertical = 8.dp)) {
                            Text(activity.title, style = MaterialTheme.typography.labelMedium, color = RoutineColors.Background, maxLines = 1)
                        }
                    }
                }
            }
            if (!today.isBefore(project.start) && !today.isAfter(project.end)) {
                Box(Modifier.offset(x = xOf(today)).matchParentSize().width(2.dp).background(RoutineColors.Amber.copy(alpha = 0.6f)))
            }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ActivityEditorSheet(project: GoalsProject, initial: GoalActivity?, busy: Boolean, progress: List<GoalProgress>,
                                onDismiss: () -> Unit, onAction: (TimelineAction) -> Unit) {
    var title by rememberSaveable(initial?.id) { mutableStateOf(initial?.title ?: "") }
    var category by rememberSaveable(initial?.id) { mutableStateOf(initial?.category ?: if (project.kind == "EE") "STAGE" else null) }
    var startEpoch by rememberSaveable(initial?.id) { mutableStateOf((initial?.start ?: LocalDate.now()).toEpochDay()) }
    var endEpoch by rememberSaveable(initial?.id) { mutableStateOf((initial?.end ?: LocalDate.now().plusDays(30)).toEpochDay()) }
    var note by rememberSaveable(initial?.id) { mutableStateOf(initial?.note.orEmpty()) }
    var casProject by rememberSaveable(initial?.id) { mutableStateOf(initial?.isCasProject ?: false) }
    var done by rememberSaveable(initial?.id) { mutableStateOf(initial?.isDone ?: false) }
    var pickingStart by rememberSaveable { mutableStateOf(false) }
    var pickingEnd by rememberSaveable { mutableStateOf(false) }
    var confirmingDelete by rememberSaveable { mutableStateOf(false) }
    var reflectionText by rememberSaveable(initial?.id) { mutableStateOf("") }
    val start = LocalDate.ofEpochDay(startEpoch)
    val end = LocalDate.ofEpochDay(endEpoch).let { if (it.isBefore(start)) start else it }
    val saved = initial != null && initial.id > 0
    ModalBottomSheet(onDismissRequest = onDismiss, shape = RoutineShapes.Sheet, containerColor = RoutineColors.Surface1,
        tonalElevation = 0.dp, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.fillMaxWidth().padding(24.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(if (saved) R.string.goals_edit_activity else R.string.goals_new_activity), style = MaterialTheme.typography.headlineSmall)
            OutlinedTextField(title, { title = it.take(80) }, modifier = Modifier.fillMaxWidth(), singleLine = true, enabled = !busy,
                label = { Text(stringResource(R.string.goals_activity_title)) })
            if (project.kind == "CAS") {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("CREATIVITY" to R.string.goals_category_creativity, "ACTIVITY" to R.string.goals_category_activity, "SERVICE" to R.string.goals_category_service).forEach { (value, res) ->
                        if (category == value) FilledTonalButton(onClick = { category = null }) { Text(stringResource(res)) }
                        else OutlinedButton(onClick = { category = value }) { Text(stringResource(res)) }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(casProject, { casProject = it }, enabled = !busy)
                    Text(stringResource(R.string.goals_cas_project_flag), style = MaterialTheme.typography.bodySmall)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(modifier = Modifier.weight(1f), enabled = !busy, onClick = { pickingStart = true }) {
                    Text("${stringResource(R.string.goals_start_date)} · ${start.format(GoalShortFormat)}", maxLines = 1)
                }
                OutlinedButton(modifier = Modifier.weight(1f), enabled = !busy, onClick = { pickingEnd = true }) {
                    Text("${stringResource(R.string.goals_end_date)} · ${end.format(GoalShortFormat)}", maxLines = 1)
                }
            }
            OutlinedTextField(note, { note = it.take(2000) }, modifier = Modifier.fillMaxWidth(), minLines = 3, enabled = !busy,
                label = { Text(stringResource(R.string.goals_activity_note)) }, placeholder = { Text(stringResource(R.string.goals_activity_note_hint)) })
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(done, { done = it }, enabled = !busy)
                Text(stringResource(R.string.goals_done))
            }
            if (saved && initial != null) {
                val hours = progress.filter { it.kind == "hour" && it.activityId == initial.id }.sumOf { it.amount }
                Text(stringResource(R.string.goals_hours_total_log, hours.roundToInt()), style = MaterialTheme.typography.titleMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.goals_hours_log), style = MaterialTheme.typography.labelMedium, color = RoutineColors.TextSecondary, modifier = Modifier.weight(1f))
                    TextButton(enabled = !busy, onClick = { onAction(logProgress(project.id, initial.id, 0.5)) }) { Text(stringResource(R.string.goals_minutes_step, 30)) }
                    TextButton(enabled = !busy, onClick = { onAction(logProgress(project.id, initial.id, 1.0)) }) { Text(stringResource(R.string.goals_hours_step, 1)) }
                    TextButton(enabled = !busy, onClick = { onAction(logProgress(project.id, initial.id, 2.0)) }) { Text(stringResource(R.string.goals_hours_step, 2)) }
                }
                Text(stringResource(R.string.goals_reflections), style = MaterialTheme.typography.titleMedium)
                val reflections = progress.filter { it.kind == "reflection" && it.activityId == initial.id }
                if (reflections.isEmpty()) Text(stringResource(R.string.goals_reflections_empty), style = MaterialTheme.typography.bodySmall, color = RoutineColors.TextSecondary)
                reflections.forEach { entry ->
                    OutlinedCard(shape = RoutineShapes.Card, border = BorderStroke(1.dp, RoutineColors.Border)) {
                        Column(Modifier.fillMaxWidth().padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(entry.date.format(GoalShortFormat), style = MaterialTheme.typography.labelSmall, color = RoutineColors.TextSecondary)
                            Text(entry.note.orEmpty(), style = MaterialTheme.typography.bodySmall)
                            Row {
                                Spacer(Modifier.weight(1f))
                                TextButton(enabled = !busy, onClick = { onAction(TimelineAction.DeleteGoalProgress(entry.id)) }) { Text(stringResource(R.string.delete), color = RoutineColors.Crimson) }
                            }
                        }
                    }
                }
                OutlinedTextField(reflectionText, { reflectionText = it.take(2000) }, modifier = Modifier.fillMaxWidth(), minLines = 2, enabled = !busy,
                    label = { Text(stringResource(R.string.goals_add_reflection)) }, placeholder = { Text(stringResource(R.string.goals_reflection_hint)) })
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilledTonalButton(enabled = !busy && reflectionText.isNotBlank(), onClick = {
                        onAction(TimelineAction.AddGoalProgress(GoalProgress(projectId = project.id, activityId = initial.id, kind = "reflection", amount = 1.0, note = reflectionText.trim(), date = LocalDate.now())))
                        reflectionText = ""
                    }) { Text(stringResource(R.string.goals_save_reflection)) }
                    Spacer(Modifier.weight(1f))
                    TextButton(enabled = !busy, onClick = { onAction(TimelineAction.GoalActivityToSchedule(initial)) }) { Text(stringResource(R.string.goals_schedule_activity)) }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                TextButton(enabled = !busy && title.isNotBlank(), onClick = {
                    onAction(TimelineAction.SaveGoalActivity(GoalActivity(id = initial?.id ?: 0, projectId = project.id, title = title.trim(), category = category,
                        start = start, end = end, note = note.trim().takeIf { it.isNotEmpty() }, isCasProject = casProject, isDone = done, isScheduled = initial?.isScheduled ?: false)))
                    onDismiss()
                }) { Text(stringResource(R.string.save)) }
                if (initial != null && initial.id > 0) {
                    Spacer(Modifier.weight(1f))
                    TextButton(enabled = !busy, onClick = { confirmingDelete = true }) { Text(stringResource(R.string.delete), color = RoutineColors.Crimson) }
                }
            }
        }
    }
    if (pickingStart) AppDatePicker(start, onDismiss = { pickingStart = false }, onDate = {
        if (it.toEpochDay() > endEpoch) endEpoch = it.toEpochDay(); startEpoch = it.toEpochDay(); pickingStart = false
    })
    if (pickingEnd) AppDatePicker(end, onDismiss = { pickingEnd = false }, onDate = {
        if (it.toEpochDay() < startEpoch) startEpoch = it.toEpochDay(); endEpoch = it.toEpochDay(); pickingEnd = false
    })
    if (confirmingDelete && initial != null) AlertDialog(onDismissRequest = { confirmingDelete = false },
        title = { Text(stringResource(R.string.goals_delete_activity_title)) },
        text = { Text(stringResource(R.string.goals_delete_activity_body, initial.title)) },
        confirmButton = { TextButton(enabled = !busy, onClick = { onAction(TimelineAction.DeleteGoalActivity(initial.id)); confirmingDelete = false; onDismiss() }) { Text(stringResource(R.string.delete), color = RoutineColors.Crimson) } },
        dismissButton = { TextButton(onClick = { confirmingDelete = false }) { Text(stringResource(R.string.keep)) } })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MilestoneEditorSheet(project: GoalsProject, initial: GoalMilestone?, busy: Boolean, onDismiss: () -> Unit, onAction: (TimelineAction) -> Unit) {
    var title by rememberSaveable(initial?.id) { mutableStateOf(initial?.title ?: "") }
    var epoch by rememberSaveable(initial?.id) { mutableStateOf((initial?.dueDate ?: LocalDate.now()).toEpochDay()) }
    var picking by rememberSaveable { mutableStateOf(false) }
    var confirmingDelete by rememberSaveable { mutableStateOf(false) }
    ModalBottomSheet(onDismissRequest = onDismiss, shape = RoutineShapes.Sheet, containerColor = RoutineColors.Surface1,
        tonalElevation = 0.dp, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(if (initial == null) R.string.goals_add_milestone else R.string.goals_edit_milestone), style = MaterialTheme.typography.headlineSmall)
            OutlinedTextField(title, { title = it.take(80) }, modifier = Modifier.fillMaxWidth(), singleLine = true, enabled = !busy,
                label = { Text(stringResource(R.string.goals_milestone_title)) })
            OutlinedButton(enabled = !busy, onClick = { picking = true }) { Text(LocalDate.ofEpochDay(epoch).format(GoalDateFormat)) }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                TextButton(enabled = !busy && title.isNotBlank(), onClick = {
                    onAction(TimelineAction.SaveGoalMilestone(GoalMilestone(id = initial?.id ?: 0, projectId = project.id, title = title.trim(),
                        dueDate = LocalDate.ofEpochDay(epoch), isDone = initial?.isDone ?: false)))
                    onDismiss()
                }) { Text(stringResource(R.string.save)) }
                if (initial != null && initial.id > 0) {
                    Spacer(Modifier.weight(1f))
                    TextButton(enabled = !busy, onClick = { confirmingDelete = true }) { Text(stringResource(R.string.delete), color = RoutineColors.Crimson) }
                }
            }
        }
    }
    if (picking) AppDatePicker(LocalDate.ofEpochDay(epoch), onDismiss = { picking = false }, onDate = { epoch = it.toEpochDay(); picking = false })
    if (confirmingDelete && initial != null) AlertDialog(onDismissRequest = { confirmingDelete = false },
        title = { Text(stringResource(R.string.goals_delete_milestone_title)) },
        text = { Text(stringResource(R.string.goals_delete_milestone_body, initial.title)) },
        confirmButton = { TextButton(enabled = !busy, onClick = { onAction(TimelineAction.DeleteGoalMilestone(initial.id)); confirmingDelete = false; onDismiss() }) { Text(stringResource(R.string.delete), color = RoutineColors.Crimson) } },
        dismissButton = { TextButton(onClick = { confirmingDelete = false }) { Text(stringResource(R.string.keep)) } })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProjectEditorSheet(initial: GoalsProject?, busy: Boolean, onDismiss: () -> Unit, onAction: (TimelineAction) -> Unit) {
    var name by rememberSaveable(initial?.id) { mutableStateOf(initial?.name ?: "") }
    var startEpoch by rememberSaveable(initial?.id) { mutableStateOf((initial?.start ?: LocalDate.now()).toEpochDay()) }
    var endEpoch by rememberSaveable(initial?.id) { mutableStateOf((initial?.end ?: LocalDate.now().plusMonths(18)).toEpochDay()) }
    var hours by rememberSaveable(initial?.id) { mutableStateOf(initial?.targetHours?.toString() ?: "150") }
    var words by rememberSaveable(initial?.id) { mutableStateOf(initial?.targetWords?.toString() ?: "") }
    var pickingStart by rememberSaveable { mutableStateOf(false) }
    var pickingEnd by rememberSaveable { mutableStateOf(false) }
    var confirmingDelete by rememberSaveable { mutableStateOf(false) }
    val start = LocalDate.ofEpochDay(startEpoch)
    val end = LocalDate.ofEpochDay(endEpoch).let { if (it.isBefore(start)) start else it }
    ModalBottomSheet(onDismissRequest = onDismiss, shape = RoutineShapes.Sheet, containerColor = RoutineColors.Surface1,
        tonalElevation = 0.dp, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.fillMaxWidth().padding(24.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(if (initial == null) R.string.goals_new_project_title else R.string.goals_edit_project), style = MaterialTheme.typography.headlineSmall)
            OutlinedTextField(name, { name = it.take(60) }, modifier = Modifier.fillMaxWidth(), singleLine = true, enabled = !busy,
                label = { Text(stringResource(R.string.goals_project_name)) })
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(modifier = Modifier.weight(1f), enabled = !busy, onClick = { pickingStart = true }) {
                    Text("${stringResource(R.string.goals_start_date)} · ${start.format(GoalShortFormat)}", maxLines = 1)
                }
                OutlinedButton(modifier = Modifier.weight(1f), enabled = !busy, onClick = { pickingEnd = true }) {
                    Text("${stringResource(R.string.goals_end_date)} · ${end.format(GoalShortFormat)}", maxLines = 1)
                }
            }
            OutlinedTextField(hours, { hours = it.filter { c -> c.isDigit() || c == '.' }.take(7) }, modifier = Modifier.fillMaxWidth(), singleLine = true, enabled = !busy,
                label = { Text(stringResource(R.string.goals_target_hours)) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
            OutlinedTextField(words, { words = it.filter(Char::isDigit).take(5) }, modifier = Modifier.fillMaxWidth(), singleLine = true, enabled = !busy,
                label = { Text(stringResource(R.string.goals_target_words)) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
            Text(stringResource(R.string.goals_period_note), style = MaterialTheme.typography.labelSmall, color = RoutineColors.Warning)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                TextButton(enabled = !busy && name.isNotBlank(), onClick = {
                    onAction(TimelineAction.SaveGoalsProject(GoalsProject(id = initial?.id ?: 0, name = name.trim(), kind = initial?.kind ?: "CUSTOM",
                        start = start, end = end, targetHours = hours.trim().toDoubleOrNull(), targetWords = words.trim().toIntOrNull())))
                    onDismiss()
                }) { Text(stringResource(R.string.save)) }
                if (initial != null && initial.id > 0) {
                    Spacer(Modifier.weight(1f))
                    TextButton(enabled = !busy, onClick = { confirmingDelete = true }) { Text(stringResource(R.string.delete), color = RoutineColors.Crimson) }
                }
            }
        }
    }
    if (pickingStart) AppDatePicker(start, onDismiss = { pickingStart = false }, onDate = {
        if (it.toEpochDay() > endEpoch) endEpoch = it.toEpochDay(); startEpoch = it.toEpochDay(); pickingStart = false
    })
    if (pickingEnd) AppDatePicker(end, onDismiss = { pickingEnd = false }, onDate = {
        if (it.toEpochDay() < startEpoch) startEpoch = it.toEpochDay(); endEpoch = it.toEpochDay(); pickingEnd = false
    })
    if (confirmingDelete && initial != null) AlertDialog(onDismissRequest = { confirmingDelete = false },
        title = { Text(stringResource(R.string.goals_delete_project_title)) },
        text = { Text(stringResource(R.string.goals_delete_project_body, initial.name)) },
        confirmButton = { TextButton(enabled = !busy, onClick = { onAction(TimelineAction.DeleteGoalsProject(initial.id)); confirmingDelete = false; onDismiss() }) { Text(stringResource(R.string.delete), color = RoutineColors.Crimson) } },
        dismissButton = { TextButton(onClick = { confirmingDelete = false }) { Text(stringResource(R.string.keep)) } })
}

private fun logProgress(projectId: Long, activityId: Long, hours: Double): TimelineAction =
    TimelineAction.AddGoalProgress(GoalProgress(projectId = projectId, activityId = activityId, kind = "hour", amount = hours, date = LocalDate.now()))

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

private fun categoryColor(category: String?): Color = when (category) {
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
