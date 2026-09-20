package aeh.heimvisir.model

/**
 * Niðurstaða einnar uppflettingar.
 *
 * Villuástöndin eru fjögur en ekki eitt, viljandi. Notandi sem fær
 * „villa kom upp" veit ekki hvort hann eigi að kveikja á netinu, reyna
 * aftur eftir mínútu, eða hvort appið sé einfaldlega hætt að virka af
 * því viðmótið hjá Dýraauðkenni breyttist. Hvert þessara ástanda á sér
 * ólíkt svar, svo þau eru aðgreind alla leið upp í viðmótið.
 */
sealed interface LookupResult {

    /** Dýr fannst. */
    data class Found(val pet: Pet) : LookupResult

    /** Skráin þekkir ekki merkið. Ekki villa — svar. */
    data object NotFound : LookupResult

    /** Náðist ekki samband: ekkert net, tímamörk runnu út, tenging rofnaði. */
    data object NetworkError : LookupResult

    /**
     * Sambandið hafðist en svarið var ekki það sem við kunnum að lesa.
     * Þetta er merkið um að viðmótið hafi breyst — sjá fyrirvarann í
     * README.
     */
    data object MalformedResponse : LookupResult

    /** Skráin svaraði með villukóða. */
    data class ServerError(val code: Int) : LookupResult
}
