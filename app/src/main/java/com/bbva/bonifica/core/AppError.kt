package com.bbva.bonifica.core

sealed class AppError {
    data class BadRequest(val msg: String) : AppError()
    data object Unauthorized : AppError()
    data class Forbidden(val msg: String = "") : AppError()
    data class NotFound(val detail: String = "") : AppError()
    /**
     * 409. `msg` da el motivo concreto: el descuento ya procesado (§8.4) o la
     * política que ya existe (§12, Módulo 7); el texto por defecto es el de §8.4.
     */
    data class Conflict(val msg: String = "Este descuento ya fue confirmado o anulado") : AppError()
    data object Server : AppError()
    data object Network : AppError()
    /** Credenciales rechazadas por Cognito (§5): no distingue usuario inexistente de contraseña errónea. */
    data object InvalidCredentials : AppError()
    /** Contraseña que no cumple la política de Cognito (§5). */
    data object InvalidPassword : AppError()
}

/** Único lugar donde se convierte un error en texto para el usuario (§8.4). */
fun AppError.toMessage(): String = when (this) {
    is AppError.BadRequest -> msg
    AppError.Unauthorized -> "Tu sesión venció"
    is AppError.Forbidden -> "No tienes permiso: solo puedes consultar tus propios datos"
    is AppError.NotFound -> if (detail.isBlank()) "No se encontró el recurso" else "No se encontró $detail"
    is AppError.Conflict -> msg
    AppError.Server -> "Error interno. Intenta más tarde"
    AppError.Network -> "Sin conexión a internet"
    AppError.InvalidCredentials -> "Correo o contraseña incorrectos"
    AppError.InvalidPassword -> "La contraseña no cumple: mín. 8, mayúscula, minúscula y número"
}
