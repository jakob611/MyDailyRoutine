package com.example.mydailyroutine.core.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.mydailyroutine.R

// Bundled under the SIL Open Font License. No downloadable-font provider or network is involved.
val RoutineFont = FontFamily(Font(R.font.roboto_flex))

/**
 * `tnum` keeps digits monospaced, which is what a time-blocking app needs: hours in the gutter,
 * durations and countdowns stay aligned while the text around them reflows. It only affects digits,
 * so it is safe on every style.
 */
private fun TextStyle.tabular() = copy(fontFamily = RoutineFont, fontFeatureSettings = "tnum")

private val defaults = Typography()

/**
 * Smallest text in the app is 12 sp (Material 3 minimum). Anything below that was unreadable on an
 * OLED panel and forced the week grid into 10 sp labels that no longer fit their cells.
 */
val Typography = Typography(
    displayLarge = defaults.displayLarge.tabular(),
    displayMedium = defaults.displayMedium.tabular(),
    displaySmall = defaults.displaySmall.tabular().copy(fontSize = 34.sp, lineHeight = 40.sp, fontWeight = FontWeight.Bold),
    headlineLarge = defaults.headlineLarge.tabular(),
    headlineMedium = defaults.headlineMedium.tabular(),
    headlineSmall = defaults.headlineSmall.tabular(),
    titleLarge = defaults.titleLarge.tabular(),
    titleMedium = defaults.titleMedium.tabular().copy(fontSize = 15.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.2).sp),
    titleSmall = defaults.titleSmall.tabular(),
    bodyLarge = defaults.bodyLarge.tabular(),
    bodyMedium = defaults.bodyMedium.tabular(),
    bodySmall = defaults.bodySmall.tabular().copy(fontSize = 12.sp, lineHeight = 17.sp),
    labelLarge = defaults.labelLarge.tabular(),
    labelMedium = defaults.labelMedium.tabular().copy(fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium),
    labelSmall = defaults.labelSmall.tabular().copy(fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.3.sp),
)
