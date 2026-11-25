package com.bbva.bonifica.data.mock

import com.bbva.bonifica.core.AppError
import com.bbva.bonifica.core.AppResult
import com.bbva.bonifica.core.toMessage
import com.bbva.bonifica.domain.model.NewPolicy
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Registro de políticas (Módulo 7, ruta 5): primera vez 201 con la política
 * `ACTIVE` y guardada en `MockStore`, mismo `policyId` otra vez 409
 * `Conflict("La política ya existe")` y 400 si el formato o el valor no valen.
 */
class FakeAdminRepositoryTest {

    private lateinit var repository: FakeAdminRepository

    @Before
    fun setUp() {
        MockStore.reset() // cada prueba parte de las semillas de §14
        repository = FakeAdminRepository()
    }

    private fun nueva(policyId: String = "POL-900", discountValue: Double = 10.0) = NewPolicy(
        policyId = policyId,
        name = "10% en cuentas",
        discountType = "PERCENTAGE",
        discountValue = discountValue,
        scope = "CATEGORY",
        scopeReference = "CUENTAS",
        minimumAmount = 50.0,
        currency = "PEN",
        channel = "ALL",
        validFrom = "2026-01-01",
        validTo = "2026-12-31"
    )

    @Test
    fun `registrar politica devuelve 201 con estado ACTIVE`() = runBlocking {
        val result = repository.registerPolicy(nueva())

        assertTrue(result is AppResult.Success)
        val policy = (result as AppResult.Success).data
        assertEquals("POL-900", policy.policyId)
        assertEquals("10% en cuentas", policy.name)
        assertEquals("ACTIVE", policy.status)
        assertEquals("CUENTAS", policy.scopeReference)
        assertTrue(MockStore.policies.any { it.policyId == "POL-900" })
    }

    @Test
    fun `mismo policyId dos veces devuelve Conflict por politica existente`() = runBlocking {
        repository.registerPolicy(nueva())

        val second = repository.registerPolicy(nueva())

        assertTrue(second is AppResult.Failure)
        val error = (second as AppResult.Failure).error
        assertTrue(error is AppError.Conflict)
        assertEquals("La política ya existe", (error as AppError.Conflict).msg)
        assertEquals("La política ya existe", error.toMessage())
    }

    @Test
    fun `el 409 no duplica la politica en la lista`() = runBlocking {
        repository.registerPolicy(nueva())
        repository.registerPolicy(nueva())

        assertEquals(1, MockStore.policies.count { it.policyId == "POL-900" })
    }

    @Test
    fun `policyId con formato invalido devuelve BadRequest`() = runBlocking {
        val result = repository.registerPolicy(nueva(policyId = "PROMO-1"))

        assertTrue(result is AppResult.Failure)
        assertTrue((result as AppResult.Failure).error is AppError.BadRequest)
    }

    @Test
    fun `valor no positivo devuelve BadRequest`() = runBlocking {
        val result = repository.registerPolicy(nueva(discountValue = 0.0))

        assertTrue(result is AppResult.Failure)
        assertTrue((result as AppResult.Failure).error is AppError.BadRequest)
    }

    @Test
    fun `porcentaje mayor a cien devuelve BadRequest`() = runBlocking {
        val result = repository.registerPolicy(nueva(discountValue = 150.0))

        assertTrue(result is AppResult.Failure)
        assertTrue((result as AppResult.Failure).error is AppError.BadRequest)
    }

    @Test
    fun `la politica registrada aparece en la ruta 6`() = runBlocking {
        repository.registerPolicy(nueva())

        val active = FakePromotionRepository().getActivePolicies()

        assertTrue(active is AppResult.Success)
        assertTrue((active as AppResult.Success).data.any { it.policyId == "POL-900" })
    }
}
