package com.bbva.bonifica.data.mock

import com.bbva.bonifica.core.AppResult
import com.bbva.bonifica.domain.model.DiscountPricing
import com.bbva.bonifica.domain.model.PointOfSale
import com.bbva.bonifica.domain.repository.OperationRepository

/** TODO (Persona C): rutas 10 y 8. */
class FakeOperationRepository : OperationRepository {
    override suspend fun getOperation(ref: String): AppResult<PointOfSale> =
        TODO("Persona C: implementar")

    override suspend fun getDiscountsByOperation(ref: String): AppResult<List<DiscountPricing>> =
        TODO("Persona C: implementar")
}
