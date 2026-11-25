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
 * ViewModel para el detalle de producto (Módulo 2, ruta 9).
 */
class ProductoDetalleViewModel(
    val productRef: String,
    private val catalogRepository: CatalogRepository
) : ViewModel() {

    private val _state = MutableStateFlow<ProductoDetalleUiState>(UiState.Loading)
    val state: StateFlow<ProductoDetalleUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.value = UiState.Loading
            when (val result = catalogRepository.getProduct(productRef)) {
                is AppResult.Success -> {
                    _state.value = UiState.Content(result.data)
                }
                is AppResult.Failure -> {
                    _state.value = UiState.Error(result.error)
                }
            }
        }
    }
}
