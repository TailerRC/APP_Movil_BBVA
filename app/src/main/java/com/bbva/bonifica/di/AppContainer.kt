package com.bbva.bonifica.di

import android.content.Context
import com.bbva.bonifica.AppConfig
import com.bbva.bonifica.data.auth.PrefsTokenStore
import com.bbva.bonifica.data.auth.SessionManager
import com.bbva.bonifica.data.mock.*
import com.bbva.bonifica.data.preferences.ThemePreferences
import com.bbva.bonifica.data.remote.api.BonificaApi
import com.bbva.bonifica.data.remote.api.CognitoApi
import com.bbva.bonifica.data.remote.interceptor.AuthInterceptor
import com.bbva.bonifica.data.repository.*
import com.bbva.bonifica.domain.repository.*
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.scalars.ScalarsConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Inyección de dependencias manual.
 * Alterna entre Mock y Remote según AppConfig.USE_MOCK.
 */
class AppContainer(context: Context) {

    /** Sesión compartida persistida. */
    val sessionManager = SessionManager(PrefsTokenStore(context))

    /** Preferencia de tema (claro/oscuro), persistida en DataStore. */
    val themePreferences = ThemePreferences(context)

    // Cliente HTTP con interceptor de autenticación y logging
    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .addInterceptor(AuthInterceptor(sessionManager))
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            })
            .build()
    }

    // Cliente para AWS Cognito (Autenticación)
    private val cognitoApi: CognitoApi by lazy {
        CognitoApi(okHttpClient)
    }

    // Cliente Retrofit para AWS API Gateway
    private val bonificaApi: BonificaApi by lazy {
        Retrofit.Builder()
            .baseUrl(AppConfig.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(ScalarsConverterFactory.create())
            .build()
            .create(BonificaApi::class.java)
    }

    private fun <T> pick(mock: T, remote: () -> T): T =
        if (AppConfig.USE_MOCK) mock else remote()

    val authRepository: AuthRepository = pick(FakeAuthRepository(sessionManager)) {
        RemoteAuthRepository(cognitoApi, sessionManager)
    }

    val customerRepository: CustomerRepository = pick(FakeCustomerRepository(sessionManager::current)) {
        RemoteCustomerRepository(bonificaApi)
    }

    val catalogRepository: CatalogRepository = pick(FakeCatalogRepository()) {
        RemoteCatalogRepository(bonificaApi)
    }

    val operationRepository: OperationRepository = pick(FakeOperationRepository()) {
        RemoteOperationRepository(bonificaApi)
    }

    val discountRepository: DiscountRepository = pick(FakeDiscountRepository()) {
        RemoteDiscountRepository(bonificaApi)
    }

    val promotionRepository: PromotionRepository = pick(FakePromotionRepository()) {
        RemotePromotionRepository(bonificaApi)
    }

    val paymentRepository: PaymentRepository = pick(FakePaymentRepository()) {
        RemotePaymentRepository(bonificaApi)
    }

    val adminRepository: AdminRepository = pick(FakeAdminRepository()) {
        RemoteAdminRepository(bonificaApi)
    }
}
