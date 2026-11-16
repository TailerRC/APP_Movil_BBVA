package com.bbva.bonifica.domain.model

// Modelos de dominio (diseño §6). Importes como Double.

enum class Role { CLIENTE, NEGOCIO }

data class Session(
    val email: String, val role: Role, val customerReference: String?, // null si negocio
    val idToken: String, val expiresAtEpochSec: Long
)

data class Customer(
    val customerReference: String, val name: String, val documentType: String,
    val documentNumber: String, val email: String, val phone: String,
    val segment: String,        // PERSONAL | PREFERENTE | PREMIUM | JOVEN | PYME
    val city: String, val status: String, // ACTIVE | INACTIVE
    val customerSince: String
)

data class Product(
    val productReference: String, val name: String,
    val category: String,       // CUENTAS | TARJETAS | PRESTAMOS | SEGUROS | TRANSFERENCIAS
    val listPrice: Double, val currency: String,       // PEN | USD
    val billingFrequency: String, // MONTHLY | ANNUAL | PER_OPERATION | ONE_TIME
    val status: String          // AVAILABLE | DISCONTINUED
)

data class PointOfSale(
    val pointOfSaleReference: String, val customerReference: String,
    val channel: String,        // MOBILE_APP | WEB | BRANCH | ATM | AGENT
    val officeCode: String, val officeName: String, val city: String,
    val status: String,         // OPEN | CLOSED
    val createdAt: String
)

data class DiscountPricing(           // Control Record: Discount Pricing Transaction
    val discountPricingId: String, val pointOfSaleReference: String,
    val customerReference: String, val productReference: String,
    val category: String, val channel: String, val currency: String,
    val listPrice: Double, val quantity: Int, val grossAmount: Double,
    val policyId: String,       // "NONE" si no hubo descuento
    val policyName: String, val discountAmount: Double, val netAmount: Double,
    val status: String,         // INITIATED | APPLIED | CANCELLED
    val createdAt: String, val updatedAt: String,
    val paymentOrderId: String? = null // solo viene en la respuesta de Update→APPLIED
)

data class Policy(
    val policyId: String, val name: String,
    val discountType: String,   // PERCENTAGE | FIXED_AMOUNT
    val discountValue: Double,
    val scope: String,          // PRODUCT | CATEGORY | ALL
    val scopeReference: String, val minimumAmount: Double, val currency: String,
    val channel: String,        // ALL | MOBILE_APP | WEB | BRANCH | ATM | AGENT
    val validFrom: String, val validTo: String, val status: String
)

data class Campaign(
    val campaignId: String, val policyId: String, val name: String,
    val targetSegment: String, val channel: String, val budget: Double,
    val currency: String, val startDate: String, val endDate: String,
    val status: String          // ACTIVE | SCHEDULED | FINISHED | PAUSED
)

data class PaymentOrder(
    val paymentOrderId: String, val discountPricingId: String,
    val customerReference: String, val pointOfSaleReference: String,
    val amount: Double, val currency: String,
    val paymentMethod: String,  // CARGO_EN_CUENTA | TARJETA_DEBITO | EFECTIVO_VENTANILLA
    val status: String,         // PAID | PENDING | CANCELLED
    val paymentDate: String
)

data class EventLogEntry(
    val discountPricingId: String, val eventTimestamp: String,
    val eventType: String, val previousStatus: String?, val newStatus: String?,
    val actor: String, val channel: String?, val policyId: String, val details: String
)

data class NewPolicy(                  // formulario del módulo 7 (ruta 5)
    val policyId: String, val name: String, val discountType: String,
    val discountValue: Double, val scope: String, val scopeReference: String = "*",
    val minimumAmount: Double = 0.0, val currency: String = "PEN",
    val channel: String = "ALL", val validFrom: String = "2026-01-01",
    val validTo: String = "2026-12-31"
)
