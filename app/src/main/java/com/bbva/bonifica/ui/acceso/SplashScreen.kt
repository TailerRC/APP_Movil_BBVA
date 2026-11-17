package com.bbva.bonifica.ui.acceso

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.bbva.bonifica.R
import com.bbva.bonifica.domain.model.Role
import com.bbva.bonifica.ui.components.BonificaLogo
import com.bbva.bonifica.ui.theme.BbvaBlue
import com.bbva.bonifica.ui.theme.BbvaGray700
import com.bbva.bonifica.ui.theme.BbvaNavy

/**
 * Splash (§12): muestra la marca y, con la sesión restaurada, entra al home del
 * rol o al login. Mantener pulsado el logo abre el menú de desarrollo (§14):
 * es la única puerta a las pantallas que todavía no tienen entrada.
 */
@Composable
fun SplashScreen(
    state: SplashUiState,
    onHome: (Role) -> Unit,
    onLogin: () -> Unit,
    onOpenDevMenu: () -> Unit,
    modifier: Modifier = Modifier
) {
    LaunchedEffect(state) {
        when (state) {
            is SplashUiState.Logged -> onHome(state.role)
            SplashUiState.Anonymous -> onLogin()
            SplashUiState.Checking -> Unit
        }
    }

    Column(
        modifier = modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        BonificaLogo(
            size = 96.dp,
            modifier = Modifier.pointerInput(Unit) {
                detectTapGestures(onLongPress = { onOpenDevMenu() })
            }
        )
        Spacer(Modifier.height(20.dp))
        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.titleLarge,
            color = BbvaNavy
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.splash_tagline),
            style = MaterialTheme.typography.bodyMedium,
            color = BbvaGray700
        )
        Spacer(Modifier.height(40.dp))
        CircularProgressIndicator(
            color = BbvaBlue,
            modifier = Modifier.size(28.dp),
            strokeWidth = 3.dp
        )
    }
}
