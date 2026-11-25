package com.bbva.bonifica.ui.operaciones

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bbva.bonifica.core.AppResult
import com.bbva.bonifica.core.UiState
import com.bbva.bonifica.domain.repository.OperationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel para el detalle de una operación y sus descuentos asociados (Módulo 5, rutas 10 y 8).
 */
class OperacionDetalleViewModel(
    val operationRef: String,
    private val operationRepository: OperationRepository
) : ViewModel() {

    private val _state = MutableStateFlow<OperacionDetalleUiState>(UiState.Loading)
    val state: StateFlow<OperacionDetalleUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.value = UiState.Loading
            when (val opResult = operationRepository.getOperation(operationRef)) {
                is AppResult.Success -> {
                    val discountsResult = operationRepository.getDiscountsByOperation(operationRef)
                    val discounts = if (discountsResult is AppResult.Success) discountsResult.data else emptyList()
                    _state.value = UiState.Content(
                        OperacionDetalleData(
                            operation = opResult.data,
                            discounts = discounts
                        )
                    )
                }
                is AppResult.Failure -> {
                    _state.value = UiState.Error(opResult.error)
                }
            }
        }
    }
}
