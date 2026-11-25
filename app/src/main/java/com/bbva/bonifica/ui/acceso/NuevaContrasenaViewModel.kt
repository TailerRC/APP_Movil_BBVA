package com.bbva.bonifica.ui.acceso

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bbva.bonifica.core.AppResult
import com.bbva.bonifica.core.PasswordRules
import com.bbva.bonifica.domain.repository.AuthRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Nueva contraseña obligatoria (§12): valida los requisitos en local y llama a
 * `AuthRepository.setNewPassword` con el `cognitoSession` del reto de §5.
 */
class NuevaContrasenaViewModel(private val auth: AuthRepository) : ViewModel() {

    private val _state = MutableStateFlow(NuevaContrasenaUiState())
    val state: StateFlow<NuevaContrasenaUiState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<NuevaContrasenaEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<NuevaContrasenaEvent> = _events.asSharedFlow()

    fun onPasswordChange(value: String) {
        _state.update { it.copy(password = value, generalError = null) }
    }

    fun onConfirmChange(value: String) {
        _state.update { it.copy(confirm = value, generalError = null) }
    }

    fun togglePasswordVisibility() {
        _state.update { it.copy(passwordVisible = !it.passwordVisible) }
    }

    fun save(challenge: PendingChallenge?) {
        if (challenge == null) return // sin reto no hay nada que enviar; lo avisa la pantalla

        val current = _state.value
        val rulesOk = PasswordRules.isValid(current.password)
        val matches = current.password == current.confirm
        if (!rulesOk || !matches) {
            _state.update { it.copy(attemptId = it.attemptId + 1) }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(saving = true, generalError = null) }
            when (val result = auth.setNewPassword(challenge.email, current.password, challenge.cognitoSession)) {
                is AppResult.Success -> {
                    // La contraseña no queda en memoria tras guardarla (§5).
                    _state.value = NuevaContrasenaUiState()
                    _events.emit(NuevaContrasenaEvent.OpenHome(result.data.role))
                }
                is AppResult.Failure -> _state.update {
                    it.copy(saving = false, generalError = result.error)
                }
            }
        }
    }
}
