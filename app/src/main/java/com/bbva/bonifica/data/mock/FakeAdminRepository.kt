package com.bbva.bonifica.data.mock

import com.bbva.bonifica.core.AppError
import com.bbva.bonifica.core.AppResult
import com.bbva.bonifica.domain.model.NewPolicy
import com.bbva.bonifica.domain.model.Policy
import com.bbva.bonifica.domain.repository.AdminRepository
import kotlinx.coroutines.delay

/**
 * Registro de políticas (Módulo 7, ruta 5).
 *
 * Réplica de lo que hará la Lambda `Policy/Register`:
 *  - 400 si el formato o los valores no son válidos (aquí se comprueba lo
 *    esencial; la validación completa vive en el formulario, §12);
 *  - 409 `Conflict("La política ya existe")` si el `policyId` ya está en uso;
 *  - 201 con la política `ACTIVE`, que queda en [MockStore.policies] para que
 *    la ruta 6 la devuelva en Promociones.
 */
class FakeAdminRepository : AdminRepository {

    override suspend fun registerPolicy(p: NewPolicy): AppResult<Policy> {
        delay(500)

        if (!POLICY_ID_REGEX.matches(p.policyId)) {
            return AppResult.Failure(AppError.BadRequest("policyId debe tener formato POL-###"))
        }
        if (p.discountValue <= 0.0) {
            return AppResult.Failure(AppError.BadRequest("discountValue debe ser mayor que cero"))
        }
        if (p.discountType == "PERCENTAGE" && p.discountValue > 100.0) {
            return AppResult.Failure(AppError.BadRequest("discountValue porcentual no puede pasar de 100"))
        }
        if (MockStore.policies.any { it.policyId == p.policyId }) {
            return AppResult.Failure(AppError.Conflict("La política ya existe"))
        }

        val policy = Policy(
            policyId = p.policyId,
            name = p.name,
            discountType = p.discountType,
            discountValue = p.discountValue,
            scope = p.scope,
            scopeReference = p.scopeReference,
            minimumAmount = p.minimumAmount,
            currency = p.currency,
            channel = p.channel,
            validFrom = p.validFrom,
            validTo = p.validTo,
            status = "ACTIVE"
        )
        MockStore.policies.add(policy)
        return AppResult.Success(policy)
    }

    private companion object {
        val POLICY_ID_REGEX = Regex("POL-\\d{3}")
    }
}
