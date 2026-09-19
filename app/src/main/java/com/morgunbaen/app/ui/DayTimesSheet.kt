package com.morgunbaen.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
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
 * Tími hvers dags, í blaði sem rennur upp neðan frá.
 *
 * Hver breyting vistast STRAX í gegnum onPickDayTime/onResetDayTime —
 * „Lokið" lokar bara blaðinu. Þannig tapast ekkert þótt því sé strokið
 * niður, ýtt á bakktakkann eða fyrir ofan það.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DayTimesSheet(
    days: Set<Int>,
    dayTimes: Map<Int, Int>,
    hour: Int,
    minute: Int,
    onPickDayTime: (Int) -> Unit,
    onResetDayTime: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    // Allir sjö dagarnir komast fyrir — hálfopið blað myndi fela sunnudaginn.
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(Modifier.padding(start = 24.dp, end = 24.dp, bottom = 24.dp)) {
            Text(
                text = stringResource(R.string.per_day_sheet_title),
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                text = stringResource(R.string.per_day_desc_on),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))

            // Aðeins valdir dagar: tími á degi sem hringir aldrei er
            // stilling sem gerir ekkert og villir um fyrir notandanum.
            WEEK_ORDER.filter { it.day in days }.forEach { entry ->
                val own = dayTimes[entry.day]
                DayTimeRow(
                    label = stringResource(entry.name),
                    hour = own?.div(60) ?: hour,
                    minute = own?.rem(60) ?: minute,
                    isOwn = own != null,
                    onPick = { onPickDayTime(entry.day) },
                    onReset = { onResetDayTime(entry.day) }
                )
            }

            Spacer(Modifier.height(16.dp))
            Button(
                onClick = onDismiss,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text(stringResource(R.string.per_day_done))
            }
        }
    }
}

/**
 * Samantektin á spjaldinu: „Fös 08:15 · Lau 09:30".
 * Aðeins valdir dagar með eigin tíma — hinir fylgja stóru klukkunni
 * og þurfa ekki að standa þar. Enginn eigin tími: „Allir dagar kl. 07:00".
 */
@Composable
internal fun dayTimesSummary(
    days: Set<Int>,
    dayTimes: Map<Int, Int>,
    hour: Int,
    minute: Int
): String {
    val parts = WEEK_ORDER
        .filter { it.day in days && dayTimes.containsKey(it.day) }
        .map { entry ->
            val m = dayTimes.getValue(entry.day)
            "${stringResource(entry.shortName)} ${clock(m / 60, m % 60)}"
        }
    return if (parts.isEmpty()) {
        stringResource(R.string.per_day_all_same, clock(hour, minute))
    } else {
        parts.joinToString(" · ")
    }
}

/** Mánudagur fyrst, eins og í DayPicker. */
private data class WeekDay(val day: Int, val name: Int, val shortName: Int)

private val WEEK_ORDER = listOf(
    WeekDay(Calendar.MONDAY, R.string.day_monday, R.string.day_monday_short),
    WeekDay(Calendar.TUESDAY, R.string.day_tuesday, R.string.day_tuesday_short),
    WeekDay(Calendar.WEDNESDAY, R.string.day_wednesday, R.string.day_wednesday_short),
    WeekDay(Calendar.THURSDAY, R.string.day_thursday, R.string.day_thursday_short),
    WeekDay(Calendar.FRIDAY, R.string.day_friday, R.string.day_friday_short),
    WeekDay(Calendar.SATURDAY, R.string.day_saturday, R.string.day_saturday_short),
    WeekDay(Calendar.SUNDAY, R.string.day_sunday, R.string.day_sunday_short)
)

private fun clock(hour: Int, minute: Int): String =
    String.format(Locale.getDefault(), "%02d:%02d", hour, minute)

/**
 * Ein lína í dagalistanum: nafn dagsins, klukka sem má ýta á, og
 * „Sjálfgefið" ef dagurinn hefur eigin tíma. Dagur án eigin tíma sýnir
 * sjálfgefna tímann daufan — hann fylgir stóru klukkunni.
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
            text = clock(hour, minute),
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
