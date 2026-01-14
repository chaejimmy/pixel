package com.pacedream.app.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pacedream.app.core.config.AppConfig
import com.pacedream.app.core.network.ApiClient
import com.pacedream.app.core.network.ApiResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.double
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import timber.log.Timber
import javax.inject.Inject

/**
 * HomeViewModel - Loads 3 sections concurrently
 * 
 * iOS Parity:
 * - Fetch Hourly Spaces, Rent Gear, Split Stays in parallel
 * - Hide sections that fail (don't block UI)
 * - Show warning banner if any section failed
 * - Support pull to refresh
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val apiClient: ApiClient,
    private val appConfig: AppConfig,
    private val json: Json
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()
    
    init {
        loadAllSections()
        // Set default hero image URL (can be fetched from API/config later)
        _uiState.update { 
            it.copy(
                heroImageUrl = "https://images.unsplash.com/photo-1506905925346-21bda4d32df4?w=1200&q=80" // Default scenic image
            )
        }
    }
    
    fun refresh() {
        loadAllSections()
    }
    
    private fun loadAllSections() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true) }
            
            // Fetch all sections concurrently
            val hourlySpacesDeferred = async { fetchHourlySpaces() }
            val rentGearDeferred = async { fetchRentGear() }
            val splitStaysDeferred = async { fetchSplitStays() }
            
            // Wait for all and update state
            val hourlySpaces = hourlySpacesDeferred.await()
            val rentGear = rentGearDeferred.await()
            val splitStays = splitStaysDeferred.await()
            
            _uiState.update {
                it.copy(
                    isRefreshing = false,
                    isLoadingHourlySpaces = false,
                    isLoadingRentGear = false,
                    isLoadingSplitStays = false,
                    hourlySpaces = hourlySpaces.first,
                    rentGear = rentGear.first,
                    splitStays = splitStays.first,
                    hourlySpacesError = hourlySpaces.second,
                    rentGearError = rentGear.second,
                    splitStaysError = splitStays.second
                )
            }
        }
    }
    
    /**
     * Fetch hourly spaces (time-based listings)
     * GET /v1/properties/filter-rentable-items-by-group/time_based?item_type=room
     */
    private suspend fun fetchHourlySpaces(): Pair<List<HomeListingItem>, String?> {
        _uiState.update { it.copy(isLoadingHourlySpaces = true) }
        
        val url = appConfig.buildApiUrl(
            "properties", "filter-rentable-items-by-group", "time_based",
            queryParams = mapOf("item_type" to "room")
        )
        
        return when (val result = apiClient.get(url, includeAuth = false)) {
            is ApiResult.Success -> {
                val items = parseListingsFromResponse(result.data, "time-based")
                Pair(items, null)
            }
            is ApiResult.Failure -> {
                Timber.w("Failed to fetch hourly spaces: ${result.error.message}")
                Pair(emptyList(), result.error.message)
            }
        }
    }
    
    /**
     * Fetch rent gear
     * GET /v1/gear-rentals/get/hourly-rental-gear/tech_gear
     */
    private suspend fun fetchRentGear(): Pair<List<HomeListingItem>, String?> {
        _uiState.update { it.copy(isLoadingRentGear = true) }
        
        val url = appConfig.buildApiUrl("gear-rentals", "get", "hourly-rental-gear", "tech_gear")
        
        return when (val result = apiClient.get(url, includeAuth = false)) {
            is ApiResult.Success -> {
                val items = parseListingsFromResponse(result.data, "gear")
                Pair(items, null)
            }
            is ApiResult.Failure -> {
                Timber.w("Failed to fetch rent gear: ${result.error.message}")
                Pair(emptyList(), result.error.message)
            }
        }
    }
    
    /**
     * Fetch split stays
     * GET /v1/roommate/get/room-stay
     */
    private suspend fun fetchSplitStays(): Pair<List<HomeListingItem>, String?> {
        _uiState.update { it.copy(isLoadingSplitStays = true) }
        
        val url = appConfig.buildApiUrl("roommate", "get", "room-stay")
        
        return when (val result = apiClient.get(url, includeAuth = false)) {
            is ApiResult.Success -> {
                val items = parseListingsFromResponse(result.data, "split-stay")
                Pair(items, null)
            }
            is ApiResult.Failure -> {
                Timber.w("Failed to fetch split stays: ${result.error.message}")
                Pair(emptyList(), result.error.message)
            }
        }
    }
    
    /**
     * Parse listings from JSON response (tolerant parsing)
     * Handles multiple response formats
     */
    private fun parseListingsFromResponse(responseBody: String, type: String): List<HomeListingItem> {
        return try {
            val element = json.parseToJsonElement(responseBody)
            val obj = element.jsonObject
            
            // Find data array in common locations
            val dataArray = obj["data"]?.jsonArray
                ?: obj["items"]?.jsonArray
                ?: (obj["data"] as? JsonObject)?.get("items")?.jsonArray
                ?: return emptyList()
            
            dataArray.mapNotNull { item ->
                try {
                    val itemObj = item.jsonObject
                    HomeListingItem(
                        id = itemObj["_id"]?.jsonPrimitive?.content
                            ?: itemObj["id"]?.jsonPrimitive?.content
                            ?: return@mapNotNull null,
                        title = itemObj["name"]?.jsonPrimitive?.content
                            ?: itemObj["title"]?.jsonPrimitive?.content
                            ?: "Listing",
                        imageUrl = itemObj["images"]?.jsonArray?.firstOrNull()?.jsonPrimitive?.content
                            ?: itemObj["image"]?.jsonPrimitive?.content,
                        location = itemObj["location"]?.let { loc ->
                            when (loc) {
                                is JsonObject -> loc["city"]?.jsonPrimitive?.content
                                else -> loc.jsonPrimitive.content
                            }
                        },
                        price = parsePrice(itemObj),
                        rating = itemObj["rating"]?.jsonPrimitive?.doubleOrNull,
                        type = type
                    )
                } catch (e: Exception) {
                    Timber.w(e, "Failed to parse listing item")
                    null
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to parse listings response")
            emptyList()
        }
    }
    
    private fun parsePrice(obj: JsonObject): String? {
        return try {
            // Try dynamic_price first (for hourly spaces)
            obj["dynamic_price"]?.jsonArray?.firstOrNull()?.jsonObject?.let { price ->
                val priceValue = price["price"]?.jsonPrimitive?.doubleOrNull
                    ?: price["price"]?.jsonPrimitive?.content?.toDoubleOrNull()
                priceValue?.let { formatPrice(it, "hr") }
            }
            // Try pricing object
            ?: obj["pricing"]?.jsonObject?.let { pricing ->
                val hourlyFrom = pricing["hourlyFrom"]?.jsonPrimitive?.doubleOrNull
                    ?: pricing["hourly_from"]?.jsonPrimitive?.doubleOrNull
                val basePrice = pricing["basePrice"]?.jsonPrimitive?.doubleOrNull
                    ?: pricing["base_price"]?.jsonPrimitive?.doubleOrNull
                val frequency = pricing["frequencyLabel"]?.jsonPrimitive?.content
                    ?: pricing["frequency"]?.jsonPrimitive?.content
                    ?: pricing["unit"]?.jsonPrimitive?.content
                
                val amount = hourlyFrom ?: basePrice
                val unit = frequency?.lowercase()?.takeIf { it.isNotBlank() } ?: "hr"
                amount?.let { formatPrice(it, unit) }
            }
            // Try price object
            ?: obj["price"]?.let { price ->
                when (price) {
                    is JsonObject -> {
                        val amount = price["amount"]?.jsonPrimitive?.doubleOrNull
                            ?: price["amount"]?.jsonPrimitive?.content?.toDoubleOrNull()
                        val unit = price["unit"]?.jsonPrimitive?.content?.lowercase() ?: "hr"
                        amount?.let { formatPrice(it, unit) }
                    }
                    else -> {
                        val priceValue = price.jsonPrimitive.doubleOrNull
                            ?: price.jsonPrimitive.content.toDoubleOrNull()
                        priceValue?.let { formatPrice(it, "hr") }
                    }
                }
            }
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Format price to match iOS format: "$12/hr" (no spaces, lowercase unit)
     */
    private fun formatPrice(amount: Double, unit: String): String {
        val formattedAmount = if (amount == amount.toInt().toDouble()) {
            amount.toInt().toString()
        } else {
            "%.2f".format(amount).trimEnd('0').trimEnd('.')
        }
        val normalizedUnit = unit.lowercase().trim()
        return "$$formattedAmount/$normalizedUnit"
    }
}

/**
 * Home UI State
 */
data class HomeUiState(
    val isRefreshing: Boolean = false,
    val isLoadingHourlySpaces: Boolean = true,
    val isLoadingRentGear: Boolean = true,
    val isLoadingSplitStays: Boolean = true,
    val hourlySpaces: List<HomeListingItem> = emptyList(),
    val rentGear: List<HomeListingItem> = emptyList(),
    val splitStays: List<HomeListingItem> = emptyList(),
    val hourlySpacesError: String? = null,
    val rentGearError: String? = null,
    val splitStaysError: String? = null,
    val heroImageUrl: String? = null // Hero background image URL
) {
    val isLoading: Boolean
        get() = isLoadingHourlySpaces || isLoadingRentGear || isLoadingSplitStays
    
    val hasErrors: Boolean
        get() = hourlySpacesError != null || rentGearError != null || splitStaysError != null
    
    val isEmpty: Boolean
        get() = hourlySpaces.isEmpty() && rentGear.isEmpty() && splitStays.isEmpty()
}


