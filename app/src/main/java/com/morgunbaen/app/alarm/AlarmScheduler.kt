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
            Log.w(TAG, "Enginn dagur valinn - ekkert skráð")
            return
        }

        // setAlarmClock er sterkasta timasetningin sem Android bydur.
        // Hun kemst i gegnum Doze og orkusparnad - olikt set() og setExact().
        // Kerfid synir lika vekjaratakn i stodustikunni, sem notendur treysta.
        val info = AlarmManager.AlarmClockInfo(triggerAt, showIntent(context))
        alarmManager.setAlarmClock(info, firePendingIntent(context))

        Log.i(TAG, "Vekjari skráður: ${Calendar.getInstance().apply { timeInMillis = triggerAt }.time}")
    }

    fun cancel(context: Context) {
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        alarmManager.cancel(firePendingIntent(context))
        Log.i(TAG, "Vekjari afskráður")
    }

    /**
     * Skrair blund - stakur vekjari eftir X minutur, an tess ad snerta
     * hina foostu daglegu skraningu.
     */
    fun scheduleSnooze(context: Context, minutes: Int) {
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        if (!canScheduleExact(context)) return

        val triggerAt = System.currentTimeMillis() + minutes * 60_000L
        val info = AlarmManager.AlarmClockInfo(triggerAt, showIntent(context))
        alarmManager.setAlarmClock(info, firePendingIntent(context))
        Log.i(TAG, "Blundur skráður eftir $minutes mín")
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
     * Skilar null ef enginn dagur er valinn.
     */
    fun nextTriggerTime(prefs: Prefs, from: Calendar = Calendar.getInstance()): Long? {
        val days = prefs.alarmDays
        if (days.isEmpty()) return null

        // Leitum ad naesta gilda degi - i dag eda innan viku.
        for (offset in 0..7) {
            val candidate = (from.clone() as Calendar).apply {
                add(Calendar.DAY_OF_YEAR, offset)
                set(Calendar.HOUR_OF_DAY, prefs.alarmHour)
                set(Calendar.MINUTE, prefs.alarmMinute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            val dayOfWeek = candidate.get(Calendar.DAY_OF_WEEK)
            if (dayOfWeek !in days) continue
            // Timinn i dag getur verid lidinn hja.
            if (candidate.timeInMillis <= from.timeInMillis) continue

            return candidate.timeInMillis
        }
        return null
    }

    /**
     * Sidasti timi sem vekjarinn ATTI ad hringja, i millisekundum.
     * Notad til ad greina hvort siminn hafi stodvad appid: ef tessi timi er
     * lidinn en vekjarinn hringdi aldrei, tha var eitthvad ad.
     * Skilar null ef enginn dagur er valinn.
     */
    fun previousTriggerTime(prefs: Prefs, from: Calendar = Calendar.getInstance()): Long? {
        val days = prefs.alarmDays
        if (days.isEmpty()) return null

        for (offset in 0 downTo -7) {
            val candidate = (from.clone() as Calendar).apply {
                add(Calendar.DAY_OF_YEAR, offset)
                set(Calendar.HOUR_OF_DAY, prefs.alarmHour)
                set(Calendar.MINUTE, prefs.alarmMinute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            val dayOfWeek = candidate.get(Calendar.DAY_OF_WEEK)
            if (dayOfWeek !in days) continue
            if (candidate.timeInMillis > from.timeInMillis) continue

            return candidate.timeInMillis
        }
        return null
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
