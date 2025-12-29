package com.shourov.apps.pacedream.feature.host.data

import com.shourov.apps.pacedream.model.BookingModel
import com.shourov.apps.pacedream.model.Property
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HostRepository @Inject constructor(
    private val hostApiService: HostApiService
) {
    
    // Dashboard
    suspend fun getHostDashboard(): Result<HostDashboardData> {
        return try {
            val response = hostApiService.getHostDashboard()
            if (response.isSuccessful) {
                Result.success(response.body() ?: HostDashboardData())
            } else {
                Result.failure(Exception("Failed to fetch dashboard data: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Listings
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
    
    suspend fun createListing(listing: Property): Result<Property> {
        return try {
            val response = hostApiService.createListing(listing)
            if (response.isSuccessful) {
                Result.success(response.body() ?: listing)
            } else {
                Result.failure(Exception("Failed to create listing: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
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
    
    // Bookings
    suspend fun getHostBookings(status: String? = null): Result<List<BookingModel>> {
        return try {
            val response = hostApiService.getHostBookings(status)
            if (response.isSuccessful) {
                Result.success(response.body() ?: emptyList())
            } else {
                Result.failure(Exception("Failed to fetch bookings: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun updateBookingStatus(id: String, status: String, reason: String? = null): Result<BookingModel> {
        return try {
            val response = hostApiService.updateBookingStatus(id, BookingStatusUpdate(status, reason))
            if (response.isSuccessful) {
                Result.success(response.body() ?: throw Exception("Empty response"))
            } else {
                Result.failure(Exception("Failed to update booking status: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Earnings
    suspend fun getHostEarnings(timeRange: String? = null): Result<HostEarningsData> {
        return try {
            val response = hostApiService.getHostEarnings(timeRange)
            if (response.isSuccessful) {
                Result.success(response.body() ?: HostEarningsData())
            } else {
                Result.failure(Exception("Failed to fetch earnings: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun requestWithdrawal(amount: Double, paymentMethod: String): Result<WithdrawalResponse> {
        return try {
            val response = hostApiService.requestWithdrawal(WithdrawalRequest(amount, paymentMethod))
            if (response.isSuccessful) {
                Result.success(response.body() ?: throw Exception("Empty response"))
            } else {
                Result.failure(Exception("Failed to request withdrawal: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Analytics
    suspend fun getHostAnalytics(timeRange: String? = null): Result<HostAnalyticsData> {
        return try {
            val response = hostApiService.getHostAnalytics(timeRange)
            if (response.isSuccessful) {
                Result.success(response.body() ?: HostAnalyticsData(
                    views = 0,
                    bookings = 0,
                    revenue = 0.0,
                    occupancyRate = 0.0,
                    averageRating = 0.0,
                    conversionRate = 0.0,
                    timeRange = timeRange ?: "Month"
                ))
            } else {
                Result.failure(Exception("Failed to fetch analytics: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
