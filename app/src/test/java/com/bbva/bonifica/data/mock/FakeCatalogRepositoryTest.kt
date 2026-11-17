package com.bbva.bonifica.data.mock

import com.bbva.bonifica.core.AppError
import com.bbva.bonifica.core.AppResult
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FakeCatalogRepositoryTest {

    private lateinit var repository: FakeCatalogRepository

    @Before
    fun setUp() {
        repository = FakeCatalogRepository()
    }

    @Test
    fun `getByCategory retorna productos de la categoria solicitada`() = runBlocking {
        val result = repository.getByCategory("TARJETAS")

        assertTrue(result is AppResult.Success)
        val products = (result as AppResult.Success).data
        assertTrue(products.isNotEmpty())
        assertTrue(products.all { it.category == "TARJETAS" })
        assertTrue(products.any { it.productReference == "PROD-0025" })
    }

    @Test
    fun `getByCategory retorna lista vacia para categoria sin productos`() = runBlocking {
        val result = repository.getByCategory("CATEGORIA_INEXISTENTE")

        assertTrue(result is AppResult.Success)
        val products = (result as AppResult.Success).data
        assertTrue(products.isEmpty())
    }

    @Test
    fun `getProduct retorna producto existente`() = runBlocking {
        val result = repository.getProduct("PROD-0025")

        assertTrue(result is AppResult.Success)
        val product = (result as AppResult.Success).data
        assertEquals("PROD-0025", product.productReference)
        assertEquals("TARJETAS", product.category)
        assertEquals(172.50, product.listPrice, 0.001)
        assertEquals("AVAILABLE", product.status)
    }

    @Test
    fun `getProduct retorna producto descontinuado correctamente`() = runBlocking {
        val result = repository.getProduct("PROD-0008")

        assertTrue(result is AppResult.Success)
        val product = (result as AppResult.Success).data
        assertEquals("PROD-0008", product.productReference)
        assertEquals("DISCONTINUED", product.status)
    }

    @Test
    fun `getProduct retorna NotFound para producto inexistente`() = runBlocking {
        val result = repository.getProduct("PROD-9999")

        assertTrue(result is AppResult.Failure)
        val error = (result as AppResult.Failure).error
        assertTrue(error is AppError.NotFound)
    }
}
