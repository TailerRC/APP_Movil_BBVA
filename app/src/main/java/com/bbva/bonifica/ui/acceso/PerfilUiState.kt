package com.bbva.bonifica.ui.acceso

import com.bbva.bonifica.core.UiState
import com.bbva.bonifica.domain.model.Customer
import com.bbva.bonifica.domain.model.Role

/**
 * Estado de la pantalla de perfil: se reutiliza el `UiState` genérico de §10
 * (Loading / Content / Empty / Error), porque el perfil es un solo objeto.
 */
typealias PerfilUiState = UiState<Customer>

/** Eventos de una sola vez del perfil (§10). */
sealed interface PerfilEvent {
    data class LoggedOut(val role: Role) : PerfilEvent
}
