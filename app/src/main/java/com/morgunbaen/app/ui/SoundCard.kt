package com.morgunbaen.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.morgunbaen.app.R

/**
 * Hvad heyrist: vekjarahljodid, frettirnar a eftir baeninni, og hvad
 * tekur vid tegar efnid klarast eda vantar.
 *
 * Adskilid fra Vakningu i v0.976. Tar voru atta stillingar i einu spjaldi
 * og turfti ad skruna framhja flestum teirra til ad finna eina.
 */
@Composable
@OptIn(ExperimentalLayoutApi::class)
internal fun SoundCard(
    newsEnabled: Boolean,
    newsDescription: String,
    fallbackRas1: Boolean,
    alarmSoundTitle: String?,
    soundImporting: Boolean,
    onNewsChange: (Boolean) -> Unit,
    onFallbackRas1Change: (Boolean) -> Unit,
    onPickSystemSound: () -> Unit,
    onPickSoundFile: () -> Unit,
    onResetSound: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp)) {
            Text(
                text = stringResource(R.string.sound_title),
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(Modifier.height(12.dp))

            // Eitt hljod, notad alls stadar tar sem kirkjuklukkan var:
            // tegar baenin vantar eda er buin. Sidasta vornin - ef hljod
            // notandans klikkar - er alltaf innbyggda klukkan.
            Text(
                text = stringResource(R.string.alarm_sound_label),
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = when {
                    soundImporting -> stringResource(R.string.alarm_sound_importing)
                    alarmSoundTitle != null -> alarmSoundTitle
                    else -> stringResource(R.string.alarm_sound_default)
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(onClick = onPickSystemSound, enabled = !soundImporting) {
                    Text(stringResource(R.string.alarm_sound_pick_system))
                }
                TextButton(onClick = onPickSoundFile, enabled = !soundImporting) {
                    Text(stringResource(R.string.alarm_sound_pick_file))
                }
                if (alarmSoundTitle != null) {
                    TextButton(onClick = onResetSound, enabled = !soundImporting) {
                        Text(stringResource(R.string.fallback_bell))
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // Frettirnar eru naesti dagskrarlidur a eftir Morgunbaeninni,
            // svo tetta speglar utsendinguna sjalfa.
            SettingRow(
                label = stringResource(R.string.news_label),
                description = newsDescription,
                checked = newsEnabled,
                onCheckedChange = onNewsChange
            )

            Spacer(Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.fallback_label),
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = stringResource(R.string.fallback_desc),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(6.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FilterChip(
                    selected = !fallbackRas1,
                    onClick = { onFallbackRas1Change(false) },
                    label = { Text(stringResource(R.string.fallback_alarm_sound)) }
                )
                FilterChip(
                    selected = fallbackRas1,
                    onClick = { onFallbackRas1Change(true) },
                    label = { Text(stringResource(R.string.fallback_ras1)) }
                )
            }
        }
    }
}
