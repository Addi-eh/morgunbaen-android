package com.morgunbaen.app.work

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.UserManager
import android.util.Log
import com.morgunbaen.app.alarm.TriggerTimes
import com.morgunbaen.app.data.Prefs
import java.util.Calendar

/**
 * Opnar soknargluggann a hverjum virkum morgni.
 *
 * "Morgunbæn og orð dagsins" er flutt kl. 06:55-07:00 a Ras 1 og birtist i
 * Spilara RUV skommu sidar. Vid byrjum ad leita kl. 07:00 og haldum afram a
 * fimm minutna fresti tangad til thattur dagsins finnst.
 */
object CatchUpScheduler {

    private const val TAG = "CatchUpScheduler"
    private const val REQUEST_CODE = 2001
    /** Aðskilinn svo endurtekning yfirskrifar ekki næsta virka morgun. */
    private const val RETRY_REQUEST_CODE = 2002

    /** Klukkan sem soknarglugginn opnast - um leid og dagskrarlidnum lykur. */
    const val WINDOW_HOUR = 7
    const val WINDOW_MINUTE = 0

    /**
     * Skrair naesta soknarglugga.
     * Ohaett ad kalla eins oft og tarf - eldri skraning er yfirskrifud.
     */
    fun schedule(context: Context) {
        val alarmManager = context.getSystemService(AlarmManager::class.java)

        // null = engir dagar valdir. Tha er ENGINN gluggi skradur - og eldri
        // skraning afskrad. Eldri utgafa fell aftur a "nuna + 24 klst" sem
        // setti gluggann a rek um allan solarhringinn.
        val triggerAt = TriggerTimes.nextWindow(
            days = Prefs(context).alarmDays,
            windowHour = WINDOW_HOUR,
            windowMinute = WINDOW_MINUTE
        )
        if (triggerAt == null) {
            alarmManager.cancel(pendingIntent(context))
            cancelRetry(context)
            Log.i(TAG, "Engir vekjaradagar - sóknarglugga sleppt")
            return
        }

        // setAndAllowWhileIdle en EKKI setExact: tetta er bakgrunnsverk, ekki
        // vekjari. Tad er ohakvaemt um nokkrar minutur en kemst i gegnum Doze
        // - og krefst engra sersstakra heimilda, olikt nakvaemum vekjurum.
        alarmManager.setAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerAt,
            pendingIntent(context)
        )

        Log.i(TAG, "Sóknargluggi skráður: ${Calendar.getInstance().apply { timeInMillis = triggerAt }.time}")
    }

    /**
     * Reynir aftur eftir stutta stund. Notað þegar síminn er enn læstur
     * kl. 07:00 - WorkManager má ekki keyra í Direct Boot, en við megum
     * ekki heldur sleppa deginum og hoppa beint í næsta virka morgun.
     */
    fun scheduleRetry(context: Context, delayMinutes: Long = 5) {
        val now = Calendar.getInstance()
        val hour = now.get(Calendar.HOUR_OF_DAY)
        if (hour >= WINDOW_HOUR + 2) {
            schedule(context)
            return
        }

        val triggerAt = System.currentTimeMillis() + delayMinutes * 60_000L
        context.getSystemService(AlarmManager::class.java).setAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerAt,
            pendingIntent(context, RETRY_REQUEST_CODE)
        )
        Log.i(TAG, "Sóknargluggi endurtekinn eftir $delayMinutes mín")
    }

    /**
     * Opnar sóknargluggann núna ef við erum inni í 07:00–09:00 á virkum degi.
     * Kallað þegar síminn er nýopnaður eftir ræsingu, svo læstur 07:00-gluggi
     * tapist ekki þó BootReceiver skrái næsta virka morgun.
     */
    fun openWindowIfDue(context: Context) {
        val now = Calendar.getInstance()
        if (now.get(Calendar.DAY_OF_WEEK) !in Prefs(context).alarmDays) return
        val hour = now.get(Calendar.HOUR_OF_DAY)
        if (hour < WINDOW_HOUR || hour >= WINDOW_HOUR + 2) return

        Log.i(TAG, "Síminn opnaður innan sóknarglugga - ræsi sókn")
        cancelRetry(context)
        SyncWorker.scheduleWindowAttempt(context, 0)
        schedule(context)
    }

    fun cancelRetry(context: Context) {
        context.getSystemService(AlarmManager::class.java)
            .cancel(pendingIntent(context, RETRY_REQUEST_CODE))
    }

    private fun pendingIntent(
        context: Context,
        requestCode: Int = REQUEST_CODE
    ): PendingIntent {
        val intent = Intent(context, CatchUpReceiver::class.java).apply {
            action = CatchUpReceiver.ACTION_OPEN_WINDOW
        }
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}

/**
 * Taka vid tegar soknarglugginn opnast kl. 07:00.
 */
class CatchUpReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_OPEN_WINDOW) return

        Log.i(TAG, "Sóknargluggi opnaður")

        val userManager = context.getSystemService(UserManager::class.java)
        if (!userManager.isUserUnlocked) {
            // WorkManager notar credential-geymslu og hrynur hér.
            // Reynum aftur eftir 5 mín í stað þess að tapa deginum.
            Log.i(TAG, "Síminn læstur - fresta sóknarglugga")
            CatchUpScheduler.scheduleRetry(context)
            return
        }

        // Fyrsta tilraun strax. SyncWorker sér sjálfur um að endurtaka
        // á fimm mínútna fresti þangað til þáttur dagsins finnst.
        CatchUpScheduler.cancelRetry(context)
        SyncWorker.scheduleWindowAttempt(context, 0)

        // Skra glugga naesta virka dags.
        CatchUpScheduler.schedule(context)
    }

    companion object {
        private const val TAG = "CatchUpReceiver"
        const val ACTION_OPEN_WINDOW = "com.morgunbaen.app.OPEN_CATCHUP_WINDOW"
    }
}
