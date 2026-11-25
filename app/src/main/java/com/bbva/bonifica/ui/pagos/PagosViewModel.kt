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
 * ViewModel de Pagos de una operación (Módulo 6, ruta 15).
 * Lista los pagos generados por los descuentos del punto de venta; se entra
 * desde el detalle de la operación (Módulo 5, "Ver pagos").
 */
class PagosViewModel(
    val posRef: String,
    private val paymentRepository: PaymentRepository
) : ViewModel() {

    private val _state = MutableStateFlow<PagosUiState>(UiState.Loading)
    val state: StateFlow<PagosUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.value = UiState.Loading
            _state.value = when (val result = paymentRepository.getPaymentsByOperation(posRef)) {
                is AppResult.Success ->
                    if (result.data.isEmpty()) UiState.Empty else UiState.Content(result.data)
                is AppResult.Failure -> UiState.Error(result.error)
            }
        }
    }
}
