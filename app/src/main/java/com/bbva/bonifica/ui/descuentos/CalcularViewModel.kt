package com.bbva.bonifica.ui.descuentos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bbva.bonifica.core.AppResult
import com.bbva.bonifica.core.UiState
import com.bbva.bonifica.domain.model.PointOfSale
import com.bbva.bonifica.domain.model.Product
import com.bbva.bonifica.domain.repository.AuthRepository
import com.bbva.bonifica.domain.repository.CatalogRepository
import com.bbva.bonifica.domain.repository.CustomerRepository
import com.bbva.bonifica.domain.repository.DiscountRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel de "Calcular descuento" (Módulo 3, ruta 1).
 *
 * Carga las operaciones del cliente (ruta 14) y, si la ruta trajo `prod`, el
 * producto del catálogo (ruta 9); con la cantidad llama a Initiate y emite un
 * [CalcularEvent.Calculado] para abrir Resultado (§12, §13).
 */
class CalcularViewModel(
    private val initialPosRef: String?,
    val productRef: String?,
    private val authRepository: AuthRepository,
    private val customerRepository: CustomerRepository,
    private val catalogRepository: CatalogRepository,
    private val discountRepository: DiscountRepository
) : ViewModel() {

    private val _state = MutableStateFlow<CalcularUiState>(UiState.Loading)
    val state: StateFlow<CalcularUiState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<CalcularEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<CalcularEvent> = _events.asSharedFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.value = UiState.Loading

            val customerRef = authRepository.currentSession()?.customerReference
            if (customerRef == null) {
                // Un usuario `negocio` no tiene operaciones propias: nada que calcular.
                _state.value = UiState.Empty
                return@launch
            }

            val operations = when (val result = customerRepository.getOperations(customerRef)) {
                is AppResult.Success -> result.data
                is AppResult.Failure -> {
                    _state.value = UiState.Error(result.error)
                    return@launch
                }
            }

            var product: Product? = null
            if (productRef != null) {
                when (val result = catalogRepository.getProduct(productRef)) {
                    is AppResult.Success -> product = result.data
                    is AppResult.Failure -> {
                        _state.value = UiState.Error(result.error)
                        return@launch
                    }
                }
            }

            if (operations.isEmpty()) {
                _state.value = UiState.Empty
                return@launch
            }

            _state.value = UiState.Content(
                CalcularData(
                    operations = operations.sortedBy { if (it.status == "OPEN") 0 else 1 },
                    product = product,
                    selectedPosRef = defaultOperation(operations)
                )
            )
        }
    }

    fun selectOperation(posRef: String) {
        updateContent { it.copy(selectedPosRef = posRef, calcError = null) }
    }

    fun increaseQuantity() = changeQuantity(+1)

    fun decreaseQuantity() = changeQuantity(-1)

    fun clearError() {
        updateContent { it.copy(calcError = null) }
    }

    /** Llama a Initiate (ruta 1). Bloqueado mientras hay una petición en curso. */
    fun calcular() {
        val data = (_state.value as? UiState.Content)?.data ?: return
        val posRef = data.selectedPosRef ?: return
        val product = data.product ?: return
        if (data.calculating || !data.canCalculate) return

        updateContent { it.copy(calculating = true, calcError = null) }
        viewModelScope.launch {
            when (val result = discountRepository.initiate(posRef, product.productReference, data.quantity)) {
                is AppResult.Success -> {
                    updateContent { it.copy(calculating = false) }
                    _events.emit(CalcularEvent.Calculado(result.data.discountPricingId))
                }
                is AppResult.Failure -> {
                    updateContent { it.copy(calculating = false, calcError = result.error) }
                }
            }
        }
    }

    private fun changeQuantity(delta: Int) {
        updateContent { current ->
            current.copy(quantity = (current.quantity + delta).coerceIn(MIN_QUANTITY, MAX_QUANTITY))
        }
    }

    private fun defaultOperation(operations: List<PointOfSale>): String? {
        val requested = operations.firstOrNull { it.pointOfSaleReference == initialPosRef }
        if (requested != null) return requested.pointOfSaleReference
        return operations.firstOrNull { it.status == "OPEN" }?.pointOfSaleReference
            ?: operations.firstOrNull()?.pointOfSaleReference
    }

    private fun updateContent(transform: (CalcularData) -> CalcularData) {
        _state.update { state ->
            if (state is UiState.Content) UiState.Content(transform(state.data)) else state
        }
    }

    companion object {
        const val MIN_QUANTITY = 1
        const val MAX_QUANTITY = 99
    }
}
