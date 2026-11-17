package com.bbva.bonifica.ui.descuentos

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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bbva.bonifica.R
import com.bbva.bonifica.core.UiState
import com.bbva.bonifica.core.toMessage
import com.bbva.bonifica.domain.model.PointOfSale
import com.bbva.bonifica.domain.model.Product
import com.bbva.bonifica.ui.components.BonificaTopBar
import com.bbva.bonifica.ui.components.EmptyView
import com.bbva.bonifica.ui.components.ErrorBanner
import com.bbva.bonifica.ui.components.ErrorView
import com.bbva.bonifica.ui.components.LoadingView
import com.bbva.bonifica.ui.components.MoneyText
import com.bbva.bonifica.ui.components.PrimaryButton
import com.bbva.bonifica.ui.components.SecondaryButton
import com.bbva.bonifica.ui.components.StatusChip
import com.bbva.bonifica.ui.operaciones.channelName
import com.bbva.bonifica.ui.theme.BbvaBlue
import com.bbva.bonifica.ui.theme.BbvaBlueLight
import com.bbva.bonifica.ui.theme.BbvaGray300
import com.bbva.bonifica.ui.theme.BbvaGray700
import com.bbva.bonifica.ui.theme.BbvaNavy
import com.bbva.bonifica.ui.theme.BbvaWhite
import com.bbva.bonifica.ui.theme.Warning

/**
 * Pantalla "Calcular descuento" (Módulo 3, ruta 1).
 *
 * Selector de operación (las `OPEN` van primero, marcadas como recomendadas),
 * producto elegido en el catálogo, cantidad con stepper (≥ 1) y botón **Calcular**.
 * Sin producto la pantalla invita a entrar al catálogo en vez de fallar más tarde.
 */
@Composable
fun CalcularScreen(
    viewModel: CalcularViewModel,
    onResultado: (String) -> Unit,
    onOpenCatalogo: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is CalcularEvent.Calculado -> onResultado(event.discountId)
            }
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        BonificaTopBar(
            title = stringResource(R.string.calcular_titulo),
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
            is UiState.Content -> CalcularContent(
                data = current.data,
                viewModel = viewModel,
                onOpenCatalogo = onOpenCatalogo,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun CalcularContent(
    data: CalcularData,
    viewModel: CalcularViewModel,
    onOpenCatalogo: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        SeccionTitulo(stringResource(R.string.calcular_seccion_operacion))
        Spacer(Modifier.height(8.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = BbvaWhite),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            data.operations.forEachIndexed { index, operation ->
                if (index > 0) {
                    HorizontalDivider(color = BbvaGray300.copy(alpha = 0.6f))
                }
                OperacionFila(
                    operation = operation,
                    selected = operation.pointOfSaleReference == data.selectedPosRef,
                    enabled = !data.calculating,
                    onClick = { viewModel.selectOperation(operation.pointOfSaleReference) }
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        SeccionTitulo(stringResource(R.string.calcular_seccion_producto))
        Spacer(Modifier.height(8.dp))

        val product = data.product
        if (product == null) {
            SinProductoCard(onOpenCatalogo = onOpenCatalogo)
        } else {
            ProductoCard(product = product)
        }

        Spacer(Modifier.height(20.dp))

        SeccionTitulo(stringResource(R.string.calcular_cantidad))
        Spacer(Modifier.height(8.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = BbvaWhite),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                StepperBoton(
                    icon = Icons.Outlined.Remove,
                    contentDescription = stringResource(R.string.calcular_disminuir),
                    enabled = data.quantity > CalcularViewModel.MIN_QUANTITY && !data.calculating,
                    onClick = viewModel::decreaseQuantity
                )
                Text(
                    text = data.quantity.toString(),
                    modifier = Modifier
                        .widthIn(min = 56.dp)
                        .padding(horizontal = 8.dp),
                    style = MaterialTheme.typography.titleLarge,
                    color = BbvaNavy,
                    textAlign = TextAlign.Center
                )
                StepperBoton(
                    icon = Icons.Outlined.Add,
                    contentDescription = stringResource(R.string.calcular_aumentar),
                    enabled = data.quantity < CalcularViewModel.MAX_QUANTITY && !data.calculating,
                    onClick = viewModel::increaseQuantity
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        val error = data.calcError
        if (error != null) {
            ErrorBanner(message = error.toMessage())
            Spacer(Modifier.height(12.dp))
        }

        PrimaryButton(
            text = stringResource(R.string.calcular_descuento),
            onClick = viewModel::calcular,
            enabled = data.canCalculate,
            loading = data.calculating,
            loadingText = stringResource(R.string.calcular_calculando)
        )

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun SeccionTitulo(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        color = BbvaNavy
    )
}

@Composable
private fun OperacionFila(
    operation: PointOfSale,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = onClick,
            enabled = enabled,
            colors = RadioButtonDefaults.colors(
                selectedColor = BbvaBlue,
                unselectedColor = BbvaGray300,
                disabledSelectedColor = BbvaBlue,
                disabledUnselectedColor = BbvaGray300
            )
        )
        Spacer(Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = operation.officeName,
                style = MaterialTheme.typography.titleSmall,
                color = BbvaNavy
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = "${operation.city} · ${channelName(operation.channel)}",
                style = MaterialTheme.typography.bodySmall,
                color = BbvaGray700
            )
        }
        Spacer(Modifier.width(8.dp))
        if (operation.status == "OPEN") {
            Surface(shape = RoundedCornerShape(50), color = BbvaBlueLight.copy(alpha = 0.15f)) {
                Text(
                    text = stringResource(R.string.calcular_recomendada),
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    color = BbvaBlueLight,
                    style = MaterialTheme.typography.labelMedium
                )
            }
        } else {
            StatusChip(status = operation.status)
        }
    }
}

@Composable
private fun ProductoCard(product: Product, modifier: Modifier = Modifier) {
    val discontinued = product.status == "DISCONTINUED"

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = BbvaWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = product.name,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium,
                    color = BbvaNavy
                )
                Spacer(Modifier.width(8.dp))
                StatusChip(status = product.status)
            }

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${product.productReference} · ${product.category}",
                    style = MaterialTheme.typography.bodySmall,
                    color = BbvaGray700
                )
                MoneyText(amount = product.listPrice, currency = product.currency, color = BbvaNavy)
            }

            if (discontinued) {
                Spacer(Modifier.height(12.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = Warning.copy(alpha = 0.15f)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Warning,
                            contentDescription = null,
                            tint = BbvaNavy,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(
                                text = stringResource(R.string.producto_no_disponible),
                                style = MaterialTheme.typography.titleSmall,
                                color = BbvaNavy
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = stringResource(R.string.producto_no_disponible_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = BbvaNavy
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SinProductoCard(onOpenCatalogo: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = BbvaWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
            Text(
                text = stringResource(R.string.calcular_sin_producto),
                style = MaterialTheme.typography.bodyMedium,
                color = BbvaGray700
            )
            Spacer(Modifier.height(12.dp))
            SecondaryButton(
                text = stringResource(R.string.calcular_ir_catalogo),
                onClick = onOpenCatalogo
            )
        }
    }
}

@Composable
private fun StepperBoton(
    icon: ImageVector,
    contentDescription: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = CircleShape,
        color = BbvaBlueLight.copy(alpha = 0.12f),
        modifier = modifier.size(44.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            IconButton(onClick = onClick, enabled = enabled) {
                Icon(
                    imageVector = icon,
                    contentDescription = contentDescription,
                    tint = if (enabled) BbvaBlue else BbvaGray300,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}
