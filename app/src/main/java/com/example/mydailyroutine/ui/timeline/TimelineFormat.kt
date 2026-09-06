package com.example.mydailyroutine.ui.timeline

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.example.mydailyroutine.domain.health.WarningType
import com.example.mydailyroutine.domain.model.RoutineCategory
import java.time.LocalTime
import java.time.format.DateTimeFormatter

val clockFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
fun LocalTime.clockLabel(): String = format(clockFormat)
fun minuteLabel(minute: Int): String = if (minute == 1440) "24:00" else LocalTime.ofSecondOfDay(minute * 60L).clockLabel()
fun durationLabel(minutes: Int): String = when {
    minutes < 60 -> "$minutes min"
    minutes % 60 == 0 -> "${minutes / 60}h"
    else -> "${minutes / 60}h ${minutes % 60}m"
}
fun RoutineCategory.label(): String = when (this) {
    RoutineCategory.SCHOOL -> "School"
    RoutineCategory.FOCUS_STUDY -> "Deep work"
    RoutineCategory.REST_BREAK -> "Recovery"
    RoutineCategory.PROJECT -> "Project"
    RoutineCategory.PERSONAL -> "Personal"
}
fun WarningType.label(): String = when (this) {
    WarningType.CONCENTRATION_LIMIT -> "Focus > 90 min"
    WarningType.HIGH_COGNITIVE_LOAD -> "High cognitive load"
    WarningType.INSUFFICIENT_TRANSITION -> "School transition"
    WarningType.BURNOUT_RISK -> "Daily focus ceiling"
    WarningType.PHYSICAL_RESET -> "Time to move"
    WarningType.FRAGMENTED_TIME -> "Fragmented time"
}
@Composable
fun categoryColor(category: RoutineCategory, subjectColor: Long? = null): Color = when (category) {
    RoutineCategory.SCHOOL -> subjectColor?.let { Color(it.toInt()) } ?: MaterialTheme.colorScheme.secondary
    RoutineCategory.FOCUS_STUDY -> MaterialTheme.colorScheme.primary
    RoutineCategory.REST_BREAK -> MaterialTheme.colorScheme.outline
    RoutineCategory.PROJECT -> MaterialTheme.colorScheme.tertiary
    RoutineCategory.PERSONAL -> MaterialTheme.colorScheme.secondary
}
