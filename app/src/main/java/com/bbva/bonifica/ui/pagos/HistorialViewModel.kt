package com.bbva.bonifica.ui.pagos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bbva.bonifica.core.AppResult
import com.bbva.bonifica.core.UiState
import com.bbva.bonifica.domain.repository.PaymentRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel del Historial (Módulo 6, ruta 4): eventos `INITIATED → APPLIED/CANCELLED`
 * del descuento. Se abre desde el comprobante ("Ver historial").
 */
class HistorialViewModel(
    val discountId: String,
    private val paymentRepository: PaymentRepository
) : ViewModel() {

    private val _state = MutableStateFlow<HistorialUiState>(UiState.Loading)
    val state: StateFlow<HistorialUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.value = UiState.Loading
            _state.value = when (val result = paymentRepository.getHistory(discountId)) {
                is AppResult.Success ->
                    if (result.data.isEmpty()) UiState.Empty else UiState.Content(result.data)
                is AppResult.Failure -> UiState.Error(result.error)
            }
        }
    }
}
