package com.bbva.bonifica.ui.acceso

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Person
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
import com.bbva.bonifica.AppConfig
import com.bbva.bonifica.R
import com.bbva.bonifica.core.toMessage
import com.bbva.bonifica.domain.model.Role
import com.bbva.bonifica.ui.components.BonificaLogo
import com.bbva.bonifica.ui.components.ErrorBanner
import com.bbva.bonifica.ui.components.LabeledField
import com.bbva.bonifica.ui.components.PrimaryButton
import com.bbva.bonifica.ui.theme.BbvaGray700
import com.bbva.bonifica.ui.theme.BbvaNavy

/**
 * Login (§12): correo + contraseña con ojo, botón **Ingresar**, mensajes de §5.
 * Sin enlace de registro ni recuperación: no están en el alcance (§2).
 */
@Composable
fun LoginScreen(
    acceso: AccesoViewModel,
    onHome: (Role) -> Unit,
    onNewPassword: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by acceso.state.collectAsStateWithLifecycle()

    LaunchedEffect(acceso) {
        acceso.events.collect { event ->
            when (event) {
                is AccesoEvent.OpenHome -> onHome(event.role)
                AccesoEvent.OpenNewPassword -> onNewPassword()
            }
        }
    }

    val emailFocus = remember { FocusRequester() }
    val passwordFocus = remember { FocusRequester() }

    // Al enviar con errores, el foco va al primer campo inválido.
    LaunchedEffect(state.emailError, state.passwordError) {
        when {
            state.emailError != null -> emailFocus.requestFocus()
            state.passwordError != null -> passwordFocus.requestFocus()
        }
    }

    Column(
        modifier = modifier.fillMaxSize()
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .padding(horizontal = 24.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        BonificaLogo(size = 84.dp, modifier = Modifier.padding(top = 24.dp))
        Spacer(Modifier.height(24.dp))
        Text(
            text = stringResource(R.string.login_titulo),
            style = MaterialTheme.typography.headlineMedium,
            color = BbvaNavy
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.login_subtitulo),
            style = MaterialTheme.typography.bodyMedium,
            color = BbvaGray700,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(32.dp))

        LabeledField(
            label = stringResource(R.string.campo_correo),
            value = state.email,
            onValueChange = acceso::onEmailChange,
            placeholder = stringResource(R.string.correo_ejemplo),
            error = emailErrorText(state.emailError),
            enabled = !state.submitting,
            keyboardType = KeyboardType.Email,
            imeAction = ImeAction.Next,
            onImeAction = { passwordFocus.requestFocus() },
            leadingIcon = Icons.Outlined.Person,
            focusRequester = emailFocus
        )
        Spacer(Modifier.height(16.dp))
        LabeledField(
            label = stringResource(R.string.campo_contrasena),
            value = state.password,
            onValueChange = acceso::onPasswordChange,
            placeholder = stringResource(R.string.contrasena_ejemplo),
            error = passwordErrorText(state.passwordError),
            enabled = !state.submitting,
            keyboardType = KeyboardType.Password,
            imeAction = ImeAction.Done,
            onImeAction = acceso::submit,
            isPassword = true,
            passwordVisible = state.passwordVisible,
            onTogglePassword = acceso::togglePasswordVisibility,
            leadingIcon = Icons.Outlined.Lock,
            focusRequester = passwordFocus
        )

        state.generalError?.let { error ->
            Spacer(Modifier.height(16.dp))
            ErrorBanner(message = error.toMessage())
        }

        Spacer(Modifier.height(24.dp))
        PrimaryButton(
            text = stringResource(R.string.ingresar),
            onClick = acceso::submit,
            loading = state.submitting,
            loadingText = stringResource(R.string.iniciando_sesion)
        )

        if (AppConfig.USE_MOCK) {
            Spacer(Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.login_modo_demo),
                style = MaterialTheme.typography.bodySmall,
                color = BbvaGray700,
                textAlign = TextAlign.Center
            )
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun emailErrorText(error: FieldError?): String? = when (error) {
    FieldError.REQUIRED -> stringResource(R.string.correo_requerido)
    FieldError.INVALID_EMAIL -> stringResource(R.string.correo_formato)
    null -> null
}

@Composable
private fun passwordErrorText(error: FieldError?): String? = when (error) {
    FieldError.REQUIRED -> stringResource(R.string.contrasena_requerida)
    FieldError.INVALID_EMAIL -> null
    null -> null
}
