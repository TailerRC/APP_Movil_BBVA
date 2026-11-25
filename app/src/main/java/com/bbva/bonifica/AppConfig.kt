package com.bbva.bonifica

/** ÚNICO archivo que se edita al integrar AWS (ver diseño §4.4 y §16). */
object AppConfig {
    /** true = datos simulados (sin internet). false = AWS real. */
    const val USE_MOCK = false

    // Se rellenan al integrar (valores de apigateway.md y cognito.md)
    const val BASE_URL = "https://d2ffu5p3zf.execute-api.us-east-2.amazonaws.com/dev/" // con "/" al final
    const val COGNITO_URL = "https://cognito-idp.us-east-2.amazonaws.com/"
    const val COGNITO_CLIENT_ID = "64bh9vor0cja0rrh4rhkpefnug" // CLIENT_ID del app client (sin secreto)

    const val TOKEN_REFRESH_MARGIN_SECONDS = 300 // renovar 5 min antes de vencer
}
