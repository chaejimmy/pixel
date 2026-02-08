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
    suspend fun createListing(@Body listing: Property): Response<Property>

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

data class HostAnalyticsData(
    val views: Int = 0,
    val bookings: Int = 0,
    val revenue: Double = 0.0,
    val occupancyRate: Double = 0.0,
    val averageRating: Double = 0.0,
    val conversionRate: Double = 0.0,
    val timeRange: String = "Month"
)
