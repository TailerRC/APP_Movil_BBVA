package com.bbva.bonifica.data.mock

import com.bbva.bonifica.core.AppError
import com.bbva.bonifica.core.AppResult
import com.bbva.bonifica.core.formatMoney
import com.bbva.bonifica.domain.model.EventLogEntry
import com.bbva.bonifica.domain.model.PaymentOrder
import com.bbva.bonifica.domain.repository.PaymentRepository
import kotlinx.coroutines.delay

/**
 * Implementación simulada de Pagos e Historial (Módulo 6, rutas 12, 15 y 4).
 *
 * Lee de [MockStore] —y no de [MockData]— para que también se vean los pagos que
 * acaba de crear el Módulo 3 al confirmar un descuento (§13). El historial se
 * reconstruye a partir del descuento: `INITIATED` siempre y, si el estado final
 * lo permite, `APPLIED` o `CANCELLED` (§12).
 */
class FakePaymentRepository : PaymentRepository {

    override suspend fun getPayment(id: String): AppResult<PaymentOrder> {
        delay(400)
        val payment = MockStore.payments.firstOrNull { it.paymentOrderId == id }
            ?: return AppResult.Failure(AppError.NotFound("el pago $id"))
        return AppResult.Success(payment)
    }

    override suspend fun getPaymentsByOperation(posRef: String): AppResult<List<PaymentOrder>> {
        delay(500)
        return AppResult.Success(MockStore.payments.filter { it.pointOfSaleReference == posRef })
    }

    override suspend fun getHistory(discountId: String): AppResult<List<EventLogEntry>> {
        delay(500)
        val discount = MockStore.discounts.firstOrNull { it.discountPricingId == discountId }
            ?: return AppResult.Failure(AppError.NotFound("el descuento $discountId"))

        val policyLabel = discount.policyName.ifBlank { "sin política aplicable" }
        val events = mutableListOf(
            EventLogEntry(
                discountPricingId = discount.discountPricingId,
                eventTimestamp = discount.createdAt,
                eventType = "DISCOUNT_INITIATED",
                previousStatus = null,
                newStatus = "INITIATED",
                actor = discount.customerReference,
                channel = discount.channel,
                policyId = discount.policyId,
                details = "Cálculo iniciado: ${discount.quantity} × ${discount.productReference} · $policyLabel"
            )
        )

        when (discount.status) {
            "APPLIED" -> events += EventLogEntry(
                discountPricingId = discount.discountPricingId,
                eventTimestamp = discount.updatedAt,
                eventType = "DISCOUNT_APPLIED",
                previousStatus = "INITIATED",
                newStatus = "APPLIED",
                actor = discount.customerReference,
                channel = discount.channel,
                policyId = discount.policyId,
                details = buildString {
                    append("Descuento confirmado: neto ${formatMoney(discount.netAmount, discount.currency)}")
                    discount.paymentOrderId?.let { append(" · orden de pago $it") }
                }
            )
            "CANCELLED" -> events += EventLogEntry(
                discountPricingId = discount.discountPricingId,
                eventTimestamp = discount.updatedAt,
                eventType = "DISCOUNT_CANCELLED",
                previousStatus = "INITIATED",
                newStatus = "CANCELLED",
                actor = discount.customerReference,
                channel = discount.channel,
                policyId = discount.policyId,
                details = "Descuento anulado: no se generó ninguna orden de pago"
            )
        }

        return AppResult.Success(events)
    }
}
