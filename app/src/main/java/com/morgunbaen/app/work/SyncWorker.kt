package com.morgunbaen.app.work

import android.content.Context
import android.util.Log
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.morgunbaen.app.data.EpisodeRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

/**
 * Saekir nyjustu baenina i bakgrunni.
 *
 * Tvaer adferdir vinna saman:
 *
 *   SOKNARGLUGGI   Fra kl. 07:00 a vekjaradogum notandans - strax og
 *                  dagskrarlidnum lykur - er reynt a fimm minutna fresti
 *                  tangad til thattur dagsins finnst. Haett um leid og hann
 *                  er kominn.
 *
 *   ORYGGISNET     Reglubundin sokn a 6 klst fresti, alla daga. Griipur tad
 *                  sem soknarglugginn missti af: siminn var slokktur,
 *                  netlaust, eda RUV birti thattinn seint.
 *
 * Tetta er lykilmunurinn a appinu og Termux-uppsetningunni: i stad tess ad reyna
 * NAKVAEMLEGA einu sinni kl. 06:55 - og tapa deginum ef netid var nidri - tha
 * heldur appid afram ad reyna tangad til tad tekst.
 */
class SyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val repository = EpisodeRepository(applicationContext)
        val attempt = inputData.getInt(KEY_ATTEMPT, NOT_IN_WINDOW)

        val result = repository.sync()

        // Frettirnar eru sottar i somu ferd. Their eru sjalfstaedar: mistakist
        // taer breytir tad engu um baenina, sem er adalatridid.
        repository.syncNews()

        when (result) {
            is EpisodeRepository.SyncResult.Downloaded ->
                Log.i(TAG, "Sótti bæn: ${result.episode.title}")
            is EpisodeRepository.SyncResult.AlreadyHave ->
                Log.i(TAG, "Bænin var þegar til staðar")
            is EpisodeRepository.SyncResult.StreamOnly ->
                Log.i(TAG, "Streymi skráð: ${result.episode.title}")
            is EpisodeRepository.SyncResult.Failed ->
                Log.w(TAG, "Mistókst: ${result.reason}")
        }

        // Utan soknarglugga: venjuleg hegdun. WorkManager reynir sjalft aftur
        // sidar med vaxandi bili ef tetta mistokst.
        if (attempt == NOT_IN_WINDOW) {
            return@withContext if (result is EpisodeRepository.SyncResult.Failed) {
                Result.retry()
            } else {
                Result.success()
            }
        }

        // Innan soknarglugga: vid erum ad bida eftir THAETTI DAGSINS.
        // Tad dugar ekki ad eiga gaerdagsins - tha er ekkert unnid.
        // Vid lokum EKKI glugganum tott baenin se komin - frettirnar geta
        // birst sidar. Se notandinn ekki med frettir valdar dugar baenin ein
        // og tetta skilar satt strax.
        if (repository.isDailyContentComplete()) {
            Log.i(TAG, "Efni dagsins komið - sóknarglugga lokað")
            return@withContext Result.success()
        }

        if (attempt >= MAX_ATTEMPTS) {
            // Gefumst upp a glugganum. Oryggisnetid heldur afram ad reyna
            // yfir daginn, svo tetta er ekki endanlegt.
            Log.w(TAG, "Þáttur dagsins fannst ekki eftir $attempt tilraunir")
            return@withContext Result.success()
        }

        Log.i(TAG, "Þáttur dagsins ekki kominn - reyni aftur eftir $RETRY_MINUTES mín")
        scheduleWindowAttempt(applicationContext, attempt + 1)
        Result.success()
    }

    companion object {
        private const val TAG = "SyncWorker"
        private const val PERIODIC_WORK = "morgunbaen_sync"
        private const val WINDOW_WORK = "morgunbaen_sokn"

        const val KEY_ATTEMPT = "attempt"
        private const val NOT_IN_WINDOW = -1

        /** Bil milli tilrauna innan soknarglugga. */
        const val RETRY_MINUTES = 5L

        /**
         * Haemarkstilraunir. 20 x 5 min = tveir timar, fra 07:00 til 09:00.
         *
         * Baedi baenin og frettirnar eru fluttar kl. 07:00 en geta tafist
         * hja RUV, svo glugginn faer rifleg svigrum. Se ekkert komid kl. 09:00
         * er eitthvad annad ad en tof.
         */
        private const val MAX_ATTEMPTS = 20

        private val networkRequired = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        /**
         * Oryggisnetid: reglubundin sokn a 6 klst fresti, ALLA daga.
         *
         * Ad tetta keyri lika um helgar skiptir mali a Samsung: opp sem
         * ekkert gera i trja daga eru svaefd, og vekjari sem hringir bara
         * a virkum dogum er einmitt ognotadur yfir helgi.
         */
        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<SyncWorker>(6, TimeUnit.HOURS)
                .setConstraints(networkRequired)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                PERIODIC_WORK,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }

        /**
         * Ein tilraun innan soknarglugga.
         * attempt = 0 er fyrsta tilraunin kl. 07:00; sidan bidur hver tilraun
         * i fimm minutur adur en hun keyrir.
         */
        fun scheduleWindowAttempt(context: Context, attempt: Int) {
            val request = OneTimeWorkRequestBuilder<SyncWorker>()
                .setConstraints(networkRequired)
                .setInputData(Data.Builder().putInt(KEY_ATTEMPT, attempt).build())
                .setInitialDelay(
                    if (attempt == 0) 0L else RETRY_MINUTES,
                    TimeUnit.MINUTES
                )
                .setBackoffCriteria(BackoffPolicy.LINEAR, RETRY_MINUTES, TimeUnit.MINUTES)
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                WINDOW_WORK,
                ExistingWorkPolicy.REPLACE,
                request
            )
        }
    }
}
