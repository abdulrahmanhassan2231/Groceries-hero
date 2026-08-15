package de.grocerycompare.app.ui.watched

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.grocerycompare.app.data.local.WatchedItemEntity
import de.grocerycompare.app.data.repo.GroceryRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class WatchedViewModel @Inject constructor(
    private val repo: GroceryRepository,
) : ViewModel() {

    val watched: StateFlow<List<WatchedItemEntity>> = repo.observeWatched()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun unwatch(query: String) = viewModelScope.launch { repo.unwatch(query) }
}
