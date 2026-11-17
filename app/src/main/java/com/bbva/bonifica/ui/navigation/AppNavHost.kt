package com.bbva.bonifica.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.bbva.bonifica.di.rememberAppFactory
import com.bbva.bonifica.domain.model.Role
import com.bbva.bonifica.ui.acceso.AccesoViewModel
import com.bbva.bonifica.ui.acceso.LoginScreen
import com.bbva.bonifica.ui.acceso.NuevaContrasenaScreen
import com.bbva.bonifica.ui.acceso.NuevaContrasenaViewModel
import com.bbva.bonifica.ui.acceso.PerfilScreen
import com.bbva.bonifica.ui.acceso.PerfilViewModel
import com.bbva.bonifica.ui.acceso.SplashScreen
import com.bbva.bonifica.ui.acceso.SplashViewModel
import com.bbva.bonifica.ui.catalogo.CategoriasScreen
import com.bbva.bonifica.ui.catalogo.ProductoDetalleScreen
import com.bbva.bonifica.ui.catalogo.ProductoDetalleViewModel
import com.bbva.bonifica.ui.catalogo.ProductosScreen
import com.bbva.bonifica.ui.catalogo.ProductosViewModel
import com.bbva.bonifica.ui.descuentos.CalcularScreen
import com.bbva.bonifica.ui.descuentos.CalcularViewModel
import com.bbva.bonifica.ui.descuentos.ResultadoScreen
import com.bbva.bonifica.ui.descuentos.ResultadoViewModel
import com.bbva.bonifica.ui.operaciones.OperacionDetalleScreen
import com.bbva.bonifica.ui.operaciones.OperacionDetalleViewModel
import com.bbva.bonifica.ui.operaciones.OperacionesScreen
import com.bbva.bonifica.ui.operaciones.OperacionesViewModel

/** Pantalla vacía. Cada persona la reemplaza por su pantalla real en su rama. */
@Composable
private fun Placeholder(nombre: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(nombre)
    }
}

/** Destino inicial según el rol (§11.2): el rol sale solo del token. */
private fun homeRoute(role: Role): String =
    if (role == Role.CLIENTE) Routes.HOME_CLIENTE else Routes.NEGOCIO_CONSULTA

/**
 * Entra al home dejando [root] fuera de la pila: así atrás desde el home cierra
 * la app en vez de volver al login. [root] es la pantalla desde la que se entra
 * (splash o login).
 */
private fun NavHostController.enterHome(role: Role, root: String) {
    navigate(homeRoute(role)) {
        popUpTo(root) { inclusive = true }
        launchSingleTop = true
    }
}

/**
 * Cierra sesión y abre el login como única pantalla. El popUpTo es contra el home
 * (raíz de la pila después de entrar, §11): splash y login ya no están debajo.
 */
private fun NavHostController.leaveSession(role: Role) {
    navigate(Routes.LOGIN) {
        popUpTo(homeRoute(role)) { inclusive = true }
        launchSingleTop = true
    }
}

@Composable
fun AppNavHost(navController: NavHostController = rememberNavController()) {

    // Scope de Activity (se crea fuera del NavHost): conserva el reto de nueva
    // contraseña y lo escrito en el login al ir y volver entre pantallas (§5).
    val acceso: AccesoViewModel = viewModel(
        factory = rememberAppFactory { AccesoViewModel(it.authRepository) }
    )

    NavHost(navController = navController, startDestination = Routes.SPLASH) {

        // Módulo 1 · Acceso y Perfil (Persona B)
        composable(Routes.SPLASH) {
            val viewModel: SplashViewModel = viewModel(
                factory = rememberAppFactory { SplashViewModel(it.authRepository) }
            )
            val state by viewModel.state.collectAsStateWithLifecycle()
            SplashScreen(
                state = state,
                onHome = { role -> navController.enterHome(role, Routes.SPLASH) },
                onLogin = {
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(Routes.SPLASH) { inclusive = true }
                    }
                },
                onOpenDevMenu = { navController.navigate(Routes.DEV_MENU) }
            )
        }

        // Menú temporal de desarrollo (§14). Mantener pulsado el logo del splash.
        composable(Routes.DEV_MENU) { DevMenuScreen(onNavigate = { navController.navigate(it) }) }

        composable(Routes.LOGIN) {
            LoginScreen(
                acceso = acceso,
                onHome = { role -> navController.enterHome(role, Routes.LOGIN) },
                onNewPassword = { navController.navigate(Routes.NEW_PASSWORD) }
            )
        }

        composable(Routes.NEW_PASSWORD) {
            val viewModel: NuevaContrasenaViewModel = viewModel(
                factory = rememberAppFactory { NuevaContrasenaViewModel(it.authRepository) }
            )
            NuevaContrasenaScreen(
                viewModel = viewModel,
                challenge = acceso.pendingChallenge,
                onHome = { role ->
                    acceso.clearChallenge()
                    navController.enterHome(role, Routes.LOGIN)
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.HOME_CLIENTE) {
            val viewModel: PerfilViewModel = viewModel(
                factory = rememberAppFactory {
                    PerfilViewModel(it.authRepository, it.customerRepository)
                }
            )
            PerfilScreen(
                viewModel = viewModel,
                onNavigateToCatalogo = { navController.navigate(Routes.CATALOGO) },
                onNavigateToOperaciones = { navController.navigate(Routes.OPERACIONES) },
                onLoggedOut = { role ->
                    acceso.reset()
                    navController.leaveSession(role)
                }
            )
        }

        // Módulo 2 · Catálogo (Persona C)
        composable(Routes.CATALOGO) {
            CategoriasScreen(
                onCategorySelected = { category ->
                    navController.navigate(Routes.catalogoCategoria(category))
                },
                onBack = if (navController.previousBackStackEntry != null) {
                    { navController.popBackStack() }
                } else null
            )
        }

        composable(
            route = Routes.CATALOGO_CATEGORIA,
            arguments = listOf(navArgument("category") { type = NavType.StringType })
        ) { backStackEntry ->
            val category = backStackEntry.arguments?.getString("category").orEmpty()
            val viewModel: ProductosViewModel = viewModel(
                key = "productos_$category",
                factory = rememberAppFactory { ProductosViewModel(category, it.catalogRepository) }
            )
            ProductosScreen(
                viewModel = viewModel,
                onProductSelected = { ref ->
                    navController.navigate(Routes.producto(ref))
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Routes.PRODUCTO,
            arguments = listOf(navArgument("ref") { type = NavType.StringType })
        ) { backStackEntry ->
            val ref = backStackEntry.arguments?.getString("ref").orEmpty()
            val viewModel: ProductoDetalleViewModel = viewModel(
                key = "producto_$ref",
                factory = rememberAppFactory { ProductoDetalleViewModel(ref, it.catalogRepository) }
            )
            ProductoDetalleScreen(
                viewModel = viewModel,
                onCalcular = { productRef ->
                    navController.navigate(Routes.calcular(prod = productRef))
                },
                onBack = { navController.popBackStack() }
            )
        }

        // Módulo 5 · Operaciones (Persona C)
        composable(Routes.OPERACIONES) {
            val viewModel: OperacionesViewModel = viewModel(
                factory = rememberAppFactory {
                    OperacionesViewModel(it.authRepository, it.customerRepository)
                }
            )
            OperacionesScreen(
                viewModel = viewModel,
                onOperationSelected = { ref ->
                    navController.navigate(Routes.operacion(ref))
                },
                onBack = if (navController.previousBackStackEntry != null) {
                    { navController.popBackStack() }
                } else null
            )
        }

        composable(
            route = Routes.OPERACION,
            arguments = listOf(navArgument("ref") { type = NavType.StringType })
        ) { backStackEntry ->
            val ref = backStackEntry.arguments?.getString("ref").orEmpty()
            val viewModel: OperacionDetalleViewModel = viewModel(
                key = "operacion_$ref",
                factory = rememberAppFactory { OperacionDetalleViewModel(ref, it.operationRepository) }
            )
            OperacionDetalleScreen(
                viewModel = viewModel,
                onCalcularDescuento = { posRef ->
                    navController.navigate(Routes.calcular(pos = posRef))
                },
                onVerPagos = { posRef ->
                    navController.navigate(Routes.pagos(posRef))
                },
                onVerDescuento = { dpId ->
                    navController.navigate(Routes.resultado(dpId))
                },
                onBack = { navController.popBackStack() }
            )
        }

        // Módulo 3 · Descuentos (Persona D)
        composable(
            route = Routes.CALCULAR,
            arguments = listOf(
                navArgument("pos") { type = NavType.StringType; nullable = true; defaultValue = null },
                navArgument("prod") { type = NavType.StringType; nullable = true; defaultValue = null }
            )
        ) { backStackEntry ->
            val pos = backStackEntry.arguments?.getString("pos")
            val prod = backStackEntry.arguments?.getString("prod")
            val viewModel: CalcularViewModel = viewModel(
                key = "calcular_${pos}_$prod",
                factory = rememberAppFactory {
                    CalcularViewModel(
                        initialPosRef = pos,
                        productRef = prod,
                        authRepository = it.authRepository,
                        customerRepository = it.customerRepository,
                        catalogRepository = it.catalogRepository,
                        discountRepository = it.discountRepository
                    )
                }
            )
            CalcularScreen(
                viewModel = viewModel,
                onResultado = { dpId -> navController.navigate(Routes.resultado(dpId)) },
                onOpenCatalogo = { navController.navigate(Routes.CATALOGO) },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Routes.RESULTADO,
            arguments = listOf(navArgument("dpId") { type = NavType.StringType })
        ) { backStackEntry ->
            val dpId = backStackEntry.arguments?.getString("dpId").orEmpty()
            val viewModel: ResultadoViewModel = viewModel(
                key = "resultado_$dpId",
                factory = rememberAppFactory { ResultadoViewModel(dpId, it.discountRepository) }
            )
            ResultadoScreen(
                viewModel = viewModel,
                onVerComprobante = { payId -> navController.navigate(Routes.comprobante(payId)) },
                onBack = { navController.popBackStack() }
            )
        }

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
