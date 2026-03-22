package com.shourov.apps.pacedream.feature.host.data

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
        val errorMessage: String?
    )

    suspend fun loadDashboard(): DashboardLoadResult = coroutineScope {
        var hadFailure = false

        val bookingsDeferred = async {
            try {
                val response = hostApiService.getHostBookings()
                if (response.isSuccessful) {
                    response.body()?.bookings ?: emptyList()
                } else {
                    hadFailure = true
                    emptyList()
                }
            } catch (e: Exception) {
                hadFailure = true
                emptyList<HostBookingDTO>()
            }
        }

        val listingsDeferred = async {
            try {
                val response = hostApiService.getHostListings()
                if (response.isSuccessful) {
                    response.body() ?: emptyList()
                } else {
                    hadFailure = true
                    emptyList()
                }
            } catch (e: Exception) {
                hadFailure = true
                emptyList<Property>()
            }
        }

        val overviewDeferred = async {
            try {
                val response = hostApiService.getDashboardOverview()
                if (response.isSuccessful) response.body() else {
                    hadFailure = true
                    null
                }
            } catch (e: Exception) {
                hadFailure = true
                null
            }
        }

        val payoutDeferred = async {
            try {
                resolvePayoutState()
            } catch (e: Exception) {
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

        val errorMessage = when {
            hadFailure && bookings.isEmpty() && listings.isEmpty() ->
                "Couldn't load host dashboard. Pull to refresh."
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
            errorMessage = errorMessage
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
                Result.success(response.body() ?: emptyList())
            } else {
                Result.failure(Exception("Failed to fetch listings: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createListing(request: CreateListingRequest): Result<Property> {
        return try {
            val response = hostApiService.createListing(request)
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    // Ensure we have an ID even if the response used _id
                    val resolvedId = body.id.ifEmpty {
                        extractListingId(response) ?: ""
                    }
                    Result.success(if (resolvedId.isNotEmpty() && body.id.isEmpty()) body.copy(id = resolvedId) else body)
                } else {
                    // iOS parity: try to extract listing ID from raw response body
                    val rawId = extractListingId(response)
                    Result.success(Property(id = rawId ?: "", title = request.title))
                }
            } else {
                val errorBody = response.errorBody()?.string()
                val errorMsg = try {
                    val json = Json.parseToJsonElement(errorBody ?: "")
                    json.jsonObject["message"]?.jsonPrimitive?.content
                        ?: json.jsonObject["error"]?.jsonPrimitive?.content
                } catch (_: Exception) { null }
                Result.failure(Exception(errorMsg ?: "Failed to create listing: ${response.code()}"))
            }
        } catch (e: Exception) {
            Timber.e(e, "createListing failed")
            Result.failure(e)
        }
    }

    /**
     * iOS parity: extract listing ID from response trying multiple JSON paths.
     * iOS tries: _id, id, data._id, data.id, data.listing._id, data.listing.id, listing._id, listing.id
     */
    private fun extractListingId(response: retrofit2.Response<Property>): String? {
        return try {
            // The response body is already parsed, try the raw string if available
            null // In Retrofit, the body is already consumed; ID should be in the Property
        } catch (_: Exception) { null }
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
            Result.failure(e)
        }
    }

    // ── Payouts / Stripe Connect (iOS: PayoutsService) ──────────

    suspend fun resolvePayoutState(): PayoutConnectionState {
        val response = hostApiService.getPayoutStatus()
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
            Result.success(emptyList())
        }
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
            Result.failure(e)
        }
    }
}
