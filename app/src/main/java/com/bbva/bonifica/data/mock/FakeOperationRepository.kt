package com.bbva.bonifica.data.mock

import com.bbva.bonifica.core.AppError
import com.bbva.bonifica.core.AppResult
import com.bbva.bonifica.domain.model.DiscountPricing
import com.bbva.bonifica.domain.model.PointOfSale
import com.bbva.bonifica.domain.repository.OperationRepository
import kotlinx.coroutines.delay

/** Implementación simulada de Operaciones (Persona C: rutas 10 y 8). */
class FakeOperationRepository : OperationRepository {
    override suspend fun getOperation(ref: String): AppResult<PointOfSale> {
        delay(400)
        val op = MockData.operations.firstOrNull { it.pointOfSaleReference == ref }
        return if (op != null) {
            AppResult.Success(op)
        } else {
            AppResult.Failure(AppError.NotFound("la operación $ref"))
        }
    }

    override suspend fun getDiscountsByOperation(ref: String): AppResult<List<DiscountPricing>> {
        delay(500)
        // MockStore y no MockData: así se ven también los descuentos que acaba de
        // calcular el Módulo 3 en esta sesión (§13).
        val discounts = MockStore.discounts.filter { it.pointOfSaleReference == ref }
        return AppResult.Success(discounts)
    }
}
