package com.shourov.apps.pacedream.feature.booking.data

import com.shourov.apps.pacedream.core.network.api.ApiClient
import com.shourov.apps.pacedream.core.network.api.ApiError
import com.shourov.apps.pacedream.core.network.api.ApiResult
import com.shourov.apps.pacedream.core.network.config.AppConfig
import com.shourov.apps.pacedream.feature.booking.model.BookingItem
import com.shourov.apps.pacedream.feature.booking.model.BookingRole
import com.shourov.apps.pacedream.model.BookingStatus
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for Bookings tab data fetching with user/host role support.
 *
 * Matches the web platform pattern:
 *   GET /account/bookings?role=renter|host&limit=N&offset=N
 *
 * Tolerant parsing handles:
 *   - { data: [] }, { bookings: [] }, { items: [] }, raw []
 *   - Various field naming conventions from the backend
 */
@Singleton
class BookingTabRepository @Inject constructor(
    private val apiClient: ApiClient,
    private val appConfig: AppConfig,
    private val json: Json
) {

    /**
     * Fetch bookings for the authenticated user, scoped by [role].
     *
     * For RENTER role, uses iOS fallback chain if primary returns empty:
     *   1. GET /account/bookings?role=renter (web platform primary)
     *   2. GET /bookings/mine              (iOS primary)
     *   3. GET /guest/bookings             (iOS fallback)
     *   4. GET /account/bookings?role=guest (iOS fallback)
     *
     * For HOST role, uses the single endpoint:
     *   GET /account/bookings?role=host
     *
     * @param role   "renter" for guest/trips, "host" for hosting bookings
     * @param limit  page size
     * @param offset pagination offset
     */
    suspend fun getBookings(
        role: BookingRole,
        limit: Int = 20,
        offset: Int = 0
    ): ApiResult<BookingsResult> {
        // For host role, use single endpoint
        if (role == BookingRole.HOST) {
            return fetchFromUrl(
                appConfig.buildApiUrlWithQuery(
                    "account", "bookings",
                    queryParams = mapOf(
                        "role" to role.apiValue,
                        "limit" to limit.toString(),
                        "offset" to offset.toString()
                    )
                )
            )
        }

        // For renter role, try multiple routes (iOS parity)
        data class Route(val pathSegments: List<String>, val queryParams: Map<String, String>)

        val routes = listOf(
            Route(listOf("account", "bookings"), mapOf("role" to "renter", "limit" to limit.toString(), "offset" to offset.toString())),
            Route(listOf("bookings", "mine"), mapOf("limit" to limit.toString(), "offset" to offset.toString())),
            Route(listOf("guest", "bookings"), mapOf("limit" to limit.toString(), "offset" to offset.toString())),
            Route(listOf("account", "bookings"), mapOf("role" to "guest", "limit" to limit.toString(), "offset" to offset.toString()))
        )

        var bestResult: BookingsResult? = null
        var lastError: ApiResult.Failure<BookingsResult>? = null

        for ((index, route) in routes.withIndex()) {
            val url = appConfig.buildApiUrlWithQuery(
                *route.pathSegments.toTypedArray(),
                queryParams = route.queryParams
            )

            Timber.d("BookingTab: trying ${route.pathSegments.joinToString("/")} (attempt ${index + 1}/${routes.size})")

            when (val result = fetchFromUrl(url)) {
                is ApiResult.Success -> {
                    if (result.data.bookings.isNotEmpty()) {
                        return result
                    }
                    // Got 0 bookings, keep as best but try next route
                    if (bestResult == null) bestResult = result.data
                }
                is ApiResult.Failure -> {
                    Timber.w("BookingTab: ${route.pathSegments.joinToString("/")} failed: ${result.error.message}")
                    lastError = result
                }
            }
        }

        // Return best empty result or last error
        bestResult?.let { return ApiResult.Success(it) }
        return lastError ?: ApiResult.Failure(ApiError.ServerError("Failed to load bookings"))
    }

    private suspend fun fetchFromUrl(url: okhttp3.HttpUrl): ApiResult<BookingsResult> {
        return when (val result = apiClient.get(url, includeAuth = true)) {
            is ApiResult.Success -> {
                try {
                    val bookingsResult = parseBookingsResponse(result.data)
                    ApiResult.Success(bookingsResult)
                } catch (e: Exception) {
                    Timber.e(e, "Failed to parse bookings response")
                    ApiResult.Failure(ApiError.DecodingError("Failed to parse bookings", e))
                }
            }
            is ApiResult.Failure -> result
        }
    }

    /**
     * Parse bookings response with tolerant path finding.
     * Handles: { data: [] }, { bookings: [] }, { items: [] }, raw array, nested wrappers.
     */
    private fun parseBookingsResponse(responseBody: String): BookingsResult {
        val root = json.parseToJsonElement(responseBody)

        val bookingsArray = findBookingsArray(root)

        val bookings = bookingsArray?.mapNotNull { element ->
            try {
                parseBookingItem(element.jsonObject)
            } catch (e: Exception) {
                Timber.w(e, "Failed to parse booking item")
                null
            }
        } ?: emptyList()

        // Try to extract pagination info
        val total = when (root) {
            is JsonObject -> {
                val data = root["data"]?.jsonObject ?: root
                data["total"]?.jsonPrimitive?.int
                    ?: data["totalCount"]?.jsonPrimitive?.int
                    ?: data["count"]?.jsonPrimitive?.int
            }
            else -> null
        }

        val hasMore = when (root) {
            is JsonObject -> {
                val data = root["data"]?.jsonObject ?: root
                data["hasMore"]?.jsonPrimitive?.boolean
                    ?: (total != null && bookings.size < total)
            }
            else -> false
        }

        return BookingsResult(
            bookings = bookings,
            total = total,
            hasMore = hasMore
        )
    }

    private fun findBookingsArray(root: JsonElement): List<JsonElement>? {
        return when (root) {
            is JsonArray -> root.toList()
            is JsonObject -> {
                // Try common wrapper keys
                for (key in listOf("bookings", "data", "items", "results")) {
                    val value = root[key]
                    when (value) {
                        is JsonArray -> return value.toList()
                        is JsonObject -> {
                            // Nested: e.g. { data: { bookings: [] } }
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

    private fun parseBookingItem(obj: JsonObject): BookingItem {
        val id = obj["_id"]?.jsonPrimitive?.content
            ?: obj["id"]?.jsonPrimitive?.content
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
            ?: extractNestedFirstImage(obj, "listing", "images")
            ?: extractNestedFirstImage(obj, "listing", "galleryImages")

        val location = obj["location"]?.jsonPrimitive?.content
            ?: extractNestedString(obj, "listing", "city")
            ?: extractNestedString(obj, "listing", "location")
            ?: extractNestedString(obj, "property", "city")

        val hostName = obj["hostName"]?.jsonPrimitive?.content
            ?: obj["host_name"]?.jsonPrimitive?.content
            ?: extractNestedString(obj, "host", "name")
            ?: extractNestedString(obj, "host", "displayName")
            ?: ""

        val guestName = obj["guestName"]?.jsonPrimitive?.content
            ?: obj["guest_name"]?.jsonPrimitive?.content
            ?: extractNestedString(obj, "guest", "name")
            ?: extractNestedString(obj, "guest", "displayName")
            ?: extractNestedString(obj, "renter", "name")
            ?: ""

        val startDate = obj["startDate"]?.jsonPrimitive?.content
            ?: obj["start_date"]?.jsonPrimitive?.content
            ?: obj["checkIn"]?.jsonPrimitive?.content
            ?: obj["check_in"]?.jsonPrimitive?.content
            ?: obj["from"]?.jsonPrimitive?.content
            ?: ""

        val endDate = obj["endDate"]?.jsonPrimitive?.content
            ?: obj["end_date"]?.jsonPrimitive?.content
            ?: obj["checkOut"]?.jsonPrimitive?.content
            ?: obj["check_out"]?.jsonPrimitive?.content
            ?: obj["to"]?.jsonPrimitive?.content
            ?: ""

        val totalPrice = obj["totalPrice"]?.jsonPrimitive?.doubleOrNull
            ?: obj["total_price"]?.jsonPrimitive?.doubleOrNull
            ?: obj["amount"]?.jsonPrimitive?.doubleOrNull
            ?: obj["price"]?.jsonPrimitive?.doubleOrNull
            ?: 0.0

        val currency = obj["currency"]?.jsonPrimitive?.content
            ?: "USD"

        val statusStr = obj["status"]?.jsonPrimitive?.content
            ?: obj["bookingStatus"]?.jsonPrimitive?.content
            ?: obj["booking_status"]?.jsonPrimitive?.content
            ?: "pending"

        val guestCount = obj["guestCount"]?.jsonPrimitive?.int
            ?: obj["guest_count"]?.jsonPrimitive?.int
            ?: obj["guests"]?.jsonPrimitive?.int
            ?: obj["numberOfGuests"]?.jsonPrimitive?.int
            ?: 1

        val createdAt = obj["createdAt"]?.jsonPrimitive?.content
            ?: obj["created_at"]?.jsonPrimitive?.content

        return BookingItem(
            id = id,
            propertyId = propertyId,
            propertyName = propertyName,
            propertyImage = propertyImage,
            location = location,
            hostName = hostName,
            guestName = guestName,
            startDate = startDate,
            endDate = endDate,
            totalPrice = totalPrice,
            currency = currency,
            status = BookingStatus.fromString(statusStr),
            guestCount = guestCount,
            createdAt = createdAt
        )
    }

    private fun extractNestedString(obj: JsonObject, parentKey: String, childKey: String): String? {
        return try {
            obj[parentKey]?.jsonObject?.get(childKey)?.jsonPrimitive?.content
        } catch (e: Exception) {
            null
        }
    }

    private fun extractNestedFirstImage(obj: JsonObject, parentKey: String, arrayKey: String): String? {
        return try {
            obj[parentKey]?.jsonObject?.get(arrayKey)?.jsonArray?.firstOrNull()?.jsonPrimitive?.content
        } catch (e: Exception) {
            null
        }
    }
}

/**
 * Result of a bookings fetch
 */
data class BookingsResult(
    val bookings: List<BookingItem>,
    val total: Int?,
    val hasMore: Boolean
)
