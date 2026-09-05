package aeh.heimvisir.ui.components

import aeh.heimvisir.R
import aeh.heimvisir.model.Pet
import aeh.heimvisir.ui.formatIcelandicDate
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Niðurstaðan: dýrið og allir skráðir eigendur þess.
 */
@Composable
fun PetCard(tagNumber: String, pet: Pet, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Text(
                    text = pet.name?.takeIf { it.isNotBlank() }
                        ?: stringResource(R.string.pet_unnamed),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                if (pet.isLost) {
                    LostBadge()
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                FieldRow(stringResource(R.string.field_tag), tagNumber, monospace = true)
                FieldRow(stringResource(R.string.field_species), speciesLine(pet))
                FieldRow(stringResource(R.string.field_gender), pet.gender)
                FieldRow(stringResource(R.string.field_birth), birthLine(pet))
                FieldRow(stringResource(R.string.field_colour), pet.color)
                // Reiturinn birtist aðeins ef svarið segir eitthvað.
                // Vefútgáfan sýndi „Óþekkt" að eilífu.
                FieldRow(
                    label = stringResource(R.string.field_castrated),
                    value = pet.isCastrated?.let {
                        stringResource(if (it) R.string.yes else R.string.no)
                    },
                )
                if (pet.isLost) {
                    FieldRow(
                        stringResource(R.string.field_lost_since),
                        formatIcelandicDate(pet.dayOfDisappear),
                    )
                }
            }

            HorizontalDivider()

            Text(
                text = stringResource(R.string.section_owner),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )

            val owners = pet.owners.orEmpty()
            if (owners.isEmpty()) {
                Text(
                    text = stringResource(R.string.owner_none),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                // ALLIR eigendur, ekki bara sá fyrsti. Dýr getur átt fleiri
                // en einn skráðan eiganda og hinir mega ekki hverfa þegjandi.
                owners.forEachIndexed { index, owner ->
                    if (index > 0) HorizontalDivider()
                    OwnerCard(owner)
                }
            }
        }
    }
}

@Composable
private fun speciesLine(pet: Pet): String? =
    listOfNotNull(pet.species, pet.breed)
        .filter { it.isNotBlank() }
        .joinToString(" · ")
        .ifBlank { null }

@Composable
private fun birthLine(pet: Pet): String? =
    formatIcelandicDate(pet.birthDate) ?: pet.birthYear?.toString()
