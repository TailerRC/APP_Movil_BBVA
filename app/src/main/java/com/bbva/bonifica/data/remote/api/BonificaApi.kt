package com.bbva.bonifica.data.remote.api

import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface BonificaApi {

    // Ruta 1 · Initiate
    @POST("v1/DiscountPricing/Initiate")
    suspend fun initiate(@Body body: RequestBody): Response<String>

    // Ruta 2 · Discount Retrieve
    @GET("v1/DiscountPricing/{id}/Retrieve")
    suspend fun getDiscountPricing(@Path("id") id: String): Response<String>

    // Ruta 3 · Discount Update
    @PUT("v1/DiscountPricing/{id}/Update")
    suspend fun updateDiscountPricing(@Path("id") id: String, @Body body: RequestBody): Response<String>

    // Ruta 4 · EventLog Retrieve
    @GET("v1/DiscountPricing/{id}/EventLog/Retrieve")
    suspend fun getEventLog(@Path("id") id: String): Response<String>

    // Ruta 5 · Policy Register
    @POST("v1/DiscountPricing/Policy/Register")
    suspend fun registerPolicy(@Body body: RequestBody): Response<String>

    // Ruta 6 · Policy Retrieve
    @GET("v1/DiscountPricing/Policy/Retrieve")
    suspend fun getPolicies(): Response<String>

    // Ruta 7 · Campaign Retrieve
    @GET("v1/DiscountPricing/Campaign/Retrieve")
    suspend fun getCampaigns(@Query("status") status: String): Response<String>

    // Ruta 8 · DiscountPricing by PointOfSale
    @GET("v1/DiscountPricing/PointOfSale/{ref}/Retrieve")
    suspend fun getDiscountsByPointOfSale(@Path("ref") posRef: String): Response<String>

    // Ruta 9 · Product Retrieve
    @GET("v1/ProductDirectory/{ref}/Retrieve")
    suspend fun getProduct(@Path("ref") productRef: String): Response<String>

    // Ruta 10 · PointOfSale Retrieve
    @GET("v1/PointOfSale/{ref}/Retrieve")
    suspend fun getPointOfSale(@Path("ref") posRef: String): Response<String>

    // Ruta 11 · Customer Retrieve
    @GET("v1/Customer/{ref}/Retrieve")
    suspend fun getCustomer(@Path("ref") customerRef: String): Response<String>

    // Ruta 12 · PaymentOrder Retrieve
    @GET("v1/PaymentOrder/{id}/Retrieve")
    suspend fun getPaymentOrder(@Path("id") paymentOrderId: String): Response<String>

    // Ruta 13 · ProductDirectory Retrieve by category
    @GET("v1/ProductDirectory/Retrieve")
    suspend fun getProductsByCategory(@Query("category") category: String): Response<String>

    // Ruta 14 · Customer PointOfSale Retrieve
    @GET("v1/Customer/{ref}/PointOfSale/Retrieve")
    suspend fun getOperationsByCustomer(@Path("ref") customerRef: String): Response<String>

    // Ruta 15 · PointOfSale PaymentOrder Retrieve
    @GET("v1/PointOfSale/{ref}/PaymentOrder/Retrieve")
    suspend fun getPaymentsByPointOfSale(@Path("ref") posRef: String): Response<String>
}
