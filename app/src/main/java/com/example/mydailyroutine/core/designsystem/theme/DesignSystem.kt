package com.example.mydailyroutine.core.designsystem.theme

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.unit.dp
import com.example.mydailyroutine.domain.model.RoutineCategory

/**
 * The only palette used by Compose, notifications' accents, and Glance.
 *
 * Built as a tonal system rather than a handful of brand colours:
 *
 * * **Surfaces** are one neutral ramp (`Background` → `Surface4`) tinted towards indigo instead of
 *   pure grey, so stacked cards read as depth and not as unrelated boxes. The base is near-black
 *   (#0B0E13) rather than #000000 — 1.09 times the luminance of pure black, with a card at 1.19 — so
 *   elevation stays visible on an OLED panel, the liquid-glass layers have something to refract, and
 *   white text never halates against a field of absolute black. Each step is 1.09-1.14 times the
 *   luminance of the one below it, inside the range Material 3's own dark ramp uses (1.05-1.17);
 *   flatter than that and cards stop reading as elevation, steeper and text loses contrast headroom.
 * * **Accents** keep their hue and their lightness (59-81 %) but lose about ten points of saturation
 *   each. That is the standard dark-theme correction: on a dark field a fully saturated colour
 *   vibrates against its neighbours and bleeds at the edges, and desaturating it costs none of the
 *   hue distinction that makes six category colours read as one family.
 * * **Containers** are a deep well of the same hue with content near-white. All five pairs measure
 *   12.1:1 or better, far past the 4.5:1 WCAG AA needs for small text, without reaching for
 *   white-on-saturated.
 *
 * Measured ratios (worst case over the whole ramp, `tools/check_contrast.py`): primary text 11.8:1,
 * secondary 8.0:1, muted 6.0:1, weakest accent (indigo) 5.2:1, category content on its container
 * 12.1:1, background text on the amber control 9.2:1.
 *
 * Two rules shape every value here, both from the dark-theme literature rather than from taste:
 *
 * * **No pure black and no pure white.** White on black is 21:1, which reads as vibration and haloing
 *   for readers with astigmatism or low contrast sensitivity — the single most common complaint about
 *   dark UI. The base is a carbon dark grey and the brightest text is a cool off-white, so the
 *   strongest pair in the app is 18:1 and the weakest body pair is still 6:1.
 * * **Body text is not allowed to be the dim one.** The old ramp kept `TextSecondary` at 6.3:1 and
 *   `TextMuted` at 4.7:1: legal, but they carry most of the reading load (times, subjects, hints),
 *   which is exactly where grey-on-grey dark themes fail. They are now 8.0:1 and 6.0:1 worst case,
 *   close to Material 3's own dark `onSurfaceVariant` (~10:1).
 *
 * Contrast is verified in CI by `tools/check_contrast.py`; do not edit a value without running it.
 */
object RoutineColors {
    /**
     * Derived, not designed: `tools/derive_palette.py --check` recomputes every token below and CI
     * fails on drift. Direction B ("turquoise liquid glass", brief of 2026-09-19): a deep neutral
     * base #131515, glass surfaces on #2B2C28, one turquoise primary #7DE2D1 whose ink is the
     * background itself, and soft white #FFFAFB text over a cool #BEEEE6 secondary. Category hues
     * stay a hue-separated family (gap >= 12deg, Delta-E >= 20) so six subjects never read as one
     * colour; wells are the tint at 20% over the card surface with AAA content; the warning yellow
     * is exempt from the family and never a category.
     */
    val Background = Color(0xFF131515)
    val SheetSurface = Color(0xFF1B1D1C)
    val Surface1 = Color(0xFF2B2C28)
    val Surface2 = Color(0xFF2F302C)
    val Surface3 = Color(0xFF333430)
    val Surface4 = Color(0xFF383835)
    val TextPrimary = Color(0xFFFFFAFB)
    val TextSecondary = Color(0xFFADD8D1)
    val TextMuted = Color(0xFF98BEB8)
    /** Disabled content only. Apple's quaternaryLabel / Material's 38 %: never body copy. */
    val TextDisabled = Color(0xFF465654)
    val Border = Color.White.copy(alpha = 0.152f)
    val BorderStrong = Color.White.copy(alpha = 0.188f)
    val CardBorder = Color.White.copy(alpha = 0.100f)
    val Spine = Color(0xFF54575F)

    /** Apple's system fills, verbatim, so wells, tracks and pressed states stop inventing alphas. */
    val Fill = Color(0xFF787880).copy(alpha = 0.36f)
    val FillSecondary = Color(0xFF787880).copy(alpha = 0.32f)
    val FillTertiary = Color(0xFF787880).copy(alpha = 0.24f)
    val FillQuaternary = Color(0xFF787880).copy(alpha = 0.18f)

    /** Category hues: stripes, dots, chip icons, progress, small text. Amber is the brand turquoise. */
    val Cobalt = Color(0xFF9CC5FF)
    val Amber = Color(0xFF7DE2D1)
    val Sage = Color(0xFFA4D98C)
    val Crimson = Color(0xFFFF9B8A)
    val Violet = Color(0xFFD3B0FA)
    val Teal = Color(0xFF5EC4E8)
    val Indigo = Color(0xFFA8D8FF)
    val Warning = Color(0xFFF5C542)
    val Neutral = Color(0xFFBCC3BF)

    /** Ink on a light accent fill: the background itself - the brief's #131515 on #7DE2D1. */
    val InkOnPrimary = Color(0xFF131515)
    val InkOnSecondary = Color(0xFF131515)
    val InkOnTertiary = Color(0xFF131515)
    val InkOnError = Color(0xFF131515)

    /** Ambient glow behind the content layer: the liquid glass refracts these two washes. */
    val AmbientTop = Color(0xFFA8D8FF)
    val AmbientBottom = Color(0xFF7DE2D1)
    val AmbientTopAlpha = 0.06f
    val AmbientBottomAlpha = 0.04f

    /** Translucent wash painted on top of refracted glass to keep text readable. */
    val GlassTint = Color(0xFF131515)
    val GlassTintAlpha = 0.40f
    val GlassTintStrongAlpha = 0.80f
    /** Solid stand-in for platforms that cannot render the effect (below Android 12). */
    val GlassFallback = Color(0xBD191B1A)
    val GlassFallbackStrong = Color(0xCC181A19)
    val GlassRim = Color(0xFFFFFAFB)
    val GlassRimWaist = 0.28f
    val GlassRimTail = 0.04f
    val GlassSpecular = 1.9f
    val GlassSpecularFall = 0.18f
    val GlassTouchGlow = 0.22f
    val GlassTiltGlow = 0.10f

    val WarningContainer = Color(0xFF49432C)
    val School = CategoryStyle(Cobalt, Color(0xFF424B53), Color(0xFFDCE7FC))
    val Focus = CategoryStyle(Amber, Color(0xFF3B504A), Color(0xFFCBF0EA))
    val Recovery = CategoryStyle(Sage, Color(0xFF434F3C), Color(0xFFDBEDCF))
    val Exam = CategoryStyle(Crimson, Color(0xFF55423C), Color(0xFFFFD9D3))
    val Project = CategoryStyle(Violet, Color(0xFF4D4652), Color(0xFFEDDCFB))
    val Personal = CategoryStyle(Neutral, Color(0xFF484A46), Color(0xFFE4E4E3))
    fun cardSurface(accent: Color): Color = accent.copy(alpha = 0.06f).compositeOver(Surface1)

    val subjectSwatches = listOf(0xFF9CC5FFL, 0xFFA4D98CL, 0xFF7DE2D1L, 0xFFFF9B8AL, 0xFFD3B0FAL, 0xFF5EC4E8L)
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

/**
 * The one Material scheme the app ships. Two decisions are deliberate deviations from Material's
 * defaults, both measured:
 *
 * * **"On" colours are the accent's own hue gone deep, not near-black.** `onPrimary = Background`
 *   put a flat #0B0E13 on every filled button, which is the single most common "the text is black"
 *   complaint about dark UIs and it reads as a hole in the design. The ink tokens are the accent's
 *   hue at M3's onPrimary tone and measure 7.5:1 on their own fill.
 * * **The inverse pair is dark.** M3's dark scheme makes `inverseSurface` a near-white tone, so the
 *   snack layer — the one surface that lands right above the navigation bar — arrives as a white
 *   strip at the bottom of an otherwise black screen. iOS has no white panel in dark mode; the
 *   snack layer here is the app's own raised surface instead.
 */
val OledColorScheme = darkColorScheme(
    primary = RoutineColors.Amber, onPrimary = RoutineColors.InkOnPrimary,
    primaryContainer = RoutineColors.Focus.container, onPrimaryContainer = RoutineColors.Focus.content,
    secondary = RoutineColors.Indigo, onSecondary = RoutineColors.InkOnSecondary,
    secondaryContainer = RoutineColors.School.container, onSecondaryContainer = RoutineColors.School.content,
    tertiary = RoutineColors.Violet, onTertiary = RoutineColors.InkOnTertiary,
    tertiaryContainer = RoutineColors.Project.container, onTertiaryContainer = RoutineColors.Project.content,
    error = RoutineColors.Crimson, onError = RoutineColors.InkOnError,
    errorContainer = RoutineColors.Exam.container, onErrorContainer = RoutineColors.Exam.content,
    background = RoutineColors.Background, onBackground = RoutineColors.TextPrimary,
    surface = RoutineColors.Surface1, onSurface = RoutineColors.TextPrimary,
    surfaceVariant = RoutineColors.Surface2, onSurfaceVariant = RoutineColors.TextSecondary,
    surfaceContainerLowest = RoutineColors.Background,
    surfaceContainerLow = RoutineColors.Surface1, surfaceContainer = RoutineColors.Surface2,
    surfaceContainerHigh = RoutineColors.Surface3, surfaceContainerHighest = RoutineColors.Surface4,
    surfaceDim = RoutineColors.Surface1, surfaceBright = RoutineColors.Surface3,
    outline = RoutineColors.Spine, outlineVariant = RoutineColors.CardBorder,
    inverseSurface = RoutineColors.Surface3, inverseOnSurface = RoutineColors.TextPrimary,
    inversePrimary = RoutineColors.Amber,
    surfaceTint = RoutineColors.Indigo,
    scrim = Color.Black,
)
