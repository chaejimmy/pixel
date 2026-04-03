package com.shourov.apps.pacedream.feature.host.data

import com.google.gson.JsonElement
import com.google.gson.annotations.SerializedName
import com.shourov.apps.pacedream.core.network.ApiEndPoints
import com.shourov.apps.pacedream.model.BookingModel
import com.shourov.apps.pacedream.model.Property
import retrofit2.Response
import retrofit2.http.*

/**
 * Host API Service - iOS parity.
 *
 * Endpoints match iOS HostDashboardService, HostBookingsService, HostListingsService,
 * and PayoutsService for full cross-platform alignment.
 */
interface HostApiService {

    // ── Dashboard (iOS: HostDashboardService) ───────────────────

    @GET(ApiEndPoints.HOST_DASHBOARD_OVERVIEW)
    suspend fun getDashboardOverview(): Response<HostDashboardOverviewResponse>

    @GET(ApiEndPoints.HOST_DASHBOARD_ANALYTICS)
    suspend fun getDashboardAnalytics(
        @Query("period") period: String = "30d"
    ): Response<HostAnalyticsResponse>

    @GET(ApiEndPoints.HOST_DASHBOARD_REVENUE)
    suspend fun getDashboardRevenue(
        @Query("period") period: String = "30d"
    ): Response<HostRevenueResponse>

    @GET(ApiEndPoints.HOST_DASHBOARD_BOOKINGS)
    suspend fun getDashboardBookings(
        @Query("period") period: String = "30d",
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20
    ): Response<PaginatedBookingsResponse>

    // ── Bookings (iOS: HostBookingsService) ─────────────────────

    @GET(ApiEndPoints.HOST_GET_BOOKINGS)
    suspend fun getHostBookings(): Response<HostBookingsListResponse>

    @PATCH(ApiEndPoints.HOST_UPDATE_BOOKING)
    suspend fun updateHostBooking(
        @Path("bookingId") bookingId: String,
        @Body body: BookingStatusUpdate
    ): Response<HostBookingUpdateResponse>

    // ── Listings (iOS: HostListingsService) ──────────────────────

    @GET(ApiEndPoints.HOST_GET_LISTINGS)
    suspend fun getHostListings(
        @Query("filter") filter: String? = null,
        @Query("sort") sort: String? = null
    ): Response<JsonElement>

    /** Fallback: fetch RentableItems directly by owner, bypassing Host document array */
    @GET(ApiEndPoints.HOST_GET_LISTINGS_BY_OWNER)
    suspend fun getHostListingsByOwner(): Response<JsonElement>

    @POST(ApiEndPoints.CREATE_LISTING)
    suspend fun createListing(@Body listing: Map<String, @JvmSuppressWildcards Any>): Response<JsonElement>

    @PUT(ApiEndPoints.HOST_UPDATE_LISTING)
    suspend fun updateListing(
        @Path("listingId") id: String,
        @Body listing: Property
    ): Response<Property>

    @DELETE(ApiEndPoints.HOST_DELETE_LISTING)
    suspend fun deleteListing(@Path("listingId") id: String): Response<Unit>

    // ── Payouts / Stripe Connect (iOS: PayoutsService) ──────────

    @GET(ApiEndPoints.HOST_PAYOUT_STATUS)
    suspend fun getPayoutStatus(): Response<PayoutStatusResponse>

    @GET(ApiEndPoints.HOST_PAYOUT_SETUP_ELIGIBILITY)
    suspend fun getPayoutSetupEligibility(): Response<PayoutSetupEligibilityResponse>

    @POST(ApiEndPoints.HOST_PAYOUT_ONBOARDING_LINK)
    suspend fun createOnboardingLink(
        @Query("platform") platform: String = "android"
    ): Response<PayoutLinkResponse>

    @POST(ApiEndPoints.HOST_PAYOUT_LOGIN_LINK)
    suspend fun createLoginLink(): Response<PayoutLinkResponse>

    @GET(ApiEndPoints.HOST_PAYOUT_METHODS)
    suspend fun getPayoutMethods(): Response<PayoutMethodsResponse>

    @POST("host/payouts/methods/set-primary")
    suspend fun setPrimaryPayoutMethod(@Body body: SetPrimaryMethodRequest): Response<Unit>

    // ── Earnings Dashboard (iOS parity: single all-in-one endpoint) ──

    @GET(ApiEndPoints.HOST_EARNINGS_DASHBOARD)
    suspend fun getEarningsDashboard(): Response<JsonElement>

    // ── Legacy Stripe Connect endpoints (kept for createPayout/createConnectAccount) ──

    @POST("host/stripe/payouts/create")
    suspend fun createPayout(@Body request: CreatePayoutRequest): Response<Payout>

    @GET("host/stripe/connect/status")
    suspend fun getConnectAccountStatus(): Response<ConnectAccount>

    @POST("host/stripe/connect/create")
    suspend fun createConnectAccount(@Body request: CreateConnectAccountRequest): Response<ConnectAccount>

    // ── Reviews (iOS: HostDashboardService) ─────────────────────

    @GET(ApiEndPoints.HOST_REVIEWS_SUMMARY)
    suspend fun getReviewsSummary(): Response<HostReviewsSummaryResponse>

    @GET(ApiEndPoints.HOST_REVIEWS)
    suspend fun getReviews(
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20
    ): Response<PaginatedReviewsResponse>

    // ── Personal Info (iOS: HostDashboardService) ───────────────

    @GET(ApiEndPoints.HOST_PERSONAL_INFO)
    suspend fun getPersonalInfo(): Response<HostPersonalInfoResponse>

    @PUT(ApiEndPoints.HOST_PERSONAL_INFO)
    suspend fun updatePersonalInfo(@Body info: Map<String, @JvmSuppressWildcards Any>): Response<HostPersonalInfoResponse>

    // ── Payments & Transactions (iOS: HostDashboardService) ─────

    @GET(ApiEndPoints.HOST_PAYMENTS)
    suspend fun getPayments(
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20
    ): Response<PaginatedPaymentsResponse>

    @GET(ApiEndPoints.HOST_TRANSACTIONS)
    suspend fun getTransactions(
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20
    ): Response<PaginatedTransactionsResponse>

    // Analytics (using ApiEndPoints constants)
    @GET(ApiEndPoints.HOST_GET_ANALYTICS)
    suspend fun getHostAnalytics(
        @Query("timeRange") timeRange: String? = null
    ): Response<HostAnalyticsData>

    // ── Calendar / Availability (backend source of truth) ───────────

    /** Host: get calendar with bookings, blocks, holds overlay for a month */
    @GET(ApiEndPoints.HOST_LISTING_CALENDAR)
    suspend fun getListingCalendar(
        @Path("listingId") listingId: String,
        @Query("month") month: Int,
        @Query("year") year: Int
    ): Response<ListingCalendarResponse>

    /** Host: create a blocked time range on a listing */
    @POST(ApiEndPoints.HOST_BLOCK_TIME)
    suspend fun blockListingTime(
        @Path("listingId") listingId: String,
        @Body body: BlockTimeRequest
    ): Response<BlockTimeResponse>

    /** Host: remove a blocked time range */
    @DELETE(ApiEndPoints.HOST_REMOVE_BLOCK)
    suspend fun removeListingBlock(
        @Path("listingId") listingId: String,
        @Path("blockId") blockId: String
    ): Response<GenericSuccessResponse>

    /** Host: update listing availability (dateRange + blocks) */
    @PATCH(ApiEndPoints.HOST_UPDATE_AVAILABILITY)
    suspend fun updateListingAvailability(
        @Path("listingId") listingId: String,
        @Body body: UpdateAvailabilityRequest
    ): Response<JsonElement>

    /** Guest/public: check if a specific time range is available */
    @POST(ApiEndPoints.CHECK_AVAILABILITY)
    suspend fun checkAvailability(
        @Path("listingId") listingId: String,
        @Body body: CheckAvailabilityRequest
    ): Response<CheckAvailabilityResponse>

    /** Guest/public: get monthly availability calendar */
    @GET(ApiEndPoints.GET_LISTING_AVAILABILITY_CALENDAR)
    suspend fun getAvailabilityCalendar(
        @Path("listingId") listingId: String,
        @Query("month") month: Int,
        @Query("year") year: Int
    ): Response<JsonElement>
}

// ── Request/Response Models (iOS parity) ────────────────────────

data class BookingStatusUpdate(
    val status: String,
    val reason: String? = null
)

data class SetPrimaryMethodRequest(val id: String)

data class CreatePayoutRequest(
    val amount: Int,
    val currency: String = "usd"
)

data class CreateConnectAccountRequest(
    val email: String,
    val country: String = "US"
)

data class WithdrawalResponse(
    val id: String,
    val status: String,
    val processedAt: String? = null
)

data class HostAnalyticsData(
    val views: Int = 0,
    val bookings: Int = 0,
    val revenue: Double = 0.0,
    val occupancyRate: Double = 0.0
)

// ── Dashboard Overview (matches iOS HostDashboardOverview) ──────

data class HostDashboardOverviewResponse(
    val totalBookings: Int = 0,
    val totalRevenue: Double = 0.0,
    val averageRating: Double = 0.0,
    val totalReviews: Int = 0,
    val occupancyRate: Double = 0.0,
    val responseRate: Double = 0.0,
    val responseTime: String = "",
    val activeListings: Int = 0,
    val pendingBookings: Int = 0,
    val recentBookings: List<HostBookingResponse> = emptyList(),
    val recentReviews: List<HostReviewResponse> = emptyList()
)

data class HostBookingResponse(
    val id: String = "",
    val guestName: String = "",
    val guestAvatar: String? = null,
    val listingName: String = "",
    val checkIn: String = "",
    val checkOut: String = "",
    val guests: Int = 0,
    val totalAmount: Double = 0.0,
    val currency: String = "USD",
    val status: String = "",
    val createdAt: String = ""
)

// ── Analytics (matches iOS HostAnalytics) ───────────────────────

data class HostAnalyticsResponse(
    val period: String = "",
    val bookings: List<AnalyticsDataPoint> = emptyList(),
    val revenue: List<AnalyticsDataPoint> = emptyList(),
    val occupancy: List<AnalyticsDataPoint> = emptyList(),
    val views: List<AnalyticsDataPoint> = emptyList(),
    val conversionRate: List<AnalyticsDataPoint> = emptyList(),
    val averageRating: List<AnalyticsDataPoint> = emptyList()
)

data class AnalyticsDataPoint(
    val date: String = "",
    val value: Double = 0.0,
    val label: String? = null
)

// ── Revenue (matches iOS HostRevenue) ───────────────────────────

data class HostRevenueResponse(
    val period: String = "",
    val totalRevenue: Double = 0.0,
    val grossRevenue: Double = 0.0,
    val platformFees: Double = 0.0,
    val netRevenue: Double = 0.0,
    val currency: String = "USD",
    val revenueByMonth: List<RevenueByMonth> = emptyList(),
    val revenueByListing: List<RevenueByListing> = emptyList()
)

data class RevenueByMonth(
    val month: String = "",
    val revenue: Double = 0.0,
    val bookings: Int = 0
)

data class RevenueByListing(
    val listingId: String = "",
    val listingName: String = "",
    val revenue: Double = 0.0,
    val bookings: Int = 0
)

// ── Bookings List (matches iOS HostBookingsService) ─────────────

data class HostBookingsListResponse(
    val bookings: List<HostBookingDTO> = emptyList()
)

data class HostBookingDTO(
    @SerializedName(value = "id", alternate = ["_id"])
    val id: String = "",
    val status: String? = null,
    val guestName: String? = null,
    val listingTitle: String? = null,
    val start: String? = null,
    val end: String? = null,
    @SerializedName(value = "checkIn", alternate = ["check_in"])
    val checkIn: String? = null,
    @SerializedName(value = "checkOut", alternate = ["check_out"])
    val checkOut: String? = null,
    val startDate: String? = null,
    val endDate: String? = null,
    val total: Double = 0.0,
    val totalAmount: Double = 0.0,
    val totalPrice: Double = 0.0,
    val currency: String = "USD",
    val guests: Int = 0,
    val createdAt: String? = null,
    val guest: GuestInfo? = null,
    val listing: ListingInfo? = null,
    // iOS parity: payout status and host earnings fields
    val payoutStatus: String? = null,
    val payout_status: String? = null,
    val hostEarnings: Double? = null,
    val host_earnings: Double? = null,
    val platformFee: Double? = null,
    val platform_fee: Double? = null,
    val cancellationReason: String? = null,
    val cancellation_reason: String? = null,
    // iOS PR #202 parity: verification PIN for guest/host check-in
    val verificationPin: String? = null,
    val verification_pin: String? = null,
    val verificationCode: String? = null,
    val verification_code: String? = null,
    val pinStatus: String? = null,
    val pin_status: String? = null,
    val verificationStatus: String? = null,
    val verification_status: String? = null
) {
    /** Resolved payout status (iOS parity: payoutStatus field) */
    val resolvedPayoutStatus: String?
        get() = payoutStatus ?: payout_status

    /** Resolved host earnings - net amount after fees (iOS parity: hostEarnings field) */
    val resolvedHostEarnings: Double?
        get() = hostEarnings ?: host_earnings

    /** Resolved platform fee */
    val resolvedPlatformFee: Double?
        get() = platformFee ?: platform_fee

    /** Resolved cancellation reason */
    val resolvedCancellationReason: String?
        get() = cancellationReason ?: cancellation_reason

    /** Resolved verification PIN */
    val resolvedVerificationPin: String?
        get() = verificationPin ?: verification_pin ?: verificationCode ?: verification_code

    /** Resolved PIN status */
    val resolvedPinStatus: String?
        get() = pinStatus ?: pin_status ?: verificationStatus ?: verification_status
    /** Resolved total matching iOS b.total logic */
    val resolvedTotal: Double get() = when {
        total > 0 -> total
        totalAmount > 0 -> totalAmount
        totalPrice > 0 -> totalPrice
        else -> 0.0
    }

    /** Resolved start date string */
    val resolvedStart: String? get() = start ?: checkIn ?: startDate

    /** Resolved end date string */
    val resolvedEnd: String? get() = end ?: checkOut ?: endDate

    /** Resolved guest name */
    val resolvedGuestName: String get() = guestName ?: guest?.name ?: "Guest"

    /** Resolved listing title */
    val resolvedListingTitle: String get() = listingTitle ?: listing?.title ?: "Listing"
}

data class GuestInfo(
    val name: String? = null,
    val avatar: String? = null
)

data class ListingInfo(
    val title: String? = null,
    val id: String? = null
)

data class HostBookingUpdateResponse(
    val ok: Boolean? = null,
    val booking: HostBookingDTO? = null,
    val error: String? = null
)

data class PaginatedBookingsResponse(
    val data: List<HostBookingResponse> = emptyList(),
    val total: Int = 0,
    val page: Int = 1,
    val limit: Int = 20
)

// ── Payouts (matches iOS PayoutsService) ────────────────────────

data class PayoutStatusResponse(
    val status: String = "",
    val payoutStatus: String? = null,
    val chargesEnabled: Boolean = false,
    val charges_enabled: Boolean = false,
    val payoutsEnabled: Boolean = false,
    val payouts_enabled: Boolean = false,
    val detailsSubmitted: Boolean = false,
    val details_submitted: Boolean = false,
    val details: String? = null,
    val requirements: PayoutRequirements? = null,
    val missingRequirements: List<String>? = null,
    val data: PayoutStatusResponse? = null
) {
    val resolvedChargesEnabled: Boolean get() = chargesEnabled || charges_enabled
    val resolvedPayoutsEnabled: Boolean get() = payoutsEnabled || payouts_enabled
    val resolvedDetailsSubmitted: Boolean get() = detailsSubmitted || details_submitted
    val resolvedCurrentlyDue: List<String> get() =
        requirements?.currentlyDue ?: requirements?.currently_due ?: missingRequirements ?: emptyList()
}

data class PayoutRequirements(
    val currentlyDue: List<String>? = null,
    val currently_due: List<String>? = null
)

data class PayoutLinkResponse(
    val url: String? = null,
    val dashboardUrl: String? = null,
    val link: String? = null,
    val data: PayoutLinkData? = null
) {
    val resolvedUrl: String? get() = url ?: dashboardUrl ?: link ?: data?.url ?: data?.link
}

data class PayoutLinkData(
    val url: String? = null,
    val link: String? = null
)

data class PayoutMethodsResponse(
    val data: List<PayoutMethodResponse>? = null,
    val methods: List<PayoutMethodResponse>? = null
) {
    val resolvedMethods: List<PayoutMethodResponse> get() = data ?: methods ?: emptyList()
}

data class PayoutMethodResponse(
    val id: String? = null,
    val type: String? = null,
    val methodType: String? = null,
    val brand: String? = null,
    val bankName: String? = null,
    val last4: String? = null,
    val last_4: String? = null,
    val isPrimary: Boolean = false,
    val primary: Boolean = false
) {
    val resolvedId: String get() = id ?: ""
    val resolvedType: String get() = type ?: methodType ?: "method"
    val resolvedLast4: String? get() = last4 ?: last_4
    val resolvedLabel: String get() {
        val brandName = brand ?: bankName
        val digits = resolvedLast4?.let { "•••• $it" }
        val parts = listOfNotNull(brandName, digits)
        return if (parts.isNotEmpty()) parts.joinToString(" ") else resolvedType
    }
    val resolvedIsPrimary: Boolean get() = isPrimary || primary
}

// ── Payout Setup Eligibility (server-driven prompt) ─────────────

/**
 * Server-driven payout setup eligibility response.
 * All clients (iOS, Android, web) use this to decide whether
 * to show payout setup prompts.
 */
data class PayoutSetupEligibilityResponse(
    val shouldShowPayoutSetupPrompt: Boolean = false,
    val payoutPromptReason: String? = null,
    val payoutOnboardingComplete: Boolean = false,
    val payoutStatus: String = "UNSET"
)

// ── Reviews (matches iOS HostReviewsSummary) ────────────────────

data class HostReviewsSummaryResponse(
    val averageRating: Double = 0.0,
    val totalReviews: Int = 0,
    val ratingBreakdown: List<RatingBreakdown> = emptyList(),
    val recentReviews: List<HostReviewResponse> = emptyList()
)

data class RatingBreakdown(
    val rating: Int = 0,
    val count: Int = 0,
    val percentage: Double = 0.0
)

data class HostReviewResponse(
    val id: String = "",
    val guestName: String = "",
    val guestAvatar: String? = null,
    val listingName: String = "",
    val rating: Int = 0,
    val comment: String = "",
    val response: String? = null,
    val createdAt: String = ""
)

data class PaginatedReviewsResponse(
    val data: List<HostReviewResponse> = emptyList(),
    val total: Int = 0,
    val page: Int = 1,
    val limit: Int = 20
)

// ── Personal Info (matches iOS HostPersonalInfo) ────────────────

data class HostPersonalInfoResponse(
    val id: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val email: String = "",
    val phone: String? = null,
    val isVerified: Boolean = false,
    val verificationStatus: String = "",
    val createdAt: String = "",
    val updatedAt: String = ""
)

// ── Payments & Transactions (matches iOS) ───────────────────────

data class HostPaymentResponse(
    val id: String = "",
    val amount: Double = 0.0,
    val currency: String = "USD",
    val status: String = "",
    val type: String = "",
    val description: String = "",
    val bookingId: String? = null,
    val createdAt: String = ""
)

data class HostTransactionResponse(
    val id: String = "",
    val type: String = "",
    val amount: Double = 0.0,
    val currency: String = "USD",
    val description: String = "",
    val status: String = "",
    val bookingId: String? = null,
    val createdAt: String = ""
)

data class PaginatedPaymentsResponse(
    val data: List<HostPaymentResponse> = emptyList(),
    val total: Int = 0,
    val page: Int = 1,
    val limit: Int = 20
)

data class PaginatedTransactionsResponse(
    val data: List<HostTransactionResponse> = emptyList(),
    val total: Int = 0,
    val page: Int = 1,
    val limit: Int = 20
)

// ── Listing Creation (web parity) ───────────────────────────────

/**
 * Request body for creating a listing.
 * Matches the website ListingWizard payload structure for full web parity.
 * Endpoint: POST /listings
 *
 * Key fields the backend requires:
 *   - title (non-empty)
 *   - price (>= $1)
 *   - availability (always sent, even for non-schedule listings)
 *   - available = true
 *   - location with street, city, state, country
 *   - prices map and details object (website sends these)
 */
data class CreateListingRequest(
    val listing_type: String,
    val subCategory: String,
    val title: String,
    val description: String = "",
    val summary: String = "",
    val price: Double,
    val pricing_type: String,
    val pricing: PricingPayload,
    // Website sends a prices map: { hour: X, day: Y, month: Z }
    val prices: Map<String, Double>? = null,
    val address: String? = null,
    val amenities: List<String>? = null,
    // Website sends a details object with features and reviewCount
    val details: DetailsPayload? = null,
    val images: List<String>? = null,
    val location: LocationPayload? = null,
    // Website always marks listings as available
    val available: Boolean = true,
    val durations: List<Int>? = null,
    // Top-level stay/month fields (website sends these alongside availability)
    val minStay: Int? = null,
    val maxStay: Int? = null,
    val minMonths: Int? = null,
    val availableFrom: String? = null,
    // Always sent (website sends availability even for non-schedule listings)
    val availability: AvailabilityPayload? = null,
    // Split-specific fields
    val shareType: String? = null,
    val share_type: String? = null,
    val splitType: String? = null,
    val totalCost: Double? = null,
    val checkInDate: String? = null,
    val checkOutDate: String? = null,
    val hotelName: String? = null,
    val roomType: String? = null,
    val deadlineAt: String? = null,
    val requirements: String? = null,
    val experiments: Map<String, String>? = null,
)

data class PricingPayload(
    val base_price: Double,
    val unit: String,
    val currency: String = "USD",
)

/**
 * Details payload matching website: { features: [...], reviewCount: 0 }
 */
data class DetailsPayload(
    val features: List<String> = emptyList(),
    val reviewCount: Int = 0,
)

/**
 * Location payload matching website format.
 * Website sends: { street, city, state, country }.
 * Backend also reads lat/lng for geocoding.
 */
data class LocationPayload(
    val street: String = "",
    val street_address: String = "",
    val city: String = "",
    val state: String = "",
    val country: String = "US",
    val latitude: Double? = null,
    val longitude: Double? = null,
)

data class AvailabilityPayload(
    val start_time: String = "09:00",
    val end_time: String = "17:00",
    val available_days: List<Int>? = listOf(1, 2, 3, 4, 5),
    val timezone: String = "America/New_York",
    val instant_booking: Boolean = false,
    val dateRange: DateRangePayload? = null,
    val blocks: List<String> = emptyList(),
)

data class DateRangePayload(
    val startDate: String? = null,
    val endDate: String? = null,
)

// ── Calendar / Availability DTOs (backend contract) ────────────

/**
 * Response from GET /host/listings/:id/calendar
 *
 * Backend returns a `days` map keyed by "YYYY-MM-DD" date strings,
 * each containing that day's status, bookings, and blocks.
 * NOT flat arrays — must be parsed as Map.
 */
data class ListingCalendarResponse(
    val success: Boolean = false,
    val data: ListingCalendarData? = null
)

data class ListingCalendarData(
    val listingId: String = "",
    val listingTitle: String = "",
    val month: Int = 0,
    val year: Int = 0,
    /** Map of "YYYY-MM-DD" → day data with status, bookings, blocks */
    val days: Map<String, CalendarDayData> = emptyMap(),
    /** Raw availability settings from the listing (snake_case from MongoDB) */
    val availability: AvailabilityData? = null
)

/** Per-day data within the calendar `days` map */
data class CalendarDayData(
    val date: String = "",
    /** Day-level status: "available", "blocked", "booked", "pending" */
    val status: String = "available",
    /** Bookings overlapping this day (only ACTIVE statuses from backend) */
    val bookings: List<CalendarDayBooking> = emptyList(),
    /** Blocked time ranges overlapping this day */
    val blocks: List<CalendarDayBlock> = emptyList()
)

/** A booking entry within a calendar day */
data class CalendarDayBooking(
    val id: String = "",
    /** ISO date-time string (serialized from Date) */
    val startTime: String? = null,
    /** ISO date-time string (serialized from Date) */
    val endTime: String? = null,
    val status: String? = null
)

/** A block entry within a calendar day */
data class CalendarDayBlock(
    val id: String = "",
    val startDate: String? = null,
    val endDate: String? = null,
    val reason: String? = null,
    val repeat: String? = null
)

data class AvailabilityData(
    val timezone: String = "America/New_York",
    @SerializedName("start_time")
    val startTime: String = "09:00",
    @SerializedName("end_time")
    val endTime: String = "17:00",
    @SerializedName("available_days")
    val availableDays: List<Int> = listOf(1, 2, 3, 4, 5),
    val dateRange: DateRangePayload? = null,
    @SerializedName("instant_booking")
    val instantBooking: Boolean = false
)

/** A blocked time range as stored on the backend */
data class BackendBlock(
    val id: String = "",
    val startDate: String = "",
    val endDate: String = "",
    val startTime: String? = null,
    val endTime: String? = null,
    val reason: String = "",
    val repeat: String = "none",
    val createdAt: String? = null
)

/** Request body for POST /host/listings/:id/calendar/block */
data class BlockTimeRequest(
    val startDate: String,
    val endDate: String,
    val startTime: String? = null,
    val endTime: String? = null,
    val reason: String = "personal",
    val repeat: String = "none"
)

/** Response from POST /host/listings/:id/calendar/block */
data class BlockTimeResponse(
    val success: Boolean = false,
    val message: String? = null,
    val data: BackendBlock? = null
)

/** Generic success response */
data class GenericSuccessResponse(
    val success: Boolean = false,
    val message: String? = null
)

/** Request body for PATCH /host/listings/:id/availability */
data class UpdateAvailabilityRequest(
    val dateRange: DateRangePayload? = null,
    val blocks: List<BackendBlock> = emptyList()
)

/** Request body for POST /listings/:id/check-availability */
data class CheckAvailabilityRequest(
    val startDate: String,
    val endDate: String
)

/** Response from POST /listings/:id/check-availability */
data class CheckAvailabilityResponse(
    val status: Boolean = false,
    val data: CheckAvailabilityData? = null
)

data class CheckAvailabilityData(
    val available: Boolean = false,
    val reason: String? = null,
    val listing: CheckAvailabilityListingInfo? = null
)

data class CheckAvailabilityListingInfo(
    val bookable: Boolean = false,
    val status: String? = null,
    val moderationStatus: String? = null,
    val timezone: String? = null,
    val blockedDates: List<BackendBlock> = emptyList()
)
