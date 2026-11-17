package com.bbva.bonifica.data.mock

import com.bbva.bonifica.core.AppError
import com.bbva.bonifica.core.AppResult
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

/**
 * Pruebas del cálculo del Módulo 3 (§17): la regla de la Lambda y el resultado
 * esperado de §14 (PROD-0025 x2 -> neto 303.60 con POL-022).
 *
 * `today` se fija para que la vigencia de las políticas no dependa de la fecha real.
 */
class FakeDiscountRepositoryTest {

    private lateinit var repository: FakeDiscountRepository

    @Before
    fun setUp() {
        MockStore.reset()
        repository = FakeDiscountRepository(today = LocalDate.parse("2026-06-01"))
    }

    @Test
    fun `initiate calcula el neto esperado de la guia`() = runBlocking {
        val result = repository.initiate("SALE-SEED-0001", "PROD-0025", 2)

        assertTrue(result is AppResult.Success)
        val dp = (result as AppResult.Success).data
        assertEquals(345.00, dp.grossAmount, DELTA)
        // POL-022 (12%) le gana a POL-025 (5%): se elige la MEJOR política.
        assertEquals("POL-022", dp.policyId)
        assertEquals(41.40, dp.discountAmount, DELTA)
        assertEquals(303.60, dp.netAmount, DELTA)
        assertEquals("INITIATED", dp.status)
        assertEquals("PEN", dp.currency)
        assertEquals("BRANCH", dp.channel)
        assertEquals("PARTY-000024", dp.customerReference)
        assertNull(dp.paymentOrderId)
    }

    @Test
    fun `initiate sin politica aplicable deja policy NONE y neto igual al bruto`() = runBlocking {
        // PROD-0051 es USD y todas las políticas sembradas son PEN.
        val result = repository.initiate("SALE-SEED-0001", "PROD-0051", 1)

        assertTrue(result is AppResult.Success)
        val dp = (result as AppResult.Success).data
        assertEquals("NONE", dp.policyId)
        assertEquals(0.0, dp.discountAmount, DELTA)
        assertEquals(dp.grossAmount, dp.netAmount, DELTA)
        assertEquals(25.00, dp.netAmount, DELTA)
    }

    @Test
    fun `initiate rechaza cantidad cero`() = runBlocking {
        val result = repository.initiate("SALE-SEED-0001", "PROD-0025", 0)

        assertTrue(result is AppResult.Failure)
        val error = (result as AppResult.Failure).error
        assertTrue(error is AppError.BadRequest)
        assertTrue((error as AppError.BadRequest).msg.contains("quantity"))
    }

    @Test
    fun `initiate rechaza producto descontinuado`() = runBlocking {
        val result = repository.initiate("SALE-SEED-0001", "PROD-0008", 1)

        assertTrue(result is AppResult.Failure)
        val error = (result as AppResult.Failure).error
        assertTrue(error is AppError.BadRequest)
        assertTrue((error as AppError.BadRequest).msg.contains("no está disponible"))
    }

    @Test
    fun `initiate devuelve NotFound si la operacion no existe`() = runBlocking {
        val result = repository.initiate("SALE-INEXISTENTE", "PROD-0025", 1)

        assertTrue(result is AppResult.Failure)
        assertTrue((result as AppResult.Failure).error is AppError.NotFound)
    }

    @Test
    fun `get devuelve el descuento guardado o NotFound`() = runBlocking {
        val created = (repository.initiate("SALE-SEED-0001", "PROD-0025", 1) as AppResult.Success).data

        val found = repository.get(created.discountPricingId)
        assertTrue(found is AppResult.Success)
        assertEquals(created.discountPricingId, (found as AppResult.Success).data.discountPricingId)

        val missing = repository.get("DP-NO-EXISTE")
        assertTrue(missing is AppResult.Failure)
        assertTrue((missing as AppResult.Failure).error is AppError.NotFound)
    }

    @Test
    fun `update a APPLIED crea el pago por el neto`() = runBlocking {
        val created = (repository.initiate("SALE-SEED-0001", "PROD-0025", 2) as AppResult.Success).data
        val pagosAntes = MockStore.payments.size

        val result = repository.update(created.discountPricingId, "APPLIED", "TARJETA_DEBITO")

        assertTrue(result is AppResult.Success)
        val updated = (result as AppResult.Success).data
        assertEquals("APPLIED", updated.status)
        assertNotNull(updated.paymentOrderId)

        val payment = MockStore.payments.first { it.paymentOrderId == updated.paymentOrderId }
        assertEquals(created.discountPricingId, payment.discountPricingId)
        assertEquals(303.60, payment.amount, DELTA)
        assertEquals("TARJETA_DEBITO", payment.paymentMethod)
        assertEquals("PAID", payment.status)
        assertEquals(pagosAntes + 1, MockStore.payments.size)

        // El descuento quedó guardado: get devuelve ahora el APPLIED.
        val reload = (repository.get(created.discountPricingId) as AppResult.Success).data
        assertEquals("APPLIED", reload.status)
    }

    @Test
    fun `update dos veces devuelve Conflict`() = runBlocking {
        val created = (repository.initiate("SALE-SEED-0001", "PROD-0025", 1) as AppResult.Success).data
        repository.update(created.discountPricingId, "APPLIED", null)

        val second = repository.update(created.discountPricingId, "APPLIED", null)

        assertTrue(second is AppResult.Failure)
        assertTrue((second as AppResult.Failure).error is AppError.Conflict)
    }

    @Test
    fun `update a CANCELLED no crea pago`() = runBlocking {
        val created = (repository.initiate("SALE-SEED-0001", "PROD-0025", 2) as AppResult.Success).data
        val pagosAntes = MockStore.payments.size

        val result = repository.update(created.discountPricingId, "CANCELLED", null)

        assertTrue(result is AppResult.Success)
        val updated = (result as AppResult.Success).data
        assertEquals("CANCELLED", updated.status)
        assertNull(updated.paymentOrderId)
        assertEquals(pagosAntes, MockStore.payments.size)
    }

    private companion object {
        const val DELTA = 0.001
    }
}
