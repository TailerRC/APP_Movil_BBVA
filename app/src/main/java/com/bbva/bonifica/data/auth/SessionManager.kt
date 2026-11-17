package com.bbva.bonifica.data.auth

import com.bbva.bonifica.domain.model.Role
import com.bbva.bonifica.domain.model.Session

/** Segundos desde la epoch (UTC). */
internal fun nowEpochSeconds(): Long = System.currentTimeMillis() / 1000

/**
 * Sesión en memoria + token persistido para restaurarla al abrir la app (§5, paso 5).
 *
 * Mock: se persiste el IdToken y la sesión se reconstruye decodificándolo con
 * [JwtDecoder]. Con AWS real (§16) aquí se guarda solo el refresh token cifrado y
 * la sesión se reconstruye con `AuthRepository.refresh()`.
 */
class SessionManager(private val store: TokenStore) {

    @Volatile
    private var cached: Session? = null

    /** Guarda la sesión en memoria y su token en el almacén persistido. */
    fun save(session: Session) {
        cached = session
        store.write(session.idToken)
    }

    /** Sesión actual; si no está en memoria, intenta restaurarla desde el token. */
    fun current(): Session? = cached ?: restore()

    /** Restaura la sesión desde el token persistido; si no sirve, limpia todo. */
    fun restore(): Session? {
        cached?.let { if (!isExpired(it)) return it else clear() }

        val token = store.read() ?: return null
        val claims = JwtDecoder.decode(token)
        val role = claims?.role
        val email = claims?.email
        val expired = claims != null && claims.expiresAtEpochSec <= nowEpochSeconds()
        if (claims == null || role == null || email.isNullOrEmpty() || expired) {
            clear()
            return null
        }
        return Session(
            email = email,
            role = role,
            customerReference = claims.customerReference,
            idToken = token,
            expiresAtEpochSec = claims.expiresAtEpochSec
        ).also { cached = it }
    }

    fun isExpired(session: Session): Boolean = session.expiresAtEpochSec <= nowEpochSeconds()

    fun clear() {
        cached = null
        store.clear()
    }
}
