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
        ),
        PointOfSale(
            pointOfSaleReference = "SALE-SEED-0002", customerReference = CLIENTE_REF,
            channel = "MOBILE_APP", officeCode = "DIG-APP-01", officeName = "Banca Móvil BBVA",
            city = "Lima", status = "OPEN", createdAt = "2026-02-01T15:30:00Z"
        ),
        PointOfSale(
            pointOfSaleReference = "SALE-SEED-0003", customerReference = CLIENTE_REF,
            channel = "WEB", officeCode = "DIG-WEB-01", officeName = "Banca por Internet",
            city = "Lima", status = "CLOSED", createdAt = "2026-01-10T09:15:00Z"
        )
    )

    val products = listOf(
        // CUENTAS
        Product("PROD-0001", "Mantenimiento Cuenta Sueldo", "CUENTAS", 10.0, "PEN", "MONTHLY", "AVAILABLE"),
        Product("PROD-0002", "Mantenimiento Cuenta Corriente", "CUENTAS", 35.0, "PEN", "MONTHLY", "AVAILABLE"),
        Product("PROD-0008", "Producto descontinuado de ejemplo", "CUENTAS", 20.0, "PEN", "MONTHLY", "DISCONTINUED"),

        // TARJETAS
        Product("PROD-0025", "Comisión de mantenimiento de tarjeta", "TARJETAS", 172.50, "PEN", "MONTHLY", "AVAILABLE"),
        Product("PROD-0026", "Membresía Tarjeta Visa Signature", "TARJETAS", 350.0, "PEN", "ANNUAL", "AVAILABLE"),
        Product("PROD-0027", "Comisión por reposición de tarjeta", "TARJETAS", 25.0, "PEN", "PER_OPERATION", "AVAILABLE"),

        // PRESTAMOS
        Product("PROD-0030", "Comisión de activación préstamo personal", "PRESTAMOS", 85.0, "PEN", "ONE_TIME", "AVAILABLE"),
        Product("PROD-0031", "Gastos administrativos crédito vehicular", "PRESTAMOS", 150.0, "PEN", "ONE_TIME", "AVAILABLE"),

        // SEGUROS
        Product("PROD-0040", "Seguro Protección de Tarjetas", "SEGUROS", 15.90, "PEN", "MONTHLY", "AVAILABLE"),
        Product("PROD-0041", "Seguro Vida y Salud Plus", "SEGUROS", 45.0, "PEN", "MONTHLY", "AVAILABLE"),

        // TRANSFERENCIAS
        Product("PROD-0050", "Comisión por transferencia interbancaria inmediata", "TRANSFERENCIAS", 5.0, "PEN", "PER_OPERATION", "AVAILABLE"),
        Product("PROD-0051", "Comisión por transferencia al exterior SWIFT", "TRANSFERENCIAS", 25.0, "USD", "PER_OPERATION", "AVAILABLE")
    )

    val discounts = listOf(
        DiscountPricing(
            discountPricingId = "DP-0001",
            pointOfSaleReference = "SALE-SEED-0001",
            customerReference = CLIENTE_REF,
            productReference = "PROD-0025",
            category = "TARJETAS",
            channel = "BRANCH",
            currency = "PEN",
            listPrice = 172.50,
            quantity = 2,
            grossAmount = 345.00,
            policyId = "POL-022",
            policyName = "12% en tarjetas",
            discountAmount = 41.40,
            netAmount = 303.60,
            status = "APPLIED",
            createdAt = "2026-02-10T11:00:00Z",
            updatedAt = "2026-02-10T11:05:00Z",
            paymentOrderId = "PAY-0001"
        ),
        DiscountPricing(
            discountPricingId = "DP-0002",
            pointOfSaleReference = "SALE-SEED-0001",
            customerReference = CLIENTE_REF,
            productReference = "PROD-0025",
            category = "TARJETAS",
            channel = "BRANCH",
            currency = "PEN",
            listPrice = 172.50,
            quantity = 1,
            grossAmount = 172.50,
            policyId = "POL-022",
            policyName = "12% en tarjetas",
            discountAmount = 20.70,
            netAmount = 151.80,
            status = "INITIATED",
            createdAt = "2026-03-01T16:20:00Z",
            updatedAt = "2026-03-01T16:20:00Z"
        )
    )

    /**
     * Pagos sembrados (§14): DP-0001 ya está `APPLIED` y pagado, para que la ruta 12
     * tenga un comprobante desde el primer arranque. Los que genere el Módulo 3 al
     * confirmar se añaden a [MockStore.payments].
     */
    val payments = listOf(
        PaymentOrder(
            paymentOrderId = "PAY-0001", discountPricingId = "DP-0001",
            customerReference = CLIENTE_REF, pointOfSaleReference = "SALE-SEED-0001",
            amount = 303.60, currency = "PEN", paymentMethod = "CARGO_EN_CUENTA",
            status = "PAID", paymentDate = "2026-02-10T11:05:00Z"
        )
    )

    val policies = listOf(
        Policy(
            policyId = "POL-022", name = "12% en tarjetas", discountType = "PERCENTAGE",
            discountValue = 12.0, scope = "CATEGORY", scopeReference = "TARJETAS",
            minimumAmount = 0.0, currency = "PEN", channel = "ALL",
            validFrom = "2026-01-01", validTo = "2026-12-31", status = "ACTIVE"
        ),
        // Módulo 3: compite con POL-022 en sucursal para comprobar que el cálculo
        // elige la MEJOR política (12% gana frente a 5%, §14).
        Policy(
            policyId = "POL-025", name = "5% general en ventanilla", discountType = "PERCENTAGE",
            discountValue = 5.0, scope = "ALL", scopeReference = "*",
            minimumAmount = 0.0, currency = "PEN", channel = "BRANCH",
            validFrom = "2026-01-01", validTo = "2026-12-31", status = "ACTIVE"
        )
    )

    // Cálculo esperado: PROD-0025 x2 -> bruto 345.00 · POL-022 · descuento 41.40 · neto 303.60
}
