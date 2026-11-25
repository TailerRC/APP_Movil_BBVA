package com.bbva.bonifica.ui.negocio

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
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bbva.bonifica.R
import com.bbva.bonifica.core.UiState
import com.bbva.bonifica.core.toMessage
import com.bbva.bonifica.domain.model.Customer
import com.bbva.bonifica.domain.model.Role
import com.bbva.bonifica.ui.components.BonificaTopBar
import com.bbva.bonifica.ui.components.EmptyView
import com.bbva.bonifica.ui.components.ErrorView
import com.bbva.bonifica.ui.components.LabeledField
import com.bbva.bonifica.ui.components.LoadingView
import com.bbva.bonifica.ui.components.PrimaryButton
import com.bbva.bonifica.ui.components.RoleGuard
import com.bbva.bonifica.ui.components.StatusChip
import com.bbva.bonifica.ui.operaciones.OperationCard

/**
 * Panel de Negocio · Consultar cliente (Módulo 7, ruta `negocio_consulta`).
 *
 * Campo con el código `PARTY-…`; al consultar llama a las rutas 11 y 14 y
 * muestra la ficha resumida (que abre `negocio_cliente/{ref}`) y las operaciones
 * del cliente (cada una abre la ruta 10, desde la que se llega a pagos y
 * descuentos). Un 404 se muestra con `AppError.toMessage()` (§8.4).
 *
 * Todo el contenido va dentro de [RoleGuard]: sin rol `negocio` solo se ve el
 * aviso de §11.2, que es la doble protección junto al 403 de la API.
 */
@Composable
fun ConsultaClienteScreen(
    viewModel: ConsultaClienteViewModel,
    onClienteSelected: (String) -> Unit,
    onOperacionSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    isDark: Boolean = false,
    onToggleTheme: (() -> Unit)? = null,
    onBack: (() -> Unit)? = null
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(modifier = modifier.fillMaxSize()) {
        BonificaTopBar(
            title = stringResource(R.string.negocio_consulta_titulo),
            onBack = onBack,
            actions = {
                if (onToggleTheme != null) {
                    IconButton(onClick = onToggleTheme) {
                        Icon(
                            imageVector = if (isDark) Icons.Outlined.LightMode else Icons.Outlined.DarkMode,
                            contentDescription = stringResource(R.string.toggle_tema),
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }
        )

        RoleGuard(role = Role.NEGOCIO, modifier = Modifier.weight(1f)) {
            ConsultaClienteContent(
                state = state,
                viewModel = viewModel,
                onClienteSelected = onClienteSelected,
                onOperacionSelected = onOperacionSelected,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
private fun ConsultaClienteContent(
    state: ConsultaClienteUiState,
    viewModel: ConsultaClienteViewModel,
    onClienteSelected: (String) -> Unit,
    onOperacionSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val buscando = state.result == UiState.Loading

    Column(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            LabeledField(
                label = stringResource(R.string.negocio_codigo_cliente),
                value = state.code,
                onValueChange = viewModel::onCodeChange,
                placeholder = stringResource(R.string.negocio_codigo_placeholder),
                error = codigoErrorText(state.codeError),
                enabled = !buscando,
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Search,
                onImeAction = viewModel::buscar,
                leadingIcon = Icons.Outlined.Search
            )
            Spacer(Modifier.height(12.dp))
            PrimaryButton(
                text = stringResource(R.string.negocio_buscar),
                onClick = viewModel::buscar,
                loading = buscando,
                loadingText = stringResource(R.string.negocio_buscar_cargando)
            )
        }

        when (val result = state.result) {
            null -> EmptyView(
                message = stringResource(R.string.negocio_consulta_vacio),
                modifier = Modifier.weight(1f)
            )
            is UiState.Loading -> LoadingView(Modifier.weight(1f))
            is UiState.Empty -> EmptyView(
                message = stringResource(R.string.negocio_cliente_no_encontrado),
                modifier = Modifier.weight(1f)
            )
            is UiState.Error -> ErrorView(
                message = result.error.toMessage(),
                onRetry = viewModel::buscar,
                modifier = Modifier.weight(1f)
            )
            is UiState.Content -> ResultadoConsulta(
                data = result.data,
                onClienteSelected = onClienteSelected,
                onOperacionSelected = onOperacionSelected,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun ResultadoConsulta(
    data: ClienteNegocioData,
    onClienteSelected: (String) -> Unit,
    onOperacionSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            FichaResumenCard(
                customer = data.customer,
                onClick = { onClienteSelected(data.customer.customerReference) }
            )
        }

        item {
            Text(
                text = stringResource(R.string.negocio_operaciones_seccion, data.operations.size),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        if (data.operations.isEmpty()) {
            item {
                Text(
                    text = stringResource(R.string.negocio_cliente_sin_operaciones),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            items(data.operations, key = { it.pointOfSaleReference }) { operation ->
                OperationCard(
                    operation = operation,
                    onClick = { onOperacionSelected(operation.pointOfSaleReference) }
                )
            }
        }

        item { Spacer(Modifier.height(16.dp)) }
    }
}

/** Ficha resumida del cliente encontrado: al tocarla se abre la ficha completa. */
@Composable
private fun FichaResumenCard(customer: Customer, onClick: () -> Unit, modifier: Modifier = Modifier) {
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
                        imageVector = Icons.Outlined.Person,
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
                        text = customer.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    StatusChip(status = customer.status)
                }

                Spacer(Modifier.height(4.dp))

                Text(
                    text = customer.customerReference,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(Modifier.height(2.dp))

                Text(
                    text = "${customer.segment} · ${customer.city}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.width(8.dp))

            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = stringResource(R.string.negocio_ver_ficha),
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun codigoErrorText(error: CodigoError?): String? = when (error) {
    CodigoError.REQUIRED -> stringResource(R.string.negocio_codigo_requerido)
    CodigoError.FORMAT -> stringResource(R.string.negocio_codigo_formato)
    null -> null
}
