package com.bbva.bonifica.ui.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.bbva.bonifica.ui.components.SecondaryButton

/**
 * Menú TEMPORAL para saltar a cualquier pantalla mientras no todas tengan su
 * entrada real. Se entra desde el splash con pulsación larga en el logo (§14)
 * y se retira cuando el flujo completo esté cableado.
 */
@Composable
fun DevMenuScreen(onNavigate: (String) -> Unit) {
    val destinos = listOf(
        Routes.LOGIN, Routes.NEW_PASSWORD, Routes.HOME_CLIENTE, Routes.CATALOGO,
        Routes.catalogoCategoria("TARJETAS"), Routes.producto("PROD-0025"),
        Routes.OPERACIONES, Routes.operacion("SALE-SEED-0001"),
        Routes.calcular("SALE-SEED-0001", "PROD-0025"), Routes.resultado("DP-0001"),
        Routes.pagos("SALE-SEED-0001"), Routes.comprobante("PAY-0001"),
        Routes.historial("DP-0001"), Routes.PROMOCIONES, Routes.NEGOCIO_CONSULTA,
        Routes.negocioCliente("PARTY-000024"), Routes.NEGOCIO_POLITICA
    )
    LazyColumn(
        modifier = Modifier.fillMaxSize().statusBarsPadding().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Text(
                "Menú de desarrollo (reemplazar por Splash)",
                style = MaterialTheme.typography.titleMedium
            )
        }
        items(destinos) { ruta ->
            SecondaryButton(text = ruta, onClick = { onNavigate(ruta) })
        }
    }
}
