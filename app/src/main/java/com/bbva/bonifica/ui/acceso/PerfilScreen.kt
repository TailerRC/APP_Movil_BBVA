package com.bbva.bonifica.ui.acceso

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.ListAlt
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Receipt
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.bbva.bonifica.ui.components.StatusChip
import com.bbva.bonifica.ui.theme.BbvaAqua
import com.bbva.bonifica.ui.theme.BbvaBlue
import com.bbva.bonifica.ui.theme.BbvaBlueLight
import com.bbva.bonifica.ui.theme.BbvaGray300
import com.bbva.bonifica.ui.theme.BbvaGray700
import com.bbva.bonifica.ui.theme.BbvaNavy
import com.bbva.bonifica.ui.theme.BbvaWhite
import com.bbva.bonifica.ui.theme.Error
import com.bbva.bonifica.ui.theme.ErrorDark
import com.bbva.bonifica.ui.theme.Warning

/**
 * Perfil del cliente (§12): ruta 11 con el `customerReference` del token —
 * nombre, segmento, ciudad, estado, cliente desde, correo, teléfono y DNI
 * enmascarado —, aviso si la cuenta está `INACTIVE` y **Cerrar sesión**.
 *
 * Maqueta de estilo: capturas PNG de `docs/referencias` (fondo gris claro, tarjetas
 * blancas de esquinas redondeadas, etiquetas de sección en mayúsculas, ícono
 * en círculo azul claro y chevron de navegación). La top bar sigue siendo la
 * azul `BbvaBlue` de §9.
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
            AvisoInactiva()
            Spacer(Modifier.height(16.dp))
        }

        Encabezado(customer)

        Spacer(Modifier.height(24.dp))
        EtiquetaSeccion(stringResource(R.string.perfil_seccion_datos_personales))
        Spacer(Modifier.height(8.dp))
        Tarjeta {
            Fila(
                label = stringResource(R.string.perfil_dni),
                value = maskDni(customer.documentNumber),
                conDivisor = true
            )
            Fila(
                label = stringResource(R.string.perfil_correo),
                value = customer.email,
                conDivisor = true
            )
            Fila(
                label = stringResource(R.string.perfil_telefono),
                value = customer.phone,
                conDivisor = false
            )
        }

        Spacer(Modifier.height(20.dp))
        EtiquetaSeccion(stringResource(R.string.perfil_seccion_cuenta))
        Spacer(Modifier.height(8.dp))
        Tarjeta {
            Fila(
                label = stringResource(R.string.perfil_segmento),
                value = customer.segment,
                conDivisor = true
            )
            Fila(
                label = stringResource(R.string.perfil_ciudad),
                value = customer.city,
                conDivisor = true
            )
            FilaEstado(status = customer.status, conDivisor = true)
            Fila(
                label = stringResource(R.string.perfil_desde),
                value = formatDate(customer.customerSince),
                conDivisor = false
            )
        }

        Spacer(Modifier.height(24.dp))
        EtiquetaSeccion(stringResource(R.string.perfil_seccion_accesos))
        Spacer(Modifier.height(8.dp))
        Tarjeta {
            AccionFila(
                icon = Icons.Outlined.Receipt,
                title = stringResource(R.string.catalogo_titulo),
                subtitle = stringResource(R.string.perfil_catalogo_desc),
                onClick = onNavigateToCatalogo,
                conDivisor = true
            )
            AccionFila(
                icon = Icons.AutoMirrored.Outlined.ListAlt,
                title = stringResource(R.string.operaciones_titulo),
                subtitle = stringResource(R.string.perfil_operaciones_desc),
                onClick = onNavigateToOperaciones,
                conDivisor = false
            )
        }

        Spacer(Modifier.height(12.dp))
        Tarjeta {
            CerrarSesionFila(onClick = onCloseSession)
        }

        Spacer(Modifier.height(16.dp))
    }
}

/** Cabecera: avatar (ícono, sin foto — decisión de diseño), nombre y estado. */
@Composable
private fun Encabezado(customer: Customer) {
    Tarjeta {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = BbvaBlueLight.copy(alpha = 0.12f),
                modifier = Modifier.size(56.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Outlined.Person,
                        contentDescription = null,
                        tint = BbvaBlue,
                        modifier = Modifier.size(30.dp)
                    )
                }
            }

            Spacer(Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = customer.name,
                    style = MaterialTheme.typography.titleLarge,
                    color = BbvaNavy
                )
                Spacer(Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .width(36.dp)
                        .height(3.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(BbvaAqua)
                )
            }

            Spacer(Modifier.width(8.dp))
            StatusChip(status = customer.status)
        }
    }
}

/** Aviso de cuenta no activa (§12), con el mismo formato de banner del resto. */
@Composable
private fun AvisoInactiva() {
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
}

/** Título de bloque en mayúsculas y gris (maqueta: "CUENTA DE ORIGEN"). */
@Composable
private fun EtiquetaSeccion(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        modifier = modifier,
        style = MaterialTheme.typography.labelSmall,
        color = BbvaGray700
    )
}

@Composable
private fun Tarjeta(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = BbvaWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
            content = content
        )
    }
}

@Composable
private fun Fila(label: String, value: String, conDivisor: Boolean = true) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
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
    if (conDivisor) Divisor()
}

@Composable
private fun FilaEstado(status: String, conDivisor: Boolean = true) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
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
    if (conDivisor) Divisor()
}

/** Fila navegable con ícono en círculo, título + descripción y chevron. */
@Composable
private fun AccionFila(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    conDivisor: Boolean = true
) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = CircleShape,
            color = BbvaBlueLight.copy(alpha = 0.12f),
            modifier = Modifier.size(44.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = BbvaBlue,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        Spacer(Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = BbvaNavy
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = BbvaGray700
            )
        }

        Spacer(Modifier.width(8.dp))

        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = BbvaBlue,
            modifier = Modifier.size(20.dp)
        )
    }
    if (conDivisor) Divisor()
}

/** Cerrar sesión en tarjeta propia, separada de las navegaciones y en rojo. */
@Composable
private fun CerrarSesionFila(onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = CircleShape,
            color = Error.copy(alpha = 0.12f),
            modifier = Modifier.size(44.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.Logout,
                    contentDescription = null,
                    tint = Error,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        Spacer(Modifier.width(14.dp))

        Text(
            text = stringResource(R.string.cerrar_sesion),
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.titleSmall,
            color = ErrorDark
        )
    }
}

@Composable
private fun Divisor() {
    HorizontalDivider(color = BbvaGray300.copy(alpha = 0.6f))
}
