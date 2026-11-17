package com.bbva.bonifica.ui.descuentos

import com.bbva.bonifica.core.AppError
import com.bbva.bonifica.core.UiState
import com.bbva.bonifica.domain.model.PointOfSale
import com.bbva.bonifica.domain.model.Product

/**
 * Estado del formulario "Calcular descuento" (Módulo 3, ruta 1).
 *
 * Se reutiliza el `UiState` genérico de §10: `Loading` mientras se cargan las
 * operaciones y el producto, `Empty` si el cliente no tiene operaciones y `Error`
 * con `Reintentar` si falla la carga. Los errores del cálculo van en [calcError]
 * para mostrarse como banner junto al botón sin perder el formulario.
 */
data class CalcularData(
    /** Operaciones del cliente (ruta 14), con las `OPEN` primero por ser las recomendadas. */
    val operations: List<PointOfSale>,
    /** Producto elegido en el catálogo (ruta 9); null si la ruta no trajo `prod`. */
    val product: Product?,
    val selectedPosRef: String?,
    val quantity: Int = CalcularViewModel.MIN_QUANTITY,
    /** Bloquea controles mientras llama a la API (evita doble envío, §12). */
    val calculating: Boolean = false,
    /** Último error de Initiate (400/404…), ya convertido a mensaje con toMessage(). */
    val calcError: AppError? = null
) {
    val productSelected: Boolean get() = product != null
    val productAvailable: Boolean get() = product != null && product.status != "DISCONTINUED"
    val canCalculate: Boolean get() = productAvailable && selectedPosRef != null && quantity >= 1
}

typealias CalcularUiState = UiState<CalcularData>

/** Eventos de una sola vez (§10): el cálculo terminó y hay que abrir el resultado. */
sealed interface CalcularEvent {
    data class Calculado(val discountId: String) : CalcularEvent
}
