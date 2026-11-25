package com.bbva.bonifica.ui.theme

import androidx.compose.ui.graphics.Color

// ─── Paleta BBVA — Modo Claro ────────────────────────────────────────────────
// Verificar hex contra la guía de marca; se cambian SOLO aquí.
val BbvaNavy       = Color(0xFF072146)
val BbvaBlue       = Color(0xFF004481)
val BbvaBlueMedium = Color(0xFF1464A5)
val BbvaBlueLight  = Color(0xFF1973B8)
val BbvaAqua       = Color(0xFF2DCCCD)
val BbvaSky        = Color(0xFF5BBEFF)
val BbvaWhite      = Color(0xFFFFFFFF)
val BbvaGray50     = Color(0xFFF4F4F4)
val BbvaGray300    = Color(0xFFC9CED6)
val BbvaGray700    = Color(0xFF5C6670)
val Success        = Color(0xFF48AE64)
val Warning        = Color(0xFFF8CD51)
val Error          = Color(0xFFDA3851)

/**
 * Variante accesible de Error para texto pequeño sobre fondo claro.
 * `Error` sobre `BbvaGray50` no llega a 4.5:1 (AA).
 */
val ErrorDark = Color(0xFFB31F38)

// ─── Paleta BBVA — Modo Oscuro (deep navy) ───────────────────────────────────
val DarkBackground      = Color(0xFF0D1B40)   // fondo principal
val DarkSurface         = Color(0xFF152650)   // tarjetas / superficies
val DarkSurfaceVariant  = Color(0xFF1E3260)   // elementos secundarios / inputs
val DarkPrimary         = Color(0xFF4D9FEC)   // azul legible sobre oscuro
val DarkOnBackground    = Color(0xFFE8EEFF)   // texto principal
val DarkOnSurface       = Color(0xFFE8EEFF)   // texto en tarjetas
val DarkOnSurfaceVariant= Color(0xFF8BA4CF)   // texto secundario
val DarkOutline         = Color(0xFF2A4070)   // bordes
val SuccessDark         = Color(0xFF5CC77A)   // verde accesible en oscuro
val ErrorOnDark         = Color(0xFFFF6B84)   // rojo legible en oscuro
