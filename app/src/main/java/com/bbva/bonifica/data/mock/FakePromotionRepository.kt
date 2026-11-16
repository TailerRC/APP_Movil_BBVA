package com.bbva.bonifica.data.mock

import com.bbva.bonifica.core.AppResult
import com.bbva.bonifica.domain.model.Campaign
import com.bbva.bonifica.domain.model.Policy
import com.bbva.bonifica.domain.repository.PromotionRepository
import kotlinx.coroutines.delay

/** Políticas ya funcionan con MockData. TODO (Persona E): campañas. */
class FakePromotionRepository : PromotionRepository {
    override suspend fun getActivePolicies(): AppResult<List<Policy>> {
        delay(500)
        return AppResult.Success(MockData.policies)
    }

    override suspend fun getCampaigns(status: String): AppResult<List<Campaign>> =
        TODO("Persona E: implementar")
}
