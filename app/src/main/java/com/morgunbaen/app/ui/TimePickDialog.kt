package com.morgunbaen.app.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimeInput
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.morgunbaen.app.R

/**
 * Klukkuval appsins. Kom i stad android.app.TimePickerDialog, sem var
 * adskotahlutur: hann las temad ur STILLINGU SIMANS en ekki ur
 * AppTheme.mode, svo "Dokkt" i ljosum sima skiladi ljosum glugga.
 *
 * Undir skifunni stendur hve langur svefninn verdur. Tessi tala var adur
 * adeins synileg EFTIR ad glugganum var lokad - i belgnum a spjaldinu -
 * tott tad se einmitt talan sem akvordunin snyst um.
 *
 * Flisarnar tvaer efst segja HVAD verdur breytt. Adur voru tvaer adskildar
 * valmyndir - stora klukkan og "Sjalfgefid" - og hvorug sagdi hvad hun aetladi
 * ad snerta. Ein valmynd sem nefnir tad sjalf leysir tad an tess ad taka
 * neitt burt.
 *
 * @param dayLabel      Dagurinn sem vinstri flisin a vid.
 * @param showScope     Falsk tegar adeins einn dagur er kveiktur - ta gera
 *                      flisarnar tvaer nakvaemlega tad sama.
 * @param sleepPreview  Hvad tillagan tydir i svefni, fyrir valinn hop. null
 *                      tegar engin naesta hringing er til (slokkt, enginn
 *                      dagur valinn).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TimePickDialog(
    dayLabel: String,
    showScope: Boolean,
    initialAllDays: Boolean,
    initialHour: Int,
    initialMinute: Int,
    sleepPreview: (Boolean, Int, Int) -> String?,
    onDismiss: () -> Unit,
    onConfirm: (Boolean, Int, Int) -> Unit
) {
    val state = rememberTimePickerState(
        initialHour = initialHour,
        initialMinute = initialMinute,
        is24Hour = true
    )
    var typing by rememberSaveable { mutableStateOf(false) }
    var allDays by rememberSaveable { mutableStateOf(initialAllDays) }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { onConfirm(allDays, state.hour, state.minute) }) {
                Text(stringResource(R.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
        // Titillinn nefnir daginn tegar flisarnar eru faldar OG eitt dagsval
        // er i gangi. Seu engir dagar valdir er ekkert dagsnafn ad nefna.
        title = {
            Text(
                if (!showScope && !allDays) dayLabel
                else stringResource(R.string.pick_time_title)
            )
        },
        text = {
            // Skifan er ha. A litlum skja i landslagi kemst hun ekki fyrir
            // an skruns, og tha er glugginn ekki haegt ad stadfesta.
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (showScope) {
                    ChoiceChips(
                        options = listOf(
                            SCOPE_DAY to dayLabel,
                            SCOPE_ALL to stringResource(R.string.all_days)
                        ),
                        selected = if (allDays) SCOPE_ALL else SCOPE_DAY,
                        onSelect = { allDays = it == SCOPE_ALL }
                    )
                    Spacer(Modifier.height(8.dp))
                }

                if (typing) TimeInput(state = state) else TimePicker(state = state)

                // state.hour/minute eru Compose-stada, svo tetta uppfaerist
                // medan fingurinn er enn a skifunni.
                sleepPreview(allDays, state.hour, state.minute)?.let { preview ->
                    Text(
                        text = preview,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(4.dp))
                }

                TextButton(onClick = { typing = !typing }) {
                    Text(
                        stringResource(
                            if (typing) R.string.time_input_dial else R.string.time_input_keyboard
                        )
                    )
                }
            }
        }
    )
}

/** Adeins innri audkenni fyrir ChoiceChips - notandinn ser merkin. */
private const val SCOPE_DAY = "day"
private const val SCOPE_ALL = "all"
