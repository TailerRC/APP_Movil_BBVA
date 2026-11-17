package com.bbva.bonifica.ui.operaciones

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bbva.bonifica.core.AppResult
import com.bbva.bonifica.core.UiState
import com.bbva.bonifica.domain.repository.AuthRepository
import com.bbva.bonifica.domain.repository.CustomerRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel para el listado de operaciones del cliente (Módulo 5, ruta 14).
 */
class OperacionesViewModel(
    private val authRepository: AuthRepository,
    private val customerRepository: CustomerRepository
) : ViewModel() {

    private val _state = MutableStateFlow<OperacionesUiState>(UiState.Loading)
    val state: StateFlow<OperacionesUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.value = UiState.Loading
            val customerRef = authRepository.currentSession()?.customerReference
            if (customerRef == null) {
                _state.value = UiState.Empty
                return@launch
            }

            when (val result = customerRepository.getOperations(customerRef)) {
                is AppResult.Success -> {
                    if (result.data.isEmpty()) {
                        _state.value = UiState.Empty
                    } else {
                        _state.value = UiState.Content(result.data)
                    }
                }
                is AppResult.Failure -> {
                    _state.value = UiState.Error(result.error)
                }
            }
        }
    }
}
