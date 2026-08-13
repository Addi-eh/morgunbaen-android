package com.morgunbaen.app.work

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
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
 * Thetta er lykilmunurinn a appinu og Termux-uppsetningunni: i stad tess ad reyna
 * NAKVAEMLEGA einu sinni kl. 06:55 - og tapa deginum ef netid var nidri eda
 * siminn slokktur - taa reynum vid nokkrum sinnum yfir daginn.
 * Android radar teim tilraunum sjalft tegar tad hentar rafhlodunni.
 */
class SyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val repository = EpisodeRepository(applicationContext)

        when (val result = repository.sync()) {
            is EpisodeRepository.SyncResult.Downloaded -> {
                Log.i(TAG, "Sótti bæn: ${result.episode.title}")
                Result.success()
            }
            is EpisodeRepository.SyncResult.AlreadyHave -> {
                Log.i(TAG, "Bænin var þegar til staðar")
                Result.success()
            }
            is EpisodeRepository.SyncResult.StreamOnly -> {
                Log.i(TAG, "Streymi skráð: ${result.episode.title}")
                Result.success()
            }
            is EpisodeRepository.SyncResult.Failed -> {
                Log.w(TAG, "Mistókst: ${result.reason}")
                // Result.retry laetur WorkManager reyna aftur sidar
                // med vaxandi bili milli tilrauna.
                Result.retry()
            }
        }
    }

    companion object {
        private const val TAG = "SyncWorker"
        private const val WORK_NAME = "morgunbaen_sync"

        /**
         * Skrair endurtekid verk sem keyrir a ~6 klst fresti.
         * Android akvedur nakvaema timasetningu - tad er einmitt tad sem
         * gerir tetta rafhlodueyd og areidanlegt.
         */
        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = PeriodicWorkRequestBuilder<SyncWorker>(6, TimeUnit.HOURS)
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }
}
