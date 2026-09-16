package com.example.mydailyroutine.core.designsystem.theme

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
    val Crimson = Color(0xFFFB7185)
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
    RoutineCategory.FOCUS_ANALYTICAL -> RoutineColors.Focus
    RoutineCategory.EMERGENCY_RESERVE -> RoutineColors.Recovery
    RoutineCategory.REST_BUFFER -> RoutineColors.Recovery
    RoutineCategory.FOCUS_SYNTHESIZING -> RoutineColors.Project
    RoutineCategory.ADMIN -> RoutineColors.Personal
}

object RoutineShapes {
    val Card = RoundedCornerShape(16.dp)
    val Chip = RoundedCornerShape(8.dp)
    val Pill = RoundedCornerShape(50)
    val Sheet = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
}

/**
 * The only spacing ladder in the app: 4/8/12/16/24. Screens that need "a bit more" pick the next
 * step instead of inventing a value, so cards, sheets and grids share the same rhythm.
 */
object RoutineSpacing {
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 24.dp
}

/**
 * Shared geometry. Every time column, spine and day-view gap uses these three numbers, which is what
 * keeps hours, cards and the vertical spine aligned across the day, week and milestone rows.
 */
object RoutineMetrics {
    /** Width of the printed hour inside the gutter; also the hour column of the weekly grid. */
    val GutterTextWidth = 48.dp
    /** Gap between the hour text and the card it belongs to. */
    val GutterGap = 8.dp
    /** Total width of the day-view time column. */
    val GutterWidth = GutterTextWidth + GutterGap
    val SpineWidth = 1.5.dp
    /** x of the vertical rail: centred inside the gutter gap. Declared after its inputs on purpose. */
    val RailX = GutterTextWidth + (GutterGap - SpineWidth) / 2
    val CategoryBarWidth = 4.dp
    /** Left inset of card content, clearing the category bar. */
    val CardContentStart = 12.dp
    /** Minimum height of a day-view card so short blocks stay readable and tappable. */
    val CardMinHeight = 96.dp
    /** A block narrower or shorter than this shows colour only; its title would be clipped. */
    val MinLabelWidth = 56.dp
    val MinLabelHeight = 20.dp
    /** Minimum touch width for anything that reads as a button. */
    val ActionMinWidth = 48.dp
    /** Categorical chips are capped so one long label cannot own a whole card line. */
    val ChipMaxWidth = 190.dp
    /** Geometry of the NOW band drawn over the running block. */
    val NowBandHeight = 18.dp
    val NowDotSize = 8.dp
    /** Week grid scale: vertical dp per scheduled minute. */
    val WeekMinuteHeight = 0.9.dp
    /** Month cell aspect ratio; wide enough for a day number and one status dot. */
    val MonthCellRatio = 0.85f
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
