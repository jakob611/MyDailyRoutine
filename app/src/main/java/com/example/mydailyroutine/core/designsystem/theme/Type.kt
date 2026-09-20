package com.example.mydailyroutine.core.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.mydailyroutine.R

// Bundled under the SIL Open Font License. No downloadable-font provider or network is involved.
//
// Apple ships SF Pro; its licence does not allow it to be redistributed inside an Android app, so the
// system face stays Roboto. What *can* be taken over is the discipline around it: one family, one
// scale, weight used for hierarchy instead of size, and optical sizing per role. Everything below is
// Apple's Dynamic Type at its default (Large) setting, converted point for point onto Material's role
// names, so every existing call site picks up the scale without being touched.
val RoutineFont = FontFamily(Font(R.font.roboto_flex))

/**
 * One step of the scale, with all four of its numbers — size, leading, weight, tracking — set
 * together, so a style can never end up half-Apple and half-Material.
 *
 * * **Size and leading** are Apple's: Body 17/22, Callout 16/21, Subhead 15/20, Footnote 13/18,
 *   Caption 1 12/16, Headline 17 semibold, Title 3 20/25, Title 2 22/28, Title 1 28/34, Large Title
 *   34/41. The ratios run 1.21-1.38 where Material's defaults run 1.43-1.50, which is why M3 text
 *   reads loose next to iOS: same words, more air, less structure.
 * * **Weight** follows Apple's rule that body copy is Regular and only emphasis is Semibold. Material
 *   sets its small styles — bodySmall and every label — in Medium, which at 12-13 sp on an OLED panel
 *   closes the counters and smears a dense row into one dark band. Regular at the same size with a
 *   proper leading is easier to read, and Semibold then actually means something.
 * * **Tracking** follows SF Pro's variable letter spacing: negative and growing with size above 15 sp,
 *   flat around 13 sp, slightly positive on the caption sizes. Material's defaults point the other way
 *   (positive tracking on body text), which is the other half of the loose look. The values are given
 *   in sp, which is what Apple's own tables are measured in and which scales with the reader's font
 *   scale just as a proportional value would.
 *
 * `tnum` keeps digits monospaced, which a time-blocking app needs: hours in the gutter, durations and
 * countdowns stay aligned while the text around them reflows. It only affects digits, so it is safe on
 * every style.
 */
private fun apple(size: Int, leading: Int, weight: FontWeight, trackingSp: Float): TextStyle = TextStyle(
    fontFamily = RoutineFont,
    fontSize = size.sp,
    lineHeight = leading.sp,
    fontWeight = weight,
    letterSpacing = trackingSp.sp,
    fontFeatureSettings = "tnum",
)

// Roboto Flex carries an `opsz` axis from 8 to 144 — the same idea as SF Pro's dynamic optical sizes,
// where small text is drawn sturdier and large text thinner and tighter. Compose cannot reach it:
// `TextStyle` and `SpanStyle` expose no font-variation parameter at all (checked against the published
// ui-text API surface, where the axis appears nowhere), so in Compose the axis stays at its default of
// 14 and only weight comes through, via `fontWeight`. The widget is the exception: it is drawn through
// `TextView`, which has had `android:fontVariationSettings` since API 26, so its four layouts do set
// `'opsz'` to the size they render at.

/**
 * The app's type scale. Role names stay Material's, because 300-odd call sites name them; the metrics
 * behind them are Apple's.
 *
 * Two deliberate departures from a literal port, both about density rather than taste:
 *
 * * **`titleSmall` is Callout at Semibold (16/21) and `bodyMedium` is Subhead (15/20), not Body 17.**
 *   A planner is read at a glance and in columns; Apple's own calendar and reminders set their
 *   secondary lines at Subhead for the same reason. Body 17 stays available as `bodyLarge` for the
 *   paragraphs that are actually meant to be read.
 * * **`labelSmall` stays at 12 sp instead of Apple's Caption 2 at 11.** Eleven sp was tried once
 *   already, in the weekly grid and the hour gutter, and it is below what an OLED panel holds at
 *   reading distance. Twelve sp with Caption 1's leading and a hair of positive tracking carries the
 *   same information legibly.
 */
val Typography = Typography(
    // Large Title and its two accessibility sizes: the countdown numbers and the biggest headings.
    displayLarge = apple(40, 48, FontWeight.Bold, -1.2f),
    displayMedium = apple(36, 43, FontWeight.Bold, -1.0f),
    displaySmall = apple(34, 41, FontWeight.Bold, -0.9f),
    // Title 1 and its xLarge step: sheet titles, screen headings.
    headlineLarge = apple(30, 37, FontWeight.SemiBold, -0.7f),
    headlineMedium = apple(28, 34, FontWeight.SemiBold, -0.6f),
    // Title 2: section headings, the folded period title in the glass bar.
    headlineSmall = apple(22, 28, FontWeight.SemiBold, -0.45f),
    // Title 3: card and panel titles.
    titleLarge = apple(20, 25, FontWeight.SemiBold, -0.4f),
    // Headline: the emphasised line inside a card — a block title, a row's leading text.
    titleMedium = apple(17, 22, FontWeight.SemiBold, -0.3f),
    // Callout at Semibold: list rows, tab labels, anything leading a secondary line.
    titleSmall = apple(16, 21, FontWeight.SemiBold, -0.25f),
    // Body: paragraphs, hints, the long-form text that is actually meant to be read.
    bodyLarge = apple(17, 22, FontWeight.Normal, -0.2f),
    // Subhead: the default secondary line — form labels, descriptions, card body copy.
    bodyMedium = apple(15, 20, FontWeight.Normal, -0.1f),
    // Footnote: metadata, timestamps, gutters, the small print under a value.
    bodySmall = apple(13, 18, FontWeight.Normal, 0f),
    // Button and chip labels. Apple sets control text in Body Semibold; 15 sp Semibold is the same
    // voice at the density this app's controls are built to, and RoutineLabel shrinks it rather than
    // truncating when a Slovenian label does not fit.
    labelLarge = apple(15, 20, FontWeight.SemiBold, -0.1f),
    // Caption 1: counters, badges, the smallest neutral text.
    labelMedium = apple(12, 16, FontWeight.Medium, 0.07f),
    // Caption 1 at Semibold: eyebrows, day names, unit suffixes — small and structural, so it earns
    // the weight instead of the size.
    labelSmall = apple(12, 16, FontWeight.SemiBold, 0.12f),
)
