package com.morgunbaen.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.morgunbaen.app.R
import java.util.Locale

/**
 * Efsta spjaldid: vekjaratimi, dagar, helgartimi og profunarhnappur.
 *
 * Allt state byr i MainScreen - spjaldid faer gildi og skilar atburdum.
 * TimePickerDialog er lika hja MainScreen, tvi hann tarf Activity-context;
 * her eru bara onPickTime/onPickWeekendTime.
 */
@Composable
internal fun AlarmCard(
    hour: Int,
    minute: Int,
    enabled: Boolean,
    days: Set<Int>,
    weekendEnabled: Boolean,
    weekendHour: Int,
    weekendMinute: Int,
    weekendDaysMissing: Boolean,
    nextAlarmText: String,
    countdownText: String?,
    skipActive: Boolean,
    skippedWhenText: String?,
    testArmed: Boolean,
    testSeconds: Int,
    onEnabledChange: (Boolean) -> Unit,
    onPickTime: () -> Unit,
    onDaysChange: (Set<Int>) -> Unit,
    onWeekendEnabledChange: (Boolean) -> Unit,
    onPickWeekendTime: () -> Unit,
    onSkipNext: () -> Unit,
    onUndoSkip: () -> Unit,
    onTest: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Ad yta a klukkuna sjalfa er fyrsta hreyfing margra. Hun
                // opnar sama tolvalsglugga og "Breyta tima"-hnappurinn, sem
                // stendur afram - flytileid fyrir ta sem giska a hana, ekki
                // stadgengill fyrir synilegu leidina.
                BigClock(hour = hour, minute = minute, onClick = onPickTime)
                Switch(checked = enabled, onCheckedChange = onEnabledChange)
            }

            Spacer(Modifier.height(4.dp))

            // "Breyta tima" beint undir klukkunni, teljarinn haegra megin
            // a somu linu. Tha stendur ekkert a milli klukkunnar og
            // hnappsins sem breytir henni.
            //
            // Teljarinn svarar teirri spurningu sem klukkan sjalf svarar
            // ekki: hve lengi ma eg enn sofa? Adeins tegar vekjarinn er a -
            // slokktur vekjari hefur engan bidtima.
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onPickTime) {
                    Text(stringResource(R.string.change_time))
                }
                if (enabled && countdownText != null) {
                    CountdownPill(text = countdownText)
                }
            }

            Spacer(Modifier.height(8.dp))
            DayPicker(selected = days, onChange = onDaysChange)

            if (enabled) {
                Spacer(Modifier.height(12.dp))
                Text(
                    text = nextAlarmText,
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(Modifier.height(8.dp))
                if (skipActive && skippedWhenText != null) {
                    Text(
                        text = stringResource(R.string.skip_active, skippedWhenText),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    OutlinedButton(
                        onClick = onUndoSkip,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.skip_undo))
                    }
                } else {
                    OutlinedButton(
                        onClick = onSkipNext,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.skip_next))
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Morgunbaenin er DAGLEG - lika um helgar - svo tetta er hrein
            // timastilling: sofa lengur an tess ad missa af baen tess dags.
            SettingRow(
                label = stringResource(R.string.weekend_label),
                description = if (weekendEnabled) {
                    stringResource(R.string.weekend_desc_on)
                } else {
                    stringResource(R.string.weekend_desc)
                },
                checked = weekendEnabled,
                onCheckedChange = onWeekendEnabledChange
            )

            if (weekendEnabled) {
                Spacer(Modifier.height(8.dp))
                BigClock(
                    hour = weekendHour,
                    minute = weekendMinute,
                    onClick = onPickWeekendTime
                )
                TextButton(onClick = onPickWeekendTime) {
                    Text(stringResource(R.string.change_time))
                }

                // Helgartimi an helgardaga gerir bokstaflega ekkert -
                // segjum tad i stad tess ad lata rofann ljuga tognandi.
                if (weekendDaysMissing) {
                    Text(
                        text = stringResource(R.string.weekend_days_warning),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // Keyrir OLLU leidina - ekki bara spilun. Eina leidin til
            // ad stadfesta Samsung-stillingar an tess ad bida til morguns.
            TextButton(onClick = onTest) {
                Text(stringResource(R.string.test_alarm))
            }

            if (testArmed) {
                Text(
                    text = stringResource(R.string.test_alarm_armed, testSeconds),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

/**
 * Bidtiminn i litlum belg: klukkutakn og "2 klst 7 min".
 *
 * Textinn er nu tegar samsettur - belgurinn veit ekkert um klukkur.
 * Taknid faer lysinguna svo skjalesarar segi hvad talan tydir.
 */
@Composable
private fun BigClock(hour: Int, minute: Int, onClick: () -> Unit) {
    Text(
        text = String.format(Locale.getDefault(), "%02d:%02d", hour, minute),
        style = MaterialTheme.typography.displayMedium,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(
                onClickLabel = stringResource(R.string.change_time),
                onClick = onClick
            )
    )
}

@Composable
private fun CountdownPill(text: String) {
    Surface(
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.Alarm,
                contentDescription = stringResource(R.string.cd_countdown),
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}
