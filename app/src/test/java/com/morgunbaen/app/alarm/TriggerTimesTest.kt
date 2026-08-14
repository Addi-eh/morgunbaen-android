package com.morgunbaen.app.alarm

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Calendar

/**
 * Prof a hjarta appsins - timareikningi vekjarans.
 *
 * Tetta eru fyrstu profin i verkefninu, og tau eru her af astaedu:
 * villan tar sem tomir vekjaradagar settu soknargluggann a rek hefdi
 * aldrei komist i gegnum "nextWindow(tomir dagar) == null". Reikningurinn
 * la bara laestur inni i hlutum sem turftu SharedPreferences til ad keyra.
 *
 * Vidmid: midvikudagurinn 12. agust 2026. Fastur dagur i stad "nuna"
 * svo profin bregdist aldrei eftir vikudegi keyrslunnar.
 */
class TriggerTimesTest {

    private val weekdays = setOf(
        Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY,
        Calendar.THURSDAY, Calendar.FRIDAY
    )

    /** Byr til fastan timapunkt. Manudur er 1-tenging (8 = agust). */
    private fun at(day: Int, hour: Int, minute: Int = 0): Calendar =
        Calendar.getInstance().apply {
            clear()
            set(2026, Calendar.AUGUST, day, hour, minute, 0)
        }

    private fun Long.asCalendar(): Calendar =
        Calendar.getInstance().apply { timeInMillis = this@asCalendar }

    // ------------------------------------------------------------------
    //  next
    // ------------------------------------------------------------------

    @Test
    fun `naesti timi er i dag ef klukkan er ekki ordin`() {
        // Midvikudagur kl. 06:00, vekjari 07:00 -> i dag kl. 07:00
        val next = TriggerTimes.next(
            days = weekdays, hour = 7, minute = 0,
            weekendEnabled = false, weekendHour = 9, weekendMinute = 0,
            from = at(12, 6)
        )!!.asCalendar()

        assertEquals(12, next.get(Calendar.DAY_OF_MONTH))
        assertEquals(7, next.get(Calendar.HOUR_OF_DAY))
        assertEquals(0, next.get(Calendar.MINUTE))
    }

    @Test
    fun `lidinn timi i dag faerist a naesta valda dag`() {
        // Midvikudagur kl. 08:00, vekjari 07:00 -> fimmtudagur
        val next = TriggerTimes.next(
            days = weekdays, hour = 7, minute = 0,
            weekendEnabled = false, weekendHour = 9, weekendMinute = 0,
            from = at(12, 8)
        )!!.asCalendar()

        assertEquals(13, next.get(Calendar.DAY_OF_MONTH))
        assertEquals(Calendar.THURSDAY, next.get(Calendar.DAY_OF_WEEK))
    }

    @Test
    fun `fostudagskvold stekkur yfir helgina tegar bara virkir dagar eru valdir`() {
        // Fostudagur 14. kl. 12:00 -> manudagur 17. kl. 07:00
        val next = TriggerTimes.next(
            days = weekdays, hour = 7, minute = 0,
            weekendEnabled = false, weekendHour = 9, weekendMinute = 0,
            from = at(14, 12)
        )!!.asCalendar()

        assertEquals(17, next.get(Calendar.DAY_OF_MONTH))
        assertEquals(Calendar.MONDAY, next.get(Calendar.DAY_OF_WEEK))
    }

    @Test
    fun `helgartimi gildir a laugardegi tegar hann er virkur`() {
        // Fostudagur kl. 12:00, laugardagur valinn, helgartimi 09:30
        val next = TriggerTimes.next(
            days = weekdays + Calendar.SATURDAY, hour = 7, minute = 0,
            weekendEnabled = true, weekendHour = 9, weekendMinute = 30,
            from = at(14, 12)
        )!!.asCalendar()

        assertEquals(15, next.get(Calendar.DAY_OF_MONTH))
        assertEquals(9, next.get(Calendar.HOUR_OF_DAY))
        assertEquals(30, next.get(Calendar.MINUTE))
    }

    @Test
    fun `an helgartima gildir venjulegi timinn lika um helgar`() {
        val next = TriggerTimes.next(
            days = weekdays + Calendar.SATURDAY, hour = 7, minute = 0,
            weekendEnabled = false, weekendHour = 9, weekendMinute = 30,
            from = at(14, 12)
        )!!.asCalendar()

        assertEquals(15, next.get(Calendar.DAY_OF_MONTH))
        assertEquals(7, next.get(Calendar.HOUR_OF_DAY))
    }

    @Test
    fun `tomir dagar skila null`() {
        assertNull(
            TriggerTimes.next(
                days = emptySet(), hour = 7, minute = 0,
                weekendEnabled = false, weekendHour = 9, weekendMinute = 0,
                from = at(12, 6)
            )
        )
    }

    // ------------------------------------------------------------------
    //  previous
    // ------------------------------------------------------------------

    @Test
    fun `sidasti timi er i dag ef klukkan er lidin`() {
        // Midvikudagur kl. 08:00 -> i dag kl. 07:00
        val prev = TriggerTimes.previous(
            days = weekdays, hour = 7, minute = 0,
            weekendEnabled = false, weekendHour = 9, weekendMinute = 0,
            from = at(12, 8)
        )!!.asCalendar()

        assertEquals(12, prev.get(Calendar.DAY_OF_MONTH))
        assertEquals(7, prev.get(Calendar.HOUR_OF_DAY))
    }

    @Test
    fun `manudagsmorgunn horfir aftur a fostudaginn`() {
        // Manudagur 17. kl. 06:00, bara virkir dagar -> fostudagur 14.
        val prev = TriggerTimes.previous(
            days = weekdays, hour = 7, minute = 0,
            weekendEnabled = false, weekendHour = 9, weekendMinute = 0,
            from = at(17, 6)
        )!!.asCalendar()

        assertEquals(14, prev.get(Calendar.DAY_OF_MONTH))
        assertEquals(Calendar.FRIDAY, prev.get(Calendar.DAY_OF_WEEK))
    }

    @Test
    fun `previous med toma daga skilar null`() {
        assertNull(
            TriggerTimes.previous(
                days = emptySet(), hour = 7, minute = 0,
                weekendEnabled = false, weekendHour = 9, weekendMinute = 0,
                from = at(12, 8)
            )
        )
    }

    // ------------------------------------------------------------------
    //  nextWindow
    // ------------------------------------------------------------------

    @Test
    fun `glugginn opnast a naesta valda degi kl 7`() {
        // Laugardagur 15. kl. 12:00, bara virkir dagar -> manudagur 17. kl. 07:00
        val window = TriggerTimes.nextWindow(
            days = weekdays, windowHour = 7, windowMinute = 0,
            from = at(15, 12)
        )!!.asCalendar()

        assertEquals(17, window.get(Calendar.DAY_OF_MONTH))
        assertEquals(7, window.get(Calendar.HOUR_OF_DAY))
    }

    @Test
    fun `glugginn fylgir helgardogum seu their valdir`() {
        // Fostudagur 14. kl. 12:00, sunnudagur valinn med -> laugardag? nei,
        // adeins sunnudagur er helgardagurinn her -> sunnudagur 16. kl. 07:00
        val window = TriggerTimes.nextWindow(
            days = setOf(Calendar.SUNDAY), windowHour = 7, windowMinute = 0,
            from = at(14, 12)
        )!!.asCalendar()

        assertEquals(16, window.get(Calendar.DAY_OF_MONTH))
        assertEquals(Calendar.SUNDAY, window.get(Calendar.DAY_OF_WEEK))
    }

    @Test
    fun `glugginn med toma daga skilar null - EKKI varatima`() {
        // Villan sem hvatti til profanna: eldri utgafa skiladi "nuna + 24 klst"
        // her, sem skradi glugga a reki um klukkuna dag eftir dag.
        assertNull(
            TriggerTimes.nextWindow(
                days = emptySet(), windowHour = 7, windowMinute = 0,
                from = at(12, 6)
            )
        )
    }
}
