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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Calculate
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bbva.bonifica.R
import com.bbva.bonifica.core.UiState
import com.bbva.bonifica.core.formatDate
import com.bbva.bonifica.core.toMessage
import com.bbva.bonifica.domain.model.DiscountPricing
import com.bbva.bonifica.domain.model.PointOfSale
import com.bbva.bonifica.ui.components.BonificaTopBar
import com.bbva.bonifica.ui.components.EmptyView
import com.bbva.bonifica.ui.components.ErrorView
import com.bbva.bonifica.ui.components.LoadingView
import com.bbva.bonifica.ui.components.MoneyText
import com.bbva.bonifica.ui.components.SecondaryButton
import com.bbva.bonifica.ui.components.StatusChip

/**
 * Pantalla de Detalle de Operación (Módulo 5, rutas 10 y 8).
 * Organizada en pestañas ("Información" / "Descuentos") con FAB para calcular descuento.
 */
@Composable
fun OperacionDetalleScreen(
    viewModel: OperacionDetalleViewModel,
    onCalcularDescuento: (String) -> Unit,
    onVerPagos: (String) -> Unit,
    onVerDescuento: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(modifier = modifier.fillMaxSize()) {
        BonificaTopBar(
            title = stringResource(R.string.operacion_detalle_titulo),
            onBack = onBack
        )

        when (val current = state) {
            is UiState.Loading -> LoadingView(Modifier.weight(1f))
            is UiState.Empty -> EmptyView(
                message = stringResource(R.string.operacion_no_encontrada),
                modifier = Modifier.weight(1f)
            )
            is UiState.Error -> ErrorView(
                message = current.error.toMessage(),
                onRetry = viewModel::load,
                modifier = Modifier.weight(1f)
            )
            is UiState.Content -> OperacionDetalleContent(
                data = current.data,
                onCalcularDescuento = { onCalcularDescuento(current.data.operation.pointOfSaleReference) },
                onVerPagos = { onVerPagos(current.data.operation.pointOfSaleReference) },
                onVerDescuento = onVerDescuento,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun OperacionDetalleContent(
    data: OperacionDetalleData,
    onCalcularDescuento: () -> Unit,
    onVerPagos: () -> Unit,
    onVerDescuento: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val operation = data.operation
    val discounts = data.discounts
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text(stringResource(R.string.operacion_tab_info)) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text(stringResource(R.string.operacion_tab_descuentos)) }
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                if (selectedTab == 0) {
                    OperacionInfoTab(operation = operation, onVerPagos = onVerPagos)
                } else {
                    OperacionDescuentosTab(discounts = discounts, onVerDescuento = onVerDescuento)
                }
                Spacer(Modifier.height(80.dp))
            }
        }

        ExtendedFloatingActionButton(
            onClick = onCalcularDescuento,
            icon = {
                Icon(
                    imageVector = Icons.Outlined.Calculate,
                    contentDescription = null
                )
            },
            text = { Text(stringResource(R.string.calcular_descuento)) },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        )
    }
}

@Composable
private fun OperacionInfoTab(
    operation: PointOfSale,
    onVerPagos: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = operation.pointOfSaleReference,
                    style = MaterialTheme.typography.titleLarge.copy(letterSpacing = 0.6.sp),
                    color = MaterialTheme.colorScheme.onSurface
                )
                StatusChip(status = operation.status)
            }

            Spacer(Modifier.height(16.dp))

            FilaOperacion(
                label = stringResource(R.string.operacion_oficina),
                value = "${operation.officeName} (${operation.officeCode})"
            )
            FilaOperacion(
                label = stringResource(R.string.operacion_ciudad),
                value = operation.city
            )
            FilaOperacion(
                label = stringResource(R.string.operacion_canal),
                value = channelName(operation.channel)
            )
            FilaOperacion(
                label = stringResource(R.string.operacion_fecha),
                value = formatDate(operation.createdAt),
                conDivisor = false
            )
        }
    }

    Spacer(Modifier.height(16.dp))

    SecondaryButton(
        text = stringResource(R.string.operacion_ver_pagos),
        onClick = onVerPagos
    )
}

@Composable
private fun OperacionDescuentosTab(
    discounts: List<DiscountPricing>,
    onVerDescuento: (String) -> Unit
) {
    if (discounts.isEmpty()) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Text(
                text = stringResource(R.string.operacion_sin_descuentos),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(20.dp)
            )
        }
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            discounts.forEach { discount ->
                DescuentoItemCard(
                    discount = discount,
                    onClick = { onVerDescuento(discount.discountPricingId) }
                )
            }
        }
    }
}

@Composable
private fun DescuentoItemCard(
    discount: DiscountPricing,
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
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = discount.discountPricingId,
                        style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 0.6.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    StatusChip(status = discount.status)
                }

                Spacer(Modifier.height(4.dp))

                Text(
                    text = discount.policyName.ifBlank { "Sin descuento" },
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${discount.productReference} (${discount.category})",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    MoneyText(
                        amount = discount.netAmount,
                        currency = discount.currency,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(Modifier.size(8.dp))

            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun FilaOperacion(label: String, value: String, conDivisor: Boolean = true) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.End
        )
    }
    if (conDivisor) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
    }
}
