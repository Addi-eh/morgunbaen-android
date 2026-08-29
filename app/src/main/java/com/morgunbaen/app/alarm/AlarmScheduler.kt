package com.morgunbaen.app.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.morgunbaen.app.MainActivity
import com.morgunbaen.app.data.Prefs
import java.util.Calendar

/**
 * Sér um að skrá vekjarann hjá Android.
 *
 * Thetta er hjartad i appinu. Allt annad ma klikka - en ef tetta klikkar
 * vaknar notandinn ekki.
 */
object AlarmScheduler {

    private const val TAG = "AlarmScheduler"
    private const val REQUEST_CODE = 1001
    private const val SHOW_REQUEST_CODE = 1002
    /** Aðskilinn frá daglega vekjaranum svo blundur yfirskrifar hann ekki. */
    private const val SNOOZE_REQUEST_CODE = 1003

    /** Prófunarhringing - enn einn aðskilinn kóði af sömu ástæðu. */
    private const val TEST_REQUEST_CODE = 1004

    /**
     * Skrair naesta vekjara samkvaemt stillingum.
     * Ohaett ad kalla eins oft og tarf - eldri skraning er einfaldlega yfirskrifud.
     */
    fun schedule(context: Context) {
        val prefs = Prefs(context)
        val alarmManager = context.getSystemService(AlarmManager::class.java)

        if (!prefs.alarmEnabled) {
            cancel(context)
            return
        }

        if (!canScheduleExact(context)) {
            Log.w(TAG, "Vantar heimild fyrir nákvæma vekjara")
            return
        }

        val triggerAt = nextTriggerTime(prefs) ?: run {
            // CatchUpScheduler afskráir gluggann þegar dagar eru tómir;
            // vekjarinn verður að gera slíkt hið sama. Annars situr eldri
            // hringing eftir og vekur fólk á degi sem það tók af.
            Log.w(TAG, "Enginn dagur valinn - ekkert skráð")
            cancel(context)
            return
        }

        // setAlarmClock er sterkasta timasetningin sem Android bydur.
        // Hun kemst i gegnum Doze og orkusparnad - olikt set() og setExact().
        // Kerfid synir lika vekjaratakn i stodustikunni, sem notendur treysta.
        val info = AlarmManager.AlarmClockInfo(triggerAt, showIntent(context))
        alarmManager.setAlarmClock(info, firePendingIntent(context))

        // Ekki yfirskrifa liðinn óhringdan tíma. Application.onCreate kallar
        // schedule() við hverja ræsingu og myndi annars færa merkið yfir á
        // morgundaginn áður en heilsuvöktunin nær að lesa klikkið.
        val previous = prefs.lastScheduledTriggerMillis
        val lastFired = prefs.lastAlarmFiredMillis
        val unfiredPast = previous > 0L &&
            previous <= System.currentTimeMillis() &&
            lastFired + 5 * 60_000L < previous
        if (!unfiredPast) {
            prefs.lastScheduledTriggerMillis = triggerAt
        }

        Log.i(TAG, "Vekjari skráður: ${Calendar.getInstance().apply { timeInMillis = triggerAt }.time}")

        // Blundur er sjálfstæður vekjari. Ef hann er enn í bið - t.d. eftir
        // endurræsingu eða stillingabreytingu - má daglegi tíminn ekki éta hann.
        restoreSnooze(context, prefs, alarmManager)
    }

    fun cancel(context: Context) {
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        alarmManager.cancel(firePendingIntent(context))
        Prefs(context).lastScheduledTriggerMillis = 0L
        cancelSnooze(context)
        Log.i(TAG, "Vekjari afskráður")
    }

    /**
     * Skrair blund - stakur vekjari eftir X minutur.
     *
     * Notar annan PendingIntent en daglegi vekjarinn, svo morgundagurinn
     * situr áfram skráður. Tíminn er vistaður í Prefs til að lifa af
     * ræsingu og persistAndReschedule.
     */
    fun scheduleSnooze(context: Context, minutes: Int) {
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        if (!canScheduleExact(context)) return

        val triggerAt = System.currentTimeMillis() + minutes * 60_000L
        val prefs = Prefs(context)
        prefs.snoozeUntilMillis = triggerAt

        // Blundur er lika lofud hringing. An tessa vissi heilsuvoktunin
        // ekkert um hann - siminn gat drepid appid a niu minutunum og
        // notandinn svaf yfir sig an nokkurrar vidvorunar.
        prefs.lastScheduledTriggerMillis = triggerAt
        val info = AlarmManager.AlarmClockInfo(triggerAt, showIntent(context))
        alarmManager.setAlarmClock(info, snoozePendingIntent(context))
        Log.i(TAG, "Blundur skráður eftir $minutes mín")
    }

    /**
     * Prófunarhringing eftir orfáar sekúndur.
     *
     * Keyrir ALLA leiðina - AlarmManager -> AlarmReceiver -> AlarmService ->
     * AlarmActivity - ekki bara spilun. Tilgangurinn er að notandinn geti
     * staðfest á 30 sekúndum að Samsung-stillingar, heimildir og hljóð
     * virki, án þess að hreyfa morguntímann eða bíða til morguns.
     *
     * Aukaverkun sem er í lagi: AlarmReceiver skráir lastAlarmFiredMillis
     * og næsta daglega vekjara eins og við alvöru hringingu. Hvort tveggja
     * er einmitt það sem próf á að æfa.
     */
    fun scheduleTest(context: Context, seconds: Int = 30) {
        if (!canScheduleExact(context)) return
        val triggerAt = System.currentTimeMillis() + seconds * 1000L
        val info = AlarmManager.AlarmClockInfo(triggerAt, showIntent(context))
        context.getSystemService(AlarmManager::class.java)
            .setAlarmClock(info, testPendingIntent(context))
        Log.i(TAG, "Prófunarhringing eftir $seconds sek")
    }

    /** Afskráir blund og hreinsar vistaðan tíma. */
    fun cancelSnooze(context: Context) {
        Prefs(context).snoozeUntilMillis = 0L
        context.getSystemService(AlarmManager::class.java)
            .cancel(snoozePendingIntent(context))
    }

    /**
     * Ma appid skra nakvaema vekjara?
     * Fra Android 12 tarf leyfi - en foor sem lysa sig vekjaraklukkur
     * (USE_EXACT_ALARM i manifest) fa tad sjalfkrafa fra Android 13.
     */
    fun canScheduleExact(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        return context.getSystemService(AlarmManager::class.java).canScheduleExactAlarms()
    }

    /**
     * Naesti timi sem vekjarinn a ad hringja, i millisekundum.
     * Reikningurinn sjalfur byr i TriggerTimes - hreinni einingu sem
     * einingaprof na utan sima. Skilar null ef enginn dagur er valinn.
     */
    fun nextTriggerTime(prefs: Prefs, from: Calendar = Calendar.getInstance()): Long? {
        val skip = prefs.skipNextMillis
        val now = from.timeInMillis
        if (skip > 0L && skip <= now) {
            prefs.skipNextMillis = 0L
        }
        return TriggerTimes.next(
            days = prefs.alarmDays,
            hour = prefs.alarmHour,
            minute = prefs.alarmMinute,
            weekendEnabled = prefs.weekendTimeEnabled,
            weekendHour = prefs.weekendHour,
            weekendMinute = prefs.weekendMinute,
            from = from,
            skipMillis = prefs.skipNextMillis
        )
    }

    /**
     * Sidasti timi sem vekjarinn ATTI ad hringja. Heilsuvoktunin notar
     * tetta til ad greina hvort siminn hafi stodvad appid.
     */
    fun previousTriggerTime(prefs: Prefs, from: Calendar = Calendar.getInstance()): Long? =
        TriggerTimes.previous(
            days = prefs.alarmDays,
            hour = prefs.alarmHour,
            minute = prefs.alarmMinute,
            weekendEnabled = prefs.weekendTimeEnabled,
            weekendHour = prefs.weekendHour,
            weekendMinute = prefs.weekendMinute,
            from = from
        )

    /**
     * Endurskráir blund ef hann er enn í framtíðinni.
     * Úreltur blundur er hreinsaður svo heilsuvöktunin ruglist ekki.
     */
    private fun restoreSnooze(
        context: Context,
        prefs: Prefs,
        alarmManager: AlarmManager
    ) {
        val snoozeAt = prefs.snoozeUntilMillis
        if (snoozeAt <= 0L) return
        if (snoozeAt <= System.currentTimeMillis()) {
            prefs.snoozeUntilMillis = 0L
            alarmManager.cancel(snoozePendingIntent(context))
            return
        }
        val info = AlarmManager.AlarmClockInfo(snoozeAt, showIntent(context))
        alarmManager.setAlarmClock(info, snoozePendingIntent(context))
        // Blundurinn er fyrsta lofada hringingin - merkid a ad benda a hann,
        // ekki morgundaginn sem schedule() var ad skra.
        prefs.lastScheduledTriggerMillis = snoozeAt
        Log.i(TAG, "Blundur endurskráður: ${Calendar.getInstance().apply { timeInMillis = snoozeAt }.time}")
    }

    /** Tetta er sent tegar klukkan hringir. */
    private fun firePendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_FIRE
        }
        return PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    /** Sérstakur PendingIntent - annar request-kóði en daglegi vekjarinn. */
    private fun snoozePendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_FIRE
        }
        return PendingIntent.getBroadcast(
            context,
            SNOOZE_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    /** Sami ACTION_FIRE - prófið á að fara nákvæmlega sömu leið. */
    private fun testPendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_FIRE
        }
        return PendingIntent.getBroadcast(
            context,
            TEST_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    /** Tetta opnast ef notandinn ytir a vekjaratáknið i stodustikunni. */
    private fun showIntent(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java)
        return PendingIntent.getActivity(
            context,
            SHOW_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
