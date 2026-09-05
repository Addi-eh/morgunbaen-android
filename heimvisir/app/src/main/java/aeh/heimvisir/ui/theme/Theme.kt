package aeh.heimvisir.ui.theme

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

// Grænt og hlýtt. Appið er oft opnað úti, með dýr í fanginu og
// símann í hinni hendinni — það á að vera rólegt á að líta.
private val LightColors = lightColorScheme(
    primary = Color(0xFF1F4A3A),
    secondary = Color(0xFF3D6A54),
    error = Color(0xFF8B2E1F),
    background = Color(0xFFF7F3EA),
    surface = Color(0xFFFFFDF7),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF8FC7AC),
    secondary = Color(0xFF7FAF96),
    error = Color(0xFFE8A197),
    background = Color(0xFF12160F),
    surface = Color(0xFF1B211A),
)

@Composable
fun HeimvisirTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colors = when {
        // Material You frá Android 12. Notandinn hefur valið sína liti
        // og appið á ekki að þykjast vita betur.
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColors
        else -> LightColors
    }

    MaterialTheme(colorScheme = colors, content = content)
}
