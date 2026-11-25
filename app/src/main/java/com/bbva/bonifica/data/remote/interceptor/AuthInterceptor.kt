package com.bbva.bonifica.data.remote.interceptor

import com.bbva.bonifica.data.auth.SessionManager
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Interceptor para peticiones a API Gateway.
 * Si existe una sesión activa, inyecta la cabecera 'Authorization: <idToken>'.
 */
class AuthInterceptor(
    private val sessionManager: SessionManager
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val idToken = sessionManager.current()?.idToken

        val request = if (!idToken.isNullOrBlank()) {
            original.newBuilder()
                .header("Authorization", idToken)
                .build()
        } else {
            original
        }

        return chain.proceed(request)
    }
}
