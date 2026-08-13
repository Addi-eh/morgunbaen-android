package com.morgunbaen.app.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.morgunbaen.app.work.SyncWorker

/**
 * Skrair vekjarann aftur eftir endurraesingu.
 *
 * Android hendir OLLUM skradum vekjurum tegar siminn endurraesist.
 * An tessa myndi appid taegja i fyrsta sinn sem einhver endurraesir simann
 * - nakvaemlega sama vandamal og Termux:Boot leysir i gomlu uppsetningunni.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED -> {
                Log.i(TAG, "Skrái vekjara aftur eftir ${intent.action}")
                AlarmScheduler.schedule(context)
                SyncWorker.schedule(context)
            }
        }
    }

    companion object {
        private const val TAG = "BootReceiver"
    }
}
