package com.bbva.bonifica.core

sealed class AppError {
    data class BadRequest(val msg: String) : AppError()
    data object Unauthorized : AppError()
    data class Forbidden(val msg: String = "") : AppError()
    data class NotFound(val detail: String = "") : AppError()
    data object Conflict : AppError()
    data object Server : AppError()
    data object Network : AppError()
}

/** Único lugar donde se convierte un error en texto para el usuario (§8.4). */
fun AppError.toMessage(): String = when (this) {
    is AppError.BadRequest -> msg
    AppError.Unauthorized -> "Tu sesión venció"
    is AppError.Forbidden -> "No tienes permiso: solo puedes consultar tus propios datos"
    is AppError.NotFound -> if (detail.isBlank()) "No se encontró el recurso" else "No se encontró $detail"
    AppError.Conflict -> "Este descuento ya fue confirmado o anulado"
    AppError.Server -> "Error interno. Intenta más tarde"
    AppError.Network -> "Sin conexión a internet"
}
