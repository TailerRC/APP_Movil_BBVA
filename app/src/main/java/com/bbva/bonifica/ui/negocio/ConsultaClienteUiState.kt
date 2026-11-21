package com.bbva.bonifica.ui.negocio

import com.bbva.bonifica.core.UiState

/** Error de validación del campo de código (el texto vive en strings.xml). */
enum class CodigoError { REQUIRED, FORMAT }

/**
 * Estado de ConsultaClienteScreen (Módulo 7, ruta `negocio_consulta`).
 *
 * `result` es `null` antes de la primera búsqueda; `Loading` equivale a
 * "buscando" y se refleja también en el estado del botón.
 */
data class ConsultaClienteUiState(
    val code: String = "",
    val codeError: CodigoError? = null,
    /** null: aún no se ha hecho ninguna consulta. */
    val result: UiState<ClienteNegocioData>? = null
)
