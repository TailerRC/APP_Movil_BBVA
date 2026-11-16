package com.bbva.bonifica.domain.repository

import com.bbva.bonifica.core.AppResult
import com.bbva.bonifica.domain.model.*

// Interfaces de repositorio (diseño §7). La UI y los ViewModels SOLO conocen estas interfaces.

sealed interface LoginOutcome {
    data class Ok(val s: Session) : LoginOutcome
    data class NewPasswordRequired(val cognitoSession: String) : LoginOutcome
}

interface AuthRepository {
    suspend fun login(email: String, password: String): AppResult<LoginOutcome>
    suspend fun setNewPassword(email: String, newPassword: String, session: String): AppResult<Session>
    suspend fun restoreSession(): Session?          // usa refresh token guardado
    suspend fun refresh(): AppResult<Session>
    fun currentSession(): Session?
    fun logout()
}

interface CustomerRepository {
    suspend fun getCustomer(ref: String): AppResult<Customer>                       // ruta 11
    suspend fun getOperations(customerRef: String): AppResult<List<PointOfSale>>    // ruta 14
}

interface CatalogRepository {
    suspend fun getByCategory(category: String): AppResult<List<Product>>           // ruta 13
    suspend fun getProduct(ref: String): AppResult<Product>                         // ruta 9
}

interface OperationRepository {
    suspend fun getOperation(ref: String): AppResult<PointOfSale>                       // ruta 10
    suspend fun getDiscountsByOperation(ref: String): AppResult<List<DiscountPricing>>  // ruta 8
}

interface DiscountRepository {
    suspend fun initiate(posRef: String, productRef: String, quantity: Int): AppResult<DiscountPricing> // ruta 1
    suspend fun get(id: String): AppResult<DiscountPricing>                                              // ruta 2
    suspend fun update(id: String, status: String, paymentMethod: String?): AppResult<DiscountPricing>  // ruta 3
}

interface PromotionRepository {
    suspend fun getActivePolicies(): AppResult<List<Policy>>             // ruta 6
    suspend fun getCampaigns(status: String): AppResult<List<Campaign>>  // ruta 7
}

interface PaymentRepository {
    suspend fun getPayment(id: String): AppResult<PaymentOrder>                        // ruta 12
    suspend fun getPaymentsByOperation(posRef: String): AppResult<List<PaymentOrder>>  // ruta 15
    suspend fun getHistory(discountId: String): AppResult<List<EventLogEntry>>         // ruta 4
}

interface AdminRepository {
    suspend fun registerPolicy(p: NewPolicy): AppResult<Policy>          // ruta 5
}
