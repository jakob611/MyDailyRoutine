package com.example.mydailyroutine.core.designsystem.theme

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.unit.dp
import com.example.mydailyroutine.domain.model.RoutineCategory

/**
 * Deep Oceanic Slate: the single palette for Compose, notifications and RemoteViews.
 * Brand values are a product decision, not claimed to be official Apple/Material tokens.
 * `tools/derive_palette.py --check` verifies the brief and the generated XML mirror;
 * `tools/check_contrast.py` checks actual opaque and composited combinations.
 */
object RoutineColors {
    val Background = Color(0xFF090D16)
    val SurfaceLowest = Color(0xFF05070B)
    val SurfaceLow = Color(0xFF0F1422)
    val SurfaceContainer = Color(0xFF151C2E)
    val SurfaceHigh = Color(0xFF1D263D)
    val SurfaceHighest = Color(0xFF26324F)
    val SheetSurface = SurfaceHighest

    val TextPrimary = Color(0xFFF1F5F9)
    val TextSecondary = Color(0xFFA8B3C2)
    /** Decorative/disabled content only: not AA for small text on raised surfaces. */
    val TextTertiary = Color(0xFF718096)
    val TextDisabled = Color(0xFF526077)
    val Border = Color(0xFF263247)
    val BorderStrong = TextTertiary
    val CardBorder = Border
    val Spine = Border

    val Primary = Color(0xFF2DD4BF)
    val Timer = Color(0xFF67E8F9)
    val FocusAccent = Color(0xFFA78BFA)
    val Success = Color(0xFF34D399)
    val Warning = Color(0xFFFBBF24)
    val Error = Color(0xFFFB7185)
    /** Category colours do not substitute for interaction/status roles. */
    val Cobalt = Color(0xFF93C5FD)
    val RecoveryAccent = Color(0xFFA3CF96)
    val ProjectAccent = Color(0xFFD8A6D8)
    val Neutral = TextSecondary
    val InkOnPrimary = Background
    val InkOnSecondary = Background
    val InkOnTertiary = Background
    val InkOnError = Background

    val AmbientTop = Color(0xFF0EA5E9)
    val AmbientBottom = Color(0xFF8B5CF6)
    val AmbientTopAlpha = 0.07f
    val AmbientBottomAlpha = 0.045f
    val GlassRim = Color.White

    val PrimaryContainer = Color(0xFF19383E)
    val WarningContainer = Color(0xFF383222)
    val ErrorContainer = Color(0xFF3D293B)
    val School = CategoryStyle(Cobalt, Color(0xFF253148), TextPrimary)
    val Focus = CategoryStyle(FocusAccent, Color(0xFF272A48), TextPrimary)
    val Recovery = CategoryStyle(RecoveryAccent, Color(0xFF27323B), TextPrimary)
    val Exam = CategoryStyle(Error, ErrorContainer, TextPrimary)
    val Project = CategoryStyle(ProjectAccent, Color(0xFF2D2D43), TextPrimary)
    val Personal = CategoryStyle(Neutral, SurfaceContainer, TextPrimary)

    /** Small category tint over an opaque surface; arbitrary saved subject colours stay decorative. */
    fun cardSurface(accent: Color): Color = accent.copy(alpha = 0.06f).compositeOver(SurfaceContainer)

    val subjectSwatches = listOf(Cobalt, RecoveryAccent, FocusAccent, Error, ProjectAccent, Primary)
        .map { it.toArgb().toLong() and 0xFFFFFFFFL }
}

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
    /**
     * Glass shapes. The lens effect refracts by the corner radius, so every glass shape keeps a
     * minimum radius of at least 12 dp on the corners the reader can actually see; corners that sit
     * at the screen edge may be square because their refraction is off-screen anyway.
     */
    val GlassTopBar = RoundedCornerShape(bottomStart = 26.dp, bottomEnd = 26.dp)
    val GlassBar = RoundedCornerShape(22.dp)
    val GlassPanel = RoundedCornerShape(28.dp)
    val GlassChip = RoundedCornerShape(14.dp)
    val GlassSheetHeader = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = 18.dp, bottomEnd = 18.dp)
    val GlassSheetFooter = RoundedCornerShape(18.dp)
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
    /** Bottom clearance the snackbar needs so it never sits under the floating fast-add control. */
    val FabClearance = 72.dp
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
    /** Smallest font any auto-sizing label may shrink to before it is allowed to ellipsise. */
    val MinAutoLabelSp = 11f
}

val SnappySpring = spring<Float>(Spring.DampingRatioNoBouncy, Spring.StiffnessMediumLow)
val PopSpring = spring<Float>(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium)
const val TransitionMillis = 180

/** Material surface and interaction roles are explicit: no default purple, light snackbar or implicit tonal overlay. */
val OledColorScheme = darkColorScheme(
    primary = RoutineColors.Primary, onPrimary = RoutineColors.InkOnPrimary,
    primaryContainer = RoutineColors.PrimaryContainer, onPrimaryContainer = RoutineColors.TextPrimary,
    secondary = RoutineColors.Cobalt, onSecondary = RoutineColors.InkOnSecondary,
    secondaryContainer = RoutineColors.School.container, onSecondaryContainer = RoutineColors.School.content,
    tertiary = RoutineColors.FocusAccent, onTertiary = RoutineColors.InkOnTertiary,
    tertiaryContainer = RoutineColors.Focus.container, onTertiaryContainer = RoutineColors.Focus.content,
    error = RoutineColors.Error, onError = RoutineColors.InkOnError,
    errorContainer = RoutineColors.ErrorContainer, onErrorContainer = RoutineColors.TextPrimary,
    background = RoutineColors.Background, onBackground = RoutineColors.TextPrimary,
    surface = RoutineColors.Background, onSurface = RoutineColors.TextPrimary,
    surfaceVariant = RoutineColors.SurfaceContainer, onSurfaceVariant = RoutineColors.TextSecondary,
    surfaceContainerLowest = RoutineColors.SurfaceLowest,
    surfaceContainerLow = RoutineColors.SurfaceLow, surfaceContainer = RoutineColors.SurfaceContainer,
    surfaceContainerHigh = RoutineColors.SurfaceHigh, surfaceContainerHighest = RoutineColors.SurfaceHighest,
    surfaceDim = RoutineColors.Background, surfaceBright = RoutineColors.SurfaceHighest,
    outline = RoutineColors.BorderStrong, outlineVariant = RoutineColors.Border,
    inverseSurface = RoutineColors.SurfaceHigh, inverseOnSurface = RoutineColors.TextPrimary,
    inversePrimary = RoutineColors.Primary,
    surfaceTint = Color.Transparent,
    scrim = Color.Black,
)
