package com.bbva.bonifica.data.mock

import com.bbva.bonifica.core.AppResult
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Pruebas de Promociones (Módulo 4, rutas 6 y 7): la ruta 6 devuelve solo
 * políticas `ACTIVE` (ocultar las vencidas es regla de pantalla, §12) y la 7
 * filtra campañas por `status`.
 */
class FakePromotionRepositoryTest {

    private lateinit var repository: FakePromotionRepository

    @Before
    fun setUp() {
        repository = FakePromotionRepository()
    }

    @Test
    fun `getActivePolicies devuelve solo politicas ACTIVE`() = runBlocking {
        val result = repository.getActivePolicies()

        assertTrue(result is AppResult.Success)
        val policies = (result as AppResult.Success).data
        assertTrue(policies.isNotEmpty())
        assertTrue(policies.all { it.status == "ACTIVE" })
        assertTrue(policies.any { it.policyId == "POL-022" })
        assertTrue(policies.any { it.policyId == "POL-025" })
        // POL-030 está INACTIVE: la ruta 6 no debe devolverla.
        assertFalse(policies.any { it.policyId == "POL-030" })
        // POL-018 sigue ACTIVE aunque esté vencida: ocultarla es trabajo de la UI.
        assertTrue(policies.any { it.policyId == "POL-018" })
    }

    @Test
    fun `getCampaigns devuelve solo las del estado pedido`() = runBlocking {
        val active = (repository.getCampaigns("ACTIVE") as AppResult.Success).data
        assertTrue(active.isNotEmpty())
        assertTrue(active.all { it.status == "ACTIVE" })
        assertTrue(active.any { it.campaignId == "CMP-001" })

        val finished = (repository.getCampaigns("FINISHED") as AppResult.Success).data
        assertTrue(finished.isNotEmpty())
        assertTrue(finished.all { it.status == "FINISHED" })
        assertTrue(finished.any { it.campaignId == "CMP-003" })
    }

    @Test
    fun `getCampaigns cubre los cuatro estados del selector`() = runBlocking {
        listOf("ACTIVE", "SCHEDULED", "FINISHED", "PAUSED").forEach { status ->
            val result = repository.getCampaigns(status)
            assertTrue(result is AppResult.Success)
            assertTrue("Faltan campañas $status", (result as AppResult.Success).data.isNotEmpty())
        }
    }

    @Test
    fun `getCampaigns con estado desconocido devuelve lista vacia`() = runBlocking {
        val result = repository.getCampaigns("OTRO")

        assertTrue(result is AppResult.Success)
        assertEquals(0, (result as AppResult.Success).data.size)
    }
}
