package aeh.heimvisir.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Ein færsla í leitarsögunni.
 *
 * Hér er EKKERT um eigandann — ekki nafn, ekki heimilisfang, ekki sími,
 * ekki netfang. Sagan er til svo notandinn geti flett upp sama merki
 * aftur án þess að slá það inn, ekki til að safna upplýsingum um fólk.
 * Það sem vantar hér er jafn mikilvægt og það sem er.
 */
@Serializable
data class HistoryItem(
    val tagNumber: String,
    val found: Boolean,
    val name: String? = null,
    val species: String? = null,
    val at: Long,
)

private val Context.historyDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "leitarsaga",
)

/**
 * Nýlegar leitir, geymdar í tækinu og hvergi annars staðar.
 */
class HistoryStore(private val context: Context) {

    val items: Flow<List<HistoryItem>> =
        context.historyDataStore.data.map { prefs -> decode(prefs[KEY]) }

    /**
     * Bætir færslu fremst og hendir tvítekningu á sama merki.
     *
     * Kallandinn á að hunsa undantekningar héðan: uppfletting sem tókst
     * má aldrei falla af því að vistun sögunnar mistókst.
     */
    suspend fun add(item: HistoryItem) {
        context.historyDataStore.edit { prefs ->
            val next = buildList {
                add(item)
                addAll(decode(prefs[KEY]).filter { it.tagNumber != item.tagNumber })
            }.take(MAX_ITEMS)
            prefs[KEY] = json.encodeToString(next)
        }
    }

    suspend fun clear() {
        context.historyDataStore.edit { it.remove(KEY) }
    }

    private fun decode(raw: String?): List<HistoryItem> {
        if (raw.isNullOrBlank()) return emptyList()
        // Skemmd eða úrelt saga á að hverfa hljóðlaust, ekki fella appið.
        return runCatching { json.decodeFromString<List<HistoryItem>>(raw) }
            .getOrDefault(emptyList())
    }

    companion object {
        const val MAX_ITEMS = 20
        private val KEY = stringPreferencesKey("faerslur")
        private val json = Json { ignoreUnknownKeys = true }
    }
}
