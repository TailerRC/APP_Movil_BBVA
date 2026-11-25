package com.bbva.bonifica.ui.acceso

import com.bbva.bonifica.core.AppError
import com.bbva.bonifica.domain.model.Role

/** Estado del formulario de nueva contraseña (§12). */
data class NuevaContrasenaUiState(
    val password: String = "",
    val confirm: String = "",
    val passwordVisible: Boolean = false,
    /** Cada intento de guardar incrementa este contador: activa los avisos de
     *  requisitos y coincidencia y reaplica el foco al primer campo inválido. */
    val attemptId: Int = 0,
    val saving: Boolean = false,
    val generalError: AppError? = null
) {
    val attempted: Boolean get() = attemptId > 0

    /** true si lo escrito no coincide con la confirmación. */
    val mismatch: Boolean get() = attempted && password != confirm
}

/** Eventos de una sola vez de la pantalla (§10). */
sealed interface NuevaContrasenaEvent {
    data class OpenHome(val role: Role) : NuevaContrasenaEvent
}
