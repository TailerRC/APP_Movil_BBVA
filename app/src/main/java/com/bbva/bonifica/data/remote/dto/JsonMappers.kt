package com.bbva.bonifica.data.remote.dto

import com.bbva.bonifica.core.AppError
import com.bbva.bonifica.core.AppResult
import com.bbva.bonifica.domain.model.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import retrofit2.Response

object JsonMappers {

    val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

    fun String.toJsonRequestBody(): RequestBody = this.toRequestBody(JSON_MEDIA_TYPE)

    fun <T> parseResponse(
        response: Response<String>,
        parser: (JSONObject) -> T
    ): AppResult<T> {
        if (response.isSuccessful) {
            val bodyStr = response.body().orEmpty()
            return try {
                val json = JSONObject(bodyStr)
                AppResult.Success(parser(json))
            } catch (e: Exception) {
                AppResult.Failure(AppError.Server)
            }
        }
        return AppResult.Failure(mapHttpError(response))
    }

    fun <T> parseListResponse(
        response: Response<String>,
        arrayKey: String,
        itemParser: (JSONObject) -> T
    ): AppResult<List<T>> {
        if (response.isSuccessful) {
            val bodyStr = response.body().orEmpty()
            return try {
                val json = JSONObject(bodyStr)
                val array = json.optJSONArray(arrayKey) ?: JSONArray()
                val list = ArrayList<T>(array.length())
                for (i in 0 until array.length()) {
                    list.add(itemParser(array.getJSONObject(i)))
                }
                AppResult.Success(list)
            } catch (e: Exception) {
                AppResult.Failure(AppError.Server)
            }
        }
        return AppResult.Failure(mapHttpError(response))
    }

    private fun mapHttpError(response: Response<*>): AppError {
        val errorBody = response.errorBody()?.string().orEmpty()
        val errorMsg = try {
            val json = JSONObject(errorBody)
            when {
                json.has("error") -> json.getString("error")
                json.has("message") -> json.getString("message")
                else -> ""
            }
        } catch (_: Exception) {
            ""
        }

        return when (response.code()) {
            400 -> AppError.BadRequest(if (errorMsg.isNotBlank()) errorMsg else "Petición inválida")
            401 -> AppError.Unauthorized
            403 -> AppError.Forbidden(if (errorMsg.isNotBlank()) errorMsg else "No tienes permiso: solo puedes consultar tus propios datos")
            404 -> AppError.NotFound(if (errorMsg.isNotBlank()) errorMsg else "recurso")
            409 -> AppError.Conflict(if (errorMsg.isNotBlank()) errorMsg else "Este descuento ya fue confirmado o anulado")
            in 500..599 -> AppError.Server
            else -> AppError.Server
        }
    }

    fun toCustomer(j: JSONObject): Customer = Customer(
        customerReference = j.optString("customerReference"),
        name = j.optString("name"),
        documentType = j.optString("documentType"),
        documentNumber = j.optString("documentNumber"),
        email = j.optString("email"),
        phone = j.optString("phone"),
        segment = j.optString("segment"),
        city = j.optString("city"),
        status = j.optString("status"),
        customerSince = j.optString("customerSince")
    )

    fun toPointOfSale(j: JSONObject): PointOfSale = PointOfSale(
        pointOfSaleReference = j.optString("pointOfSaleReference"),
        customerReference = j.optString("customerReference"),
        channel = j.optString("channel"),
        officeCode = j.optString("officeCode"),
        officeName = j.optString("officeName"),
        city = j.optString("city"),
        status = j.optString("status"),
        createdAt = j.optString("createdAt")
    )

    fun toProduct(j: JSONObject): Product = Product(
        productReference = j.optString("productReference"),
        name = j.optString("name"),
        category = j.optString("category"),
        listPrice = j.optDouble("listPrice", 0.0),
        currency = j.optString("currency", "PEN"),
        billingFrequency = j.optString("billingFrequency", "MONTHLY"),
        status = j.optString("status", "AVAILABLE")
    )

    fun toDiscountPricing(j: JSONObject): DiscountPricing = DiscountPricing(
        discountPricingId = j.optString("discountPricingId"),
        pointOfSaleReference = j.optString("pointOfSaleReference"),
        customerReference = j.optString("customerReference"),
        productReference = j.optString("productReference"),
        category = j.optString("category"),
        channel = j.optString("channel"),
        currency = j.optString("currency", "PEN"),
        listPrice = j.optDouble("listPrice", 0.0),
        quantity = j.optInt("quantity", 1),
        grossAmount = j.optDouble("grossAmount", 0.0),
        policyId = j.optString("policyId", "NONE"),
        policyName = j.optString("policyName", "Sin descuento"),
        discountAmount = j.optDouble("discountAmount", 0.0),
        netAmount = j.optDouble("netAmount", 0.0),
        status = j.optString("status", "INITIATED"),
        createdAt = j.optString("createdAt"),
        updatedAt = j.optString("updatedAt"),
        paymentOrderId = if (j.has("paymentOrderId") && !j.isNull("paymentOrderId")) j.getString("paymentOrderId") else null
    )

    fun toPolicy(j: JSONObject): Policy = Policy(
        policyId = j.optString("policyId"),
        name = j.optString("name"),
        discountType = j.optString("discountType"),
        discountValue = j.optDouble("discountValue", 0.0),
        scope = j.optString("scope"),
        scopeReference = j.optString("scopeReference", "*"),
        minimumAmount = j.optDouble("minimumAmount", 0.0),
        currency = j.optString("currency", "PEN"),
        channel = j.optString("channel", "ALL"),
        validFrom = j.optString("validFrom"),
        validTo = j.optString("validTo"),
        status = j.optString("status", "ACTIVE")
    )

    fun toCampaign(j: JSONObject): Campaign = Campaign(
        campaignId = j.optString("campaignId"),
        policyId = j.optString("policyId"),
        name = j.optString("name"),
        targetSegment = j.optString("targetSegment"),
        channel = j.optString("channel"),
        budget = j.optDouble("budget", 0.0),
        currency = j.optString("currency", "PEN"),
        startDate = j.optString("startDate"),
        endDate = j.optString("endDate"),
        status = j.optString("status", "ACTIVE")
    )

    fun toPaymentOrder(j: JSONObject): PaymentOrder = PaymentOrder(
        paymentOrderId = j.optString("paymentOrderId"),
        discountPricingId = j.optString("discountPricingId"),
        customerReference = j.optString("customerReference"),
        pointOfSaleReference = j.optString("pointOfSaleReference"),
        amount = j.optDouble("amount", 0.0),
        currency = j.optString("currency", "PEN"),
        paymentMethod = j.optString("paymentMethod", "CARGO_EN_CUENTA"),
        status = j.optString("status", "PAID"),
        paymentDate = j.optString("paymentDate")
    )

    fun toEventLogEntry(j: JSONObject): EventLogEntry = EventLogEntry(
        discountPricingId = j.optString("discountPricingId"),
        eventTimestamp = j.optString("eventTimestamp"),
        eventType = j.optString("eventType"),
        previousStatus = if (j.has("previousStatus") && !j.isNull("previousStatus")) j.getString("previousStatus") else null,
        newStatus = if (j.has("newStatus") && !j.isNull("newStatus")) j.getString("newStatus") else null,
        actor = j.optString("actor"),
        channel = if (j.has("channel") && !j.isNull("channel")) j.getString("channel") else null,
        policyId = j.optString("policyId"),
        details = j.optString("details")
    )
}
