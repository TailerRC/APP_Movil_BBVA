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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bbva.bonifica.R
import com.bbva.bonifica.core.AppError
import com.bbva.bonifica.core.UiState
import com.bbva.bonifica.core.formatDate
import com.bbva.bonifica.core.formatMoney
import com.bbva.bonifica.core.toMessage
import com.bbva.bonifica.domain.model.DiscountPricing
import com.bbva.bonifica.ui.components.BonificaTopBar
import com.bbva.bonifica.ui.components.ConfirmDialog
import com.bbva.bonifica.ui.components.EmptyView
import com.bbva.bonifica.ui.components.ErrorBanner
import com.bbva.bonifica.ui.components.ErrorView
import com.bbva.bonifica.ui.components.LoadingView
import com.bbva.bonifica.ui.components.PrimaryButton
import com.bbva.bonifica.ui.components.SecondaryButton
import com.bbva.bonifica.ui.components.StatusChip
import com.bbva.bonifica.ui.theme.BbvaBlue
import com.bbva.bonifica.ui.theme.BbvaGray300
import com.bbva.bonifica.ui.theme.BbvaGray700
import com.bbva.bonifica.ui.theme.BbvaNavy
import com.bbva.bonifica.ui.theme.BbvaWhite
import com.bbva.bonifica.ui.theme.Success

/**
 * Pantalla de resultado (Módulo 3, ruta 2): bruto, política aplicada, descuento
 * resaltado y **neto grande**. Si el estado es `INITIATED` aparece el selector de
 * medio de pago con **Confirmar** (`APPLIED`) y **Anular** (`CANCELLED`), ambos con
 * `ConfirmDialog`; tras confirmar queda **Ver comprobante** (§12, §13).
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
                color = BbvaNavy
            )
            Spacer(Modifier.height(8.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = BbvaWhite),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                PAYMENT_METHODS.forEachIndexed { index, method ->
                    if (index > 0) {
                        HorizontalDivider(color = BbvaGray300.copy(alpha = 0.6f))
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
                color = BbvaGray700
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
                    text = discount.discountPricingId,
                    style = MaterialTheme.typography.labelLarge,
                    color = BbvaGray700
                )
                StatusChip(status = discount.status)
            }

            Spacer(Modifier.height(12.dp))

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

            Spacer(Modifier.height(4.dp))
            HorizontalDivider(color = BbvaGray300.copy(alpha = 0.6f))
            Spacer(Modifier.height(4.dp))

            FilaResultado(
                label = stringResource(R.string.resultado_bruto),
                value = formatMoney(discount.grossAmount, discount.currency)
            )
            FilaResultado(
                label = stringResource(R.string.resultado_politica),
                value = if (sinPolitica) {
                    stringResource(R.string.resultado_sin_politica)
                } else {
                    discount.policyName
                }
            )
            // El ahorro siempre destaca (§3): en verde de éxito, como los chips APPLIED.
            FilaResultado(
                label = stringResource(R.string.resultado_descuento),
                value = formatMoney(discount.discountAmount, discount.currency),
                valueColor = Success
            )

            Spacer(Modifier.height(16.dp))

            // El neto es el número más grande de la pantalla (§3, reglas de diseño).
            Text(
                text = stringResource(R.string.resultado_neto),
                style = MaterialTheme.typography.bodyMedium,
                color = BbvaGray700
            )
            Text(
                text = formatMoney(discount.netAmount, discount.currency),
                style = MaterialTheme.typography.headlineLarge,
                color = BbvaNavy
            )
        }
    }
}

@Composable
private fun FilaResultado(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = BbvaNavy
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
            color = BbvaGray700
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
                selectedColor = BbvaBlue,
                unselectedColor = BbvaGray300,
                disabledSelectedColor = BbvaBlue,
                disabledUnselectedColor = BbvaGray300
            )
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = medioPagoNombre(method),
            style = MaterialTheme.typography.bodyLarge,
            color = BbvaNavy
        )
    }
}

@Composable
private fun medioPagoNombre(method: String): String = when (method) {
    "CARGO_EN_CUENTA" -> stringResource(R.string.pago_cargo_cuenta)
    "TARJETA_DEBITO" -> stringResource(R.string.pago_tarjeta_debito)
    "EFECTIVO_VENTANILLA" -> stringResource(R.string.pago_efectivo_ventanilla)
    else -> method
}
