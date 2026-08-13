package com.morgunbaen.app.data

import android.content.Context
import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Sér um að sækja nýjustu bænina og geyma hana á tækinu.
 *
 * Grunnreglan: vid viljum ALLTAF eiga eitthvad ad spila kl. 7 ad morgni.
 * Tess vegna er gamla skrain adeins fjarlaegd tegar ny er komin heil i hus.
 */
class EpisodeRepository(private val context: Context) {

    private val prefs = Prefs(context)
    private val client = RuvClient()

    private val http = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    /** Mappa undir gogn appsins tar sem baenirnar eru geymdar. */
    private val audioDir: File
        get() = File(context.filesDir, "baenir").apply { mkdirs() }

    sealed class SyncResult {
        /** Ny baen naadist og er tilbuin. */
        data class Downloaded(val episode: Episode) : SyncResult()

        /** Vid attum tennan thatt nu tegar. */
        data class AlreadyHave(val episodeId: String) : SyncResult()

        /**
         * Thatturinn er HLS-streymi svo ekki var haegt ad geyma hann.
         * Appid streymir honum tha tegar vekjarinn hringir.
         */
        data class StreamOnly(val episode: Episode) : SyncResult()

        /** Ekkert naadist. Gamla baenin (ef til) er enn til stadar. */
        data class Failed(val reason: String) : SyncResult()
    }

    /**
     * Adalfallid. Kallad ur SyncWorker nokkrum sinnum a dag.
     * Ma ekki keyra a adalthraedinum.
     */
    fun sync(): SyncResult {
        val episode = client.fetchLatestEpisode()
            ?: return SyncResult.Failed("Náði ekki sambandi við RÚV")

        // Eigum vid tennan thatt nu tegar? Tha er ekkert ad gera.
        val existing = prefs.cachedFilePath?.let { File(it) }
        if (prefs.cachedEpisodeId == episode.id && existing != null && existing.exists()) {
            return SyncResult.AlreadyHave(episode.id)
        }

        // HLS-streymi er ekki haegt ad vista sem eina skra an ffmpeg.
        // Vid geymum tha slodina og streymum i staddinn.
        if (episode.isHls) {
            Log.i(TAG, "Thatturinn er HLS - streymi verdur notad")
            prefs.cachedStreamUrl = episode.fileUrl
            prefs.cachedFilePath = null
            saveMetadata(episode)
            return SyncResult.StreamOnly(episode)
        }

        // Venjuleg hljodskra - hladum henni nidur.
        // Haldum upprunalegu endingunni (.mp3) - tad audveldar ExoPlayer lifid.
        val extension = episode.fileUrl.substringBefore('?')
            .substringAfterLast('.', "mp3")
        val target = File(audioDir, "baen_${episode.id}.$extension")
        val temp = File(audioDir, "baen_${episode.id}.part")

        return try {
            downloadTo(episode.fileUrl, temp)

            if (temp.length() < MIN_VALID_BYTES) {
                temp.delete()
                return SyncResult.Failed("Skráin sem barst var of lítil")
            }

            // Skiptum bara ut tegar nyja skrain er heil.
            temp.renameTo(target)

            val oldPath = prefs.cachedFilePath
            prefs.cachedFilePath = target.absolutePath
            prefs.cachedStreamUrl = null
            saveMetadata(episode)

            // Nu er ohaett ad henda teirri gomlu.
            oldPath?.let { path ->
                val old = File(path)
                if (old.exists() && old.absolutePath != target.absolutePath) old.delete()
            }
            cleanUpOrphans(target)

            SyncResult.Downloaded(episode)
        } catch (e: Exception) {
            Log.w(TAG, "Nidurhal mistokst", e)
            temp.delete()
            SyncResult.Failed(e.message ?: "Óþekkt villa við niðurhal")
        }
    }

    private fun saveMetadata(episode: Episode) {
        prefs.cachedEpisodeId = episode.id
        prefs.cachedTitle = episode.title
        prefs.cachedFirstrun = episode.firstrun
        prefs.lastSyncMillis = System.currentTimeMillis()
    }

    private fun downloadTo(url: String, destination: File) {
        val request = Request.Builder().url(url).build()
        http.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IllegalStateException("Niðurhal svaraði með kóða ${response.code}")
            }
            val body = response.body ?: throw IllegalStateException("Tómt svar")
            destination.outputStream().use { out ->
                body.byteStream().copyTo(out)
            }
        }
    }

    /** Hendir gomlum skram sem eru ekki lengur i notkun. */
    private fun cleanUpOrphans(keep: File) {
        audioDir.listFiles()?.forEach { file ->
            if (file.absolutePath != keep.absolutePath) file.delete()
        }
    }

    /**
     * Hvad a ad spila tegar vekjarinn hringir?
     * Skilar null ef ekkert er til - tha spilar AlarmService varahljod simans.
     */
    fun playbackSource(): PlaybackSource? {
        val path = prefs.cachedFilePath
        if (path != null) {
            val file = File(path)
            if (file.exists() && file.length() > MIN_VALID_BYTES) {
                return PlaybackSource.LocalFile(file)
            }
        }
        val stream = prefs.cachedStreamUrl
        if (stream != null) return PlaybackSource.Stream(stream)
        return null
    }

    sealed class PlaybackSource {
        data class LocalFile(val file: File) : PlaybackSource()
        data class Stream(val url: String) : PlaybackSource()
    }

    companion object {
        private const val TAG = "EpisodeRepository"

        /** Minni skra en tetta er orugglega gollud. Baenin er ~5 min. */
        private const val MIN_VALID_BYTES = 50_000L
    }
}
