package com.shourov.apps.pacedream.feature.host.data

import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.reflect.TypeToken
import com.shourov.apps.pacedream.model.Property
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Host Repository - iOS parity.
 *
 * Matches iOS HostDataStore + HostDashboardService + HostBookingsService + PayoutsService
 * with concurrent loading and partial-success handling.
 */
@Singleton
class HostRepository @Inject constructor(
    private val hostApiService: HostApiService
) {

    // ── Dashboard: concurrent load (iOS: HostDataStore.refresh + HostDashboardViewModel.load) ──

    data class DashboardLoadResult(
        val bookings: List<HostBookingDTO>,
        val listings: List<Property>,
        val overview: HostDashboardOverviewResponse?,
        val payoutState: PayoutConnectionState,
        val payoutEligibility: PayoutSetupEligibilityResponse?,
        val hadPartialFailure: Boolean,
        val errorMessage: String?,
        val hasLoaded: Boolean = true
    )

    suspend fun loadDashboard(): DashboardLoadResult = coroutineScope {
        var hadFailure = false
        Timber.d("Loading host dashboard: bookings, listings, overview, payout state, eligibility")

        // Track whether we got non-host responses (401, 403, 404 with "not found")
        // These are expected for users who haven't created a host profile yet.
        var gotNonHostResponse = false

        /** Returns true if the HTTP code indicates a non-host account (not a real failure). */
        fun isNonHostCode(code: Int): Boolean = code == 401 || code == 403 || code == 404

        val bookingsDeferred = async {
            try {
                Timber.d("Fetching host bookings from /bookings/host")
                val response = hostApiService.getHostBookings()
                if (response.isSuccessful) {
                    val bookings = response.body()?.bookings ?: emptyList()
                    Timber.d("Loaded ${bookings.size} host bookings")
                    bookings
                } else {
                    if (isNonHostCode(response.code())) {
                        Timber.d("Host bookings returned ${response.code()} — user may not have a host profile")
                        gotNonHostResponse = true
                    } else {
                        Timber.w("Host bookings failed [${response.code()}]")
                        hadFailure = true
                    }
                    emptyList()
                }
            } catch (e: Exception) {
                Timber.e(e, "Host bookings fetch exception")
                hadFailure = true
                emptyList<HostBookingDTO>()
            }
        }

        val listingsDeferred = async {
            try {
                Timber.d("Fetching host listings from /host/listings")
                val response = hostApiService.getHostListings()
                if (response.isSuccessful) {
                    val json = response.body()
                    val listings = if (json != null) parseHostListings(json) else emptyList()
                    Timber.d("Loaded ${listings.size} host listings")
                    listings
                } else {
                    if (isNonHostCode(response.code())) {
                        Timber.d("Host listings returned ${response.code()} — user may not have a host profile")
                        gotNonHostResponse = true
                    } else {
                        Timber.w("Host listings failed [${response.code()}]")
                        hadFailure = true
                    }
                    emptyList()
                }
            } catch (e: Exception) {
                Timber.e(e, "Host listings fetch exception")
                hadFailure = true
                emptyList<Property>()
            }
        }

        val overviewDeferred = async {
            try {
                Timber.d("Fetching dashboard overview from /hosts/dashboard/overview")
                val response = hostApiService.getDashboardOverview()
                if (response.isSuccessful) {
                    Timber.d("Dashboard overview loaded")
                    response.body()
                } else {
                    if (isNonHostCode(response.code())) {
                        Timber.d("Dashboard overview returned ${response.code()} — non-host account")
                        gotNonHostResponse = true
                    } else {
                        Timber.w("Dashboard overview failed [${response.code()}]")
                        hadFailure = true
                    }
                    null
                }
            } catch (e: Exception) {
                Timber.e(e, "Dashboard overview fetch exception")
                hadFailure = true
                null
            }
        }

        val payoutDeferred = async {
            try {
                Timber.d("Resolving payout state from /host/payouts/status")
                resolvePayoutState()
            } catch (e: Exception) {
                Timber.e(e, "Payout state resolution exception")
                hadFailure = true
                PayoutConnectionState.NOT_CONNECTED
            }
        }

        val eligibilityDeferred = async {
            try {
                getPayoutSetupEligibility()
            } catch (e: Exception) {
                Timber.e(e, "Failed to load payout eligibility")
                null
            }
        }

        val bookings = bookingsDeferred.await()
        val listings = listingsDeferred.await()
        val overview = overviewDeferred.await()
        val payoutState = payoutDeferred.await()
        val payoutEligibility = eligibilityDeferred.await()

        // iOS parity: 401/403/404 on host-only endpoints for users without a host profile
        // is NOT a failure. Treat as empty data with no error message — matches iOS behavior
        // where non-host users simply see empty state, not error banners.
        val primaryDataFailed = hadFailure && bookings.isEmpty() && listings.isEmpty()
        val errorMessage = when {
            gotNonHostResponse && !hadFailure && bookings.isEmpty() && listings.isEmpty() ->
                null  // Non-host user: show empty state, not error
            primaryDataFailed ->
                "Couldn't load host data. Pull to refresh."
            hadFailure ->
                "Some data couldn't load. Pull to refresh."
            else -> null
        }

        DashboardLoadResult(
            bookings = bookings,
            listings = listings,
            overview = overview,
            payoutState = payoutState,
            payoutEligibility = payoutEligibility,
            hadPartialFailure = hadFailure,
            errorMessage = errorMessage,
            hasLoaded = true
        )
    }

    // ── Bookings (iOS: HostBookingsService) ─────────────────────

    suspend fun getHostBookings(): Result<List<HostBookingDTO>> {
        return try {
            val response = hostApiService.getHostBookings()
            if (response.isSuccessful) {
                Result.success(response.body()?.bookings ?: emptyList())
            } else {
                Result.failure(Exception("Failed to fetch bookings: ${response.code()}"))
            }
        } catch (e: Exception) {
            Timber.e(e, "Host bookings fetch failed")
            Result.failure(e)
        }
    }

    suspend fun updateBookingStatus(id: String, status: String, reason: String? = null): Result<HostBookingDTO> {
        return try {
            val response = hostApiService.updateHostBooking(id, BookingStatusUpdate(status, reason))
            if (response.isSuccessful) {
                val body = response.body()
                val booking = body?.booking
                if (booking != null) {
                    Result.success(booking)
                } else {
                    Result.failure(Exception(body?.error ?: "Couldn't update booking."))
                }
            } else {
                Result.failure(Exception("Failed to update booking status: ${response.code()}"))
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to update booking status for id=$id to status=$status")
            Result.failure(e)
        }
    }

    suspend fun cancelBooking(id: String, reason: String? = null): Result<HostBookingDTO> {
        return updateBookingStatus(id, "cancelled", reason)
    }

    suspend fun acceptBooking(id: String): Result<HostBookingDTO> {
        return updateBookingStatus(id, "accepted")
    }

    suspend fun declineBooking(id: String): Result<HostBookingDTO> {
        return updateBookingStatus(id, "declined")
    }

    // ── Listings ────────────────────────────────────────────────

    suspend fun getHostListings(filter: String? = null, sort: String? = null): Result<List<Property>> {
        return try {
            val response = hostApiService.getHostListings(filter, sort)
            if (response.isSuccessful) {
                val json = response.body()
                Result.success(if (json != null) parseHostListings(json) else emptyList())
            } else {
                Result.failure(Exception("Failed to fetch listings: ${response.code()}"))
            }
        } catch (e: Exception) {
            Timber.e(e, "Host listings fetch failed")
            Result.failure(e)
        }
    }

    suspend fun createListing(request: CreateListingRequest): Result<Property> {
        return try {
            Timber.d(
                "createListing: POST /listings — type=%s sub=%s title='%s' price=%.2f pricing_type=%s " +
                    "images=%d address='%s' available=%s availability=%s",
                request.listing_type, request.subCategory, request.title, request.price,
                request.pricing_type, request.images?.size ?: 0, request.address ?: "",
                request.available, request.availability != null,
            )
            // Build request body as Map to guarantee field names survive serialization.
            // Gson data class serialization can drop fields in release builds (R8 obfuscation)
            // or with certain Kotlin data class configurations.
            val body = buildCreateListingBody(request)
            Timber.d("createListing body keys: %s", body.keys.joinToString())
            val response = hostApiService.createListing(body)
            if (response.isSuccessful) {
                val json = response.body()
                if (json != null) {
                    val property = parseCreateListingResponse(json, request)
                    Timber.d("createListing success: id=%s", property.id)
                    Result.success(property)
                } else {
                    Result.success(Property(id = "", title = request.title))
                }
            } else {
                val errorBody = response.errorBody()?.string()
                Timber.w(
                    "createListing failed: HTTP %d — %s",
                    response.code(), errorBody,
                )
                val errorMsg = try {
                    val json = Json.parseToJsonElement(errorBody ?: "")
                    json.jsonObject["message"]?.jsonPrimitive?.content
                        ?: json.jsonObject["error"]?.jsonPrimitive?.content
                        ?: json.jsonObject["details"]?.jsonPrimitive?.content
                        ?: json.jsonObject["data"]?.let { data ->
                            try {
                                data.jsonObject["message"]?.jsonPrimitive?.content
                                    ?: data.jsonObject["error"]?.jsonPrimitive?.content
                            } catch (_: Exception) { null }
                        }
                } catch (_: Exception) { null }
                Result.failure(Exception(errorMsg ?: "Failed to create listing (HTTP ${response.code()}). Please check all fields and try again."))
            }
        } catch (e: Exception) {
            Timber.e(e, "createListing failed with exception")
            Result.failure(e)
        }
    }

    /**
     * Parse the create listing response which is wrapped by the backend:
     * { action, code, status, data: { listing, listingId, id, _id, ... }, message }
     *
     * iOS parity: tries multiple JSON paths for the listing ID:
     * _id, id, listingId, data._id, data.id, data.listingId,
     * data.listing._id, data.listing.id, listing._id, listing.id
     */
    private fun parseCreateListingResponse(json: JsonElement, request: CreateListingRequest): Property {
        if (!json.isJsonObject) {
            Timber.w("createListing response is not a JSON object, returning fallback")
            return Property(id = "", title = request.title)
        }

        val root = json.asJsonObject

        // Try to extract listing ID from multiple paths (iOS parity)
        val listingId = extractIdFromPaths(root)
        val title = extractTitleFromPaths(root) ?: request.title

        Timber.d("createListing parsed: id=$listingId, title=$title")
        return Property(id = listingId, title = title)
    }

    /**
     * Extract listing ID from backend response trying multiple JSON paths.
     * Backend wraps response: { data: { listingId, id, _id, listing: { _id, id } } }
     */
    private fun extractIdFromPaths(root: JsonObject): String {
        // Top-level ID fields
        val topLevel = root.getStringOrNull("id")
            ?: root.getStringOrNull("_id")
            ?: root.getStringOrNull("listingId")
        if (!topLevel.isNullOrBlank()) return topLevel

        // Inside "data" wrapper
        val data = root.getAsJsonObject("data")
        if (data != null) {
            val dataId = data.getStringOrNull("id")
                ?: data.getStringOrNull("_id")
                ?: data.getStringOrNull("listingId")
            if (!dataId.isNullOrBlank()) return dataId

            // Inside "data.listing"
            val listing = data.getAsJsonObject("listing")
            if (listing != null) {
                val listingId = listing.getStringOrNull("id")
                    ?: listing.getStringOrNull("_id")
                if (!listingId.isNullOrBlank()) return listingId
            }
        }

        // Inside top-level "listing"
        val listing = root.getAsJsonObject("listing")
        if (listing != null) {
            val listingId = listing.getStringOrNull("id")
                ?: listing.getStringOrNull("_id")
            if (!listingId.isNullOrBlank()) return listingId
        }

        return ""
    }

    private fun extractTitleFromPaths(root: JsonObject): String? {
        root.getStringOrNull("title")?.let { return it }
        root.getAsJsonObject("data")?.let { data ->
            data.getStringOrNull("title")?.let { return it }
            data.getAsJsonObject("listing")?.getStringOrNull("title")?.let { return it }
        }
        root.getAsJsonObject("listing")?.getStringOrNull("title")?.let { return it }
        return null
    }

    private fun JsonObject.getStringOrNull(key: String): String? {
        val el = get(key) ?: return null
        return if (el.isJsonPrimitive && el.asJsonPrimitive.isString) el.asString else null
    }

    private fun JsonObject.getAsJsonObject(key: String): JsonObject? {
        val el = get(key) ?: return null
        return if (el.isJsonObject) el.asJsonObject else null
    }

    suspend fun updateListing(id: String, listing: Property): Result<Property> {
        return try {
            val response = hostApiService.updateListing(id, listing)
            if (response.isSuccessful) {
                Result.success(response.body() ?: listing)
            } else {
                Result.failure(Exception("Failed to update listing: ${response.code()}"))
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to update listing id=$id")
            Result.failure(e)
        }
    }

    suspend fun deleteListing(id: String): Result<Unit> {
        return try {
            val response = hostApiService.deleteListing(id)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to delete listing: ${response.code()}"))
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to delete listing id=$id")
            Result.failure(e)
        }
    }

    // ── Payouts / Stripe Connect (iOS: PayoutsService) ──────────

    suspend fun resolvePayoutState(): PayoutConnectionState {
        val response = try {
            hostApiService.getPayoutStatus()
        } catch (e: Exception) {
            Timber.e(e, "resolvePayoutState: failed to fetch payout status")
            return PayoutConnectionState.NOT_CONNECTED
        }
        if (!response.isSuccessful) return PayoutConnectionState.NOT_CONNECTED

        val body = response.body() ?: return PayoutConnectionState.NOT_CONNECTED
        val raw = (body.status.ifEmpty { body.payoutStatus ?: "" }).lowercase()
        val chargesEnabled = body.resolvedChargesEnabled
        val payoutsEnabled = body.resolvedPayoutsEnabled
        val detailsSubmitted = body.resolvedDetailsSubmitted

        return when {
            (raw == "active" || raw.contains("connect")) && payoutsEnabled ->
                PayoutConnectionState.CONNECTED
            raw.contains("pending") || raw.contains("action") ||
                (detailsSubmitted && !payoutsEnabled) || chargesEnabled || detailsSubmitted ->
                PayoutConnectionState.PENDING
            else ->
                PayoutConnectionState.NOT_CONNECTED
        }
    }

    suspend fun getPayoutStatus(): Result<PayoutStatusResponse> {
        return try {
            val response = hostApiService.getPayoutStatus()
            if (response.isSuccessful) {
                Result.success(response.body() ?: PayoutStatusResponse())
            } else {
                Result.failure(Exception("Failed to fetch payout status: ${response.code()}"))
            }
        } catch (e: Exception) {
            Timber.e(e, "Payout status fetch failed")
            Result.failure(e)
        }
    }

    /**
     * Check payout setup eligibility from the backend (server-driven).
     * Returns whether the current authenticated user should see payout setup prompts.
     * If the request fails (e.g. not authenticated), defaults to NOT showing the prompt.
     */
    suspend fun getPayoutSetupEligibility(): PayoutSetupEligibilityResponse {
        return try {
            val response = hostApiService.getPayoutSetupEligibility()
            if (response.isSuccessful) {
                response.body() ?: PayoutSetupEligibilityResponse()
            } else {
                Timber.d("Payout eligibility check failed: ${response.code()}")
                PayoutSetupEligibilityResponse()
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to check payout setup eligibility")
            PayoutSetupEligibilityResponse()
        }
    }

    suspend fun createOnboardingLink(): Result<String> {
        return try {
            val response = hostApiService.createOnboardingLink()
            if (response.isSuccessful) {
                val url = response.body()?.resolvedUrl
                if (url != null) Result.success(url)
                else Result.failure(Exception("Missing URL in response"))
            } else {
                Result.failure(Exception("Failed to create onboarding link: ${response.code()}"))
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to create onboarding link")
            Result.failure(e)
        }
    }

    suspend fun createLoginLink(): Result<String> {
        return try {
            val response = hostApiService.createLoginLink()
            if (response.isSuccessful) {
                val url = response.body()?.resolvedUrl
                if (url != null) Result.success(url)
                else Result.failure(Exception("Missing URL in response"))
            } else {
                Result.failure(Exception("Failed to create login link: ${response.code()}"))
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to create login link")
            Result.failure(e)
        }
    }

    suspend fun getPayoutMethods(): Result<List<PayoutMethod>> {
        return try {
            val response = hostApiService.getPayoutMethods()
            if (response.isSuccessful) {
                val methods = response.body()?.resolvedMethods?.map { m ->
                    PayoutMethod(
                        id = m.resolvedId,
                        type = m.resolvedType,
                        label = m.resolvedLabel,
                        isPrimary = m.resolvedIsPrimary
                    )
                } ?: emptyList()
                Result.success(methods)
            } else {
                Result.success(emptyList())
            }
        } catch (e: Exception) {
            Timber.w(e, "Payout methods fetch failed, returning empty list")
            Result.success(emptyList())
        }
    }

    // ── Flexible listings parser (iOS parity: HostListingsService.parseHostListings) ──

    private val gson = Gson()
    private val propertyListType = object : TypeToken<List<Property>>() {}.type

    /**
     * Parses host listings from multiple backend response shapes:
     * - Raw JSON array: [...]
     * - Wrapped: { data: [...] }, { items: [...] }, { results: [...] }, { listings: [...] }
     * - Category-keyed: { rooms: [...], properties: [...], rentableItems: [...], ... }
     */
    private fun parseHostListings(json: JsonElement): List<Property> {
        // Shape 1: raw array
        if (json.isJsonArray) {
            return parsePropertyArray(json.asJsonArray)
        }

        // Shape 2: wrapped object
        if (json.isJsonObject) {
            val obj = json.asJsonObject

            // Unwrap common wrapper keys
            val unwrapped = obj.get("data") ?: obj.get("items") ?: obj.get("results")
            if (unwrapped != null && unwrapped.isJsonArray) {
                return parsePropertyArray(unwrapped.asJsonArray)
            }

            // "listings" key
            val listings = obj.get("listings")
            if (listings != null && listings.isJsonArray) {
                return parsePropertyArray(listings.asJsonArray)
            }

            // Category-keyed (iOS parity)
            val categoryKeys = listOf("rooms", "properties", "rentableItems", "services", "attractions", "roommates")
            val all = mutableListOf<Property>()
            val seenIds = mutableSetOf<String>()
            for (key in categoryKeys) {
                val arr = obj.get(key)
                if (arr != null && arr.isJsonArray) {
                    for (p in parsePropertyArray(arr.asJsonArray)) {
                        if (p.id.isNotEmpty() && seenIds.add(p.id)) {
                            all.add(p)
                        }
                    }
                }
            }
            if (all.isNotEmpty()) return all
        }

        return emptyList()
    }

    private fun parsePropertyArray(arr: JsonArray): List<Property> {
        return try {
            gson.fromJson<List<Property>>(arr, propertyListType) ?: emptyList()
        } catch (e: Exception) {
            Timber.e(e, "Gson parsing failed for listings array, trying element-by-element")
            arr.mapNotNull { element ->
                try {
                    gson.fromJson(element, Property::class.java)
                } catch (_: Exception) {
                    null
                }
            }
        }
    }

    // ── Listing body builder (explicit Map to bypass Gson data-class issues) ──

    private fun buildCreateListingBody(r: CreateListingRequest): Map<String, Any> {
        val body = mutableMapOf<String, Any>(
            "listing_type" to r.listing_type,
            "subCategory" to r.subCategory,
            "title" to r.title,
            "price" to r.price,
            "pricing_type" to r.pricing_type,
            "available" to r.available,
        )

        // Always include description & summary (backend RentableItem requires summary)
        body["description"] = r.description
        body["summary"] = r.summary.ifBlank { r.title }

        // Pricing object
        body["pricing"] = mapOf(
            "base_price" to r.pricing.base_price,
            "unit" to r.pricing.unit,
            "currency" to r.pricing.currency,
        )

        // Optional fields — only include when set
        r.prices?.let { body["prices"] = it }
        r.address?.let { body["address"] = it }
        r.amenities?.let { body["amenities"] = it }
        r.details?.let {
            body["details"] = mapOf(
                "features" to it.features,
                "reviewCount" to it.reviewCount,
            )
        }
        r.images?.let { body["images"] = it }
        r.location?.let {
            body["location"] = mutableMapOf<String, Any>(
                "street" to it.street,
                "street_address" to it.street_address,
                "city" to it.city,
                "state" to it.state,
                "country" to it.country,
            ).apply {
                it.latitude?.let { lat -> put("latitude", lat) }
                it.longitude?.let { lng -> put("longitude", lng) }
            }
        }
        r.durations?.let { body["durations"] = it }
        r.minStay?.let { body["minStay"] = it }
        r.maxStay?.let { body["maxStay"] = it }
        r.minMonths?.let { body["minMonths"] = it }
        r.availableFrom?.let { body["availableFrom"] = it }
        r.availability?.let {
            body["availability"] = mutableMapOf<String, Any>(
                "start_time" to it.start_time,
                "end_time" to it.end_time,
                "timezone" to it.timezone,
                "instant_booking" to it.instant_booking,
            ).apply {
                it.available_days?.let { days -> put("available_days", days) }
            }
        }
        // Split-specific fields
        r.shareType?.let { body["shareType"] = it }
        r.share_type?.let { body["share_type"] = it }
        r.splitType?.let { body["splitType"] = it }
        r.totalCost?.let { body["totalCost"] = it }
        r.checkInDate?.let { body["checkInDate"] = it }
        r.checkOutDate?.let { body["checkOutDate"] = it }
        r.hotelName?.let { body["hotelName"] = it }
        r.roomType?.let { body["roomType"] = it }
        r.deadlineAt?.let { body["deadlineAt"] = it }
        r.requirements?.let { body["requirements"] = it }

        return body
    }

    // ── Revenue (iOS: HostDashboardService.getRevenue) ──────────

    suspend fun getRevenue(period: String = "30d"): Result<HostRevenueResponse> {
        return try {
            val response = hostApiService.getDashboardRevenue(period)
            if (response.isSuccessful) {
                Result.success(response.body() ?: HostRevenueResponse())
            } else {
                Result.failure(Exception("Failed to fetch revenue: ${response.code()}"))
            }
        } catch (e: Exception) {
            Timber.e(e, "Revenue fetch failed for period=$period")
            Result.failure(e)
        }
    }
}
