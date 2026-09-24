package com.morgunbaen.app.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
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
import com.morgunbaen.app.data.Prefs

/**
 * Hvernig tu vaknar: vakningarhamur, hvad gerist eftir ad tu slekkur,
 * vaxandi hljodstyrkur, titringur og blundur.
 *
 * Hljodid sjalft - vekjarahljod, frettir, varahljod - byr i SoundCard.
 * Tetta spjald var atta stillingar adur en tvi var skipt.
 */
@Composable
@OptIn(ExperimentalLayoutApi::class)
internal fun WakeSettingsCard(
    wakeMode: String,
    afterWake: String,
    fadeIn: Boolean,
    fadeSeconds: Int,
    vibrate: Boolean,
    snoozeMinutes: Int,
    onWakeModeChange: (String) -> Unit,
    onAfterWakeChange: (String) -> Unit,
    onFadeInChange: (Boolean) -> Unit,
    onFadeSecondsChange: (Int) -> Unit,
    onVibrateChange: (Boolean) -> Unit,
    onSnoozeChange: (Int) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp)) {
            Text(
                text = stringResource(R.string.settings_title),
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(Modifier.height(8.dp))

            // Margir vilja heyra baenina VAKANDI. Vekjarahljodid vekur,
            // baenin kemur a eftir - sja AlarmService.awake().
            Text(
                text = stringResource(R.string.wake_mode_label),
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = if (wakeMode == Prefs.WAKE_SOUND) {
                    stringResource(R.string.wake_mode_sound_desc)
                } else {
                    stringResource(R.string.wake_mode_prayer_desc)
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(6.dp))
            ChoiceChips(
                options = listOf(
                    Prefs.WAKE_PRAYER to stringResource(R.string.wake_mode_prayer),
                    Prefs.WAKE_SOUND to stringResource(R.string.wake_mode_sound)
                ),
                selected = wakeMode,
                onSelect = onWakeModeChange
            )

            if (wakeMode == Prefs.WAKE_SOUND) {
                Spacer(Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.after_wake_label),
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = when (afterWake) {
                        Prefs.AFTER_ASK -> stringResource(R.string.after_wake_ask_desc)
                        Prefs.AFTER_LATER -> stringResource(R.string.after_wake_later_desc)
                        else -> stringResource(R.string.after_wake_auto_desc)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(6.dp))
                ChoiceChips(
                    options = listOf(
                        Prefs.AFTER_AUTO to stringResource(R.string.after_wake_auto),
                        Prefs.AFTER_ASK to stringResource(R.string.after_wake_ask),
                        Prefs.AFTER_LATER to stringResource(R.string.after_wake_later)
                    ),
                    selected = afterWake,
                    onSelect = onAfterWakeChange
                )
            }

            Spacer(Modifier.height(12.dp))

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
            MinuteStepper(
                value = snoozeMinutes,
                range = Prefs.SNOOZE_RANGE,
                onChange = onSnoozeChange
            )
        }
    }
}
