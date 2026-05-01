package com.pacedream.app.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pacedream.app.core.auth.AuthState
import com.pacedream.app.core.auth.SessionManager
import com.pacedream.app.core.config.AppConfig
import com.pacedream.app.core.network.ApiClient
import com.pacedream.app.core.network.ApiResult
import com.pacedream.app.feature.listing.ListingPriceFormatter
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
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import timber.log.Timber
import javax.inject.Inject

/**
 * HomeViewModel - Loads 3 sections concurrently
 *
 * iOS/Web Parity:
 * - Fetch Spaces, Items, Services in parallel
 * - Spaces and Services both come from shareType=USE, split by shareCategory
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
        // heroImageUrl is intentionally left null until the backend / remote config
        // returns a branded asset. Previously we seeded a hardcoded Unsplash URL,
        // which shipped a third-party CDN image in production UI with no content
        // licence review and a trust-breaking "stock photo" feel. The hero header
        // renders a brand gradient fine without a photo, so null is preferable
        // to a placeholder.
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

            // Clear any previous favorites error on retry
            _uiState.update { it.copy(favoritesError = null) }

            // Try primary endpoint first (matches iOS), fallback to legacy
            val primaryUrl = appConfig.buildApiUrl("wishlists")
            val primary = apiClient.get(primaryUrl, includeAuth = true)
            if (primary is ApiResult.Success) {
                val ids = parseFavoriteIds(primary.data)
                if (ids.isNotEmpty()) {
                    _uiState.update { it.copy(favoriteListingIds = ids) }
                    return@launch
                }
            }

            // Fallback to legacy endpoint
            val fallbackUrl = appConfig.buildApiUrl("account", "wishlist")
            when (val result = apiClient.get(fallbackUrl, includeAuth = true)) {
                is ApiResult.Success -> {
                    val ids = parseFavoriteIds(result.data)
                    _uiState.update { it.copy(favoriteListingIds = ids) }
                }
                is ApiResult.Failure -> {
                    Timber.w("Failed to load favorites: ${result.error.message}")
                    _uiState.update {
                        it.copy(favoritesError = "Could not load your favorites. Pull to refresh to try again.")
                    }
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

            val ids = mutableSetOf<String>()
            for (item in itemsArray) {
                try {
                    val itemObj = item.jsonObject
                    // Backend returns wishlists with nested rooms arrays:
                    // { _id: "wishlist_id", name: "Favorites", rooms: [{ _id: "listing_id", ... }] }
                    // We need the room/listing IDs, not the wishlist IDs.
                    val rooms = (itemObj["rooms"] as? JsonArray)
                        ?: (itemObj["properties"] as? JsonArray)
                        ?: (itemObj["items"] as? JsonArray)
                        ?: (itemObj["listings"] as? JsonArray)
                    if (rooms != null) {
                        for (room in rooms) {
                            try {
                                val roomObj = room.jsonObject
                                val roomId = roomObj["_id"]?.jsonPrimitive?.content
                                    ?: roomObj["id"]?.jsonPrimitive?.content
                                if (roomId != null) ids.add(roomId)
                            } catch (_: Exception) { /* skip malformed room */ }
                        }
                    } else {
                        // Flat item (not a wishlist wrapper) — extract ID directly
                        val listingData = itemObj["listing"]?.jsonObject
                            ?: itemObj["item"]?.jsonObject
                            ?: itemObj
                        val id = listingData["_id"]?.jsonPrimitive?.content
                            ?: listingData["id"]?.jsonPrimitive?.content
                        if (id != null) ids.add(id)
                    }
                } catch (_: Exception) { /* skip malformed item */ }
            }
            ids
        } catch (_: Exception) { emptySet() }
    }

    fun selectCategory(category: String) {
        _uiState.update { it.copy(selectedCategory = category) }
    }
    
    companion object {
        /** shareCategory values that identify service listings (matches website). */
        private val SERVICE_SHARE_CATEGORIES = setOf(
            "HOME_HELP", "MOVING_HELP", "CLEANING_ORGANIZING", "EVERYDAY_HELP",
            "FITNESS", "LEARNING", "CREATIVE", "OTHER_SERVICE"
        )

        /** subCategory / room_type values that identify service listings (website parity). */
        private val SERVICE_SUBCATEGORY_IDS = setOf(
            "home_help", "moving_help", "cleaning_organizing", "everyday_help",
            "fitness", "learning", "creative", "other_service"
        )
    }

    /** Returns true if this listing should be classified as a service. */
    private fun isServiceListing(item: HomeListingItem): Boolean {
        if (item.shareCategory in SERVICE_SHARE_CATEGORIES) return true
        if (item.subCategory?.lowercase() in SERVICE_SUBCATEGORY_IDS) return true
        return false
    }

    private fun loadAllSections() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true) }

            // Website parity: fetch 3 sections in parallel
            // Section 1: Hourly Spaces (shareType=USE, non-service)
            // Section 2: Rental Items (shareType=BORROW)
            // Section 3: Events (shareType=SPLIT) — website shows SPLIT here, not services
            val useListingsDeferred = async { fetchUseListings() }
            val rentGearDeferred = async { fetchRentGear() }
            val splitListingsDeferred = async { fetchSplitListings() }

            val useListings = useListingsDeferred.await()
            val rentGear = rentGearDeferred.await()
            val splitListings = splitListingsDeferred.await()

            // Spaces = USE listings excluding service categories
            val spaces = if (useListings.second != null) {
                emptyList()
            } else {
                useListings.first.filter { !isServiceListing(it) }
            }

            _uiState.update {
                it.copy(
                    isRefreshing = false,
                    isLoadingHourlySpaces = false,
                    isLoadingRentGear = false,
                    isLoadingSplitStays = false,
                    hourlySpaces = spaces,
                    rentGear = rentGear.first,
                    splitStays = splitListings.first,
                    hourlySpacesError = useListings.second,
                    rentGearError = rentGear.second,
                    splitStaysError = splitListings.second
                )
            }
        }
    }
    
    /**
     * Fetch all USE-type listings (shareType=USE).
     * The caller splits results into spaces vs services by shareCategory.
     * GET /v1/poc/listings?shareType=USE&status=published&limit=24&skip_pagination=true
     */
    private suspend fun fetchUseListings(): Pair<List<HomeListingItem>, String?> {
        _uiState.update { it.copy(isLoadingHourlySpaces = true, isLoadingSplitStays = true) }

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
                Timber.w("Failed to fetch USE listings: ${result.error.message}")
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
     * Website parity: Fetch SPLIT listings (Events section).
     * Website shows shareType=SPLIT as "Events" (subscriptions, sports, cost-sharing).
     * GET /v1/poc/listings?shareType=SPLIT&status=published&limit=24&skip_pagination=true
     */
    private suspend fun fetchSplitListings(): Pair<List<HomeListingItem>, String?> {
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
                val items = parseListingsFromResponse(result.data, "split")
                Pair(items, null)
            }
            is ApiResult.Failure -> {
                Timber.w("Failed to fetch SPLIT listings: ${result.error.message}")
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
                        title = ListingPriceFormatter.stripTrailingPriceFromTitle(
                            (itemObj["name"] as? kotlinx.serialization.json.JsonPrimitive)?.content
                                ?: (itemObj["title"] as? kotlinx.serialization.json.JsonPrimitive)?.content
                                ?: "Listing"
                        ),
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
                        price = ListingPriceFormatter.parseListingPrice(itemObj),
                        rating = (itemObj["rating"] as? kotlinx.serialization.json.JsonPrimitive)?.doubleOrNull,
                        type = type,
                        shareCategory = (itemObj["shareCategory"] as? kotlinx.serialization.json.JsonPrimitive)?.content,
                        subCategory = (itemObj["subCategory"] as? kotlinx.serialization.json.JsonPrimitive)?.content
                            ?: (itemObj["roomType"] as? kotlinx.serialization.json.JsonPrimitive)?.content
                            ?: (itemObj["listing_type"] as? kotlinx.serialization.json.JsonPrimitive)?.content,
                        instantBook = (itemObj["instantBook"] as? kotlinx.serialization.json.JsonPrimitive)?.booleanOrNull
                            ?: (itemObj["instant_book"] as? kotlinx.serialization.json.JsonPrimitive)?.booleanOrNull
                            ?: (itemObj["isInstantBook"] as? kotlinx.serialization.json.JsonPrimitive)?.booleanOrNull,
                        available = (itemObj["available"] as? kotlinx.serialization.json.JsonPrimitive)?.booleanOrNull
                            ?: (itemObj["isAvailable"] as? kotlinx.serialization.json.JsonPrimitive)?.booleanOrNull,
                        latitude = (itemObj["location"] as? JsonObject)?.get("latitude")?.jsonPrimitive?.doubleOrNull
                            ?: (itemObj["latitude"] as? kotlinx.serialization.json.JsonPrimitive)?.doubleOrNull,
                        longitude = (itemObj["location"] as? JsonObject)?.get("longitude")?.jsonPrimitive?.doubleOrNull
                            ?: (itemObj["longitude"] as? kotlinx.serialization.json.JsonPrimitive)?.doubleOrNull
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
    val favoriteListingIds: Set<String> = emptySet(),
    /** Non-null when loading wishlists failed; UI should display a dismissible banner or snackbar. */
    val favoritesError: String? = null
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


