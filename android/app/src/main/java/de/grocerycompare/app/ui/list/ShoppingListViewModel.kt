package de.grocerycompare.app.ui.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.grocerycompare.app.data.remote.dto.BasketDto
import de.grocerycompare.app.data.repo.GroceryRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ShoppingListUiState(
    val items: List<String> = emptyList(),
    val draft: String = "",
    val plz: String = "80331",
    val loading: Boolean = false,
    val result: BasketDto? = null,
    val error: String? = null,
)

@HiltViewModel
class ShoppingListViewModel @Inject constructor(
    private val repo: GroceryRepository,
) : ViewModel() {

    private val _ui = MutableStateFlow(ShoppingListUiState())
    val ui: StateFlow<ShoppingListUiState> = _ui.asStateFlow()

    fun onDraftChange(v: String) { _ui.value = _ui.value.copy(draft = v) }
    fun onPlzChange(v: String) { _ui.value = _ui.value.copy(plz = v) }

    fun addItem() {
        val item = _ui.value.draft.trim()
        if (item.isEmpty() || item in _ui.value.items) return
        _ui.value = _ui.value.copy(items = _ui.value.items + item, draft = "")
    }

    fun removeItem(item: String) {
        _ui.value = _ui.value.copy(items = _ui.value.items - item)
    }

    fun compare() {
        val items = _ui.value.items
        if (items.isEmpty()) return
        _ui.value = _ui.value.copy(loading = true, error = null)
        viewModelScope.launch {
            val res = repo.basket(items, _ui.value.plz.trim())
            _ui.value = _ui.value.copy(
                loading = false,
                result = res.getOrNull(),
                error = res.exceptionOrNull()?.let { "Netzwerkfehler" },
            )
        }
    }
}
