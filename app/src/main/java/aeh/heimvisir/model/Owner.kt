package aeh.heimvisir.model

import kotlinx.serialization.Serializable

/**
 * Skráður eigandi.
 *
 * Dýraauðkenni birtir þessa reiti aðeins þegar eigandinn hefur samþykkt
 * birtingu. Hafi hann ekki gert það koma þeir auðir eða vantar alveg —
 * þess vegna eru þeir allir valfrjálsir, og þess vegna er [hasContact]
 * til: viðmótið á að segja „þessi eigandi hefur ekki samþykkt birtingu"
 * frekar en að sýna auðan reit.
 */
@Serializable
data class Owner(
    val personId: Int? = null,
    val name: String? = null,
    val personEmail: String? = null,
    val personAddress: String? = null,
    val postalCode: String? = null,
    val location: String? = null,
    val phoneNumbers: List<String>? = null,
) {
    /** Símanúmer án auðra strengja. */
    val phones: List<String>
        get() = phoneNumbers.orEmpty().filter { it.isNotBlank() }

    /** Póstnúmer og sveitarfélag sameinuð, eða null ef hvorugt er til. */
    val place: String?
        get() = listOfNotNull(postalCode, location)
            .filter { it.isNotBlank() }
            .joinToString(" ")
            .ifBlank { null }

    /** Satt ef eitthvað er til að birta yfirleitt. */
    val hasContact: Boolean
        get() = !name.isNullOrBlank() ||
            !personEmail.isNullOrBlank() ||
            !personAddress.isNullOrBlank() ||
            phones.isNotEmpty()
}
