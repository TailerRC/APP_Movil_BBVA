package com.bbva.bonifica.data.mock

import com.bbva.bonifica.domain.model.*

/**
 * Datos de prueba (diseño §14). Deben coincidir con dynamobd.md / Postman.
 * TODO (Persona A): completar con más productos, políticas y campañas reales de dynamobd.md.
 */
object MockData {
    const val CLIENTE_EMAIL = "cliente1@example.com"
    const val NEGOCIO_EMAIL = "negocio1@example.com"
    const val CLIENTE_REF = "PARTY-000024"
    const val CLIENTE_AJENO_REF = "PARTY-000001" // con token de cliente debe dar 403

    val customer = Customer(
        customerReference = CLIENTE_REF, name = "Cliente Demo", documentType = "DNI",
        documentNumber = "47856522", email = CLIENTE_EMAIL, phone = "+51 999 888 777",
        segment = "PERSONAL", city = "Chiclayo", status = "ACTIVE", customerSince = "2020-05-10"
    )

    val operations = listOf(
        PointOfSale(
            pointOfSaleReference = "SALE-SEED-0001", customerReference = CLIENTE_REF,
            channel = "BRANCH", officeCode = "OF-CHI-01", officeName = "Oficina Chiclayo Centro",
            city = "Chiclayo", status = "OPEN", createdAt = "2026-01-15T10:00:00Z"
        )
    )

    val products = listOf(
        Product("PROD-0025", "Comisión de mantenimiento de tarjeta", "TARJETAS", 172.50, "PEN", "MONTHLY", "AVAILABLE"),
        Product("PROD-0008", "Producto descontinuado de ejemplo", "CUENTAS", 20.0, "PEN", "MONTHLY", "DISCONTINUED")
    )

    val policies = listOf(
        Policy(
            policyId = "POL-022", name = "12% en tarjetas", discountType = "PERCENTAGE",
            discountValue = 12.0, scope = "CATEGORY", scopeReference = "TARJETAS",
            minimumAmount = 0.0, currency = "PEN", channel = "ALL",
            validFrom = "2026-01-01", validTo = "2026-12-31", status = "ACTIVE"
        )
    )

    // Cálculo esperado: PROD-0025 x2 -> bruto 345.00 · POL-022 · descuento 41.40 · neto 303.60
}
