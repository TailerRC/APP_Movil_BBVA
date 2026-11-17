package com.bbva.bonifica.ui.acceso

import com.bbva.bonifica.core.AppError
import com.bbva.bonifica.domain.model.Role

/** Error de validación de un campo del login (el texto vive en strings.xml). */
enum class FieldError { REQUIRED, INVALID_EMAIL }

/** Estado del formulario de login (§12). */
data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val emailError: FieldError? = null,
    val passwordError: FieldError? = null,
    val generalError: AppError? = null,
    val passwordVisible: Boolean = false,
    val submitting: Boolean = false
)

/** Reto `NEW_PASSWORD_REQUIRED` de Cognito (§5, paso 2). */
data class PendingChallenge(val email: String, val cognitoSession: String)

/** Eventos de una sola vez del login (§10). */
sealed interface AccesoEvent {
    data class OpenHome(val role: Role) : AccesoEvent
    data object OpenNewPassword : AccesoEvent
}
