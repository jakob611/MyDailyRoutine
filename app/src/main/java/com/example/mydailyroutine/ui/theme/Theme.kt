package com.example.mydailyroutine.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val LightColors = lightColorScheme(
    primary = Color(0xFF52684C), onPrimary = Color.White,
    primaryContainer = Color(0xFFD5E8CC), onPrimaryContainer = Color(0xFF121F10),
    secondary = Color(0xFF56624F), secondaryContainer = Color(0xFFD9E7CF),
    tertiary = Color(0xFF75608A), tertiaryContainer = Color(0xFFEFDCFF),
    background = Color(0xFFFAFBF5), surface = Color(0xFFFAFBF5),
)
private val DarkColors = darkColorScheme(
    primary = Color(0xFFBAD0AF), onPrimary = Color(0xFF263421),
    primaryContainer = Color(0xFF3B5034), onPrimaryContainer = Color(0xFFD5E8CC),
    secondary = Color(0xFFBDCCB3), secondaryContainer = Color(0xFF3D4A36),
    tertiary = Color(0xFFD6BDEB), tertiaryContainer = Color(0xFF58446C),
    background = Color(0xFF11150F), surface = Color(0xFF11150F),
)

@Composable
fun MyDailyRoutineTheme(darkTheme: Boolean = isSystemInDarkTheme(), dynamicColor: Boolean = true, content: @Composable () -> Unit) {
    val context = LocalContext.current
    val colors = if (dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else if (darkTheme) DarkColors else LightColors
    MaterialTheme(colorScheme = colors, typography = Typography, content = content)
}
