package com.example.mydailyroutine.core.designsystem.theme

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.mydailyroutine.domain.model.RoutineCategory

/**
 * The only palette used by Compose, notifications' accents, and Glance.
 *
 * Built as a tonal system rather than a handful of brand colours:
 *
 * * **Surfaces** are one neutral ramp (`Background` → `Surface4`) tinted towards indigo instead of
 *   pure grey, so stacked cards read as depth and not as unrelated boxes. The base is near-black
 *   (#07080B) rather than #000000: a hair of blue keeps elevation visible on an OLED panel and gives
 *   the liquid-glass layers something to refract.
 * * **Accents** all sit at the same perceived lightness (HCT tone ≈ 80, chroma ≈ 45-60). Equal tone
 *   across hues is what makes six category colours look like one family instead of a box of crayons,
 *   and it keeps every accent legible on the dark ramp.
 * * **Containers** are the matching tone ≈ 22 of the same hue with content at tone ≈ 90, which is the
 *   pair that passes WCAG AA (4.5:1) for small text without reaching for white-on-saturated.
 *
 * Contrast is verified in CI by `tools/check_contrast.py`; do not edit a value without running it.
 */
object RoutineColors {
    /** Ambient base of the whole app; also the colour the glass backdrop is filled with. */
    val Background = Color(0xFF07080B)
    /** Neutral surface ramp: cards, rows, sheets, popovers — each step is one elevation level. */
    val Surface1 = Color(0xFF0C0E13)
    val Surface2 = Color(0xFF12151B)
    val Surface3 = Color(0xFF191D25)
    val Surface4 = Color(0xFF20242E)
    /** Bottom sheets sit above the dim scrim; slightly darker than Surface1 so glass reads on them. */
    val SheetSurface = Color(0xFF0E1116)
    val Border = Color.White.copy(alpha = 0.08f)
    val BorderStrong = Color.White.copy(alpha = 0.14f)
    val CardBorder = Color.White.copy(alpha = 0.06f)
    val TextPrimary = Color(0xFFF2F5FA)
    val TextSecondary = Color(0xFFA3ADBE)
    val TextMuted = Color(0xFF6C7789)
    val Spine = Color(0xFF242933)

    /** Accents at equal tone. Names kept stable so every call site keeps its meaning. */
    val Cobalt = Color(0xFF7FB0FF)
    val Amber = Color(0xFFF0A93B)
    val Sage = Color(0xFF5FD9A6)
    val Crimson = Color(0xFFFF8A9B)
    val Violet = Color(0xFFB79CFF)
    val Teal = Color(0xFF6FE3D2)
    val Indigo = Color(0xFF8C9BFF)
    val Warning = Color(0xFFFFA861)
    val WarningContainer = Color(0xFF33200E)

    /** Ambient glow behind the content layer: the liquid glass refracts these two washes. */
    val AmbientTop = Color(0xFF8C9BFF)
    val AmbientBottom = Color(0xFFF0A93B)
    val AmbientTopAlpha = 0.055f
    val AmbientBottomAlpha = 0.035f

    /** Translucent wash painted on top of refracted glass to keep text at ≥ 4.5:1. */
    val GlassTint = Color(0xFF0B0D12)
    val GlassTintAlpha = 0.58f
    val GlassTintStrongAlpha = 0.72f
    /** Solid stand-in used when the platform cannot render the effect (below Android 12). */
    val GlassFallback = Color(0xF2151922)
    val GlassFallbackStrong = Color(0xF7191D26)

    val School = CategoryStyle(Cobalt, Color(0xFF131C31), Color(0xFFD9E5FF))
    val Focus = CategoryStyle(Amber, Color(0xFF2B2010), Color(0xFFFFE3B8))
    val Recovery = CategoryStyle(Sage, Color(0xFF0E2A20), Color(0xFFC9F5E4))
    val Exam = CategoryStyle(Crimson, Color(0xFF31141B), Color(0xFFFFDCE1))
    val Project = CategoryStyle(Violet, Color(0xFF1F1633), Color(0xFFE6DDFF))
    val Personal = CategoryStyle(TextSecondary, Surface2, Color(0xFFDDE3EC))
    val subjectSwatches = listOf(0xFF7FB0FFL, 0xFF5FD9A6L, 0xFFF0A93BL, 0xFFFF8A9BL, 0xFFB79CFFL, 0xFF6FE3D2L)
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

val OledColorScheme = darkColorScheme(
    primary = RoutineColors.Amber, onPrimary = RoutineColors.Background,
    primaryContainer = RoutineColors.Focus.container, onPrimaryContainer = RoutineColors.Focus.content,
    secondary = RoutineColors.Indigo, onSecondary = RoutineColors.Background,
    secondaryContainer = RoutineColors.School.container, onSecondaryContainer = RoutineColors.School.content,
    tertiary = RoutineColors.Violet, onTertiary = RoutineColors.Background,
    tertiaryContainer = RoutineColors.Project.container, onTertiaryContainer = RoutineColors.Project.content,
    error = RoutineColors.Crimson, onError = RoutineColors.Background,
    errorContainer = RoutineColors.Exam.container, onErrorContainer = RoutineColors.Exam.content,
    background = RoutineColors.Background, onBackground = RoutineColors.TextPrimary,
    surface = RoutineColors.Surface1, onSurface = RoutineColors.TextPrimary,
    surfaceVariant = RoutineColors.Surface2, onSurfaceVariant = RoutineColors.TextSecondary,
    surfaceContainerLowest = RoutineColors.Background,
    surfaceContainerLow = RoutineColors.Surface1, surfaceContainer = RoutineColors.Surface2,
    surfaceContainerHigh = RoutineColors.Surface3, surfaceContainerHighest = RoutineColors.Surface4,
    surfaceDim = RoutineColors.Surface1, surfaceBright = RoutineColors.Surface3,
    outline = RoutineColors.TextMuted, outlineVariant = RoutineColors.Border,
    inverseSurface = RoutineColors.TextPrimary, inverseOnSurface = RoutineColors.Background,
    inversePrimary = Color(0xFF8A5A12),
    surfaceTint = RoutineColors.Indigo,
    scrim = Color.Black,
)
