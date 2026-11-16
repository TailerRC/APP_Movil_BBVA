package com.bbva.bonifica.data.mock

import com.bbva.bonifica.core.AppResult
import com.bbva.bonifica.domain.model.Session
import com.bbva.bonifica.domain.repository.AuthRepository
import com.bbva.bonifica.domain.repository.LoginOutcome

/**
 * TODO (Persona B): con cliente1@example.com devolver primero NewPasswordRequired (una vez),
 * validar contraseña simulada, crear Session según rol (cliente/negocio). Ver §14.
 */
class FakeAuthRepository : AuthRepository {
    private var current: Session? = null

    override suspend fun login(email: String, password: String): AppResult<LoginOutcome> =
        TODO("Persona B: implementar login simulado")

    override suspend fun setNewPassword(email: String, newPassword: String, session: String): AppResult<Session> =
        TODO("Persona B: implementar nueva contraseña simulada")

    override suspend fun restoreSession(): Session? = null

    override suspend fun refresh(): AppResult<Session> =
        TODO("Persona B: implementar refresh simulado")

    override fun currentSession(): Session? = current

    override fun logout() { current = null }
}
