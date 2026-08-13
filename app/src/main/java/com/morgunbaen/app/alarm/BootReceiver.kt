package com.morgunbaen.app.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.UserManager
import android.util.Log
import com.morgunbaen.app.work.CatchUpScheduler
import com.morgunbaen.app.work.SyncWorker

/**
 * Skrair vekjarann aftur eftir endurraesingu.
 *
 * Android hendir OLLUM skradum vekjurum tegar siminn endurraesist.
 * An tessa myndi appid tegja i fyrsta sinn sem einhver endurraesir simann.
 *
 * Vid hlustum a TVO raesiskeyti:
 *
 *   LOCKED_BOOT_COMPLETED  berst strax vid raesingu, adur en notandinn
 *                          slaer inn PIN. Tetta er tad sem skiptir mali:
 *                          endurraesist siminn kl. 03:00 verdur vekjarinn
 *                          skradur strax, ekki tegar einhver vaknar.
 *
 *   BOOT_COMPLETED         berst fyrst tegar siminn hefur verid opnadur.
 *                          Tha er credential-geymslan laesileg og vid getum
 *                          skrad bakgrunnssoknina lika.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_LOCKED_BOOT_COMPLETED,
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED -> {
                Log.i(TAG, "Skrái vekjara aftur eftir ${intent.action}")

                // Tetta virkar i Direct Boot - stillingarnar bua i
                // device-protected geymslu.
                AlarmScheduler.schedule(context)
                CatchUpScheduler.schedule(context)

                // WorkManager tarf credential-geymslu og getur tvi ekki
                // keyrt fyrr en siminn hefur verid opnadur.
                val userManager = context.getSystemService(UserManager::class.java)
                if (userManager.isUserUnlocked) {
                    SyncWorker.schedule(context)
                }
            }
        }
    }

    companion object {
        private const val TAG = "BootReceiver"
    }
}
