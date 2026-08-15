package de.grocerycompare.app.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.grocerycompare.app.data.local.OfferEntity
import de.grocerycompare.app.data.repo.GroceryRepository
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SearchUiState(
    val query: String = "",
    val plz: String = "80331",
    val suggestions: List<String> = emptyList(),
    val loading: Boolean = false,
    val offline: Boolean = false,
    val error: String? = null,
)

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val repo: GroceryRepository,
) : ViewModel() {

    private val _ui = MutableStateFlow(SearchUiState())
    val ui: StateFlow<SearchUiState> = _ui.asStateFlow()

    // the (query, plz) actually submitted; results react to both.
    private val submitted = MutableStateFlow("" to "80331")

    // offline-first: results always come from the Room cache; refresh feeds the cache.
    val results: StateFlow<List<OfferEntity>> = submitted
        .distinctUntilChanged()
        .flatMapLatest { (q, plz) -> repo.observeOffers(q, plz) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Set of query strings the user is currently watching (for the bell toggle). */
    val watchedQueries: StateFlow<Set<String>> = repo.observeWatched()
        .map { list -> list.map { it.query }.toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    init {
        // debounced autocomplete as the user types
        viewModelScope.launch {
            _ui.debounce(250).distinctUntilChanged { a, b -> a.query == b.query }.collect { s ->
                if (s.query.length >= 2) {
                    val sug = repo.autocomplete(s.query, s.plz)
                    _ui.value = _ui.value.copy(suggestions = sug)
                }
            }
        }
    }

    fun onQueryChange(q: String) { _ui.value = _ui.value.copy(query = q) }

    fun onPlzChange(plz: String) { _ui.value = _ui.value.copy(plz = plz) }

    fun submit(lat: Double? = null, lon: Double? = null, maxDistanceKm: Double? = null) {
        val q = _ui.value.query.trim()
        val plz = _ui.value.plz.trim()
        if (q.isEmpty()) return
        submitted.value = q to plz
        _ui.value = _ui.value.copy(loading = true, error = null, suggestions = emptyList())
        viewModelScope.launch {
            val res = repo.refreshOffers(q, plz, lat, lon, maxDistanceKm)
            _ui.value = _ui.value.copy(
                loading = false,
                offline = res.isFailure,   // still shows cached results underneath
                error = res.exceptionOrNull()?.let { "network" },
            )
        }
    }

    fun watch(query: String) = viewModelScope.launch { repo.watch(query, _ui.value.plz) }
}
