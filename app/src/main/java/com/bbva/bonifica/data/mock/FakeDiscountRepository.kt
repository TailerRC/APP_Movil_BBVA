package com.bbva.bonifica.data.mock

import com.bbva.bonifica.core.AppError
import com.bbva.bonifica.core.AppResult
import com.bbva.bonifica.domain.model.DiscountPricing
import com.bbva.bonifica.domain.model.PaymentOrder
import com.bbva.bonifica.domain.model.Policy
import com.bbva.bonifica.domain.model.Product
import com.bbva.bonifica.domain.repository.DiscountRepository
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import kotlin.math.min
import kotlin.math.round

/**
 * Implementación simulada de Descuentos (Módulo 3, rutas 1, 2 y 3).
 *
 * Reproduce la regla de la Lambda (§14): mejor política activa por moneda, canal,
 * monto mínimo, vigencia y alcance; `update` cambia el estado en memoria y crea el
 * pago; repetir `update` devuelve `Conflict` (§17).
 *
 * @param today fecha de "hoy" para evaluar la vigencia de las políticas; se inyecta
 *   para que las pruebas sean deterministas (por defecto, la fecha real).
 */
class FakeDiscountRepository(
    private val today: LocalDate = LocalDate.now()
) : DiscountRepository {

    override suspend fun initiate(
        posRef: String,
        productRef: String,
        quantity: Int
    ): AppResult<DiscountPricing> {
        delay(600)
        // Validaciones igual que la Lambda (§8.4): 400 para datos inválidos, 404 si no existe.
        if (quantity <= 0) {
            return AppResult.Failure(AppError.BadRequest("quantity debe ser mayor que cero"))
        }
        val operation = MockData.operations.firstOrNull { it.pointOfSaleReference == posRef }
            ?: return AppResult.Failure(AppError.NotFound("la operación $posRef"))
        val product = MockData.products.firstOrNull { it.productReference == productRef }
            ?: return AppResult.Failure(AppError.NotFound("el producto $productRef"))
        if (product.status != "AVAILABLE") {
            return AppResult.Failure(
                AppError.BadRequest("El producto ${product.name} no está disponible")
            )
        }

        val gross = round2(product.listPrice * quantity)
        val policy = bestPolicy(product, operation.channel, gross)
        val discountAmount = policy?.let { discountOf(it, gross) } ?: 0.0
        val now = nowIso()

        val discount = DiscountPricing(
            discountPricingId = MockStore.newDiscountId(),
            pointOfSaleReference = operation.pointOfSaleReference,
            customerReference = operation.customerReference,
            productReference = product.productReference,
            category = product.category,
            channel = operation.channel,
            currency = product.currency,
            listPrice = product.listPrice,
            quantity = quantity,
            grossAmount = gross,
            policyId = policy?.policyId ?: "NONE",     // "NONE" si no hubo descuento (§6)
            policyName = policy?.name.orEmpty(),
            discountAmount = discountAmount,
            netAmount = round2(gross - discountAmount),
            status = "INITIATED",
            createdAt = now,
            updatedAt = now
        )
        MockStore.discounts.add(0, discount)
        return AppResult.Success(discount)
    }

    override suspend fun get(id: String): AppResult<DiscountPricing> {
        delay(400)
        val discount = MockStore.discounts.firstOrNull { it.discountPricingId == id }
            ?: return AppResult.Failure(AppError.NotFound("el descuento $id"))
        return AppResult.Success(discount)
    }

    override suspend fun update(
        id: String,
        status: String,
        paymentMethod: String?
    ): AppResult<DiscountPricing> {
        delay(600)
        if (status != "APPLIED" && status != "CANCELLED") {
            return AppResult.Failure(AppError.BadRequest("status debe ser APPLIED o CANCELLED"))
        }
        val index = MockStore.discounts.indexOfFirst { it.discountPricingId == id }
        if (index < 0) {
            return AppResult.Failure(AppError.NotFound("el descuento $id"))
        }
        val current = MockStore.discounts[index]
        // Solo se actúa sobre INITIATED: repetir da 409 (§12, §17).
        if (current.status != "INITIATED") {
            return AppResult.Failure(AppError.Conflict)
        }

        val updated = if (status == "APPLIED") {
            // Se paga el neto y el pago nace PAID (§13, §14).
            val payment = PaymentOrder(
                paymentOrderId = MockStore.newPaymentId(),
                discountPricingId = current.discountPricingId,
                customerReference = current.customerReference,
                pointOfSaleReference = current.pointOfSaleReference,
                amount = current.netAmount,
                currency = current.currency,
                paymentMethod = paymentMethod ?: "CARGO_EN_CUENTA",
                status = "PAID",
                paymentDate = nowIso()
            )
            MockStore.payments.add(0, payment)
            current.copy(status = "APPLIED", paymentOrderId = payment.paymentOrderId, updatedAt = nowIso())
        } else {
            current.copy(status = "CANCELLED", paymentOrderId = null, updatedAt = nowIso())
        }

        MockStore.discounts[index] = updated
        return AppResult.Success(updated)
    }

    /** Mejor política activa: mayor descuento entre las que cumplen todas las reglas. */
    private fun bestPolicy(product: Product, channel: String, gross: Double): Policy? =
        MockData.policies
            .filter { policy ->
                policy.status == "ACTIVE" &&
                    policy.currency == product.currency &&
                    (policy.channel == "ALL" || policy.channel == channel) &&
                    gross >= policy.minimumAmount &&
                    matchesScope(policy, product) &&
                    isValidOn(policy, today)
            }
            .maxByOrNull { discountOf(it, gross) }

    private fun matchesScope(policy: Policy, product: Product): Boolean = when (policy.scope) {
        "ALL" -> true
        "CATEGORY" -> policy.scopeReference == product.category
        "PRODUCT" -> policy.scopeReference == product.productReference
        else -> false
    }

    private fun isValidOn(policy: Policy, day: LocalDate): Boolean = runCatching {
        !day.isBefore(LocalDate.parse(policy.validFrom)) && !day.isAfter(LocalDate.parse(policy.validTo))
    }.getOrDefault(false)

    private fun discountOf(policy: Policy, gross: Double): Double = when (policy.discountType) {
        "PERCENTAGE" -> round2(gross * policy.discountValue / 100.0)
        "FIXED_AMOUNT" -> round2(min(policy.discountValue, gross))
        else -> 0.0
    }

    private fun round2(value: Double): Double = round(value * 100.0) / 100.0

    private fun nowIso(): String =
        OffsetDateTime.now(ZoneOffset.UTC).truncatedTo(ChronoUnit.SECONDS).toString()
}
