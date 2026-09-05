package aeh.heimvisir.net

import aeh.heimvisir.model.LookupResult
import aeh.heimvisir.model.Pet
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/**
 * Biðlari fyrir leitarviðmót Dýraauðkennis.
 *
 * ATH: `https://dyraaudkenni.is/api/MicroTags` er ÓSKJALFAÐ viðmót. Það
 * getur breyst án fyrirvara. Þess vegna kastar ekkert fall hér — hvert
 * þeirra skilar [LookupResult], og [LookupResult.MalformedResponse] er
 * merkið um að viðmótið hafi breyst.
 *
 * Fyrirspurnin fer beint úr tækinu í skrána, eins og vafri notandans
 * myndi gera. Enginn milliliður er á leiðinni og engin gögn fara annað.
 */
class DyraAudkenniApi(
    private val baseUrl: HttpUrl = DEFAULT_BASE_URL.toHttpUrl(),
    private val client: OkHttpClient = defaultClient(),
) {

    /**
     * Flettir upp einu merki. [tagNumber] á að vera þegar hreinsað með
     * `TagNumber.check`.
     *
     * Keyrir á [Dispatchers.IO]; kallandinn þarf ekki að hugsa um það.
     */
    suspend fun lookup(tagNumber: String): LookupResult = withContext(Dispatchers.IO) {
        val url = baseUrl.newBuilder()
            .addQueryParameter("tagNumber", tagNumber)
            .build()

        val request = Request.Builder()
            .url(url)
            .header("Accept", "application/json")
            .header("User-Agent", USER_AGENT)
            .build()

        val body = try {
            client.newCall(request).execute().use { response ->
                when {
                    response.code == 404 -> return@withContext LookupResult.NotFound
                    !response.isSuccessful -> {
                        // Aðeins kóðinn er skráður. Merkið sjálft og allt sem
                        // svarið ber eru persónuupplýsingar og eiga ekkert
                        // erindi í logcat.
                        Log.w(TAG, "Skráin svaraði með kóða ${response.code}")
                        return@withContext LookupResult.ServerError(response.code)
                    }
                    else -> response.body.string()
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Náði ekki sambandi við skrána", e)
            return@withContext LookupResult.NetworkError
        }

        try {
            LookupResult.Found(json.decodeFromString<Pet>(body))
        } catch (e: Exception) {
            // Hér lendum við ef viðmótið hefur breyst — eða ef skráin
            // skilar villu með kóða 200, sem hún gerir stundum (svarið er
            // þá tvíkóðað JSON, strengur sem inniheldur JSON).
            Log.w(TAG, "Svarið var ekki á því formi sem appið kann að lesa", e)
            LookupResult.MalformedResponse
        }
    }

    companion object {
        private const val TAG = "DyraAudkenniApi"

        const val DEFAULT_BASE_URL = "https://dyraaudkenni.is/api/MicroTags"

        /**
         * Sjálfgefni OkHttp-hausinn er það fyrsta sem vefþjónar loka á
         * þegar þeir taka til. Við kynnum okkur með nafni — það er bæði
         * kurteisi og lífsvon.
         */
        const val USER_AGENT = "Heimvisir-Android/0.1"

        /**
         * `ignoreUnknownKeys` er ekki værukærð heldur forsenda: bæti
         * Dýraauðkenni reit við svarið á appið að halda áfram að virka,
         * ekki að hrynja fyrir framan mann sem heldur á týndum ketti.
         */
        private val json = Json {
            ignoreUnknownKeys = true
            coerceInputValues = true
        }

        /**
         * Tímamörkin eru stutt af ásettu ráði. Notandinn horfir á
         * snúningshjól meðan á þessu stendur; betra er að segja „náði
         * ekki sambandi" eftir fimmtán sekúndur en að þegja í mínútu.
         */
        private fun defaultClient() = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .callTimeout(20, TimeUnit.SECONDS)
            .build()
    }
}
