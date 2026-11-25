package com.bbva.bonifica.ui.negocio

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bbva.bonifica.R
import com.bbva.bonifica.core.toMessage
import com.bbva.bonifica.domain.model.Role
import com.bbva.bonifica.ui.components.BonificaTopBar
import com.bbva.bonifica.ui.components.ErrorBanner
import com.bbva.bonifica.ui.components.LabeledField
import com.bbva.bonifica.ui.components.PrimaryButton
import com.bbva.bonifica.ui.components.RoleGuard
import com.bbva.bonifica.ui.operaciones.channelName
import com.bbva.bonifica.ui.theme.Success
import kotlinx.coroutines.delay

/** Pausa antes de volver tras el 201, para que se lea el aviso de éxito (§12). */
private const val EXITO_MS = 900L

/**
 * Panel de Negocio · Registrar política (Módulo 7, ruta `negocio_politica`).
 *
 * Formulario de `Policy/Register` (ruta 5): `policyId` con formato `POL-###`,
 * nombre, tipo, valor, alcance + referencia (si es `PRODUCT`/`CATEGORY`),
 * monto mínimo, canal, moneda y vigencia. Validación local; un 201 muestra el
 * aviso de éxito y vuelve, y un 409 muestra "La política ya existe" (§12).
 *
 * Contenido protegido con [RoleGuard] (§11.2): sin rol `negocio` solo se ve el
 * aviso de acceso restringido.
 */
@Composable
fun RegistrarPoliticaScreen(
    viewModel: RegistrarPoliticaViewModel,
    onRegistered: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    // Evento de una sola vez: éxito → volver (§10).
    LaunchedEffect(state.registered) {
        if (state.registered) {
            delay(EXITO_MS)
            onRegistered()
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        BonificaTopBar(
            title = stringResource(R.string.negocio_politica_titulo),
            onBack = onBack
        )

        RoleGuard(role = Role.NEGOCIO, modifier = Modifier.weight(1f)) {
            RegistrarPoliticaContent(state = state, viewModel = viewModel, modifier = Modifier.fillMaxSize())
        }
    }
}

@Composable
private fun RegistrarPoliticaContent(
    state: RegistrarPoliticaUiState,
    viewModel: RegistrarPoliticaViewModel,
    modifier: Modifier = Modifier
) {
    val bloqueado = state.submitting || state.registered

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // --- Identificación -------------------------------------------------
        Seccion(stringResource(R.string.politica_seccion_identificacion))
        LabeledField(
            label = stringResource(R.string.politica_codigo),
            value = state.policyId,
            onValueChange = { viewModel.onChange(CampoPolitica.POLICY_ID, it) },
            placeholder = stringResource(R.string.politica_codigo_ejemplo),
            error = errorTexto(CampoPolitica.POLICY_ID, state.errors[CampoPolitica.POLICY_ID]),
            enabled = !bloqueado,
            keyboardType = KeyboardType.Text,
            imeAction = ImeAction.Next
        )
        Spacer(Modifier.height(12.dp))
        LabeledField(
            label = stringResource(R.string.politica_nombre),
            value = state.name,
            onValueChange = { viewModel.onChange(CampoPolitica.NOMBRE, it) },
            placeholder = stringResource(R.string.politica_nombre_ejemplo),
            error = errorTexto(CampoPolitica.NOMBRE, state.errors[CampoPolitica.NOMBRE]),
            enabled = !bloqueado,
            keyboardType = KeyboardType.Text,
            imeAction = ImeAction.Next
        )

        Spacer(Modifier.height(20.dp))

        // --- Descuento ------------------------------------------------------
        Seccion(stringResource(R.string.politica_seccion_descuento))
        ChipSelector(
            label = stringResource(R.string.politica_tipo),
            options = listOf(
                "PERCENTAGE" to stringResource(R.string.politica_tipo_porcentaje),
                "FIXED_AMOUNT" to stringResource(R.string.politica_tipo_fijo)
            ),
            selected = state.discountType,
            onSelect = viewModel::selectTipo,
            enabled = !bloqueado
        )
        Spacer(Modifier.height(12.dp))
        LabeledField(
            label = stringResource(R.string.politica_valor),
            value = state.discountValue,
            onValueChange = { viewModel.onChange(CampoPolitica.VALOR, it) },
            placeholder = stringResource(R.string.politica_valor_placeholder),
            error = errorTexto(CampoPolitica.VALOR, state.errors[CampoPolitica.VALOR]),
            helper = stringResource(R.string.politica_valor_ayuda),
            enabled = !bloqueado,
            keyboardType = KeyboardType.Decimal,
            imeAction = ImeAction.Next
        )

        Spacer(Modifier.height(12.dp))
        ChipSelector(
            label = stringResource(R.string.promo_alcance),
            options = listOf(
                "ALL" to stringResource(R.string.promo_alcance_todo),
                "CATEGORY" to stringResource(R.string.promo_alcance_categoria),
                "PRODUCT" to stringResource(R.string.promo_alcance_producto)
            ),
            selected = state.scope,
            onSelect = viewModel::selectAlcance,
            enabled = !bloqueado
        )

        // La referencia solo aplica a CATEGORY y PRODUCT (§12).
        if (state.scope != "ALL") {
            Spacer(Modifier.height(12.dp))
            LabeledField(
                label = stringResource(R.string.politica_referencia),
                value = state.scopeReference,
                onValueChange = { viewModel.onChange(CampoPolitica.REFERENCIA, it) },
                placeholder = stringResource(
                    if (state.scope == "CATEGORY") R.string.politica_referencia_categoria
                    else R.string.politica_referencia_producto
                ),
                error = errorTexto(CampoPolitica.REFERENCIA, state.errors[CampoPolitica.REFERENCIA]),
                enabled = !bloqueado,
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Next
            )
        }

        Spacer(Modifier.height(12.dp))
        LabeledField(
            label = stringResource(R.string.promo_minimo),
            value = state.minimumAmount,
            onValueChange = { viewModel.onChange(CampoPolitica.MINIMO, it) },
            placeholder = stringResource(R.string.politica_minimo_placeholder),
            error = errorTexto(CampoPolitica.MINIMO, state.errors[CampoPolitica.MINIMO]),
            helper = stringResource(R.string.politica_minimo_ayuda),
            enabled = !bloqueado,
            keyboardType = KeyboardType.Decimal,
            imeAction = ImeAction.Next
        )

        Spacer(Modifier.height(20.dp))

        // --- Aplicación -----------------------------------------------------
        Seccion(stringResource(R.string.politica_seccion_aplicacion))
        ChipSelector(
            label = stringResource(R.string.promo_filtro_canal),
            options = listOf("ALL" to stringResource(R.string.filtro_todos)) +
                listOf("BRANCH", "MOBILE_APP", "WEB", "ATM", "AGENT").map { it to channelName(it) },
            selected = state.channel,
            onSelect = viewModel::selectCanal,
            enabled = !bloqueado
        )
        Spacer(Modifier.height(12.dp))
        ChipSelector(
            label = stringResource(R.string.promo_filtro_moneda),
            options = listOf("PEN" to "PEN", "USD" to "USD"),
            selected = state.currency,
            onSelect = viewModel::selectMoneda,
            enabled = !bloqueado
        )

        Spacer(Modifier.height(20.dp))

        // --- Vigencia -------------------------------------------------------
        Seccion(stringResource(R.string.politica_seccion_vigencia))
        LabeledField(
            label = stringResource(R.string.politica_desde),
            value = state.validFrom,
            onValueChange = { viewModel.onChange(CampoPolitica.DESDE, it) },
            placeholder = stringResource(R.string.politica_fecha_placeholder),
            error = errorTexto(CampoPolitica.DESDE, state.errors[CampoPolitica.DESDE]),
            enabled = !bloqueado,
            keyboardType = KeyboardType.Text,
            imeAction = ImeAction.Next
        )
        Spacer(Modifier.height(12.dp))
        LabeledField(
            label = stringResource(R.string.politica_hasta),
            value = state.validTo,
            onValueChange = { viewModel.onChange(CampoPolitica.HASTA, it) },
            placeholder = stringResource(R.string.politica_fecha_placeholder),
            error = errorTexto(CampoPolitica.HASTA, state.errors[CampoPolitica.HASTA]),
            enabled = !bloqueado,
            keyboardType = KeyboardType.Text,
            imeAction = ImeAction.Done,
            onImeAction = viewModel::submit
        )

        state.generalError?.let { error ->
            Spacer(Modifier.height(16.dp))
            ErrorBanner(message = error.toMessage())
        }

        if (state.registered) {
            Spacer(Modifier.height(16.dp))
            AvisoExito()
        }

        Spacer(Modifier.height(20.dp))
        PrimaryButton(
            text = stringResource(R.string.politica_registrar),
            onClick = viewModel::submit,
            enabled = !state.registered,
            loading = state.submitting,
            loadingText = stringResource(R.string.politica_registrando)
        )
        Spacer(Modifier.height(16.dp))
    }
}

/** Título de bloque en mayúsculas y gris, igual que en el perfil (§12). */
@Composable
private fun Seccion(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        modifier = modifier.padding(top = 4.dp, bottom = 8.dp),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

/** Selector de opciones en píldoras (tipo, alcance, canal, moneda). */
@Composable
private fun ChipSelector(
    label: String,
    options: List<Pair<String, String>>,
    selected: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        )
        Spacer(Modifier.height(6.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            options.forEach { (value, text) ->
                ChipItem(
                    label = text,
                    selected = value == selected,
                    onClick = { onSelect(value) },
                    enabled = enabled
                )
            }
        }
    }
}

@Composable
private fun ChipItem(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Surface(
        modifier = modifier
            .clickable(enabled = enabled, onClick = onClick)
            .border(
                1.dp,
                if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                RoundedCornerShape(50)
            ),
        shape = RoundedCornerShape(50),
        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
            style = MaterialTheme.typography.labelMedium,
            color = when {
                !enabled -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                selected -> MaterialTheme.colorScheme.onPrimary
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            }
        )
    }
}

/** 201: aviso de éxito que la pantalla muestra antes de volver (§12). */
@Composable
private fun AvisoExito(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = Success.copy(alpha = 0.12f)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = null,
                tint = Success,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.politica_registrada),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

/** Texto del error de un campo (§12); el texto vive en strings.xml. */
@Composable
private fun errorTexto(campo: CampoPolitica, error: ErrorCampo?): String? {
    if (error == null) return null
    return when (campo) {
        CampoPolitica.POLICY_ID -> when (error) {
            ErrorCampo.REQUIRED -> stringResource(R.string.politica_codigo_requerido)
            else -> stringResource(R.string.politica_codigo_formato)
        }
        CampoPolitica.NOMBRE -> stringResource(R.string.politica_nombre_requerido)
        CampoPolitica.VALOR -> when (error) {
            ErrorCampo.REQUIRED -> stringResource(R.string.politica_valor_requerido)
            ErrorCampo.FORMAT -> stringResource(R.string.politica_valor_formato)
            else -> stringResource(R.string.politica_valor_rango)
        }
        CampoPolitica.REFERENCIA -> stringResource(R.string.politica_referencia_requerida)
        CampoPolitica.MINIMO -> when (error) {
            ErrorCampo.FORMAT -> stringResource(R.string.politica_minimo_formato)
            else -> stringResource(R.string.politica_minimo_rango)
        }
        CampoPolitica.DESDE -> when (error) {
            ErrorCampo.REQUIRED -> stringResource(R.string.politica_fecha_requerido)
            else -> stringResource(R.string.politica_fecha_formato)
        }
        CampoPolitica.HASTA -> when (error) {
            ErrorCampo.REQUIRED -> stringResource(R.string.politica_fecha_requerido)
            ErrorCampo.ORDER -> stringResource(R.string.politica_fecha_orden)
            else -> stringResource(R.string.politica_fecha_formato)
        }
    }
}
