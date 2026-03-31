package com.pacedream.app.feature.checkout

import com.pacedream.app.core.config.AppConfig
import com.pacedream.app.core.network.ApiClient
import com.pacedream.app.core.network.ApiError
import com.pacedream.app.core.network.ApiResult
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

// ── API Models (iOS parity: NativePaymentService.swift) ─────────────

@Serializable
data class QuoteResponse(
    @SerialName("quote_id") val quoteId: String,
    val currency: String = "usd",
    @SerialName("base_amount_cents") val baseAmountCents: Int,
    @SerialName("service_fee_cents") val serviceFeeCents: Int,
    @SerialName("pilot_pricing") val pilotPricing: Boolean? = null,
    @SerialName("tax_cents") val taxCents: Int = 0,
    @SerialName("total_cents") val totalCents: Int,
    @SerialName("expires_at") val expiresAt: String? = null
)

@Serializable
data class PaymentSheetConfig(
    val publishableKey: String? = null,
    val paymentIntentClientSecret: String,
    val merchantDisplayName: String = "PaceDream",
    val customerId: String? = null,
    val ephemeralKeySecret: String? = null
)

@Serializable
data class ConfirmBookingData(
    @SerialName("_id") val id: String? = null,
    val title: String? = null,
    val status: String? = null,
    val startDate: String? = null,
    val endDate: String? = null,
    val location: String? = null,
    val priceTotal: Double? = null,
    val createdAt: String? = null
)

@Serializable
data class ConfirmBookingResponse(
    val success: Boolean = false,
    val booking: ConfirmBookingData? = null
)

// ── Repository ──────────────────────────────────────────────────────

/**
 * NativePaymentRepository - Handles native Stripe PaymentSheet flow.
 *
 * iOS parity: NativePaymentService.swift
 *
 * Flow:
 *   1. createQuote()       → POST /payments/native/quote
 *   2. createPaymentIntent() → POST /payments/native/payment-intent
 *   3. confirmBooking()     → POST /payments/native/confirm-booking
 */
@Singleton
class NativePaymentRepository @Inject constructor(
    private val apiClient: ApiClient,
    private val appConfig: AppConfig,
    private val json: Json
) {

    /**
     * Step 1: Create a server-side quote for the booking.
     * Returns pricing breakdown (base, fees, tax, total) and a quoteId.
     */
    suspend fun createQuote(
        listingId: String,
        bookingType: String,
        startTime: String,
        endTime: String,
        quantity: Int = 1
    ): ApiResult<QuoteResponse> {
        val url = appConfig.buildApiUrl("payments", "native", "quote")
        val body = buildJsonObject {
            put("listingId", listingId)
            put("bookingType", bookingType)
            put("startTime", startTime)
            put("endTime", endTime)
            put("quantity", quantity)
        }.toString()

        return when (val result = apiClient.post(url, body, includeAuth = true)) {
            is ApiResult.Success -> {
                try {
                    val quote = json.decodeFromString(QuoteResponse.serializer(), result.data)
                    ApiResult.Success(quote)
                } catch (e: Exception) {
                    Timber.e(e, "Failed to parse quote response")
                    ApiResult.Failure(ApiError.DecodingError())
                }
            }
            is ApiResult.Failure -> result
        }
    }

    /**
     * Step 2: Create a Stripe PaymentIntent and get the client secret + ephemeral key.
     * The backend ties the PaymentIntent to the quote.
     */
    suspend fun createPaymentIntent(quoteId: String): ApiResult<PaymentSheetConfig> {
        val url = appConfig.buildApiUrl("payments", "native", "payment-intent")
        val body = buildJsonObject {
            put("quote_id", quoteId)
            // Only allow card payments (Google Pay uses the card rail in Stripe).
            // This prevents the backend from enabling ACH / US bank account / bank
            // transfer methods on the PaymentIntent for guest checkout.
            put("payment_method_types", buildJsonArray { add(JsonPrimitive("card")) })
        }.toString()

        return when (val result = apiClient.post(url, body, includeAuth = true)) {
            is ApiResult.Success -> {
                try {
                    val config = json.decodeFromString(PaymentSheetConfig.serializer(), result.data)
                    ApiResult.Success(config)
                } catch (e: Exception) {
                    Timber.e(e, "Failed to parse payment intent response")
                    ApiResult.Failure(ApiError.DecodingError())
                }
            }
            is ApiResult.Failure -> result
        }
    }

    /**
     * Step 3: Confirm booking after PaymentSheet reports success.
     * The backend verifies the PaymentIntent with Stripe and creates the booking record.
     */
    suspend fun confirmBooking(paymentIntentId: String): ApiResult<ConfirmBookingResponse> {
        val url = appConfig.buildApiUrl("payments", "native", "confirm-booking")
        val body = buildJsonObject {
            put("payment_intent_id", paymentIntentId)
        }.toString()

        return when (val result = apiClient.post(url, body, includeAuth = true)) {
            is ApiResult.Success -> {
                try {
                    val response = json.decodeFromString(ConfirmBookingResponse.serializer(), result.data)
                    ApiResult.Success(response)
                } catch (e: Exception) {
                    Timber.e(e, "Failed to parse confirm booking response")
                    ApiResult.Failure(ApiError.DecodingError())
                }
            }
            is ApiResult.Failure -> result
        }
    }

    /**
     * Resolve Stripe publishable key.
     * Priority: PaymentSheetConfig response → BuildConfig → backend /stripe/config
     */
    suspend fun resolvePublishableKey(config: PaymentSheetConfig): String? {
        // 1. From payment-intent response
        if (!config.publishableKey.isNullOrBlank()) return config.publishableKey

        // 2. From BuildConfig
        val localKey = try {
            com.shourov.apps.pacedream.BuildConfig.STRIPE_PUBLISHABLE_KEY
        } catch (_: Exception) { "" }
        if (localKey.isNotBlank()) return localKey

        // 3. Fetch from backend
        return fetchPublishableKeyFromBackend()
    }

    private suspend fun fetchPublishableKeyFromBackend(): String? {
        val url = appConfig.buildApiUrl("stripe", "config")
        return when (val result = apiClient.get(url, includeAuth = true)) {
            is ApiResult.Success -> {
                try {
                    val element = json.parseToJsonElement(result.data)
                    val obj = element as? kotlinx.serialization.json.JsonObject
                    obj?.get("publishableKey")
                        ?.let { (it as? kotlinx.serialization.json.JsonPrimitive)?.content }
                        ?.takeIf { it.isNotBlank() }
                } catch (e: Exception) {
                    Timber.e(e, "Failed to parse stripe config")
                    null
                }
            }
            is ApiResult.Failure -> null
        }
    }

    /**
     * Extract PaymentIntent ID from client secret.
     * Format: "pi_xxx_secret_yyy" → "pi_xxx"
     */
    fun extractPaymentIntentId(clientSecret: String): String? {
        val parts = clientSecret.split("_secret_")
        val piId = parts.firstOrNull()
        return piId?.takeIf { it.startsWith("pi_") }
    }
}
