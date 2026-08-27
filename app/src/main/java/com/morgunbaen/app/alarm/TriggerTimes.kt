package com.morgunbaen.app.alarm

import java.util.Calendar

/**
 * Hreinn tímareikningur vekjarans — engin Android-tenging.
 *
 * Þetta er hjartað úr AlarmScheduler og CatchUpScheduler, dregið út í föll
 * sem taka bara gildi og skila gildi. Ástæðan er ekki snyrtimennska heldur
 * prófanleiki: villan þar sem tómir vekjaradagar settu sóknargluggann á rek
 * hefði aldrei komist í gegnum einfaldasta einingapróf — en reikningurinn
 * var læstur inni í hlutum sem þurftu SharedPreferences til að keyra.
 *
 * AlarmScheduler og CatchUpScheduler eiga að vera þunnar umbúðir um þetta.
 */
object TriggerTimes {

    /**
     * Næsti tími sem vekjarinn á að hringja, í millisekúndum.
     * Skilar null ef enginn dagur er valinn.
     *
     * @param days           Valdir dagar, Calendar.SUNDAY=1 .. SATURDAY=7
     * @param hour,minute    Venjulegi vekjaratíminn
     * @param weekendEnabled Gildir annar tími um helgar?
     * @param weekendHour,weekendMinute  Helgartíminn, ef virkur
     */
    fun next(
        days: Set<Int>,
        hour: Int,
        minute: Int,
        weekendEnabled: Boolean,
        weekendHour: Int,
        weekendMinute: Int,
        from: Calendar = Calendar.getInstance()
    ): Long? {
        if (days.isEmpty()) return null

        for (offset in 0..7) {
            val candidate = (from.clone() as Calendar).apply {
                add(Calendar.DAY_OF_YEAR, offset)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            val dayOfWeek = candidate.get(Calendar.DAY_OF_WEEK)
            if (dayOfWeek !in days) continue

            applyTime(candidate, dayOfWeek, hour, minute, weekendEnabled, weekendHour, weekendMinute)

            // Tíminn í dag getur verið liðinn hjá.
            if (candidate.timeInMillis <= from.timeInMillis) continue

            return candidate.timeInMillis
        }
        return null
    }

    /**
     * Síðasti tími sem vekjarinn ÁTTI að hringja.
     * Skilar null ef enginn dagur er valinn.
     */
    fun previous(
        days: Set<Int>,
        hour: Int,
        minute: Int,
        weekendEnabled: Boolean,
        weekendHour: Int,
        weekendMinute: Int,
        from: Calendar = Calendar.getInstance()
    ): Long? {
        if (days.isEmpty()) return null

        for (offset in 0 downTo -7) {
            val candidate = (from.clone() as Calendar).apply {
                add(Calendar.DAY_OF_YEAR, offset)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            val dayOfWeek = candidate.get(Calendar.DAY_OF_WEEK)
            if (dayOfWeek !in days) continue

            applyTime(candidate, dayOfWeek, hour, minute, weekendEnabled, weekendHour, weekendMinute)

            if (candidate.timeInMillis > from.timeInMillis) continue

            return candidate.timeInMillis
        }
        return null
    }

    /**
     * Næsta opnun sóknargluggans: kl. windowHour:windowMinute á næsta degi
     * sem er í days.
     *
     * Skilar null ef enginn dagur er valinn — EKKI varatíma. Eldri útgáfa
     * féll aftur á "núna + 24 klst", sem setti gluggann á rek: hann opnaðist
     * daglega á tíma sem færðist með klukkunni í stað 07:00, og kveikti
     * fimm mínútna leitarlotur á tilviljanakenndum tímum sólarhringsins.
     */
    fun nextWindow(
        days: Set<Int>,
        windowHour: Int,
        windowMinute: Int,
        from: Calendar = Calendar.getInstance()
    ): Long? {
        if (days.isEmpty()) return null

        for (offset in 0..7) {
            val candidate = (from.clone() as Calendar).apply {
                add(Calendar.DAY_OF_YEAR, offset)
                set(Calendar.HOUR_OF_DAY, windowHour)
                set(Calendar.MINUTE, windowMinute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            if (candidate.get(Calendar.DAY_OF_WEEK) !in days) continue
            if (candidate.timeInMillis <= from.timeInMillis) continue

            return candidate.timeInMillis
        }
        return null
    }

    /**
     * Hversu langt er þangað til klukkan er `toMillis`, sundurliðað í daga,
     * klukkustundir og mínútur. Skilar null sé tíminn liðinn hjá.
     *
     * Mínúturnar eru NÁMUNDAÐAR UPP: standi 6 mín og 20 sek eftir segir
     * teljarinn „7 mín". Annars sæti hann á „0 mín" heila mínútu áður en
     * vekjarinn hringir — og teljari sem segir núll en hringir ekki er
     * verri en enginn teljari.
     */
    fun countdown(fromMillis: Long, toMillis: Long): Countdown? {
        val diff = toMillis - fromMillis
        if (diff <= 0L) return null

        val totalMinutes = ((diff + MINUTE_MILLIS - 1) / MINUTE_MILLIS).toInt()
        return Countdown(
            days = totalMinutes / MINUTES_PER_DAY,
            hours = (totalMinutes % MINUTES_PER_DAY) / 60,
            minutes = totalMinutes % 60
        )
    }

    /** Sundurliðaður biðtími — dagar, klukkustundir og mínútur. */
    data class Countdown(val days: Int, val hours: Int, val minutes: Int)

    private const val MINUTE_MILLIS = 60_000L
    private const val MINUTES_PER_DAY = 24 * 60

    private fun applyTime(
        calendar: Calendar,
        dayOfWeek: Int,
        hour: Int,
        minute: Int,
        weekendEnabled: Boolean,
        weekendHour: Int,
        weekendMinute: Int
    ) {
        val isWeekend = dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY
        val useWeekend = isWeekend && weekendEnabled
        calendar.set(Calendar.HOUR_OF_DAY, if (useWeekend) weekendHour else hour)
        calendar.set(Calendar.MINUTE, if (useWeekend) weekendMinute else minute)
    }
}
