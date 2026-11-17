package com.bbva.bonifica.data.auth

import com.bbva.bonifica.domain.model.Role
import org.json.JSONObject
import java.util.Base64

/** Claims que la app necesita del IdToken (§5, paso 4). */
data class JwtClaims(
    val email: String?,
    val role: Role?,
    val customerReference: String?,
    val expiresAtEpochSec: Long
)

/**
 * Decodifica el payload (segunda parte) de un JWT: Base64 URL sin relleno.
 * En la app real el token lo emite Cognito; en mock lo genera FakeAuthRepository
 * con el mismo formato, así el flujo recorre el mismo camino (§5, paso 4).
 */
object JwtDecoder {

    fun decode(idToken: String): JwtClaims? {
        val parts = idToken.split(".")
        if (parts.size != 3) return null

        val payload = runCatching {
            // java.util.Base64 está disponible desde API 26 (minSdk del proyecto).
            String(Base64.getUrlDecoder().decode(parts[1]), Charsets.UTF_8)
        }.getOrNull() ?: return null

        return runCatching {
            val json = JSONObject(payload)
            JwtClaims(
                email = json.stringOrNull("email"),
                role = json.roleFromGroups(),
                customerReference = json.stringOrNull("custom:customerReference"),
                expiresAtEpochSec = if (json.has("exp")) json.getLong("exp") else 0L
            )
        }.getOrNull()
    }

    private fun JSONObject.stringOrNull(key: String): String? =
        if (has(key) && !isNull(key)) optString(key).takeIf { it.isNotEmpty() } else null

    /** El rol sale SOLO del claim `cognito:groups` (§11.2). */
    private fun JSONObject.roleFromGroups(): Role? {
        val groups = optJSONArray("cognito:groups") ?: return null
        for (i in 0 until groups.length()) {
            when (groups.optString(i).lowercase()) {
                "cliente" -> return Role.CLIENTE
                "negocio" -> return Role.NEGOCIO
            }
        }
        return null
    }
}
