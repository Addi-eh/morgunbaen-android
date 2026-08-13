package com.morgunbaen.app.alarm

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.morgunbaen.app.R
import com.morgunbaen.app.data.Prefs
import com.morgunbaen.app.ui.MorgunbaenTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Skjarinn sem notandinn vaknar vid.
 *
 * Vidmotid er viljandi mjog einfalt - manneskja sem er nyvoknud
 * a ekki ad turfa ad leita ad neinu.
 */
class AlarmActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Bakktakkinn ma ekki loka vekjaranum - annars slekkur folk
        // a honum i svefnrofunum an tess ad atta sig a tvi.
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // Visvitandi tomt
            }
        })

        val prefs = Prefs(this)
        val title = prefs.cachedTitle
        val firstrun = prefs.cachedFirstrun

        setContent {
            MorgunbaenTheme {
                AlarmScreen(
                    episodeTitle = title,
                    firstrun = firstrun,
                    onDismiss = {
                        AlarmService.dismiss(this)
                        finish()
                    },
                    onSnooze = {
                        AlarmService.snooze(this)
                        finish()
                    }
                )
            }
        }
    }
}

@Composable
private fun AlarmScreen(
    episodeTitle: String?,
    firstrun: String?,
    onDismiss: () -> Unit,
    onSnooze: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = currentTimeString(),
                style = MaterialTheme.typography.displayLarge
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = stringResource(R.string.alarm_screen_heading),
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(Modifier.height(40.dp))

            if (episodeTitle != null) {
                Text(
                    text = episodeTitle,
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center
                )
            }

            if (firstrun != null) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = formatFirstrun(firstrun),
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(Modifier.height(64.dp))

            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
            ) {
                Text(
                    text = stringResource(R.string.dismiss),
                    style = MaterialTheme.typography.titleLarge
                )
            }

            Spacer(Modifier.height(16.dp))

            OutlinedButton(
                onClick = onSnooze,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text(text = stringResource(R.string.snooze))
            }
        }
    }
}

private fun formatFirstrun(firstrun: String): String {
    // "2026-08-12T06:55:00" -> "12. ágúst"
    val datePart = firstrun.substringBefore('T').substringBefore(' ')
    return try {
        val parsed = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(datePart)!!
        SimpleDateFormat("d. MMMM", Locale("is", "IS")).format(parsed)
    } catch (e: Exception) {
        datePart
    }
}

private fun currentTimeString(): String =
    SimpleDateFormat("HH:mm", Locale("is", "IS")).format(Date())
