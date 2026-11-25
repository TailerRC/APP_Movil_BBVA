package com.bbva.bonifica.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

private val LightColors = lightColorScheme(
    primary            = BbvaBlue,
    onPrimary          = BbvaWhite,
    primaryContainer   = BbvaBlueLight,
    onPrimaryContainer = BbvaWhite,
    secondary          = BbvaAqua,
    onSecondary        = BbvaNavy,
    background         = BbvaGray50,
    onBackground       = BbvaNavy,
    surface            = BbvaWhite,
    onSurface          = BbvaNavy,
    surfaceVariant     = BbvaGray50,
    onSurfaceVariant   = BbvaGray700,
    error              = Error,
    onError            = BbvaWhite,
    outline            = BbvaGray300
)

private val DarkColors = darkColorScheme(
    primary            = DarkPrimary,
    onPrimary          = BbvaWhite,
    primaryContainer   = DarkSurfaceVariant,
    onPrimaryContainer = DarkOnBackground,
    secondary          = BbvaAqua,
    onSecondary        = DarkBackground,
    background         = DarkBackground,
    onBackground       = DarkOnBackground,
    surface            = DarkSurface,
    onSurface          = DarkOnSurface,
    surfaceVariant     = DarkSurfaceVariant,
    onSurfaceVariant   = DarkOnSurfaceVariant,
    error              = ErrorOnDark,
    onError            = DarkBackground,
    outline            = DarkOutline
)

@Composable
fun BonificaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = Typography,
        shapes = BonificaShapes
    ) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            content()
        }
    }
}
