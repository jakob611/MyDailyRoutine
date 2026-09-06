package com.example.mydailyroutine.ui.theme

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.mydailyroutine.domain.model.RoutineCategory

/** The only palette used by Compose, notifications' accents, and Glance. */
object RoutineColors {
    val Background = Color(0xFF000000)
    val Surface1 = Color(0xFF121316)
    val Surface2 = Color(0xFF1A1C20)
    val Border = Color.White.copy(alpha = 0.08f)
    val CardBorder = Color.White.copy(alpha = 0.06f)
    val TextPrimary = Color(0xFFF8FAFC)
    val TextSecondary = Color(0xFF94A3B8)
    val TextMuted = Color(0xFF64748B)
    val Spine = Color(0xFF22242A)
    val Cobalt = Color(0xFF3B82F6)
    val Amber = Color(0xFFF59E0B)
    val Sage = Color(0xFF10B981)
    val Crimson = Color(0xFFEF4444)
    val Violet = Color(0xFF8B5CF6)
    val Warning = Color(0xFFFB923C)
    val WarningContainer = Color(0xFF341A0B)
    val School = CategoryStyle(Cobalt, Color(0xFF1E293B), Color(0xFF93C5FD))
    val Focus = CategoryStyle(Amber, Color(0xFF2E210D), Color(0xFFFCD34D))
    val Recovery = CategoryStyle(Sage, Color(0xFF0A261D), Color(0xFF6EE7B7))
    val Exam = CategoryStyle(Crimson, Color(0xFF2D1214), Color(0xFFFCA5A5))
    val Project = CategoryStyle(Violet, Color(0xFF24153B), Color(0xFFC4B5FD))
    val Personal = CategoryStyle(TextMuted, Surface2, TextSecondary)
    val subjectSwatches = listOf(0xFF3B82F6L, 0xFF10B981L, 0xFFF59E0BL, 0xFFEF4444L, 0xFF8B5CF6L, 0xFF64748BL)
}

@Immutable
data class CategoryStyle(val accent: Color, val container: Color, val content: Color)

fun categoryStyle(category: RoutineCategory, subjectColor: Long? = null): CategoryStyle = when (category) {
    RoutineCategory.SCHOOL -> RoutineColors.School.let { style -> subjectColor?.let { style.copy(accent = Color(it.toInt())) } ?: style }
    RoutineCategory.FOCUS_STUDY -> RoutineColors.Focus
    RoutineCategory.REST_BREAK -> RoutineColors.Recovery
    RoutineCategory.PROJECT -> RoutineColors.Project
    RoutineCategory.PERSONAL -> RoutineColors.Personal
}

object RoutineShapes {
    val Card = RoundedCornerShape(16.dp)
    val Chip = RoundedCornerShape(8.dp)
    val Pill = RoundedCornerShape(50)
    val Sheet = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
}

val SnappySpring = spring<Float>(Spring.DampingRatioNoBouncy, Spring.StiffnessMediumLow)
val PopSpring = spring<Float>(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium)
const val TransitionMillis = 180

val OledColorScheme = darkColorScheme(
    primary = RoutineColors.Amber, onPrimary = RoutineColors.Background,
    primaryContainer = RoutineColors.Focus.container, onPrimaryContainer = RoutineColors.Focus.content,
    secondary = RoutineColors.Cobalt, onSecondary = RoutineColors.Background,
    secondaryContainer = RoutineColors.School.container, onSecondaryContainer = RoutineColors.School.content,
    tertiary = RoutineColors.Violet, onTertiary = RoutineColors.Background,
    tertiaryContainer = RoutineColors.Project.container, onTertiaryContainer = RoutineColors.Project.content,
    error = RoutineColors.Crimson, onError = RoutineColors.Background,
    errorContainer = RoutineColors.Exam.container, onErrorContainer = RoutineColors.Exam.content,
    background = RoutineColors.Background, onBackground = RoutineColors.TextPrimary,
    surface = RoutineColors.Surface1, onSurface = RoutineColors.TextPrimary,
    surfaceVariant = RoutineColors.Surface2, onSurfaceVariant = RoutineColors.TextSecondary,
    surfaceContainerLowest = RoutineColors.Background,
    surfaceContainerLow = RoutineColors.Surface1, surfaceContainer = RoutineColors.Surface1,
    surfaceContainerHigh = RoutineColors.Surface2, surfaceContainerHighest = RoutineColors.Surface2,
    surfaceDim = RoutineColors.Surface1, surfaceBright = RoutineColors.Surface2,
    outline = RoutineColors.TextMuted, outlineVariant = RoutineColors.Border,
    inverseSurface = RoutineColors.TextPrimary, inverseOnSurface = RoutineColors.Background,
    scrim = Color.Black,
)
