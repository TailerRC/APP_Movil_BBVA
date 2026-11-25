package com.bbva.bonifica.ui.acceso

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
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
import com.bbva.bonifica.ui.theme.BbvaAqua
import com.bbva.bonifica.ui.theme.BbvaBlue
import com.bbva.bonifica.ui.theme.BbvaNavy
import com.bbva.bonifica.ui.theme.BbvaWhite

/**
 * Login (§12): diseño bicolor con cabecera degradada BBVA y hoja inferior
 * redondeada que ocupa todo el espacio restante (sin huecos en blanco).
 * En modo demo (`AppConfig.USE_MOCK`) ofrece accesos rápidos de un toque
 * para autocompletar credenciales de Cliente o Negocio.
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

    LaunchedEffect(state.emailError, state.passwordError) {
        when {
            state.emailError != null -> emailFocus.requestFocus()
            state.passwordError != null -> passwordFocus.requestFocus()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(BbvaNavy, BbvaBlue)
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .imePadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Cabecera compacta con logo BBVA visible
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 28.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BonificaLogo(size = 64.dp)
                Spacer(Modifier.width(16.dp))
                Column {
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = BbvaWhite
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = stringResource(R.string.splash_tagline),
                        style = MaterialTheme.typography.bodyMedium,
                        color = BbvaWhite.copy(alpha = 0.85f)
                    )
                    Spacer(Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .width(40.dp)
                            .height(3.dp)
                            .background(BbvaAqua, RoundedCornerShape(2.dp))
                    )
                }
            }

            // Hoja inferior que ocupa todo el alto restante
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .navigationBarsPadding()
                        .padding(horizontal = 24.dp, vertical = 28.dp)
                ) {
                    Text(
                        text = stringResource(R.string.login_titulo),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.login_subtitulo),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (AppConfig.USE_MOCK) {
                        Spacer(Modifier.height(16.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = state.email == "cliente1@example.com",
                                onClick = {
                                    acceso.onEmailChange("cliente1@example.com")
                                    acceso.onPasswordChange("Demo1234!")
                                },
                                label = { Text("Cliente Demo") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                )
                            )
                            FilterChip(
                                selected = state.email == "negocio1@example.com",
                                onClick = {
                                    acceso.onEmailChange("negocio1@example.com")
                                    acceso.onPasswordChange("Demo1234!")
                                },
                                label = { Text("Negocio Demo") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                )
                            )
                        }
                    }

                    Spacer(Modifier.height(20.dp))

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
                        Spacer(Modifier.height(14.dp))
                        Text(
                            text = stringResource(R.string.login_modo_demo),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
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
