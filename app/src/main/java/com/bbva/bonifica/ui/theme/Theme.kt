package com.bbva.bonifica.ui.theme

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

private val LightColors = lightColorScheme(
    primary = BbvaBlue,
    onPrimary = BbvaWhite,
    secondary = BbvaAqua,
    onSecondary = BbvaNavy,
    background = BbvaGray50,
    onBackground = BbvaNavy,
    surface = BbvaWhite,
    onSurface = BbvaNavy,
    error = Error,
    outline = BbvaGray300
)

@Composable
fun BonificaTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColors,
        typography = Typography,
        shapes = BonificaShapes
    ) {
        // Sin esta Surface el fondo de pantalla sería el de la ventana (blanco), no BbvaGray50.
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            content()
        }
    }
}
