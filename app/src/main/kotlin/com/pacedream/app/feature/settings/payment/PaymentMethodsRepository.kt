package com.pacedream.app.feature.settings.payment

import com.pacedream.app.core.config.AppConfig
import com.pacedream.app.core.network.ApiClient
import com.pacedream.app.core.network.ApiError
import com.pacedream.app.core.network.ApiResult
import kotlinx.serialization.json.Json
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PaymentMethodsRepository @Inject constructor(
    private val apiClient: ApiClient,
    private val appConfig: AppConfig,
    private val json: Json
) {

    suspend fun fetchPaymentMethods(): ApiResult<List<PaymentMethod>> {
        val url = appConfig.buildFrontendUrl(
            "api",
            "proxy",
            "account",
            "payment-methods"
        )

        return when (val result = apiClient.get(url, includeAuth = true)) {
            is ApiResult.Success -> {
                try {
                    val response = json.decodeFromString(PaymentMethodsResponse.serializer(), result.data)
                    if (response.isOk && response.data != null) {
                        ApiResult.Success(response.data.paymentMethods)
                    } else {
                        val message = response.error ?: response.message ?: "Failed to load payment methods."
                        ApiResult.Failure(ApiError.ServerError(response.code ?: 500, message))
                    }
                } catch (e: Exception) {
                    Timber.e(e, "Failed to parse payment methods response")
                    ApiResult.Failure(ApiError.DecodingError())
                }
            }
            is ApiResult.Failure -> result
        }
    }

    /**
     * Create Stripe SetupIntent and return client secret.
     */
    suspend fun createSetupIntent(): ApiResult<String> {
        val url = appConfig.buildFrontendUrl(
            "api",
            "proxy",
            "account",
            "payment-methods",
            "create-setup-intent"
        )

        val emptyBody = "{}"

        return when (val result = apiClient.post(url, emptyBody, includeAuth = true)) {
            is ApiResult.Success -> {
                try {
                    val response = json.decodeFromString(SetupIntentResponse.serializer(), result.data)
                    if (response.isOk && response.data != null) {
                        ApiResult.Success(response.data.clientSecret)
                    } else {
                        val message = response.error ?: response.message ?: "Failed to create setup intent."
                        ApiResult.Failure(ApiError.ServerError(response.code ?: 500, message))
                    }
                } catch (e: Exception) {
                    Timber.e(e, "Failed to parse setup intent response")
                    ApiResult.Failure(ApiError.DecodingError())
                }
            }
            is ApiResult.Failure -> result
        }
    }

    /**
     * Set default payment method and return updated list.
     */
    suspend fun setDefault(paymentMethodId: String): ApiResult<List<PaymentMethod>> {
        val url = appConfig.buildFrontendUrl(
            "api",
            "proxy",
            "account",
            "payment-methods",
            "default"
        )

        val body = """
            {
                "paymentMethodId": "$paymentMethodId"
            }
        """.trimIndent()

        return when (val result = apiClient.post(url, body, includeAuth = true)) {
            is ApiResult.Success -> {
                try {
                    val response = json.decodeFromString(PaymentMethodsResponse.serializer(), result.data)
                    if (response.isOk && response.data != null) {
                        ApiResult.Success(response.data.paymentMethods)
                    } else {
                        val message = response.error ?: response.message ?: "Failed to set default payment method."
                        ApiResult.Failure(ApiError.ServerError(response.code ?: 500, message))
                    }
                } catch (e: Exception) {
                    Timber.e(e, "Failed to parse set-default response")
                    ApiResult.Failure(ApiError.DecodingError())
                }
            }
            is ApiResult.Failure -> result
        }
    }

    /**
     * Delete a payment method and return updated list.
     */
    suspend fun deletePaymentMethod(id: String): ApiResult<List<PaymentMethod>> {
        val url = appConfig.buildFrontendUrl(
            "api",
            "proxy",
            "account",
            "payment-methods",
            id
        )

        return when (val result = apiClient.delete(url, includeAuth = true)) {
            is ApiResult.Success -> {
                try {
                    val response = json.decodeFromString(PaymentMethodsResponse.serializer(), result.data)
                    if (response.isOk && response.data != null) {
                        ApiResult.Success(response.data.paymentMethods)
                    } else {
                        val message = response.error ?: response.message ?: "Failed to delete payment method."
                        ApiResult.Failure(ApiError.ServerError(response.code ?: 500, message))
                    }
                } catch (e: Exception) {
                    Timber.e(e, "Failed to parse delete response")
                    ApiResult.Failure(ApiError.DecodingError())
                }
            }
            is ApiResult.Failure -> result
        }
    }
}

