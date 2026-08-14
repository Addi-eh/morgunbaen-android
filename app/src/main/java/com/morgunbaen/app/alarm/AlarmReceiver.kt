package com.morgunbaen.app.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.morgunbaen.app.data.Prefs

/**
 * Tekur við þegar klukkan hringir.
 *
 * Verdur ad vera FLJOTUR - Android gefur BroadcastReceiver bara nokkrar sekundur.
 * Tess vegna gerir hann adeins tvennt: raesir tjonustuna og skrair naesta dag.
 */
class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_FIRE) return

        Log.i(TAG, "Vekjari hringdi")

        // Skra ad vekjarinn hafi raunverulega komist a. Tetta er
        // sonnunargagnid sem heilsuvoktunin byggir a - an tess vaeri
        // engin leid ad vita hvort siminn hefdi stodvad appid.
        val prefs = Prefs(context)
        prefs.lastAlarmFiredMillis = System.currentTimeMillis()

        // Þessi hringing gæti verið blundur. Hreinsum hann áður en næsti
        // daglegi tími er skráður, annars endurheimtir schedule() hann.
        AlarmScheduler.cancelSnooze(context)

        // Raesa spilun i forgrunnstjonustu.
        // Android leyfir tetta ur bakgrunni tegar tad kemur fra setAlarmClock.
        val serviceIntent = Intent(context, AlarmService::class.java).apply {
            action = AlarmService.ACTION_START
        }
        context.startForegroundService(serviceIntent)

        // Skra strax naesta vekjara. Ef tetta gleymist hringir hann
        // aldrei aftur - algengasta villan i heimasmiduðum vekjurum.
        AlarmScheduler.schedule(context)
    }

    companion object {
        private const val TAG = "AlarmReceiver"
        const val ACTION_FIRE = "com.morgunbaen.app.FIRE_ALARM"
    }
}
