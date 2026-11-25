package com.bbva.bonifica.ui.negocio

import com.bbva.bonifica.core.UiState
import com.bbva.bonifica.domain.model.Customer
import com.bbva.bonifica.domain.model.PointOfSale

/** Ficha de cliente + sus operaciones (Módulo 7, rutas 11 y 14). */
data class ClienteNegocioData(
    val customer: Customer,
    val operations: List<PointOfSale>
)

/**
 * Estado de la ficha del cliente consultado (`negocio_cliente/{ref}`).
 * `Empty` significa "cliente no encontrado" (404 de la ruta 11, §12).
 */
typealias ClienteNegocioUiState = UiState<ClienteNegocioData>
