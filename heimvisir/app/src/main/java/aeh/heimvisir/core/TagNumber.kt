package aeh.heimvisir.core

/**
 * Hreinsun og athugun á merkjanúmeri, aðskilið frá öllu öðru svo það
 * sé hægt að prófa án síma.
 *
 * Örmerki eru 15 tölustafir (ISO 11784). Eyrnamerki og eldri merki eru
 * styttri og geta innihaldið bókstafi, svo reglan hér er rúm — við
 * höfnum aðeins því sem getur augljóslega ekki verið merki. Skráin
 * sjálf sker úr um restina.
 */
object TagNumber {

    /** Stysta merki sem við sendum áfram. */
    const val MIN_LENGTH = 4

    /** Lengsta merki sem við sendum áfram. */
    const val MAX_LENGTH = 20

    /**
     * Hreinsar það sem notandinn sló inn: bil, bandstrik, punktar og
     * undirstrik falla burt og bókstafir verða hástafir.
     *
     * Fólk les merki af kvittun eða skilti þar sem þau eru oft rituð með
     * bilum („352 098 100 000 000"), svo þetta er ekki snyrtimennska
     * heldur nauðsyn.
     */
    fun normalize(raw: String): String =
        raw.filterNot { it.isWhitespace() || it in "-_." }.uppercase()

    /**
     * Niðurstaða athugunar. [Valid] ber hreinsaða merkið; hinar tvær bera
     * ástæðu sem má sýna notandanum orðrétt.
     */
    sealed interface Check {
        data class Valid(val normalized: String) : Check
        data object TooShort : Check
        data object TooLong : Check
        data object HasIllegalCharacters : Check
    }

    /**
     * Athugar hreinsað merki. Skilar aldrei undantekningu — kallandinn
     * fær ástæðu sem hann getur birt.
     */
    fun check(raw: String): Check {
        val normalized = normalize(raw)
        return when {
            normalized.length < MIN_LENGTH -> Check.TooShort
            normalized.length > MAX_LENGTH -> Check.TooLong
            !normalized.all { it.isDigit() || it in 'A'..'Z' } -> Check.HasIllegalCharacters
            else -> Check.Valid(normalized)
        }
    }
}
