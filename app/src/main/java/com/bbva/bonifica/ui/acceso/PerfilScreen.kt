package com.bbva.bonifica.ui.acceso

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.bbva.bonifica.ui.components.PrimaryButton
import com.bbva.bonifica.ui.components.SecondaryButton
import com.bbva.bonifica.ui.components.StatusChip
import com.bbva.bonifica.ui.theme.BbvaGray300
import com.bbva.bonifica.ui.theme.BbvaGray700
import com.bbva.bonifica.ui.theme.BbvaNavy
import com.bbva.bonifica.ui.theme.BbvaWhite
import com.bbva.bonifica.ui.theme.Warning

/**
 * Perfil del cliente (§12): ruta 11 con el `customerReference` del token —
 * nombre, segmento, ciudad, estado, cliente desde, correo, teléfono y DNI
 * enmascarado —, aviso si la cuenta está `INACTIVE` y botón **Cerrar sesión**.
 */
@Composable
fun PerfilScreen(
    viewModel: PerfilViewModel,
    onNavigateToCatalogo: () -> Unit = {},
    onNavigateToOperaciones: () -> Unit = {},
    onLoggedOut: (Role) -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is PerfilEvent.LoggedOut -> onLoggedOut(event.role)
            }
        }
    }

    Column(modifier.fillMaxSize()) {
        BonificaTopBar(title = stringResource(R.string.perfil_titulo))

        when (val current = state) {
            is UiState.Loading -> LoadingView(Modifier.weight(1f))
            is UiState.Empty -> EmptyView(
                message = stringResource(R.string.perfil_sin_datos),
                modifier = Modifier.weight(1f)
            )
            is UiState.Error -> ErrorView(
                message = current.error.toMessage(),
                onRetry = viewModel::load,
                modifier = Modifier.weight(1f)
            )
            is UiState.Content -> Contenido(
                customer = current.data,
                onNavigateToCatalogo = onNavigateToCatalogo,
                onNavigateToOperaciones = onNavigateToOperaciones,
                onCloseSession = viewModel::logout,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun Contenido(
    customer: Customer,
    onNavigateToCatalogo: () -> Unit,
    onNavigateToOperaciones: () -> Unit,
    onCloseSession: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        if (customer.status != "ACTIVE") {
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
                        tint = BbvaNavy,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.perfil_cuenta_inactiva),
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyMedium,
                        color = BbvaNavy
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = BbvaWhite),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(Modifier.fillMaxWidth().padding(20.dp)) {
                Text(
                    text = customer.name,
                    style = MaterialTheme.typography.titleLarge,
                    color = BbvaNavy
                )
                Spacer(Modifier.height(8.dp))
                Fila(label = stringResource(R.string.perfil_segmento), value = customer.segment)
                Fila(label = stringResource(R.string.perfil_ciudad), value = customer.city)
                FilaEstado(status = customer.status)
                Fila(
                    label = stringResource(R.string.perfil_desde),
                    value = formatDate(customer.customerSince)
                )
                Fila(label = stringResource(R.string.perfil_correo), value = customer.email)
                Fila(label = stringResource(R.string.perfil_telefono), value = customer.phone)
                Fila(
                    label = stringResource(R.string.perfil_dni),
                    value = maskDni(customer.documentNumber)
                )
            }
        }

        Spacer(Modifier.height(24.dp))
        PrimaryButton(
            text = stringResource(R.string.catalogo_titulo),
            onClick = onNavigateToCatalogo
        )
        Spacer(Modifier.height(12.dp))
        SecondaryButton(
            text = stringResource(R.string.operaciones_titulo),
            onClick = onNavigateToOperaciones
        )
        Spacer(Modifier.height(16.dp))
        SecondaryButton(
            text = stringResource(R.string.cerrar_sesion),
            onClick = onCloseSession
        )
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun Fila(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 10.dp)) {
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            color = BbvaGray700
        )
        Text(
            text = value,
            modifier = Modifier.weight(1.5f),
            style = MaterialTheme.typography.bodyMedium,
            color = BbvaNavy,
            textAlign = TextAlign.End
        )
    }
    Divisor()
}

@Composable
private fun FilaEstado(status: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(R.string.perfil_estado),
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            color = BbvaGray700
        )
        StatusChip(status = status)
    }
    Divisor()
}

@Composable
private fun Divisor() {
    HorizontalDivider(color = BbvaGray300.copy(alpha = 0.6f))
}
