package com.shourov.apps.pacedream.feature.webflow.data

import com.shourov.apps.pacedream.core.network.api.ApiClient
import com.shourov.apps.pacedream.core.network.api.ApiError
import com.shourov.apps.pacedream.core.network.api.ApiResult
import com.shourov.apps.pacedream.core.network.auth.TokenStorage
import com.shourov.apps.pacedream.core.network.config.AppConfig
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for booking operations and Stripe checkout
 * 
 * Booking creation endpoints:
 * - Time-based: POST /v1/properties/bookings/timebased
 *   Body: { itemId, start_time, end_time, amount }
 *   Response: { status:true, data:{ checkoutUrl } }
 * 
 * - Gear: POST /v1/gear-rentals/book
 *   Body: { gearId, startDate, endDate, startTime, endTime, amount }
 *   Response: { status:true, data:{ checkoutUrl } }
 * 
 * Success confirmation endpoints:
 * - Time-based: GET /v1/properties/bookings/timebased/success/checkout?session_id=...
 * - Gear: GET /v1/gear-rentals/success/checkout?session_id=...
 */
@Singleton
class BookingRepository @Inject constructor(
    private val apiClient: ApiClient,
    private val appConfig: AppConfig,
    private val tokenStorage: TokenStorage,
    private val json: Json
) {
    
    /**
     * Create a time-based booking and get checkout URL
     */
    suspend fun createTimeBasedBooking(
        itemId: String,
        startTime: String,
        endTime: String,
        amount: Double
    ): ApiResult<CheckoutResult> {
        val url = appConfig.buildApiUrl("properties", "bookings", "timebased")
        
        val body = """
            {
                "itemId": "$itemId",
                "start_time": "$startTime",
                "end_time": "$endTime",
                "amount": $amount
            }
        """.trimIndent()
        
        return when (val result = apiClient.post(url, body, includeAuth = true)) {
            is ApiResult.Success -> {
                try {
                    val checkoutResult = parseCheckoutResponse(result.data, BookingType.TIME_BASED)
                    
                    // Store session info for resume after relaunch
                    checkoutResult.sessionId?.let { sessionId ->
                        tokenStorage.lastCheckoutSessionId = sessionId
                        tokenStorage.lastCheckoutBookingType = BookingType.TIME_BASED.name
                    }
                    
                    ApiResult.Success(checkoutResult)
                } catch (e: Exception) {
                    Timber.e(e, "Failed to parse checkout response")
                    ApiResult.Failure(ApiError.DecodingError("Failed to parse checkout URL", e))
                }
            }
            is ApiResult.Failure -> result
        }
    }
    
    /**
     * Create a gear rental booking and get checkout URL
     */
    suspend fun createGearBooking(
        gearId: String,
        startDate: String,
        endDate: String,
        startTime: String?,
        endTime: String?,
        amount: Double
    ): ApiResult<CheckoutResult> {
        val url = appConfig.buildApiUrl("gear-rentals", "book")
        
        val bodyBuilder = StringBuilder()
        bodyBuilder.append("{")
        bodyBuilder.append("\"gearId\": \"$gearId\",")
        bodyBuilder.append("\"startDate\": \"$startDate\",")
        bodyBuilder.append("\"endDate\": \"$endDate\",")
        startTime?.let { bodyBuilder.append("\"startTime\": \"$it\",") }
        endTime?.let { bodyBuilder.append("\"endTime\": \"$it\",") }
        bodyBuilder.append("\"amount\": $amount")
        bodyBuilder.append("}")
        
        return when (val result = apiClient.post(url, bodyBuilder.toString(), includeAuth = true)) {
            is ApiResult.Success -> {
                try {
                    val checkoutResult = parseCheckoutResponse(result.data, BookingType.GEAR)
                    
                    // Store session info for resume after relaunch
                    checkoutResult.sessionId?.let { sessionId ->
                        tokenStorage.lastCheckoutSessionId = sessionId
                        tokenStorage.lastCheckoutBookingType = BookingType.GEAR.name
                    }
                    
                    ApiResult.Success(checkoutResult)
                } catch (e: Exception) {
                    Timber.e(e, "Failed to parse checkout response")
                    ApiResult.Failure(ApiError.DecodingError("Failed to parse checkout URL", e))
                }
            }
            is ApiResult.Failure -> result
        }
    }
    
    /**
     * Confirm time-based booking after Stripe checkout success
     */
    suspend fun confirmTimeBasedBooking(sessionId: String): ApiResult<BookingConfirmation> {
        val url = appConfig.buildApiUrlWithQuery(
            "properties", "bookings", "timebased", "success", "checkout",
            queryParams = mapOf("session_id" to sessionId)
        )
        
        return when (val result = apiClient.get(url, includeAuth = true)) {
            is ApiResult.Success -> {
                try {
                    val confirmation = parseBookingConfirmation(result.data, BookingType.TIME_BASED)
                    clearStoredCheckout()
                    ApiResult.Success(confirmation)
                } catch (e: Exception) {
                    Timber.e(e, "Failed to parse booking confirmation")
                    ApiResult.Failure(ApiError.DecodingError("Failed to parse confirmation", e))
                }
            }
            is ApiResult.Failure -> result
        }
    }
    
    /**
     * Confirm gear booking after Stripe checkout success
     */
    suspend fun confirmGearBooking(sessionId: String): ApiResult<BookingConfirmation> {
        val url = appConfig.buildApiUrlWithQuery(
            "gear-rentals", "success", "checkout",
            queryParams = mapOf("session_id" to sessionId)
        )
        
        return when (val result = apiClient.get(url, includeAuth = true)) {
            is ApiResult.Success -> {
                try {
                    val confirmation = parseBookingConfirmation(result.data, BookingType.GEAR)
                    clearStoredCheckout()
                    ApiResult.Success(confirmation)
                } catch (e: Exception) {
                    Timber.e(e, "Failed to parse booking confirmation")
                    ApiResult.Failure(ApiError.DecodingError("Failed to parse confirmation", e))
                }
            }
            is ApiResult.Failure -> result
        }
    }
    
    /**
     * Get stored checkout session for resume after app relaunch
     */
    fun getStoredCheckout(): StoredCheckout? {
        val sessionId = tokenStorage.lastCheckoutSessionId ?: return null
        val bookingType = tokenStorage.lastCheckoutBookingType?.let { 
            try { BookingType.valueOf(it) } catch (e: Exception) { null }
        } ?: return null
        
        return StoredCheckout(sessionId, bookingType)
    }
    
    /**
     * Clear stored checkout after successful confirmation
     */
    fun clearStoredCheckout() {
        tokenStorage.lastCheckoutSessionId = null
        tokenStorage.lastCheckoutBookingType = null
    }
    
    /**
     * Handle booking cancelled - just clear stored session
     */
    fun handleBookingCancelled() {
        clearStoredCheckout()
    }
    
    // Parsing methods
    
    private fun parseCheckoutResponse(responseBody: String, type: BookingType): CheckoutResult {
        val jsonElement = json.parseToJsonElement(responseBody)
        val jsonObject = jsonElement.jsonObject
        
        val data = jsonObject["data"]?.jsonObject ?: jsonObject
        
        val checkoutUrl = data["checkoutUrl"]?.jsonPrimitive?.content
            ?: data["checkout_url"]?.jsonPrimitive?.content
            ?: data["url"]?.jsonPrimitive?.content
        
        // Extract session ID from checkout URL if present
        val sessionId = checkoutUrl?.let { url ->
            extractSessionIdFromUrl(url)
        }
        
        return CheckoutResult(
            checkoutUrl = checkoutUrl,
            sessionId = sessionId,
            bookingType = type
        )
    }
    
    private fun parseBookingConfirmation(responseBody: String, type: BookingType): BookingConfirmation {
        val jsonElement = json.parseToJsonElement(responseBody)
        val jsonObject = jsonElement.jsonObject
        
        val data = jsonObject["data"]?.jsonObject ?: jsonObject
        
        val bookingId = data["bookingId"]?.jsonPrimitive?.content
            ?: data["_id"]?.jsonPrimitive?.content
            ?: data["id"]?.jsonPrimitive?.content
            ?: ""
        
        val status = data["status"]?.jsonPrimitive?.content
            ?: "confirmed"
        
        val message = data["message"]?.jsonPrimitive?.content
            ?: "Booking confirmed successfully"
        
        val itemTitle = data["itemTitle"]?.jsonPrimitive?.content
            ?: data["title"]?.jsonPrimitive?.content
            ?: data["listingTitle"]?.jsonPrimitive?.content
        
        val startDate = data["startDate"]?.jsonPrimitive?.content
            ?: data["start_date"]?.jsonPrimitive?.content
            ?: data["startTime"]?.jsonPrimitive?.content
        
        val endDate = data["endDate"]?.jsonPrimitive?.content
            ?: data["end_date"]?.jsonPrimitive?.content
            ?: data["endTime"]?.jsonPrimitive?.content
        
        val amount = data["amount"]?.jsonPrimitive?.content?.toDoubleOrNull()
            ?: data["total"]?.jsonPrimitive?.content?.toDoubleOrNull()
        
        return BookingConfirmation(
            bookingId = bookingId,
            bookingType = type,
            status = status,
            message = message,
            itemTitle = itemTitle,
            startDate = startDate,
            endDate = endDate,
            amount = amount
        )
    }
    
    private fun extractSessionIdFromUrl(url: String): String? {
        // Try to extract session_id query parameter
        val regex = "[?&]session_id=([^&]+)".toRegex()
        return regex.find(url)?.groupValues?.getOrNull(1)
    }
}

/**
 * Result of checkout URL creation
 */
data class CheckoutResult(
    val checkoutUrl: String?,
    val sessionId: String?,
    val bookingType: BookingType
)

/**
 * Booking confirmation after Stripe success
 */
data class BookingConfirmation(
    val bookingId: String,
    val bookingType: BookingType,
    val status: String,
    val message: String,
    val itemTitle: String?,
    val startDate: String?,
    val endDate: String?,
    val amount: Double?
) {
    val formattedAmount: String
        get() = amount?.let { "$${String.format("%.2f", it)}" } ?: ""
}

/**
 * Booking type enum
 */
enum class BookingType {
    TIME_BASED,
    GEAR
}

/**
 * Stored checkout for resume after app relaunch
 */
data class StoredCheckout(
    val sessionId: String,
    val bookingType: BookingType
)

