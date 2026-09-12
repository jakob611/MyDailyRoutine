package com.example.mydailyroutine.features.goals.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.matchParentSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.mydailyroutine.core.designsystem.theme.*
import com.example.mydailyroutine.domain.model.GoalActivity
import com.example.mydailyroutine.domain.model.GoalMilestone
import com.example.mydailyroutine.domain.model.GoalsProject
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit

internal fun GoalBar(fraction: Float, color: Color) {
    Box(Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)).background(RoutineColors.Surface2)) {
        Box(Modifier.fillMaxWidth(fraction.coerceIn(0f, 1f)).height(8.dp).clip(RoundedCornerShape(4.dp)).background(color))
    }
}

/**
 * Month-scale timeline. Positions are linear in days across the project window (an approximation of
 * calendar months, deliberate for a planning glance); month labels sit on true month boundaries.
 */
@Composable
internal fun GoalGantt(project: GoalsProject, activities: List<GoalActivity>, milestones: List<GoalMilestone>, onActivity: (GoalActivity) -> Unit) {
    val today = LocalDate.now()
    val spanDays = maxOf(1L, ChronoUnit.DAYS.between(project.start, project.end))
    val monthCount = (spanDays / 31 + 1).toInt().coerceIn(2, 30)
    val gridWidth = (monthCount * 64f).dp
    fun xOf(date: LocalDate): Dp {
        val days = ChronoUnit.DAYS.between(project.start, date).toFloat().coerceIn(0f, spanDays.toFloat())
        return (days / spanDays.toFloat() * monthCount * 64f).dp
    }
    Column(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
        Box {
            Column(Modifier.width(gridWidth)) {
                Box(Modifier.fillMaxWidth().height(20.dp)) {
                    for (index in 0 until monthCount) {
                        val month = YearMonth.from(project.start).plusMonths(index.toLong())
                        val markerDate = if (index == 0) project.start else maxOf(month.atDay(1), project.start)
                        Box(Modifier.offset(x = xOf(markerDate)).padding(start = 4.dp)) {
                            Text(month.format(GoalMonthFormat), style = MaterialTheme.typography.labelSmall, color = RoutineColors.TextSecondary, maxLines = 1)
                        }
                    }
                }
                Box(Modifier.fillMaxWidth().height(26.dp)) {
                    milestones.forEach { milestone ->
                        Box(Modifier.offset(x = maxOf(0.dp, xOf(milestone.dueDate) - 5.dp), y = 7.dp).size(11.dp).rotate(45f)
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
