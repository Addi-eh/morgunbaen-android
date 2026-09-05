package aeh.heimvisir.ui

import aeh.heimvisir.core.TagNumber
import aeh.heimvisir.data.HistoryItem
import aeh.heimvisir.data.HistoryStore
import aeh.heimvisir.model.LookupResult
import aeh.heimvisir.net.DyraAudkenniApi
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Ástand leitarskjásins.
 *
 * [inputError] er aðskilið frá [result] af því þetta tvennt er ekki það
 * sama: ógilt innslegið merki er leiðrétting til notandans, ekki svar
 * frá skránni.
 */
data class LookupUiState(
    val tag: String = "",
    val busy: Boolean = false,
    val inputError: TagNumber.Check? = null,
    val result: LookupResult? = null,
    val lastTag: String? = null,
)

class LookupViewModel(
    application: Application,
    private val api: DyraAudkenniApi = DyraAudkenniApi(),
) : AndroidViewModel(application) {

    private val history = HistoryStore(application)

    private val _state = MutableStateFlow(LookupUiState())
    val state: StateFlow<LookupUiState> = _state.asStateFlow()

    val historyItems: StateFlow<List<HistoryItem>> = history.items.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList(),
    )

    fun onTagChanged(value: String) {
        // Villan hverfur um leið og notandinn fer að laga innsláttinn —
        // það á ekki að standa rautt undir reit sem verið er að leiðrétta.
        _state.value = _state.value.copy(tag = value, inputError = null)
    }

    /**
     * Flettir upp merkinu sem stendur í reitnum.
     *
     * Ógilt merki fær SÝNILEGA skýringu. Vefútgáfan skilaði einfaldlega
     * auðu og notandinn stóð eftir með ekkert að fara eftir.
     */
    fun lookup(raw: String = _state.value.tag) {
        if (_state.value.busy) return

        when (val check = TagNumber.check(raw)) {
            is TagNumber.Check.Valid -> runLookup(check.normalized)
            else -> _state.value = _state.value.copy(inputError = check, result = null)
        }
    }

    private fun runLookup(tagNumber: String) {
        _state.value = _state.value.copy(
            busy = true,
            inputError = null,
            result = null,
            lastTag = tagNumber,
        )

        viewModelScope.launch {
            val result = api.lookup(tagNumber)
            _state.value = _state.value.copy(busy = false, result = result)
            remember(tagNumber, result)
        }
    }

    /**
     * Skráir uppflettinguna í söguna.
     *
     * Tvennt hér er viljandi:
     *
     * 1. Aðeins [LookupResult.Found] og [LookupResult.NotFound] rata í
     *    söguna. Netvilla segir ekkert um hvort merkið er skráð, og að
     *    geyma hana sem „fannst ekki" er hreinlega rangt.
     * 2. Allt er í [runCatching] og gerist EFTIR að niðurstaðan er komin
     *    á skjáinn. Mistakist vistun hefur það engin áhrif á það sem
     *    notandinn sér — uppfletting sem tókst má aldrei líta út eins og
     *    villa af því geymslan fylltist.
     */
    private suspend fun remember(tagNumber: String, result: LookupResult) {
        val item = when (result) {
            is LookupResult.Found -> HistoryItem(
                tagNumber = tagNumber,
                found = true,
                name = result.pet.name,
                species = result.pet.species,
                at = System.currentTimeMillis(),
            )
            LookupResult.NotFound -> HistoryItem(
                tagNumber = tagNumber,
                found = false,
                at = System.currentTimeMillis(),
            )
            else -> return
        }
        runCatching { history.add(item) }
    }

    fun clearHistory() {
        viewModelScope.launch { runCatching { history.clear() } }
    }
}
