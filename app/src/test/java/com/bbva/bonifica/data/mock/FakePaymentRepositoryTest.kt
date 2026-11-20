package com.bbva.bonifica.data.mock

import com.bbva.bonifica.core.AppError
import com.bbva.bonifica.core.AppResult
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Pruebas de Pagos e Historial (Módulo 6, rutas 12, 15 y 4).
 *
 * `MockStore.reset()` en cada test: las semillas de [MockData] y lo que cree el
 * Módulo 3 al confirmar comparten el mismo almacén (§14).
 */
class FakePaymentRepositoryTest {

    private lateinit var repository: FakePaymentRepository

    @Before
    fun setUp() {
        MockStore.reset()
        repository = FakePaymentRepository()
    }

    @Test
    fun `getPayment devuelve el pago sembrado`() = runBlocking {
        val result = repository.getPayment("PAY-0001")

        assertTrue(result is AppResult.Success)
        val payment = (result as AppResult.Success).data
        assertEquals("DP-0001", payment.discountPricingId)
        assertEquals(303.60, payment.amount, DELTA)
        assertEquals("PEN", payment.currency)
        assertEquals("CARGO_EN_CUENTA", payment.paymentMethod)
        assertEquals("PAID", payment.status)
        assertEquals("SALE-SEED-0001", payment.pointOfSaleReference)
    }

    @Test
    fun `getPayment devuelve NotFound para pago inexistente`() = runBlocking {
        val result = repository.getPayment("PAY-NO-EXISTE")

        assertTrue(result is AppResult.Failure)
        assertTrue((result as AppResult.Failure).error is AppError.NotFound)
    }

    @Test
    fun `getPaymentsByOperation devuelve solo los pagos de la operacion`() = runBlocking {
        val result = repository.getPaymentsByOperation("SALE-SEED-0001")

        assertTrue(result is AppResult.Success)
        val payments = (result as AppResult.Success).data
        assertTrue(payments.isNotEmpty())
        assertTrue(payments.all { it.pointOfSaleReference == "SALE-SEED-0001" })
        assertTrue(payments.any { it.paymentOrderId == "PAY-0001" })
    }

    @Test
    fun `getPaymentsByOperation devuelve lista vacia si la operacion no tiene pagos`() = runBlocking {
        val result = repository.getPaymentsByOperation("SALE-SEED-0003")

        assertTrue(result is AppResult.Success)
        assertTrue((result as AppResult.Success).data.isEmpty())
    }

    @Test
    fun `getHistory reconstruye INITIATED y APPLIED para un descuento pagado`() = runBlocking {
        val result = repository.getHistory("DP-0001")

        assertTrue(result is AppResult.Success)
        val events = (result as AppResult.Success).data
        assertEquals(2, events.size)
        assertEquals("DISCOUNT_INITIATED", events[0].eventType)
        assertEquals("INITIATED", events[0].newStatus)
        assertEquals(null, events[0].previousStatus)
        assertEquals("DISCOUNT_APPLIED", events[1].eventType)
        assertEquals("INITIATED", events[1].previousStatus)
        assertEquals("APPLIED", events[1].newStatus)
        assertTrue(events[1].details.contains("PAY-0001"))
        events.forEach { assertEquals("DP-0001", it.discountPricingId) }
    }

    @Test
    fun `getHistory de un descuento sin confirmar solo trae INITIATED`() = runBlocking {
        val result = repository.getHistory("DP-0002")

        assertTrue(result is AppResult.Success)
        val events = (result as AppResult.Success).data
        assertEquals(1, events.size)
        assertEquals("DISCOUNT_INITIATED", events[0].eventType)
        assertEquals("INITIATED", events[0].newStatus)
    }

    @Test
    fun `getHistory devuelve NotFound para descuento inexistente`() = runBlocking {
        val result = repository.getHistory("DP-NO-EXISTE")

        assertTrue(result is AppResult.Failure)
        assertTrue((result as AppResult.Failure).error is AppError.NotFound)
    }

    private companion object {
        const val DELTA = 0.001
    }
}
