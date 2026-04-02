package com.shourov.apps.pacedream.feature.host.data

import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.reflect.TypeToken
import com.shourov.apps.pacedream.model.Property
import com.shourov.apps.pacedream.model.PropertyPricing
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
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
    private val hostApiService: HostApiService,
    private val okHttpClient: okhttp3.OkHttpClient,
    private val appConfig: com.shourov.apps.pacedream.core.network.config.AppConfig
) {

    // ── Recently created listing cache ──
    // The backend GET /host/listings may not immediately return a newly created
    // listing (race condition or Host document not yet updated). Cache the most
    // recent creation so the dashboard can merge it in.
    @Volatile
    private var recentlyCreatedListing: Property? = null

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

    suspend fun loadDashboard(): DashboardLoadResult = withContext(Dispatchers.IO) {
        coroutineScope {
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
                // Strategy: try multiple sources to find host listings.
                // 1. Frontend proxy (/api/proxy/host/listings)
                // 2. Backend primary (/host/listings) - populates from Host document
                // 3. Backend by-owner (/host/listings/by-owner) - queries RentableItems directly
                // Source 3 is critical because listing_controller.js may not push
                // new listings to Host.listings.rentableItems, making them invisible
                // to source 2. The admin panel confirms the listing exists in the DB.

                // Source 1: frontend proxy
                val proxyListings = try {
                    withContext(Dispatchers.IO) { fetchHostListingsViaProxy() }
                } catch (e: Exception) {
                    Timber.d(e, "Frontend proxy exception, falling back to backend")
                    null
                }
                if (proxyListings != null && proxyListings.isNotEmpty()) {
                    Timber.d("Using %d listings from frontend proxy", proxyListings.size)
                    return@async proxyListings
                }

                // Source 2: primary backend endpoint
                var listings = emptyList<Property>()
                var primarySucceeded = false
                Timber.d("Fetching host listings from /host/listings (backend)")
                val response = hostApiService.getHostListings()
                if (response.isSuccessful) {
                    primarySucceeded = true
                    val json = response.body()
                    if (json != null) {
                        Timber.d("Host listings raw response keys: %s",
                            if (json.isJsonObject) json.asJsonObject.keySet().toString()
                            else if (json.isJsonArray) "array[${json.asJsonArray.size()}]"
                            else json.javaClass.simpleName
                        )
                        listings = withContext(Dispatchers.Default) {
                            parseHostListings(json)
                        }
                    }
                    Timber.d("Loaded ${listings.size} host listings from primary endpoint: %s",
                        listings.joinToString { "${it.id}:status=${it.status},pending=${it.isPendingReview},active=${it.isActiveStatus}" }
                    )
                } else {
                    if (isNonHostCode(response.code())) {
                        Timber.d("Host listings returned ${response.code()} — user may not have a host profile")
                        gotNonHostResponse = true
                        primarySucceeded = true // not an error, just no host profile
                    } else {
                        Timber.w("Host listings failed [${response.code()}]")
                        hadFailure = true
                    }
                }

                // Source 3: direct by-owner fallback
                // If primary returned 0 listings, try the by-owner endpoint which
                // queries RentableItem.find({ owner }) directly — not the Host document.
                if (listings.isEmpty() && primarySucceeded) {
                    Timber.d("Primary returned 0 listings, trying /host/listings/by-owner fallback")
                    try {
                        val fallbackResponse = hostApiService.getHostListingsByOwner()
                        if (fallbackResponse.isSuccessful) {
                            val fallbackJson = fallbackResponse.body()
                            if (fallbackJson != null) {
                                val fallbackListings = withContext(Dispatchers.Default) {
                                    parseHostListings(fallbackJson)
                                }
                                if (fallbackListings.isNotEmpty()) {
                                    Timber.d("By-owner fallback returned %d listings: %s",
                                        fallbackListings.size,
                                        fallbackListings.joinToString { "${it.id}:status=${it.status}" }
                                    )
                                    listings = fallbackListings
                                }
                            }
                        } else {
                            Timber.d("By-owner fallback failed [${fallbackResponse.code()}] (non-fatal)")
                        }
                    } catch (e: Exception) {
                        Timber.d(e, "By-owner fallback exception (non-fatal)")
                    }
                }

                listings
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
        var listings = listingsDeferred.await()
        val overview = overviewDeferred.await()
        val payoutState = payoutDeferred.await()
        val payoutEligibility = eligibilityDeferred.await()

        // Merge recently created listing if not yet returned by the backend.
        // The backend GET /host/listings may lag behind creation because
        // listing_controller.js doesn't always push to Host.listings.rentableItems.
        val cached = recentlyCreatedListing
        if (cached != null && cached.id.isNotEmpty()) {
            val alreadyPresent = listings.any { it.id == cached.id }
            if (!alreadyPresent) {
                Timber.d("Merging recently created listing %s into dashboard (not in API response)", cached.id)
                listings = listings + cached
            } else {
                // Backend now returns it — clear the cache
                recentlyCreatedListing = null
            }
        } else if (cached != null && cached.id.isEmpty() && listings.isEmpty()) {
            // Listing was created but ID wasn't returned. Still show it as pending
            // so the host doesn't see an empty dashboard right after creating.
            Timber.d("Merging recently created listing (no ID) into dashboard as pending placeholder")
            listings = listings + cached
        }

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
}

    // ── Bookings (iOS: HostBookingsService) ─────────────────────

    suspend fun getHostBookings(): Result<List<HostBookingDTO>> = withContext(Dispatchers.IO) {
        try {
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

    suspend fun updateBookingStatus(id: String, status: String, reason: String? = null): Result<HostBookingDTO> = withContext(Dispatchers.IO) {
        try {
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

    suspend fun getHostListings(filter: String? = null, sort: String? = null): Result<List<Property>> = withContext(Dispatchers.IO) {
        try {
            // iOS parity: try frontend proxy first, then backend
            val proxyListings = fetchHostListingsViaProxy()
            if (proxyListings != null && proxyListings.isNotEmpty()) {
                return@withContext Result.success(proxyListings)
            }

            val response = hostApiService.getHostListings(filter, sort)
            if (response.isSuccessful) {
                val json = response.body()
                val listings = if (json != null) {
                    withContext(Dispatchers.Default) {
                        parseHostListings(json)
                    }
                } else emptyList()
                Result.success(listings)
            } else {
                Result.failure(Exception("Failed to fetch listings: ${response.code()}"))
            }
        } catch (e: Exception) {
            Timber.e(e, "Host listings fetch failed")
            Result.failure(e)
        }
    }

    suspend fun createListing(request: CreateListingRequest): Result<Property> = withContext(Dispatchers.IO) {
        try {
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
                    val property = withContext(Dispatchers.Default) {
                        parseCreateListingResponse(json, request)
                    }
                    Timber.d("createListing success: id=%s", property.id)
                    // Cache the created listing as pending_review so the
                    // dashboard can merge it in when the backend GET
                    // /host/listings hasn't been updated yet.
                    val cached = property.copy(
                        status = property.status ?: "pending_review",
                        title = property.title.ifEmpty { request.title },
                        images = request.images ?: emptyList()
                    )
                    recentlyCreatedListing = cached
                    Timber.d("Cached recently created listing: id=%s status=%s", cached.id, cached.status)
                    Result.success(property)
                } else {
                    val fallback = Property(id = "", title = request.title, status = "pending_review", images = request.images ?: emptyList())
                    recentlyCreatedListing = fallback
                    Result.success(fallback)
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

    suspend fun resolvePayoutState(): PayoutConnectionState = withContext(Dispatchers.Default) {
        val response = try {
            // Retrofit calls are safe to call from any thread as they switch to IO internally
            // but we wrap the entire resolution in Default for the mapping logic.
            hostApiService.getPayoutStatus()
        } catch (e: Exception) {
            Timber.e(e, "resolvePayoutState: failed to fetch payout status")
            return@withContext PayoutConnectionState.NOT_CONNECTED
        }
        if (!response.isSuccessful) return@withContext PayoutConnectionState.NOT_CONNECTED

        val body = response.body() ?: return@withContext PayoutConnectionState.NOT_CONNECTED
        val raw = (body.status.ifEmpty { body.payoutStatus ?: "" }).lowercase()
        val chargesEnabled = body.resolvedChargesEnabled
        val payoutsEnabled = body.resolvedPayoutsEnabled
        val detailsSubmitted = body.resolvedDetailsSubmitted

        when {
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

    // ── Frontend proxy for host listings (iOS parity: FrontendProxyClient) ──

    /**
     * iOS parity: fetch host listings from the frontend web proxy first.
     * iOS calls GET /api/proxy/host/listings via FrontendProxyClient,
     * falling back to the direct backend /host/listings endpoint.
     * The web proxy returns listings that the direct backend may not (e.g. pending review).
     *
     * The injected OkHttpClient already has the auth token interceptor, so
     * Authorization: Bearer <token> is added automatically.
     */
    private fun fetchHostListingsViaProxy(): List<Property>? {
        return try {
            val url = appConfig.buildFrontendUrl("api", "proxy", "host", "listings")
            Timber.d("Trying frontend proxy for host listings: $url")

            val request = okhttp3.Request.Builder()
                .url(url)
                .get()
                .addHeader("Accept", "application/json")
                .build()

            val response = okHttpClient.newCall(request).execute()
            if (response.isSuccessful) {
                val body = response.body?.string()
                if (!body.isNullOrBlank()) {
                    Timber.d("Frontend proxy returned %d bytes for host listings", body.length)
                    val json = com.google.gson.JsonParser.parseString(body)
                    val listings = parseHostListings(json)
                    if (listings.isNotEmpty()) {
                        Timber.d("Frontend proxy: parsed %d listings", listings.size)
                        return listings
                    } else {
                        Timber.d("Frontend proxy returned data but parsed 0 listings")
                    }
                }
            } else {
                Timber.d("Frontend proxy failed [${response.code}], falling back to backend")
            }
            null
        } catch (e: Exception) {
            Timber.d(e, "Frontend proxy unavailable, falling back to backend: ${e.message}")
            null
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
            // Backend helper.sendSuccess wraps as { data: { rooms: [...], ... } }.
            // When `data` is an object (not array), recurse into it to find
            // category-keyed or flat-array listings inside.
            if (unwrapped != null && unwrapped.isJsonObject) {
                val inner = parseHostListings(unwrapped)
                if (inner.isNotEmpty()) return inner
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
        val properties = try {
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
        // iOS parity: fill in status from alternative fields ("state", "published" boolean)
        // when Gson didn't pick it up from "status" or "listingStatus".
        // Also check "approved"/"isApproved" boolean — if the listing has an active-like
        // status but approved is explicitly false, override to "pending_review" so the
        // dashboard correctly shows it as pending admin approval (iOS web proxy does this).
        return properties.mapIndexed { index, property ->
            val element = arr.getOrNull(index)
            val obj = if (element != null && element.isJsonObject) element.asJsonObject else null

            // Log raw status fields from the backend for debugging
            if (obj != null) {
                val rawStatus = obj.getStringOrNull("status")
                val rawListingStatus = obj.getStringOrNull("listingStatus")
                val rawState = obj.getStringOrNull("state")
                val rawApproved = obj.get("approved")?.toString()
                val rawIsApproved = obj.get("isApproved")?.toString()
                Timber.d(
                    "Listing[%d] id=%s gsonStatus=%s raw: status=%s listingStatus=%s state=%s approved=%s isApproved=%s",
                    index, property.id, property.status,
                    rawStatus, rawListingStatus, rawState, rawApproved, rawIsApproved
                )
            }

            // Step 1: resolve status if missing
            val resolved = if (property.status != null) {
                property
            } else if (obj != null) {
                val fallbackStatus = obj.getStringOrNull("state")
                    ?: obj.get("published")?.let { pub ->
                        if (pub.isJsonPrimitive && pub.asJsonPrimitive.isBoolean) {
                            if (pub.asBoolean) "published" else "draft"
                        } else null
                    }
                    // If none of the known status fields have a value, default to
                    // "pending_review" for newly created listings that show up in
                    // /host/listings without an explicit status.
                    ?: "pending_review"
                property.copy(status = fallbackStatus)
            } else {
                // No raw JSON available and no status — treat as pending
                if (property.status == null) property.copy(status = "pending_review") else property
            }

            // Step 2: check approved/isApproved boolean — override active-like status
            // to "pending_review" when admin has not approved the listing yet.
            // The iOS web proxy performs this override server-side; Android must do it client-side
            // because it calls the backend API directly.
            if (obj != null && resolved.isActiveStatus) {
                // 2a: explicit boolean approval fields
                val approvedField = obj.get("approved") ?: obj.get("isApproved")
                    ?: obj.get("is_approved") ?: obj.get("adminApproved")
                    ?: obj.get("admin_approved")
                if (approvedField != null && approvedField.isJsonPrimitive &&
                    approvedField.asJsonPrimitive.isBoolean && !approvedField.asBoolean) {
                    return@mapIndexed resolved.copy(status = "pending_review")
                }

                // 2b: string-based approval/moderation status fields
                val moderationStatus = obj.getStringOrNull("moderationStatus")
                    ?: obj.getStringOrNull("moderation_status")
                    ?: obj.getStringOrNull("adminStatus")
                    ?: obj.getStringOrNull("admin_status")
                    ?: obj.getStringOrNull("reviewStatus")
                    ?: obj.getStringOrNull("review_status")
                    ?: obj.getStringOrNull("approvalStatus")
                    ?: obj.getStringOrNull("approval_status")
                if (moderationStatus != null) {
                    val ms = moderationStatus.trim().lowercase()
                    if (ms.contains("pending") || ms.contains("review") ||
                        ms.contains("awaiting") || ms == "submitted") {
                        return@mapIndexed resolved.copy(status = "pending_review")
                    }
                }

                // 2c: verified field — if listing has an explicit verified=false, treat as pending
                val verifiedField = obj.get("verified") ?: obj.get("isVerified")
                    ?: obj.get("is_verified")
                if (verifiedField != null && verifiedField.isJsonPrimitive &&
                    verifiedField.asJsonPrimitive.isBoolean && !verifiedField.asBoolean) {
                    // Only override if there's also an indication this is moderation-related
                    // (e.g. the listing was just created). Check createdAt vs updatedAt proximity
                    // or just override — the backend sets verified=false for new listings.
                    return@mapIndexed resolved.copy(status = "pending_review")
                }
            }

            // Step 3: extract images from gallery if Property.images is empty.
            // RentableItems store images in gallery.images / gallery.thumbnail,
            // not a top-level images array. Gson won't populate Property.images
            // from these nested fields, so we extract them manually.
            val withImages = if (resolved.images.isEmpty() && obj != null) {
                val extractedImages = mutableListOf<String>()
                val gallery = obj.getAsJsonObject("gallery")
                if (gallery != null) {
                    // gallery.thumbnail
                    gallery.getStringOrNull("thumbnail")?.let { thumb ->
                        if (thumb.isNotBlank()) extractedImages.add(thumb)
                    }
                    // gallery.images array
                    val galleryImages = gallery.get("images")
                    if (galleryImages != null && galleryImages.isJsonArray) {
                        galleryImages.asJsonArray.forEach { el ->
                            if (el.isJsonPrimitive && el.asJsonPrimitive.isString) {
                                val url = el.asString
                                if (url.isNotBlank() && url !in extractedImages) {
                                    extractedImages.add(url)
                                }
                            }
                        }
                    }
                }
                // Also try top-level photos, photoUrls, imageUrls, media arrays
                for (key in listOf("photos", "photoUrls", "imageUrls", "media")) {
                    val arr = obj.get(key)
                    if (arr != null && arr.isJsonArray && extractedImages.isEmpty()) {
                        arr.asJsonArray.forEach { el ->
                            if (el.isJsonPrimitive && el.asJsonPrimitive.isString) {
                                val url = el.asString
                                if (url.isNotBlank()) extractedImages.add(url)
                            }
                        }
                    }
                }
                // Single image fields: coverImage, cover, image, imageUrl, thumbnail
                if (extractedImages.isEmpty()) {
                    for (key in listOf("coverImage", "cover", "image", "imageUrl", "thumbnail")) {
                        obj.getStringOrNull(key)?.let { url ->
                            if (url.isNotBlank()) extractedImages.add(url)
                        }
                    }
                }
                if (extractedImages.isNotEmpty()) {
                    Timber.d("Extracted %d images from gallery/alt fields for listing %s", extractedImages.size, resolved.id)
                    resolved.copy(images = extractedImages)
                } else resolved
            } else resolved

            // Step 4: extract pricing from RentableItem price array.
            // RentableItem has price: [{ amount, pricing_type, currency, frequency }]
            // but Property.pricing expects { basePrice, unit, currency }.
            val withPricing = if (withImages.pricing.basePrice == 0.0 && obj != null) {
                val priceArr = obj.get("price")
                if (priceArr != null && priceArr.isJsonArray && priceArr.asJsonArray.size() > 0) {
                    val first = priceArr.asJsonArray[0]
                    if (first.isJsonObject) {
                        val p = first.asJsonObject
                        val amount = try { p.get("amount")?.asDouble ?: 0.0 } catch (_: Exception) { 0.0 }
                        val pricingType = p.getStringOrNull("pricing_type") ?: ""
                        val currency = p.getStringOrNull("currency") ?: "USD"
                        val unit = when (pricingType.lowercase()) {
                            "hourly" -> "hour"
                            "daily" -> "day"
                            "weekly" -> "week"
                            "monthly" -> "month"
                            else -> pricingType.ifBlank { "hour" }
                        }
                        if (amount > 0) {
                            withImages.copy(
                                pricing = PropertyPricing(
                                    basePrice = amount,
                                    currency = currency,
                                    unit = unit,
                                    pricingType = pricingType
                                )
                            )
                        } else withImages
                    } else withImages
                } else withImages
            } else withImages

            withPricing
        }
    }

    private fun JsonArray.getOrNull(index: Int): JsonElement? =
        if (index in 0 until size()) get(index) else null

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

    suspend fun getRevenue(period: String = "30d"): Result<HostRevenueResponse> = withContext(Dispatchers.IO) {
        try {
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
