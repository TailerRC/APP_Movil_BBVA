package com.bbva.bonifica.ui.acceso

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bbva.bonifica.core.AppResult
import com.bbva.bonifica.domain.repository.AuthRepository
import com.bbva.bonifica.domain.repository.LoginOutcome
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel del flujo de acceso (§5): login y reto de nueva contraseña.
 *
 * Su scope es el de la Activity (se crea fuera del NavHost) para conservar lo que
 * escribió el usuario y el `cognitoSession` pendiente al ir de Login a
 * Nueva contraseña y volver.
 */
class AccesoViewModel(private val auth: AuthRepository) : ViewModel() {

    private val _state = MutableStateFlow(LoginUiState())
    val state: StateFlow<LoginUiState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<AccesoEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<AccesoEvent> = _events.asSharedFlow()

    /** Reto pendiente; lo consume NuevaContrasenaScreen (§5, paso 2). */
    var pendingChallenge: PendingChallenge? = null
        private set

    fun onEmailChange(value: String) {
        _state.update { it.copy(email = value, emailError = null, generalError = null) }
    }

    fun onPasswordChange(value: String) {
        _state.update { it.copy(password = value, passwordError = null, generalError = null) }
    }

    fun togglePasswordVisibility() {
        _state.update { it.copy(passwordVisible = !it.passwordVisible) }
    }

    fun submit() {
        val current = _state.value
        val emailError = when {
            current.email.isBlank() -> FieldError.REQUIRED
            !EMAIL_REGEX.matches(current.email.trim()) -> FieldError.INVALID_EMAIL
            else -> null
        }
        val passwordError = if (current.password.isBlank()) FieldError.REQUIRED else null
        if (emailError != null || passwordError != null) {
            _state.update { it.copy(emailError = emailError, passwordError = passwordError) }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(submitting = true, generalError = null) }
            val email = current.email.trim()
            when (val result = auth.login(email, current.password)) {
                is AppResult.Success -> {
                    // La contraseña no se conserva en memoria tras entrar (§5).
                    _state.update { it.copy(submitting = false, password = "") }
                    when (val outcome = result.data) {
                        is LoginOutcome.Ok -> _events.emit(AccesoEvent.OpenHome(outcome.s.role))
                        is LoginOutcome.NewPasswordRequired -> {
                            pendingChallenge = PendingChallenge(email, outcome.cognitoSession)
                            _events.emit(AccesoEvent.OpenNewPassword)
                        }
                    }
                }
                is AppResult.Failure -> _state.update {
                    it.copy(submitting = false, generalError = result.error)
                }
            }
        }
    }

    /** Limpia el formulario y el reto al cerrar sesión (§5). */
    fun reset() {
        pendingChallenge = null
        _state.value = LoginUiState()
    }

    /** El reto ya se resolvió: no debe volver a consumirse. */
    fun clearChallenge() {
        pendingChallenge = null
    }

    private companion object {
        val EMAIL_REGEX = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
    }
}
