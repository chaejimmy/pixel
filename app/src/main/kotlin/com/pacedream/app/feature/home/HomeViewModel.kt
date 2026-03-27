package com.pacedream.app.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pacedream.app.core.auth.AuthState
import com.pacedream.app.core.auth.SessionManager
import com.pacedream.app.core.config.AppConfig
import com.pacedream.app.core.network.ApiClient
import com.pacedream.app.core.network.ApiResult
import com.pacedream.app.feature.listingdetail.ListingWishlistRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
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
    private val sessionManager: SessionManager,
    private val wishlistRepository: ListingWishlistRepository,
    private val json: Json
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    sealed class Effect {
        data object ShowAuthRequired : Effect()
        data class ShowToast(val message: String) : Effect()
    }

    private val _effects = Channel<Effect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    init {
        loadAllSections()
        loadFavorites()
        // Set default hero image URL (can be fetched from API/config later)
        _uiState.update {
            it.copy(
                heroImageUrl = "https://images.unsplash.com/photo-1506905925346-21bda4d32df4?w=1200&q=80" // Default scenic image
            )
        }
    }

    fun refresh() {
        loadAllSections()
        loadFavorites()
    }

    fun isFavorite(listingId: String): Boolean {
        return listingId in _uiState.value.favoriteListingIds
    }

    fun toggleFavorite(listingId: String) {
        viewModelScope.launch {
            if (sessionManager.authState.value != AuthState.Authenticated) {
                _effects.send(Effect.ShowAuthRequired)
                return@launch
            }

            val currentlyFavorite = listingId in _uiState.value.favoriteListingIds
            val previousIds = _uiState.value.favoriteListingIds

            // Optimistic update
            _uiState.update {
                it.copy(
                    favoriteListingIds = if (currentlyFavorite) previousIds - listingId else previousIds + listingId
                )
            }

            val result = if (currentlyFavorite) {
                wishlistRepository.removeFromWishlist(listingId, null)
            } else {
                wishlistRepository.addToWishlist(listingId)
            }

            when (result) {
                is ApiResult.Success -> {
                    val isFav = result.data.isFavorite
                    _uiState.update {
                        it.copy(
                            favoriteListingIds = if (isFav) it.favoriteListingIds + listingId else it.favoriteListingIds - listingId
                        )
                    }
                    _effects.send(Effect.ShowToast(if (isFav) "Saved to Favorites" else "Removed from Favorites"))
                }
                is ApiResult.Failure -> {
                    // Revert
                    _uiState.update { it.copy(favoriteListingIds = previousIds) }
                    _effects.send(Effect.ShowToast("Failed to update favorite"))
                }
            }
        }
    }

    private fun loadFavorites() {
        viewModelScope.launch {
            if (sessionManager.authState.value != AuthState.Authenticated) return@launch

            val url = appConfig.buildApiUrl("account", "wishlist")
            when (val result = apiClient.get(url, includeAuth = true)) {
                is ApiResult.Success -> {
                    val ids = parseFavoriteIds(result.data)
                    _uiState.update { it.copy(favoriteListingIds = ids) }
                }
                is ApiResult.Failure -> {
                    Timber.w("Failed to load favorites: ${result.error.message}")
                }
            }
        }
    }

    private fun parseFavoriteIds(responseBody: String): Set<String> {
        return try {
            val element = json.parseToJsonElement(responseBody)
            val obj = element.jsonObject
            val dataElement = obj["data"]
            val itemsArray = when {
                dataElement is JsonObject -> {
                    dataElement["items"]?.jsonArray ?: dataElement["wishlist"]?.jsonArray
                }
                dataElement is JsonArray -> dataElement
                else -> null
            }
                ?: obj["items"]?.jsonArray
                ?: obj["wishlist"]?.jsonArray
                ?: return emptySet()

            itemsArray.mapNotNull { item ->
                try {
                    val itemObj = item.jsonObject
                    val listingData = itemObj["listing"]?.jsonObject
                        ?: itemObj["item"]?.jsonObject
                        ?: itemObj
                    listingData["_id"]?.jsonPrimitive?.content
                        ?: listingData["id"]?.jsonPrimitive?.content
                } catch (_: Exception) { null }
            }.toSet()
        } catch (_: Exception) { emptySet() }
    }

    fun selectCategory(category: String) {
        _uiState.update { it.copy(selectedCategory = category) }
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
     * Fetch hourly spaces (USE share type - matches website endpoint)
     * GET /v1/poc/listings?shareType=USE&status=published&limit=24&skip_pagination=true
     */
    private suspend fun fetchHourlySpaces(): Pair<List<HomeListingItem>, String?> {
        _uiState.update { it.copy(isLoadingHourlySpaces = true) }

        val url = appConfig.buildApiUrl(
            "poc", "listings",
            queryParams = mapOf(
                "shareType" to "USE",
                "status" to "published",
                "limit" to "24",
                "skip_pagination" to "true"
            )
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
     * Fetch rent gear (BORROW share type - matches website endpoint)
     * GET /v1/poc/listings?shareType=BORROW&status=published&limit=24&skip_pagination=true
     */
    private suspend fun fetchRentGear(): Pair<List<HomeListingItem>, String?> {
        _uiState.update { it.copy(isLoadingRentGear = true) }

        val url = appConfig.buildApiUrl(
            "poc", "listings",
            queryParams = mapOf(
                "shareType" to "BORROW",
                "status" to "published",
                "limit" to "24",
                "skip_pagination" to "true"
            )
        )

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
     * Fetch split stays (SPLIT share type - matches website endpoint)
     * GET /v1/poc/listings?shareType=SPLIT&status=published&limit=24&skip_pagination=true
     */
    private suspend fun fetchSplitStays(): Pair<List<HomeListingItem>, String?> {
        _uiState.update { it.copy(isLoadingSplitStays = true) }

        val url = appConfig.buildApiUrl(
            "poc", "listings",
            queryParams = mapOf(
                "shareType" to "SPLIT",
                "status" to "published",
                "limit" to "24",
                "skip_pagination" to "true"
            )
        )

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

            // Find data array in common locations (tolerate multiple response shapes)
            val dataArray = (obj["data"] as? JsonArray)
                ?: (obj["results"] as? JsonArray)
                ?: (obj["items"] as? JsonArray)
                ?: (obj["data"] as? JsonObject)?.get("items")?.jsonArray
                ?: (obj["data"] as? JsonObject)?.get("results")?.jsonArray
                ?: (obj["data"] as? JsonObject)?.get("listings")?.jsonArray
                ?: return emptyList()

            dataArray.mapNotNull { item ->
                try {
                    val itemObj = item.jsonObject
                    HomeListingItem(
                        id = (itemObj["_id"] as? kotlinx.serialization.json.JsonPrimitive)?.content
                            ?: (itemObj["id"] as? kotlinx.serialization.json.JsonPrimitive)?.content
                            ?: return@mapNotNull null,
                        title = (itemObj["name"] as? kotlinx.serialization.json.JsonPrimitive)?.content
                            ?: (itemObj["title"] as? kotlinx.serialization.json.JsonPrimitive)?.content
                            ?: "Listing",
                        imageUrl = (itemObj["images"] as? JsonArray)?.firstOrNull()?.jsonPrimitive?.content
                            ?: (itemObj["gallery"] as? JsonObject)?.get("images")?.jsonArray?.firstOrNull()?.jsonPrimitive?.content
                            ?: (itemObj["gallery"] as? JsonObject)?.get("thumbnail")?.jsonPrimitive?.content
                            ?: (itemObj["primaryImage"] as? kotlinx.serialization.json.JsonPrimitive)?.content
                            ?: (itemObj["image"] as? kotlinx.serialization.json.JsonPrimitive)?.content,
                        location = itemObj["location"]?.let { loc ->
                            when (loc) {
                                is JsonObject -> {
                                    val city = (loc["city"] as? kotlinx.serialization.json.JsonPrimitive)?.content
                                    val state = (loc["state"] as? kotlinx.serialization.json.JsonPrimitive)?.content
                                    listOfNotNull(city, state).joinToString(", ").ifBlank { null }
                                }
                                is kotlinx.serialization.json.JsonPrimitive -> loc.content
                                else -> null
                            }
                        },
                        price = parsePrice(itemObj),
                        rating = (itemObj["rating"] as? kotlinx.serialization.json.JsonPrimitive)?.doubleOrNull,
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
            // Extract standalone frequency from top-level pricingUnit (backend list endpoint)
            // or pricing object fields. iOS reads pricingUnit for list views.
            val standaloneFrequency = obj["pricingUnit"]?.jsonPrimitive?.content?.let { formatPriceUnit(it) }
                ?: obj["frequency"]?.jsonPrimitive?.content?.let { formatPriceUnit(it) }
                ?: (obj["pricing"] as? JsonObject)?.let { p ->
                    p["pricing_type"]?.jsonPrimitive?.content?.let { formatPriceUnit(it) }
                        ?: p["frequencyLabel"]?.jsonPrimitive?.content?.let { formatPriceUnit(it) }
                        ?: p["frequency"]?.jsonPrimitive?.content?.let { formatPriceUnit(it) }
                        ?: p["unit"]?.jsonPrimitive?.content?.let { formatPriceUnit(it) }
                }

            // Try dynamic_price first (for hourly spaces)
            (obj["dynamic_price"] as? JsonArray)?.firstOrNull()?.jsonObject?.let { price ->
                val priceValue = price["price"]?.jsonPrimitive?.doubleOrNull
                    ?: price["price"]?.jsonPrimitive?.content?.toDoubleOrNull()
                val freq = price["frequency"]?.jsonPrimitive?.content?.let { formatPriceUnit(it) }
                    ?: standaloneFrequency
                priceValue?.let { formatPrice(it, freq) }
            }
            // Try pricing object
            ?: (obj["pricing"] as? JsonObject)?.let { pricing ->
                val hourlyFrom = pricing["hourlyFrom"]?.jsonPrimitive?.doubleOrNull
                    ?: pricing["hourly_from"]?.jsonPrimitive?.doubleOrNull
                val basePrice = pricing["basePrice"]?.jsonPrimitive?.doubleOrNull
                    ?: pricing["base_price"]?.jsonPrimitive?.doubleOrNull
                val frequency = pricing["frequencyLabel"]?.jsonPrimitive?.content?.let { formatPriceUnit(it) }
                    ?: pricing["frequency"]?.jsonPrimitive?.content?.let { formatPriceUnit(it) }
                    ?: pricing["pricing_type"]?.jsonPrimitive?.content?.let { formatPriceUnit(it) }
                    ?: pricing["unit"]?.jsonPrimitive?.content?.let { formatPriceUnit(it) }
                    ?: standaloneFrequency

                val amount = hourlyFrom ?: basePrice
                amount?.let { formatPrice(it, frequency) }
            }
            // Try price as array of pricing objects (RentableItem format)
            ?: (obj["price"] as? JsonArray)?.firstOrNull()?.jsonObject?.let { price ->
                val amount = price["amount"]?.jsonPrimitive?.doubleOrNull
                val frequency = price["frequency"]?.jsonPrimitive?.content?.let { formatPriceUnit(it) }
                    ?: standaloneFrequency
                amount?.let { formatPrice(it, frequency) }
            }
            // Try price as object
            ?: (obj["price"] as? JsonObject)?.let { price ->
                val amount = price["amount"]?.jsonPrimitive?.doubleOrNull
                    ?: price["amount"]?.jsonPrimitive?.content?.toDoubleOrNull()
                val freq = price["frequency"]?.jsonPrimitive?.content?.let { formatPriceUnit(it) }
                    ?: price["unit"]?.jsonPrimitive?.content?.let { formatPriceUnit(it) }
                    ?: standaloneFrequency
                amount?.let { formatPrice(it, freq) }
            }
            // Try price as primitive value
            ?: (obj["price"] as? kotlinx.serialization.json.JsonPrimitive)?.let { price ->
                val priceValue = price.doubleOrNull
                    ?: price.content.toDoubleOrNull()
                priceValue?.let { formatPrice(it, standaloneFrequency) }
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Normalize backend frequency strings to short display labels matching iOS.
     * Maps backend values like "HOUR", "hourly", "daily", "MONTH" etc.
     */
    private fun formatPriceUnit(frequency: String): String {
        return when (frequency.lowercase().trim()) {
            "hourly", "hour", "hr" -> "hr"
            "daily", "day" -> "day"
            "weekly", "week", "wk" -> "wk"
            "monthly", "month", "mo" -> "mo"
            "once" -> "total"
            else -> frequency.lowercase()
        }
    }

    /**
     * Format price to match iOS format: "$12/hr" (no spaces, lowercase unit)
     * When unit is null, displays price without suffix (e.g. "$12")
     */
    private fun formatPrice(amount: Double, unit: String?): String {
        val formattedAmount = if (amount == amount.toInt().toDouble()) {
            amount.toInt().toString()
        } else {
            "%.2f".format(amount).trimEnd('0').trimEnd('.')
        }
        return if (unit != null) "$$formattedAmount/$unit" else "$$formattedAmount"
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
    val heroImageUrl: String? = null,
    val selectedCategory: String = "All",
    val favoriteListingIds: Set<String> = emptySet()
) {
    val isLoading: Boolean
        get() = isLoadingHourlySpaces || isLoadingRentGear || isLoadingSplitStays

    val hasErrors: Boolean
        get() = hourlySpacesError != null || rentGearError != null || splitStaysError != null

    val isEmpty: Boolean
        get() = filteredHourlySpaces.isEmpty() && filteredRentGear.isEmpty() && filteredSplitStays.isEmpty()

    /** Filter listings by selected category (case-insensitive title match) */
    val filteredHourlySpaces: List<HomeListingItem>
        get() = if (selectedCategory == "All") hourlySpaces
                else hourlySpaces.filter { it.title.contains(selectedCategory, ignoreCase = true) ||
                    it.location?.contains(selectedCategory, ignoreCase = true) == true }

    val filteredRentGear: List<HomeListingItem>
        get() = if (selectedCategory == "All") rentGear
                else rentGear.filter { it.title.contains(selectedCategory, ignoreCase = true) ||
                    it.location?.contains(selectedCategory, ignoreCase = true) == true }

    val filteredSplitStays: List<HomeListingItem>
        get() = if (selectedCategory == "All") splitStays
                else splitStays.filter { it.title.contains(selectedCategory, ignoreCase = true) ||
                    it.location?.contains(selectedCategory, ignoreCase = true) == true }
}


