package com.bbva.bonifica.ui.promociones

import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bbva.bonifica.R
import com.bbva.bonifica.core.UiState
import com.bbva.bonifica.core.formatDate
import com.bbva.bonifica.core.formatMoney
import com.bbva.bonifica.core.toMessage
import com.bbva.bonifica.domain.model.Campaign
import com.bbva.bonifica.domain.model.Policy
import com.bbva.bonifica.ui.components.BonificaTopBar
import com.bbva.bonifica.ui.components.EmptyView
import com.bbva.bonifica.ui.components.ErrorBanner
import com.bbva.bonifica.ui.components.ErrorView
import com.bbva.bonifica.ui.components.LoadingView
import com.bbva.bonifica.ui.components.SecondaryButton
import com.bbva.bonifica.ui.components.StatusChip
import com.bbva.bonifica.ui.operaciones.channelName

/**
 * Pantalla de Promociones (Módulo 4): dos pestañas, **Políticas** (ruta 6) y
 * **Campañas** (ruta 7).
 *
 * Diseño limpio y compacto:
 * - Filtros mediante menús desplegables (Dropdowns) en una sola fila, sin saturar la pantalla.
 * - Tarjetas con badge destacado del descuento y metadatos resumidos en una línea.
 */
@Composable
fun PromocionesScreen(
    viewModel: PromocionesViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(modifier = modifier.fillMaxSize()) {
        BonificaTopBar(
            title = stringResource(R.string.promo_titulo),
            onBack = onBack
        )

        when (val current = state) {
            is UiState.Loading -> LoadingView(Modifier.weight(1f))
            is UiState.Empty -> EmptyView(
                message = stringResource(R.string.promo_politicas_vacias),
                modifier = Modifier.weight(1f)
            )
            is UiState.Error -> ErrorView(
                message = current.error.toMessage(),
                onRetry = viewModel::load,
                modifier = Modifier.weight(1f)
            )
            is UiState.Content -> PromocionesContent(
                data = current.data,
                viewModel = viewModel,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun PromocionesContent(
    data: PromocionesData,
    viewModel: PromocionesViewModel,
    modifier: Modifier = Modifier
) {
    val selectedTabIndex = if (data.tab == PromocionesTab.POLITICAS) 0 else 1

    Column(modifier = modifier.fillMaxSize()) {
        TabRow(
            selectedTabIndex = selectedTabIndex,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary
        ) {
            Tab(
                selected = selectedTabIndex == 0,
                onClick = { viewModel.selectTab(PromocionesTab.POLITICAS) },
                text = { Text(stringResource(R.string.promo_tab_politicas)) }
            )
            Tab(
                selected = selectedTabIndex == 1,
                onClick = { viewModel.selectTab(PromocionesTab.CAMPANAS) },
                text = { Text(stringResource(R.string.promo_tab_campanas)) }
            )
        }

        when (data.tab) {
            PromocionesTab.POLITICAS -> PoliticasTab(
                data = data,
                viewModel = viewModel,
                modifier = Modifier.weight(1f)
            )
            PromocionesTab.CAMPANAS -> CampanasTab(
                data = data,
                viewModel = viewModel,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Pestaña Políticas (ruta 6)
// ---------------------------------------------------------------------------

@Composable
private fun PoliticasTab(
    data: PromocionesData,
    viewModel: PromocionesViewModel,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // Fila única de 3 Dropdowns compactos
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterDropdown(
                label = stringResource(R.string.promo_filtro_categoria),
                selectedLabel = data.categoryFilter?.let { categoryName(it) }
                    ?: stringResource(R.string.filtro_todas),
                options = listOf(null to stringResource(R.string.filtro_todas)) +
                    POLICY_CATEGORIES.map { it to categoryName(it) },
                onSelect = viewModel::selectCategory,
                modifier = Modifier.weight(1.2f)
            )
            FilterDropdown(
                label = stringResource(R.string.promo_filtro_moneda),
                selectedLabel = data.currencyFilter ?: stringResource(R.string.filtro_todas),
                options = listOf(null to stringResource(R.string.filtro_todas)) +
                    POLICY_CURRENCIES.map { it to it },
                onSelect = viewModel::selectCurrency,
                modifier = Modifier.weight(0.9f)
            )
            FilterDropdown(
                label = stringResource(R.string.promo_filtro_canal),
                selectedLabel = data.channelFilter?.let { channelName(it) }
                    ?: stringResource(R.string.filtro_todos),
                options = listOf(null to stringResource(R.string.filtro_todos)) +
                    POLICY_CHANNELS.map { it to channelName(it) },
                onSelect = viewModel::selectChannel,
                modifier = Modifier.weight(1.1f)
            )
        }

        Spacer(Modifier.height(14.dp))

        if (data.visiblePolicies.isEmpty()) {
            Text(
                text = stringResource(R.string.promo_politicas_sin_resultados),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 12.dp)
            )
        } else {
            data.visiblePolicies.forEach { policy ->
                PolicyCard(policy = policy)
                Spacer(Modifier.height(12.dp))
            }
        }

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun <T> FilterDropdown(
    label: String,
    selectedLabel: String,
    options: List<Pair<T, String>>,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = true }
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.7f),
                    shape = RoundedCornerShape(12.dp)
                ),
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = selectedLabel,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        imageVector = Icons.Outlined.KeyboardArrowDown,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { (value, text) ->
                DropdownMenuItem(
                    text = { Text(text) },
                    onClick = {
                        expanded = false
                        onSelect(value)
                    }
                )
            }
        }
    }
}

@Composable
private fun PolicyCard(policy: Policy, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Badge destacado con el porcentaje/monto del descuento
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                modifier = Modifier.size(width = 72.dp, height = 64.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = policyValue(policy),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
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
                        text = policy.policyId,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    StatusChip(status = policy.status)
                }

                Spacer(Modifier.height(4.dp))

                Text(
                    text = policy.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(Modifier.height(6.dp))

                // Metadatos resumidos en una línea limpia
                Text(
                    text = "${alcanceTexto(policy)} · ${canalTexto(policy.channel)} · ${policy.currency}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(Modifier.height(4.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.CalendarToday,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "${formatDate(policy.validFrom)} – ${formatDate(policy.validTo)} · ${minimoTexto(policy)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Pestaña Campañas (ruta 7)
// ---------------------------------------------------------------------------

@Composable
private fun CampanasTab(
    data: PromocionesData,
    viewModel: PromocionesViewModel,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        FilterDropdown(
            label = stringResource(R.string.promo_estado_campana),
            selectedLabel = campaignStatusName(data.campaignStatus),
            options = CAMPAIGN_STATUSES.map { it to campaignStatusName(it) },
            onSelect = viewModel::selectCampaignStatus,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(14.dp))

        when {
            data.campaignsLoading -> Box(
                modifier = Modifier.fillMaxWidth().padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }

            data.campaignsError != null -> {
                ErrorBanner(message = data.campaignsError.toMessage())
                Spacer(Modifier.height(12.dp))
                SecondaryButton(
                    text = stringResource(R.string.reintentar),
                    onClick = viewModel::retryCampaigns
                )
            }

            data.campaigns.isEmpty() -> Text(
                text = stringResource(R.string.promo_campanas_vacias),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 12.dp)
            )

            else -> data.campaigns.forEach { campaign ->
                CampaignCard(campaign = campaign)
                Spacer(Modifier.height(12.dp))
            }
        }

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun CampaignCard(campaign: Campaign, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
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
                    text = campaign.campaignId,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                StatusChip(status = campaign.status)
            }

            Spacer(Modifier.height(6.dp))

            Text(
                text = campaign.name,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(Modifier.height(6.dp))

            Text(
                text = "${segmentName(campaign.targetSegment)} · ${canalTexto(campaign.channel)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(4.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.CalendarToday,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(12.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = "${formatDate(campaign.startDate)} – ${formatDate(campaign.endDate)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Auxiliares
// ---------------------------------------------------------------------------

/** "10%" / "S/ 20" según el tipo de descuento (§12). */
private fun policyValue(policy: Policy): String = when (policy.discountType) {
    "PERCENTAGE" -> {
        val value = policy.discountValue
        val text = if (value % 1.0 == 0.0) value.toLong().toString() else value.toString()
        "$text%"
    }
    "FIXED_AMOUNT" -> formatMoney(policy.discountValue, policy.currency)
    else -> "-"
}

@Composable
private fun alcanceTexto(policy: Policy): String = when (policy.scope) {
    "ALL" -> stringResource(R.string.promo_alcance_todo)
    "CATEGORY" -> categoryName(policy.scopeReference)
    "PRODUCT" -> policy.scopeReference
    else -> policy.scopeReference
}

@Composable
private fun minimoTexto(policy: Policy): String =
    if (policy.minimumAmount <= 0.0) {
        stringResource(R.string.promo_sin_minimo)
    } else {
        "Mín. ${formatMoney(policy.minimumAmount, policy.currency)}"
    }

/** Canal `ALL` no es un canal concreto: se muestra como "Todos". */
@Composable
private fun canalTexto(channel: String): String =
    if (channel == "ALL") stringResource(R.string.filtro_todos) else channelName(channel)

@Composable
private fun categoryName(category: String): String = when (category) {
    "CUENTAS" -> stringResource(R.string.categoria_cuentas)
    "TARJETAS" -> stringResource(R.string.categoria_tarjetas)
    "PRESTAMOS" -> stringResource(R.string.categoria_prestamos)
    "SEGUROS" -> stringResource(R.string.categoria_seguros)
    "TRANSFERENCIAS" -> stringResource(R.string.categoria_transferencias)
    else -> category
}

@Composable
private fun segmentName(segment: String): String = when (segment) {
    "PERSONAL" -> stringResource(R.string.segmento_personal)
    "PREFERENTE" -> stringResource(R.string.segmento_preferente)
    "PREMIUM" -> stringResource(R.string.segmento_premium)
    "JOVEN" -> stringResource(R.string.segmento_joven)
    "PYME" -> stringResource(R.string.segmento_pyme)
    "ALL" -> stringResource(R.string.segmento_todos)
    else -> segment
}

@Composable
private fun campaignStatusName(status: String): String = when (status) {
    "ACTIVE" -> stringResource(R.string.campana_estado_activas)
    "SCHEDULED" -> stringResource(R.string.campana_estado_programadas)
    "FINISHED" -> stringResource(R.string.campana_estado_finalizadas)
    "PAUSED" -> stringResource(R.string.campana_estado_pausadas)
    else -> status
}

/** Opciones de los filtros de Políticas (§12). */
private val POLICY_CATEGORIES =
    listOf("CUENTAS", "TARJETAS", "PRESTAMOS", "SEGUROS", "TRANSFERENCIAS")
private val POLICY_CURRENCIES = listOf("PEN", "USD")
private val POLICY_CHANNELS = listOf("BRANCH", "MOBILE_APP", "WEB", "ATM", "AGENT")

/** Estados del selector de Campañas; `ACTIVE` es el primero, por defecto (§12). */
private val CAMPAIGN_STATUSES = listOf("ACTIVE", "SCHEDULED", "FINISHED", "PAUSED")
