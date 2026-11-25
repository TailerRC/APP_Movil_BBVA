package com.bbva.bonifica.data.repository

import com.bbva.bonifica.core.AppError
import com.bbva.bonifica.core.AppResult
import com.bbva.bonifica.data.auth.JwtDecoder
import com.bbva.bonifica.data.auth.SessionManager
import com.bbva.bonifica.data.remote.api.BonificaApi
import com.bbva.bonifica.data.remote.api.CognitoApi
import com.bbva.bonifica.data.remote.api.CognitoAuthResult
import com.bbva.bonifica.data.remote.dto.JsonMappers
import com.bbva.bonifica.data.remote.dto.JsonMappers.toJsonRequestBody
import com.bbva.bonifica.domain.model.*
import com.bbva.bonifica.domain.repository.*
import org.json.JSONObject

class RemoteAuthRepository(
    private val cognitoApi: CognitoApi,
    private val sessionManager: SessionManager
) : AuthRepository {

    override suspend fun login(email: String, password: String): AppResult<LoginOutcome> {
        return when (val result = cognitoApi.login(email, password)) {
            is AppResult.Failure -> result
            is AppResult.Success -> {
                when (val auth = result.data) {
                    is CognitoAuthResult.NewPasswordChallenge -> {
                        AppResult.Success(LoginOutcome.NewPasswordRequired(auth.session))
                    }
                    is CognitoAuthResult.Success -> {
                        val session = buildSession(email, auth.idToken, auth.expiresIn)
                        if (session != null) {
                            sessionManager.save(session)
                            AppResult.Success(LoginOutcome.Ok(session))
                        } else {
                            AppResult.Failure(AppError.Server)
                        }
                    }
                }
            }
        }
    }

    override suspend fun setNewPassword(
        email: String,
        newPassword: String,
        session: String
    ): AppResult<Session> {
        return when (val result = cognitoApi.setNewPassword(email, newPassword, session)) {
            is AppResult.Failure -> result
            is AppResult.Success -> {
                when (val auth = result.data) {
                    is CognitoAuthResult.Success -> {
                        val newSession = buildSession(email, auth.idToken, auth.expiresIn)
                        if (newSession != null) {
                            sessionManager.save(newSession)
                            AppResult.Success(newSession)
                        } else {
                            AppResult.Failure(AppError.Server)
                        }
                    }
                    is CognitoAuthResult.NewPasswordChallenge -> {
                        AppResult.Failure(AppError.BadRequest("Se requiere confirmación adicional"))
                    }
                }
            }
        }
    }

    override suspend fun restoreSession(): Session? {
        return sessionManager.restore()
    }

    override suspend fun refresh(): AppResult<Session> {
        val current = sessionManager.current()
        return if (current != null) AppResult.Success(current) else AppResult.Failure(AppError.Unauthorized)
    }

    override fun currentSession(): Session? = sessionManager.current()

    override fun logout() {
        sessionManager.clear()
    }

    private fun buildSession(email: String, idToken: String, expiresIn: Long): Session? {
        val claims = JwtDecoder.decode(idToken) ?: return null
        val role = claims.role ?: return null
        val expiresAt = if (claims.expiresAtEpochSec > 0) {
            claims.expiresAtEpochSec
        } else {
            System.currentTimeMillis() / 1000 + expiresIn
        }
        return Session(
            email = claims.email ?: email,
            role = role,
            customerReference = claims.customerReference,
            idToken = idToken,
            expiresAtEpochSec = expiresAt
        )
    }
}

class RemoteCustomerRepository(
    private val api: BonificaApi
) : CustomerRepository {

    override suspend fun getCustomer(ref: String): AppResult<Customer> =
        JsonMappers.parseResponse(api.getCustomer(ref), JsonMappers::toCustomer)

    override suspend fun getOperations(customerRef: String): AppResult<List<PointOfSale>> =
        JsonMappers.parseListResponse(api.getOperationsByCustomer(customerRef), "operations", JsonMappers::toPointOfSale)
}

class RemoteCatalogRepository(
    private val api: BonificaApi
) : CatalogRepository {

    override suspend fun getByCategory(category: String): AppResult<List<Product>> =
        JsonMappers.parseListResponse(api.getProductsByCategory(category), "products", JsonMappers::toProduct)

    override suspend fun getProduct(ref: String): AppResult<Product> =
        JsonMappers.parseResponse(api.getProduct(ref), JsonMappers::toProduct)
}

class RemoteOperationRepository(
    private val api: BonificaApi
) : OperationRepository {

    override suspend fun getOperation(ref: String): AppResult<PointOfSale> =
        JsonMappers.parseResponse(api.getPointOfSale(ref), JsonMappers::toPointOfSale)

    override suspend fun getDiscountsByOperation(ref: String): AppResult<List<DiscountPricing>> =
        JsonMappers.parseListResponse(api.getDiscountsByPointOfSale(ref), "transactions", JsonMappers::toDiscountPricing)
}

class RemoteDiscountRepository(
    private val api: BonificaApi
) : DiscountRepository {

    override suspend fun initiate(
        posRef: String,
        productRef: String,
        quantity: Int
    ): AppResult<DiscountPricing> {
        val body = JSONObject().apply {
            put("pointOfSaleReference", posRef)
            put("productReference", productRef)
            put("quantity", quantity)
        }.toString().toJsonRequestBody()

        return JsonMappers.parseResponse(api.initiate(body), JsonMappers::toDiscountPricing)
    }

    override suspend fun get(id: String): AppResult<DiscountPricing> =
        JsonMappers.parseResponse(api.getDiscountPricing(id), JsonMappers::toDiscountPricing)

    override suspend fun update(
        id: String,
        status: String,
        paymentMethod: String?
    ): AppResult<DiscountPricing> {
        val json = JSONObject().apply {
            put("status", status)
            if (!paymentMethod.isNullOrBlank()) {
                put("paymentMethod", paymentMethod)
            }
        }
        return JsonMappers.parseResponse(
            api.updateDiscountPricing(id, json.toString().toJsonRequestBody()),
            JsonMappers::toDiscountPricing
        )
    }
}

class RemotePromotionRepository(
    private val api: BonificaApi
) : PromotionRepository {

    override suspend fun getActivePolicies(): AppResult<List<Policy>> =
        JsonMappers.parseListResponse(api.getPolicies(), "policies", JsonMappers::toPolicy)

    override suspend fun getCampaigns(status: String): AppResult<List<Campaign>> =
        JsonMappers.parseListResponse(api.getCampaigns(status), "campaigns", JsonMappers::toCampaign)
}

class RemotePaymentRepository(
    private val api: BonificaApi
) : PaymentRepository {

    override suspend fun getPayment(id: String): AppResult<PaymentOrder> =
        JsonMappers.parseResponse(api.getPaymentOrder(id), JsonMappers::toPaymentOrder)

    override suspend fun getPaymentsByOperation(posRef: String): AppResult<List<PaymentOrder>> =
        JsonMappers.parseListResponse(api.getPaymentsByPointOfSale(posRef), "payments", JsonMappers::toPaymentOrder)

    override suspend fun getHistory(discountId: String): AppResult<List<EventLogEntry>> =
        JsonMappers.parseListResponse(api.getEventLog(discountId), "events", JsonMappers::toEventLogEntry)
}

class RemoteAdminRepository(
    private val api: BonificaApi
) : AdminRepository {

    override suspend fun registerPolicy(p: NewPolicy): AppResult<Policy> {
        val json = JSONObject().apply {
            put("policyId", p.policyId)
            put("name", p.name)
            put("discountType", p.discountType)
            put("discountValue", p.discountValue)
            put("scope", p.scope)
            put("scopeReference", p.scopeReference)
            put("minimumAmount", p.minimumAmount)
            put("currency", p.currency)
            put("channel", p.channel)
            put("validFrom", p.validFrom)
            put("validTo", p.validTo)
            put("status", "ACTIVE")
        }
        return JsonMappers.parseResponse(
            api.registerPolicy(json.toString().toJsonRequestBody()),
            JsonMappers::toPolicy
        )
    }
}
