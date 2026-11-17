package com.bbva.bonifica.ui.acceso

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bbva.bonifica.domain.model.Role
import com.bbva.bonifica.domain.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Estado del splash (§12): intenta `restoreSession()` y decide adónde ir.
 *
 * Va en un `StateFlow` y no en un `SharedFlow` de eventos porque el resultado se
 * produce en el `init`, antes de que exista ningún colector: el StateFlow guarda
 * el último valor y la pantalla lo lee igualmente.
 */
sealed interface SplashUiState {
    data object Checking : SplashUiState
    data class Logged(val role: Role) : SplashUiState
    data object Anonymous : SplashUiState
}

class SplashViewModel(private val auth: AuthRepository) : ViewModel() {

    private val _state = MutableStateFlow<SplashUiState>(SplashUiState.Checking)
    val state: StateFlow<SplashUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            _state.value = when (val session = auth.restoreSession()) {
                null -> SplashUiState.Anonymous
                else -> SplashUiState.Logged(session.role)
            }
        }
    }
}
