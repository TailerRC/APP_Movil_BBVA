package com.bbva.bonifica.ui.pagos

import com.bbva.bonifica.core.UiState
import com.bbva.bonifica.domain.model.EventLogEntry
import com.bbva.bonifica.domain.model.PaymentOrder

/**
 * Estados de las pantallas del Módulo 6:
 * listado de pagos de una operación (ruta 15), comprobante (ruta 12) y
 * línea de tiempo del descuento (ruta 4).
 */
typealias PagosUiState = UiState<List<PaymentOrder>>
typealias ComprobanteUiState = UiState<PaymentOrder>
typealias HistorialUiState = UiState<List<EventLogEntry>>
