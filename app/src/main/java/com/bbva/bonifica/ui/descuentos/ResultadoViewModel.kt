package com.bbva.bonifica.ui.descuentos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bbva.bonifica.core.AppError
import com.bbva.bonifica.core.AppResult
import com.bbva.bonifica.core.UiState
import com.bbva.bonifica.domain.repository.DiscountRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel de Resultado / Confirmar / Anular (Módulo 3, rutas 2 y 3).
 *
 * Reglas de §12: solo se actúa sobre `INITIATED`, los botones se bloquean mientras
 * hay una petición en curso y un 409 se muestra como aviso **y** recarga el detalle.
 */
class ResultadoViewModel(
    val discountId: String,
    private val discountRepository: DiscountRepository
) : ViewModel() {

    private val _state = MutableStateFlow<ResultadoUiState>(UiState.Loading)
    val state: StateFlow<ResultadoUiState> = _state.asStateFlow()

    /**
     * Aviso de la última acción fallida (409 "ya procesado", 400…). Va en su propio
     * flujo para que el banner siga visible tras recargar el detalle con `load()`.
     */
    private val _notice = MutableStateFlow<AppError?>(null)
    val notice: StateFlow<AppError?> = _notice.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.value = UiState.Loading
            when (val result = discountRepository.get(discountId)) {
                is AppResult.Success -> _state.value = UiState.Content(ResultadoData(discount = result.data))
                is AppResult.Failure -> _state.value = UiState.Error(result.error)
            }
        }
    }

    fun selectPaymentMethod(method: String) {
        updateContent { it.copy(paymentMethod = method) }
    }

    fun confirmar() = update("APPLIED") { data -> data.paymentMethod }

    fun anular() = update("CANCELLED") { null }

    /** Llama a Update (ruta 3) con el estado y el medio de pago indicados. */
    private fun update(status: String, method: (ResultadoData) -> String?) {
        val data = (_state.value as? UiState.Content)?.data ?: return
        if (!data.canAct) return // solo sobre INITIATED y sin doble envío

        _notice.value = null
        updateContent { it.copy(saving = true, pendingStatus = status) }

        viewModelScope.launch {
            when (val result = discountRepository.update(discountId, status, method(data))) {
                is AppResult.Success -> updateContent {
                    it.copy(discount = result.data, saving = false, pendingStatus = null)
                }
                is AppResult.Failure -> {
                    updateContent { it.copy(saving = false, pendingStatus = null) }
                    _notice.value = result.error
                    if (result.error is AppError.Conflict) {
                        // 409: ya se procesó; recarga para mostrar el estado real (§12).
                        load()
                    }
                }
            }
        }
    }

    private fun updateContent(transform: (ResultadoData) -> ResultadoData) {
        _state.update { state ->
            if (state is UiState.Content) UiState.Content(transform(state.data)) else state
        }
    }

    companion object {
        const val DEFAULT_PAYMENT_METHOD = "CARGO_EN_CUENTA"
    }
}
