package com.example.mydailyroutine.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Badge
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallTopAppBar
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.mydailyroutine.data.local.SubjectEntity
import com.example.mydailyroutine.domain.HealthWarning
import com.example.mydailyroutine.domain.MonthMarkers
import com.example.mydailyroutine.domain.ResolvedTimelineItem
import com.example.mydailyroutine.domain.RoutineCategory
import com.example.mydailyroutine.domain.TimelineItemKind
import com.example.mydailyroutine.domain.WarningType
import com.example.mydailyroutine.domain.YearOverview
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoutineApp(viewModel: RoutineViewModel, openFastAddRequest: Int = 0) {
    val date by viewModel.selectedDate.collectAsStateWithLifecycle()
    val mode by viewModel.viewMode.collectAsStateWithLifecycle()
    val timeline by viewModel.timeline.collectAsStateWithLifecycle()
    val warnings by viewModel.warnings.collectAsStateWithLifecycle()
    val subjects by viewModel.subjects.collectAsStateWithLifecycle()
    val completed by viewModel.completedIds.collectAsStateWithLifecycle()
    val week by viewModel.week.collectAsStateWithLifecycle()
    val markers by viewModel.monthMarkers.collectAsStateWithLifecycle()
    val year by viewModel.yearOverview.collectAsStateWithLifecycle()
    var showFastAdd by rememberSaveable { mutableStateOf(openFastAddRequest > 0) }
    val haptics = LocalHapticFeedback.current

    LaunchedEffect(openFastAddRequest) {
        if (openFastAddRequest > 0) showFastAdd = true
    }

    Scaffold(
        topBar = {
            SmallTopAppBar(
                title = {
                    Column {
                        Text("My Daily Routine", fontWeight = FontWeight.Bold)
                        Text(
                            text = when (mode) {
                                ViewMode.DAY -> date.prettyDate()
                                ViewMode.WEEK -> "Your week at a glance"
                                ViewMode.MONTH -> date.month.getDisplayName(TextStyle.FULL, Locale.getDefault())
                                ViewMode.YEAR -> "School year macro view"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                actions = {
                    TextButton(onClick = {
                        viewModel.selectDate(LocalDate.now())
                        viewModel.setViewMode(ViewMode.DAY)
                    }) { Text("Today") }
                },
                colors = TopAppBarDefaults.smallTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                showFastAdd = true
            }) { Text("+") }
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            ViewModeBar(mode = mode, onModeSelected = viewModel::setViewMode)
            DateNavigator(
                date = date,
                mode = mode,
                onPrevious = { viewModel.moveDate(if (mode == ViewMode.WEEK) -7 else if (mode == ViewMode.MONTH) -30 else -1) },
                onNext = { viewModel.moveDate(if (mode == ViewMode.WEEK) 7 else if (mode == ViewMode.MONTH) 30 else 1) },
            )
            when (mode) {
                ViewMode.DAY -> DayView(
                    date = date,
                    timeline = timeline,
                    warnings = warnings,
                    completed = completed,
                    onItemClick = { haptics.performHapticFeedback(HapticFeedbackType.LongPress) },
                    onToggleCompleted = { item ->
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        viewModel.toggleCompleted(item)
                    },
                )
                ViewMode.WEEK -> WeekView(week)
                ViewMode.MONTH -> MonthView(date, markers, viewModel::selectDate)
                ViewMode.YEAR -> YearView(year)
            }
        }
    }

    if (showFastAdd) {
        FastAddSheet(
            subjects = subjects,
            onDismiss = { showFastAdd = false },
            onSubject = { subject ->
                viewModel.addSubjectBlock(subject)
                showFastAdd = false
            },
            onPreset = { preset ->
                viewModel.addPreset(preset)
                showFastAdd = false
            },
        )
    }
}

@Composable
private fun ViewModeBar(mode: ViewMode, onModeSelected: (ViewMode) -> Unit) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(ViewMode.entries) { item ->
            FilterChip(
                selected = mode == item,
                onClick = { onModeSelected(item) },
                label = { Text(item.label) },
            )
        }
    }
}

@Composable
private fun DateNavigator(date: LocalDate, mode: ViewMode, onPrevious: () -> Unit, onNext: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        TextButton(onClick = onPrevious) { Text("‹") }
        Text(
            text = when (mode) {
                ViewMode.DAY -> date.format(DateTimeFormatter.ofPattern("EEE, d MMM yyyy"))
                ViewMode.WEEK -> "Week of ${date.minusDays((date.dayOfWeek.value - 1).toLong()).format(DateTimeFormatter.ofPattern("d MMM"))}"
                ViewMode.MONTH -> date.format(DateTimeFormatter.ofPattern("MMMM yyyy"))
                ViewMode.YEAR -> "${date.year} / ${date.year + 1}"
            },
            fontWeight = FontWeight.SemiBold,
        )
        TextButton(onClick = onNext) { Text("›") }
    }
}

@Composable
private fun DayView(
    date: LocalDate,
    timeline: List<ResolvedTimelineItem>,
    warnings: List<HealthWarning>,
    completed: Set<String>,
    onItemClick: (ResolvedTimelineItem) -> Unit,
    onToggleCompleted: (ResolvedTimelineItem) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (warnings.isNotEmpty()) {
            item(key = "warning-header") { WarningSummary(warnings) }
        }
        if (timeline.isEmpty()) {
            item(key = "empty") { EmptyDay(date) }
        } else {
            itemsIndexed(timeline, key = { _, item -> item.stableId }) { index, item ->
                if (index > 0) {
                    val gapMinutes = Duration.between(timeline[index - 1].endTime, item.startTime).toMinutes().coerceAtLeast(0)
                    if (gapMinutes > 0) {
                        Spacer(Modifier.height((gapMinutes * 0.35f).coerceIn(6f, 120f).dp))
                    }
                }
                val itemWarnings = warnings.filter { it.relatedItemId == item.stableId }
                TimelineCard(
                    item = item,
                    warnings = itemWarnings,
                    completed = item.stableId in completed,
                    onClick = { onItemClick(item) },
                    onToggleCompleted = { onToggleCompleted(item) },
                )
            }
        }
    }
}

@Composable
private fun WarningSummary(warnings: List<HealthWarning>) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
        shape = RoundedCornerShape(18.dp),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Schedule health · ${warnings.size} check${if (warnings.size == 1) "" else "s"}", fontWeight = FontWeight.Bold)
            warnings.take(3).forEach { warning ->
                Text("• ${warning.message}", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun TimelineCard(
    item: ResolvedTimelineItem,
    warnings: List<HealthWarning>,
    completed: Boolean,
    onClick: () -> Unit,
    onToggleCompleted: () -> Unit,
) {
    var expanded by rememberSaveable(item.stableId) { mutableStateOf(false) }
    val accent = item.subjectColorHex?.let { Color(it.toInt()) } ?: categoryColor(item.category)
    val cardColor = when (item.category) {
        RoutineCategory.REST_BREAK -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        RoutineCategory.SCHOOL -> MaterialTheme.colorScheme.surfaceContainerLow
        else -> MaterialTheme.colorScheme.surface
    }
    val now = LocalTime.now()
    val active = item.kind == TimelineItemKind.ROUTINE && item.startTime <= now && item.endTime > now

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
            .border(BorderStroke(if (active) 3.dp else 2.dp, accent), RoundedCornerShape(18.dp))
            .clickable {
                expanded = !expanded
                onClick()
            },
        colors = CardDefaults.cardColors(containerColor = cardColor),
        elevation = CardDefaults.cardElevation(defaultElevation = if (active) 5.dp else 1.dp),
    ) {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.Top) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(58.dp)) {
                Text(
                    text = if (item.isAllDay) "ALL" else item.startTime.toDisplay(),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = accent,
                )
                if (!item.isAllDay) {
                    Text(item.endTime.toDisplay(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(Modifier.height(8.dp))
                Box(Modifier.size(10.dp).clip(CircleShape).background(accent))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f),
                    )
                    Checkbox(checked = completed, onCheckedChange = { onToggleCompleted() })
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = item.kindLabel(),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (item.isSchoolDayOff) {
                        Badge(containerColor = MaterialTheme.colorScheme.tertiaryContainer) { Text("Day off") }
                    }
                    if (active) {
                        Badge(containerColor = MaterialTheme.colorScheme.primaryContainer) { Text("Now") }
                    }
                }
                AnimatedVisibility(visible = expanded, enter = slideInVertically() + fadeIn()) {
                    Column(Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        if (item.isSchoolDayOff) Text(item.schoolDayOffTitle ?: "School holiday", style = MaterialTheme.typography.bodySmall)
                        if (item.kind == TimelineItemKind.MILESTONE && item.isExam) Text("Exam / milestone", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                        if (item.durationMinutes > 0) Text("${item.durationMinutes} minutes planned", style = MaterialTheme.typography.bodySmall)
                    }
                }
                warnings.forEach { warning -> InlineWarning(warning) }
            }
        }
    }
}

@Composable
private fun InlineWarning(warning: HealthWarning) {
    AssistChip(
        onClick = {},
        label = { Text(warning.type.label(), maxLines = 1) },
        leadingIcon = { Text("!") },
        modifier = Modifier.padding(top = 5.dp),
    )
}

@Composable
private fun EmptyDay(date: LocalDate) {
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("A clear day", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(6.dp))
            Text("Use + to add a focused block, recovery window, or milestone.", textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun WeekView(days: List<DaySummary>) {
    val scroll = rememberScrollState()
    Row(Modifier.fillMaxWidth().horizontalScroll(scroll).padding(horizontal = 10.dp), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        days.forEach { day ->
            val focusMinutes = day.items.filter { it.category == RoutineCategory.FOCUS_STUDY }.sumOf { it.durationMinutes }
            val schoolMinutes = day.items.filter { it.category == RoutineCategory.SCHOOL }.sumOf { it.durationMinutes }
            Column(Modifier.width(72.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(day.date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()), style = MaterialTheme.typography.labelSmall)
                Text(day.date.dayOfMonth.toString(), fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(10.dp))
                Box(Modifier.width(48.dp).height(230.dp).clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.surfaceVariant)) {
                    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Bottom, horizontalAlignment = Alignment.CenterHorizontally) {
                        if (schoolMinutes > 0) Box(Modifier.width(48.dp).height((schoolMinutes / 6f).coerceIn(12f, 150f).dp).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.65f)))
                        if (focusMinutes > 0) Box(Modifier.width(48.dp).height((focusMinutes / 3f).coerceIn(10f, 150f).dp).background(MaterialTheme.colorScheme.tertiary))
                    }
                }
                Text("${day.items.size} blocks", style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = 5.dp))
                Text("${focusMinutes}m focus", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MonthView(date: LocalDate, markers: MonthMarkers, onDateSelected: (LocalDate) -> Unit) {
    val first = date.withDayOfMonth(1)
    val offset = first.dayOfWeek.value - DayOfWeek.MONDAY.value
    val dates = (0 until 42).map { first.minusDays(offset.toLong()).plusDays(it.toLong()) }
    Column(Modifier.fillMaxSize().padding(horizontal = 12.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            DayOfWeek.entries.forEach { Text(it.getDisplayName(TextStyle.NARROW, Locale.getDefault()), modifier = Modifier.width(42.dp), textAlign = TextAlign.Center, style = MaterialTheme.typography.labelSmall) }
        }
        Spacer(Modifier.height(6.dp))
        LazyVerticalGrid(
            columns = GridCells.Fixed(7),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            gridItems(items = dates) { cell ->
                val inMonth = cell.month == date.month
                val workFree = cell in markers.workFreeDates
                val milestone = cell in markers.milestoneDates
                val exam = cell in markers.examDates
                Card(
                    modifier = Modifier.height(58.dp).clickable { onDateSelected(cell) },
                    colors = CardDefaults.cardColors(
                        containerColor = when {
                            exam -> MaterialTheme.colorScheme.errorContainer
                            workFree -> MaterialTheme.colorScheme.secondaryContainer
                            else -> MaterialTheme.colorScheme.surfaceContainerLow
                        },
                    ),
                ) {
                    Column(Modifier.fillMaxSize().padding(6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(cell.dayOfMonth.toString(), color = if (inMonth) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = .35f))
                        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                            if (milestone) Box(Modifier.size(5.dp).clip(CircleShape).background(MaterialTheme.colorScheme.tertiary))
                            if (workFree) Box(Modifier.size(5.dp).clip(CircleShape).background(MaterialTheme.colorScheme.secondary))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun YearView(year: YearOverview) {
    val daysLeft = ChronoUnit.DAYS.between(LocalDate.now(), year.schoolYearEnd).coerceAtLeast(0)
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp, bottom = 96.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                Column(Modifier.padding(20.dp)) {
                    Text("School year countdown", style = MaterialTheme.typography.labelLarge)
                    Text("$daysLeft", style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Bold)
                    Text("days until ${year.schoolYearEnd.format(DateTimeFormatter.ofPattern("d MMM yyyy"))}")
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MacroStat("Recovery days", year.workFreeDays.toString(), Modifier.weight(1f))
                MacroStat("Milestones", year.milestoneCount.toString(), Modifier.weight(1f))
            }
        }
        if (year.breakDistribution.isNotEmpty()) {
            item {
                Text("Break distribution", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                    items(year.breakDistribution) { breakSummary ->
                        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                            Column(Modifier.padding(12.dp)) {
                                Text(breakSummary.days.toString(), fontWeight = FontWeight.Bold)
                                Text(breakSummary.title, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }
        }
        item { Text("Upcoming milestone radar", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
        if (year.upcomingMilestones.isEmpty()) item { Text("No upcoming milestones in this school year.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        items(year.upcomingMilestones, key = { it.id }) { milestone ->
            Card { Row(Modifier.fillMaxWidth().padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(9.dp).clip(CircleShape).background(if (milestone.isExam) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiary))
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) { Text(milestone.title, fontWeight = FontWeight.SemiBold); Text(milestone.dueDate.toString(), style = MaterialTheme.typography.labelSmall) }
                if (milestone.isExam) Badge { Text("Exam") }
            } }
        }
    }
}

@Composable
private fun MacroStat(label: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier) { Column(Modifier.padding(14.dp)) { Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold); Text(label, style = MaterialTheme.typography.labelSmall) } }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FastAddSheet(
    subjects: List<SubjectEntity>,
    onDismiss: () -> Unit,
    onSubject: (SubjectEntity) -> Unit,
    onPreset: (RoutineViewModel.QuickPreset) -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                Text("Fast add", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text("One tap creates a block on the selected day.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            item {
                Text("Saved subjects", fontWeight = FontWeight.SemiBold)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                    items(subjects) { subject ->
                        FilterChip(selected = false, onClick = { onSubject(subject) }, label = { Text(subject.name) })
                    }
                }
            }
            item { HorizontalDivider() }
            item { Text("Quick presets", fontWeight = FontWeight.SemiBold) }
            items(RoutineViewModel.QuickPreset.entries) { preset ->
                Card(Modifier.fillMaxWidth().clickable { onPreset(preset) }) {
                    Row(Modifier.padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(preset.label, modifier = Modifier.weight(1f), fontWeight = FontWeight.Medium)
                        Text("Add ›", color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
}

private fun categoryColor(category: RoutineCategory?): Color = when (category) {
    RoutineCategory.FOCUS_STUDY -> Color(0xFF6750A4)
    RoutineCategory.SCHOOL -> Color(0xFF006A6A)
    RoutineCategory.REST_BREAK -> Color(0xFF7B6F66)
    RoutineCategory.PROJECT -> Color(0xFF8A4F7D)
    RoutineCategory.PERSONAL -> Color(0xFF9C4146)
    null -> Color(0xFF7B4A16)
}

private fun ResolvedTimelineItem.kindLabel(): String = when {
    kind == TimelineItemKind.MILESTONE && isExam -> "EXAM MILESTONE"
    kind == TimelineItemKind.MILESTONE -> "MILESTONE"
    else -> category?.name?.replace('_', ' ') ?: "ROUTINE"
}

private fun LocalDate.prettyDate(): String = format(DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.getDefault()))
private fun LocalTime.toDisplay(): String = format(DateTimeFormatter.ofPattern("HH:mm"))
private fun WarningType.label(): String = name.replace('_', ' ').lowercase().replaceFirstChar { it.uppercase() }
