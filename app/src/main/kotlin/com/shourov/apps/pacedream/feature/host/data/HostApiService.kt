package com.shourov.apps.pacedream.feature.host.data

import com.shourov.apps.pacedream.model.BookingModel
import com.shourov.apps.pacedream.model.Property
import retrofit2.Response
import retrofit2.http.*

interface HostApiService {
    
    // Dashboard
    @GET("host/dashboard")
    suspend fun getHostDashboard(): Response<HostDashboardData>
    
    // Listings
    @GET("host/listings")
    suspend fun getHostListings(
        @Query("filter") filter: String? = null,
        @Query("sort") sort: String? = null
    ): Response<List<Property>>
    
    @POST("host/listings")
    suspend fun createListing(@Body listing: Property): Response<Property>
    
    @PUT("host/listings/{id}")
    suspend fun updateListing(
        @Path("id") id: String,
        @Body listing: Property
    ): Response<Property>
    
    @DELETE("host/listings/{id}")
    suspend fun deleteListing(@Path("id") id: String): Response<Unit>
    
    // Bookings
    @GET("host/bookings")
    suspend fun getHostBookings(
        @Query("status") status: String? = null
    ): Response<List<BookingModel>>
    
    @PUT("host/bookings/{id}/status")
    suspend fun updateBookingStatus(
        @Path("id") id: String,
        @Body statusUpdate: BookingStatusUpdate
    ): Response<BookingModel>
    
    // Earnings
    @GET("host/earnings")
    suspend fun getHostEarnings(
        @Query("timeRange") timeRange: String? = null
    ): Response<HostEarningsData>
    
    @POST("host/earnings/withdraw")
    suspend fun requestWithdrawal(@Body withdrawal: WithdrawalRequest): Response<WithdrawalResponse>
    
    // Analytics
    @GET("host/analytics")
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
