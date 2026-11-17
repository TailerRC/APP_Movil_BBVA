package com.bbva.bonifica.ui.catalogo

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bbva.bonifica.R
import com.bbva.bonifica.core.UiState
import com.bbva.bonifica.core.toMessage
import com.bbva.bonifica.domain.model.Product
import com.bbva.bonifica.ui.components.BonificaTopBar
import com.bbva.bonifica.ui.components.EmptyView
import com.bbva.bonifica.ui.components.ErrorView
import com.bbva.bonifica.ui.components.LoadingView
import com.bbva.bonifica.ui.components.MoneyText
import com.bbva.bonifica.ui.components.PrimaryButton
import com.bbva.bonifica.ui.components.StatusChip
import com.bbva.bonifica.ui.theme.BbvaGray300
import com.bbva.bonifica.ui.theme.BbvaGray700
import com.bbva.bonifica.ui.theme.BbvaNavy
import com.bbva.bonifica.ui.theme.BbvaWhite
import com.bbva.bonifica.ui.theme.Warning

/**
 * Pantalla de Detalle de Producto (Módulo 2, ruta 9).
 * Muestra información completa del producto y botón para calcular descuento.
 * Si el producto está `DISCONTINUED`, el botón se bloquea / oculta con aviso.
 */
@Composable
fun ProductoDetalleScreen(
    viewModel: ProductoDetalleViewModel,
    onCalcular: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(modifier = modifier.fillMaxSize()) {
        BonificaTopBar(
            title = stringResource(R.string.producto_detalle_titulo),
            onBack = onBack
        )

        when (val current = state) {
            is UiState.Loading -> LoadingView(Modifier.weight(1f))
            is UiState.Empty -> EmptyView(
                message = stringResource(R.string.producto_no_encontrado),
                modifier = Modifier.weight(1f)
            )
            is UiState.Error -> ErrorView(
                message = current.error.toMessage(),
                onRetry = viewModel::load,
                modifier = Modifier.weight(1f)
            )
            is UiState.Content -> ProductoDetalleContent(
                product = current.data,
                onCalcular = { onCalcular(current.data.productReference) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun ProductoDetalleContent(
    product: Product,
    onCalcular: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDiscontinued = product.status == "DISCONTINUED"

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = BbvaWhite),
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
                        text = product.productReference,
                        style = MaterialTheme.typography.labelLarge,
                        color = BbvaGray700
                    )
                    StatusChip(status = product.status)
                }

                Spacer(Modifier.height(12.dp))

                Text(
                    text = product.name,
                    style = MaterialTheme.typography.titleLarge,
                    color = BbvaNavy
                )

                Spacer(Modifier.height(16.dp))

                DetalleFila(
                    label = stringResource(R.string.producto_categoria),
                    value = product.category
                )

                DetalleFila(
                    label = stringResource(R.string.producto_frecuencia),
                    value = formatBillingFrequency(product.billingFrequency)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.producto_comision_lista),
                        style = MaterialTheme.typography.bodyMedium,
                        color = BbvaGray700
                    )
                    MoneyText(
                        amount = product.listPrice,
                        currency = product.currency,
                        color = BbvaNavy
                    )
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        if (isDiscontinued) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = Warning.copy(alpha = 0.15f)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Warning,
                        contentDescription = null,
                        tint = BbvaNavy,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.width(12.dp))
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
        } else {
            PrimaryButton(
                text = stringResource(R.string.calcular_descuento),
                onClick = onCalcular
            )
        }

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun DetalleFila(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = BbvaGray700
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = BbvaNavy,
            textAlign = TextAlign.End
        )
    }
    HorizontalDivider(color = BbvaGray300.copy(alpha = 0.6f))
}
