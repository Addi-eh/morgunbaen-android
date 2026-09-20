package aeh.heimvisir.model

import kotlinx.serialization.Serializable

/**
 * Dýr eins og skrá Dýraauðkennis skilar því.
 *
 * Allir reitir nema [petId] eru valfrjálsir og með sjálfgildi. Það er
 * ekki varkárni út í loftið: viðmótið er óskjalfað, og reitir sem eru
 * til staðar í einu svari vantar í því næsta — `dayOfDisappear` kemur
 * til dæmis aðeins þegar dýr er skráð týnt. Reitur sem vantar má aldrei
 * fella þáttunina.
 */
@Serializable
data class Pet(
    val petId: Int,
    val name: String? = null,
    val gender: String? = null,
    val breed: String? = null,
    val species: String? = null,
    val birthDate: String? = null,
    val birthYear: Int? = null,
    val color: String? = null,
    val isCastrated: Boolean? = null,
    val isLost: Boolean = false,
    val dayOfDisappear: String? = null,
    val owners: List<Owner>? = null,
)
