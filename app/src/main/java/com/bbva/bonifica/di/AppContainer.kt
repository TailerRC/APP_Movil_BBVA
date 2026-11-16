package com.bbva.bonifica.di

import com.bbva.bonifica.AppConfig
import com.bbva.bonifica.data.mock.*
import com.bbva.bonifica.domain.repository.*

/**
 * Inyección manual. Elige Mock o Remote según AppConfig.USE_MOCK.
 * TODO (Persona A, al integrar AWS): crear las clases Remote* y cambiar el `remotePendiente()`.
 */
class AppContainer {
    private fun <T> pick(mock: T, remote: () -> T): T =
        if (AppConfig.USE_MOCK) mock else remote()

    private fun remotePendiente(): Nothing =
        error("Implementación Remote pendiente (ver §16 del diseño). Usa USE_MOCK = true.")

    val authRepository: AuthRepository = pick(FakeAuthRepository()) { remotePendiente() }
    val customerRepository: CustomerRepository = pick(FakeCustomerRepository()) { remotePendiente() }
    val catalogRepository: CatalogRepository = pick(FakeCatalogRepository()) { remotePendiente() }
    val operationRepository: OperationRepository = pick(FakeOperationRepository()) { remotePendiente() }
    val discountRepository: DiscountRepository = pick(FakeDiscountRepository()) { remotePendiente() }
    val promotionRepository: PromotionRepository = pick(FakePromotionRepository()) { remotePendiente() }
    val paymentRepository: PaymentRepository = pick(FakePaymentRepository()) { remotePendiente() }
    val adminRepository: AdminRepository = pick(FakeAdminRepository()) { remotePendiente() }
}
