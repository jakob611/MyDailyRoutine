package com.example.mydailyroutine.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable

/** Deliberately fixed OLED dark-first. Wallpaper/dynamic color never replaces category semantics. */
@Composable
fun MyDailyRoutineTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = OledColorScheme, typography = Typography, content = content)
}
