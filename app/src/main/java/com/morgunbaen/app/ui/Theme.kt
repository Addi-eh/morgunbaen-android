package com.morgunbaen.app.ui

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

// Rolegir, daufir tonar - appid er notad snemma morguns
// tegar augun tola ekki sterka liti.
private val LightColors = lightColorScheme(
    primary = Color(0xFF4A5D7E),
    secondary = Color(0xFF6B7B95),
    background = Color(0xFFFAF9F7),
    surface = Color(0xFFFFFFFF)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF9BB0D3),
    secondary = Color(0xFF8494AE),
    background = Color(0xFF12141A),
    surface = Color(0xFF1B1E26)
)

@Composable
fun MorgunbaenTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context)
            else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColors
        else -> LightColors
    }

    MaterialTheme(colorScheme = colors, content = content)
}
