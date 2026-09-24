package com.morgunbaen.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.morgunbaen.app.R
import java.util.Calendar
import java.util.Locale

/**
 * Vikan i sjo reitum: stafur ofan, timi nedan.
 *
 * HVER REITUR ER EINN SNERTIFLOTUR. Adur voru teir tveir - stafurinn
 * kveikti og slokkti, timinn opnadi klukkuna - og ekkert sagdi hvort
 * gerdi hvad. Nu opnar reiturinn valmynd dagsins, tar sem rofinn heitir
 * "Hringja a fostudogum" og klukkan stendur undir honum. Fjortan
 * omerktir fletir urdu ad sjo merktum.
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
    onOpenDay: (Int) -> Unit
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
        // 16 dp milli rada en 14 dp milli stafs og tima innan reits: annars
        // er timinn SJONRAENT naer naestu rod en sinum eigin degi, og
        // strimillinn les sem tvaer adskildar radir af tolum.
        verticalArrangement = Arrangement.spacedBy(16.dp),
        maxItemsInEachRow = 5
    ) {
        WEEK_ORDER.forEach { entry ->
            DayColumn(
                entry = entry,
                on = entry.day in days,
                own = dayTimes[entry.day],
                defaultHour = defaultHour,
                defaultMinute = defaultMinute,
                onOpen = { onOpenDay(entry.day) }
            )
        }
    }
}

/** Hringurinn einn er 48 dp - reiturinn allur er snertifloturinn. */
private val TARGET = 48.dp

@Composable
private fun DayColumn(
    entry: WeekDay,
    on: Boolean,
    own: Int?,
    defaultHour: Int,
    defaultMinute: Int,
    onOpen: () -> Unit
) {
    val name = stringResource(entry.name)
    val hour = own?.div(60) ?: defaultHour
    val minute = own?.rem(60) ?: defaultMinute
    val label = clock(hour, minute)
    val description = if (on) {
        stringResource(R.string.cd_day_on_at, name, label)
    } else {
        stringResource(R.string.cd_day_off, name)
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(24.dp))
            .clickable(onClickLabel = stringResource(R.string.cd_open_day), onClick = onOpen)
            .semantics(mergeDescendants = true) { contentDescription = description }
    ) {
        Box(
            modifier = Modifier
                .size(TARGET)
                .clip(CircleShape)
                .background(
                    if (on) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                ),
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

        Spacer(Modifier.height(4.dp))

        Text(
            text = if (on) label else stringResource(R.string.day_off),
            style = MaterialTheme.typography.labelLarge,
            color = when {
                !on -> MaterialTheme.colorScheme.onSurfaceVariant
                own != null -> MaterialTheme.colorScheme.onSurface
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            }
        )

        Spacer(Modifier.height(4.dp))
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
