package com.bbva.bonifica.data.mock

import com.bbva.bonifica.core.AppResult
import com.bbva.bonifica.domain.model.EventLogEntry
import com.bbva.bonifica.domain.model.PaymentOrder
import com.bbva.bonifica.domain.repository.PaymentRepository

/** TODO (Persona E): rutas 12, 15 y 4. */
class FakePaymentRepository : PaymentRepository {
    override suspend fun getPayment(id: String): AppResult<PaymentOrder> =
        TODO("Persona E: implementar")

    override suspend fun getPaymentsByOperation(posRef: String): AppResult<List<PaymentOrder>> =
        TODO("Persona E: implementar")

    override suspend fun getHistory(discountId: String): AppResult<List<EventLogEntry>> =
        TODO("Persona E: implementar")
}
