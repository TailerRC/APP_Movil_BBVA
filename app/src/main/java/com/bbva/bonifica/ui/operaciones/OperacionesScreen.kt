package com.bbva.bonifica.ui.operaciones

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Atm
import androidx.compose.material.icons.outlined.Computer
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.Store
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bbva.bonifica.R
import com.bbva.bonifica.core.UiState
import com.bbva.bonifica.core.formatDate
import com.bbva.bonifica.core.toMessage
import com.bbva.bonifica.domain.model.PointOfSale
import com.bbva.bonifica.ui.components.BonificaTopBar
import com.bbva.bonifica.ui.components.EmptyView
import com.bbva.bonifica.ui.components.ErrorView
import com.bbva.bonifica.ui.components.LoadingView
import com.bbva.bonifica.ui.components.StatusChip

/**
 * Pantalla de Mis Operaciones (Módulo 5, ruta 14).
 * Muestra el listado de contratos/puntos de atención comerciales agrupados por estado.
 */
@Composable
fun OperacionesScreen(
    viewModel: OperacionesViewModel,
    onOperationSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(modifier = modifier.fillMaxSize()) {
        BonificaTopBar(
            title = stringResource(R.string.operaciones_titulo),
            onBack = onBack
        )

        when (val current = state) {
            is UiState.Loading -> LoadingView(Modifier.weight(1f))
            is UiState.Empty -> EmptyView(
                message = stringResource(R.string.operaciones_vacio),
                modifier = Modifier.weight(1f)
            )
            is UiState.Error -> ErrorView(
                message = current.error.toMessage(),
                onRetry = viewModel::load,
                modifier = Modifier.weight(1f)
            )
            is UiState.Content -> OperacionesContent(
                operations = current.data,
                onOperationSelected = onOperationSelected,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun OperacionesContent(
    operations: List<PointOfSale>,
    onOperationSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val openOps = operations.filter { it.status == "OPEN" }
    val otherOps = operations.filter { it.status != "OPEN" }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = stringResource(R.string.operaciones_subtitulo),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }

        if (openOps.isNotEmpty()) {
            item {
                Text(
                    text = "OPEN",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            items(openOps, key = { it.pointOfSaleReference }) { op ->
                OperationCard(
                    operation = op,
                    onClick = { onOperationSelected(op.pointOfSaleReference) }
                )
            }
        }

        if (otherOps.isNotEmpty()) {
            item {
                Text(
                    text = "CLOSED",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            items(otherOps, key = { it.pointOfSaleReference }) { op ->
                OperationCard(
                    operation = op,
                    onClick = { onOperationSelected(op.pointOfSaleReference) }
                )
            }
        }

        item {
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
fun OperationCard(
    operation: PointOfSale,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = channelIcon(operation.channel),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = operation.pointOfSaleReference,
                        style = MaterialTheme.typography.titleMedium.copy(letterSpacing = 0.6.sp),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    StatusChip(status = operation.status)
                }

                Spacer(Modifier.height(4.dp))

                Text(
                    text = "${operation.officeName} (${operation.city})",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = channelName(operation.channel),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = formatDate(operation.createdAt),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.width(8.dp))

            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

fun channelIcon(channel: String): ImageVector = when (channel) {
    "BRANCH" -> Icons.Outlined.Store
    "MOBILE_APP" -> Icons.Outlined.PhoneAndroid
    "WEB" -> Icons.Outlined.Computer
    "ATM" -> Icons.Outlined.Atm
    "AGENT" -> Icons.Outlined.Storefront
    else -> Icons.Outlined.Store
}

@Composable
fun channelName(channel: String): String = when (channel) {
    "BRANCH" -> stringResource(R.string.canal_sucursal)
    "MOBILE_APP" -> stringResource(R.string.canal_banca_movil)
    "WEB" -> stringResource(R.string.canal_web)
    "ATM" -> stringResource(R.string.canal_cajero)
    "AGENT" -> stringResource(R.string.canal_agente)
    else -> stringResource(R.string.canal_desconocido)
}
