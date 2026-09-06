package com.example.mydailyroutine.ui.overview

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mydailyroutine.domain.calendar.SlovenianAcademicCalendar
import com.example.mydailyroutine.domain.health.WeeklyLayout
import com.example.mydailyroutine.domain.model.Milestone
import com.example.mydailyroutine.domain.model.ResolvedTimelineItem
import com.example.mydailyroutine.domain.model.SchedulePreferences
import com.example.mydailyroutine.ui.timeline.*
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters

@Composable
fun WeeklyOverview(content: TimelineContent, onDate: (LocalDate) -> Unit) {
    val days = content.days.values.sortedBy { it.date }
    val blocks = days.flatMap { it.items.filterIsInstance<ResolvedTimelineItem.Block>() }
    val startHour = minOf(7, (blocks.minOfOrNull { it.startMinute } ?: 420) / 60)
    val endHour = maxOf(21, ((blocks.maxOfOrNull { it.endMinute } ?: 1260) + 59) / 60).coerceAtMost(24)
    val minuteHeight = 0.9.dp
    val gridHeight = minuteHeight * ((endHour - startHour) * 60)
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    LazyColumn(contentPadding = PaddingValues(16.dp, 12.dp, 16.dp, 108.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        item {
            Text("Your week, at a glance", style = MaterialTheme.typography.titleLarge)
            Text("${durationLabel(days.sumOf { it.metrics.focusMinutes })} deep work · ${durationLabel(days.sumOf { it.metrics.recoveryMinutes })} recovery",
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("Scroll across the grid. Tap a day or block to open it.", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 6.dp))
            if (!SlovenianAcademicCalendar.covers(content.date)) Text("No verified holiday data outside 2026/27.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        }
        item {
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                val gridWidth = maxOf(maxWidth, 748.dp)
                val dayWidth = (gridWidth - 48.dp) / 7
                Column(Modifier.horizontalScroll(rememberScrollState())) {
                    Row(Modifier.width(gridWidth)) {
                        Spacer(Modifier.width(48.dp))
                        days.forEach { day ->
                            Column(Modifier.width(dayWidth).clickable { onDate(day.date) }.padding(6.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                Text(day.date.format(DateTimeFormatter.ofPattern("EEE d")), style = MaterialTheme.typography.labelLarge)
                                Text("${durationLabel(day.metrics.focusMinutes)} focus", style = MaterialTheme.typography.labelSmall)
                                LinearProgressIndicator(progress = { (day.metrics.occupiedMinutes / 840f).coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth())
                            }
                        }
                    }
                    Row(Modifier.width(gridWidth).height(gridHeight)) {
                        Column(Modifier.width(48.dp)) {
                            (startHour until endHour).forEach { hour ->
                                Text("%02d:00".format(hour), style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.height(minuteHeight * 60).padding(top = 3.dp))
                            }
                        }
                        days.forEach { day ->
                            Box(Modifier.width(dayWidth).height(gridHeight).drawBehind {
                                drawLine(gridColor, Offset.Zero, Offset(0f, size.height), 1.dp.toPx())
                                for (hour in 0..(endHour - startHour)) {
                                    val y = (minuteHeight * (hour * 60)).toPx()
                                    drawLine(gridColor, Offset(0f, y), Offset(size.width, y), 1.dp.toPx())
                                }
                            }) {
                                val positioned = remember(day.items) { WeeklyLayout.position(day.items.filterIsInstance<ResolvedTimelineItem.Block>()) }
                                positioned.forEach { position ->
                                    val block = position.block
                                    val laneWidth = dayWidth / position.laneCount
                                    val accent = categoryColor(block.category, block.subject?.colorHex)
                                    Column(
                                        Modifier.offset(x = laneWidth * position.lane + 2.dp, y = minuteHeight * (block.startMinute - startHour * 60))
                                            .width(maxOf(1.dp, laneWidth - 4.dp)).height(maxOf(18.dp, minuteHeight * block.durationMinutes - 2.dp))
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(if (block.isSuppressed) MaterialTheme.colorScheme.surfaceContainerHigh else accent.copy(alpha = 0.22f))
                                            .clickable { onDate(day.date) }
                                            .semantics { contentDescription = "${block.title}, ${day.date}, ${minuteLabel(block.startMinute)} to ${minuteLabel(block.endMinute)}" }
                                            .padding(4.dp),
                                    ) {
                                        Text(block.title, fontSize = 11.sp, lineHeight = 12.sp, maxLines = if (block.durationMinutes >= 40) 2 else 1, overflow = TextOverflow.Ellipsis)
                                        if (block.durationMinutes >= 60) Text(if (block.isSuppressed) "No school" else durationLabel(block.durationMinutes), fontSize = 10.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        milestoneSection("This week’s markers", content.milestones, onDate)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MonthlyOverview(content: TimelineContent, today: LocalDate, onDate: (LocalDate) -> Unit) {
    val month = YearMonth.from(content.date)
    val dates = content.days.keys.sorted()
    LazyColumn(contentPadding = PaddingValues(16.dp, 12.dp, 16.dp, 108.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        item {
            Text("Build a rhythm, not a streak.", style = MaterialTheme.typography.titleLarge)
            Text("Deeper shading means more planned focus. Recovery days count, too.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (!SlovenianAcademicCalendar.covers(content.date)) Text("No verified holiday data outside 2026/27.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(Modifier.fillMaxWidth()) {
                    DayOfWeek.values().forEach { Text(it.name.take(1), Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.Center, style = MaterialTheme.typography.labelMedium) }
                }
                dates.chunked(7).forEach { week ->
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        week.forEach { date ->
                            val day = content.days.getValue(date)
                            val holiday = day.calendar.any { it.isWorkFreeDay }
                            val inMonth = YearMonth.from(date) == month
                            val heat = (day.metrics.focusMinutes / 300f).coerceIn(0f, 1f)
                            val background = when {
                                holiday -> MaterialTheme.colorScheme.secondaryContainer
                                heat > 0 -> MaterialTheme.colorScheme.primary.copy(alpha = 0.08f + heat * 0.35f)
                                else -> MaterialTheme.colorScheme.surfaceContainerLow
                            }
                            Surface(
                                onClick = { onDate(date) },
                                modifier = Modifier.weight(1f).aspectRatio(0.85f).alpha(if (inMonth) 1f else 0.4f)
                                    .semantics { contentDescription = "$date, ${day.metrics.focusMinutes} focus minutes, ${day.metrics.examCount} exams, ${day.metrics.milestoneCount} markers${if (holiday) ", school holiday" else ""}" },
                                shape = RoundedCornerShape(12.dp), color = background,
                                border = if (date == today) androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                                    Text(date.dayOfMonth.toString(), style = MaterialTheme.typography.titleSmall)
                                    Row(horizontalArrangement = Arrangement.spacedBy(3.dp), modifier = Modifier.padding(top = 5.dp)) {
                                        if (day.metrics.examCount > 0) Dot(MaterialTheme.colorScheme.error)
                                        if (day.metrics.milestoneCount > day.metrics.examCount) Dot(MaterialTheme.colorScheme.tertiary)
                                    }
                                    if (holiday) Text("Off", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }
            }
        }
        item {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Legend("Exam", MaterialTheme.colorScheme.error)
                Legend("Deadline", MaterialTheme.colorScheme.tertiary)
                Legend("No school", MaterialTheme.colorScheme.secondaryContainer)
                Legend("Focus", MaterialTheme.colorScheme.primary)
            }
        }
        milestoneSection("Dates to keep in mind", content.milestones.filter { YearMonth.from(it.dueDate) == month }, onDate)
    }
}

@Composable
fun YearlyOverview(content: TimelineContent, preferences: SchedulePreferences, today: LocalDate, onDate: (LocalDate) -> Unit) {
    val (start, end) = TimelineViewModel.range(content.date, TimelineMode.YEAR)
    val target = preferences.teachingEndDate
    val targetInCycle = target in start..end
    val remaining = SlovenianAcademicCalendar.daysRemaining(today, target)
    val months = (0L..11L).map { YearMonth.from(start).plusMonths(it) }
    val upcoming = content.milestones.filter { !it.isCompleted && it.dueDate >= today }
    LazyColumn(contentPadding = PaddingValues(16.dp, 12.dp, 16.dp, 108.dp), verticalArrangement = Arrangement.spacedBy(22.dp)) {
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer), shape = RoundedCornerShape(24.dp)) {
                Column(Modifier.fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("THE BIG PICTURE", style = MaterialTheme.typography.labelLarge)
                    if (targetInCycle) {
                        Text(if (remaining > 0) "$remaining days" else "You made it.", style = MaterialTheme.typography.displaySmall)
                        Text(if (remaining > 0) "until your teaching year ends" else "Your configured teaching end date has passed.")
                        Text(target.format(DateTimeFormatter.ofPattern("d MMMM yyyy")), style = MaterialTheme.typography.labelLarge)
                        val total = ChronoUnit.DAYS.between(start, target).coerceAtLeast(1)
                        val elapsed = ChronoUnit.DAYS.between(start, today).coerceIn(0, total)
                        LinearProgressIndicator(progress = { elapsed.toFloat() / total }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
                    } else {
                        Text("Your year, your pace.", style = MaterialTheme.typography.headlineSmall)
                        Text("Set a teaching end date for this cycle in Settings to see a countdown.")
                    }
                    Text("Default: 24 June 2027, non-final years. Final-year and IB calendars may end earlier; your date is editable.", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        item {
            Text("Twelve months of balance", style = MaterialTheme.typography.titleLarge)
            Text("${SlovenianAcademicCalendar.CYCLE_LABEL} calendar bundled offline", style = MaterialTheme.typography.bodySmall)
            if (!SlovenianAcademicCalendar.covers(start)) Text("No verified vacation data for this cycle.", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 12.dp)) {
                months.chunked(3).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { month ->
                            val monthDays = content.days.values.filter { YearMonth.from(it.date) == month }
                            val markers = monthDays.sumOf { it.metrics.milestoneCount }
                            val off = monthDays.count { day -> day.calendar.any { it.isWorkFreeDay } }
                            OutlinedCard(onClick = { onDate(month.atDay(1)) }, modifier = Modifier.weight(1f)) {
                                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                    Text(month.format(DateTimeFormatter.ofPattern("MMM")), style = MaterialTheme.typography.titleMedium)
                                    Text("$markers markers", style = MaterialTheme.typography.labelSmall)
                                    Text("$off days off", style = MaterialTheme.typography.labelSmall)
                                    LinearProgressIndicator(progress = { off.toFloat() / month.lengthOfMonth() }, modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.secondary)
                                }
                            }
                        }
                    }
                }
            }
        }
        item { MilestoneRadar(upcoming, today) }
        item {
            Text("Room to recover", style = MaterialTheme.typography.titleLarge)
            val vacations = content.calendar.filter { it.isWorkFreeDay && (it.title.contains("počitnice") || it.title.contains("oddih")) }
                .groupBy { it.title }.entries.sortedBy { entry -> entry.value.minOf { it.date } }
            if (vacations.isEmpty()) Text("No bundled vacation ranges in this cycle.", style = MaterialTheme.typography.bodyMedium)
            vacations.forEach { (title, dates) ->
                ListItem(headlineContent = { Text(title) }, supportingContent = {
                    Text("${dates.minOf { it.date }.format(DateTimeFormatter.ofPattern("d MMM"))} – ${dates.maxOf { it.date }.format(DateTimeFormatter.ofPattern("d MMM yyyy"))}")
                }, trailingContent = { Text("${dates.size}d", style = MaterialTheme.typography.labelLarge) },
                    modifier = Modifier.clickable { onDate(dates.minOf { it.date }) })
            }
        }
        milestoneSection("Upcoming milestones", upcoming, onDate)
    }
}

@Composable
private fun MilestoneRadar(milestones: List<Milestone>, today: LocalDate) {
    val weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    val buckets = (0L..7L).map { index ->
        val start = weekStart.plusWeeks(index)
        start to milestones.count { it.dueDate in start..start.plusDays(6) }
    }
    val largest = buckets.maxOf { it.second }.coerceAtLeast(1)
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Milestone radar", style = MaterialTheme.typography.titleLarge)
        Text("Deadline density over the next eight weeks · includes exams, IA, EE and TOK entries you add.", style = MaterialTheme.typography.bodySmall)
        Row(Modifier.fillMaxWidth().height(130.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Bottom) {
            buckets.forEach { (start, count) ->
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(count.toString(), style = MaterialTheme.typography.labelSmall)
                    Box(Modifier.fillMaxWidth().height(maxOf(3.dp, (count.toFloat() / largest * 75).dp))
                        .clip(RoundedCornerShape(6.dp)).background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.7f)))
                    Text(start.format(DateTimeFormatter.ofPattern("d/M")), style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

private fun LazyListScope.milestoneSection(title: String, milestones: List<Milestone>, onDate: (LocalDate) -> Unit) {
    item(key = "markers-heading:$title") { Text(title, style = MaterialTheme.typography.titleLarge) }
    if (milestones.isEmpty()) item(key = "markers-empty:$title") {
        Text("No markers yet. Add exams and IA, EE or TOK deadlines when your school confirms their dates.",
            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    items(milestones, key = { "marker:$title:${it.id}" }, contentType = { "milestone" }) { marker ->
        OutlinedCard(onClick = { onDate(marker.dueDate) }, modifier = Modifier.fillMaxWidth()) {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Dot(if (marker.isExam) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiary)
                Column(Modifier.weight(1f)) {
                    Text(marker.title, fontWeight = FontWeight.Medium)
                    Text("${if (marker.isExam) "Exam" else "Deadline"}${if (marker.isCompleted) " · completed" else ""}", style = MaterialTheme.typography.labelSmall)
                }
                Text(marker.dueDate.format(DateTimeFormatter.ofPattern("d MMM")), style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
private fun Dot(color: Color) { Box(Modifier.size(6.dp).clip(CircleShape).background(color)) }
@Composable
private fun Legend(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Dot(color); Text(label, style = MaterialTheme.typography.labelSmall)
    }
}
