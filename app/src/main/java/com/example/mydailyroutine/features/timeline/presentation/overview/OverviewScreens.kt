package com.example.mydailyroutine.features.timeline.presentation.overview

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.testTag
import com.example.mydailyroutine.R
import com.example.mydailyroutine.core.designsystem.components.CollapsibleSection
import com.example.mydailyroutine.core.designsystem.components.RoutineLabel
import com.example.mydailyroutine.core.designsystem.components.RoutineText
import com.example.mydailyroutine.core.designsystem.components.RoutineTextDefaults
import com.example.mydailyroutine.core.designsystem.theme.RoutineColors
import com.example.mydailyroutine.core.designsystem.theme.RoutineMetrics
import com.example.mydailyroutine.core.designsystem.theme.RoutineShapes
import com.example.mydailyroutine.core.designsystem.theme.RoutineSpacing
import com.example.mydailyroutine.core.platform.uiLocale
import com.example.mydailyroutine.core.presentation.TimelineContent
import com.example.mydailyroutine.core.presentation.TimelineMode
import com.example.mydailyroutine.core.presentation.PeriodRanges
import com.example.mydailyroutine.core.presentation.categoryColor
import com.example.mydailyroutine.core.presentation.durationLabel
import com.example.mydailyroutine.core.presentation.minuteLabel
import com.example.mydailyroutine.core.presentation.RoutineDate
import com.example.mydailyroutine.domain.calendar.SlovenianAcademicCalendar
import com.example.mydailyroutine.domain.health.PositionedBlock
import com.example.mydailyroutine.domain.health.WeeklyLayout
import com.example.mydailyroutine.domain.model.Milestone
import com.example.mydailyroutine.domain.model.ResolvedTimelineItem
import com.example.mydailyroutine.domain.model.SchedulePreferences
import com.example.mydailyroutine.domain.routines.RoutineOrigin
import com.example.mydailyroutine.features.timeline.components.Legend
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters

/**
 * Week, month and year overviews.
 *
 * Grids place their blocks with a measured [Layout] instead of `Modifier.offset`: a child is
 * measured with exactly the lane width and the block height it owns, and a label is drawn only when
 * that box is big enough for it. Overlap is therefore impossible by construction, and a 15-minute
 * block shows colour plus an accessible description instead of a clipped fragment of its title.
 */
@Composable
fun WeeklyOverview(content: TimelineContent, onGoals: () -> Unit = {}, topInset: Dp = 0.dp, onDate: (LocalDate) -> Unit) {
    val days = content.days.values.sortedBy { it.date }
    val blocks = days.flatMap { it.items.filterIsInstance<ResolvedTimelineItem.Block>() }.filter { it.origin != RoutineOrigin.SLEEP }
    val startHour = minOf(7, (blocks.minOfOrNull { it.startMinute } ?: 420) / 60)
    val endHour = maxOf(21, ((blocks.maxOfOrNull { it.endMinute } ?: 1260) + 59) / 60).coerceAtMost(24)
    val minuteHeight = RoutineMetrics.WeekMinuteHeight
    val gridHeight = minuteHeight * ((endHour - startHour) * 60)
    val gridColor = RoutineColors.CardBorder
    LazyColumn(
        contentPadding = PaddingValues(RoutineSpacing.lg, topInset + RoutineSpacing.md, RoutineSpacing.lg, 108.dp),
        verticalArrangement = Arrangement.spacedBy(RoutineSpacing.lg),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(RoutineSpacing.xs)) {
                RoutineText(stringResource(R.string.week_heading), style = MaterialTheme.typography.titleLarge,
                    maxLines = RoutineTextDefaults.Body)
                RoutineText(
                    stringResource(R.string.week_totals, durationLabel(days.sumOf { it.metrics.focusMinutes }),
                        durationLabel(days.sumOf { it.metrics.recoveryMinutes })),
                    style = MaterialTheme.typography.bodyMedium,
                    color = RoutineColors.TextSecondary,
                    maxLines = RoutineTextDefaults.Body,
                )
                RoutineText(stringResource(R.string.week_hint), style = MaterialTheme.typography.bodySmall,
                    maxLines = RoutineTextDefaults.Paragraph)
                if (!SlovenianAcademicCalendar.covers(content.date)) {
                    RoutineText(stringResource(R.string.coverage_warning), style = MaterialTheme.typography.bodySmall,
                        color = RoutineColors.Error, maxLines = RoutineTextDefaults.Paragraph)
                }
            }
        }
        item {
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                val gridWidth = maxOf(maxWidth, 748.dp)
                val dayWidth = (gridWidth - RoutineMetrics.GutterTextWidth) / 7
                Column(Modifier.horizontalScroll(rememberScrollState())) {
                    Row(Modifier.width(gridWidth)) {
                        Box(Modifier.width(RoutineMetrics.GutterTextWidth))
                        days.forEach { day ->
                            // Tagged because it is the drill-down entry point: tapping a week column
                            // opens the day, and the back stack has to be able to prove it walks out again.
                            Column(
                                Modifier.width(dayWidth).clickable { onDate(day.date) }
                                    .testTag("week-day-column").padding(RoutineSpacing.xs),
                                verticalArrangement = Arrangement.spacedBy(RoutineSpacing.xs),
                            ) {
                                RoutineLabel(
                                    RoutineDate.weekdayTight(day.date),
                                    style = MaterialTheme.typography.labelLarge,
                                )
                                RoutineLabel(
                                    stringResource(R.string.week_focus, durationLabel(day.metrics.focusMinutes)),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = RoutineColors.TextSecondary,
                                )
                                LinearProgressIndicator(
                                    progress = { (day.metrics.occupiedMinutes / 840f).coerceIn(0f, 1f) },
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                        }
                    }
                    Row(
                        Modifier.width(gridWidth).height(gridHeight).drawBehind {
                            val x = RoutineMetrics.GutterTextWidth.toPx()
                            drawLine(gridColor, Offset(x, 0f), Offset(x, size.height), 1.dp.toPx())
                        },
                    ) {
                        Column(Modifier.width(RoutineMetrics.GutterTextWidth)) {
                            (startHour until endHour).forEach { hour ->
                                RoutineLabel(
                                    minuteLabel(hour * 60),
                                    modifier = Modifier.height(minuteHeight * 60).padding(top = RoutineSpacing.xs),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = RoutineColors.TextMuted,
                                )
                            }
                        }
                        days.forEach { day ->
                            val positioned = remember(day.items) {
                                WeeklyLayout.position(
                                    day.items.filterIsInstance<ResolvedTimelineItem.Block>()
                                        .filter { it.origin != RoutineOrigin.SLEEP },
                                )
                            }
                            WeeklyDayColumn(
                                positioned = positioned,
                                date = day.date,
                                startMinute = startHour * 60,
                                dayWidth = dayWidth,
                                gridHeight = gridHeight,
                                minuteHeight = minuteHeight,
                                gridColor = gridColor,
                                hourCount = endHour - startHour,
                                onDate = onDate,
                            )
                        }
                    }
                }
            }
        }
        milestoneSection(R.string.week_markers, content.milestones, content.taskMarkers, content.goalMarkers, onGoals, onDate)
    }
}

/** One day of the weekly grid: lanes measured and placed, never offset over a neighbour. */
@Composable
private fun WeeklyDayColumn(
    positioned: List<PositionedBlock>,
    date: LocalDate,
    startMinute: Int,
    dayWidth: Dp,
    gridHeight: Dp,
    minuteHeight: Dp,
    gridColor: Color,
    hourCount: Int,
    onDate: (LocalDate) -> Unit,
) {
    Layout(
        modifier = Modifier.width(dayWidth).height(gridHeight).drawBehind {
            drawLine(gridColor, Offset.Zero, Offset(0f, size.height), 1.dp.toPx())
            for (hour in 0..hourCount) {
                val y = (minuteHeight * (hour * 60)).toPx()
                drawLine(gridColor, Offset(0f, y), Offset(size.width, y), 1.dp.toPx())
            }
        },
        content = { positioned.forEach { position -> WeeklyBlockCell(position, date, onDate) } },
    ) { measurables, constraints ->
        val gap = 2.dp.roundToPx()
        val minHeight = 2.dp.roundToPx()
        val placed = measurables.mapIndexed { index, measurable ->
            val position = positioned[index]
            val laneWidth = constraints.maxWidth / position.laneCount
            val top = (minuteHeight * (position.block.startMinute - startMinute)).roundToPx()
                .coerceIn(0, constraints.maxHeight)
            val height = (minuteHeight * position.block.durationMinutes).roundToPx()
                .coerceIn(minHeight, (constraints.maxHeight - top).coerceAtLeast(minHeight))
            val placeable = measurable.measure(Constraints.fixed((laneWidth - gap).coerceAtLeast(1), height))
            Triple(placeable, laneWidth * position.lane + gap / 2, top)
        }
        layout(constraints.maxWidth, constraints.maxHeight) {
            placed.forEach { (placeable, x, y) -> placeable.placeRelative(x, y) }
        }
    }
}

/** A single weekly block. Text is drawn only when the measured box can actually hold it. */
@Composable
private fun WeeklyBlockCell(position: PositionedBlock, date: LocalDate, onDate: (LocalDate) -> Unit) {
    val block = position.block
    val accent = categoryColor(block.category, block.subject?.colorHex)
    val range = stringResource(R.string.time_range, minuteLabel(block.startMinute), minuteLabel(block.endMinute))
    val duration = durationLabel(block.durationMinutes)
    val description = stringResource(
        R.string.week_block_description,
        block.title,
        RoutineDate.weekdayFull(date),
        range,
        duration,
    )
    BoxWithConstraints(
        Modifier.clip(RoundedCornerShape(6.dp))
            .background(if (block.isSuppressed) RoutineColors.Surface3 else accent.copy(alpha = 0.22f))
            .clickable { onDate(date) }
            .semantics { contentDescription = description }
            .padding(RoutineSpacing.xs),
    ) {
        // Hoisted out of the Column: BoxWithConstraints properties are not visible as implicit
        // receivers inside a nested layout scope.
        val labelFits = maxWidth >= RoutineMetrics.MinLabelWidth && maxHeight >= RoutineMetrics.MinLabelHeight
        val twoLines = maxHeight >= 40.dp
        val showsDuration = maxHeight >= 48.dp
        if (labelFits) {
            Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
                RoutineText(
                    text = block.title,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = if (twoLines) RoutineTextDefaults.Body else 1,
                )
                if (showsDuration) {
                    RoutineLabel(
                        text = if (block.isSuppressed) stringResource(R.string.no_school_short) else duration,
                        style = MaterialTheme.typography.labelSmall,
                        color = RoutineColors.TextSecondary,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MonthlyOverview(content: TimelineContent, today: LocalDate, onGoals: () -> Unit = {}, topInset: Dp = 0.dp, onDate: (LocalDate) -> Unit) {
    val month = YearMonth.from(content.date)
    val dates = content.days.keys.sorted()
    LazyColumn(
        contentPadding = PaddingValues(RoutineSpacing.lg, topInset + RoutineSpacing.md, RoutineSpacing.lg, 108.dp),
        verticalArrangement = Arrangement.spacedBy(RoutineSpacing.lg),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(RoutineSpacing.xs)) {
                RoutineText(stringResource(R.string.month_heading), style = MaterialTheme.typography.titleLarge,
                    maxLines = RoutineTextDefaults.Body)
                RoutineText(stringResource(R.string.month_hint), style = MaterialTheme.typography.bodyMedium,
                    color = RoutineColors.TextSecondary, maxLines = RoutineTextDefaults.Paragraph)
                if (!SlovenianAcademicCalendar.covers(content.date)) {
                    RoutineText(stringResource(R.string.coverage_warning), style = MaterialTheme.typography.bodySmall,
                        color = RoutineColors.Error, maxLines = RoutineTextDefaults.Paragraph)
                }
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(RoutineSpacing.sm)) {
                Row(Modifier.fillMaxWidth()) {
                    DayOfWeek.values().forEach {
                        RoutineLabel(
                            it.getDisplayName(TextStyle.NARROW_STANDALONE, uiLocale()),
                            Modifier.weight(1f),
                            style = MaterialTheme.typography.labelMedium,
                            color = RoutineColors.TextSecondary,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
                dates.chunked(7).forEach { week ->
                    Row(horizontalArrangement = Arrangement.spacedBy(RoutineSpacing.xs)) {
                        week.forEach { date ->
                            val day = content.days.getValue(date)
                            val holiday = day.calendar.any { it.isWorkFreeDay }
                            val inMonth = YearMonth.from(date) == month
                            val heat = (day.metrics.focusMinutes / 300f).coerceIn(0f, 1f)
                            val background = when {
                                holiday -> RoutineColors.Recovery.container
                                heat > 0 -> RoutineColors.Primary.copy(alpha = 0.08f + heat * 0.35f)
                                else -> RoutineColors.Surface1
                            }
                            val description = if (holiday) stringResource(R.string.month_cell_off, date.toString())
                            else stringResource(
                                R.string.month_cell_description,
                                date.toString(), day.metrics.focusMinutes, day.metrics.examCount, day.metrics.milestoneCount,
                            )
                            Surface(
                                onClick = { onDate(date) },
                                modifier = Modifier.weight(1f).aspectRatioCell()
                                    .alpha(if (inMonth) 1f else 0.4f)
                                    .semantics { contentDescription = description },
                                shape = RoundedCornerShape(12.dp),
                                color = background,
                                border = if (date == today) androidx.compose.foundation.BorderStroke(2.dp, RoutineColors.Primary) else null,
                            ) {
                                Column(
                                    Modifier.fillMaxWidth().padding(RoutineSpacing.xs),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center,
                                ) {
                                    RoutineLabel(
                                        date.dayOfMonth.toString(),
                                        style = MaterialTheme.typography.titleSmall,
                                        color = RoutineColors.TextPrimary,
                                    )
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                                        modifier = Modifier.padding(top = RoutineSpacing.xs),
                                    ) {
                                        if (holiday) Dot(RoutineColors.Recovery.accent)
                                        if (day.metrics.examCount > 0) Dot(RoutineColors.Error)
                                        if (day.metrics.milestoneCount > day.metrics.examCount) Dot(RoutineColors.Error)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        item {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(RoutineSpacing.lg),
                verticalArrangement = Arrangement.spacedBy(RoutineSpacing.sm),
            ) {
                Legend(stringResource(R.string.legend_exam), RoutineColors.Error)
                Legend(stringResource(R.string.legend_deadline), RoutineColors.Error)
                Legend(stringResource(R.string.legend_no_school), RoutineColors.Recovery.accent)
                Legend(stringResource(R.string.legend_focus), RoutineColors.Primary)
            }
        }
        milestoneSection(R.string.month_markers, content.milestones.filter { YearMonth.from(it.dueDate) == month },
            content.taskMarkers.filter { YearMonth.from(it.dueDate) == month },
            content.goalMarkers.filter { YearMonth.from(it.dueDate) == month }, onGoals, onDate)
    }
}

/** Month cells keep one aspect ratio from the shared metrics instead of a local magic number. */
private fun Modifier.aspectRatioCell(): Modifier = aspectRatio(RoutineMetrics.MonthCellRatio)

@Composable
fun YearlyOverview(content: TimelineContent, preferences: SchedulePreferences, today: LocalDate, onGoals: () -> Unit = {}, topInset: Dp = 0.dp, onDate: (LocalDate) -> Unit) {
    val (start, end) = PeriodRanges.range(content.date, TimelineMode.YEAR)
    val target = preferences.teachingEndDate
    val targetInCycle = target in start..end
    val remaining = SlovenianAcademicCalendar.daysRemaining(today, target)
    val months = (0L..11L).map { YearMonth.from(start).plusMonths(it) }
    val upcomingMilestones = content.milestones.filter { !it.isCompleted && it.dueDate >= today }
    val upcomingTasks = content.taskMarkers.filter { !it.isCompleted && it.dueDate >= today }
    // Goal milestones are pre-filtered to open ones in the visible range; the radar should not miss them.
    val upcomingGoals = content.goalMarkers.filter { it.dueDate >= today }
    val upcoming = upcomingMilestones + upcomingTasks + upcomingGoals
    // The year view folds into panels: the countdown card is always visible, the rest opens on
    // demand, so nobody has to scroll past three screens of numbers to reach the one they want.
    var balanceOpen by rememberSaveable { mutableStateOf(true) }
    var radarOpen by rememberSaveable { mutableStateOf(false) }
    var recoveryOpen by rememberSaveable { mutableStateOf(true) }
    LazyColumn(
        contentPadding = PaddingValues(RoutineSpacing.lg, topInset + RoutineSpacing.md, RoutineSpacing.lg, 108.dp),
        verticalArrangement = Arrangement.spacedBy(RoutineSpacing.lg),
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = RoutineColors.Surface1),
                shape = RoutineShapes.Card,
                border = androidx.compose.foundation.BorderStroke(1.dp, RoutineColors.Border),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    Modifier.fillMaxWidth().padding(RoutineSpacing.lg),
                    verticalArrangement = Arrangement.spacedBy(RoutineSpacing.sm),
                ) {
                    RoutineLabel(stringResource(R.string.year_big_picture), style = MaterialTheme.typography.labelLarge,
                        color = RoutineColors.TextSecondary)
                    if (targetInCycle) {
                        RoutineLabel(
                            text = if (remaining > 0) remaining.toString() else stringResource(R.string.year_done),
                            style = MaterialTheme.typography.displaySmall,
                        )
                        RoutineText(
                            text = stringResource(if (remaining > 0) R.string.year_count_caption else R.string.year_done_caption),
                            maxLines = RoutineTextDefaults.Body,
                        )
                        RoutineLabel(
                            RoutineDate.full(target),
                            style = MaterialTheme.typography.labelLarge,
                        )
                        val total = ChronoUnit.DAYS.between(start, target).coerceAtLeast(1)
                        val elapsed = ChronoUnit.DAYS.between(start, today).coerceIn(0, total)
                        LinearProgressIndicator(
                            progress = { elapsed.toFloat() / total },
                            modifier = Modifier.fillMaxWidth().padding(top = RoutineSpacing.sm),
                        )
                    } else {
                        RoutineText(stringResource(R.string.year_no_target_title), style = MaterialTheme.typography.headlineSmall,
                            maxLines = RoutineTextDefaults.Body)
                        RoutineText(stringResource(R.string.year_no_target_body), maxLines = RoutineTextDefaults.Paragraph)
                    }
                    RoutineText(stringResource(R.string.year_profile_hint), style = MaterialTheme.typography.bodySmall,
                        color = RoutineColors.TextSecondary, maxLines = RoutineTextDefaults.Paragraph)
                }
            }
        }
        item(key = "year-balance") {
            CollapsibleSection(
                title = stringResource(R.string.year_balance),
                subtitle = stringResource(R.string.school_cycle_label),
                expanded = balanceOpen,
                onToggle = { balanceOpen = !balanceOpen },
                tag = "year-balance-toggle",
            ) {
                if (!SlovenianAcademicCalendar.covers(start)) {
                    RoutineText(stringResource(R.string.coverage_warning), color = RoutineColors.Error,
                        style = MaterialTheme.typography.bodySmall, maxLines = RoutineTextDefaults.Paragraph)
                }
                Column(Modifier.fillMaxWidth().testTag("year-month-grid"),
                    verticalArrangement = Arrangement.spacedBy(RoutineSpacing.sm)) {
                    months.chunked(3).forEach { row ->
                        Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(RoutineSpacing.sm)) {
                            row.forEach { month ->
                                val monthDays = content.days.values.filter { YearMonth.from(it.date) == month }
                                val markers = monthDays.sumOf { it.metrics.milestoneCount }
                                val off = monthDays.count { day -> day.calendar.any { it.isWorkFreeDay } }
                                OutlinedCard(
                                    onClick = { onDate(month.atDay(1)) },
                                    shape = RoutineShapes.Card,
                                    modifier = Modifier.weight(1f).fillMaxHeight(),
                                ) {
                                    Column(
                                        Modifier.fillMaxWidth().padding(RoutineSpacing.md),
                                        verticalArrangement = Arrangement.spacedBy(RoutineSpacing.xs),
                                    ) {
                                        RoutineLabel(RoutineDate.monthTight(month),
                                            style = MaterialTheme.typography.titleMedium)
                                        RoutineLabel(stringResource(R.string.year_markers_count, markers),
                                            style = MaterialTheme.typography.labelSmall, color = RoutineColors.TextSecondary)
                                        RoutineLabel(stringResource(R.string.year_days_off, off),
                                            style = MaterialTheme.typography.labelSmall, color = RoutineColors.TextSecondary)
                                        LinearProgressIndicator(
                                            progress = { off.toFloat() / month.lengthOfMonth() },
                                            modifier = Modifier.fillMaxWidth().padding(top = RoutineSpacing.xs),
                                            color = RoutineColors.Success,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        item(key = "year-radar") { MilestoneRadar(upcoming, today, radarOpen) { radarOpen = !radarOpen } }
        item(key = "year-recovery") {
            CollapsibleSection(
                title = stringResource(R.string.year_recovery_heading),
                expanded = recoveryOpen,
                onToggle = { recoveryOpen = !recoveryOpen },
                tag = "year-recovery-toggle",
            ) {
                val vacations = content.calendar.filter { it.isWorkFreeDay && (it.title.contains("počitnice") || it.title.contains("oddih")) }
                    .groupBy { it.title }.entries.sortedBy { entry -> entry.value.minOf { it.date } }
                if (vacations.isEmpty()) {
                    RoutineText(stringResource(R.string.year_no_vacations), style = MaterialTheme.typography.bodyMedium,
                        maxLines = RoutineTextDefaults.Paragraph)
                }
                vacations.forEach { (title, dates) ->
                    ListItem(
                        headlineContent = {
                            RoutineText(title, style = MaterialTheme.typography.bodyLarge, maxLines = RoutineTextDefaults.Body)
                        },
                        supportingContent = {
                            RoutineLabel(
                                stringResource(
                                    R.string.date_range,
                                    RoutineDate.normal(dates.minOf { it.date }),
                                    RoutineDate.normalYear(dates.maxOf { it.date }),
                                ),
                                style = MaterialTheme.typography.labelMedium,
                                color = RoutineColors.TextSecondary,
                            )
                        },
                        trailingContent = {
                            RoutineLabel(dates.size.toString(), style = MaterialTheme.typography.labelLarge)
                        },
                        modifier = Modifier.clickable { onDate(dates.minOf { it.date }) },
                    )
                }
            }
        }
        milestoneSection(R.string.upcoming_milestones, upcomingMilestones, upcomingTasks, upcomingGoals, onGoals, onDate)
    }
}

/**
 * Eight-week milestone radar. Bars are weighted children of a fixed-height column, so the counts and
 * the week labels always keep their own space and can never be clipped by the tallest bar.
 */
@Composable
private fun MilestoneRadar(milestones: List<Milestone>, today: LocalDate, expanded: Boolean, onToggle: () -> Unit) {
    val weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    val buckets = (0L..7L).map { index ->
        val start = weekStart.plusWeeks(index)
        start to milestones.count { it.dueDate in start..start.plusDays(6) }
    }
    val largest = buckets.maxOf { it.second }.coerceAtLeast(1)
    CollapsibleSection(
        title = stringResource(R.string.milestone_radar),
        subtitle = stringResource(R.string.milestone_radar_hint),
        expanded = expanded,
        onToggle = onToggle,
        tag = "milestone-radar-toggle",
    ) {
        Row(
            Modifier.fillMaxWidth().height(140.dp).testTag("milestone-radar-chart"),
            horizontalArrangement = Arrangement.spacedBy(RoutineSpacing.sm),
        ) {
            buckets.forEach { (start, count) ->
                val fraction = count.toFloat() / largest
                Column(
                    Modifier.weight(1f).fillMaxHeight(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    RoutineLabel(count.toString(), style = MaterialTheme.typography.labelSmall,
                        color = RoutineColors.TextSecondary)
                    Spacer(Modifier.weight((1f - fraction).coerceAtLeast(0.05f)))
                    Box(
                        Modifier.fillMaxWidth().weight(fraction.coerceAtLeast(0.02f))
                            .clip(RoundedCornerShape(6.dp)).background(RoutineColors.Error.copy(alpha = 0.7f)),
                    )
                    Spacer(Modifier.height(RoutineSpacing.xs))
                    RoutineLabel(RoutineDate.axisDay(start),
                        style = MaterialTheme.typography.labelSmall, color = RoutineColors.TextMuted)
                }
            }
        }
    }
}

private fun LazyListScope.milestoneSection(
    @androidx.annotation.StringRes title: Int,
    milestones: List<Milestone>,
    taskMarkers: List<Milestone> = emptyList(),
    goalMarkers: List<Milestone> = emptyList(),
    onGoals: () -> Unit = {},
    onDate: (LocalDate) -> Unit,
) {
    // Tasks with a due date reuse the marker row; the id prefix keeps the two tables' id spaces apart in list keys.
    val entries = milestones.map { it to "m" } + taskMarkers.map { it to "t" } + goalMarkers.map { it to "g" }
    item(key = "markers-heading:$title") {
        RoutineText(stringResource(title), style = MaterialTheme.typography.titleLarge, maxLines = RoutineTextDefaults.Body)
    }
    if (entries.isEmpty()) {
        item(key = "markers-empty:$title") {
            RoutineText(stringResource(R.string.no_milestones), style = MaterialTheme.typography.bodyMedium,
                color = RoutineColors.TextSecondary, maxLines = RoutineTextDefaults.Paragraph)
        }
    }
    items(entries, key = { "marker:$title:${it.second}:${it.first.id}" }, contentType = { "milestone" }) { (marker, source) ->
        OutlinedCard(
            onClick = { if (source == "g") onGoals() else onDate(marker.dueDate) },
            shape = RoutineShapes.Card,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                Modifier.fillMaxWidth().padding(RoutineSpacing.lg),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(RoutineSpacing.md),
            ) {
                Dot(
                    when {
                        source == "g" -> RoutineColors.FocusAccent
                        marker.isExam -> RoutineColors.Error
                        else -> RoutineColors.Error
                    },
                )
                Column(Modifier.weight(1f)) {
                    RoutineText(marker.title, style = MaterialTheme.typography.titleMedium, maxLines = RoutineTextDefaults.Body)
                    RoutineLabel(
                        when {
                            source == "g" -> stringResource(R.string.marker_goal)
                            source == "t" -> stringResource(R.string.tasks_open)
                            marker.isCompleted -> stringResource(
                                R.string.completed_marker,
                                stringResource(if (marker.isExam) R.string.entry_exam else R.string.entry_deadline),
                            )
                            else -> stringResource(if (marker.isExam) R.string.entry_exam else R.string.entry_deadline)
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = RoutineColors.TextSecondary,
                    )
                }
                RoutineLabel(
                    RoutineDate.normal(marker.dueDate),
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    }
}

@Composable
private fun Dot(color: Color) {
    Box(Modifier.size(6.dp).clip(CircleShape).background(color))
}
