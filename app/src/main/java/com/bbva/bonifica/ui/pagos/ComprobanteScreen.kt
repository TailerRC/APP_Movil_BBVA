package com.bbva.bonifica.ui.pagos

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
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
import com.bbva.bonifica.domain.model.PaymentOrder
import com.bbva.bonifica.ui.components.BonificaTopBar
import com.bbva.bonifica.ui.components.EmptyView
import com.bbva.bonifica.ui.components.ErrorView
import com.bbva.bonifica.ui.components.LoadingView
import com.bbva.bonifica.ui.components.PrimaryButton
import com.bbva.bonifica.ui.components.StatusChip
import com.bbva.bonifica.ui.components.paymentMethodName
import com.bbva.bonifica.ui.theme.BbvaGray300
import com.bbva.bonifica.ui.theme.BbvaGray700
import com.bbva.bonifica.ui.theme.BbvaNavy
import com.bbva.bonifica.ui.theme.BbvaWhite

/**
 * Comprobante de pago (Módulo 6, ruta 12): tarjeta tipo recibo con importe,
 * moneda, medio, estado, fecha y `discountPricingId`. El botón **Ver historial**
 * abre la línea de tiempo del descuento asociado (ruta 4, §12).
 */
@Composable
fun ComprobanteScreen(
    viewModel: ComprobanteViewModel,
    onVerHistorial: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(modifier = modifier.fillMaxSize()) {
        BonificaTopBar(
            title = stringResource(R.string.comprobante_titulo),
            onBack = onBack
        )

        when (val current = state) {
            is UiState.Loading -> LoadingView(Modifier.weight(1f))
            is UiState.Empty -> EmptyView(
                message = stringResource(R.string.comprobante_no_encontrado),
                modifier = Modifier.weight(1f)
            )
            is UiState.Error -> ErrorView(
                message = current.error.toMessage(),
                onRetry = viewModel::load,
                modifier = Modifier.weight(1f)
            )
            is UiState.Content -> ComprobanteContent(
                payment = current.data,
                onVerHistorial = onVerHistorial,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun ComprobanteContent(
    payment: PaymentOrder,
    onVerHistorial: (String) -> Unit,
    modifier: Modifier = Modifier
) {
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
            Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = payment.paymentOrderId,
                        style = MaterialTheme.typography.labelLarge,
                        color = BbvaGray700
                    )
                    StatusChip(status = payment.status)
                }

                Spacer(Modifier.height(12.dp))

                Text(
                    text = stringResource(R.string.comprobante_recibo),
                    style = MaterialTheme.typography.titleMedium,
                    color = BbvaNavy
                )

                Spacer(Modifier.height(8.dp))

                Text(
                    text = stringResource(R.string.comprobante_monto),
                    style = MaterialTheme.typography.bodyMedium,
                    color = BbvaGray700
                )

                // El importe es el número más grande del recibo (§3).
                Text(
                    text = formatMoney(payment.amount, payment.currency),
                    style = MaterialTheme.typography.headlineLarge,
                    color = BbvaNavy
                )

                Spacer(Modifier.height(12.dp))
                HorizontalDivider(color = BbvaGray300.copy(alpha = 0.6f))
                Spacer(Modifier.height(4.dp))

                FilaComprobante(
                    label = stringResource(R.string.comprobante_moneda),
                    value = payment.currency
                )
                FilaComprobante(
                    label = stringResource(R.string.pago_medio),
                    value = paymentMethodName(payment.paymentMethod)
                )
                FilaComprobante(
                    label = stringResource(R.string.comprobante_estado),
                    value = payment.status
                )
                FilaComprobante(
                    label = stringResource(R.string.pago_fecha),
                    value = formatDate(payment.paymentDate)
                )
                FilaComprobante(
                    label = stringResource(R.string.comprobante_descuento),
                    value = payment.discountPricingId
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        PrimaryButton(
            text = stringResource(R.string.comprobante_ver_historial),
            onClick = { onVerHistorial(payment.discountPricingId) }
        )

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun FilaComprobante(label: String, value: String, modifier: Modifier = Modifier) {
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
