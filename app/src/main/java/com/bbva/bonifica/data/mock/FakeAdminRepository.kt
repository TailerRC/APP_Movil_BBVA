package com.bbva.bonifica.data.mock

import com.bbva.bonifica.core.AppResult
import com.bbva.bonifica.domain.model.NewPolicy
import com.bbva.bonifica.domain.model.Policy
import com.bbva.bonifica.domain.repository.AdminRepository

/** TODO (Persona F): ruta 5. Primera vez 201, mismo policyId otra vez -> Conflict (409). */
class FakeAdminRepository : AdminRepository {
    override suspend fun registerPolicy(p: NewPolicy): AppResult<Policy> =
        TODO("Persona F: implementar")
}
