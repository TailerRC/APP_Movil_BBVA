package com.bbva.bonifica.data.auth

import android.content.Context
import androidx.core.content.edit

/**
 * Almacén persistido de la sesión (§5, paso 5).
 * Con AWS real aquí vive el refresh token cifrado (EncryptedSharedPreferences,
 * ver §16); el IdToken y los datos de la sesión solo viven en memoria.
 */
interface TokenStore {
    fun read(): String?
    fun write(value: String)
    fun clear()
}

/**
 * Preferencias privadas de la app. Al integrar AWS se sustituye la implementación
 * por `EncryptedSharedPreferences` (diseño §4.3); la interfaz no cambia.
 */
class PrefsTokenStore(context: Context) : TokenStore {

    private val prefs =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    override fun read(): String? = prefs.getString(KEY_TOKEN, null)

    override fun write(value: String) {
        prefs.edit { putString(KEY_TOKEN, value) }
    }

    override fun clear() {
        prefs.edit { remove(KEY_TOKEN) }
    }

    private companion object {
        const val PREFS_NAME = "bonifica_session"
        const val KEY_TOKEN = "token"
    }
}
