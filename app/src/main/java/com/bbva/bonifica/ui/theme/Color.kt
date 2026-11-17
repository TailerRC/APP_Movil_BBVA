package com.bbva.bonifica.ui.theme

import androidx.compose.ui.graphics.Color

// Paleta BBVA (diseño §3). Verificar hex contra la guía de marca; se cambian SOLO aquí.
val BbvaNavy = Color(0xFF072146)
val BbvaBlue = Color(0xFF004481)
val BbvaBlueMedium = Color(0xFF1464A5)
val BbvaBlueLight = Color(0xFF1973B8)
val BbvaAqua = Color(0xFF2DCCCD)
val BbvaSky = Color(0xFF5BBEFF)
val BbvaWhite = Color(0xFFFFFFFF)
val BbvaGray50 = Color(0xFFF4F4F4)
val BbvaGray300 = Color(0xFFC9CED6)
val BbvaGray700 = Color(0xFF5C6670)
val Success = Color(0xFF48AE64)
val Warning = Color(0xFFF8CD51)
val Error = Color(0xFFDA3851)

/**
 * Variante accesible de Error para texto pequeño: `Error` sobre `BbvaGray50`
 * no llega a 4.5:1 (AA). Se usa en mensajes de campo; íconos, bordes y chips
 * siguen usando `Error`.
 */
val ErrorDark = Color(0xFFB31F38)
