package com.pacedream.app.feature.host

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pacedream.app.core.auth.SessionManager
import com.pacedream.app.core.auth.TokenStorage
import com.pacedream.app.core.config.AppConfig
import com.pacedream.app.core.network.ApiClient
import com.pacedream.app.core.network.ApiResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import timber.log.Timber
import javax.inject.Inject

/**
 * HostHomeViewModel - Host dashboard data management
 *
 * iOS Parity:
 * - Fetches host stats (listings count, bookings count, total earnings)
 * - Fetches host listings for "Your Listings" section
 * - Fetches recent bookings for "Recent Bookings" section
 * - Pull-to-refresh support
 * - Switch to guest mode via TokenStorage
 */
@HiltViewModel
class HostHomeViewModel @Inject constructor(
    private val apiClient: ApiClient,
    private val appConfig: AppConfig,
    private val sessionManager: SessionManager,
    private val tokenStorage: TokenStorage,
    private val json: Json
) : ViewModel() {

    private val _uiState = MutableStateFlow(HostHomeUiState())
    val uiState: StateFlow<HostHomeUiState> = _uiState.asStateFlow()

    init {
        loadDashboard()
    }

    /**
     * Load all dashboard data in parallel
     */
    fun loadDashboard() {
        _uiState.update { it.copy(isLoading = true, error = null) }

        viewModelScope.launch {
            try {
                // Fetch host stats, listings, and bookings concurrently
                val statsJob = launch { fetchHostStats() }
                val listingsJob = launch { fetchHostListings() }
                val bookingsJob = launch { fetchRecentBookings() }

                statsJob.join()
                listingsJob.join()
                bookingsJob.join()

                _uiState.update { it.copy(isLoading = false, isRefreshing = false) }
            } catch (e: Exception) {
                Timber.e(e, "Failed to load host dashboard")
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isRefreshing = false,
                        error = e.message ?: "Failed to load dashboard"
                    )
                }
            }
        }
    }

    /**
     * Pull-to-refresh handler
     */
    fun refresh() {
        _uiState.update { it.copy(isRefreshing = true) }
        loadDashboard()
    }

    /**
     * Switch from host mode to guest mode
     */
    fun switchToGuestMode() {
        tokenStorage.isHostMode = false
    }

    /**
     * Sign out the current user
     */
    fun signOut() {
        sessionManager.signOut()
    }

    /**
     * Fetch host statistics (total listings, bookings, earnings)
     */
    private suspend fun fetchHostStats() {
        val url = appConfig.buildApiUrl("host", "stats")
        when (val result = apiClient.get(url, includeAuth = true)) {
            is ApiResult.Success -> {
                try {
                    val element = json.parseToJsonElement(result.data)
                    val obj = element.jsonObject
                    val data = obj["data"]?.jsonObject ?: obj

                    _uiState.update {
                        it.copy(
                            totalListings = data["totalListings"]?.jsonPrimitive?.intOrNull
                                ?: data["listingsCount"]?.jsonPrimitive?.intOrNull ?: 0,
                            totalBookings = data["totalBookings"]?.jsonPrimitive?.intOrNull
                                ?: data["bookingsCount"]?.jsonPrimitive?.intOrNull ?: 0,
                            totalEarnings = data["totalEarnings"]?.jsonPrimitive?.doubleOrNull
                                ?: data["earnings"]?.jsonPrimitive?.doubleOrNull ?: 0.0
                        )
                    }
                } catch (e: Exception) {
                    Timber.e(e, "Failed to parse host stats")
                }
            }
            is ApiResult.Failure -> {
                Timber.e("Failed to fetch host stats: ${result.error.message}")
            }
        }
    }

    /**
     * Fetch host's active listings
     */
    private suspend fun fetchHostListings() {
        val url = appConfig.buildApiUrl("host", "listings",
            queryParams = mapOf("limit" to "10", "status" to "active")
        )
        when (val result = apiClient.get(url, includeAuth = true)) {
            is ApiResult.Success -> {
                try {
                    val element = json.parseToJsonElement(result.data)
                    val obj = element.jsonObject
                    val listingsArray = obj["data"]?.jsonArray
                        ?: obj["listings"]?.jsonArray

                    val listings = listingsArray?.mapNotNull { item ->
                        val listingObj = item.jsonObject
                        HostListing(
                            id = listingObj["_id"]?.jsonPrimitive?.content
                                ?: listingObj["id"]?.jsonPrimitive?.content ?: return@mapNotNull null,
                            title = listingObj["title"]?.jsonPrimitive?.content ?: "Untitled",
                            imageUrl = listingObj["imageUrl"]?.jsonPrimitive?.content
                                ?: listingObj["image"]?.jsonPrimitive?.content
                                ?: listingObj["images"]?.jsonArray?.firstOrNull()?.jsonPrimitive?.content,
                            price = listingObj["price"]?.jsonPrimitive?.doubleOrNull ?: 0.0,
                            priceUnit = listingObj["priceUnit"]?.jsonPrimitive?.content
                                ?: listingObj["pricePer"]?.jsonPrimitive?.content ?: "night",
                            rating = listingObj["rating"]?.jsonPrimitive?.doubleOrNull
                                ?: listingObj["averageRating"]?.jsonPrimitive?.doubleOrNull,
                            reviewCount = listingObj["reviewCount"]?.jsonPrimitive?.intOrNull
                                ?: listingObj["reviewsCount"]?.jsonPrimitive?.intOrNull ?: 0,
                            status = listingObj["status"]?.jsonPrimitive?.content ?: "active",
                            location = listingObj["location"]?.jsonPrimitive?.content
                                ?: listingObj["address"]?.jsonPrimitive?.content
                        )
                    } ?: emptyList()

                    _uiState.update { it.copy(listings = listings) }
                } catch (e: Exception) {
                    Timber.e(e, "Failed to parse host listings")
                }
            }
            is ApiResult.Failure -> {
                Timber.e("Failed to fetch host listings: ${result.error.message}")
            }
        }
    }

    /**
     * Fetch recent bookings for the host
     */
    private suspend fun fetchRecentBookings() {
        val url = appConfig.buildApiUrl("host", "bookings",
            queryParams = mapOf("limit" to "5", "sort" to "-createdAt")
        )
        when (val result = apiClient.get(url, includeAuth = true)) {
            is ApiResult.Success -> {
                try {
                    val element = json.parseToJsonElement(result.data)
                    val obj = element.jsonObject
                    val bookingsArray = obj["data"]?.jsonArray
                        ?: obj["bookings"]?.jsonArray

                    val bookings = bookingsArray?.mapNotNull { item ->
                        val bookingObj = item.jsonObject
                        HostBooking(
                            id = bookingObj["_id"]?.jsonPrimitive?.content
                                ?: bookingObj["id"]?.jsonPrimitive?.content ?: return@mapNotNull null,
                            guestName = bookingObj["guestName"]?.jsonPrimitive?.content
                                ?: bookingObj["guest"]?.jsonObject?.let { guest ->
                                    val first = guest["firstName"]?.jsonPrimitive?.content ?: ""
                                    val last = guest["lastName"]?.jsonPrimitive?.content ?: ""
                                    "$first $last".trim()
                                } ?: "Guest",
                            listingTitle = bookingObj["listingTitle"]?.jsonPrimitive?.content
                                ?: bookingObj["listing"]?.jsonObject?.get("title")?.jsonPrimitive?.content
                                ?: "Listing",
                            checkIn = bookingObj["checkIn"]?.jsonPrimitive?.content
                                ?: bookingObj["startDate"]?.jsonPrimitive?.content ?: "",
                            checkOut = bookingObj["checkOut"]?.jsonPrimitive?.content
                                ?: bookingObj["endDate"]?.jsonPrimitive?.content ?: "",
                            status = bookingObj["status"]?.jsonPrimitive?.content ?: "pending",
                            totalAmount = bookingObj["totalAmount"]?.jsonPrimitive?.doubleOrNull
                                ?: bookingObj["total"]?.jsonPrimitive?.doubleOrNull ?: 0.0
                        )
                    } ?: emptyList()

                    _uiState.update { it.copy(bookings = bookings) }
                } catch (e: Exception) {
                    Timber.e(e, "Failed to parse host bookings")
                }
            }
            is ApiResult.Failure -> {
                Timber.e("Failed to fetch host bookings: ${result.error.message}")
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// UI State & Data Models
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Host Dashboard UI State
 */
data class HostHomeUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val error: String? = null,

    // Stats
    val totalListings: Int = 0,
    val totalBookings: Int = 0,
    val totalEarnings: Double = 0.0,

    // Data
    val listings: List<HostListing> = emptyList(),
    val bookings: List<HostBooking> = emptyList()
)

/**
 * Host listing model
 */
data class HostListing(
    val id: String,
    val title: String,
    val imageUrl: String? = null,
    val price: Double = 0.0,
    val priceUnit: String = "night",
    val rating: Double? = null,
    val reviewCount: Int = 0,
    val status: String = "active",
    val location: String? = null
)

/**
 * Host booking model
 */
data class HostBooking(
    val id: String,
    val guestName: String,
    val listingTitle: String,
    val checkIn: String,
    val checkOut: String,
    val status: String = "pending",
    val totalAmount: Double = 0.0
)
