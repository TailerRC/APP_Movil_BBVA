package com.bbva.bonifica.ui.negocio

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bbva.bonifica.core.AppError
import com.bbva.bonifica.core.AppResult
import com.bbva.bonifica.core.UiState
import com.bbva.bonifica.domain.repository.CustomerRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Consulta de cliente por código (Módulo 7, ruta `negocio_consulta`).
 * Llama a las rutas 11 (ficha) y 14 (operaciones); un 404 se muestra como
 * "No se encontró …" vía `AppError.toMessage()` (§8.4).
 */
class ConsultaClienteViewModel(
    private val customerRepository: CustomerRepository
) : ViewModel() {

    private val _state = MutableStateFlow(ConsultaClienteUiState())
    val state: StateFlow<ConsultaClienteUiState> = _state.asStateFlow()

    fun onCodeChange(value: String) {
        _state.update { it.copy(code = value, codeError = null) }
    }

    /** Valida el código en local y, si es válido, consulta rutas 11 y 14 (§12). */
    fun buscar() {
        val code = _state.value.code.trim()
        val error = when {
            code.isBlank() -> CodigoError.REQUIRED
            !CODE_REGEX.matches(code) -> CodigoError.FORMAT
            else -> null
        }
        if (error != null) {
            _state.update { it.copy(codeError = error) }
            return
        }

        _state.update { it.copy(code = code, codeError = null, result = UiState.Loading) }
        viewModelScope.launch {
            when (val customer = customerRepository.getCustomer(code)) {
                // 404 → Empty con "Cliente no encontrado" (§12); el resto de
                // errores pasan por Error con Reintentar (§10).
                is AppResult.Failure -> _state.update {
                    it.copy(
                        result = if (customer.error is AppError.NotFound) {
                            UiState.Empty
                        } else {
                            UiState.Error(customer.error)
                        }
                    )
                }
                is AppResult.Success -> when (val operations = customerRepository.getOperations(code)) {
                    is AppResult.Failure -> _state.update {
                        it.copy(result = UiState.Error(operations.error))
                    }
                    is AppResult.Success -> _state.update {
                        it.copy(result = UiState.Content(ClienteNegocioData(customer.data, operations.data)))
                    }
                }
            }
        }
    }

    private companion object {
        val CODE_REGEX = Regex("PARTY-\\d+")
    }
}
