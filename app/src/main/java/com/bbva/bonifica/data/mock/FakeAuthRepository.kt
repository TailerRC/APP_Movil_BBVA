package com.bbva.bonifica.data.mock

import com.bbva.bonifica.core.AppError
import com.bbva.bonifica.core.AppResult
import com.bbva.bonifica.core.PasswordRules
import com.bbva.bonifica.data.auth.JwtClaims
import com.bbva.bonifica.data.auth.JwtDecoder
import com.bbva.bonifica.data.auth.SessionManager
import com.bbva.bonifica.data.auth.nowEpochSeconds
import com.bbva.bonifica.domain.model.Role
import com.bbva.bonifica.domain.model.Session
import com.bbva.bonifica.domain.repository.AuthRepository
import com.bbva.bonifica.domain.repository.LoginOutcome
import kotlinx.coroutines.delay
import org.json.JSONArray
import org.json.JSONObject
import java.util.Base64
import java.util.UUID
import kotlin.random.Random

/**
 * Login simulado (§14). Reglas:
 *  - 400–800 ms de retardo para ver los estados de carga.
 *  - Solo existen los usuarios de prueba de §14; cualquier otro correo responde
 *    `InvalidCredentials` sin decir cuál de los dos campos falló (§5).
 *  - La contraseña se escribe en el login y **no está escrita en el código**: en
 *    mock se acepta cualquier valor distinto de vacío.
 *  - `cliente1@example.com` pide nueva contraseña la primera vez que entra en
 *    cada arranque, para poder probar `NuevaContrasenaScreen` (§14).
 *
 * El IdToken se fabrica con el mismo formato que el de Cognito y la sesión se
 * deriva de él con [JwtDecoder], igual que hará `RemoteAuthRepository` (§5).
 */
class FakeAuthRepository(private val sessionManager: SessionManager) : AuthRepository {

    private var clientePendienteDePassword = true

    override suspend fun login(email: String, password: String): AppResult<LoginOutcome> {
        delay(Random.nextLong(MIN_DELAY_MS, MAX_DELAY_MS + 1))
        if (password.isBlank()) return AppResult.Failure(AppError.BadRequest("Escribe tu contraseña"))

        val role = roleOf(email) ?: return AppResult.Failure(AppError.InvalidCredentials)

        if (role == Role.CLIENTE && clientePendienteDePassword) {
            clientePendienteDePassword = false
            return AppResult.Success(LoginOutcome.NewPasswordRequired(cognitoSession()))
        }
        return AppResult.Success(LoginOutcome.Ok(newSession(email.trim(), role)))
    }

    override suspend fun setNewPassword(
        email: String,
        newPassword: String,
        session: String
    ): AppResult<Session> {
        delay(Random.nextLong(MIN_DELAY_MS, MAX_DELAY_MS + 1))
        // Sin sesión de reto no se puede responder al desafío de Cognito (§5, paso 2).
        if (session.isBlank()) return AppResult.Failure(AppError.Unauthorized)
        if (!PasswordRules.isValid(newPassword)) return AppResult.Failure(AppError.InvalidPassword)

        val role = roleOf(email) ?: return AppResult.Failure(AppError.InvalidCredentials)
        return AppResult.Success(newSession(email.trim(), role))
    }

    override suspend fun restoreSession(): Session? {
        delay(SPLASH_DELAY_MS) // para que se vea el splash
        return sessionManager.restore()
    }

    override suspend fun refresh(): AppResult<Session> {
        delay(Random.nextLong(MIN_DELAY_MS, MAX_DELAY_MS + 1))
        val current = sessionManager.current() ?: return AppResult.Failure(AppError.Unauthorized)
        if (sessionManager.isExpired(current)) {
            sessionManager.clear()
            return AppResult.Failure(AppError.Unauthorized)
        }
        return AppResult.Success(newSession(current.email, current.role))
    }

    override fun currentSession(): Session? = sessionManager.current()

    override fun logout() {
        sessionManager.clear()
    }

    /** Solo los usuarios de prueba de §14; otro correo no existe en el mock. */
    private fun roleOf(email: String): Role? = when (email.trim().lowercase()) {
        MockData.CLIENTE_EMAIL -> Role.CLIENTE
        MockData.NEGOCIO_EMAIL -> Role.NEGOCIO
        else -> null
    }

    /** Crea la sesión, la guarda y devuelve (§5, paso 5). */
    private fun newSession(email: String, role: Role): Session {
        val claims = JwtClaims(
            email = email,
            role = role,
            customerReference = if (role == Role.CLIENTE) MockData.CLIENTE_REF else null,
            expiresAtEpochSec = nowEpochSeconds() + ACCESS_TOKEN_TTL_SECONDS
        )
        return Session(
            email = claims.email ?: email,
            role = role,
            customerReference = claims.customerReference,
            idToken = fakeIdToken(claims),
            expiresAtEpochSec = claims.expiresAtEpochSec
        ).also { sessionManager.save(it) }
    }

    private fun fakeIdToken(claims: JwtClaims): String {
        val payload = JSONObject().apply {
            put("email", claims.email)
            put(
                "cognito:groups",
                JSONArray().put(if (claims.role == Role.CLIENTE) "cliente" else "negocio")
            )
            claims.customerReference?.let { put("custom:customerReference", it) }
            put("exp", claims.expiresAtEpochSec)
        }.toString()
        val header = """{"alg":"HS256","typ":"JWT"}"""
        return listOf(base64Url(header), base64Url(payload), "mock-signature").joinToString(".")
    }

    private fun base64Url(raw: String): String =
        Base64.getUrlEncoder().withoutPadding().encodeToString(raw.toByteArray(Charsets.UTF_8))

    private fun cognitoSession(): String = "mock-session-" + UUID.randomUUID()

    private companion object {
        const val MIN_DELAY_MS = 400L
        const val MAX_DELAY_MS = 800L
        const val SPLASH_DELAY_MS = 300L
        const val ACCESS_TOKEN_TTL_SECONDS = 3600L
    }
}
