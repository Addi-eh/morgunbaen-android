package com.morgunbaen.app.data

import android.content.Context
import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
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
        // Sama kynning og i RuvClient - nidurhalid fer a akamaized.net
        // sem er enn liklegri til ad sia eftir User-Agent en RUV sjalft.
        .addInterceptor { chain ->
            chain.proceed(
                chain.request().newBuilder()
                    .header("User-Agent", RuvClient.USER_AGENT)
                    .build()
            )
        }
        .build()

    /**
     * Mappa undir gogn appsins tar sem baenirnar eru geymdar.
     * deviceStorage svo skrain se laesileg jafnvel tott siminn hafi endurraest
     * um nottina og enginn hafi slegid inn PIN.
     */
    private val audioDir: File
        get() = File(context.deviceStorage.filesDir, "baenir").apply { mkdirs() }

    /** Sama fyrir frettirnar - adskilin mappa svo hreinsun ruglist ekki. */
    private val newsDir: File
        get() = File(context.deviceStorage.filesDir, "frettir").apply { mkdirs() }

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
            if (!moveIntoPlace(temp, target)) {
                temp.delete()
                return SyncResult.Failed("Gat ekki vistað bænina")
            }

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

    /**
     * Færir temp-skrá á loka stað. renameTo getur klikkað yfir skráarkerfi;
     * þá afritum við. Skilar false ef hvorugt tókst - köllunaraðili má þá
     * ekki vista slóð á skrá sem er ekki til.
     */
    private fun moveIntoPlace(temp: File, target: File): Boolean {
        if (temp.renameTo(target)) return true
        return try {
            temp.copyTo(target, overwrite = true)
            temp.delete()
            target.exists() && target.length() >= MIN_VALID_BYTES
        } catch (e: Exception) {
            Log.w(TAG, "Gat ekki fært niðurhal á sinn stað", e)
            target.delete()
            false
        }
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

    // ------------------------------------------------------------------
    //  Frettir
    // ------------------------------------------------------------------

    /**
     * Saekir nyjasta frettatima DAGSINS I DAG.
     *
     * Grunnreglan er onnur en fyrir baenina: gomul baen er i lagi, gamlar
     * frettir eru tad ekki. Vid geymum tvi ALDREI frettatima fra i gaer -
     * betra ad sleppa theim en ad vekja folk med urelttum frettum.
     */
    fun syncNews(): Boolean {
        // Fyrst: henda tvi sem er ordid gamalt, hvad sem gerist naest.
        discardStaleNews()

        val today = Dates.todayIso()

        val newest = try {
            client.fetchEpisodes(RuvClient.FRETTIR_PROGRAM_ID)
                .filter { Dates.datePart(it.firstrun) == today }
                // Ekki thaettir sem eru ekki komnir - firstrun getur verid
                // framtidarskrad i dagskra.
                .filter { it.firstrun <= nowString() }
                .maxByOrNull { it.firstrun }
        } catch (e: Exception) {
            Log.w(TAG, "Náði ekki í fréttir", e)
            null
        } ?: return false

        // Eigum vid tennan nu tegar?
        val existing = prefs.newsFilePath?.let { File(it) }
        if (prefs.newsEpisodeId == newest.id && existing != null && existing.exists()) {
            return true
        }

        if (newest.isHls) {
            Log.i(TAG, "Fréttir eru HLS - sleppt")
            return false
        }

        val extension = newest.fileUrl.substringBefore('?').substringAfterLast('.', "mp3")
        val target = File(newsDir, "frettir_${newest.id}.$extension")
        val temp = File(newsDir, "frettir_${newest.id}.part")

        return try {
            downloadTo(newest.fileUrl, temp)
            if (temp.length() < MIN_VALID_BYTES) {
                temp.delete()
                return false
            }
            if (!moveIntoPlace(temp, target)) {
                temp.delete()
                return false
            }

            newsDir.listFiles()?.forEach { f ->
                if (f.absolutePath != target.absolutePath) f.delete()
            }

            prefs.newsFilePath = target.absolutePath
            prefs.newsEpisodeId = newest.id
            // Thaettirnir heita "Þáttur 224 af 365" hja RUV, sem segir
            // notandanum ekkert. Vid smidum lysandi titil ur firstrun.
            prefs.newsTitle = newsLabel(newest.firstrun)
            prefs.newsFirstrun = newest.firstrun
            Log.i(TAG, "Sótti fréttatíma: ${newest.firstrun}")
            true
        } catch (e: Exception) {
            Log.w(TAG, "Niðurhal frétta mistókst", e)
            temp.delete()
            false
        }
    }

    /**
     * Hendir frettatima sem er ekki fra i dag.
     *
     * Tetta er kallad ADUR en reynt er ad saekja nyjan - annars gaeti
     * appid spilad gaerdagsfrettir ef netid er nidri i morgunsarid.
     * Betra ad tegja en ad ljuga.
     */
    private fun discardStaleNews() {
        val stored = prefs.newsFirstrun ?: return
        if (Dates.isToday(stored)) return
        val storedDate = Dates.datePart(stored)

        Log.i(TAG, "Hendi úreltum fréttatíma frá $storedDate")
        prefs.newsFilePath?.let { File(it).delete() }
        prefs.newsFilePath = null
        prefs.newsEpisodeId = null
        prefs.newsTitle = null
        prefs.newsFirstrun = null
    }

    /** Eigum vid frettatima fra deginum i dag? Ohad tvi hvort notandinn vill hann. */
    fun hasTodaysNews(): Boolean {
        if (!Dates.isToday(prefs.newsFirstrun)) return false
        val path = prefs.newsFilePath ?: return false
        return File(path).let { it.exists() && it.length() > MIN_VALID_BYTES }
    }

    /**
     * Er allt efni dagsins komid i hus?
     *
     * Soknarglugginn notar tetta til ad vita hvenaer hann ma loka ser.
     * Frettirnar (38786) eru fluttar kl. 07:00, a somu minutu og baeninni
     * lykur - en taer geta samt birst sidar hja RUV. Glugginn verdur tvi ad
     * halda afram tar til BADAR eru komnar, ekki bara baenin.
     */
    fun isDailyContentComplete(): Boolean {
        if (!hasTodaysEpisode()) return false
        if (!prefs.newsEnabled) return true
        return hasTodaysNews()
    }

    /**
     * Frettatimi dagsins, ef hann er til og notandinn vill hann.
     * Skilar null annars - tha spilar appid einfaldlega ekki frettir.
     */
    fun newsPlaybackSource(): File? {
        if (!prefs.newsEnabled) return null

        if (!Dates.isToday(prefs.newsFirstrun)) return null

        val path = prefs.newsFilePath ?: return null
        val file = File(path)
        return if (file.exists() && file.length() > MIN_VALID_BYTES) file else null
    }

    /** "2026-08-13T07:00:00" -> "Fréttir kl. 07:00" */
    private fun newsLabel(firstrun: String): String {
        val time = Dates.timePart(firstrun)
        return if (time.isNotEmpty()) "Fréttir kl. $time" else "Fréttir"
    }

    private fun nowString(): String =
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).format(Date())

    /**
     * Eigum vid thatt dagsins i dag?
     *
     * Tetta er spurningin sem soknarglugginn byggir a: vid haettum ekki ad
     * reyna fyrr en thattur dagsins er kominn i hus, ekki bara "einhver thattur".
     */
    fun hasTodaysEpisode(): Boolean = Dates.isToday(prefs.cachedFirstrun)

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
