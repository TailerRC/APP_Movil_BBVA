package com.bbva.bonifica.core

import org.junit.Assert.assertEquals
import org.junit.Test

/** Textos de `AppError.toMessage()` (§8.4): un solo lugar para los errores. */
class AppErrorTest {

    @Test
    fun `Conflict sin detalle conserva el mensaje de descuento de la 4`() {
        assertEquals(
            "Este descuento ya fue confirmado o anulado",
            AppError.Conflict().toMessage()
        )
    }

    @Test
    fun `Conflict con detalle muestra el motivo recibido`() {
        // Lo usa el registro de políticas del Módulo 7 (409 de la ruta 5, §12).
        val error = AppError.Conflict("La política ya existe")

        assertEquals("La política ya existe", error.toMessage())
    }
}
