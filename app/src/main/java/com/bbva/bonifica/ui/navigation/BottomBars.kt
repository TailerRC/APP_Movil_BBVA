package com.bbva.bonifica.ui.navigation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Gavel
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.LocalOffer
import androidx.compose.material.icons.outlined.PersonSearch
import androidx.compose.material.icons.outlined.Receipt
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import com.bbva.bonifica.R
import com.bbva.bonifica.domain.model.Role
import com.bbva.bonifica.ui.theme.BbvaBlue
import com.bbva.bonifica.ui.theme.BbvaGray300
import com.bbva.bonifica.ui.theme.BbvaGray700
import com.bbva.bonifica.ui.theme.BbvaWhite

/** Una pestaña: su ruta raíz, su etiqueta y las rutas que navega (§11.2). */
private data class Tab(val route: String, val labelRes: Int, val rutas: List<String>)

// Cliente: Inicio · Catálogo · Operaciones · Promociones (§11.2).
private val tabsCliente = listOf(
    Tab(Routes.HOME_CLIENTE, R.string.tab_inicio, listOf(Routes.HOME_CLIENTE)),
    Tab(
        Routes.CATALOGO, R.string.tab_catalogo,
        listOf(Routes.CATALOGO, Routes.CATALOGO_CATEGORIA, Routes.PRODUCTO)
    ),
    Tab(
        Routes.OPERACIONES, R.string.tab_operaciones,
        listOf(Routes.OPERACIONES, Routes.OPERACION)
    ),
    Tab(Routes.PROMOCIONES, R.string.tab_promociones, listOf(Routes.PROMOCIONES))
)

// Negocio: Consultar cliente · Registrar política · Promociones · Catálogo (§11.2).
private val tabsNegocio = listOf(
    Tab(
        Routes.NEGOCIO_CONSULTA, R.string.tab_consultar_cliente,
        listOf(Routes.NEGOCIO_CONSULTA, Routes.NEGOCIO_CLIENTE, Routes.OPERACION)
    ),
    Tab(
        Routes.NEGOCIO_POLITICA, R.string.tab_registrar_politica,
        listOf(Routes.NEGOCIO_POLITICA)
    ),
    Tab(Routes.PROMOCIONES, R.string.tab_promociones, listOf(Routes.PROMOCIONES)),
    Tab(
        Routes.CATALOGO, R.string.tab_catalogo,
        listOf(Routes.CATALOGO, Routes.CATALOGO_CATEGORIA, Routes.PRODUCTO)
    )
)

private fun tabs(role: Role): List<Tab> = when (role) {
    Role.CLIENTE -> tabsCliente
    Role.NEGOCIO -> tabsNegocio
}

/**
 * true si la barra inferior (§11.2) se pinta en [route] para [role].
 *
 * Solo en las rutas de sección: detalle (`negocio_cliente`, `producto`…), flujos
 * (`calcular`, `pagos`…) y accesos (`splash`, `login`, menú dev) la ocultan, así
 * que siempre que se vea habrá una pestaña activa de dónde salir.
 */
fun barraInferiorVisible(role: Role?, route: String?): Boolean =
    role != null && route != null && tabs(role).any { route in it.rutas }

/** Ícono de la pestaña cuya ruta raíz es [route]. */
private fun iconoTab(route: String): ImageVector = when (route) {
    Routes.HOME_CLIENTE -> Icons.Outlined.Home
    Routes.CATALOGO -> Icons.Outlined.Storefront
    Routes.OPERACIONES -> Icons.Outlined.Receipt
    Routes.PROMOCIONES -> Icons.Outlined.LocalOffer
    Routes.NEGOCIO_CONSULTA -> Icons.Outlined.PersonSearch
    Routes.NEGOCIO_POLITICA -> Icons.Outlined.Gavel
    else -> Icons.Outlined.Info
}

/**
 * Barra inferior por rol (§11.2): la única salida del home de negocio, que sin
 * ella sería un callejón. El rol lo aporta `LocalRole` (§11.2, sale del claim).
 *
 * Al pulsar una pestaña la pila queda `[home, pestaña]`: el `popUpTo(home)`
 * descarta lo que había encima y `saveState`/`restoreState` conservan el
 * scroll de la pestaña anterior.
 */
@Composable
fun RoleBottomBar(
    role: Role,
    currentRoute: String?,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        HorizontalDivider(color = BbvaGray300.copy(alpha = 0.7f))
        NavigationBar(
            containerColor = BbvaWhite,
            windowInsets = WindowInsets.navigationBars
        ) {
            tabs(role).forEach { tab ->
                NavigationBarItem(
                    selected = currentRoute in tab.rutas,
                    onClick = { onNavigate(tab.route) },
                    icon = {
                        Icon(
                            imageVector = iconoTab(tab.route),
                            contentDescription = null
                        )
                    },
                    label = { Text(stringResource(tab.labelRes)) },
                    alwaysShowLabel = true,
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = BbvaWhite,
                        selectedTextColor = BbvaBlue,
                        indicatorColor = BbvaBlue,
                        unselectedIconColor = BbvaGray700,
                        unselectedTextColor = BbvaGray700
                    )
                )
            }
        }
    }
}
