package com.pacedream.app.core.auth

import com.pacedream.app.core.config.AppConfig
import com.pacedream.app.core.network.ApiClient
import com.pacedream.app.core.network.ApiResult
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl
import javax.inject.Inject
import javax.inject.Singleton

/**
 * AuthRepository - handles auth API calls (no Authorization header on auth endpoints).
 */
@Singleton
class AuthRepository @Inject constructor(
    private val appConfig: AppConfig,
    private val apiClient: ApiClient,
    private val json: Json
) {
    suspend fun emailLogin(email: String, password: String): ApiResult<String> {
        val url = appConfig.buildApiUrl("auth", "login", "email")
        val body = json.encodeToString(
            EmailLoginRequest.serializer(),
            EmailLoginRequest(method = "email", email = email, password = password)
        )
        return apiClient.post(url, body, includeAuth = false)
    }

    suspend fun emailSignup(
        email: String,
        firstName: String,
        lastName: String,
        password: String
    ): ApiResult<String> {
        val url = appConfig.buildApiUrl("auth", "signup", "email")
        val body = json.encodeToString(
            EmailSignupRequest.serializer(),
            EmailSignupRequest(
                email = email,
                firstName = firstName,
                lastName = lastName,
                password = password
            )
        )
        return apiClient.post(url, body, includeAuth = false)
    }

    // ── Website parity: 3-step OTP signup flow ──────────────────

    /**
     * Step 1: Initiate email signup - sends verification code to email.
     * Website: POST /auth/signup/initiate { email }
     */
    suspend fun initiateEmailSignup(email: String): ApiResult<String> {
        val url = appConfig.buildApiUrl("auth", "signup", "initiate")
        val body = json.encodeToString(
            kotlinx.serialization.json.JsonObject.serializer(),
            kotlinx.serialization.json.buildJsonObject { put("email", kotlinx.serialization.json.JsonPrimitive(email)) }
        )
        return apiClient.post(url, body, includeAuth = false)
    }

    /**
     * Step 1 (mobile): Send SMS OTP for mobile signup.
     * Website: POST /auth/signup/send-sms-otp { mobile }
     */
    suspend fun initiateMobileSignup(mobile: String): ApiResult<String> {
        val url = appConfig.buildApiUrl("auth", "signup", "send-sms-otp")
        val body = json.encodeToString(
            kotlinx.serialization.json.JsonObject.serializer(),
            kotlinx.serialization.json.buildJsonObject { put("mobile", kotlinx.serialization.json.JsonPrimitive(mobile)) }
        )
        return apiClient.post(url, body, includeAuth = false)
    }

    /**
     * Step 2: Verify email OTP code.
     * Website: POST /auth/signup/verify-email-otp { email, code }
     */
    suspend fun verifyEmailOtp(email: String, code: String): ApiResult<String> {
        val url = appConfig.buildApiUrl("auth", "signup", "verify-email-otp")
        val body = json.encodeToString(
            kotlinx.serialization.json.JsonObject.serializer(),
            kotlinx.serialization.json.buildJsonObject {
                put("email", kotlinx.serialization.json.JsonPrimitive(email))
                put("code", kotlinx.serialization.json.JsonPrimitive(code))
            }
        )
        return apiClient.post(url, body, includeAuth = false)
    }

    /**
     * Step 2 (mobile): Verify SMS OTP code.
     * Website: POST /auth/signup/verify-sms-otp { mobile, otp }
     */
    suspend fun verifySmsOtp(mobile: String, otp: String): ApiResult<String> {
        val url = appConfig.buildApiUrl("auth", "signup", "verify-sms-otp")
        val body = json.encodeToString(
            kotlinx.serialization.json.JsonObject.serializer(),
            kotlinx.serialization.json.buildJsonObject {
                put("mobile", kotlinx.serialization.json.JsonPrimitive(mobile))
                put("otp", kotlinx.serialization.json.JsonPrimitive(otp))
            }
        )
        return apiClient.post(url, body, includeAuth = false)
    }

    /**
     * Step 3: Complete email signup with form data.
     * Website: POST /auth/signup/complete { email, firstName, lastName, password, ... }
     */
    suspend fun completeEmailSignup(
        email: String,
        firstName: String,
        lastName: String,
        password: String
    ): ApiResult<String> {
        val url = appConfig.buildApiUrl("auth", "signup", "complete")
        val body = json.encodeToString(
            EmailSignupCompleteRequest.serializer(),
            EmailSignupCompleteRequest(
                email = email,
                firstName = firstName,
                lastName = lastName,
                password = password
            )
        )
        return apiClient.post(url, body, includeAuth = false)
    }

    /**
     * Step 3 (mobile): Complete mobile signup with form data.
     * Website: POST /auth/signup/complete-mobile { mobile, firstName, lastName, password, ... }
     */
    suspend fun completeMobileSignup(
        mobile: String,
        firstName: String,
        lastName: String,
        password: String
    ): ApiResult<String> {
        val url = appConfig.buildApiUrl("auth", "signup", "complete-mobile")
        val body = json.encodeToString(
            MobileSignupCompleteRequest.serializer(),
            MobileSignupCompleteRequest(
                mobile = mobile,
                firstName = firstName,
                lastName = lastName,
                password = password
            )
        )
        return apiClient.post(url, body, includeAuth = false)
    }

    suspend fun auth0Callback(auth0AccessToken: String, auth0IdToken: String): ApiResult<String> {
        val url = appConfig.buildApiUrl("auth", "auth0", "callback")
        val body = json.encodeToString(
            Auth0CallbackRequest.serializer(),
            Auth0CallbackRequest(accessToken = auth0AccessToken, idToken = auth0IdToken)
        )
        return apiClient.post(url, body, includeAuth = false)
    }

    suspend fun refresh(refreshToken: String): ApiResult<String> {
        val body = json.encodeToString(
            RefreshTokenRequest.serializer(),
            RefreshTokenRequest(refreshToken = refreshToken)
        )

        val primaryUrl = appConfig.buildApiUrl("auth", "refresh-token")
        val primary = apiClient.post(primaryUrl, body, includeAuth = false)
        if (primary is ApiResult.Success) return primary

        val fallbackUrl = appConfig.buildFrontendUrl("api", "proxy", "auth", "refresh-token")
        return apiClient.post(fallbackUrl, body, includeAuth = false)
    }

    suspend fun fetchProfileWithFallbacks(): ApiResult<String> {
        val candidates = listOf(
            appConfig.buildApiUrl("account", "me"),
            appConfig.buildApiUrl("users", "get", "profile"),
            appConfig.buildApiUrl("user", "get", "profile")
        )

        var lastFailure: ApiResult.Failure? = null
        for (url in candidates) {
            val result = apiClient.get(url, includeAuth = true)
            when (result) {
                is ApiResult.Success -> return result
                is ApiResult.Failure -> {
                    // If any endpoint returns 401, propagate immediately so SessionManager can refresh once.
                    if (result.error is com.pacedream.app.core.network.ApiError.Unauthorized) return result
                    lastFailure = result
                }
            }
        }

        return lastFailure ?: ApiResult.Failure(com.pacedream.app.core.network.ApiError.Unknown("Failed to fetch profile"))
    }

    // Expose for logging/testing if needed
    fun buildApiUrl(vararg segments: String): HttpUrl = appConfig.buildApiUrl(*segments)
}

@Serializable
data class RefreshTokenRequest(val refreshToken: String)

@Serializable
data class Auth0CallbackRequest(val accessToken: String, val idToken: String)

@Serializable
data class EmailLoginRequest(val method: String = "email", val email: String, val password: String)

@Serializable
data class EmailSignupRequest(
    val email: String,
    @SerialName("first_name") val firstName: String,
    @SerialName("last_name") val lastName: String,
    val password: String,
    val dob: String? = null,
    val gender: String? = null
)

// Website parity: 3-step signup completion request models
@Serializable
data class EmailSignupCompleteRequest(
    val email: String,
    val firstName: String,
    val lastName: String,
    val password: String
)

@Serializable
data class MobileSignupCompleteRequest(
    val mobile: String,
    val firstName: String,
    val lastName: String,
    val password: String
)

