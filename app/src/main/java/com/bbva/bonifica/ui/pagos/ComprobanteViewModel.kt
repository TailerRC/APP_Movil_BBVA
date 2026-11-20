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
 * ViewModel del Comprobante de pago (Módulo 6, ruta 12).
 * Se abre desde el resultado (Módulo 3, "Ver comprobante") o desde la lista de pagos.
 */
class ComprobanteViewModel(
    val payId: String,
    private val paymentRepository: PaymentRepository
) : ViewModel() {

    private val _state = MutableStateFlow<ComprobanteUiState>(UiState.Loading)
    val state: StateFlow<ComprobanteUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.value = UiState.Loading
            _state.value = when (val result = paymentRepository.getPayment(payId)) {
                is AppResult.Success -> UiState.Content(result.data)
                is AppResult.Failure -> UiState.Error(result.error)
            }
        }
    }
}
