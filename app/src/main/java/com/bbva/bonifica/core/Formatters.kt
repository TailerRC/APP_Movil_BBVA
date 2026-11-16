package com.bbva.bonifica.core

import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private val fechaCorta: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

/** 303.6, "PEN" -> "S/ 303.60" ; 40.8, "USD" -> "US$ 40.80" */
fun formatMoney(amount: Double, currency: String): String {
    val symbol = if (currency == "USD") "US$" else "S/"
    return "$symbol ${String.format(Locale.US, "%,.2f", amount)}"
}

/** Acepta "2026-03-01T10:00:00Z" o "2026-03-01". Si no se puede leer, devuelve el texto original. */
fun formatDate(iso: String): String =
    runCatching { OffsetDateTime.parse(iso).format(fechaCorta) }
        .getOrElse { runCatching { LocalDate.parse(iso.take(10)).format(fechaCorta) }.getOrDefault(iso) }

/** "47856522" -> "****6522" */
fun maskDni(document: String): String =
    if (document.length <= 4) document else "****" + document.takeLast(4)
