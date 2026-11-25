package com.bbva.bonifica.data.remote.api

import com.bbva.bonifica.AppConfig
import com.bbva.bonifica.core.AppError
import com.bbva.bonifica.core.AppResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

sealed interface CognitoAuthResult {
    data class Success(
        val idToken: String,
        val accessToken: String,
        val refreshToken: String?,
        val expiresIn: Long
    ) : CognitoAuthResult

    data class NewPasswordChallenge(
        val session: String
    ) : CognitoAuthResult
}

class CognitoApi(
    private val client: OkHttpClient,
    private val cognitoUrl: String = AppConfig.COGNITO_URL,
    private val clientId: String = AppConfig.COGNITO_CLIENT_ID
) {
    private val mediaType = "application/x-amz-json-1.1".toMediaType()

    suspend fun login(email: String, pass: String): AppResult<CognitoAuthResult> = withContext(Dispatchers.IO) {
        val payload = JSONObject().apply {
            put("AuthFlow", "USER_PASSWORD_AUTH")
            put("ClientId", clientId)
            put("AuthParameters", JSONObject().apply {
                put("USERNAME", email)
                put("PASSWORD", pass)
            })
        }
        executeCall("InitiateAuth", payload)
    }

    suspend fun setNewPassword(
        email: String,
        newPass: String,
        session: String
    ): AppResult<CognitoAuthResult> = withContext(Dispatchers.IO) {
        val payload = JSONObject().apply {
            put("ChallengeName", "NEW_PASSWORD_REQUIRED")
            put("ClientId", clientId)
            put("Session", session)
            put("ChallengeResponses", JSONObject().apply {
                put("USERNAME", email)
                put("NEW_PASSWORD", newPass)
            })
        }
        executeCall("RespondToAuthChallenge", payload)
    }

    suspend fun refreshToken(refreshToken: String): AppResult<CognitoAuthResult> = withContext(Dispatchers.IO) {
        val payload = JSONObject().apply {
            put("AuthFlow", "REFRESH_TOKEN_AUTH")
            put("ClientId", clientId)
            put("AuthParameters", JSONObject().apply {
                put("REFRESH_TOKEN", refreshToken)
            })
        }
        executeCall("InitiateAuth", payload)
    }

    private fun executeCall(targetOperation: String, bodyJson: JSONObject): AppResult<CognitoAuthResult> {
        val requestBody = bodyJson.toString().toRequestBody(mediaType)
        val request = Request.Builder()
            .url(cognitoUrl)
            .addHeader("Content-Type", "application/x-amz-json-1.1")
            .addHeader("X-Amz-Target", "AWSCognitoIdentityProviderService.$targetOperation")
            .post(requestBody)
            .build()

        return try {
            val response = client.newCall(request).execute()
            val raw = response.body?.string().orEmpty()

            if (response.isSuccessful) {
                val json = JSONObject(raw)
                val challenge = json.optString("ChallengeName")
                if (challenge == "NEW_PASSWORD_REQUIRED") {
                    val session = json.optString("Session")
                    AppResult.Success(CognitoAuthResult.NewPasswordChallenge(session))
                } else {
                    val authResult = json.getJSONObject("AuthenticationResult")
                    val idToken = authResult.getString("IdToken")
                    val accessToken = authResult.getString("AccessToken")
                    val refreshToken = if (authResult.has("RefreshToken")) authResult.getString("RefreshToken") else null
                    val expiresIn = authResult.optLong("ExpiresIn", 3600L)
                    AppResult.Success(CognitoAuthResult.Success(idToken, accessToken, refreshToken, expiresIn))
                }
            } else {
                mapCognitoError(raw, response.code)
            }
        } catch (e: Exception) {
            AppResult.Failure(AppError.Network)
        }
    }

    private fun mapCognitoError(rawBody: String, code: Int): AppResult.Failure {
        return try {
            val json = JSONObject(rawBody)
            val type = json.optString("__type", "")
            when {
                type.contains("NotAuthorizedException") || type.contains("UserNotFoundException") ->
                    AppResult.Failure(AppError.InvalidCredentials)
                type.contains("InvalidPasswordException") ->
                    AppResult.Failure(AppError.InvalidPassword)
                code in 500..599 ->
                    AppResult.Failure(AppError.Server)
                else -> {
                    val message = json.optString("message", "Error de autenticación")
                    AppResult.Failure(AppError.BadRequest(message))
                }
            }
        } catch (_: Exception) {
            AppResult.Failure(if (code in 500..599) AppError.Server else AppError.Network)
        }
    }
}
