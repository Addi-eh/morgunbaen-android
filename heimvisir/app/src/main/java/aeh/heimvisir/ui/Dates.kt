package aeh.heimvisir.ui

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

private val ICELANDIC = DateTimeFormatter.ofPattern("d.M.yyyy")

/**
 * Sníður dagsetningu úr svari skrárinnar á íslenskt form (`5.9.2026`).
 *
 * Skráin skilar ISO-dagsetningum, ýmist berum (`2019-04-01`) eða með
 * tíma aftan við. Við lesum aðeins dagshlutann með [LocalDate], sem er
 * tímabeltislaust — ólíkt `new Date(...)` í vefútgáfunni, sem gat fært
 * dagsetningu um einn dag eftir því hvar notandinn var staddur.
 *
 * Skilar null ef ekkert er hægt að lesa; þá felur reiturinn sig.
 */
fun formatIcelandicDate(value: String?): String? {
    val raw = value?.trim().orEmpty()
    if (raw.isEmpty()) return null

    val datePart = raw.substringBefore('T').substringBefore(' ')
    return try {
        LocalDate.parse(datePart).format(ICELANDIC)
    } catch (_: DateTimeParseException) {
        null
    }
}
