package com.bbva.bonifica.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.bbva.bonifica.R
import com.bbva.bonifica.core.formatMoney
import com.bbva.bonifica.ui.theme.BbvaBlue
import com.bbva.bonifica.ui.theme.BbvaBlueLight
import com.bbva.bonifica.ui.theme.BbvaGray300
import com.bbva.bonifica.ui.theme.BbvaGray700
import com.bbva.bonifica.ui.theme.BbvaNavy
import com.bbva.bonifica.ui.theme.BbvaWhite
import com.bbva.bonifica.ui.theme.Error
import com.bbva.bonifica.ui.theme.ErrorDark
import com.bbva.bonifica.ui.theme.Success
import com.bbva.bonifica.ui.theme.Warning

// Componentes reutilizables (diseño §9). Persona A agrega el resto
// (InfoCard, ProductCard, RoleGuard...); ConfirmDialog lo aporta el Módulo 3
// y TimelineItem + paymentMethodName el Módulo 6.

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    /** Texto mientras carga (ej. "Iniciando sesión…"); si es null se muestra `text`. */
    loadingText: String? = null
) {
    Button(
        onClick = onClick,
        enabled = enabled && !loading,
        modifier = modifier.fillMaxWidth().height(52.dp),
        shape = RoundedCornerShape(26.dp),
        colors = ButtonDefaults.buttonColors(containerColor = BbvaBlue, contentColor = BbvaWhite)
    ) {
        if (loading) {
            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = BbvaWhite, strokeWidth = 2.dp)
            Spacer(Modifier.width(10.dp))
        }
        Text(if (loading) loadingText ?: text else text)
    }
}

@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    /** Texto mientras carga (ej. "Anulando…"); si es null se muestra `text`. */
    loadingText: String? = null
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled && !loading,
        modifier = modifier.fillMaxWidth().height(52.dp),
        shape = RoundedCornerShape(26.dp),
        border = BorderStroke(1.dp, BbvaBlue)
    ) {
        if (loading) {
            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = BbvaBlue, strokeWidth = 2.dp)
            Spacer(Modifier.width(10.dp))
        }
        Text(if (loading) loadingText ?: text else text, color = BbvaBlue)
    }
}

@Composable
fun MoneyText(
    amount: Double,
    currency: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified
) {
    Text(
        text = formatMoney(amount, currency),
        modifier = modifier,
        color = color,
        style = MaterialTheme.typography.titleMedium
    )
}

/** Color del chip según §8.3. */
fun statusColor(status: String): Color = when (status) {
    "APPLIED", "PAID", "ACTIVE" -> Success
    "INITIATED", "PENDING", "PAUSED" -> Warning
    "CANCELLED" -> Error
    "OPEN", "SCHEDULED" -> BbvaBlueLight
    else -> BbvaGray700 // CLOSED, FINISHED, otros
}

@Composable
fun StatusChip(status: String, modifier: Modifier = Modifier) {
    val color = statusColor(status)
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = color.copy(alpha = 0.15f)
    ) {
        Text(
            text = status,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            color = color,
            style = MaterialTheme.typography.labelMedium
        )
    }
}

/** Nombre en español del medio de pago (§6, `PaymentOrder.paymentMethod`). */
@Composable
fun paymentMethodName(method: String): String = when (method) {
    "CARGO_EN_CUENTA" -> stringResource(R.string.pago_cargo_cuenta)
    "TARJETA_DEBITO" -> stringResource(R.string.pago_tarjeta_debito)
    "EFECTIVO_VENTANILLA" -> stringResource(R.string.pago_efectivo_ventanilla)
    else -> method
}

@Composable
fun LoadingView(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = BbvaBlue)
    }
}

@Composable
fun EmptyView(message: String, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Text(message, textAlign = TextAlign.Center, color = BbvaGray700)
    }
}

@Composable
fun ErrorView(message: String, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(message, textAlign = TextAlign.Center)
        SecondaryButton(
            text = stringResource(R.string.reintentar),
            onClick = onRetry,
            modifier = Modifier.padding(top = 16.dp)
        )
    }
}

/** Barra azul de marca con título, botón atrás opcional y acciones (§9). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BonificaTopBar(
    title: String,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    TopAppBar(
        title = { Text(title, color = BbvaWhite, style = MaterialTheme.typography.titleLarge) },
        modifier = modifier,
        navigationIcon = {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.volver),
                        tint = BbvaWhite
                    )
                }
            }
        },
        actions = actions,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = BbvaBlue,
            titleContentColor = BbvaWhite,
            navigationIconContentColor = BbvaWhite,
            actionIconContentColor = BbvaWhite
        )
    )
}

/**
 * Marca de la app: el logo oficial de BBVA Perú (`R.drawable.logo_bbva`).
 * Splash y Login la usan para que la marca viva en un solo sitio.
 */
@Composable
fun BonificaLogo(modifier: Modifier = Modifier, size: Dp = 84.dp) {
    Image(
        painter = painterResource(R.drawable.logo_bbva),
        contentDescription = stringResource(R.string.logo_bbva),
        modifier = modifier.size(size)
            .clip(RoundedCornerShape(size * 0.18f)),
        contentScale = ContentScale.Crop
    )
}

/**
 * Campo de texto con etiqueta visible arriba, error junto al campo y teclado
 * adecuado (§9). El placeholder es solo un ejemplo: la etiqueta nunca se oculta.
 */
@Composable
fun LabeledField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    error: String? = null,
    helper: String? = null,
    enabled: Boolean = true,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Next,
    /** Se llama al pulsar "Siguiente"/"Listo" del teclado (para saltar o enviar). */
    onImeAction: () -> Unit = {},
    isPassword: Boolean = false,
    passwordVisible: Boolean = false,
    onTogglePassword: (() -> Unit)? = null,
    leadingIcon: ImageVector? = null,
    focusRequester: FocusRequester? = null
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth()
                .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier),
            enabled = enabled,
            isError = error != null,
            placeholder = { Text(placeholder, color = BbvaGray700) },
            leadingIcon = if (leadingIcon != null) {
                {
                    Icon(leadingIcon, contentDescription = null, tint = BbvaGray700)
                }
            } else null,
            trailingIcon = if (isPassword && onTogglePassword != null) {
                {
                    IconButton(onClick = onTogglePassword, modifier = Modifier.size(44.dp)) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                            contentDescription = stringResource(
                                if (passwordVisible) R.string.ocultar_contrasena else R.string.mostrar_contrasena
                            ),
                            tint = BbvaGray700
                        )
                    }
                }
            } else null,
            visualTransformation = if (isPassword && !passwordVisible) {
                PasswordVisualTransformation()
            } else {
                VisualTransformation.None
            },
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = imeAction),
            keyboardActions = KeyboardActions(
                onNext = { onImeAction() },
                onDone = { onImeAction() },
                onGo = { onImeAction() }
            ),
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = BbvaBlue,
                unfocusedBorderColor = BbvaGray300,
                errorBorderColor = Error,
                focusedTextColor = BbvaNavy,
                unfocusedTextColor = BbvaNavy,
                errorTextColor = BbvaNavy,
                focusedContainerColor = BbvaWhite,
                unfocusedContainerColor = BbvaWhite,
                errorContainerColor = BbvaWhite,
                disabledTextColor = BbvaGray700,
                disabledBorderColor = BbvaGray300,
                disabledContainerColor = BbvaWhite
            )
        )
        when {
            error != null -> Row(
                modifier = Modifier.padding(start = 12.dp, top = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.ErrorOutline,
                    contentDescription = null,
                    tint = ErrorDark,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(error, style = MaterialTheme.typography.bodySmall, color = ErrorDark)
            }
            helper != null -> Text(
                text = helper,
                modifier = Modifier.padding(start = 12.dp, top = 6.dp),
                style = MaterialTheme.typography.bodySmall,
                color = BbvaGray700
            )
        }
    }
}

/** Aviso de error general (§10): mensaje + ícono, junto a la acción que falló. */
@Composable
fun ErrorBanner(message: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = Error.copy(alpha = 0.08f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.ErrorOutline,
                contentDescription = null,
                tint = Error,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = message,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium,
                color = ErrorDark
            )
        }
    }
}

/** Fila de lista de requisitos: check verde cuando se cumple, círculo vacío si no. */
@Composable
fun RequirementRow(text: String, satisfied: Boolean, modifier: Modifier = Modifier) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = if (satisfied) Icons.Filled.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
            contentDescription = null,
            tint = if (satisfied) Success else BbvaGray300,
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = if (satisfied) BbvaNavy else BbvaGray700
        )
    }
}

/**
 * Diálogo de confirmar/anular (§9): se usa antes de **Confirmar** y de **Anular**
 * un descuento, porque ambos cambian el estado de forma definitiva (§12).
 */
@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    confirmText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    dismissText: String = stringResource(R.string.cancelar)
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier,
        containerColor = BbvaWhite,
        title = { Text(title, color = BbvaNavy, style = MaterialTheme.typography.titleLarge) },
        text = { Text(message, color = BbvaNavy, style = MaterialTheme.typography.bodyMedium) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(confirmText, color = BbvaBlue, style = MaterialTheme.typography.labelLarge)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(dismissText, color = BbvaGray700, style = MaterialTheme.typography.labelLarge)
            }
        }
    )
}

/**
 * Punto + línea de la línea de tiempo del historial (§9). El punto se colorea con
 * `statusColor(status)`; la línea llega hasta el inicio del siguiente elemento y
 * `isLast` la omite, para que la columna de eventos quede continua.
 *
 * La fila usa `IntrinsicSize.Min` para que el punto y la línea midan lo mismo que
 * el contenido de la derecha (el alto del texto varía por el detalle de cada evento).
 */
@Composable
fun TimelineItem(
    title: String,
    timestamp: String,
    modifier: Modifier = Modifier,
    status: String? = null,
    channel: String? = null,
    detail: String? = null,
    isLast: Boolean = false
) {
    Row(modifier = modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
        // Punto arriba y línea debajo, centrados en una columna del ancho fijo.
        Box(
            modifier = Modifier.width(28.dp).fillMaxHeight(),
            contentAlignment = Alignment.TopCenter
        ) {
            if (!isLast) {
                Box(
                    modifier = Modifier
                        .padding(top = 16.dp) // deja libre el punto (14 dp)
                        .width(2.dp)
                        .fillMaxHeight()
                        .background(BbvaGray300)
                )
            }
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .background(statusColor(status ?: ""), CircleShape)
            )
        }

        Column(modifier = Modifier.weight(1f).padding(bottom = 16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = BbvaNavy
                )
                if (status != null) {
                    StatusChip(status = status)
                }
            }

            Spacer(Modifier.height(4.dp))

            Text(
                text = timestamp,
                style = MaterialTheme.typography.bodySmall,
                color = BbvaGray700
            )

            channel?.let {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = BbvaGray700
                )
            }

            detail?.let {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = BbvaNavy
                )
            }
        }
    }
}
