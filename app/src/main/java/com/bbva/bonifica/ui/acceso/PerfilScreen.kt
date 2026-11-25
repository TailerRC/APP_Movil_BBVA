package com.bbva.bonifica.ui.acceso

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ListAlt
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.LocalOffer
import androidx.compose.material.icons.outlined.Receipt
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
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
import com.bbva.bonifica.ui.components.HeroHeader
import com.bbva.bonifica.ui.components.LoadingView
import com.bbva.bonifica.ui.components.StatusChip
import com.bbva.bonifica.ui.theme.BbvaAqua
import com.bbva.bonifica.ui.theme.BbvaSky
import com.bbva.bonifica.ui.theme.Success
import com.bbva.bonifica.ui.theme.Warning

/**
 * Inicio / Perfil del cliente (§12):
 * - HeroHeader con logo BBVA, saludo y toggle claro/oscuro (extendido detrás de la barra de estado).
 * - Tarjeta visual con gráfico de resumen de bonificaciones y actividad mensual.
 * - Fila de accesos rápidos circulares estilo BBVA (Catálogo, Operaciones, Promociones, Tema).
 * - Ficha compacta de cuenta y datos personales + Cerrar sesión.
 */
@Composable
fun PerfilScreen(
    viewModel: PerfilViewModel,
    isDark: Boolean = false,
    onToggleTheme: () -> Unit = {},
    onNavigateToCatalogo: () -> Unit = {},
    onNavigateToOperaciones: () -> Unit = {},
    onNavigateToPromociones: () -> Unit = {},
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
        when (val current = state) {
            is UiState.Loading -> {
                BonificaTopBar(
                    title = stringResource(R.string.perfil_titulo),
                    actions = { ThemeToggleAction(isDark = isDark, onToggle = onToggleTheme) }
                )
                LoadingView(Modifier.weight(1f))
            }
            is UiState.Empty -> {
                BonificaTopBar(
                    title = stringResource(R.string.perfil_titulo),
                    actions = { ThemeToggleAction(isDark = isDark, onToggle = onToggleTheme) }
                )
                EmptyView(
                    message = stringResource(R.string.perfil_sin_datos),
                    modifier = Modifier.weight(1f)
                )
            }
            is UiState.Error -> {
                BonificaTopBar(
                    title = stringResource(R.string.perfil_titulo),
                    actions = { ThemeToggleAction(isDark = isDark, onToggle = onToggleTheme) }
                )
                ErrorView(
                    message = current.error.toMessage(),
                    onRetry = viewModel::load,
                    modifier = Modifier.weight(1f)
                )
            }
            is UiState.Content -> Contenido(
                customer = current.data,
                isDark = isDark,
                onToggleTheme = onToggleTheme,
                onNavigateToCatalogo = onNavigateToCatalogo,
                onNavigateToOperaciones = onNavigateToOperaciones,
                onNavigateToPromociones = onNavigateToPromociones,
                onCloseSession = viewModel::logout,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun ThemeToggleAction(isDark: Boolean, onToggle: () -> Unit) {
    IconButton(onClick = onToggle) {
        Icon(
            imageVector = if (isDark) Icons.Outlined.LightMode else Icons.Outlined.DarkMode,
            contentDescription = stringResource(R.string.toggle_tema),
            tint = MaterialTheme.colorScheme.onPrimary
        )
    }
}

@Composable
private fun Contenido(
    customer: Customer,
    isDark: Boolean,
    onToggleTheme: () -> Unit,
    onNavigateToCatalogo: () -> Unit,
    onNavigateToOperaciones: () -> Unit,
    onNavigateToPromociones: () -> Unit,
    onCloseSession: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        HeroHeader(
            name = customer.name,
            segment = customer.segment,
            onToggleTheme = onToggleTheme,
            isDark = isDark
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (customer.status != "ACTIVE") {
                AvisoInactiva()
            }

            // 1. Gráfico visual de resumen de bonificaciones y actividad
            ResumenBeneficiosCard()

            // 2. Accesos rápidos circulares estilo BBVA
            AccesosRapidosRow(
                isDark = isDark,
                onNavigateToCatalogo = onNavigateToCatalogo,
                onNavigateToOperaciones = onNavigateToOperaciones,
                onNavigateToPromociones = onNavigateToPromociones,
                onToggleTheme = onToggleTheme
            )

            // 3. Ficha compacta de cuenta y datos personales
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.perfil_seccion_cuenta).uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        StatusChip(status = customer.status)
                    }

                    Spacer(Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        DatoMini(
                            label = stringResource(R.string.perfil_ciudad),
                            value = customer.city,
                            modifier = Modifier.weight(1f)
                        )
                        DatoMini(
                            label = stringResource(R.string.perfil_desde),
                            value = formatDate(customer.customerSince),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(Modifier.height(12.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
                    Spacer(Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        DatoMini(
                            label = stringResource(R.string.perfil_dni),
                            value = maskDni(customer.documentNumber),
                            modifier = Modifier.weight(1f)
                        )
                        DatoMini(
                            label = stringResource(R.string.perfil_telefono),
                            value = customer.phone,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(Modifier.height(10.dp))
                    DatoMini(
                        label = stringResource(R.string.perfil_correo),
                        value = customer.email,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // 4. Botón Cerrar sesión
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onCloseSession)
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.error.copy(alpha = 0.12f),
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.Logout,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = stringResource(R.string.cerrar_sesion),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}

/**
 * Tarjeta visual con gráfico de anillo (ahorro en comisiones) y barras mensuales.
 */
@Composable
private fun ResumenBeneficiosCard() {
    val trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
    val primaryColor = MaterialTheme.colorScheme.primary
    val textPrimary = MaterialTheme.colorScheme.onSurface
    val textSecondary = MaterialTheme.colorScheme.onSurfaceVariant

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(18.dp)) {
            Text(
                text = "RESUMEN DE BONIFICACIONES",
                style = MaterialTheme.typography.labelSmall,
                color = textSecondary
            )

            Spacer(Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Gráfico de anillo (Donut Chart)
                Box(
                    modifier = Modifier.size(84.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val strokeWidth = 10.dp.toPx()
                        drawArc(
                            color = trackColor,
                            startAngle = 0f,
                            sweepAngle = 360f,
                            useCenter = false,
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                        )
                        // 68% ahorro aplicado
                        drawArc(
                            color = BbvaAqua,
                            startAngle = -90f,
                            sweepAngle = 245f,
                            useCenter = false,
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                        )
                        // 20% pendiente
                        drawArc(
                            color = BbvaSky,
                            startAngle = 155f,
                            sweepAngle = 72f,
                            useCenter = false,
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                        )
                    }
                    Text(
                        text = "68%",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = textPrimary
                    )
                }

                Spacer(Modifier.width(18.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "S/ 148.50",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = textPrimary
                    )
                    Text(
                        text = "Ahorro acumulado en comisiones",
                        style = MaterialTheme.typography.bodySmall,
                        color = textSecondary
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        LeyendaDot(color = BbvaAqua, label = "Aplicado")
                        LeyendaDot(color = BbvaSky, label = "En curso")
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
            Spacer(Modifier.height(12.dp))

            // Mini gráfico de barras de los últimos 5 meses
            Text(
                text = "Actividad mensual",
                style = MaterialTheme.typography.labelMedium,
                color = textSecondary
            )
            Spacer(Modifier.height(8.dp))

            val barValues = listOf(0.42f, 0.65f, 0.50f, 0.85f, 1.0f)
            val barLabels = listOf("Jul", "Ago", "Sep", "Oct", "Nov")

            Row(
                modifier = Modifier.fillMaxWidth().height(56.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                barValues.forEachIndexed { index, ratio ->
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        val isLast = index == barValues.lastIndex
                        val barColor = if (isLast) Success else primaryColor.copy(alpha = 0.55f)
                        Canvas(
                            modifier = Modifier
                                .width(22.dp)
                                .height(36.dp)
                        ) {
                            val barHeight = size.height * ratio
                            drawRoundRect(
                                color = trackColor,
                                topLeft = Offset.Zero,
                                size = size,
                                cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                            )
                            drawRoundRect(
                                color = barColor,
                                topLeft = Offset(0f, size.height - barHeight),
                                size = Size(size.width, barHeight),
                                cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = barLabels[index],
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isLast) textPrimary else textSecondary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LeyendaDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Canvas(modifier = Modifier.size(8.dp)) {
            drawCircle(color = color)
        }
        Spacer(Modifier.width(5.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Fila de accesos rápidos circulares estilo BBVA.
 */
@Composable
private fun AccesosRapidosRow(
    isDark: Boolean,
    onNavigateToCatalogo: () -> Unit,
    onNavigateToOperaciones: () -> Unit,
    onNavigateToPromociones: () -> Unit,
    onToggleTheme: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        AccesoCircular(
            icon = Icons.Outlined.Receipt,
            label = stringResource(R.string.tab_catalogo),
            onClick = onNavigateToCatalogo,
            modifier = Modifier.weight(1f)
        )
        AccesoCircular(
            icon = Icons.AutoMirrored.Outlined.ListAlt,
            label = stringResource(R.string.tab_operaciones),
            onClick = onNavigateToOperaciones,
            modifier = Modifier.weight(1f)
        )
        AccesoCircular(
            icon = Icons.Outlined.LocalOffer,
            label = stringResource(R.string.tab_promociones),
            onClick = onNavigateToPromociones,
            modifier = Modifier.weight(1f)
        )
        AccesoCircular(
            icon = if (isDark) Icons.Outlined.LightMode else Icons.Outlined.DarkMode,
            label = if (isDark) "Claro" else "Oscuro",
            onClick = onToggleTheme,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun AccesoCircular(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.clickable(onClick = onClick).padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
            modifier = Modifier.size(52.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun DatoMini(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

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
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.perfil_cuenta_inactiva),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
