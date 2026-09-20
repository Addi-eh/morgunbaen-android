package com.morgunbaen.app.ui

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.morgunbaen.app.R
import kotlinx.coroutines.delay
import java.util.Calendar

/**
 * Litlu endurnytanlegu einingarnar sem spjoldin thrju deila.
 * Fluttar hingad ur MainActivity tegar hun nalgadist 1000 linur.
 */

/**
 * Dagavalid. Notar FlowRow svo allir sjo dagarnir komist fyrir
 * - i venjulegri Row dettur sunnudagurinn ut fyrir skjabrunina.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun DayPicker(selected: Set<Int>, onChange: (Set<Int>) -> Unit) {
    // Calendar.SUNDAY = 1 ... Calendar.SATURDAY = 7
    val labels = listOf(
        Calendar.MONDAY to "Má",
        Calendar.TUESDAY to "Þr",
        Calendar.WEDNESDAY to "Mi",
        Calendar.THURSDAY to "Fi",
        Calendar.FRIDAY to "Fö",
        Calendar.SATURDAY to "La",
        Calendar.SUNDAY to "Su"
    )

    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        labels.forEach { (day, label) ->
            val isOn = day in selected
            FilterChip(
                selected = isOn,
                onClick = {
                    onChange(if (isOn) selected - day else selected + day)
                },
                label = { Text(label) }
            )
        }
    }
}

@Composable
internal fun WarningCard(text: String, actionLabel: String, onAction: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = onAction) { Text(actionLabel) }
        }
    }
}

@Composable
internal fun SettingRow(
    label: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(text = label, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.width(12.dp))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

/**
 * Teljari fyrir minutur. Adur voru fastir kostir her - 5, 9, 10, 15, 20 -
 * en fólk vill sinn eigin tima, og blundur er eina stillingin tar sem
 * ein minuta til eda fra skiptir raunverulegu mali.
 */
@Composable
internal fun MinuteStepper(value: Int, range: IntRange, onChange: (Int) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        StepButton(
            icon = Icons.Outlined.Remove,
            contentDescription = stringResource(R.string.snooze_less),
            enabled = value > range.first,
            onStep = { onChange((value - 1).coerceAtLeast(range.first)) }
        )
        Text(
            text = stringResource(R.string.snooze_value, value),
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f)
        )
        StepButton(
            icon = Icons.Outlined.Add,
            contentDescription = stringResource(R.string.snooze_more),
            enabled = value < range.last,
            onStep = { onChange((value + 1).coerceAtMost(range.last)) }
        )
    }
}

/** Bidin adur en haldinn hnappur fer ad telja sjalfur. */
private const val HOLD_DELAY_MS = 400L

/** Hversu ort haldinn hnappur telur. 51 yting ur 9 i 60 er engum bjodandi. */
private const val HOLD_STEP_MS = 80L

@Composable
private fun StepButton(
    icon: ImageVector,
    contentDescription: String,
    enabled: Boolean,
    onStep: () -> Unit
) {
    val interactions = remember { MutableInteractionSource() }
    val pressed by interactions.collectIsPressedAsState()
    // Satt medan haldid er inni OG teljarinn er tekinn vid ser. An tessa
    // baetti smellurinn, sem kemur vid ad sleppa takkanum, einu skrefi
    // ofan a allt sem haldid skiladi - talan hoppadi um eitt vid sleppinn.
    val repeating = remember { mutableStateOf(false) }
    val step by rememberUpdatedState(onStep)

    LaunchedEffect(pressed, enabled) {
        if (!pressed || !enabled) return@LaunchedEffect
        repeating.value = false
        delay(HOLD_DELAY_MS)
        repeating.value = true
        while (true) {
            step()
            delay(HOLD_STEP_MS)
        }
    }

    IconButton(
        onClick = {
            if (repeating.value) repeating.value = false else step()
        },
        enabled = enabled,
        interactionSource = interactions
    ) {
        Icon(imageVector = icon, contentDescription = contentDescription)
    }
}

/** Einval med flisum. Notad badi i Vakningu og i utlitsvalinu. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ChoiceChips(
    options: List<Pair<String, String>>,
    selected: String,
    onSelect: (String) -> Unit
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        options.forEach { (value, label) ->
            FilterChip(
                selected = selected == value,
                onClick = { onSelect(value) },
                label = { Text(label) }
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun FadeLengthPicker(selected: Int, onChange: (Int) -> Unit) {
    val options = listOf(10, 30, 60, 120)
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        options.forEach { seconds ->
            FilterChip(
                selected = seconds == selected,
                onClick = { onChange(seconds) },
                label = {
                    Text(
                        if (seconds < 60) "$seconds sek"
                        else "${seconds / 60} mín"
                    )
                }
            )
        }
    }
}

@Composable
internal fun InfoCard(
    title: String,
    text: String,
    primaryLabel: String,
    onPrimary: () -> Unit,
    secondaryLabel: String,
    onSecondary: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Spacer(Modifier.height(8.dp))
            Row {
                TextButton(onClick = onPrimary) { Text(primaryLabel) }
                Spacer(Modifier.width(8.dp))
                TextButton(onClick = onSecondary) { Text(secondaryLabel) }
            }
        }
    }
}
