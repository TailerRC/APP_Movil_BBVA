package com.bbva.bonifica.data.mock

import com.bbva.bonifica.core.AppError
import com.bbva.bonifica.core.AppResult
import com.bbva.bonifica.domain.model.Customer
import com.bbva.bonifica.domain.model.PointOfSale
import com.bbva.bonifica.domain.repository.CustomerRepository
import kotlinx.coroutines.delay

/** Ejemplo ya funcional: úsalo como referencia para los demás Fake*. */
class FakeCustomerRepository : CustomerRepository {
    override suspend fun getCustomer(ref: String): AppResult<Customer> {
        delay(500)
        // TODO: devolver Forbidden si la sesión es de cliente y ref != su customerReference (§14)
        return if (ref == MockData.customer.customerReference) AppResult.Success(MockData.customer)
        else AppResult.Failure(AppError.NotFound("el cliente $ref"))
    }

    override suspend fun getOperations(customerRef: String): AppResult<List<PointOfSale>> {
        delay(500)
        return AppResult.Success(MockData.operations.filter { it.customerReference == customerRef })
    }
}
