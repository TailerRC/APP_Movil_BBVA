package com.bbva.bonifica.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument

/** Pantalla vacía. Cada persona la reemplaza por su pantalla real en su rama. */
@Composable
private fun Placeholder(nombre: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(nombre)
    }
}

@Composable
fun AppNavHost(navController: NavHostController = rememberNavController()) {
    NavHost(navController = navController, startDestination = Routes.SPLASH) {
        // TODO (Persona B): Splash real. Por ahora un menú para probar la navegación.
        composable(Routes.SPLASH) { DevMenuScreen(onNavigate = { navController.navigate(it) }) }

        // Módulo 1 · Acceso y Perfil (Persona B)
        composable(Routes.LOGIN) { Placeholder("Login") }
        composable(Routes.NEW_PASSWORD) { Placeholder("Nueva contraseña") }
        composable(Routes.HOME_CLIENTE) { Placeholder("Inicio cliente (Perfil)") }

        // Módulo 2 · Catálogo (Persona C)
        composable(Routes.CATALOGO) { Placeholder("Categorías") }
        composable(Routes.CATALOGO_CATEGORIA) { Placeholder("Productos de categoría") }
        composable(Routes.PRODUCTO) { Placeholder("Detalle de producto") }

        // Módulo 5 · Operaciones (Persona C)
        composable(Routes.OPERACIONES) { Placeholder("Mis operaciones") }
        composable(Routes.OPERACION) { Placeholder("Detalle de operación") }

        // Módulo 3 · Descuentos (Persona D)
        composable(
            route = Routes.CALCULAR,
            arguments = listOf(
                navArgument("pos") { type = NavType.StringType; nullable = true; defaultValue = null },
                navArgument("prod") { type = NavType.StringType; nullable = true; defaultValue = null }
            )
        ) { Placeholder("Calcular descuento") }
        composable(Routes.RESULTADO) { Placeholder("Resultado / Confirmar / Anular") }

        // Módulo 6 · Pagos e Historial (Persona E)
        composable(Routes.PAGOS) { Placeholder("Pagos de la operación") }
        composable(Routes.COMPROBANTE) { Placeholder("Comprobante") }
        composable(Routes.HISTORIAL) { Placeholder("Historial") }

        // Módulo 4 · Promociones (Persona E)
        composable(Routes.PROMOCIONES) { Placeholder("Promociones") }

        // Módulo 7 · Panel de Negocio (Persona F)
        composable(Routes.NEGOCIO_CONSULTA) { Placeholder("Consultar cliente") }
        composable(Routes.NEGOCIO_CLIENTE) { Placeholder("Ficha de cliente") }
        composable(Routes.NEGOCIO_POLITICA) { Placeholder("Registrar política") }
    }
}
