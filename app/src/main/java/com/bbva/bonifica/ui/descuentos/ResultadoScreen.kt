package com.bbva.bonifica.ui.descuentos

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bbva.bonifica.R
import com.bbva.bonifica.core.AppError
import com.bbva.bonifica.core.UiState
import com.bbva.bonifica.core.formatDate
import com.bbva.bonifica.core.toMessage
import com.bbva.bonifica.domain.model.DiscountPricing
import com.bbva.bonifica.ui.components.AmountDisplay
import com.bbva.bonifica.ui.components.BonificaTopBar
import com.bbva.bonifica.ui.components.ConfirmDialog
import com.bbva.bonifica.ui.components.EmptyView
import com.bbva.bonifica.ui.components.ErrorBanner
import com.bbva.bonifica.ui.components.ErrorView
import com.bbva.bonifica.ui.components.LoadingView
import com.bbva.bonifica.ui.components.PrimaryButton
import com.bbva.bonifica.ui.components.SecondaryButton
import com.bbva.bonifica.ui.components.StatusChip
import com.bbva.bonifica.ui.components.paymentMethodName

/**
 * Pantalla de resultado (Módulo 3, ruta 2): bruto, política aplicada, descuento
 * resaltado y **neto grande** con `AmountDisplay`. Si el estado es `INITIATED`
 * aparece el selector de medio de pago con **Confirmar** y **Anular**.
 */
@Composable
fun ResultadoScreen(
    viewModel: ResultadoViewModel,
    onVerComprobante: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val notice by viewModel.notice.collectAsStateWithLifecycle()

    Column(modifier = modifier.fillMaxSize()) {
        BonificaTopBar(
            title = stringResource(R.string.resultado_titulo),
            onBack = onBack
        )

        when (val current = state) {
            is UiState.Loading -> LoadingView(Modifier.weight(1f))
            is UiState.Empty -> EmptyView(
                message = stringResource(R.string.resultado_no_encontrado),
                modifier = Modifier.weight(1f)
            )
            is UiState.Error -> ErrorView(
                message = current.error.toMessage(),
                onRetry = viewModel::load,
                modifier = Modifier.weight(1f)
            )
            is UiState.Content -> ResultadoContent(
                data = current.data,
                notice = notice,
                viewModel = viewModel,
                onVerComprobante = onVerComprobante,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/** Acción pendiente de confirmación en el diálogo (§9). */
private enum class Accion { CONFIRMAR, ANULAR }

@Composable
private fun ResultadoContent(
    data: ResultadoData,
    notice: AppError?,
    viewModel: ResultadoViewModel,
    onVerComprobante: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val discount = data.discount
    var accion by remember { mutableStateOf<Accion?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        ResumenCard(discount = discount)

        Spacer(Modifier.height(20.dp))

        if (discount.status == "INITIATED") {
            Text(
                text = stringResource(R.string.resultado_pago_seccion),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(Modifier.height(8.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                PAYMENT_METHODS.forEachIndexed { index, method ->
                    if (index > 0) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                    }
                    MedioPagoFila(
                        method = method,
                        selected = method == data.paymentMethod,
                        enabled = !data.saving,
                        onClick = { viewModel.selectPaymentMethod(method) }
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
        }

        notice?.let {
            ErrorBanner(message = it.toMessage())
            Spacer(Modifier.height(12.dp))
        }

        when (discount.status) {
            "INITIATED" -> {
                PrimaryButton(
                    text = stringResource(R.string.resultado_confirmar),
                    onClick = { accion = Accion.CONFIRMAR },
                    enabled = !data.saving,
                    loading = data.saving && data.pendingStatus == "APPLIED",
                    loadingText = stringResource(R.string.resultado_confirmando)
                )
                Spacer(Modifier.height(12.dp))
                SecondaryButton(
                    text = stringResource(R.string.resultado_anular),
                    onClick = { accion = Accion.ANULAR },
                    enabled = !data.saving,
                    loading = data.saving && data.pendingStatus == "CANCELLED",
                    loadingText = stringResource(R.string.resultado_anulando)
                )
            }
            "APPLIED" -> {
                val payId = data.paymentOrderId
                if (payId != null) {
                    PrimaryButton(
                        text = stringResource(R.string.resultado_ver_comprobante),
                        onClick = { onVerComprobante(payId) }
                    )
                }
            }
            else -> Text(
                text = stringResource(R.string.resultado_anulado),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(Modifier.height(16.dp))

        accion?.let { pending ->
            val confirmar = pending == Accion.CONFIRMAR
            ConfirmDialog(
                title = stringResource(
                    if (confirmar) R.string.dialog_confirmar_titulo else R.string.dialog_anular_titulo
                ),
                message = stringResource(
                    if (confirmar) R.string.dialog_confirmar_mensaje else R.string.dialog_anular_mensaje
                ),
                confirmText = stringResource(
                    if (confirmar) R.string.resultado_confirmar else R.string.resultado_anular
                ),
                dismissText = stringResource(R.string.cancelar),
                onConfirm = {
                    accion = null
                    if (confirmar) viewModel.confirmar() else viewModel.anular()
                },
                onDismiss = { accion = null }
            )
        }
    }
}

@Composable
private fun ResumenCard(discount: DiscountPricing, modifier: Modifier = Modifier) {
    val sinPolitica = discount.policyId == "NONE" || discount.policyName.isBlank()

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = discount.discountPricingId,
                    style = MaterialTheme.typography.labelLarge.copy(letterSpacing = 0.8.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                StatusChip(status = discount.status)
            }

            Spacer(Modifier.height(20.dp))

            AmountDisplay(
                grossAmount = discount.grossAmount,
                discountAmount = discount.discountAmount,
                netAmount = discount.netAmount,
                policyName = if (sinPolitica) {
                    stringResource(R.string.resultado_sin_politica)
                } else {
                    discount.policyName
                },
                currency = discount.currency
            )

            Spacer(Modifier.height(20.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
            Spacer(Modifier.height(8.dp))

            FilaResultado(
                label = stringResource(R.string.resultado_producto),
                value = "${discount.productReference} · ${discount.category}"
            )
            FilaResultado(
                label = stringResource(R.string.resultado_cantidad),
                value = discount.quantity.toString()
            )
            FilaResultado(
                label = stringResource(R.string.resultado_operacion),
                value = discount.pointOfSaleReference
            )
            FilaResultado(
                label = stringResource(R.string.resultado_fecha),
                value = formatDate(discount.createdAt)
            )
        }
    }
}

@Composable
private fun FilaResultado(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = MaterialTheme.colorScheme.onSurface
) {
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
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            color = valueColor,
            textAlign = TextAlign.End
        )
    }
}

@Composable
private fun MedioPagoFila(
    method: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = onClick,
            enabled = enabled,
            colors = RadioButtonDefaults.colors(
                selectedColor = MaterialTheme.colorScheme.primary,
                unselectedColor = MaterialTheme.colorScheme.outline,
                disabledSelectedColor = MaterialTheme.colorScheme.primary,
                disabledUnselectedColor = MaterialTheme.colorScheme.outline
            )
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = paymentMethodName(method),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
