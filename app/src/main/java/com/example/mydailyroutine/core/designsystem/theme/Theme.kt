package com.example.mydailyroutine.core.designsystem.theme

import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

/** Deliberately fixed OLED dark-first. Wallpaper/dynamic color never replaces category semantics. */
@Composable
fun MyDailyRoutineTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = OledColorScheme, typography = Typography) {
        // Compose's default content colour is black, and the app's root is a plain full-bleed Box:
        // without this provider every label that does not name its own colour renders black on a
        // black screen. Material components provide their own content colour inside themselves, so
        // buttons, chips and cards keep their own ink; this only fixes the text that had none.
        CompositionLocalProvider(LocalContentColor provides RoutineColors.TextPrimary, content = content)
    }
}
