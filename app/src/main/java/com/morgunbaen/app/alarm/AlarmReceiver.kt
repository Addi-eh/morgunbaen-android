package com.morgunbaen.app.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

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
