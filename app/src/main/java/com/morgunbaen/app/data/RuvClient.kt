package com.morgunbaen.app.data

import android.util.Log
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Einn thattur af "Morgunbaen og ord dagsins".
 *
 * @param id          Audkenni thattarins hja RUV (t.d. "bme3rd")
 * @param title       Titill, oftast nafn prestsins
 * @param firstrun    Hvenaer thatturinn var fluttur, ISO 8601: "2026-08-12T06:55:00"
 * @param fileUrl     Slod a hljodskrana sjalfa
 */
data class Episode(
    val id: String,
    val title: String,
    val firstrun: String,
    val fileUrl: String
) {
    /** Satt ef slodin er HLS-streymi (.m3u8) frekar en venjuleg hljodskra. */
    val isHls: Boolean
        get() = fileUrl.substringBefore('?').endsWith(".m3u8", ignoreCase = true)
}

/**
 * Lettur biddlari fyrir GraphQL-vidmot RUV.
 *
 * ATH: Thetta er ekki opinberlega skjalfest vidmot. Thad getur breyst an fyrirvara.
 * Thess vegna skilar hver fall null i stad thess ad hrynja - appid daettur tha
 * aftur a sidasta thattinn sem naest i.
 */
class RuvClient {

    private val http = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    /**
     * Saekir alla thaetti thattarrada og skilar theim nyjasta.
     * Skilar null ef ekkert naest - t.d. ef netid er nidri eda RUV hefur breytt vidmotinu.
     */
    fun fetchLatestEpisode(programId: Int = MORGUNBAEN_PROGRAM_ID): Episode? {
        return try {
            val episodes = fetchEpisodes(programId)
            // firstrun er ISO 8601 ("2026-08-12T06:55:00") svo einfaldur
            // strengjasamanburdur radar rett i timarod.
            episodes.maxByOrNull { it.firstrun }
        } catch (e: Exception) {
            Log.w(TAG, "Naadi ekki i thaetti fra RUV", e)
            null
        }
    }

    /** Saekir alla adgengilega thaetti. Nyjasti er ekki endilega fremstur i listanum. */
    fun fetchEpisodes(programId: Int = MORGUNBAEN_PROGRAM_ID): List<Episode> {
        val query = """
            query getEpisode(${'$'}programID: Int!) {
              Program(id: ${'$'}programID) {
                title
                episodes {
                  title
                  id
                  firstrun
                  description
                  file
                }
              }
            }
        """.trimIndent()

        val payload = JSONObject().apply {
            put("operationName", "getEpisode")
            put("variables", JSONObject().put("programID", programId))
            put("query", query)
        }

        val request = Request.Builder()
            .url(BASE_URL)
            .post(payload.toString().toRequestBody(JSON))
            .header("content-type", "application/json")
            .header("Referer", "https://www.ruv.is/utvarp")
            .header("Origin", "https://www.ruv.is")
            .build()

        http.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                Log.w(TAG, "RUV svaradi med kota ${response.code}")
                return emptyList()
            }

            val body = response.body?.string() ?: return emptyList()
            val root = JSONObject(body)

            if (root.has("errors")) {
                Log.w(TAG, "GraphQL-villa: ${root.get("errors")}")
                return emptyList()
            }

            val program = root.optJSONObject("data")?.optJSONObject("Program")
                ?: return emptyList()
            val array = program.optJSONArray("episodes") ?: return emptyList()

            val result = mutableListOf<Episode>()
            for (i in 0 until array.length()) {
                val item = array.optJSONObject(i) ?: continue
                val file = item.optString("file", "")
                if (file.isBlank()) continue
                result += Episode(
                    id = item.optString("id", ""),
                    title = item.optString("title", "Morgunbæn"),
                    firstrun = item.optString("firstrun", ""),
                    fileUrl = file
                )
            }
            return result
        }
    }

    companion object {
        private const val TAG = "RuvClient"
        private const val BASE_URL = "https://spilari.nyr.ruv.is/gql/"
        private val JSON = "application/json; charset=utf-8".toMediaType()

        /** "Morgunbæn og orð dagsins" a Ras 1. */
        const val MORGUNBAEN_PROGRAM_ID = 25329

        /** Slod a thattinn i Spilara RUV - notud tegar baenin er deilt. */
        fun episodeWebUrl(episodeId: String): String =
            "https://www.ruv.is/utvarp/spila/morgunbaen-og-ord-dagsins/" +
                "$MORGUNBAEN_PROGRAM_ID/$episodeId"
    }
}
