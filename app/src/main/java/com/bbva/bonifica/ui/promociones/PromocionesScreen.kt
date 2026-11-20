package com.bbva.bonifica.ui.promociones

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import com.bbva.bonifica.ui.theme.BbvaBlue
import com.bbva.bonifica.ui.theme.BbvaGray300
import com.bbva.bonifica.ui.theme.BbvaGray700
import com.bbva.bonifica.ui.theme.BbvaNavy
import com.bbva.bonifica.ui.theme.BbvaWhite

/**
 * Pantalla de Promociones (Módulo 4): dos pestañas, **Políticas** (ruta 6) y
 * **Campañas** (ruta 7).
 *
 * - Políticas: tarjetas con valor, alcance, monto mínimo, canal, moneda y vigencia;
 *   filtros de categoría, moneda y canal, y se ocultan las vencidas (§12).
 * - Campañas: selector de estado (`ACTIVE` por defecto) con nombre, segmento,
 *   canal y fechas.
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
    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TabPill(
                text = stringResource(R.string.promo_tab_politicas),
                selected = data.tab == PromocionesTab.POLITICAS,
                onClick = { viewModel.selectTab(PromocionesTab.POLITICAS) },
                modifier = Modifier.weight(1f)
            )
            TabPill(
                text = stringResource(R.string.promo_tab_campanas),
                selected = data.tab == PromocionesTab.CAMPANAS,
                onClick = { viewModel.selectTab(PromocionesTab.CAMPANAS) },
                modifier = Modifier.weight(1f)
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
            .padding(horizontal = 16.dp)
    ) {
        FilterSection(
            label = stringResource(R.string.promo_filtro_categoria),
            options = listOf(null to stringResource(R.string.filtro_todas)) +
                POLICY_CATEGORIES.map { it to categoryName(it) },
            selected = data.categoryFilter,
            onSelect = viewModel::selectCategory
        )
        FilterSection(
            label = stringResource(R.string.promo_filtro_moneda),
            options = listOf(null to stringResource(R.string.filtro_todas)) +
                POLICY_CURRENCIES.map { it to it },
            selected = data.currencyFilter,
            onSelect = viewModel::selectCurrency
        )
        FilterSection(
            label = stringResource(R.string.promo_filtro_canal),
            options = listOf(null to stringResource(R.string.filtro_todos)) +
                POLICY_CHANNELS.map { it to channelName(it) },
            selected = data.channelFilter,
            onSelect = viewModel::selectChannel
        )

        Spacer(Modifier.height(16.dp))

        if (data.visiblePolicies.isEmpty()) {
            // Sin resultados por los filtros (no por falta de datos): el aviso queda
            // junto a los filtros para que el usuario pueda cambiarlos.
            Text(
                text = stringResource(R.string.promo_politicas_sin_resultados),
                style = MaterialTheme.typography.bodyMedium,
                color = BbvaGray700,
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
private fun FilterSection(
    label: String,
    options: List<Pair<String?, String>>,
    selected: String?,
    onSelect: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = BbvaGray700
        )
        Spacer(Modifier.height(6.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            options.forEach { (value, text) ->
                FilterChipItem(
                    label = text,
                    selected = value == selected,
                    onClick = { onSelect(value) }
                )
            }
        }
    }
}

@Composable
private fun PolicyCard(policy: Policy, modifier: Modifier = Modifier) {
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
                    text = policy.policyId,
                    style = MaterialTheme.typography.labelLarge,
                    color = BbvaGray700
                )
                StatusChip(status = policy.status)
            }

            Spacer(Modifier.height(8.dp))

            Text(
                text = policy.name,
                style = MaterialTheme.typography.titleMedium,
                color = BbvaNavy
            )

            Spacer(Modifier.height(4.dp))

            // El valor del descuento es el dato que destaca de la tarjeta (§12).
            Text(
                text = policyValue(policy),
                style = MaterialTheme.typography.headlineSmall,
                color = BbvaBlue
            )

            Spacer(Modifier.height(8.dp))

            FilaPromo(
                label = stringResource(R.string.promo_alcance),
                value = alcanceTexto(policy)
            )
            FilaPromo(
                label = stringResource(R.string.promo_minimo),
                value = minimoTexto(policy)
            )
            FilaPromo(
                label = stringResource(R.string.promo_filtro_canal),
                value = canalTexto(policy.channel)
            )
            FilaPromo(
                label = stringResource(R.string.promo_filtro_moneda),
                value = policy.currency
            )
            FilaPromo(
                label = stringResource(R.string.promo_vigencia),
                value = "${formatDate(policy.validFrom)} – ${formatDate(policy.validTo)}"
            )
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
            .padding(horizontal = 16.dp)
    ) {
        Text(
            text = stringResource(R.string.promo_estado_campana),
            style = MaterialTheme.typography.labelLarge,
            color = BbvaGray700
        )
        Spacer(Modifier.height(6.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CAMPAIGN_STATUSES.forEach { status ->
                FilterChipItem(
                    label = campaignStatusName(status),
                    selected = status == data.campaignStatus,
                    onClick = { viewModel.selectCampaignStatus(status) }
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        when {
            data.campaignsLoading -> Box(
                modifier = Modifier.fillMaxWidth().padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = BbvaBlue)
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
                color = BbvaGray700,
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
                    text = campaign.campaignId,
                    style = MaterialTheme.typography.labelLarge,
                    color = BbvaGray700
                )
                StatusChip(status = campaign.status)
            }

            Spacer(Modifier.height(8.dp))

            Text(
                text = campaign.name,
                style = MaterialTheme.typography.titleMedium,
                color = BbvaNavy
            )

            Spacer(Modifier.height(8.dp))

            FilaPromo(
                label = stringResource(R.string.promo_segmento),
                value = segmentName(campaign.targetSegment)
            )
            FilaPromo(
                label = stringResource(R.string.promo_filtro_canal),
                value = canalTexto(campaign.channel)
            )
            FilaPromo(
                label = stringResource(R.string.promo_vigencia),
                value = "${formatDate(campaign.startDate)} – ${formatDate(campaign.endDate)}"
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Auxiliares
// ---------------------------------------------------------------------------

/** Píldora de pestaña / chip de filtro: azul cuando está seleccionado (§9). */
@Composable
private fun FilterChipItem(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clickable(onClick = onClick)
            .border(1.dp, if (selected) BbvaBlue else BbvaGray300, RoundedCornerShape(50)),
        shape = RoundedCornerShape(50),
        color = if (selected) BbvaBlue else BbvaWhite
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) BbvaWhite else BbvaGray700
        )
    }
}

@Composable
private fun TabPill(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FilterChipItem(
        label = text,
        selected = selected,
        onClick = onClick,
        modifier = modifier
    )
}

@Composable
private fun FilaPromo(label: String, value: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
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
            modifier = Modifier.weight(1f, fill = false),
            style = MaterialTheme.typography.titleSmall,
            color = BbvaNavy,
            textAlign = TextAlign.End
        )
    }
}

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
    "CATEGORY" ->
        "${stringResource(R.string.promo_alcance_categoria)}: ${categoryName(policy.scopeReference)}"
    "PRODUCT" -> "${stringResource(R.string.promo_alcance_producto)}: ${policy.scopeReference}"
    else -> policy.scopeReference
}

@Composable
private fun minimoTexto(policy: Policy): String =
    if (policy.minimumAmount <= 0.0) {
        stringResource(R.string.promo_sin_minimo)
    } else {
        formatMoney(policy.minimumAmount, policy.currency)
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
