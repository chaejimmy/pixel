/*
 * Copyright 2024 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.shourov.apps.pacedream.core.data.repository

import com.shourov.apps.pacedream.core.common.result.Result
import com.shourov.apps.pacedream.core.database.dao.BookingDao
import com.shourov.apps.pacedream.core.database.entity.asEntity
import com.shourov.apps.pacedream.core.database.entity.asExternalModel
import com.shourov.apps.pacedream.core.network.api.ApiClient
import com.shourov.apps.pacedream.core.network.api.ApiResult
import com.shourov.apps.pacedream.core.network.config.AppConfig
import com.shourov.apps.pacedream.core.network.services.PaceDreamApiService
import com.shourov.apps.pacedream.model.BookingModel
import com.shourov.apps.pacedream.model.BookingStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for booking data.
 *
 * iOS parity: fetches bookings from the API with tolerant parsing and
 * multiple fallback routes, caching results in Room for offline access.
 *
 * Booking detail: GET /v1/bookings/{id}
 *   - Handles shapes: direct object, { booking: ... }, { data: { booking: ... } }, { data: ... }
 *
 * My bookings (iOS fallback chain):
 *   1. GET /v1/bookings/mine
 *   2. GET /v1/guest/bookings
 *   3. GET /v1/account/bookings?role=guest
 */
@Singleton
class BookingRepository @Inject constructor(
    private val apiService: PaceDreamApiService,
    private val apiClient: ApiClient,
    private val appConfig: AppConfig,
    private val json: Json,
    private val bookingDao: BookingDao
) {

    fun getUserBookings(userName: String): Flow<Result<List<BookingModel>>> {
        return bookingDao.getBookingsByUserName(userName).map { entities ->
            Result.Success(entities.map { it.asExternalModel() })
        }
    }

    /**
     * Fetch booking by ID from API (iOS parity), caching in Room.
     * Falls back to Room if API is unreachable.
     */
    fun getBookingById(bookingId: String): Flow<Result<BookingModel?>> = flow {
        emit(Result.Loading)

        // Try API first (iOS parity: GET /v1/bookings/{id})
        val apiBooking = fetchBookingByIdFromApi(bookingId)
        if (apiBooking != null) {
            // Cache to Room for offline access
            apiBooking.asEntity()?.let { entity ->
                try {
                    bookingDao.insertBooking(entity)
                } catch (e: Exception) {
                    Timber.w(e, "Failed to cache booking to Room")
                }
            }
            emit(Result.Success(apiBooking))
            return@flow
        }

        // Fallback: try Room cache
        Timber.d("API fetch failed for booking $bookingId, falling back to Room cache")
        val cachedEntity = bookingDao.getBookingByIdOnce(bookingId)
        emit(Result.Success(cachedEntity?.asExternalModel()))
    }

    /**
     * Fetch user's bookings from API with iOS fallback routes.
     *
     * Routes tried in order (matching iOS BookingsService.fetchMyBookings):
     *   1. GET /v1/bookings/mine
     *   2. GET /v1/guest/bookings
     *   3. GET /v1/account/bookings?role=guest
     */
    suspend fun fetchMyBookings(): Result<List<BookingModel>> {
        data class Route(val pathSegments: List<String>, val queryParams: Map<String, String> = emptyMap())

        val routes = listOf(
            Route(listOf("bookings", "mine")),
            Route(listOf("guest", "bookings")),
            Route(listOf("account", "bookings"), mapOf("role" to "guest"))
        )

        var lastError: Exception? = null
        var bestResult: List<BookingModel>? = null

        for ((index, route) in routes.withIndex()) {
            try {
                val url = if (route.queryParams.isNotEmpty()) {
                    appConfig.buildApiUrlWithQuery(
                        *route.pathSegments.toTypedArray(),
                        queryParams = route.queryParams
                    )
                } else {
                    appConfig.buildApiUrl(*route.pathSegments.toTypedArray())
                }

                Timber.d("Bookings: trying GET ${url}")

                when (val result = apiClient.get(url, includeAuth = true)) {
                    is ApiResult.Success -> {
                        val bookings = withContext(Dispatchers.Default) {
                            parseBookingsListResponse(result.data)
                        }
                        Timber.d("Bookings: decoded ${bookings.size} bookings from ${route.pathSegments.joinToString("/")}")

                        if (bookings.isNotEmpty()) {
                            // Cache to Room
                            cacheBookingsToRoom(bookings)
                            return Result.Success(bookings)
                        }

                        // Got 0 bookings — keep as best but try next route
                        if (bestResult == null) bestResult = bookings

                        if (index == routes.lastIndex) {
                            return Result.Success(bestResult ?: emptyList())
                        }
                    }
                    is ApiResult.Failure -> {
                        Timber.w("Bookings: ${route.pathSegments.joinToString("/")} failed: ${result.error.message}")
                        lastError = Exception(result.error.message)
                    }
                }
            } catch (e: Exception) {
                Timber.w(e, "Bookings: ${route.pathSegments.joinToString("/")} failed")
                lastError = e
            }
        }

        // All routes failed — return best empty result or error
        bestResult?.let { return Result.Success(it) }
        return Result.Error(lastError ?: Exception("Failed to fetch bookings"))
    }

    fun getBookingsByStatus(status: String): Flow<Result<List<BookingModel>>> {
        return bookingDao.getBookingsByStatus(status).map { entities ->
            Result.Success(entities.map { it.asExternalModel() })
        }
    }

    fun getAllBookings(): Flow<Result<List<BookingModel>>> {
        return bookingDao.getAllBookings().map { entities ->
            Result.Success(entities.map { it.asExternalModel() })
        }
    }

    suspend fun createBooking(booking: BookingModel): Result<BookingModel> {
        return try {
            val response = apiService.createBooking(booking)
            if (response.isSuccessful) {
                // Use the server response to get the real booking ID and status
                val serverBooking = response.body()?.data?.let { serverData ->
                    booking.copy(
                        id = serverData.id.ifBlank { booking.id },
                        bookingStatus = serverData.status.ifBlank { booking.bookingStatus }
                    )
                } ?: booking
                serverBooking.asEntity()?.let { bookingDao.insertBooking(it) }
                Result.Success(serverBooking)
            } else {
                // Parse error body for security-related responses
                val errorBody = response.errorBody()?.string()
                val securityError = if (errorBody != null) {
                    com.shourov.apps.pacedream.core.network.api.SecurityErrorHandler
                        .parseSecurityError(response.code(), errorBody, json)
                } else null

                val errorMsg = if (securityError != null) {
                    com.shourov.apps.pacedream.core.network.api.SecurityErrorHandler.getUserMessage(securityError)
                } else {
                    "We couldn't complete your booking. Please try again."
                }
                Result.Error(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    suspend fun updateBooking(booking: BookingModel): Result<BookingModel> {
        return try {
            if (booking.id.isBlank()) return Result.Error(Exception("Booking ID is required"))
            val response = apiService.updateBooking(booking.id, booking)
            if (response.isSuccessful) {
                booking.asEntity()?.let { bookingDao.updateBooking(it) }
                Result.Success(booking)
            } else {
                Result.Error(Exception("We couldn't save your changes. Please try again."))
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    suspend fun cancelBooking(bookingId: String): Result<Unit> {
        return try {
            val response = apiService.cancelBooking(bookingId)
            if (response.isSuccessful) {
                bookingDao.updateBookingStatus(bookingId, "CANCELLED")
                Result.Success(Unit)
            } else {
                Result.Error(Exception("We couldn't cancel this booking. Please try again."))
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    suspend fun confirmBooking(bookingId: String): Result<Unit> {
        return try {
            val response = apiService.confirmBooking(bookingId)
            if (response.isSuccessful) {
                bookingDao.updateBookingStatus(bookingId, "CONFIRMED")
                Result.Success(Unit)
            } else {
                Result.Error(Exception("We couldn't confirm this booking. Please try again."))
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    /**
     * Save a booking model directly to Room cache.
     * Used after booking confirmation to ensure the booking is available for detail view.
     */
    suspend fun cacheBooking(booking: BookingModel) {
        booking.asEntity()?.let { entity ->
            try {
                bookingDao.insertBooking(entity)
            } catch (e: Exception) {
                Timber.w(e, "Failed to cache booking to Room")
            }
        }
    }

    // ── Availability Check (backend source of truth) ──────────

    /**
     * Check if a specific time range is available for booking on a listing.
     * Calls POST /v1/listings/:id/check-availability (backend source of truth).
     *
     * Returns a [AvailabilityCheckResult] with available status and reason if unavailable.
     * Must be called BEFORE creating a booking to prevent invalid/overlapping bookings.
     */
    suspend fun checkAvailability(
        listingId: String,
        startDate: String,
        endDate: String
    ): Result<AvailabilityCheckResult> = withContext(Dispatchers.IO) {
        try {
            Timber.d("Checking availability: listing=%s %s to %s", listingId, startDate, endDate)
            val body = mapOf("startDate" to startDate, "endDate" to endDate)
            val response = apiService.checkListingAvailability(listingId, body)

            if (response.isSuccessful) {
                val jsonBody = response.body()
                if (jsonBody != null && jsonBody.isJsonObject) {
                    val root = jsonBody.asJsonObject
                    val data = root.getAsJsonObject("data")
                    val available = data?.get("available")?.asBoolean ?: false
                    val reason = data?.get("reason")?.let {
                        if (it.isJsonNull) null else it.asString
                    }
                    val listingInfo = data?.getAsJsonObject("listing")
                    val bookable = listingInfo?.get("bookable")?.asBoolean ?: true
                    val listingStatus = listingInfo?.get("status")?.asString
                    val timezone = listingInfo?.get("timezone")?.asString

                    Timber.d("Availability check result: available=%s reason=%s bookable=%s", available, reason, bookable)
                    Result.Success(
                        AvailabilityCheckResult(
                            available = available,
                            reason = reason,
                            listingBookable = bookable,
                            listingStatus = listingStatus,
                            listingTimezone = timezone
                        )
                    )
                } else {
                    Result.Error(Exception("We couldn't check availability right now. Please try again."))
                }
            } else {
                Timber.w("Availability check failed [%d]", response.code())
                Result.Error(Exception("We couldn't check availability right now. Please try again."))
            }
        } catch (e: Exception) {
            Timber.e(e, "Availability check exception")
            Result.Error(e)
        }
    }

    /**
     * Result of an availability check from the backend.
     * Maps directly to the backend check-availability response.
     */
    data class AvailabilityCheckResult(
        val available: Boolean,
        val reason: String?,
        val listingBookable: Boolean,
        val listingStatus: String?,
        val listingTimezone: String?
    ) {
        /**
         * User-friendly message for the unavailability reason.
         * Maps backend reason codes to display strings.
         */
        /**
         * User-friendly message for the unavailability reason.
         * Maps ALL backend reason codes from checkFullAvailability() to display strings.
         *
         * Backend reason codes (availabilityService.js):
         *   LISTING_NOT_FOUND, LISTING_DELETED, LISTING_SNOOZED, LISTING_ARCHIVED,
         *   LISTING_HIDDEN, LISTING_STATUS_NOT_BOOKABLE:{status},
         *   MODERATION_STATUS_NOT_BOOKABLE:{status}, INVALID_DATE_FORMAT,
         *   END_BEFORE_START, START_IN_PAST, DURATION_TOO_SHORT:min={X}m,
         *   DURATION_TOO_LONG:max={X}m, BEFORE_AVAILABILITY_START,
         *   AFTER_AVAILABILITY_END, BLOCKED_PERIOD:{dates}, DAY_NOT_AVAILABLE:{date},
         *   BOOKING_CONFLICT, HOLD_CONFLICT
         */
        val displayReason: String get() = when {
            available -> ""
            reason == null -> "This time is not available"
            reason == "BOOKING_CONFLICT" -> "This time overlaps with an existing booking"
            reason == "HOLD_CONFLICT" -> "This time is being held for another checkout"
            reason == "LISTING_NOT_FOUND" -> "This listing could not be found"
            reason == "LISTING_SNOOZED" -> "This listing is currently paused"
            reason == "LISTING_ARCHIVED" -> "This listing is no longer available"
            reason == "LISTING_DELETED" -> "This listing has been removed"
            reason == "LISTING_HIDDEN" -> "This listing is not currently visible"
            reason == "INVALID_DATE_FORMAT" -> "Invalid date format"
            reason == "END_BEFORE_START" -> "End time must be after start time"
            reason == "START_IN_PAST" -> "Cannot book in the past"
            reason == "BEFORE_AVAILABILITY_START" -> "This date is before the listing's available dates"
            reason == "AFTER_AVAILABILITY_END" -> "This date is after the listing's available dates"
            reason.startsWith("DURATION_TOO_SHORT") -> "Booking duration is too short (minimum 15 minutes)"
            reason.startsWith("DURATION_TOO_LONG") -> "Booking duration is too long (maximum 30 days)"
            reason.startsWith("BLOCKED_PERIOD") -> "This time is blocked by the host"
            reason.startsWith("DAY_NOT_AVAILABLE") -> "This day is not available for booking"
            reason.startsWith("LISTING_STATUS_NOT_BOOKABLE") -> "This listing is not currently accepting bookings"
            reason.startsWith("MODERATION_STATUS_NOT_BOOKABLE") -> "This listing is pending moderation review"
            !listingBookable -> "This listing is not available for booking"
            else -> "This time is not available"
        }
    }

    // ── Private API helpers ─────────────────────────────────

    /**
     * Fetch a single booking from API by ID.
     * Handles multiple response shapes (iOS parity):
     *   - Direct BookingModel object
     *   - { booking: BookingModel }
     *   - { data: BookingModel }
     *   - { data: { booking: BookingModel } }
     */
    private suspend fun fetchBookingByIdFromApi(bookingId: String): BookingModel? {
        val url = appConfig.buildApiUrl("bookings", bookingId)

        return when (val result = apiClient.get(url, includeAuth = true)) {
            is ApiResult.Success -> {
                try {
                    withContext(Dispatchers.Default) {
                        parseBookingDetailResponse(result.data)
                    }
                } catch (e: Exception) {
                    Timber.e(e, "Failed to parse booking detail response")
                    null
                }
            }
            is ApiResult.Failure -> {
                Timber.w("Failed to fetch booking $bookingId from API: ${result.error.message}")
                null
            }
        }
    }

    /**
     * Parse a single booking from various response shapes.
     */
    private fun parseBookingDetailResponse(responseBody: String): BookingModel? {
        val root = json.parseToJsonElement(responseBody)
        if (root !is JsonObject) return null

        // Try: { data: { booking: {...} } }
        root["data"]?.let { dataElement ->
            if (dataElement is JsonObject) {
                dataElement["booking"]?.let { bookingElement ->
                    if (bookingElement is JsonObject) {
                        return parseBookingObject(bookingElement)
                    }
                }
                // Try: { data: {...} } (data IS the booking)
                if (looksLikeBooking(dataElement)) {
                    return parseBookingObject(dataElement)
                }
            }
        }

        // Try: { booking: {...} }
        root["booking"]?.let { bookingElement ->
            if (bookingElement is JsonObject) {
                return parseBookingObject(bookingElement)
            }
        }

        // Try: root IS the booking
        if (looksLikeBooking(root)) {
            return parseBookingObject(root)
        }

        return null
    }

    private fun looksLikeBooking(obj: JsonObject): Boolean {
        return obj.containsKey("_id") || obj.containsKey("bookingId") ||
            (obj.containsKey("id") && (obj.containsKey("status") || obj.containsKey("checkIn") || obj.containsKey("startDate")))
    }

    /**
     * Parse bookings list from various response shapes (matching iOS BookingMineResponse).
     * Handles: { bookings: [] }, { data: [] }, { data: { bookings: [] } }, { items: [] }, raw []
     */
    private fun parseBookingsListResponse(responseBody: String): List<BookingModel> {
        val root = json.parseToJsonElement(responseBody)
        val bookingsArray = findBookingsArray(root) ?: return emptyList()

        return bookingsArray.mapNotNull { element ->
            try {
                if (element is JsonObject) parseBookingObject(element) else null
            } catch (e: Exception) {
                Timber.w(e, "Failed to parse booking item in list")
                null
            }
        }
    }

    private fun findBookingsArray(root: JsonElement): List<JsonElement>? {
        return when (root) {
            is JsonArray -> root.toList()
            is JsonObject -> {
                for (key in listOf("bookings", "data", "items", "results")) {
                    val value = root[key]
                    when (value) {
                        is JsonArray -> return value.toList()
                        is JsonObject -> {
                            for (inner in listOf("bookings", "items", "data", "results")) {
                                value[inner]?.let { if (it is JsonArray) return it.toList() }
                            }
                        }
                        else -> Unit
                    }
                }
                null
            }
            else -> null
        }
    }

    /**
     * Tolerant parsing of a single booking JSON object into BookingModel.
     * Handles various field naming conventions from the backend.
     */
    private fun parseBookingObject(obj: JsonObject): BookingModel {
        val id = obj["_id"]?.jsonPrimitive?.content
            ?: obj["id"]?.jsonPrimitive?.content
            ?: obj["bookingId"]?.jsonPrimitive?.content
            ?: ""

        val propertyId = obj["propertyId"]?.jsonPrimitive?.content
            ?: obj["property_id"]?.jsonPrimitive?.content
            ?: obj["listingId"]?.jsonPrimitive?.content
            ?: obj["listing_id"]?.jsonPrimitive?.content
            ?: ""

        val propertyName = obj["propertyName"]?.jsonPrimitive?.content
            ?: obj["property_name"]?.jsonPrimitive?.content
            ?: obj["listingTitle"]?.jsonPrimitive?.content
            ?: obj["title"]?.jsonPrimitive?.content
            ?: extractNestedString(obj, "listing", "title")
            ?: extractNestedString(obj, "property", "title")
            ?: extractNestedString(obj, "listing", "name")
            ?: ""

        val propertyImage = obj["coverImage"]?.jsonPrimitive?.content
            ?: obj["image"]?.jsonPrimitive?.content
            ?: obj["thumbnail"]?.jsonPrimitive?.content
            ?: extractNestedString(obj, "listing", "coverImage")
            ?: extractNestedString(obj, "listing", "image")
            ?: extractNestedString(obj, "property", "image")

        val hostName = obj["hostName"]?.jsonPrimitive?.content
            ?: obj["host_name"]?.jsonPrimitive?.content
            ?: extractNestedString(obj, "host", "name")
            ?: extractNestedString(obj, "host", "displayName")
            ?: ""

        val startDate = obj["startDate"]?.jsonPrimitive?.content
            ?: obj["start_date"]?.jsonPrimitive?.content
            ?: obj["checkIn"]?.jsonPrimitive?.content
            ?: obj["check_in"]?.jsonPrimitive?.content
            ?: obj["start_time"]?.jsonPrimitive?.content
            ?: ""

        val endDate = obj["endDate"]?.jsonPrimitive?.content
            ?: obj["end_date"]?.jsonPrimitive?.content
            ?: obj["checkOut"]?.jsonPrimitive?.content
            ?: obj["check_out"]?.jsonPrimitive?.content
            ?: obj["end_time"]?.jsonPrimitive?.content
            ?: ""

        val totalPrice = obj["totalPrice"]?.jsonPrimitive?.doubleOrNull
            ?: obj["total_price"]?.jsonPrimitive?.doubleOrNull
            ?: obj["amount"]?.jsonPrimitive?.doubleOrNull
            ?: obj["price"]?.jsonPrimitive?.doubleOrNull
            ?: 0.0

        val currency = obj["currency"]?.jsonPrimitive?.content ?: "USD"

        val statusStr = obj["status"]?.jsonPrimitive?.content
            ?: obj["bookingStatus"]?.jsonPrimitive?.content
            ?: obj["booking_status"]?.jsonPrimitive?.content
            ?: "pending"

        val userName = obj["userName"]?.jsonPrimitive?.content
            ?: obj["guestName"]?.jsonPrimitive?.content
            ?: obj["guest_name"]?.jsonPrimitive?.content
            ?: extractNestedString(obj, "guest", "name")
            ?: extractNestedString(obj, "renter", "name")

        val guestCount = try {
            obj["guestCount"]?.jsonPrimitive?.int
                ?: obj["guest_count"]?.jsonPrimitive?.int
                ?: obj["guests"]?.jsonPrimitive?.int
                ?: obj["numberOfGuests"]?.jsonPrimitive?.int
                ?: 1
        } catch (e: Exception) {
            timber.log.Timber.w(e, "Failed to parse guest count, defaulting to 1")
            1
        }

        val createdAt = obj["createdAt"]?.jsonPrimitive?.content
            ?: obj["created_at"]?.jsonPrimitive?.content
            ?: ""

        // Optional payment breakdown — tolerant across snake_case / camelCase
        // and nested "priceBreakdown" / "fees" / "pricing" envelopes.  When
        // the response omits all of these we leave the fields null and the
        // UI falls back to the current Total-only render.  No contract is
        // invented: each field uses common existing aliases matching what
        // other PaceDream responses (checkout quote, listing pricing)
        // already emit.
        val breakdownObj = (obj["priceBreakdown"] as? JsonObject)
            ?: (obj["price_breakdown"] as? JsonObject)
            ?: (obj["pricing"] as? JsonObject)
            ?: (obj["fees"] as? JsonObject)

        fun findDouble(vararg keys: String): Double? {
            for (k in keys) {
                obj[k]?.jsonPrimitive?.doubleOrNull?.let { return it }
                breakdownObj?.get(k)?.jsonPrimitive?.doubleOrNull?.let { return it }
            }
            return null
        }

        val subtotal = findDouble(
            "subtotal", "subtotalAmount", "base_amount", "baseAmount", "basePrice", "base_price"
        )
        val serviceFee = findDouble(
            "serviceFee", "service_fee", "platformFee", "platform_fee", "serviceCharge", "service_charge"
        )
        val cleaningFee = findDouble(
            "cleaningFee", "cleaning_fee"
        )
        val taxAmount = findDouble(
            "taxAmount", "tax_amount", "tax", "taxes", "salesTax", "sales_tax"
        )

        return BookingModel(
            id = id,
            propertyId = propertyId,
            propertyName = propertyName,
            propertyImage = propertyImage,
            hostName = hostName,
            startDate = startDate,
            endDate = endDate,
            totalPrice = totalPrice,
            currency = currency,
            status = BookingStatus.fromString(statusStr),
            bookingStatus = statusStr,
            userName = userName,
            guestCount = guestCount,
            createdAt = createdAt,
            checkInTime = startDate,
            checkOutTime = endDate,
            price = totalPrice.toString(),
            subtotal = subtotal,
            serviceFee = serviceFee,
            cleaningFee = cleaningFee,
            taxAmount = taxAmount
        )
    }

    private fun extractNestedString(obj: JsonObject, parentKey: String, childKey: String): String? {
        return try {
            obj[parentKey]?.jsonObject?.get(childKey)?.jsonPrimitive?.content
        } catch (e: Exception) {
            null
        }
    }

    private suspend fun cacheBookingsToRoom(bookings: List<BookingModel>) {
        try {
            val entities = bookings.mapNotNull { it.asEntity() }
            if (entities.isNotEmpty()) {
                bookingDao.insertBookings(entities)
            }
        } catch (e: Exception) {
            Timber.w(e, "Failed to cache bookings to Room")
        }
    }
}
