package com.morgunbaen.app.ui

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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
 * Fastir valkostir frekar en sleði. Enginn tarf ad velja 47 sekundur,
 * og fastir kostir eru miklu audveldari i notkun a litlum skja.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun MinutePicker(options: List<Int>, selected: Int, onChange: (Int) -> Unit) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        options.forEach { minutes ->
            FilterChip(
                selected = minutes == selected,
                onClick = { onChange(minutes) },
                label = { Text("$minutes mín") }
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

/**
 * Titillinn i toppbordanum - sami a badum skjum.
 *
 * 26sp i stad sjalfgefinna 22sp: Cormorant hefur laga x-haed og virkar
 * minna en sans-letur i somu punktastaerd.
 */
@Composable
internal fun AppBarTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleLarge.copy(
            fontFamily = TitleFont,
            fontWeight = FontWeight.SemiBold,
            fontSize = 26.sp
        )
    )
}
