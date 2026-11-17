package com.bbva.bonifica.ui.acceso

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bbva.bonifica.core.AppResult
import com.bbva.bonifica.core.UiState
import com.bbva.bonifica.domain.model.Role
import com.bbva.bonifica.domain.repository.AuthRepository
import com.bbva.bonifica.domain.repository.CustomerRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Perfil del cliente (§12): ruta 11 con el `customerReference` del token y
 * cierre de sesión.
 */
class PerfilViewModel(
    private val auth: AuthRepository,
    private val customerRepository: CustomerRepository
) : ViewModel() {

    private val _state = MutableStateFlow<PerfilUiState>(UiState.Loading)
    val state: StateFlow<PerfilUiState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<PerfilEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<PerfilEvent> = _events.asSharedFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.value = UiState.Loading
            val customerRef = auth.currentSession()?.customerReference
            if (customerRef == null) {
                // Una cuenta de negocio no tiene ficha de cliente (§11.2).
                _state.value = UiState.Empty
                return@launch
            }
            _state.value = when (val result = customerRepository.getCustomer(customerRef)) {
                is AppResult.Success -> UiState.Content(result.data)
                is AppResult.Failure -> UiState.Error(result.error)
            }
        }
    }

    fun logout() {
        val role = auth.currentSession()?.role ?: Role.CLIENTE
        auth.logout()
        _events.tryEmit(PerfilEvent.LoggedOut(role))
    }
}
