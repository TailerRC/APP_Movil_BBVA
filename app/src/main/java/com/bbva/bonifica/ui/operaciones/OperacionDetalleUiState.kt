package com.bbva.bonifica.ui.operaciones

import com.bbva.bonifica.core.UiState
import com.bbva.bonifica.domain.model.DiscountPricing
import com.bbva.bonifica.domain.model.PointOfSale

/**
 * Datos para la pantalla de detalle de operación (Módulo 5, rutas 10 y 8).
 */
data class OperacionDetalleData(
    val operation: PointOfSale,
    val discounts: List<DiscountPricing>
)

/**
 * Estado para la pantalla de detalle de operación.
 */
typealias OperacionDetalleUiState = UiState<OperacionDetalleData>
