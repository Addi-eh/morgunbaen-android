package com.morgunbaen.app.ui

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.morgunbaen.app.data.Prefs
import kotlinx.coroutines.flow.MutableStateFlow

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

/**
 * Valid utlit, deilt milli allra fjogurra skjanna i somu ferli.
 * Sama mynstur og AlarmService.listeningState: MutableStateFlow er
 * einfaldasta leidin til ad lata tvo Activity vita hvort af odru.
 *
 * Sett i MorgunbaenApp.onCreate, adur en nokkur skjar getur opnast.
 */
object AppTheme {
    val mode = MutableStateFlow(Prefs.THEME_SYSTEM)
}

@Composable
fun MorgunbaenTheme(content: @Composable () -> Unit) {
    val mode by AppTheme.mode.collectAsState()
    val darkTheme = when (mode) {
        Prefs.THEME_LIGHT -> false
        Prefs.THEME_DARK -> true
        else -> isSystemInDarkTheme()
    }

    val colors = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context)
            else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColors
        else -> LightColors
    }

    // Taknin i stodu- og flettistiku fara ekki eftir Compose-litunum.
    // Tetta verdur ad gerast her en ekki i enableEdgeToEdge: valid utlit
    // getur gengid gegn stillingu simans, og SystemBarStyle.auto les
    // eingongu stillingu simans.
    val view = LocalView.current
    if (!LocalInspectionMode.current) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(colorScheme = colors, content = content)
}
