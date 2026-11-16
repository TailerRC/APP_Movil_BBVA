package com.bbva.bonifica.data.mock

import com.bbva.bonifica.core.AppError
import com.bbva.bonifica.core.AppResult
import com.bbva.bonifica.domain.model.Product
import com.bbva.bonifica.domain.repository.CatalogRepository
import kotlinx.coroutines.delay

/** Ejemplo ya funcional. TODO (Persona C): ampliar MockData.products con más productos por categoría. */
class FakeCatalogRepository : CatalogRepository {
    override suspend fun getByCategory(category: String): AppResult<List<Product>> {
        delay(500)
        return AppResult.Success(MockData.products.filter { it.category == category })
    }

    override suspend fun getProduct(ref: String): AppResult<Product> {
        delay(400)
        val p = MockData.products.firstOrNull { it.productReference == ref }
        return if (p != null) AppResult.Success(p) else AppResult.Failure(AppError.NotFound("el producto $ref"))
    }
}
