package com.bbva.bonifica.ui.catalogo

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.bbva.bonifica.R
import com.bbva.bonifica.ui.components.BonificaTopBar

/**
 * Categoría visual para el catálogo (§12).
 */
data class CategoriaItem(
    val id: String,
    val titleRes: Int,
    val descriptionRes: Int,
    val icon: ImageVector
)

val CATEGORIAS_CATALOGO = listOf(
    CategoriaItem(
        id = "CUENTAS",
        titleRes = R.string.categoria_cuentas,
        descriptionRes = R.string.categoria_cuentas_desc,
        icon = Icons.Outlined.AccountBalance
    ),
    CategoriaItem(
        id = "TARJETAS",
        titleRes = R.string.categoria_tarjetas,
        descriptionRes = R.string.categoria_tarjetas_desc,
        icon = Icons.Outlined.CreditCard
    ),
    CategoriaItem(
        id = "PRESTAMOS",
        titleRes = R.string.categoria_prestamos,
        descriptionRes = R.string.categoria_prestamos_desc,
        icon = Icons.Outlined.Payments
    ),
    CategoriaItem(
        id = "SEGUROS",
        titleRes = R.string.categoria_seguros,
        descriptionRes = R.string.categoria_seguros_desc,
        icon = Icons.Outlined.Security
    ),
    CategoriaItem(
        id = "TRANSFERENCIAS",
        titleRes = R.string.categoria_transferencias,
        descriptionRes = R.string.categoria_transferencias_desc,
        icon = Icons.Outlined.SwapHoriz
    )
)

/**
 * Pantalla de Categorías del Catálogo de Comisiones (§12).
 * Grid de 2 columnas con tarjetas visuales: Cuentas, Tarjetas, Préstamos, Seguros, Transferencias.
 */
@Composable
fun CategoriasScreen(
    onCategorySelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null
) {
    Column(modifier = modifier.fillMaxSize()) {
        BonificaTopBar(
            title = stringResource(R.string.catalogo_titulo),
            onBack = onBack
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item(span = { GridItemSpan(2) }) {
                Text(
                    text = stringResource(R.string.catalogo_subtitulo),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }

            items(CATEGORIAS_CATALOGO, key = { it.id }) { cat ->
                CategoriaGridCard(
                    categoria = cat,
                    onClick = { onCategorySelected(cat.id) }
                )
            }
        }
    }
}

@Composable
private fun CategoriaGridCard(
    categoria: CategoriaItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = categoria.icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            Text(
                text = stringResource(categoria.titleRes),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(Modifier.height(4.dp))

            Text(
                text = stringResource(categoria.descriptionRes),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
