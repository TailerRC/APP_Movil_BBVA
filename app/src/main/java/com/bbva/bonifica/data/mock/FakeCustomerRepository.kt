package com.bbva.bonifica.data.mock

import com.bbva.bonifica.core.AppError
import com.bbva.bonifica.core.AppResult
import com.bbva.bonifica.domain.model.Customer
import com.bbva.bonifica.domain.model.PointOfSale
import com.bbva.bonifica.domain.model.Role
import com.bbva.bonifica.domain.model.Session
import com.bbva.bonifica.domain.repository.CustomerRepository
import kotlinx.coroutines.delay

/**
 * Ficha de cliente y operaciones (rutas 11 y 14).
 *
 * Ejemplo ya funcional: úsalo como referencia para los demás Fake*.
 *
 * Regla de permisos (§14): un token de **cliente** solo consulta lo suyo
 * (`ref == customerReference`); un token de **negocio**, cualquiera. En cualquier
 * otro caso responde `Forbidden` (403) antes de mirar los datos, igual que hará
 * API Gateway con el backend real.
 */
class FakeCustomerRepository(private val session: () -> Session?) : CustomerRepository {

    /** true si la sesión activa no tiene permiso para consultar `ref` (403). */
    private fun prohibido(ref: String): Boolean {
        val current = session() ?: return false // sin sesión no hay identidad que comparar
        return current.role == Role.CLIENTE && current.customerReference != ref
    }

    override suspend fun getCustomer(ref: String): AppResult<Customer> {
        delay(500)
        if (prohibido(ref)) return AppResult.Failure(AppError.Forbidden())
        return if (ref == MockData.customer.customerReference) AppResult.Success(MockData.customer)
        else AppResult.Failure(AppError.NotFound("el cliente $ref"))
    }

    override suspend fun getOperations(customerRef: String): AppResult<List<PointOfSale>> {
        delay(500)
        if (prohibido(customerRef)) return AppResult.Failure(AppError.Forbidden())
        return AppResult.Success(MockData.operations.filter { it.customerReference == customerRef })
    }
}
