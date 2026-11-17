package com.bbva.bonifica.ui.catalogo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bbva.bonifica.core.AppResult
import com.bbva.bonifica.core.UiState
import com.bbva.bonifica.domain.repository.CatalogRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel para la lista de productos por categoría (Módulo 2, ruta 13).
 */
class ProductosViewModel(
    val category: String,
    private val catalogRepository: CatalogRepository
) : ViewModel() {

    private val _state = MutableStateFlow<ProductosUiState>(UiState.Loading)
    val state: StateFlow<ProductosUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.value = UiState.Loading
            when (val result = catalogRepository.getByCategory(category)) {
                is AppResult.Success -> {
                    if (result.data.isEmpty()) {
                        _state.value = UiState.Empty
                    } else {
                        _state.value = UiState.Content(
                            ProductosUiData(
                                category = category,
                                allProducts = result.data,
                                searchQuery = "",
                                filteredProducts = result.data
                            )
                        )
                    }
                }
                is AppResult.Failure -> {
                    _state.value = UiState.Error(result.error)
                }
            }
        }
    }

    fun onSearchQueryChange(query: String) {
        val current = _state.value
        if (current is UiState.Content) {
            val trimmed = query.trim()
            val filtered = if (trimmed.isBlank()) {
                current.data.allProducts
            } else {
                current.data.allProducts.filter {
                    it.name.contains(trimmed, ignoreCase = true) ||
                        it.productReference.contains(trimmed, ignoreCase = true)
                }
            }
            _state.value = UiState.Content(
                current.data.copy(
                    searchQuery = query,
                    filteredProducts = filtered
                )
            )
        }
    }
}
