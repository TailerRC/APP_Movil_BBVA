package com.bbva.bonifica.data.mock

import com.bbva.bonifica.core.AppError
import com.bbva.bonifica.core.AppResult
import com.bbva.bonifica.domain.model.Role
import com.bbva.bonifica.domain.model.Session
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Regla 403 de §14: cliente solo consulta lo suyo; negocio, cualquiera.
 * Sin sesión no se compara identidad (el RoleGuard de la UI ya acota el acceso).
 */
class FakeCustomerRepositoryTest {

    private var session: Session? = null
    private val repository = FakeCustomerRepository { session }

    private fun cliente(ref: String?) = Session(
        email = "cliente1@example.com", role = Role.CLIENTE,
        customerReference = ref, idToken = "token", expiresAtEpochSec = Long.MAX_VALUE
    )

    private fun negocio() = Session(
        email = "negocio1@example.com", role = Role.NEGOCIO,
        customerReference = null, idToken = "token", expiresAtEpochSec = Long.MAX_VALUE
    )

    // --- Ruta 11 · getCustomer ---

    @Test
    fun `cliente con su propio ref obtiene su ficha`() = runBlocking {
        session = cliente(MockData.CLIENTE_REF)

        val result = repository.getCustomer(MockData.CLIENTE_REF)

        assertTrue(result is AppResult.Success)
        assertEquals(MockData.CLIENTE_REF, (result as AppResult.Success).data.customerReference)
    }

    @Test
    fun `cliente con ref ajeno recibe Forbidden`() = runBlocking {
        session = cliente(MockData.CLIENTE_REF)

        val result = repository.getCustomer(MockData.CLIENTE_AJENO_REF)

        assertTrue(result is AppResult.Failure)
        assertTrue((result as AppResult.Failure).error is AppError.Forbidden)
    }

    @Test
    fun `negocio consulta cualquier ficha`() = runBlocking {
        session = negocio()

        val result = repository.getCustomer(MockData.CLIENTE_REF)

        assertTrue(result is AppResult.Success)
    }

    @Test
    fun `ref inexistente sin sesion ajena devuelve NotFound`() = runBlocking {
        session = negocio()

        val result = repository.getCustomer("PARTY-999999")

        assertTrue(result is AppResult.Failure)
        assertTrue((result as AppResult.Failure).error is AppError.NotFound)
    }

    // --- Ruta 14 · getOperations ---

    @Test
    fun `cliente obtiene sus operaciones`() = runBlocking {
        session = cliente(MockData.CLIENTE_REF)

        val result = repository.getOperations(MockData.CLIENTE_REF)

        assertTrue(result is AppResult.Success)
        val ops = (result as AppResult.Success).data
        assertTrue(ops.isNotEmpty())
        assertTrue(ops.all { it.customerReference == MockData.CLIENTE_REF })
    }

    @Test
    fun `cliente con operaciones ajenas recibe Forbidden`() = runBlocking {
        session = cliente(MockData.CLIENTE_REF)

        val result = repository.getOperations(MockData.CLIENTE_AJENO_REF)

        assertTrue(result is AppResult.Failure)
        assertTrue((result as AppResult.Failure).error is AppError.Forbidden)
    }

    @Test
    fun `negocio consulta operaciones de cualquier cliente`() = runBlocking {
        session = negocio()

        val result = repository.getOperations(MockData.CLIENTE_REF)

        assertTrue(result is AppResult.Success)
        assertTrue((result as AppResult.Success).data.isNotEmpty())
    }

    @Test
    fun `sin sesion no se aplica la regla de 403`() = runBlocking {
        session = null

        val result = repository.getOperations(MockData.CLIENTE_AJENO_REF)

        assertTrue(result is AppResult.Success)
        assertTrue((result as AppResult.Success).data.isEmpty())
    }
}
