package com.bbva.bonifica.ui.catalogo

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
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
import com.bbva.bonifica.ui.components.StatusChip
import com.bbva.bonifica.ui.theme.BbvaBlue
import com.bbva.bonifica.ui.theme.BbvaGray300
import com.bbva.bonifica.ui.theme.BbvaGray700
import com.bbva.bonifica.ui.theme.BbvaNavy
import com.bbva.bonifica.ui.theme.BbvaWhite

/**
 * Pantalla de productos de una categoría (Módulo 2, ruta 13).
 * Incluye buscador local por nombre y tarjetas con importe, frecuencia y estado.
 */
@Composable
fun ProductosScreen(
    viewModel: ProductosViewModel,
    onProductSelected: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    val categoryTitle = getCategoryTitle(viewModel.category)

    Column(modifier = modifier.fillMaxSize()) {
        BonificaTopBar(
            title = categoryTitle,
            onBack = onBack
        )

        when (val current = state) {
            is UiState.Loading -> LoadingView(Modifier.weight(1f))
            is UiState.Empty -> EmptyView(
                message = stringResource(R.string.productos_categoria_vacia),
                modifier = Modifier.weight(1f)
            )
            is UiState.Error -> ErrorView(
                message = current.error.toMessage(),
                onRetry = viewModel::load,
                modifier = Modifier.weight(1f)
            )
            is UiState.Content -> ProductosContent(
                data = current.data,
                onSearchChange = viewModel::onSearchQueryChange,
                onProductSelected = onProductSelected,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun ProductosContent(
    data: ProductosUiData,
    onSearchChange: (String) -> Unit,
    onProductSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        // Buscador local
        OutlinedTextField(
            value = data.searchQuery,
            onValueChange = onSearchChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            placeholder = { Text(stringResource(R.string.productos_buscar_placeholder), color = BbvaGray700) },
            leadingIcon = {
                Icon(Icons.Outlined.Search, contentDescription = null, tint = BbvaGray700)
            },
            trailingIcon = if (data.searchQuery.isNotEmpty()) {
                {
                    IconButton(onClick = { onSearchChange("") }) {
                        Icon(Icons.Outlined.Clear, contentDescription = null, tint = BbvaGray700)
                    }
                }
            } else null,
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = BbvaBlue,
                unfocusedBorderColor = BbvaGray300,
                focusedContainerColor = BbvaWhite,
                unfocusedContainerColor = BbvaWhite,
                focusedTextColor = BbvaNavy,
                unfocusedTextColor = BbvaNavy
            )
        )

        if (data.filteredProducts.isEmpty()) {
            EmptyView(
                message = stringResource(R.string.productos_sin_resultados),
                modifier = Modifier.weight(1f)
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(data.filteredProducts, key = { it.productReference }) { product ->
                    ProductCard(
                        product = product,
                        onClick = { onProductSelected(product.productReference) }
                    )
                }
                item {
                    Spacer(Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
fun ProductCard(
    product: Product,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = BbvaWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = product.productReference,
                    style = MaterialTheme.typography.labelMedium,
                    color = BbvaGray700
                )
                StatusChip(status = product.status)
            }

            Spacer(Modifier.height(8.dp))

            Text(
                text = product.name,
                style = MaterialTheme.typography.titleMedium,
                color = BbvaNavy
            )

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.producto_comision_lista),
                        style = MaterialTheme.typography.bodySmall,
                        color = BbvaGray700
                    )
                    MoneyText(
                        amount = product.listPrice,
                        currency = product.currency,
                        color = BbvaNavy
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = formatBillingFrequency(product.billingFrequency),
                        style = MaterialTheme.typography.bodySmall,
                        color = BbvaGray700
                    )
                    Spacer(Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = BbvaBlue,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun formatBillingFrequency(frequency: String): String = when (frequency) {
    "MONTHLY" -> stringResource(R.string.frecuencia_mensual)
    "ANNUAL" -> stringResource(R.string.frecuencia_anual)
    "PER_OPERATION" -> stringResource(R.string.frecuencia_por_operacion)
    "ONE_TIME" -> stringResource(R.string.frecuencia_unica_vez)
    else -> frequency
}

@Composable
private fun getCategoryTitle(category: String): String = when (category) {
    "CUENTAS" -> stringResource(R.string.categoria_cuentas)
    "TARJETAS" -> stringResource(R.string.categoria_tarjetas)
    "PRESTAMOS" -> stringResource(R.string.categoria_prestamos)
    "SEGUROS" -> stringResource(R.string.categoria_seguros)
    "TRANSFERENCIAS" -> stringResource(R.string.categoria_transferencias)
    else -> category
}
