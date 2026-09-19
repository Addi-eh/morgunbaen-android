package com.morgunbaen.app.data

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Vekjarahljóð sem notandinn valdi sjálfur.
 *
 * Hljóðið er AFRITAÐ inn í geymslu appsins, ekki geymt sem slóð. Bæði
 * vekjarahljóð símans (content://media) og skrár úr skráavafranum (SAF)
 * eru ólæsileg fyrir aflæsingu eftir endurræsingu — og vekjarinn keyrir
 * einmitt þá. Sama rök og í DeviceStorage.kt: allt sem vekjarinn þarf býr
 * í device-protected geymslu.
 *
 * Afritun kemur líka í veg fyrir að vekjarinn þegi ef notandinn eyðir
 * upprunalegu skránni eða færir hana.
 */
class AlarmSoundStore(private val context: Context) {

    private val prefs = Prefs(context)

    private val dir: File
        get() = File(context.deviceStorage.filesDir, "vekjarahljod").apply { mkdirs() }

    sealed interface ImportResult {
        data object Ok : ImportResult
        data object TooLarge : ImportResult
        data object NotAudio : ImportResult
        data class Failed(val reason: String) : ImportResult
    }

    /** Valda hljóðið, eða null ef kirkjuklukkan gildir (sjálfgefið). */
    fun file(): File? {
        val path = prefs.alarmSoundPath ?: return null
        return File(path).takeIf { it.exists() && it.length() > 0L }
    }

    /**
     * Afritar hljóðið og tekur það í notkun. Fyrra val helst óbreytt
     * ef eitthvað klikkar — hálfafrituð skrá má aldrei verða vekjarahljóð.
     */
    suspend fun importFrom(uri: Uri, title: String): ImportResult = withContext(Dispatchers.IO) {
        val target = File(dir, "hljod_${System.currentTimeMillis()}")
        val temp = File(dir, "${target.name}.part")
        try {
            val input = context.contentResolver.openInputStream(uri)
                ?: return@withContext ImportResult.Failed("opnaðist ekki")
            var tooLarge = false
            input.use { source ->
                temp.outputStream().use { sink ->
                    val buffer = ByteArray(64 * 1024)
                    var total = 0L
                    while (true) {
                        val read = source.read(buffer)
                        if (read < 0) break
                        total += read
                        if (total > MAX_BYTES) {
                            tooLarge = true
                            break
                        }
                        sink.write(buffer, 0, read)
                    }
                }
            }
            if (tooLarge) {
                temp.delete()
                return@withContext ImportResult.TooLarge
            }
            if (!hasAudio(temp)) {
                temp.delete()
                return@withContext ImportResult.NotAudio
            }
            if (!temp.renameTo(target)) {
                temp.delete()
                return@withContext ImportResult.Failed("gat ekki vistað")
            }
            prefs.alarmSoundPath = target.absolutePath
            prefs.alarmSoundTitle = title
            removeAllExcept(target)
            ImportResult.Ok
        } catch (e: Exception) {
            Log.w(TAG, "Afritun vekjarahljóðs mistókst", e)
            temp.delete()
            ImportResult.Failed(e.message ?: e.javaClass.simpleName)
        }
    }

    /** Aftur í kirkjuklukku Staðarfells. */
    fun clear() {
        prefs.alarmSoundPath = null
        prefs.alarmSoundTitle = null
        removeAllExcept(null)
    }

    /**
     * Mynd eða PDF með .mp3-endingu á ekki að verða vekjari sem þegir.
     * Spilarinn félli þá á kirkjuklukkuna — en betra er að segja strax nei.
     */
    private fun hasAudio(file: File): Boolean {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(file.absolutePath)
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_AUDIO) == "yes"
        } catch (e: Exception) {
            false
        } finally {
            try { retriever.release() } catch (_: Exception) { }
        }
    }

    private fun removeAllExcept(keep: File?) {
        dir.listFiles()?.forEach { if (it.absolutePath != keep?.absolutePath) it.delete() }
    }

    companion object {
        private const val TAG = "AlarmSoundStore"

        /** Vekjarahljóð, ekki plata. Kemur í veg fyrir að klukkutíma mp3 fylli geymsluna. */
        const val MAX_BYTES = 20L * 1024 * 1024
    }
}
