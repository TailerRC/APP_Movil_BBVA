package com.bbva.bonifica.ui.negocio

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bbva.bonifica.R
import com.bbva.bonifica.core.UiState
import com.bbva.bonifica.core.formatDate
import com.bbva.bonifica.core.maskDni
import com.bbva.bonifica.core.toMessage
import com.bbva.bonifica.domain.model.Customer
import com.bbva.bonifica.domain.model.Role
import com.bbva.bonifica.ui.components.BonificaTopBar
import com.bbva.bonifica.ui.components.EmptyView
import com.bbva.bonifica.ui.components.ErrorView
import com.bbva.bonifica.ui.components.LoadingView
import com.bbva.bonifica.ui.components.RoleGuard
import com.bbva.bonifica.ui.components.StatusChip
import com.bbva.bonifica.ui.operaciones.OperationCard
import com.bbva.bonifica.ui.theme.Warning

/**
 * Panel de Negocio · Ficha del cliente (Módulo 7, ruta `negocio_cliente/{ref}`).
 *
 * Ficha completa + operaciones del cliente (rutas 11 y 14). Cada operación abre
 * la ruta 10, desde la que se llega a los pagos (15) y a los descuentos (8).
 * El DNI se muestra enmascarado, igual que en el perfil del propio cliente (§12).
 *
 * Contenido protegido con [RoleGuard] (§11.2): sin rol `negocio` solo se ve el
 * aviso de acceso restringido.
 */
@Composable
fun ClienteNegocioScreen(
    viewModel: ClienteNegocioViewModel,
    onOperacionSelected: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(modifier = modifier.fillMaxSize()) {
        BonificaTopBar(
            title = stringResource(R.string.negocio_cliente_titulo),
            onBack = onBack
        )

        RoleGuard(role = Role.NEGOCIO, modifier = Modifier.weight(1f)) {
            when (val current = state) {
                is UiState.Loading -> LoadingView(Modifier.fillMaxSize())
                is UiState.Empty -> EmptyView(
                    message = stringResource(R.string.negocio_cliente_no_encontrado),
                    modifier = Modifier.fillMaxSize()
                )
                is UiState.Error -> ErrorView(
                    message = current.error.toMessage(),
                    onRetry = viewModel::load,
                    modifier = Modifier.fillMaxSize()
                )
                is UiState.Content -> ClienteFicha(
                    data = current.data,
                    onOperacionSelected = onOperacionSelected,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@Composable
private fun ClienteFicha(
    data: ClienteNegocioData,
    onOperacionSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val customer = data.customer

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        if (customer.status != "ACTIVE") {
            AvisoInactivo()
            Spacer(Modifier.height(16.dp))
        }

        // Encabezado: avatar, nombre y estado.
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                    modifier = Modifier.size(52.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Outlined.Person,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                Spacer(Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = customer.name,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = customer.customerReference,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(Modifier.width(8.dp))

                StatusChip(status = customer.status)
            }
        }

        Spacer(Modifier.height(20.dp))

        Text(
            text = stringResource(R.string.negocio_cliente_seccion_datos).uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(8.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
                FilaCliente(
                    label = stringResource(R.string.perfil_dni),
                    value = maskDni(customer.documentNumber)
                )
                FilaCliente(
                    label = stringResource(R.string.perfil_correo),
                    value = customer.email
                )
                FilaCliente(
                    label = stringResource(R.string.perfil_telefono),
                    value = customer.phone
                )
                FilaCliente(
                    label = stringResource(R.string.perfil_segmento),
                    value = customer.segment
                )
                FilaCliente(
                    label = stringResource(R.string.perfil_ciudad),
                    value = customer.city
                )
                FilaCliente(
                    label = stringResource(R.string.perfil_desde),
                    value = formatDate(customer.customerSince),
                    conDivisor = false
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        Text(
            text = stringResource(R.string.negocio_operaciones_seccion, data.operations.size),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(Modifier.height(12.dp))

        if (data.operations.isEmpty()) {
            Text(
                text = stringResource(R.string.negocio_cliente_sin_operaciones),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                data.operations.forEach { operation ->
                    OperationCard(
                        operation = operation,
                        onClick = { onOperacionSelected(operation.pointOfSaleReference) }
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun FilaCliente(label: String, value: String, conDivisor: Boolean = true) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            modifier = Modifier.weight(1.5f),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.End
        )
    }
    if (conDivisor) HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
}

/** Aviso de cliente `INACTIVE`, con el mismo formato del perfil (§12). */
@Composable
private fun AvisoInactivo() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = Warning.copy(alpha = 0.20f)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.negocio_cliente_inactivo),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
