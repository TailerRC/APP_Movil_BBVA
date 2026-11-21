package com.bbva.bonifica.ui.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.bbva.bonifica.R
import com.bbva.bonifica.ui.components.BonificaTopBar
import com.bbva.bonifica.ui.components.SecondaryButton

/**
 * Menú TEMPORAL para saltar a cualquier pantalla mientras no todas tengan su
 * entrada real. Se entra desde el splash con pulsación larga en el logo (§14)
 * y se retira cuando el flujo completo esté cableado.
 *
 * Lleva barra con botón atrás: sin ella el menú era otra pantalla sin salida.
 */
@Composable
fun DevMenuScreen(onNavigate: (String) -> Unit, onBack: () -> Unit) {
    val destinos = listOf(
        Routes.LOGIN, Routes.NEW_PASSWORD, Routes.HOME_CLIENTE, Routes.CATALOGO,
        Routes.catalogoCategoria("TARJETAS"), Routes.producto("PROD-0025"),
        Routes.OPERACIONES, Routes.operacion("SALE-SEED-0001"),
        Routes.calcular("SALE-SEED-0001", "PROD-0025"), Routes.resultado("DP-0001"),
        Routes.pagos("SALE-SEED-0001"), Routes.comprobante("PAY-0001"),
        Routes.historial("DP-0001"), Routes.PROMOCIONES, Routes.NEGOCIO_CONSULTA,
        Routes.negocioCliente("PARTY-000024"), Routes.NEGOCIO_POLITICA
    )
    Column(Modifier.fillMaxSize()) {
        BonificaTopBar(
            title = stringResource(R.string.dev_menu_titulo),
            onBack = onBack
        )
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Text(
                    text = stringResource(R.string.dev_menu_subtitulo),
                    style = MaterialTheme.typography.titleMedium
                )
            }
            items(destinos) { ruta ->
                SecondaryButton(text = ruta, onClick = { onNavigate(ruta) })
            }
        }
    }
}
