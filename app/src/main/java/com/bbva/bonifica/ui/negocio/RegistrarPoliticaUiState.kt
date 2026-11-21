package com.bbva.bonifica.ui.negocio

import com.bbva.bonifica.core.AppError
import java.time.LocalDate

/** Campos con validación propia del formulario (§12). */
enum class CampoPolitica { POLICY_ID, NOMBRE, VALOR, REFERENCIA, MINIMO, DESDE, HASTA }

/** Tipo de error de un campo; el texto vive en strings.xml. */
enum class ErrorCampo { REQUIRED, FORMAT, RANGE, ORDER }

/**
 * Estado del formulario de `Policy/Register` (Módulo 7, ruta 5).
 * Los valores numéricos y de fecha se guardan como texto mientras se escribe;
 * se convierten al enviar.
 */
data class RegistrarPoliticaUiState(
    val policyId: String = "",
    val name: String = "",
    val discountType: String = "PERCENTAGE",   // PERCENTAGE | FIXED_AMOUNT
    val discountValue: String = "",
    val scope: String = "ALL",                 // ALL | CATEGORY | PRODUCT
    val scopeReference: String = "",
    val minimumAmount: String = "",
    val currency: String = "PEN",              // PEN | USD
    val channel: String = "ALL",               // ALL | MOBILE_APP | WEB | BRANCH | ATM | AGENT
    val validFrom: String = LocalDate.now().toString(),
    val validTo: String = LocalDate.now().plusYears(1).toString(),
    /** Campo → tipo de error; vacío cuando todo es válido. */
    val errors: Map<CampoPolitica, ErrorCampo> = emptyMap(),
    val generalError: AppError? = null,
    val submitting: Boolean = false,
    /** true tras el 201: la pantalla muestra el aviso y vuelve (§12). */
    val registered: Boolean = false
)
