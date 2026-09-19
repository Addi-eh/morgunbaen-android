package com.morgunbaen.app.data

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Tími hvers dags er vistaður sem strengur, "1=540,7=540".
 * Skemmd færsla má aldrei fella vekjarann — hún er einfaldlega hunsuð
 * og dagurinn fylgir sjálfgefna tímanum.
 */
class DayTimesParseTest {

    @Test
    fun `gildur strengur lesst rett`() {
        assertEquals(mapOf(1 to 540, 7 to 570), Prefs.parseDayTimes("1=540,7=570"))
    }

    @Test
    fun `tomur eda vantar skilar tomu korti`() {
        assertEquals(emptyMap<Int, Int>(), Prefs.parseDayTimes(null))
        assertEquals(emptyMap<Int, Int>(), Prefs.parseDayTimes(""))
    }

    @Test
    fun `skemmdar faerslur eru hunsadar en hinar halda ser`() {
        // Dagur 8 er ekki til, 1440 mín er ekki klukka, "x" er ekki tala.
        assertEquals(
            mapOf(2 to 420),
            Prefs.parseDayTimes("8=400,3=1440,x=10,2=420,4")
        )
    }
}
