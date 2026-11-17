package com.bbva.bonifica.data.mock

import com.bbva.bonifica.domain.model.DiscountPricing
import com.bbva.bonifica.domain.model.PaymentOrder
import java.util.Locale

/**
 * Estado en memoria de descuentos y pagos (diseño §14): `FakeDiscountRepository`
 * cambia el estado en memoria y, al confirmar, crea el pago.
 *
 * Las semillas vienen de [MockData]; lo que el cliente calcula o confirma se añade
 * aquí para que el resto de la app (p. ej. el listado de descuentos de una operación,
 * ruta 8) vea exactamente los mismos datos que el Módulo 3.
 */
object MockStore {

    /** Descuentos (rutas 2 y 8). El recién calculado va primero. */
    val discounts: MutableList<DiscountPricing> = mutableListOf()

    /** Pagos creados al confirmar un descuento (rutas 12 y 15, Persona E). */
    val payments: MutableList<PaymentOrder> = mutableListOf()

    private var nextDiscountSeq = 1000
    private var nextPaymentSeq = 2 // PAY-0001 ya viene sembrado en MockData.payments

    init {
        reset()
    }

    /** Restaura las semillas de [MockData] (también se usa en pruebas). */
    @Synchronized
    fun reset() {
        discounts.clear()
        discounts.addAll(MockData.discounts)
        payments.clear()
        payments.addAll(MockData.payments)
        nextDiscountSeq = 1000
        nextPaymentSeq = 2
    }

    @Synchronized
    fun newDiscountId(): String = String.format(Locale.US, "DP-%04d", nextDiscountSeq++)

    @Synchronized
    fun newPaymentId(): String = String.format(Locale.US, "PAY-%04d", nextPaymentSeq++)
}
