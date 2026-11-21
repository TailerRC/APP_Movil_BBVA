package com.bbva.bonifica.data.mock

import com.bbva.bonifica.core.AppResult
import com.bbva.bonifica.domain.model.Campaign
import com.bbva.bonifica.domain.model.Policy
import com.bbva.bonifica.domain.repository.PromotionRepository
import kotlinx.coroutines.delay

/**
 * Implementación simulada de Promociones (Módulo 4, rutas 6 y 7).
 *
 * La ruta 6 devuelve solo políticas `ACTIVE`; ocultar las vencidas (`validTo` < hoy)
 * es regla de pantalla (§12), no del repositorio. La ruta 7 filtra por `status`.
 *
 * Las políticas se leen de [MockStore.policies] (semillas + las que registra el
 * Módulo 7 por la ruta 5) para que una política nueva aparezca en Promociones.
 */
class FakePromotionRepository : PromotionRepository {

    override suspend fun getActivePolicies(): AppResult<List<Policy>> {
        delay(500)
        return AppResult.Success(MockStore.policies.filter { it.status == "ACTIVE" })
    }

    override suspend fun getCampaigns(status: String): AppResult<List<Campaign>> {
        delay(500)
        // Estado desconocido → lista vacía (la API real devolvería 400 o vacío).
        return AppResult.Success(MockData.campaigns.filter { it.status == status })
    }
}
