package com.bbva.bonifica.ui.descuentos

import com.bbva.bonifica.core.AppError
import com.bbva.bonifica.core.UiState
import com.bbva.bonifica.domain.model.DiscountPricing

/**
 * Estado de la pantalla de resultado (Módulo 3, ruta 2): el descuento devuelto por
 * Retrieve más los datos del formulario (medio de pago) y el bloqueo de los botones
 * mientras se llama a Update (§12).
 *
 * El error de una acción (p. ej. el 409 "ya procesado") no vive aquí: va en
 * `ResultadoViewModel.notice` para que el banner sobreviva a la recarga del detalle.
 */
data class ResultadoData(
    val discount: DiscountPricing,
    /** Medio de pago elegido; por defecto el de la Lambda (§8.2). */
    val paymentMethod: String = ResultadoViewModel.DEFAULT_PAYMENT_METHOD,
    /** Confirmar/Anular en curso: bloquea ambos botones (evita doble envío, §12). */
    val saving: Boolean = false,
    /** `"APPLIED"` o `"CANCELLED"` mientras la petición está en curso; null si no hay. */
    val pendingStatus: String? = null
) {
    val canAct: Boolean get() = discount.status == "INITIATED" && !saving
    val paymentOrderId: String? get() = discount.paymentOrderId
}

typealias ResultadoUiState = UiState<ResultadoData>

/** Medios de pago válidos para Update→APPLIED (§6, PaymentOrder.paymentMethod). */
val PAYMENT_METHODS = listOf("CARGO_EN_CUENTA", "TARJETA_DEBITO", "EFECTIVO_VENTANILLA")
