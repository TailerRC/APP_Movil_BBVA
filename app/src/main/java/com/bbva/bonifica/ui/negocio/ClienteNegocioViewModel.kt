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
import kotlinx.coroutines.launch

/**
 * Ficha del cliente consultado (Módulo 7, ruta `negocio_cliente/{ref}`):
 * carga la ruta 11 y las operaciones del cliente con la ruta 14.
 */
class ClienteNegocioViewModel(
    val customerRef: String,
    private val customerRepository: CustomerRepository
) : ViewModel() {

    private val _state = MutableStateFlow<ClienteNegocioUiState>(UiState.Loading)
    val state: StateFlow<ClienteNegocioUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.value = UiState.Loading
            when (val customer = customerRepository.getCustomer(customerRef)) {
                // 404 → Empty con "Cliente no encontrado" (§12); el resto de
                // errores pasan por Error con Reintentar (§10).
                is AppResult.Failure -> _state.value = if (customer.error is AppError.NotFound) {
                    UiState.Empty
                } else {
                    UiState.Error(customer.error)
                }
                is AppResult.Success -> when (val operations = customerRepository.getOperations(customerRef)) {
                    is AppResult.Failure -> _state.value = UiState.Error(operations.error)
                    is AppResult.Success -> _state.value =
                        UiState.Content(ClienteNegocioData(customer.data, operations.data))
                }
            }
        }
    }
}
