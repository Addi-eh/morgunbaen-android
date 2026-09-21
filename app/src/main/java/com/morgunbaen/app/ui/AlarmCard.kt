package com.morgunbaen.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.Edit
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
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.morgunbaen.app.R

/**
 * Hvad stora talan a spjaldinu synir.
 *
 * NEXT_RING er venjulega astandid: talan svarar teirri spurningu sem
 * notandinn vaknar med - "hvenaer hringir hann naest?" - en ekki teirri
 * obeinu, hver sjalfgefni timinn se.
 */
internal enum class ClockMode { NEXT_RING, DEFAULT, SNOOZE }

/**
 * Efsta spjaldid: naesta hringing, vikan i dalkum og profunarhnappur.
 *
 * Allt state byr i MainScreen - spjaldid faer gildi og skilar atburdum.
 * Klukkuglugginn er lika hja MainScreen, tvi hann tarf ad vita hvad er
 * verid ad stilla; her eru bara onPickNext/onPickDefault/onPickDayTime.
 */
@Composable
internal fun AlarmCard(
    displayHour: Int,
    displayMinute: Int,
    clockMode: ClockMode,
    nextDayLabel: String?,
    defaultHour: Int,
    defaultMinute: Int,
    enabled: Boolean,
    days: Set<Int>,
    dayTimes: Map<Int, Int>,
    nextAlarmText: String,
    countdownText: String?,
    skipActive: Boolean,
    skippedWhenText: String?,
    testArmed: Boolean,
    testSeconds: Int,
    onEnabledChange: (Boolean) -> Unit,
    onPickNext: () -> Unit,
    onPickDefault: () -> Unit,
    onToggleDay: (Int) -> Unit,
    onPickDayTime: (Int) -> Unit,
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
                // Blundur er eina astandid tar sem talan er ekki stilling
                // heldur frett: hun segir hvenaer hann hringir aftur i
                // dag. Morgundagurinn er stilltur i dalkunum.
                BigClock(
                    hour = displayHour,
                    minute = displayMinute,
                    mode = clockMode,
                    nextDayLabel = nextDayLabel,
                    onClick = when (clockMode) {
                        ClockMode.NEXT_RING -> onPickNext
                        ClockMode.DEFAULT -> onPickDefault
                        ClockMode.SNOOZE -> null
                    }
                )
                Switch(checked = enabled, onCheckedChange = onEnabledChange)
            }

            if (nextDayLabel != null) {
                Text(
                    text = nextDayLabel,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    // Lysing storu tolunnar nefnir daginn tegar. An tessa
                    // laesi TalkBack "laugardag" tvisvar.
                    modifier = Modifier.clearAndSetSemantics { }
                )
            }

            // Sjalfgefni timinn er eina leidin ad alarmHour/alarmMinute.
            // Hann stendur ALLTAF tegar stora talan synir eitthvad annad,
            // ekki adeins tegar naesti dagur vikur fra - annars veit enginn
            // hvert a ad fara til ad breyta ollum dogunum i einu.
            //
            // Belgurinn deilir linunni med honum frekar en ad standa vid
            // hlidina a storu tolunni: a 320 dp skja tekur "9 klst 30 min"
            // svo mikid plass ad klukkan sjalf kemst ekki fyrir.
            if (clockMode != ClockMode.DEFAULT) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    DefaultTimeLine(
                        hour = defaultHour,
                        minute = defaultMinute,
                        onClick = onPickDefault
                    )
                    // Teljarinn svarar teirri spurningu sem klukkan sjalf
                    // svarar ekki: hve lengi ma eg enn sofa?
                    if (enabled && countdownText != null) {
                        CountdownPill(text = countdownText)
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // Vikan: stafur kveikir eda slekkur, talan undir stillir tann
            // dag. Morgunbaenin er DAGLEG - lika um helgar - svo tetta er
            // hrein timastilling: sofa lengur, eda fara fyrr a faetur, an
            // tess ad missa af baen tess dags.
            DayStrip(
                days = days,
                dayTimes = dayTimes,
                defaultHour = defaultHour,
                defaultMinute = defaultMinute,
                onToggleDay = onToggleDay,
                onPickDayTime = onPickDayTime
            )

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

/**
 * Stora talan. Undirstrikid er eina merkid um ad hun se stillanleg -
 * "Breyta tima"-hnappurinn undir henni for ut tegar hann og hun gerdu
 * ordid sama hlutinn.
 */
@Composable
private fun BigClock(
    hour: Int,
    minute: Int,
    mode: ClockMode,
    nextDayLabel: String?,
    onClick: (() -> Unit)?
) {
    val label = clock(hour, minute)
    val description = when (mode) {
        ClockMode.NEXT_RING ->
            stringResource(R.string.cd_next_ring_edit_day, nextDayLabel.orEmpty(), label)
        ClockMode.DEFAULT -> stringResource(R.string.cd_default_clock, label)
        ClockMode.SNOOZE -> stringResource(R.string.cd_snooze_clock, label)
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        onClickLabel = stringResource(R.string.change_time),
                        onClick = onClick
                    )
                } else {
                    Modifier
                }
            )
            .semantics(mergeDescendants = true) { contentDescription = description }
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.displayMedium
        )
        // Enginn penni medan blundad er: ta er talan frett en ekki stilling,
        // og ma ekki lita ut fyrir ad vera stillanleg.
        if (onClick != null) {
            Spacer(Modifier.width(10.dp))
            Icon(
                imageVector = Icons.Outlined.Edit,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

/** „sjálfgefið 07:00“ — leiðin að alarmHour/alarmMinute. */
@Composable
private fun DefaultTimeLine(hour: Int, minute: Int, onClick: () -> Unit) {
    val label = clock(hour, minute)
    val description = stringResource(R.string.default_time_desc, label)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(
                onClickLabel = stringResource(R.string.change_time),
                onClick = onClick
            )
            // Eina leidin ad sjalfgefna timanum - tvi ma hun ekki vera
            // minni en 48 dp, tott textinn sjalfur se lagur.
            .defaultMinSize(minHeight = 48.dp)
            .semantics(mergeDescendants = true) { contentDescription = description }
    ) {
        Text(
            text = stringResource(R.string.default_time, label),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.width(6.dp))
        Icon(
            imageVector = Icons.Outlined.Edit,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(16.dp)
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
