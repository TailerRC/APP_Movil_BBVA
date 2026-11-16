package com.bbva.bonifica.data.mock

import com.bbva.bonifica.core.AppResult
import com.bbva.bonifica.domain.model.DiscountPricing
import com.bbva.bonifica.domain.repository.DiscountRepository

/**
 * TODO (Persona D): aplicar la misma regla que la Lambda (mejor política por moneda, canal, mínimo,
 * vigencia, alcance). `update` cambia estado en memoria y crea el pago; repetir `update` -> Conflict.
 * Resultado esperado: PROD-0025 x2 -> neto 303.60 con POL-022 (§14).
 */
class FakeDiscountRepository : DiscountRepository {
    override suspend fun initiate(posRef: String, productRef: String, quantity: Int): AppResult<DiscountPricing> =
        TODO("Persona D: implementar")

    override suspend fun get(id: String): AppResult<DiscountPricing> =
        TODO("Persona D: implementar")

    override suspend fun update(id: String, status: String, paymentMethod: String?): AppResult<DiscountPricing> =
        TODO("Persona D: implementar")
}
