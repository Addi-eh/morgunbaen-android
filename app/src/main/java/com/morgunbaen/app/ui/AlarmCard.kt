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
import java.util.Calendar
import java.util.Locale

/**
 * Efsta spjaldid: vekjaratimi, dagar, timi hvers dags og profunarhnappur.
 *
 * Allt state byr i MainScreen - spjaldid faer gildi og skilar atburdum.
 * TimePickerDialog er lika hja MainScreen, tvi hann tarf Activity-context;
 * her eru bara onPickTime/onPickDayTime.
 */
@Composable
internal fun AlarmCard(
    hour: Int,
    minute: Int,
    enabled: Boolean,
    days: Set<Int>,
    perDayEnabled: Boolean,
    dayTimes: Map<Int, Int>,
    nextAlarmText: String,
    countdownText: String?,
    skipActive: Boolean,
    skippedWhenText: String?,
    testArmed: Boolean,
    testSeconds: Int,
    onEnabledChange: (Boolean) -> Unit,
    onPickTime: () -> Unit,
    onDaysChange: (Set<Int>) -> Unit,
    onPerDayEnabledChange: (Boolean) -> Unit,
    onPickDayTime: (Int) -> Unit,
    onResetDayTime: (Int) -> Unit,
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

            Spacer(Modifier.height(16.dp))

            // Morgunbaenin er DAGLEG - lika um helgar - svo tetta er hrein
            // timastilling: sofa lengur, eda fara fyrr a faetur, an tess
            // ad missa af baen tess dags. Leysti helgartimann af holmi.
            SettingRow(
                label = stringResource(R.string.per_day_label),
                description = if (perDayEnabled) {
                    stringResource(R.string.per_day_desc_on)
                } else {
                    stringResource(R.string.per_day_desc)
                },
                checked = perDayEnabled,
                onCheckedChange = onPerDayEnabledChange
            )

            // Adeins valdir dagar: timi a degi sem hringir aldrei er
            // stilling sem gerir ekkert og villir um fyrir notandanum.
            if (perDayEnabled) {
                Spacer(Modifier.height(4.dp))
                WEEK_ORDER.filter { it.first in days }.forEach { (day, labelRes) ->
                    val own = dayTimes[day]
                    DayTimeRow(
                        label = stringResource(labelRes),
                        hour = own?.div(60) ?: hour,
                        minute = own?.rem(60) ?: minute,
                        isOwn = own != null,
                        onPick = { onPickDayTime(day) },
                        onReset = { onResetDayTime(day) }
                    )
                }
            }

            if (enabled) {
                Spacer(Modifier.height(12.dp))
                Text(
                    text = nextAlarmText,
                    style = MaterialTheme.typography.bodySmall
                )

                // Ein hringing, ekki auka vekjari. Þjóðhátíð, veikindi.
                if (days.isNotEmpty()) {
                    if (skipActive && skippedWhenText != null) {
                        Text(
                            text = stringResource(R.string.skip_active, skippedWhenText),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        TextButton(onClick = onUndoSkip) {
                            Text(stringResource(R.string.skip_undo))
                        }
                    } else {
                        TextButton(onClick = onSkipNext) {
                            Text(stringResource(R.string.skip_next))
                        }
                    }
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

/** Manudagur fyrst, eins og i DayPicker. */
private val WEEK_ORDER = listOf(
    Calendar.MONDAY to R.string.day_monday,
    Calendar.TUESDAY to R.string.day_tuesday,
    Calendar.WEDNESDAY to R.string.day_wednesday,
    Calendar.THURSDAY to R.string.day_thursday,
    Calendar.FRIDAY to R.string.day_friday,
    Calendar.SATURDAY to R.string.day_saturday,
    Calendar.SUNDAY to R.string.day_sunday
)

/**
 * Ein lina i dagalistanum: nafn dagsins, klukka sem ma yta a, og
 * "Sjalfgefid" ef dagurinn hefur eigin tima. Dagur an eigin tima synir
 * sjalfgefna timann daufan - hann fylgir storu klukkunni.
 */
@Composable
private fun DayTimeRow(
    label: String,
    hour: Int,
    minute: Int,
    isOwn: Boolean,
    onPick: () -> Unit,
    onReset: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )
        if (isOwn) {
            TextButton(onClick = onReset) {
                Text(stringResource(R.string.per_day_reset))
            }
        }
        Text(
            text = String.format(Locale.getDefault(), "%02d:%02d", hour, minute),
            style = MaterialTheme.typography.titleLarge,
            color = if (isOwn) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .clickable(
                    onClickLabel = stringResource(R.string.change_time),
                    onClick = onPick
                )
                .padding(horizontal = 8.dp, vertical = 6.dp)
        )
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
