package com.bbva.bonifica.ui.navigation

import com.bbva.bonifica.domain.model.Role
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Visibilidad de la barra inferior por rol (§11.2): se pinta solo en las rutas
 * de sección del rol y nunca en detalle, flujos ni accesos, para que siempre
 * haya una pestaña activa de dónde salir.
 */
class BottomBarsTest {

    @Test
    fun `la barra se pinta en las cuatro secciones del cliente`() {
        listOf(
            Routes.HOME_CLIENTE,
            Routes.CATALOGO,
            Routes.OPERACIONES,
            Routes.PROMOCIONES
        ).forEach { ruta ->
            assertTrue(ruta, barraInferiorVisible(Role.CLIENTE, ruta))
        }
    }

    @Test
    fun `la barra se pinta en las cuatro secciones de negocio`() {
        listOf(
            Routes.NEGOCIO_CONSULTA,
            Routes.NEGOCIO_POLITICA,
            Routes.PROMOCIONES,
            Routes.CATALOGO
        ).forEach { ruta ->
            assertTrue(ruta, barraInferiorVisible(Role.NEGOCIO, ruta))
        }
    }

    @Test
    fun `las subpantallas de una seccion mantienen la barra y su pestana activa`() {
        // Catálogo → categoría → producto; consulta → ficha del cliente.
        assertTrue(barraInferiorVisible(Role.CLIENTE, Routes.CATALOGO_CATEGORIA))
        assertTrue(barraInferiorVisible(Role.CLIENTE, Routes.PRODUCTO))
        assertTrue(barraInferiorVisible(Role.NEGOCIO, Routes.NEGOCIO_CLIENTE))
        // La operación se llega desde las dos secciones según el rol.
        assertTrue(barraInferiorVisible(Role.CLIENTE, Routes.OPERACION))
        assertTrue(barraInferiorVisible(Role.NEGOCIO, Routes.OPERACION))
    }

    @Test
    fun `la barra se oculta en flujos, accesos y pantallas sin pestana propia`() {
        listOf(
            Routes.SPLASH,
            Routes.DEV_MENU,
            Routes.LOGIN,
            Routes.NEW_PASSWORD,
            Routes.CALCULAR,
            Routes.RESULTADO,
            Routes.PAGOS,
            Routes.COMPROBANTE,
            Routes.HISTORIAL
        ).forEach { ruta ->
            assertFalse("cliente: $ruta", barraInferiorVisible(Role.CLIENTE, ruta))
            assertFalse("negocio: $ruta", barraInferiorVisible(Role.NEGOCIO, ruta))
        }
    }

    @Test
    fun `cada rol solo ve sus propias secciones`() {
        // El home de cliente no es una sección de negocio ni viceversa.
        assertFalse(barraInferiorVisible(Role.NEGOCIO, Routes.HOME_CLIENTE))
        assertFalse(barraInferiorVisible(Role.CLIENTE, Routes.NEGOCIO_CONSULTA))
        assertFalse(barraInferiorVisible(Role.CLIENTE, Routes.NEGOCIO_POLITICA))
    }

    @Test
    fun `sin rol no hay barra`() {
        assertFalse(barraInferiorVisible(null, Routes.HOME_CLIENTE))
        assertFalse(barraInferiorVisible(null, Routes.NEGOCIO_CONSULTA))
        assertFalse(barraInferiorVisible(null, null))
    }
}
