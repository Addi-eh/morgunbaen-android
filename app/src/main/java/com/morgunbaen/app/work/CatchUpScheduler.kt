package com.morgunbaen.app.work

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
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

    /** Klukkan sem soknarglugginn opnast - um leid og dagskrarlidnum lykur. */
    const val WINDOW_HOUR = 7
    const val WINDOW_MINUTE = 0

    /**
     * Skrair naesta soknarglugga.
     * Ohaett ad kalla eins oft og tarf - eldri skraning er yfirskrifud.
     */
    fun schedule(context: Context) {
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        val triggerAt = nextWindowTime()

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
     * Naesti virki morgunn kl. 07:00.
     * Adeins manudag til fostudags - Morgunbaenin er ekki flutt um helgar,
     * svo tad vaeri tilgangslaust ad leita ta.
     */
    private fun nextWindowTime(from: Calendar = Calendar.getInstance()): Long {
        for (offset in 0..7) {
            val candidate = (from.clone() as Calendar).apply {
                add(Calendar.DAY_OF_YEAR, offset)
                set(Calendar.HOUR_OF_DAY, WINDOW_HOUR)
                set(Calendar.MINUTE, WINDOW_MINUTE)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            val day = candidate.get(Calendar.DAY_OF_WEEK)
            if (day == Calendar.SATURDAY || day == Calendar.SUNDAY) continue
            if (candidate.timeInMillis <= from.timeInMillis) continue

            return candidate.timeInMillis
        }
        // Aetti aldrei ad gerast, en betra en ad hrynja.
        return from.timeInMillis + 24 * 60 * 60 * 1000L
    }

    private fun pendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, CatchUpReceiver::class.java).apply {
            action = CatchUpReceiver.ACTION_OPEN_WINDOW
        }
        return PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
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

        // Fyrsta tilraun strax. SyncWorker sér sjálfur um að endurtaka
        // á fimm mínútna fresti þangað til þáttur dagsins finnst.
        SyncWorker.scheduleWindowAttempt(context, 0)

        // Skra glugga naesta virka dags.
        CatchUpScheduler.schedule(context)
    }

    companion object {
        private const val TAG = "CatchUpReceiver"
        const val ACTION_OPEN_WINDOW = "com.morgunbaen.app.OPEN_CATCHUP_WINDOW"
    }
}
