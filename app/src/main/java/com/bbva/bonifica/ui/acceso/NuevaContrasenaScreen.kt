package com.bbva.bonifica.ui.acceso

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bbva.bonifica.R
import com.bbva.bonifica.core.PasswordRules
import com.bbva.bonifica.core.toMessage
import com.bbva.bonifica.domain.model.Role
import com.bbva.bonifica.ui.components.BonificaTopBar
import com.bbva.bonifica.ui.components.ErrorBanner
import com.bbva.bonifica.ui.components.LabeledField
import com.bbva.bonifica.ui.components.PrimaryButton
import com.bbva.bonifica.ui.components.RequirementRow
import com.bbva.bonifica.ui.components.SecondaryButton
import com.bbva.bonifica.ui.theme.BbvaGray700
import com.bbva.bonifica.ui.theme.BbvaNavy

/**
 * Nueva contraseña obligatoria (§12): requisitos visibles antes de fallar
 * (≥8, mayúscula, minúscula, número) y confirmación. Llama a `setNewPassword`.
 */
@Composable
fun NuevaContrasenaScreen(
    viewModel: NuevaContrasenaViewModel,
    challenge: PendingChallenge?,
    onHome: (Role) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is NuevaContrasenaEvent.OpenHome -> onHome(event.role)
            }
        }
    }

    Column(modifier.fillMaxSize()) {
        BonificaTopBar(
            title = stringResource(R.string.campo_contrasena_nueva),
            onBack = onBack
        )

        if (challenge == null) {
            // Sin reto pendiente no se puede cambiar la contraseña (se llega desde el menú de desarrollo).
            Column(
                modifier = Modifier.weight(1f).fillMaxWidth().padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.sesion_vencida),
                    style = MaterialTheme.typography.bodyMedium,
                    color = BbvaGray700,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(24.dp))
                SecondaryButton(
                    text = stringResource(R.string.volver_a_iniciar_sesion),
                    onClick = onBack
                )
            }
        } else {
            Formulario(
                state = state,
                challenge = challenge,
                viewModel = viewModel,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun Formulario(
    state: NuevaContrasenaUiState,
    challenge: PendingChallenge,
    viewModel: NuevaContrasenaViewModel,
    modifier: Modifier = Modifier
) {
    val newFocus = remember { FocusRequester() }
    val confirmFocus = remember { FocusRequester() }

    // Al enviar con errores, el foco va al primer campo inválido.
    LaunchedEffect(state.attemptId) {
        if (state.attempted) {
            if (!PasswordRules.isValid(state.password)) newFocus.requestFocus()
            else if (state.mismatch) confirmFocus.requestFocus()
        }
    }

    val rules = listOf(
        stringResource(R.string.regla_min8) to PasswordRules.hasMinLength(state.password),
        stringResource(R.string.regla_mayuscula) to PasswordRules.hasUpperCase(state.password),
        stringResource(R.string.regla_minuscula) to PasswordRules.hasLowerCase(state.password),
        stringResource(R.string.regla_numero) to PasswordRules.hasDigit(state.password)
    )
    val rulesOk = rules.all { it.second }

    Column(
        modifier = modifier.fillMaxSize()
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
            .imePadding()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.nueva_contrasena_titulo),
            style = MaterialTheme.typography.headlineMedium,
            color = BbvaNavy,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.nueva_contrasena_subtitulo),
            style = MaterialTheme.typography.bodyMedium,
            color = BbvaGray700,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(24.dp))

        LabeledField(
            label = stringResource(R.string.campo_contrasena_nueva),
            value = state.password,
            onValueChange = viewModel::onPasswordChange,
            placeholder = stringResource(R.string.contrasena_ejemplo),
            enabled = !state.saving,
            keyboardType = KeyboardType.Password,
            imeAction = ImeAction.Next,
            onImeAction = { confirmFocus.requestFocus() },
            isPassword = true,
            passwordVisible = state.passwordVisible,
            onTogglePassword = viewModel::togglePasswordVisibility,
            focusRequester = newFocus
        )
        Spacer(Modifier.height(16.dp))
        LabeledField(
            label = stringResource(R.string.campo_confirmar_contrasena),
            value = state.confirm,
            onValueChange = viewModel::onConfirmChange,
            placeholder = stringResource(R.string.contrasena_ejemplo),
            error = if (state.mismatch) stringResource(R.string.contrasenas_no_coinciden) else null,
            enabled = !state.saving,
            keyboardType = KeyboardType.Password,
            imeAction = ImeAction.Done,
            onImeAction = { viewModel.save(challenge) },
            isPassword = true,
            passwordVisible = state.passwordVisible,
            onTogglePassword = viewModel::togglePasswordVisibility,
            focusRequester = confirmFocus
        )

        Spacer(Modifier.height(20.dp))
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            rules.forEach { (text, satisfied) -> RequirementRow(text = text, satisfied = satisfied) }
        }

        if (state.attempted && !rulesOk) {
            Spacer(Modifier.height(12.dp))
            ErrorBanner(message = stringResource(R.string.regla_pendiente))
        }

        state.generalError?.let { error ->
            Spacer(Modifier.height(16.dp))
            ErrorBanner(message = error.toMessage())
        }

        Spacer(Modifier.height(24.dp))
        PrimaryButton(
            text = stringResource(R.string.guardar_contrasena),
            onClick = { viewModel.save(challenge) },
            loading = state.saving,
            loadingText = stringResource(R.string.guardando_contrasena)
        )
        Spacer(Modifier.height(16.dp))
    }
}
