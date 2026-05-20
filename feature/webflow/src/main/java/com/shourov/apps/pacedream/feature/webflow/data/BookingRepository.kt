package com.shourov.apps.pacedream.feature.webflow.data

import com.shourov.apps.pacedream.core.common.featureflags.FeatureFlags
import com.shourov.apps.pacedream.core.network.api.ApiClient
import com.shourov.apps.pacedream.core.network.api.ApiError
import com.shourov.apps.pacedream.core.network.api.ApiResult
import com.shourov.apps.pacedream.core.network.auth.TokenStorage
import com.shourov.apps.pacedream.core.network.config.AppConfig
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import timber.log.Timber
import androidx.annotation.VisibleForTesting
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean
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
     * Client-side in-flight guard.  A second Reserve tap while a
     * previous POST is still pending would otherwise race the first one
     * — two booking rows + two Stripe Checkout Sessions get created.
     * This flag is process-wide (one guest reserves one listing at a
     * time) and is released in a `finally` so an exception can never
     * leave it stuck.
     */
    private val inFlightCreate = AtomicBoolean(false)

    /**
     * Create a time-based booking and get checkout URL
     */
    suspend fun createTimeBasedBooking(
        itemId: String,
        startTime: String,
        endTime: String,
        amount: Double
    ): ApiResult<CheckoutResult> {
        if (!inFlightCreate.compareAndSet(false, true)) {
            WebflowTelemetry.inFlightBlocked(itemId, BookingType.TIME_BASED)
            return ApiResult.Failure(
                ApiError.Unknown("A reservation is already being created. Please wait a moment.")
            )
        }
        try {
            WebflowTelemetry.createAttempt(
                itemId,
                BookingType.TIME_BASED,
                FeatureFlags.WEBFLOW_IDEMPOTENCY_KEY,
            )

            val url = appConfig.buildApiUrl("properties", "bookings", "timebased")

            val body = buildJsonObject {
                put("itemId", itemId)
                put("start_time", startTime)
                put("end_time", endTime)
                put("amount", amount)
            }.toString()

            // Mint (or reuse) a per-attempt idempotency key, persisted across
            // process death so a relaunch retry hits the same backend dedup
            // slot.  Only sent on the wire when the feature flag is on —
            // unknown headers are dropped by servers that do not honour the
            // contract, but flipping the flag without backend support
            // would give a false sense of security.
            val idempotencyKey = acquireOrReuseIdempotencyKey()
            val headers = idempotencyHeader(idempotencyKey)

            return when (val result = apiClient.post(url, body, includeAuth = true, additionalHeaders = headers)) {
                is ApiResult.Success -> {
                    try {
                        val checkoutResult = parseCheckoutResponse(result.data, BookingType.TIME_BASED)

                        // Store session info for resume after relaunch
                        checkoutResult.sessionId?.let { sessionId ->
                            tokenStorage.lastCheckoutSessionId = sessionId
                            tokenStorage.lastCheckoutBookingType = BookingType.TIME_BASED.name
                        }

                        // Booking row is now committed on the backend; the
                        // idempotency key has done its job for this attempt.
                        // A future Reserve tap (potentially for a different
                        // listing) should mint a fresh key.
                        tokenStorage.checkoutIdempotencyKey = null
                        WebflowTelemetry.createSucceeded(
                            itemId,
                            BookingType.TIME_BASED,
                            checkoutResult.sessionId != null,
                        )

                        ApiResult.Success(checkoutResult)
                    } catch (e: Exception) {
                        Timber.e(e, "Failed to parse checkout response")
                        WebflowTelemetry.createFailed(itemId, BookingType.TIME_BASED, "parse_error")
                        ApiResult.Failure(ApiError.DecodingError("Failed to parse checkout URL", e))
                    }
                }
                is ApiResult.Failure -> {
                    WebflowTelemetry.createFailed(
                        itemId,
                        BookingType.TIME_BASED,
                        result.error::class.simpleName ?: "unknown",
                    )
                    result
                }
            }
        } finally {
            inFlightCreate.set(false)
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
        if (!inFlightCreate.compareAndSet(false, true)) {
            WebflowTelemetry.inFlightBlocked(gearId, BookingType.GEAR)
            return ApiResult.Failure(
                ApiError.Unknown("A reservation is already being created. Please wait a moment.")
            )
        }
        try {
            WebflowTelemetry.createAttempt(
                gearId,
                BookingType.GEAR,
                FeatureFlags.WEBFLOW_IDEMPOTENCY_KEY,
            )

            val url = appConfig.buildApiUrl("gear-rentals", "book")

            val body = buildJsonObject {
                put("gearId", gearId)
                put("startDate", startDate)
                put("endDate", endDate)
                startTime?.let { put("startTime", it) }
                endTime?.let { put("endTime", it) }
                put("amount", amount)
            }.toString()

            val idempotencyKey = acquireOrReuseIdempotencyKey()
            val headers = idempotencyHeader(idempotencyKey)

            return when (val result = apiClient.post(url, body, includeAuth = true, additionalHeaders = headers)) {
                is ApiResult.Success -> {
                    try {
                        val checkoutResult = parseCheckoutResponse(result.data, BookingType.GEAR)

                        // Store session info for resume after relaunch
                        checkoutResult.sessionId?.let { sessionId ->
                            tokenStorage.lastCheckoutSessionId = sessionId
                            tokenStorage.lastCheckoutBookingType = BookingType.GEAR.name
                        }

                        // Booking row committed; allow the next attempt to
                        // mint a fresh idempotency key (see equivalent note
                        // in createTimeBasedBooking).
                        tokenStorage.checkoutIdempotencyKey = null
                        WebflowTelemetry.createSucceeded(
                            gearId,
                            BookingType.GEAR,
                            checkoutResult.sessionId != null,
                        )

                        ApiResult.Success(checkoutResult)
                    } catch (e: Exception) {
                        Timber.e(e, "Failed to parse checkout response")
                        WebflowTelemetry.createFailed(gearId, BookingType.GEAR, "parse_error")
                        ApiResult.Failure(ApiError.DecodingError("Failed to parse checkout URL", e))
                    }
                }
                is ApiResult.Failure -> {
                    WebflowTelemetry.createFailed(
                        gearId,
                        BookingType.GEAR,
                        result.error::class.simpleName ?: "unknown",
                    )
                    result
                }
            }
        } finally {
            inFlightCreate.set(false)
        }
    }

    /**
     * Returns the persisted idempotency key when one exists (a previous
     * Reserve attempt against the same draft is being retried), otherwise
     * mints a fresh one and persists it.  Persistence is essential — a
     * process death between this call and the network response would
     * otherwise let the relaunch mint a new key and create a duplicate
     * booking row.
     */
    private fun acquireOrReuseIdempotencyKey(): String {
        val existing = tokenStorage.checkoutIdempotencyKey
        if (!existing.isNullOrBlank()) return existing
        val fresh = UUID.randomUUID().toString()
        tokenStorage.checkoutIdempotencyKey = fresh
        return fresh
    }

    /**
     * Builds the request header map for the booking-creation POST.
     * Gated by [FeatureFlags.WEBFLOW_IDEMPOTENCY_KEY] so the header is
     * only sent when backend support has been verified.
     */
    private fun idempotencyHeader(key: String): Map<String, String> {
        if (!FeatureFlags.WEBFLOW_IDEMPOTENCY_KEY) return emptyMap()
        return mapOf("Idempotency-Key" to key)
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
     * Clear stored checkout after successful confirmation.  Also drops
     * the per-attempt idempotency key so a follow-up Reserve (which is
     * semantically a brand new attempt) mints a fresh key rather than
     * accidentally deduping against the just-completed booking.
     */
    fun clearStoredCheckout() {
        tokenStorage.lastCheckoutSessionId = null
        tokenStorage.lastCheckoutBookingType = null
        tokenStorage.checkoutIdempotencyKey = null
    }
    
    /**
     * Handle booking cancelled - just clear stored session
     */
    fun handleBookingCancelled() {
        clearStoredCheckout()
    }
    
    @VisibleForTesting
    internal fun parseCheckoutResponseForTest(responseBody: String, type: BookingType): CheckoutResult =
        parseCheckoutResponse(responseBody, type)

    @VisibleForTesting
    internal fun parseBookingConfirmationForTest(responseBody: String, type: BookingType): BookingConfirmation =
        parseBookingConfirmation(responseBody, type)

    @VisibleForTesting
    internal fun extractSessionIdFromUrlForTest(url: String): String? =
        extractSessionIdFromUrl(url)

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

        // Fail closed: never render success without an explicit bookingId
        // AND a confirmed/succeeded/paid status from the server.  A
        // missing bookingId means the booking row may not exist yet
        // (webhook in flight, stale session, malformed 200); a missing
        // or unknown status means we have no signal that payment
        // actually completed.  Either case is bubbled up as a parse
        // failure so the user sees the error UI rather than a green
        // "Booking Confirmed" with nothing behind it.
        val bookingId = data["bookingId"]?.jsonPrimitive?.content
            ?: data["_id"]?.jsonPrimitive?.content
            ?: data["id"]?.jsonPrimitive?.content
        if (bookingId.isNullOrBlank()) {
            WebflowTelemetry.confirmParseError(type, "missing_booking_id")
            throw BookingConfirmationParseException("missing_booking_id")
        }

        val rawStatus = data["status"]?.jsonPrimitive?.content
        val status = rawStatus?.trim()?.lowercase()
        if (status.isNullOrBlank() || status !in CONFIRMED_STATUSES) {
            WebflowTelemetry.confirmParseError(
                type,
                "invalid_status:${status ?: "null"}",
            )
            throw BookingConfirmationParseException("invalid_status:${status ?: "null"}")
        }

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

        WebflowTelemetry.confirmSucceeded(type)
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
        return SESSION_ID_REGEX.find(url)?.groupValues?.getOrNull(1)
    }

    companion object {
        private val SESSION_ID_REGEX = "[?&]session_id=([^&]+)".toRegex()

        /**
         * Server-side statuses that count as a real, payment-completed
         * booking.  Anything else (including "pending", "processing", or
         * a missing field) is treated as not-yet-confirmed and surfaced
         * to the user as an error rather than a green check.
         */
        internal val CONFIRMED_STATUSES = setOf("confirmed", "succeeded", "paid")
    }
}

/**
 * Thrown from [BookingRepository.parseBookingConfirmation] when the
 * server's success response is missing the fields we need to claim a
 * booking is real.  Caught at the call site and wrapped in a
 * `DecodingError` so callers see a consistent typed failure.
 */
internal class BookingConfirmationParseException(
    val reason: String,
) : RuntimeException("Booking confirmation parse error: $reason")

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


