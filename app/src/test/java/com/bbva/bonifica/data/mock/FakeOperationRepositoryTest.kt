package com.bbva.bonifica.data.mock

import com.bbva.bonifica.core.AppError
import com.bbva.bonifica.core.AppResult
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FakeOperationRepositoryTest {

    private lateinit var repository: FakeOperationRepository

    @Before
    fun setUp() {
        repository = FakeOperationRepository()
    }

    @Test
    fun `getOperation retorna operacion existente`() = runBlocking {
        val result = repository.getOperation("SALE-SEED-0001")

        assertTrue(result is AppResult.Success)
        val op = (result as AppResult.Success).data
        assertEquals("SALE-SEED-0001", op.pointOfSaleReference)
        assertEquals("PARTY-000024", op.customerReference)
        assertEquals("BRANCH", op.channel)
        assertEquals("OPEN", op.status)
    }

    @Test
    fun `getOperation retorna NotFound para operacion inexistente`() = runBlocking {
        val result = repository.getOperation("SALE-INEXISTENTE")

        assertTrue(result is AppResult.Failure)
        val error = (result as AppResult.Failure).error
        assertTrue(error is AppError.NotFound)
    }

    @Test
    fun `getDiscountsByOperation retorna descuentos de la operacion`() = runBlocking {
        val result = repository.getDiscountsByOperation("SALE-SEED-0001")

        assertTrue(result is AppResult.Success)
        val discounts = (result as AppResult.Success).data
        assertTrue(discounts.isNotEmpty())
        assertTrue(discounts.any { it.discountPricingId == "DP-0001" })
    }

    @Test
    fun `getDiscountsByOperation retorna lista vacia si no hay descuentos`() = runBlocking {
        val result = repository.getDiscountsByOperation("SALE-SEED-0003")

        assertTrue(result is AppResult.Success)
        val discounts = (result as AppResult.Success).data
        assertTrue(discounts.isEmpty())
    }
}
