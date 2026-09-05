package aeh.heimvisir.ui.components

import aeh.heimvisir.R
import aeh.heimvisir.model.Owner
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

/**
 * Einn eigandi.
 *
 * Hafi eigandinn ekki samþykkt birtingu koma reitirnir auðir frá skránni.
 * Þá segjum við það hreint út og bendum á Dýraauðkenni — það er miklu
 * betra en spjald fullt af auðum reitum sem lítur út eins og bilun.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OwnerCard(owner: Owner, modifier: Modifier = Modifier) {
    val context = LocalContext.current

    if (!owner.hasContact) {
        Text(
            text = stringResource(R.string.owner_not_published),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = modifier.fillMaxWidth(),
        )
        return
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        FieldRow(stringResource(R.string.field_owner_name), owner.name)
        FieldRow(stringResource(R.string.field_address), owner.personAddress)
        FieldRow(stringResource(R.string.field_municipality), owner.place)

        if (owner.phones.isNotEmpty()) {
            Text(
                text = stringResource(R.string.field_phone).uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                owner.phones.forEach { phone ->
                    AssistChip(
                        onClick = {
                            val number = phone.filterNot { it.isWhitespace() }
                            context.startActivity(
                                Intent(Intent.ACTION_DIAL, "tel:$number".toUriCompat()),
                            )
                        },
                        label = { Text(phone) },
                        leadingIcon = { Icon(Icons.Filled.Phone, contentDescription = null) },
                    )
                }
            }
        }

        owner.personEmail?.takeIf { it.isNotBlank() }?.let { email ->
            AssistChip(
                onClick = {
                    context.startActivity(Intent(Intent.ACTION_SENDTO, "mailto:$email".toUriCompat()))
                },
                label = { Text(email) },
                leadingIcon = { Icon(Icons.Filled.Mail, contentDescription = null) },
            )
        }
    }
}

private fun String.toUriCompat(): Uri = Uri.parse(this)
