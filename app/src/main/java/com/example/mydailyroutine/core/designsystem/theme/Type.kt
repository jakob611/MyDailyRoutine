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
private fun TextStyle.tabular() = copy(fontFamily = RoutineFont, fontFeatureSettings = "tnum")
private val defaults = Typography()
val Typography = Typography(
    displayLarge = defaults.displayLarge.tabular(), displayMedium = defaults.displayMedium.tabular(),
    displaySmall = defaults.displaySmall.tabular().copy(fontSize = 36.sp, lineHeight = 42.sp, fontWeight = FontWeight.Black),
    headlineLarge = defaults.headlineLarge.tabular(), headlineMedium = defaults.headlineMedium.tabular(),
    headlineSmall = defaults.headlineSmall.tabular(), titleLarge = defaults.titleLarge.tabular(),
    titleMedium = defaults.titleMedium.tabular().copy(fontSize = 15.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.2).sp),
    titleSmall = defaults.titleSmall.tabular(), bodyLarge = defaults.bodyLarge.tabular(),
    bodyMedium = defaults.bodyMedium.tabular(), bodySmall = defaults.bodySmall.tabular().copy(fontSize = 12.sp),
    labelLarge = defaults.labelLarge.tabular(), labelMedium = defaults.labelMedium.tabular().copy(fontSize = 12.sp, fontWeight = FontWeight.Medium),
    labelSmall = defaults.labelSmall.tabular().copy(fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp),
)
