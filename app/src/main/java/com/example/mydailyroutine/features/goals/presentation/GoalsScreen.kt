package com.example.mydailyroutine.features.goals.presentation

import android.content.Context
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.ui.unit.IntOffset
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.mydailyroutine.R
import androidx.annotation.StringRes
import com.example.mydailyroutine.core.designsystem.components.ActionRow
import com.example.mydailyroutine.core.designsystem.components.CategoryTabs
import com.example.mydailyroutine.core.designsystem.components.RoutineLabel
import com.example.mydailyroutine.core.designsystem.components.RoutineSheet
import com.example.mydailyroutine.core.designsystem.components.RoutineSheetScaffold
import com.example.mydailyroutine.core.designsystem.components.RoutineText
import com.example.mydailyroutine.core.designsystem.components.RoutineTextDefaults
import com.example.mydailyroutine.core.designsystem.components.SectionHeader
import com.example.mydailyroutine.core.designsystem.components.SheetPrimaryButton
import com.example.mydailyroutine.core.designsystem.components.SheetSecondaryButton
import com.example.mydailyroutine.core.designsystem.haptics.LocalRoutineHaptics
import com.example.mydailyroutine.core.designsystem.theme.CategoryStyle
import com.example.mydailyroutine.core.designsystem.motion.routineItemAnimation
import com.example.mydailyroutine.core.designsystem.theme.RoutineColors
import com.example.mydailyroutine.core.designsystem.theme.RoutineMetrics
import com.example.mydailyroutine.core.designsystem.theme.RoutineShapes
import com.example.mydailyroutine.core.designsystem.theme.RoutineSpacing
import com.example.mydailyroutine.core.designsystem.motion.LocalReduceMotion
import com.example.mydailyroutine.core.designsystem.motion.effectSpec
import com.example.mydailyroutine.core.designsystem.motion.spatialSpec
import com.example.mydailyroutine.core.designsystem.theme.SnappySpring
import com.example.mydailyroutine.core.presentation.TimelineAction
import com.example.mydailyroutine.core.presentation.RoutineDate
import com.example.mydailyroutine.domain.model.GoalActivity
import com.example.mydailyroutine.domain.model.GoalMilestone
import com.example.mydailyroutine.domain.model.GoalProgress
import com.example.mydailyroutine.domain.model.GoalsProject
import com.example.mydailyroutine.core.presentation.GoalsUiState
import com.example.mydailyroutine.core.presentation.durationLabel
import com.example.mydailyroutine.core.presentation.interfaceLocale
import java.text.NumberFormat
import java.util.Locale
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit
import kotlin.math.roundToInt


/** Width of the project-name column in the Gantt; the timeline itself scrolls when it needs more. */
private val GanttLaneLabel = 84.dp

/** The two starter projects the app knows how to lay out, in the order the empty state offers them. */
private val StarterKinds = listOf("CAS", "EE")

/**
 * The project row's "every plan" option. CAS and EE run in parallel for the whole two years, so a
 * screen that can only ever show one of them answers a question nobody asked; the sentinel is a
 * value no project id can take, which keeps `null` meaning "nothing chosen yet".
 */
private const val AllProjects = -1L

/**
 * The action that creates the CAS or EE starter plan. Shared by the empty state and by the "the other
 * one is still missing" chip, so both paths produce exactly the same project — the reason a student
 * who started with CAS can still get EE a week later.
 */
private fun starterAction(context: Context, kind: String): TimelineAction = when (kind) {
    "EE" -> TimelineAction.SeedGoalProject(
        kind, context.getString(R.string.goals_seed_ee_name),
        listOf(R.string.ee_stage_1, R.string.ee_stage_2, R.string.ee_stage_3, R.string.ee_stage_4,
            R.string.ee_stage_5, R.string.ee_stage_6).map { context.getString(it) },
        listOf(R.string.ee_milestone_1, R.string.ee_milestone_2, R.string.ee_milestone_3,
            R.string.ee_milestone_4, R.string.ee_milestone_5).map { context.getString(it) },
    )
    else -> TimelineAction.SeedGoalProject(
        "CAS", context.getString(R.string.goals_seed_cas_name), emptyList(),
        listOf(R.string.goals_cas_meeting_1, R.string.goals_cas_meeting_2, R.string.goals_cas_meeting_3,
            R.string.goals_cas_statement).map { context.getString(it) },
    )
}

/**
 * Full-screen long-term planner for CAS/EE: status, month plan, activities, milestones.
 *
 * Every sheet here renders through [RoutineSheetScaffold], so the goals editors are pixel-identical
 * to the fast-add sheet: same header, same padding, same sticky footer, same button hierarchy.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun GoalsScreen(goals: GoalsUiState, busy: Boolean, onAction: (TimelineAction) -> Unit, topInset: Dp = 0.dp,
    bottomInset: Dp = RoutineMetrics.ListBottomInset) {
    val haptics = LocalRoutineHaptics.current
    var selectedId by rememberSaveable { mutableStateOf<Long?>(null) }
    var editingActivity by remember { mutableStateOf<GoalActivity?>(null) }
    var addingActivity by remember { mutableStateOf(false) }
    var editingMilestone by remember { mutableStateOf<GoalMilestone?>(null) }
    var addingMilestone by remember { mutableStateOf(false) }
    var editingProject by remember { mutableStateOf<GoalsProject?>(null) }
    var addingProject by remember { mutableStateOf(false) }
    var tab by rememberSaveable { mutableStateOf(GoalTab.OVERVIEW) }
    // From the second plan on, the screen opens on both at once; a single plan is its own whole view.
    val showAll = selectedId == AllProjects || selectedId == null && goals.projects.size > 1
    val visible = if (showAll) goals.projects
        else listOfNotNull(goals.projects.firstOrNull { it.id == selectedId } ?: goals.projects.lastOrNull())
    val project = visible.lastOrNull()
    val visibleIds = visible.map { it.id }.toSet()
    val reduceMotion = LocalReduceMotion.current
    AnimatedContent(
        targetState = project == null,
        label = "goals-swap",
        modifier = Modifier.fillMaxSize(),
        transitionSpec = {
            (fadeIn(effectSpec<Float>(reduceMotion)) + slideInVertically(spatialSpec<IntOffset>(reduceMotion)) { it / 10 }) togetherWith
                fadeOut(effectSpec<Float>(reduceMotion, 120))
        },
    ) { isEmpty ->
        if (isEmpty) {
            Box(Modifier.fillMaxSize().padding(top = topInset)) {
                GoalEmptyState(busy, onAction) { haptics.press(); addingProject = true }
            }
        } else {
            val context = LocalContext.current
            Column(Modifier.fillMaxSize()) {
                // Projects scroll sideways: a wrapping chip row grew to four lines with three projects.
                LazyRow(
                    Modifier.fillMaxWidth().padding(top = topInset + RoutineSpacing.sm),
                    contentPadding = PaddingValues(horizontal = RoutineMetrics.ScreenPadding),
                    horizontalArrangement = Arrangement.spacedBy(RoutineSpacing.sm),
                ) {
                    if (goals.projects.size > 1) {
                        item(key = "all") {
                            FilterChip(
                                selected = showAll,
                                onClick = { haptics.selection(); selectedId = AllProjects; tab = GoalTab.OVERVIEW },
                                enabled = !busy,
                                shape = RoutineShapes.Chip,
                                modifier = Modifier.testTag("goal-project-all"),
                                label = {
                                    RoutineLabel(stringResource(R.string.goals_all_projects),
                                        style = MaterialTheme.typography.labelLarge)
                                },
                            )
                        }
                    }
                    items(goals.projects, key = { it.id }) { candidate ->
                        FilterChip(
                            selected = !showAll && project?.id == candidate.id,
                            onClick = { haptics.selection(); selectedId = candidate.id; tab = GoalTab.OVERVIEW },
                            enabled = !busy,
                            shape = RoutineShapes.Chip,
                            modifier = routineItemAnimation().testTag("goal-project-${candidate.id}"),
                            label = { RoutineLabel(candidate.name, style = MaterialTheme.typography.labelLarge) },
                        )
                    }
                    // Whatever starter is still missing keeps its offer here, in the same row the
                    // reader uses to move between projects. Without this, a student who made CAS on
                    // the first screen could never find the EE plan again.
                    val missing = StarterKinds.filter { kind -> goals.projects.none { it.kind == kind } }
                    items(missing, key = { "seed-$it" }) { kind ->
                        SuggestionChip(
                            onClick = { haptics.press(); onAction(starterAction(context, kind)) },
                            enabled = !busy,
                            shape = RoutineShapes.Chip,
                            modifier = routineItemAnimation().testTag("goal-seed-$kind"),
                            label = {
                                RoutineLabel(
                                    stringResource(R.string.goals_add_starter, kind),
                                    style = MaterialTheme.typography.labelLarge,
                                )
                            },
                        )
                    }
                    item {
                        SuggestionChip(
                            onClick = { haptics.press(); addingProject = true },
                            enabled = !busy,
                            shape = RoutineShapes.Chip,
                            modifier = Modifier.testTag("goal-project-new"),
                            label = { RoutineLabel(stringResource(R.string.goals_new_project), style = MaterialTheme.typography.labelLarge) },
                        )
                    }
                }
                if (project != null) {
                    val activities = goals.activities.filter { it.projectId in visibleIds }
                    val milestones = goals.milestones.filter { it.projectId in visibleIds }.sortedBy { it.dueDate }
                    val progress = goals.progress.filter { it.projectId in visibleIds }
                    // In the both-plans view every row names its plan: two different milestones can
                    // carry the same word, and a list that hides where a row comes from is a riddle.
                    val planName: (Long) -> String? =
                        { id -> if (showAll) goals.projects.firstOrNull { it.id == id }?.name else null }
                    CategoryTabs(
                        entries = GoalTab.entries.toList(),
                        selected = tab,
                        label = { stringResource(it.labelRes) },
                        onSelect = { haptics.selection(); tab = it },
                        modifier = Modifier.padding(horizontal = RoutineMetrics.ScreenPadding, vertical = RoutineSpacing.sm),
                        tagPrefix = "goal-tab",
                        enabled = !busy,
                    )
                    LazyColumn(
                        Modifier.fillMaxSize().testTag("goal-tab-body"),
                        contentPadding = PaddingValues(RoutineMetrics.ScreenPadding, RoutineSpacing.xs,
                            RoutineMetrics.ScreenPadding, bottomInset),
                        verticalArrangement = Arrangement.spacedBy(RoutineSpacing.md),
                    ) {
                        when (tab) {
                            GoalTab.OVERVIEW -> {
                                visible.forEach { plan ->
                                    item(key = "status-${plan.id}") {
                                        StatusCard(plan,
                                            activities = goals.activities.filter { it.projectId == plan.id },
                                            milestones = goals.milestones.filter { it.projectId == plan.id },
                                            progress = goals.progress.filter { it.projectId == plan.id },
                                            busy = busy,
                                            onEdit = { haptics.press(); editingProject = plan },
                                            onAction = onAction)
                                    }
                                }
                                item(key = "gantt") {
                                    Card(
                                        Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(containerColor = RoutineColors.Surface1),
                                        shape = RoutineShapes.Card,
                                    ) {
                                        Column(
                                            Modifier.fillMaxWidth().padding(vertical = RoutineSpacing.md),
                                            verticalArrangement = Arrangement.spacedBy(RoutineSpacing.sm),
                                        ) {
                                            RoutineText(
                                                text = stringResource(R.string.goals_gantt),
                                                style = MaterialTheme.typography.titleMedium,
                                                modifier = Modifier.padding(horizontal = RoutineSpacing.md),
                                                maxLines = RoutineTextDefaults.Body,
                                            )
                                            GoalGantt(goals.projects, goals.activities, goals.milestones,
                                                if (showAll) null else project.id) { activity ->
                                                haptics.press()
                                                goals.projects.firstOrNull { it.id == activity.projectId }?.let { selectedId = it.id }
                                                editingActivity = activity
                                            }
                                        }
                                    }
                                }
                            }
                            GoalTab.ACTIVITIES -> item(key = "activities") {
                                ActivityList(activities, progress, busy, onAction,
                                    onAdd = { haptics.press(); addingActivity = true },
                                    onEdit = { haptics.press(); editingActivity = it },
                                    projectNameOf = planName)
                            }
                            GoalTab.MILESTONES -> item(key = "milestones") {
                                MilestoneList(milestones, busy, onAction,
                                    onAdd = { haptics.press(); addingMilestone = true },
                                    onEdit = { haptics.press(); editingMilestone = it },
                                    projectNameOf = planName)
                            }
                            GoalTab.PROGRESS -> item(key = "progress") {
                                ProgressLog(progress, activities, busy, onAction, projectNameOf = planName)
                            }
                        }
                    }
                }
            }
        }
    }
    project?.let { current ->
        ActivityEditorHost(addingActivity || editingActivity != null, goals.projects, current, editingActivity, busy, goals.progress,
            onDismiss = { addingActivity = false; editingActivity = null }, onAction = onAction)
        MilestoneEditorHost(addingMilestone || editingMilestone != null, goals.projects, current, editingMilestone, busy,
            onDismiss = { addingMilestone = false; editingMilestone = null }, onAction = onAction)
    }
    ProjectEditorHost(addingProject || editingProject != null, editingProject, busy,
        onDismiss = { addingProject = false; editingProject = null }, onAction = onAction)
}

/**
 * The goals sheets keep the shared animated exit of [com.example.mydailyroutine.core.designsystem.components.RoutineSheet]
 * by mounting through the same host pattern used across the app.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ActivityEditorHost(
    visible: Boolean,
    projects: List<GoalsProject>,
    preferred: GoalsProject,
    initial: GoalActivity?,
    busy: Boolean,
    progress: List<GoalProgress>,
    onDismiss: () -> Unit,
    onAction: (TimelineAction) -> Unit,
) {
    // Which plan the sheet writes to. An existing activity keeps its own plan; a new one starts in
    // the plan the reader came from and can be pointed elsewhere before it is saved.
    var targetId by rememberSaveable(visible, initial?.id) { mutableStateOf(initial?.projectId ?: preferred.id) }
    val target = projects.firstOrNull { it.id == targetId } ?: preferred
    RoutineSheet(visible) { sheetState ->
        ActivityEditorSheet(target, projects, { targetId = it.id }, initial, busy, progress, sheetState, onDismiss, onAction)
    }
}

/**
 * Picks the plan a new activity or milestone belongs to, in the same chip vocabulary as the screen
 * behind it. It only appears when there is a choice: with one plan the question has one answer.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ProjectPicker(
    projects: List<GoalsProject>,
    selectedId: Long,
    enabled: Boolean,
    onPick: (GoalsProject) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(RoutineSpacing.xs)) {
        RoutineLabel(stringResource(R.string.goals_project_label), style = MaterialTheme.typography.labelMedium,
            color = RoutineColors.TextSecondary)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(RoutineSpacing.sm),
            verticalArrangement = Arrangement.spacedBy(RoutineSpacing.xs),
        ) {
            projects.forEach { candidate ->
                FilterChip(
                    selected = candidate.id == selectedId,
                    onClick = { onPick(candidate) },
                    enabled = enabled,
                    shape = RoutineShapes.Chip,
                    modifier = Modifier.testTag("goal-target-${candidate.kind}"),
                    label = { RoutineLabel(candidate.name, style = MaterialTheme.typography.labelLarge) },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MilestoneEditorHost(
    visible: Boolean,
    projects: List<GoalsProject>,
    preferred: GoalsProject,
    initial: GoalMilestone?,
    busy: Boolean,
    onDismiss: () -> Unit,
    onAction: (TimelineAction) -> Unit,
) {
    var targetId by rememberSaveable(visible, initial?.id) { mutableStateOf(initial?.projectId ?: preferred.id) }
    val target = projects.firstOrNull { it.id == targetId } ?: preferred
    RoutineSheet(visible) { sheetState ->
        MilestoneEditorSheet(target, projects, { targetId = it.id }, initial, busy, sheetState, onDismiss, onAction)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProjectEditorHost(
    visible: Boolean,
    initial: GoalsProject?,
    busy: Boolean,
    onDismiss: () -> Unit,
    onAction: (TimelineAction) -> Unit,
) {
    RoutineSheet(visible) { sheetState ->
        ProjectEditorSheet(initial, busy, sheetState, onDismiss, onAction)
    }
}

/**
 * The goals screen used to be one ~1000 dp scroll: status card, Gantt, every activity and every
 * milestone stacked on top of each other. Splitting it into tabs keeps each view short enough to
 * survey at a glance; the project selector above the tabs stays put.
 */
private enum class GoalTab(@StringRes val labelRes: Int) {
    OVERVIEW(R.string.goals_tab_overview),
    ACTIVITIES(R.string.goals_activities),
    MILESTONES(R.string.goals_milestones),
    PROGRESS(R.string.goals_tab_progress),
}

/**
 * Everything ever logged for the project, newest first. The status card shows the totals; this is
 * the trail behind them, and the only place reflections are readable without opening an activity.
 */
@Composable
private fun ProgressLog(
    progress: List<GoalProgress>,
    activities: List<GoalActivity>,
    busy: Boolean,
    onAction: (TimelineAction) -> Unit,
    projectNameOf: (Long) -> String?,
) {
    val entries = progress.sortedWith(compareByDescending<GoalProgress> { it.date }.thenByDescending { it.id })
    Column(verticalArrangement = Arrangement.spacedBy(RoutineSpacing.sm)) {
        SectionHeader(title = stringResource(R.string.goals_progress_log), style = MaterialTheme.typography.titleLarge)
        if (entries.isEmpty()) {
            RoutineText(stringResource(R.string.goals_progress_empty), style = MaterialTheme.typography.bodyMedium,
                color = RoutineColors.TextSecondary, maxLines = RoutineTextDefaults.Paragraph)
        }
        entries.forEach { entry ->
            OutlinedCard(
                shape = RoutineShapes.Card,
                border = BorderStroke(1.dp, RoutineColors.CardBorder),
                modifier = Modifier.fillMaxWidth().testTag("goal-progress-${entry.id}"),
            ) {
                Column(
                    Modifier.fillMaxWidth().padding(RoutineSpacing.md),
                    verticalArrangement = Arrangement.spacedBy(RoutineSpacing.xs),
                ) {
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(RoutineSpacing.sm),
                    ) {
                        RoutineLabel(RoutineDate.normal(entry.date), style = MaterialTheme.typography.labelSmall,
                            color = RoutineColors.TextSecondary)
                        RoutineLabel(
                            when (entry.kind) {
                                "word" -> stringResource(R.string.goals_progress_words, entry.amount.toInt())
                                "reflection" -> stringResource(R.string.goals_progress_kind_reflection)
                                else -> goalsMinutesLabel(entry.amount)
                            },
                            Modifier.weight(1f),
                            style = MaterialTheme.typography.labelMedium,
                        )
                        if (entry.kind != "reflection") {
                            TextButton(enabled = !busy, onClick = { onAction(TimelineAction.DeleteGoalProgress(entry.id)) }) {
                                RoutineLabel(stringResource(R.string.delete), style = MaterialTheme.typography.labelLarge,
                                    color = RoutineColors.Error)
                            }
                        }
                    }
                    val where = listOfNotNull(projectNameOf(entry.projectId),
                        activities.firstOrNull { it.id == entry.activityId }?.title).joinToString(" · ")
                    if (where.isNotEmpty()) {
                        RoutineLabel(where, style = MaterialTheme.typography.labelSmall, color = RoutineColors.TextMuted)
                    }
                    if (entry.kind == "reflection") {
                        RoutineText(entry.note.orEmpty(), style = MaterialTheme.typography.bodySmall,
                            maxLines = RoutineTextDefaults.Paragraph)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GoalEmptyState(busy: Boolean, onAction: (TimelineAction) -> Unit, onNewCustom: () -> Unit) {
    val context = LocalContext.current
    Column(
        Modifier.fillMaxSize().padding(RoutineSpacing.xl),
        verticalArrangement = Arrangement.spacedBy(RoutineSpacing.md),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.weight(1f))
        RoutineText(stringResource(R.string.goals_title), style = MaterialTheme.typography.headlineMedium,
            maxLines = RoutineTextDefaults.Body, heading = true)
        RoutineText(stringResource(R.string.goals_empty_body), style = MaterialTheme.typography.bodyMedium,
            color = RoutineColors.TextSecondary, maxLines = RoutineTextDefaults.Paragraph)
        StarterKinds.forEach { kind ->
            SheetPrimaryButton(
                label = stringResource(if (kind == "EE") R.string.goals_seed_ee else R.string.goals_seed_cas),
                enabled = !busy,
                onClick = { onAction(starterAction(context, kind)) },
            )
        }
        SheetSecondaryButton(label = stringResource(R.string.goals_new_project), enabled = !busy, onClick = onNewCustom)
        Spacer(Modifier.weight(1f))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StatusCard(
    project: GoalsProject,
    activities: List<GoalActivity>,
    milestones: List<GoalMilestone>,
    progress: List<GoalProgress>,
    busy: Boolean,
    onEdit: () -> Unit,
    onAction: (TimelineAction) -> Unit,
) {
    val hours = progress.filter { it.kind == "hour" }.sumOf { it.amount }
    val words = progress.filter { it.kind == "word" }.sumOf { it.amount.toInt() }
    val next = milestones.filter { !it.isDone }.minByOrNull { it.dueDate }
    val categoryHours = progress.filter { it.kind == "hour" }
        .groupBy { entry -> activities.firstOrNull { it.id == entry.activityId }?.category }
    OutlinedCard(
        // Named by kind and id: the kind is what a reader would call the plan, the id keeps two
        // hand-made plans apart. Nothing but a test reads this tag.
        Modifier.fillMaxWidth().testTag("goal-status-${project.kind}-${project.id}"),
        shape = RoutineShapes.Card,
        border = BorderStroke(1.dp, RoutineColors.CardBorder),
    ) {
        Column(
            Modifier.fillMaxWidth().padding(RoutineSpacing.md),
            verticalArrangement = Arrangement.spacedBy(RoutineSpacing.sm),
        ) {
            SectionHeader(
                title = project.name,
                style = MaterialTheme.typography.titleLarge,
                action = {
                    TextButton(enabled = !busy, onClick = onEdit) {
                        RoutineLabel(stringResource(R.string.edit), style = MaterialTheme.typography.labelLarge)
                    }
                },
            )
            RoutineLabel(
                stringResource(R.string.date_range, RoutineDate.normalYear(project.start), RoutineDate.normalYear(project.end)),
                style = MaterialTheme.typography.bodySmall,
                color = RoutineColors.TextSecondary,
            )
            val hourTarget = project.targetHours
            if (hourTarget != null) {
                GoalBar((hours / hourTarget).toFloat(), RoutineColors.Primary)
                val recorded = goalsHoursLabel(hours)
                RoutineLabel(stringResource(R.string.goals_hours_total, recorded, hourTarget.roundToInt()),
                    style = MaterialTheme.typography.labelMedium)
            }
            if (project.kind == "CAS") {
                Row(horizontalArrangement = Arrangement.spacedBy(RoutineSpacing.sm)) {
                    listOf(
                        "CREATIVITY" to R.string.goals_category_creativity,
                        "ACTIVITY" to R.string.goals_category_activity,
                        "SERVICE" to R.string.goals_category_service,
                    ).forEach { (category, res) ->
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(RoutineSpacing.xs)) {
                            RoutineLabel(stringResource(res), style = MaterialTheme.typography.labelSmall,
                                color = goalCategoryStyle(category).accent)
                            RoutineLabel(
                                // Divided by 60 again: the label takes a duration in minutes, the
                                // same unit the "+30 min" button writes.
                                goalsMinutesLabel(categoryHours[category]?.sumOf { it.amount } ?: 0.0),
                                style = MaterialTheme.typography.labelMedium,
                            )
                        }
                    }
                }
                if (listOf("CREATIVITY", "ACTIVITY", "SERVICE")
                        .any { (categoryHours[it]?.sumOf { entry -> entry.amount } ?: 0.0) == 0.0 }) {
                    RoutineText(stringResource(R.string.goals_balance_missing), style = MaterialTheme.typography.labelSmall,
                        color = RoutineColors.Warning, maxLines = RoutineTextDefaults.Paragraph)
                }
            }
            val wordTarget = project.targetWords
            if (project.kind == "EE" && wordTarget != null) {
                GoalBar((words.toFloat() / wordTarget), RoutineColors.Timer)
                RoutineLabel(stringResource(R.string.goals_words_total, words, wordTarget),
                    style = MaterialTheme.typography.labelMedium)
                // Word steps reflow at full label size; they never share a line with the counter.
                ActionRow {
                    listOf(100, 250, 500).forEach { step ->
                        OutlinedButton(
                            enabled = !busy,
                            shape = RoutineShapes.Pill,
                            onClick = {
                                onAction(
                                    TimelineAction.AddGoalProgress(
                                        GoalProgress(projectId = project.id, activityId = null, kind = "word",
                                            amount = step.toDouble(), date = LocalDate.now()),
                                    ),
                                )
                            },
                        ) { RoutineLabel(stringResource(R.string.goals_words_step, step), style = MaterialTheme.typography.labelLarge) }
                    }
                }
            }
            if (next != null) {
                val days = ChronoUnit.DAYS.between(LocalDate.now(), next.dueDate)
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(RoutineSpacing.sm),
                ) {
                    Box(
                        Modifier.size(RoutineSpacing.sm).clip(CircleShape).background(
                            when {
                                days < 0 -> RoutineColors.Error
                                // The warning yellow, not the brand turquoise: this dot is the
                                // middle step of an urgency traffic light, and the brand colour
                                // must never double as a warning.
                                days <= 14 -> RoutineColors.Warning
                                else -> RoutineColors.Success
                            },
                        ),
                    )
                    Column(Modifier.weight(1f)) {
                        RoutineLabel(stringResource(R.string.goals_next_milestone),
                            style = MaterialTheme.typography.labelMedium, color = RoutineColors.TextSecondary)
                        RoutineText(next.title, style = MaterialTheme.typography.titleSmall,
                            maxLines = RoutineTextDefaults.Body)
                    }
                    RoutineLabel(
                        goalRelative(next.dueDate),
                        style = MaterialTheme.typography.labelMedium,
                        color = if (days < 0) RoutineColors.Error else RoutineColors.TextSecondary,
                    )
                }
            } else if (milestones.isEmpty()) {
                RoutineText(stringResource(R.string.goals_milestones_empty), style = MaterialTheme.typography.bodySmall,
                    color = RoutineColors.TextSecondary, maxLines = RoutineTextDefaults.Paragraph)
            } else {
                RoutineLabel(stringResource(R.string.goals_all_milestones_done), style = MaterialTheme.typography.labelMedium,
                    color = RoutineColors.Success)
            }
            RoutineLabel(stringResource(R.string.goals_activity_progress, activities.count { it.isDone }, activities.size),
                style = MaterialTheme.typography.labelMedium, color = RoutineColors.TextSecondary)
            if (project.kind == "EE") {
                RoutineText(stringResource(R.string.goals_period_note), style = MaterialTheme.typography.labelSmall,
                    color = RoutineColors.Warning, maxLines = RoutineTextDefaults.Paragraph)
            }
        }
    }
}

@Composable
private fun GoalBar(fraction: Float, color: Color) {
    var shown by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(fraction) { shown = fraction }
    val width by animateFloatAsState(shown.coerceIn(0f, 1f),
        if (LocalReduceMotion.current) snap<Float>() else SnappySpring, label = "goal-progress")
    Box(Modifier.fillMaxWidth().height(RoutineSpacing.sm).clip(RoutineShapes.Chip).background(RoutineColors.Surface2)) {
        Box(Modifier.fillMaxWidth(width.coerceIn(0f, 1f)).height(RoutineSpacing.sm).clip(RoutineShapes.Chip).background(color))
    }
}

/**
 * Month plan for all projects on one shared day scale.
 *
 * Positioning is measured, not offset: milestone diamonds live in their own strip above the bars, and
 * every bar is measured with exactly the width its dates own. A bar that is too narrow for a label
 * shows colour only and keeps the title in its accessible description, so two activities can never
 * print over each other and a short activity never turns into a lone ellipsis.
 */
@Composable
private fun GoalGantt(
    projects: List<GoalsProject>,
    activities: List<GoalActivity>,
    milestones: List<GoalMilestone>,
    selectedId: Long?,
    onActivity: (GoalActivity) -> Unit,
) {
    if (projects.isEmpty()) return
    val today = LocalDate.now()
    val start = projects.minOf { it.start }
    val end = projects.maxOf { it.end }
    val spanDays = maxOf(1L, ChronoUnit.DAYS.between(start, end))
    val monthCount = (spanDays / 31 + 1).toInt().coerceIn(2, 30)
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val cellWidth = ((maxWidth - GanttLaneLabel - RoutineSpacing.sm) / monthCount).coerceAtLeast(28.dp)
        val timelineWidth = cellWidth * monthCount
        fun xOf(date: LocalDate): Dp {
            val days = ChronoUnit.DAYS.between(start, date).toFloat().coerceIn(0f, spanDays.toFloat())
            return timelineWidth * (days / spanDays.toFloat())
        }
        // Days where two or more projects both plan open work get a soft crimson band.
        val busyWindows = projects.mapNotNull { project ->
            val open = activities.filter { it.projectId == project.id && !it.isDone }
            if (open.isEmpty()) return@mapNotNull null // nothing planned here — never claim an overlap
            val from = maxOf(open.minOf { it.start }, project.start)
            val to = minOf(open.maxOf { it.end }, project.end)
            if (to < from) null else from to to
        }
        val overlapRuns = if (projects.size < 2) emptyList() else run {
            val days = mutableListOf<LocalDate>()
            var day = start
            while (!day.isAfter(end)) {
                if (busyWindows.count { !day.isBefore(it.first) && !day.isAfter(it.second) } > 1) days += day
                day = day.plusDays(1)
            }
            days.fold(mutableListOf<Pair<LocalDate, LocalDate>>()) { runs, current ->
                val last = runs.lastOrNull()
                if (last != null && last.second == current.minusDays(1)) runs[runs.lastIndex] = last.first to current
                else runs.add(current to current)
                runs
            }
        }
        val showEveryMonth = cellWidth >= 44.dp
        Column(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
            val laneRows = projects.map { project -> packActivityRows(activities.filter { it.projectId == project.id }) }
            // Header: one equal cell per month, so labels can never collide.
            Row(Modifier.width(GanttLaneLabel + timelineWidth)) {
                Spacer(Modifier.width(GanttLaneLabel))
                for (index in 0 until monthCount) {
                    if (!showEveryMonth && index % 2 == 1) continue
                    Box(Modifier.width(if (showEveryMonth) cellWidth else cellWidth * 2)) {
                        RoutineLabel(
                            RoutineDate.axisLabel(YearMonth.from(start).plusMonths(index.toLong())),
                            modifier = Modifier.padding(start = RoutineSpacing.xs),
                            style = MaterialTheme.typography.labelSmall,
                            color = RoutineColors.TextSecondary,
                        )
                    }
                }
            }
            projects.forEachIndexed { laneIndex, lane ->
                val rows = laneRows[laneIndex]
                val laneMilestones = milestones.filter { it.projectId == lane.id }
                Row(Modifier.width(GanttLaneLabel + timelineWidth)) {
                    Column(
                        Modifier.width(GanttLaneLabel).height(laneHeight(rows))
                            .padding(start = RoutineSpacing.md, end = RoutineSpacing.sm),
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(RoutineSpacing.xs)) {
                            Box(Modifier.size(RoutineMetrics.DotSize).clip(CircleShape)
                                .background(goalCategoryStyle(if (lane.kind == "CAS") "CREATIVITY" else "STAGE").accent))
                            // A lane label names a project, so it wraps to a second line instead of
                            // ending in an ellipsis: "Extended essay" is two words in a 64 dp column,
                            // and losing the second one would leave the reader with "Extended ...".
                            RoutineLabel(
                                lane.name,
                                style = MaterialTheme.typography.labelMedium,
                                // null means every plan is on screen, so every lane is equally current.
                                color = if (selectedId == null || lane.id == selectedId) RoutineColors.TextPrimary
                                    else RoutineColors.TextSecondary,
                                maxLines = RoutineTextDefaults.Body,
                            )
                        }
                    }
                    Column(
                        Modifier.width(timelineWidth).height(laneHeight(rows)).drawBehind {
                            overlapRuns.forEach { (from, to) ->
                                val left = xOf(from).toPx()
                                val right = xOf(to.plusDays(1)).toPx().coerceAtMost(size.width)
                                if (right > left) {
                                    drawRect(RoutineColors.Error.copy(alpha = 0.07f),
                                        Offset(left, 0f), androidx.compose.ui.geometry.Size(right - left, size.height))
                                }
                            }
                            if (!today.isBefore(start) && !today.isAfter(end)) {
                                val x = xOf(today).toPx()
                                drawRect(RoutineColors.Primary.copy(alpha = 0.6f), Offset(x, 0f),
                                    androidx.compose.ui.geometry.Size(2.dp.toPx(), size.height))
                            }
                        },
                    ) {
                        GanttMilestoneStrip(laneMilestones, Modifier.fillMaxWidth().height(GanttStripHeight), xOf = ::xOf)
                        GanttBars(
                            rows = rows,
                            modifier = Modifier.fillMaxWidth().height(laneHeight(rows) - GanttStripHeight),
                            xOf = ::xOf,
                            onActivity = onActivity,
                        )
                    }
                }
            }
        }
    }
}

private val GanttStripHeight = 18.dp

/**
 * Vertical rhythm of one Gantt lane. One geometry for both the lane and the bars inside it, because
 * they used to be two: the lane reserved 28 dp for a single row while a bar was measured 20 dp tall
 * with 4 dp of its own padding, so a bar's bottom edge sat *exactly* on the next row's top edge. Two
 * activities in one lane were drawn touching, and the next lane's first bar sat against the previous
 * lane's last one — the reader called that "occasionally one thing is hidden under another".
 */
private val GanttBarHeight = 20.dp
private val GanttBarGap = 6.dp
private val GanttBarsTopPad = 4.dp

private fun ganttRowStep(): Dp = GanttBarHeight + GanttBarGap

private fun laneHeight(rows: List<List<GoalActivity>>): Dp =
    GanttStripHeight + GanttBarsTopPad + ganttRowStep() * rows.size.coerceAtLeast(1)

/** Milestone diamonds: measured placement inside their own strip, above the bars. */
@Composable
private fun GanttMilestoneStrip(milestones: List<GoalMilestone>, modifier: Modifier = Modifier, xOf: (LocalDate) -> Dp) {
    Layout(
        modifier = modifier,
        content = {
            milestones.forEach { milestone ->
                Box(
                    Modifier.size(RoutineMetrics.DiamondSize).rotate(45f)
                        .background(
                            if (milestone.isDone) RoutineColors.TextDisabled else RoutineColors.Error,
                            RoutineShapes.Diamond,
                        )
                        .semantics { contentDescription = RoutineDate.normal(milestone.dueDate) },
                )
            }
        },
    ) { measurables, constraints ->
        val side = RoutineMetrics.DiamondSize.roundToPx()
        val placed = measurables.mapIndexed { index, measurable ->
            val placeable = measurable.measure(Constraints.fixed(side, side))
            val x = (xOf(milestones[index].dueDate).roundToPx() - placeable.width / 2)
                .coerceIn(0, (constraints.maxWidth - placeable.width).coerceAtLeast(0))
            val y = ((constraints.maxHeight - placeable.height) / 2).coerceAtLeast(0)
            Triple(placeable, x, y)
        }
        layout(constraints.maxWidth, constraints.maxHeight) {
            placed.forEach { (placeable, x, y) -> placeable.placeRelative(x, y) }
        }
    }
}

/** Activity bars, one measured row per parallel strand. */
@Composable
private fun GanttBars(
    rows: List<List<GoalActivity>>,
    modifier: Modifier = Modifier,
    xOf: (LocalDate) -> Dp,
    onActivity: (GoalActivity) -> Unit,
) {
    Layout(
        modifier = modifier,
        content = {
            rows.forEach { row ->
                row.forEach { activity -> GanttBar(activity, onActivity) }
            }
        },
    ) { measurables, constraints ->
        val flat = mutableListOf<Pair<Int, GoalActivity>>()
        rows.forEachIndexed { rowIndex, row -> row.forEach { activity -> flat.add(rowIndex to activity) } }
        val barHeight = GanttBarHeight.roundToPx()
        val rowStep = ganttRowStep().roundToPx()
        val topPad = GanttBarsTopPad.roundToPx()
        val minWidth = 14.dp.roundToPx()
        val placed = measurables.mapIndexed { index, measurable ->
            val (rowIndex, activity) = flat[index]
            val left = xOf(activity.start).roundToPx().coerceIn(0, constraints.maxWidth.coerceAtLeast(0))
            val right = xOf(activity.end).roundToPx().coerceIn(left, constraints.maxWidth)
            val width = (right - left).coerceAtLeast(minWidth).coerceAtMost((constraints.maxWidth - left).coerceAtLeast(1))
            val placeable = measurable.measure(Constraints.fixed(width, barHeight))
            val y = topPad + rowIndex * rowStep
            // A bar that would poke out of its lane is the thing the reader described, so a row that
            // does not fit is left out rather than drawn on top of the row above it. The lane height
            // comes from the same three constants, so this can only trigger if a later change forgets
            // one of them — and even then a missing bar beats two bars on top of each other.
            if (y + barHeight > constraints.maxHeight) null else Triple(placeable, left, y)
        }
        layout(constraints.maxWidth, constraints.maxHeight) {
            placed.forEach { row -> row?.let { (placeable, x, y) -> placeable.placeRelative(x, y) } }
        }
    }
}

/** One bar: label only when the measured width can hold something readable. */
@Composable
private fun GanttBar(activity: GoalActivity, onActivity: (GoalActivity) -> Unit) {
    val style = goalCategoryStyle(activity.category)
    val description = stringResource(
        R.string.goals_gantt_bar_description,
        activity.title,
        RoutineDate.normal(activity.start),
        RoutineDate.normal(activity.end),
    )
    val border = if (activity.isCasProject) Modifier.border(1.dp, RoutineColors.FocusAccent, RoutineShapes.Chip) else Modifier
    val dim = if (activity.isDone) Modifier.alpha(0.55f) else Modifier
    BoxWithConstraints(
        Modifier.clip(RoutineShapes.Chip)
            .background(style.container)
            .then(border)
            .then(dim)
            .clickable { onActivity(activity) }
            .semantics { contentDescription = description }
            .padding(horizontal = RoutineSpacing.sm),
    ) {
        if (maxWidth >= RoutineMetrics.MinLabelWidth) {
            RoutineLabel(
                activity.title,
                modifier = Modifier.align(Alignment.CenterStart),
                style = MaterialTheme.typography.labelSmall,
                color = style.content,
            )
        }
    }
}

/** Greedy interval packing so CAS strands that run in parallel get their own sub-row instead of colliding. */
private fun packActivityRows(activities: List<GoalActivity>): List<List<GoalActivity>> {
    val rows = mutableListOf<MutableList<GoalActivity>>()
    activities.sortedWith(compareBy({ it.start }, { it.end })).forEach { activity ->
        val openRow = rows.firstOrNull { it.last().end.isBefore(activity.start) }
        if (openRow != null) openRow.add(activity) else rows.add(mutableListOf(activity))
    }
    return rows
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ActivityList(
    activities: List<GoalActivity>,
    progress: List<GoalProgress>,
    busy: Boolean,
    onAction: (TimelineAction) -> Unit,
    onAdd: () -> Unit,
    onEdit: (GoalActivity) -> Unit,
    /** Names the plan a row belongs to; null while one plan is on screen, where it would be noise. */
    projectNameOf: (Long) -> String?,
) {
    Column(verticalArrangement = Arrangement.spacedBy(RoutineSpacing.sm)) {
        SectionHeader(
            title = stringResource(R.string.goals_activities),
            action = {
                FilledTonalButton(enabled = !busy, onClick = onAdd) {
                    RoutineLabel(stringResource(R.string.goals_add_activity), style = MaterialTheme.typography.labelLarge)
                }
            },
        )
        if (activities.isEmpty()) {
            RoutineText(stringResource(R.string.goals_activities_empty), style = MaterialTheme.typography.bodySmall,
                color = RoutineColors.TextSecondary, maxLines = RoutineTextDefaults.Paragraph)
        }
        activities.forEach { activity ->
            val hours = progress.filter { it.kind == "hour" && it.activityId == activity.id }.sumOf { it.amount }
            val details = listOfNotNull(
                projectNameOf(activity.projectId),
                stringResource(R.string.date_range, RoutineDate.normal(activity.start), RoutineDate.normal(activity.end)),
                categoryLabel(activity.category),
                if (hours > 0.0) goalsMinutesLabel(hours) else null,
                if (activity.isScheduled) stringResource(R.string.goals_scheduled) else null,
                if (activity.isCasProject) stringResource(R.string.goals_cas_project_short) else null,
                if (activity.isDone) stringResource(R.string.goals_activity_done) else null,
            ).joinToString(" · ")
            OutlinedCard(
                onClick = { onEdit(activity) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoutineShapes.Card,
                border = BorderStroke(1.dp, RoutineColors.CardBorder),
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = RoutineSpacing.md, vertical = RoutineSpacing.sm),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(RoutineSpacing.sm),
                ) {
                    Checkbox(
                        checked = activity.isDone,
                        onCheckedChange = { if (!busy) onAction(TimelineAction.ToggleGoalActivity(activity.id)) },
                        enabled = !busy,
                        modifier = Modifier.size(RoutineMetrics.ActionMinWidth),
                    )
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(RoutineSpacing.xs)) {
                        RoutineText(activity.title, style = MaterialTheme.typography.titleSmall,
                            maxLines = RoutineTextDefaults.Body)
                        RoutineText(details, style = MaterialTheme.typography.labelSmall,
                            color = RoutineColors.TextSecondary, maxLines = RoutineTextDefaults.Body)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MilestoneList(
    milestones: List<GoalMilestone>,
    busy: Boolean,
    onAction: (TimelineAction) -> Unit,
    onAdd: () -> Unit,
    onEdit: (GoalMilestone) -> Unit,
    projectNameOf: (Long) -> String?,
) {
    Column(verticalArrangement = Arrangement.spacedBy(RoutineSpacing.sm)) {
        SectionHeader(
            title = stringResource(R.string.goals_milestones),
            action = {
                FilledTonalButton(enabled = !busy, onClick = onAdd) {
                    RoutineLabel(stringResource(R.string.goals_add_milestone), style = MaterialTheme.typography.labelLarge)
                }
            },
        )
        if (milestones.isEmpty()) {
            RoutineText(stringResource(R.string.goals_milestones_empty), style = MaterialTheme.typography.bodySmall,
                color = RoutineColors.TextSecondary, maxLines = RoutineTextDefaults.Paragraph)
        }
        milestones.forEach { milestone ->
            OutlinedCard(shape = RoutineShapes.Card, border = BorderStroke(1.dp, RoutineColors.CardBorder),
                modifier = Modifier.fillMaxWidth()) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = RoutineSpacing.md, vertical = RoutineSpacing.xs),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(RoutineSpacing.sm),
                ) {
                    Checkbox(
                        checked = milestone.isDone,
                        onCheckedChange = { if (!busy) onAction(TimelineAction.ToggleGoalMilestone(milestone.id)) },
                        enabled = !busy,
                        modifier = Modifier.size(RoutineMetrics.ActionMinWidth),
                    )
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(RoutineSpacing.xs)) {
                        RoutineText(milestone.title, style = MaterialTheme.typography.titleSmall,
                            maxLines = RoutineTextDefaults.Body)
                        // A date and its "in 3 weeks" belong together: if the row is narrow the line
                        // wraps rather than cutting the relative day off the end.
                        RoutineText(
                            text = listOfNotNull(projectNameOf(milestone.projectId),
                                RoutineDate.normal(milestone.dueDate), goalRelative(milestone.dueDate)).joinToString(" · "),
                            style = MaterialTheme.typography.labelSmall,
                            color = RoutineColors.TextSecondary,
                            maxLines = RoutineTextDefaults.Body,
                        )
                    }
                    TextButton(enabled = !busy, onClick = { onEdit(milestone) }) {
                        RoutineLabel(stringResource(R.string.edit), style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
    }
}



/**
 * Progress is stored as decimal hours; the rows used to read it back with `toInt()`, so half an hour
 * showed as "0 h" and 45 minutes vanished. The two helpers below are the only place that converts a
 * stored amount into something a reader sees, and both are named after the unit they return.
 */
@Composable
internal fun goalsHoursLabel(hours: Double): String =
    stringResource(R.string.goals_hours_total_log, formatHoursRounded(hours))

@Composable
private fun goalsMinutesLabel(minutes: Double): String =
    when {
        minutes < 60 -> stringResource(R.string.duration_minutes, minutes.roundToInt())
        else -> durationLabel(minutes.roundToInt())
    }

/**
 * Rounds to one decimal in both languages, so Slovenian writes "1,5 h" and English "1.5 h". The
 * locale is a parameter with the interface's own as its default: a unit test can then pass one
 * explicitly instead of depending on the machine it happens to run on.
 */
internal fun formatHoursRounded(hours: Double, locale: Locale = interfaceLocale): String {
    val rounded = (hours * 10).roundToInt() / 10.0
    return if (rounded % 1.0 == 0.0) rounded.toInt().toString()
    else NumberFormat.getNumberInstance(locale).apply {
        minimumFractionDigits = 1; maximumFractionDigits = 1
    }.format(rounded)
}

internal fun logProgress(projectId: Long, activityId: Long, hours: Double): TimelineAction =
    TimelineAction.AddGoalProgress(
        GoalProgress(projectId = projectId, activityId = activityId, kind = "hour", amount = hours, date = LocalDate.now()),
    )

@Composable
private fun goalRelative(date: LocalDate): String {
    val days = ChronoUnit.DAYS.between(LocalDate.now(), date)
    return when {
        days == 0L -> stringResource(R.string.tasks_today_short)
        days == 1L -> stringResource(R.string.tasks_tomorrow)
        days > 0 -> pluralStringResource(R.plurals.tasks_in_days, days.toInt(), days.toInt())
        else -> pluralStringResource(R.plurals.tasks_ago_days, (-days).toInt(), (-days).toInt())
    }
}

/** Container/content pairs, so a bar label always has enough contrast on its own background. */
private fun goalCategoryStyle(category: String?): CategoryStyle = when (category) {
    "CREATIVITY" -> RoutineColors.Focus
    "ACTIVITY" -> RoutineColors.Recovery
    "SERVICE" -> RoutineColors.Project
    "STAGE" -> RoutineColors.School
    else -> RoutineColors.Personal
}

@Composable
private fun categoryLabel(category: String?): String? = when (category) {
    "CREATIVITY" -> stringResource(R.string.goals_category_creativity)
    "ACTIVITY" -> stringResource(R.string.goals_category_activity)
    "SERVICE" -> stringResource(R.string.goals_category_service)
    "STAGE" -> stringResource(R.string.goals_category_stage)
    else -> null
}
