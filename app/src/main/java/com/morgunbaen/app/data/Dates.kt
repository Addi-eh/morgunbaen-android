package com.morgunbaen.app.data

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Allur lestur á firstrun-timastimplum RUV a einum stad.
 *
 * RUV skilar ISO 8601 med T ("2026-08-13T06:55:00") en eldri svor hofdu bil
 * i stad T. Badar utgafur eru studdar her - og HVERGI annars stadar.
 * Breyti RUV snidinu er tetta eina skrain sem tarf ad opna; adur la sama
 * substringBefore-rokfraedin a fimm stodum og hver teirra gat gleymst.
 */
object Dates {

    private val icelandic = Locale("is", "IS")

    /** "2026-08-13T06:55:00" -> "2026-08-13". Tholir bil i stad T. */
    fun datePart(firstrun: String): String =
        firstrun.substringBefore('T').substringBefore(' ')

    /** "2026-08-13T06:55:00" -> "06:55". Tomur strengur ef enginn timi. */
    fun timePart(firstrun: String): String {
        val raw = firstrun.substringAfter('T', "")
            .ifEmpty { firstrun.substringAfter(' ', "") }
        return if (raw.length >= 5) raw.take(5) else ""
    }

    /** Dagurinn i dag sem "2026-08-13". */
    fun todayIso(): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    /** Er tessi timastimpill fra deginum i dag? null telst nei. */
    fun isToday(firstrun: String?): Boolean =
        firstrun != null && datePart(firstrun) == todayIso()

    /** "2026-08-13T06:55:00" -> "13. ágúst". Fellur aftur a hraan streng. */
    fun formatShort(firstrun: String): String = try {
        val parsed = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            .parse(datePart(firstrun))!!
        SimpleDateFormat("d. MMMM", icelandic).format(parsed)
    } catch (e: Exception) {
        datePart(firstrun)
    }

    /** "2026-08-13T06:55:00" -> "fimmtudagur 13. ágúst". */
    fun formatWithWeekday(firstrun: String): String = try {
        val parsed = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            .parse(datePart(firstrun))!!
        SimpleDateFormat("EEEE d. MMMM", icelandic).format(parsed)
    } catch (e: Exception) {
        datePart(firstrun)
    }
}
