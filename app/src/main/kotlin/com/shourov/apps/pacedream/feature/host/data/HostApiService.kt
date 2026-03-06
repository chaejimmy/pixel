package com.shourov.apps.pacedream.feature.host.data

import com.shourov.apps.pacedream.core.network.ApiEndPoints
import com.shourov.apps.pacedream.model.BookingModel
import com.shourov.apps.pacedream.model.Property
import retrofit2.Response
import retrofit2.http.*

interface HostApiService {

    // Dashboard
    @GET("host/dashboard")
    suspend fun getHostDashboard(): Response<HostDashboardData>

    // Listings (using ApiEndPoints constants)
    @GET(ApiEndPoints.HOST_GET_LISTINGS)
    suspend fun getHostListings(
        @Query("filter") filter: String? = null,
        @Query("sort") sort: String? = null
    ): Response<List<Property>>

    @POST(ApiEndPoints.HOST_CREATE_LISTING)
    suspend fun createListing(@Body listing: CreateListingRequest): Response<Property>

    @PUT(ApiEndPoints.HOST_UPDATE_LISTING)
    suspend fun updateListing(
        @Path("listingId") id: String,
        @Body listing: Property
    ): Response<Property>

    @DELETE(ApiEndPoints.HOST_DELETE_LISTING)
    suspend fun deleteListing(@Path("listingId") id: String): Response<Unit>

    // Bookings (using ApiEndPoints constants)
    @GET(ApiEndPoints.HOST_GET_BOOKINGS)
    suspend fun getHostBookings(
        @Query("status") status: String? = null
    ): Response<List<BookingModel>>

    @POST(ApiEndPoints.HOST_ACCEPT_BOOKING)
    suspend fun acceptBooking(@Path("bookingId") id: String): Response<BookingModel>

    @POST(ApiEndPoints.HOST_DECLINE_BOOKING)
    suspend fun declineBooking(
        @Path("bookingId") id: String,
        @Body reason: BookingStatusUpdate
    ): Response<BookingModel>

    @PATCH("host/bookings/{bookingId}")
    suspend fun updateBookingStatus(
        @Path("bookingId") bookingId: String,
        @Body body: BookingStatusUpdate
    ): Response<BookingModel>

    // Earnings (using ApiEndPoints constants)
    @GET(ApiEndPoints.HOST_GET_EARNINGS)
    suspend fun getHostEarnings(
        @Query("timeRange") timeRange: String? = null
    ): Response<HostEarningsData>

    @POST("host/earnings/withdraw")
    suspend fun requestWithdrawal(@Body withdrawal: WithdrawalRequest): Response<WithdrawalResponse>

    // Analytics (using ApiEndPoints constants)
    @GET(ApiEndPoints.HOST_GET_ANALYTICS)
    suspend fun getHostAnalytics(
        @Query("timeRange") timeRange: String? = null
    ): Response<HostAnalyticsData>

    // Stripe Connect - Payouts
    @GET("host/payouts/status")
    suspend fun getPayoutStatus(): Response<PayoutStatus>

    @POST("host/payouts/create-onboarding-link")
    suspend fun createOnboardingLink(): Response<AccountLink>

    @POST("host/payouts/create-login-link")
    suspend fun createLoginLink(): Response<LoginLink>

    @GET("host/payouts/methods")
    suspend fun getPayoutMethods(): Response<List<PayoutMethod>>

    @POST("host/payouts/methods/set-primary")
    suspend fun setPrimaryPayoutMethod(@Body body: SetPrimaryMethodRequest): Response<Unit>

    // Stripe Connect - Balance & Transfers
    @GET("host/stripe/balance")
    suspend fun getStripeBalance(): Response<ConnectBalance>

    @GET("host/stripe/transfers")
    suspend fun getStripeTransfers(@Query("limit") limit: Int = 20): Response<List<Transfer>>

    @GET("host/stripe/payouts")
    suspend fun getStripePayouts(@Query("limit") limit: Int = 20): Response<List<Payout>>

    @POST("host/stripe/payouts/create")
    suspend fun createPayout(@Body request: CreatePayoutRequest): Response<Payout>

    @GET("host/stripe/connect/status")
    suspend fun getConnectAccountStatus(): Response<ConnectAccount>

    @POST("host/stripe/connect/create")
    suspend fun createConnectAccount(@Body request: CreateConnectAccountRequest): Response<ConnectAccount>
}

data class BookingStatusUpdate(
    val status: String,
    val reason: String? = null
)

data class WithdrawalRequest(
    val amount: Double,
    val paymentMethod: String
)

data class WithdrawalResponse(
    val id: String,
    val status: String,
    val processedAt: String? = null
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

data class HostAnalyticsData(
    val views: Int = 0,
    val bookings: Int = 0,
    val revenue: Double = 0.0,
    val occupancyRate: Double = 0.0,
    val averageRating: Double = 0.0,
    val conversionRate: Double = 0.0,
    val timeRange: String = "Month"
)

/**
 * Request body for creating a listing.
 * Matches the web platform payload structure from ListingWizard.tsx.
 */
data class CreateListingRequest(
    val listing_type: String,
    val subCategory: String,
    val title: String,
    val description: String,
    val summary: String,
    val price: Double,
    val pricing_type: String,
    val prices: PricesPayload,
    val pricing: PricingPayload,
    val address: String,
    val amenities: List<String> = emptyList(),
    val images: List<String> = emptyList(),
    val location: LocationPayload,
    val available: Boolean = true,
    val durations: List<Int>? = null,
    val availability: AvailabilityPayload? = null,
)

data class PricesPayload(
    val hour: Double = 0.0,
    val day: Double = 0.0,
    val week: Double = 0.0,
    val month: Double = 0.0,
)

data class PricingPayload(
    val base_price: Double,
    val unit: String,
    val currency: String = "USD",
)

data class LocationPayload(
    val street_address: String = "",
    val street: String = "",
    val address: String = "",
    val city: String = "",
    val state: String = "",
    val country: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
)

data class AvailabilityPayload(
    val start_time: String = "09:00",
    val end_time: String = "17:00",
    val available_days: List<Int> = listOf(1, 2, 3, 4, 5),
    val timezone: String = "America/New_York",
    val instant_booking: Boolean = false,
)
