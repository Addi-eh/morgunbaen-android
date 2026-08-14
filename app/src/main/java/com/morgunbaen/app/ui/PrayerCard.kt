package com.morgunbaen.app.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.morgunbaen.app.R

/**
 * Bæn dagsins: stada, sokn, spilun, saga og deiling.
 * State og öll hlidarverk (spilari, netkall, deiling) bua i MainScreen.
 */
@Composable
internal fun PrayerCard(
    title: String?,
    dateText: String?,
    status: String?,
    syncing: Boolean,
    playingToday: Boolean,
    canShare: Boolean,
    onFetch: () -> Unit,
    onPlayToggle: () -> Unit,
    onOpenHistory: () -> Unit,
    onShare: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp)) {
            Text(
                text = stringResource(R.string.prayer_status),
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(Modifier.height(8.dp))

            if (title != null) {
                Text(title, style = MaterialTheme.typography.bodyLarge)
                dateText?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall)
                }
            } else {
                Text(
                    text = stringResource(R.string.no_prayer_yet),
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Spacer(Modifier.height(12.dp))

            Button(onClick = onFetch, enabled = !syncing) {
                if (syncing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(Modifier.width(8.dp))
                }
                Text(stringResource(R.string.fetch_now))
            }

            status?.let {
                Spacer(Modifier.height(8.dp))
                Text(it, style = MaterialTheme.typography.bodySmall)
            }

            Spacer(Modifier.height(4.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Baenin er thegar a taekinu - takkinn er naestum okeypis.
                // Raunverulega astaedan er samt onnur: hann gefur folki
                // tilefni til ad OPNA appid, og Samsung svaefir einmitt
                // opp sem enginn opnar.
                TextButton(onClick = onPlayToggle, enabled = title != null) {
                    Text(
                        stringResource(
                            if (playingToday) R.string.stop_playback
                            else R.string.play_today
                        )
                    )
                }

                TextButton(onClick = onOpenHistory) {
                    Text(stringResource(R.string.history_open))
                }

                if (canShare) {
                    TextButton(onClick = onShare) {
                        Text(stringResource(R.string.share))
                    }
                }
            }
        }
    }
}
