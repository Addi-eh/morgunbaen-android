package com.morgunbaen.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.morgunbaen.app.R
import java.util.Calendar
import java.util.Locale

/**
 * Vikan i sjo reitum: stafur ofan, timi nedan.
 *
 * Ytt a stafinn kveikir eda slekkur a deginum. Ytt a timann stillir TANN
 * dag. Dagur an eigin tima synir sjalfgefna timann daufan og fylgir honum.
 *
 * Leysti af holmi DayPicker (stafirnir einir) og DayTimesSheet (bladid
 * nedan fra): baedi spurdu um sama hlutinn a sitt hvorum stadnum, og timi
 * hvers dags var hamur sem turfti ad kveikja a adur en matti stilla hann.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun DayStrip(
    days: Set<Int>,
    dayTimes: Map<Int, Int>,
    defaultHour: Int,
    defaultMinute: Int,
    onToggleDay: (Int) -> Unit,
    onPickDayTime: (Int) -> Unit
) {
    // Talan er EFRI MORK, ekki fastur fjoldi: FlowRow brytur linuna hvort
    // sem er tegar breiddin klarast.
    // Oheft myndi tad gefa 6+1 a Pixel-flokki - sunnudagurinn einn a
    // linu - svo morkin utiloka tad.
    //
    // Reitur er 48 dp og bilid 6, svo fimm reitir taka 264 dp og spjaldid
    // hefur skjabreidd minus 80. Virku dagarnir komast tvi a fyrri linuna
    // og helgin a tha sidari fra 360 dp og upp. A 320 dp sima fellur tad
    // sjalfkrafa i 4+3 frekar en ad klippa sunnudaginn af.
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        maxItemsInEachRow = 5
    ) {
        WEEK_ORDER.forEach { entry ->
            DayColumn(
                entry = entry,
                on = entry.day in days,
                own = dayTimes[entry.day],
                defaultHour = defaultHour,
                defaultMinute = defaultMinute,
                onToggle = { onToggleDay(entry.day) },
                onPickTime = { onPickDayTime(entry.day) }
            )
        }
    }
}

/** 48 dp er minnsti snertifloturinn sem ma bjoda. Badir eru tad. */
private val TARGET = 48.dp

@Composable
private fun DayColumn(
    entry: WeekDay,
    on: Boolean,
    own: Int?,
    defaultHour: Int,
    defaultMinute: Int,
    onToggle: () -> Unit,
    onPickTime: () -> Unit
) {
    val name = stringResource(entry.name)
    val hour = own?.div(60) ?: defaultHour
    val minute = own?.rem(60) ?: defaultMinute
    val label = clock(hour, minute)
    val stringDay = stringResource(
        if (on) R.string.cd_day_on else R.string.cd_day_off,
        name
    )
    val stringTime = stringResource(
        if (own != null) R.string.cd_day_time_own else R.string.cd_day_time_default,
        name,
        label
    )

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(TARGET)
                .clip(CircleShape)
                .background(
                    if (on) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                )
                .clickable(
                    onClickLabel = stringResource(
                        if (on) R.string.cd_day_turn_off else R.string.cd_day_turn_on
                    ),
                    onClick = onToggle
                )
                // semantics en ekki clearAndSetSemantics: tad sidarnefnda
                // tekur smellinn sjalfan med ser og skilur eftir reit sem
                // TalkBack getur ekki ytt a.
                .semantics(mergeDescendants = true) {
                    contentDescription = stringDay
                },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(entry.shortName),
                style = MaterialTheme.typography.labelLarge,
                color = if (on) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }

        if (on) {
            Box(
                modifier = Modifier
                    .size(TARGET)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(
                        onClickLabel = stringResource(R.string.change_time),
                        onClick = onPickTime
                    )
                    .semantics(mergeDescendants = true) {
                        contentDescription = stringTime
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (own != null) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
        } else {
            // Slokktur dagur hefur engan tima ad stilla. Strikid er skraut,
            // ekki annar snertiflotur - tvi ma tad ekki vera i tab-rodinni.
            Box(
                modifier = Modifier
                    .size(TARGET)
                    .clearAndSetSemantics { },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.day_off),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/** Manudagur fyrst, eins og i gamla DayPicker. */
internal data class WeekDay(val day: Int, val name: Int, val shortName: Int)

internal val WEEK_ORDER = listOf(
    WeekDay(Calendar.MONDAY, R.string.day_monday, R.string.day_monday_short),
    WeekDay(Calendar.TUESDAY, R.string.day_tuesday, R.string.day_tuesday_short),
    WeekDay(Calendar.WEDNESDAY, R.string.day_wednesday, R.string.day_wednesday_short),
    WeekDay(Calendar.THURSDAY, R.string.day_thursday, R.string.day_thursday_short),
    WeekDay(Calendar.FRIDAY, R.string.day_friday, R.string.day_friday_short),
    WeekDay(Calendar.SATURDAY, R.string.day_saturday, R.string.day_saturday_short),
    WeekDay(Calendar.SUNDAY, R.string.day_sunday, R.string.day_sunday_short)
)

internal fun clock(hour: Int, minute: Int): String =
    String.format(Locale.getDefault(), "%02d:%02d", hour, minute)
