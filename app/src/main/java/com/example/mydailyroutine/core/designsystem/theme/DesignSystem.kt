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
    /** Ambient base of the whole app; also the colour the glass backdrop is filled with. */
    val Background = Color(0xFF0B0E13)
    /** Neutral surface ramp: cards, rows, sheets, popovers — each step is one elevation level. */
    val Surface1 = Color(0xFF141922)
    val Surface2 = Color(0xFF1B212C)
    val Surface3 = Color(0xFF232A37)
    val Surface4 = Color(0xFF2B3342)
    /** Bottom sheets sit above the dim scrim; slightly darker than Surface1 so glass reads on them. */
    val SheetSurface = Color(0xFF101419)
    /**
     * Hairlines. Apple's dark separator is `rgba(84, 84, 88, 0.6)`, which over its secondary
     * background lands near `#3D3D41` — about 1.47:1, and visibly more present than the 1.34:1 a
     * 10 % white hairline measured here. The three steps below are tuned to that level rather than to
     * the 1.25:1 floor of the contrast gate: a border the reader has to look for is not a border.
     */
    val Border = Color.White.copy(alpha = 0.14f)
    val BorderStrong = Color.White.copy(alpha = 0.24f)
    val CardBorder = Color.White.copy(alpha = 0.12f)
    val TextPrimary = Color(0xFFF4F7FC)
    val TextSecondary = Color(0xFFC6CEDC)
    val TextMuted = Color(0xFFAAB3C3)
    val Spine = Color(0xFF2C3542)

    /** Accents at equal tone. Names kept stable so every call site keeps its meaning. */
    val Cobalt = Color(0xFF85B1F9)
    val Amber = Color(0xFFE5A746)
    val Sage = Color(0xFF69CFA4)
    val Crimson = Color(0xFFF9909F)
    val Violet = Color(0xFFB9A1FA)
    val Teal = Color(0xFF78DACC)
    val Indigo = Color(0xFF929FF9)
    val Warning = Color(0xFFF7A969)
    val WarningContainer = Color(0xFF3A2510)

    /** Ambient glow behind the content layer: the liquid glass refracts these two washes. */
    val AmbientTop = Color(0xFF929FF9)
    val AmbientBottom = Color(0xFFE5A746)
    val AmbientTopAlpha = 0.06f
    val AmbientBottomAlpha = 0.04f

    /**
     * Translucent wash painted on top of refracted glass to keep text readable.
     *
     * Dark-mode glass needs more opacity than light-mode glass, and the worst case is not an accent
     * scrolling under the bar but **primary text** at #F4F7FC — the brightest content in the palette.
     * Blended over that, 0.74 still leaves 8.2:1 for primary and 5.5:1 for secondary text; muted text
     * falls to 4.2:1, so muted is used for meta text on solid surfaces only and never on glass (a rule
     * the contrast gate enforces). Every point of opacity traded away is a point of glass gained, and
     * the heavy blur (24-28 dp) is what makes the lower alpha safe: it removes the high-frequency
     * detail that competes with text. Verified by `tools/check_contrast.py`.
     */
    val GlassTint = Color(0xFF0B0E14)
    val GlassTintAlpha = 0.74f
    val GlassTintStrongAlpha = 0.80f
    /** Solid stand-in used when the platform cannot render the effect (below Android 12): the same
     *  tint at the same alpha over the surface it would have been floating on, so nothing shifts. */
    val GlassFallback = Color(0xBD0D1118)
    val GlassFallbackStrong = Color(0xCC0C0F15)
    /** Cool near-white specular highlight for the glass rim. Not pure white: the rim sits on top of
     * everything else, and pure white would make the chrome the brightest thing on screen — exactly
     * the halation the text tokens are tuned against. It is a 1.6 dp stroke at ≤0.38 alpha, so it
     * reads as an edge and never as an area. */
    val GlassRim = Color(0xFFF4F8FF)
    /** Rim ramp stops, multiplied by each material's own rim strength: the highlight falls away from
     * the top-left through the waist to the tail. */
    val GlassRimWaist = 0.28f
    val GlassRimTail = 0.04f
    /**
     * The specular edge, as a multiple of each material's rim strength.
     *
     * A pane lit from above catches far more light along its top than along its sides, and that one
     * bright hairline is the detail that separates glass from tinted film: same blur, same tint, and
     * the panel suddenly has thickness. Apple's own top edge is brighter still (roughly 0.85 alpha at
     * 1.5 px), which would be the loudest thing on an OLED screen full of near-white text, so the peak
     * here lands near 0.57 on chrome and 0.72 on the amber control — lit, not glaring.
     */
    val GlassSpecular = 1.9f
    /** How fast the specular edge dies along the width: 18 % of the peak by 60 %, nothing at the end. */
    val GlassSpecularFall = 0.18f
    /** Peak alpha of the touch-point highlight Apple's interactive glass puts under the finger. It is
     * the same cool white as the rim, so a pressed control reads as the same material lit harder,
     * not as a different surface. */
    val GlassTouchGlow = 0.22f

    val School = CategoryStyle(Cobalt, Color(0xFF16203A), Color(0xFFDCE7FF))
    val Focus = CategoryStyle(Amber, Color(0xFF33260F), Color(0xFFFFE6BC))
    val Recovery = CategoryStyle(Sage, Color(0xFF10301F), Color(0xFFCDF6E6))
    val Exam = CategoryStyle(Crimson, Color(0xFF38161E), Color(0xFFFFDEE3))
    val Project = CategoryStyle(Violet, Color(0xFF241A3B), Color(0xFFE9E0FF))
    val Personal = CategoryStyle(TextSecondary, Surface2, Color(0xFFE0E6EF))
    val subjectSwatches = listOf(0xFF85B1F9L, 0xFF69CFA4L, 0xFFE5A746L, 0xFFF9909FL, 0xFFB9A1FAL, 0xFF78DACCL)
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
