package com.bbva.bonifica.ui.pagos

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bbva.bonifica.R
import com.bbva.bonifica.core.UiState
import com.bbva.bonifica.core.formatDate
import com.bbva.bonifica.core.toMessage
import com.bbva.bonifica.domain.model.EventLogEntry
import com.bbva.bonifica.ui.components.BonificaTopBar
import com.bbva.bonifica.ui.components.EmptyView
import com.bbva.bonifica.ui.components.ErrorView
import com.bbva.bonifica.ui.components.LoadingView
import com.bbva.bonifica.ui.components.TimelineItem
import com.bbva.bonifica.ui.operaciones.channelName

/**
 * Pantalla de Historial (Módulo 6, ruta 4): línea de tiempo
 * `INITIATED → APPLIED/CANCELLED` con fecha, canal y detalle de cada evento (§12).
 */
@Composable
fun HistorialScreen(
    viewModel: HistorialViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(modifier = modifier.fillMaxSize()) {
        BonificaTopBar(
            title = stringResource(R.string.historial_titulo),
            onBack = onBack
        )

        when (val current = state) {
            is UiState.Loading -> LoadingView(Modifier.weight(1f))
            is UiState.Empty -> EmptyView(
                message = stringResource(R.string.historial_vacio),
                modifier = Modifier.weight(1f)
            )
            is UiState.Error -> ErrorView(
                message = current.error.toMessage(),
                onRetry = viewModel::load,
                modifier = Modifier.weight(1f)
            )
            is UiState.Content -> HistorialContent(
                events = current.data,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun HistorialContent(
    events: List<EventLogEntry>,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text(
            text = stringResource(R.string.historial_subtitulo),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(12.dp))

        events.forEachIndexed { index, event ->
            TimelineItem(
                title = eventTypeTitle(event.eventType),
                timestamp = formatDate(event.eventTimestamp),
                status = event.newStatus,
                channel = event.channel?.let { channelName(it) },
                detail = event.details,
                isLast = index == events.lastIndex
            )
        }

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun eventTypeTitle(eventType: String): String = when (eventType) {
    "DISCOUNT_INITIATED" -> stringResource(R.string.historial_iniciado)
    "DISCOUNT_APPLIED" -> stringResource(R.string.historial_confirmado)
    "DISCOUNT_CANCELLED" -> stringResource(R.string.historial_anulado)
    else -> eventType
}
