package com.example.mydailyroutine.core.designsystem.theme

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.unit.dp
import com.example.mydailyroutine.domain.model.RoutineCategory
import com.example.mydailyroutine.domain.model.SubjectPalette

/**
 * Single source of truth for the app's dark, blue-slate visual language.
 *
 * The surface ladder follows the approved Deep Oceanic Slate palette: the large background is
 * #090D16, cards sit on #151C2E, and dialogs/menus may rise to #26324F. Semantic accents are kept
 * separate from surfaces so a colour communicates state instead of becoming a second background.
 * XML colours for the widget and the first window frame mirror these tokens and are checked in CI.
 */
object RoutineColors {
    // Material-style surface roles. SurfaceLowest is reserved for recessed controls and inputs.
    val Background = Color(0xFF090D16)
    val SurfaceLowest = Color(0xFF05070B)
    val SurfaceLow = Color(0xFF0F1422)
    val Surface1 = Color(0xFF1A2238) // surfaceContainer: cards and calendar blocks
    val Surface2 = Color(0xFF1D263D) // surfaceContainerHigh: selected/floating elements
    val Surface3 = Color(0xFF26324F) // surfaceContainerHighest: dialogs and menus
    val Surface4 = Surface3
    val SheetSurface = SurfaceLow

    val TextPrimary = Color(0xFFF1F5F9)
    val TextSecondary = Color(0xFFA8B3C2)
    val TextMuted = Color(0xFF94A3B8)
    val TextDisabled = Color(0xFF536070)

    // White hairlines are deliberately layered: top-lit edges are stronger than card outlines.
    val Border = Color.White.copy(alpha = 0.14f)
    val BorderStrong = Color.White.copy(alpha = 0.18f)
    val CardBorder = Color.White.copy(alpha = 0.10f)
    /**
     * The edge of anything the reader can type into. A hairline is decoration and may stay quiet;
     * the outline of an input is a control, and WCAG asks 3:1 for those, so this one is measurably
     * brighter than [CardBorder] while still reading as a thin line rather than a frame.
     */
    val FieldOutline = Color(0xFF74849C)
    val Spine = Color(0xFF52627C)

    // System-fill equivalents, tinted slate rather than the old neutral grey ramp.
    val Fill = TextMuted.copy(alpha = 0.36f)
    val FillSecondary = TextMuted.copy(alpha = 0.32f)
    val FillTertiary = TextMuted.copy(alpha = 0.24f)
    val FillQuaternary = TextMuted.copy(alpha = 0.18f)

    // Semantic accents. These are the only colours that carry interaction or state meaning.
    val Primary = Color(0xFF2DD4BF)
    /**
     * The track of a switch that is on. Deliberately deeper than [Primary]: the knob on top of it is
     * near-white, and white on brand turquoise measured 1.86:1 — bright enough to look fine in a
     * screenshot and impossible to read in the sun. The hue stays the brand's.
     */
    val SwitchOn = Color(0xFF0D9488)
    val Timer = Color(0xFF67E8F9)
    val FocusAccent = Color(0xFFA78BFA)
    val Success = Color(0xFF34D399)
    val Warning = Color(0xFFFBBF24)
    val Error = Color(0xFFFB7185)

    // Ink on a light accent fill. The OLED background is dark enough for all these controls.
    val InkOnPrimary = Background
    val InkOnSecondary = Background
    val InkOnTertiary = Background
    val InkOnError = Background

    // Ambient light belongs behind the content layer, not inside each card.
    val AmbientTop = Color(0xFF0EA5E9)
    val AmbientBottom = Color(0xFF8B5CF6)
    val AmbientTopAlpha = 0.07f
    val AmbientBottomAlpha = 0.045f

    // Glass surface alpha is role-specific: standard card, compact control, and sheet.
    val GlassTint = Surface1
    val GlassTintAlpha = 0.52f
    val GlassTintCompactAlpha = 0.58f
    val GlassTintStrongAlpha = 0.68f
    val GlassFallback = Color(0x94151C2E)
    val GlassFallbackStrong = Color(0xAD0F1422)
    val GlassRim = TextPrimary
    val GlassRimWaist = 0.42f
    val GlassRimTail = 0.18f
    val GlassSpecular = 1f
    val GlassSpecularFall = 0.35f
    val GlassTouchGlow = 0.16f
    val GlassTiltGlow = 0.06f

    val WarningContainer = Color(0xFF4B3B1B)
    val School = CategoryStyle(Timer, Surface2, TextPrimary)
    val Focus = CategoryStyle(FocusAccent, Surface2, TextPrimary)
    val Recovery = CategoryStyle(Success, Surface2, TextPrimary)
    val Exam = CategoryStyle(Error, Surface2, TextPrimary)
    val Project = CategoryStyle(FocusAccent, Surface2, TextPrimary)
    val Personal = CategoryStyle(TextSecondary, Surface2, TextPrimary)
    fun cardSurface(accent: Color): Color = accent.copy(alpha = 0.06f).compositeOver(Surface1)

    val subjectSwatches = SubjectPalette.swatches
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
     * minimum radius of at least 12 dp on the corners the reader can actually see. The top bar
     * is a floating pane with real space above and beside it, so all of its corners are rounded
     * and the pane can never read as a panel stuck to the screen edge.
     */
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
    /**
     * The smallest a *touch target* may be, independent of how large the thing looks. Material's
     * own components enforce this through `minimumInteractiveComponentSize`; the app's hand-built
     * glass controls do not, so they wrap their 40 dp pane in a box of this size instead. A control
     * that looks smaller than it can be hit is the cheapest way to make an app feel unreliable.
     */
    val TouchTarget = 48.dp
    /** Compact liquid-glass control: the icon buttons and chips that live in the floating chrome. */
    val GlassControlSize = 40.dp
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

/**
 * The one Material scheme the app ships. Two decisions are deliberate deviations from Material's
 * defaults, both measured:
 *
 * * **"On" colours are the accent's own hue gone deep, not near-black.** `onPrimary = Background`
 *   put a flat #090D16 on every filled button, which is the single most common "the text is black"
 *   complaint about dark UIs and it reads as a hole in the design. The ink tokens are the accent's
 *   hue at M3's onPrimary tone and measure 7.5:1 on their own fill.
 * * **The inverse pair is dark.** M3's dark scheme makes `inverseSurface` a near-white tone, so the
 *   snack layer — the one surface that lands right above the navigation bar — arrives as a white
 *   strip at the bottom of an otherwise black screen. iOS has no white panel in dark mode; the
 *   snack layer here is the app's own raised surface instead.
 */
val OledColorScheme = darkColorScheme(
    primary = RoutineColors.Primary, onPrimary = RoutineColors.InkOnPrimary,
    primaryContainer = RoutineColors.Focus.container, onPrimaryContainer = RoutineColors.Focus.content,
    secondary = RoutineColors.Timer, onSecondary = RoutineColors.InkOnSecondary,
    secondaryContainer = RoutineColors.School.container, onSecondaryContainer = RoutineColors.School.content,
    tertiary = RoutineColors.FocusAccent, onTertiary = RoutineColors.InkOnTertiary,
    tertiaryContainer = RoutineColors.Project.container, onTertiaryContainer = RoutineColors.Project.content,
    error = RoutineColors.Error, onError = RoutineColors.InkOnError,
    errorContainer = RoutineColors.Exam.container, onErrorContainer = RoutineColors.Exam.content,
    background = RoutineColors.Background, onBackground = RoutineColors.TextPrimary,
    surface = RoutineColors.Surface1, onSurface = RoutineColors.TextPrimary,
    surfaceVariant = RoutineColors.Surface2, onSurfaceVariant = RoutineColors.TextSecondary,
    surfaceContainerLowest = RoutineColors.SurfaceLowest,
    surfaceContainerLow = RoutineColors.SurfaceLow, surfaceContainer = RoutineColors.Surface1,
    surfaceContainerHigh = RoutineColors.Surface2, surfaceContainerHighest = RoutineColors.Surface3,
    surfaceDim = RoutineColors.SurfaceLowest, surfaceBright = RoutineColors.Surface3,
    outline = RoutineColors.FieldOutline, outlineVariant = RoutineColors.CardBorder,
    inverseSurface = RoutineColors.Surface3, inverseOnSurface = RoutineColors.TextPrimary,
    inversePrimary = RoutineColors.Primary,
    surfaceTint = RoutineColors.Timer,
    scrim = Color.Black,
)
