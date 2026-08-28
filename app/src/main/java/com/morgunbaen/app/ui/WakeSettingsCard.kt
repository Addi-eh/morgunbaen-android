package com.morgunbaen.app.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.morgunbaen.app.R

/**
 * Vakningarstillingar: fade-in, titringur, frettir og blundur.
 *
 * Frettalysingin (newsDescription) er reiknud i MainScreen - hun tarf
 * baedi vekjaratimann og klukkuna, og su rokfraedi a heima a einum stad.
 */
@Composable
internal fun WakeSettingsCard(
    fadeIn: Boolean,
    fadeSeconds: Int,
    vibrate: Boolean,
    newsEnabled: Boolean,
    newsDescription: String,
    snoozeMinutes: Int,
    onFadeInChange: (Boolean) -> Unit,
    onFadeSecondsChange: (Int) -> Unit,
    onVibrateChange: (Boolean) -> Unit,
    onNewsChange: (Boolean) -> Unit,
    onSnoozeChange: (Int) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp)) {
            Text(
                text = stringResource(R.string.settings_title),
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(Modifier.height(4.dp))

            SettingRow(
                label = stringResource(R.string.fade_in_label),
                description = if (fadeIn) {
                    stringResource(R.string.fade_in_desc, fadeSeconds)
                } else {
                    stringResource(R.string.fade_in_off_desc)
                },
                checked = fadeIn,
                onCheckedChange = onFadeInChange
            )

            // Lengdin skiptir adeins mali tegar fade-in er virkt.
            if (fadeIn) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.fade_length),
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(Modifier.height(4.dp))
                FadeLengthPicker(selected = fadeSeconds, onChange = onFadeSecondsChange)
            }

            Spacer(Modifier.height(12.dp))

            // Slokkta stadan fyrst. Adur greindist lysingin EINGONGU a
            // fadeIn og aldrei a rofanum sjalfum, svo "Titrar medan
            // vekjarinn hringir" stod undir slokktum titringi.
            SettingRow(
                label = stringResource(R.string.vibrate_label),
                description = when {
                    !vibrate -> stringResource(R.string.vibrate_off_desc)
                    fadeIn -> stringResource(R.string.vibrate_desc_fade)
                    else -> stringResource(R.string.vibrate_desc)
                },
                checked = vibrate,
                onCheckedChange = onVibrateChange
            )

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
                text = stringResource(R.string.snooze_label),
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = stringResource(R.string.snooze_desc),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(6.dp))
            MinutePicker(
                options = listOf(5, 9, 10, 15, 20),
                selected = snoozeMinutes,
                onChange = onSnoozeChange
            )
        }
    }
}
