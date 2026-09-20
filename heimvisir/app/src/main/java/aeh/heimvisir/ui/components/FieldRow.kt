package aeh.heimvisir.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Merki og gildi.
 *
 * Reiturinn FELUR SIG þegar gildið vantar — undantekningarlaust. Í
 * vefútgáfunni gilti það um alla reiti nema „Geldur", sem birtist alltaf
 * með „Óþekkt" og lét skjáinn líta út fyrir að vera fullur af upplýsingum
 * sem voru ekki til.
 */
@Composable
fun FieldRow(
    label: String,
    value: String?,
    modifier: Modifier = Modifier,
    monospace: Boolean = false,
) {
    if (value.isNullOrBlank()) return

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontFamily = if (monospace) FontFamily.Monospace else null,
        )
    }
}
