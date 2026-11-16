package com.bbva.bonifica.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

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
        shapes = BonificaShapes,
        content = content
    )
}
