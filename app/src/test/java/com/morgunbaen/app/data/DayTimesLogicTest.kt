package com.morgunbaen.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Calendar

/**
 * Rökfræðin á bak við dálka vekjaraspjaldsins, prófuð án Android.
 *
 * Bæði föllin eru hrein af ásetningi: annað ræður því hvenær dagur fær
 * eigin tíma, hitt því hvaða tímar lifa af að rofinn var lagður niður.
 * Hvort tveggja ræður því hvenær fólk vaknar, svo hvorugt má vera
 * prófanlegt eingöngu í gegnum viðmótið.
 */
class DayTimesLogicTest {

    // ---------- applyPickedTime ----------

    @Test
    fun `dagur faer eigin tima tegar valid vikur fra sjalfgefnu`() {
        assertEquals(
            mapOf(Calendar.SATURDAY to 570),
            Prefs.applyPickedTime(
                defaultMinutes = 420,
                dayTimes = emptyMap(),
                day = Calendar.SATURDAY,
                pickedMinutes = 570
            )
        )
    }

    @Test
    fun `sjalfgefinn timi eydir lyklinum i stad tess ad skra hann`() {
        // Tetta er astaedan fyrir tvi ad "Sjalfgefid"-hnappurinn turfti
        // ekki ad fylgja: dagurinn verdur daufur um leid og hann er
        // stilltur a sama tima og hinir.
        assertEquals(
            emptyMap<Int, Int>(),
            Prefs.applyPickedTime(
                defaultMinutes = 420,
                dayTimes = mapOf(Calendar.SATURDAY to 570),
                day = Calendar.SATURDAY,
                pickedMinutes = 420
            )
        )
    }

    @Test
    fun `adrir dagar hreyfast ekki`() {
        assertEquals(
            mapOf(Calendar.FRIDAY to 390, Calendar.SATURDAY to 570),
            Prefs.applyPickedTime(
                defaultMinutes = 420,
                dayTimes = mapOf(Calendar.FRIDAY to 390, Calendar.SATURDAY to 600),
                day = Calendar.SATURDAY,
                pickedMinutes = 570
            )
        )
    }

    // ---------- retiredPerDayTimes ----------

    @Test
    fun `slokktur rofi tyddi ad timarnir giltu ekki - teir fara`() {
        // Sa sem stillti laugardag 09_30 og slokkti svo a rofanum atti
        // 07_00 a laugardag. Lifdi kortid af yrdi vekjarinn taum og
        // faerdist um tvo tima - tegjandi.
        assertNull(Prefs.retiredPerDayTimes(enabled = false, raw = "7=570"))
    }

    @Test
    fun `kveiktur rofi heldur kortinu obreyttu`() {
        assertEquals("1=540,7=540", Prefs.retiredPerDayTimes(enabled = true, raw = "1=540,7=540"))
    }

    @Test
    fun `tomt kort tolir faersluna i badar attir`() {
        assertNull(Prefs.retiredPerDayTimes(enabled = false, raw = null))
        assertEquals(null, Prefs.retiredPerDayTimes(enabled = true, raw = null))
    }
}
