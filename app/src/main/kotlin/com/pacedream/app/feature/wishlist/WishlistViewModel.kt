package com.pacedream.app.feature.wishlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pacedream.app.core.auth.AuthSession
import com.pacedream.app.core.auth.AuthState
import com.pacedream.app.core.config.AppConfig
import com.pacedream.app.core.network.ApiClient
import com.pacedream.app.core.network.ApiResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import timber.log.Timber
import javax.inject.Inject

/**
 * WishlistViewModel - Wishlist with optimistic remove
 * 
 * iOS Parity:
 * - GET /v1/account/wishlist (tolerant wrapper parsing)
 * - POST /v1/account/wishlist/toggle (body supports itemId or listingId)
 * - Optimistic remove: remove immediately, restore on failure or unexpected liked=true
 * - Routing rules for Book Now based on item type
 */
@HiltViewModel
class WishlistViewModel @Inject constructor(
    private val apiClient: ApiClient,
    private val appConfig: AppConfig,
    private val authSession: AuthSession,
    private val json: Json
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(WishlistUiState())
    val uiState: StateFlow<WishlistUiState> = _uiState.asStateFlow()
    
    private val _toastMessages = Channel<String>(Channel.BUFFERED)
    val toastMessages = _toastMessages.receiveAsFlow()
    
    // Full list of items (before filtering)
    private var allItems: List<WishlistItem> = emptyList()
    
    init {
        viewModelScope.launch {
            authSession.authState.collect { state ->
                when (state) {
                    AuthState.Authenticated -> {
                        _uiState.update { it.copy(showLockedState = false, requiresAuth = false) }
                        loadWishlist()
                    }
                    AuthState.Unauthenticated -> {
                        _uiState.update { it.copy(showLockedState = true, isLoading = false) }
                    }
                    AuthState.Unknown -> {
                        // Wait for auth to initialize
                    }
                }
            }
        }
    }
    
    fun refresh() {
        loadWishlist()
    }
    
    fun setFilter(filter: WishlistFilter) {
        _uiState.update { state ->
            state.copy(
                selectedFilter = filter,
                filteredItems = filterItems(allItems, filter)
            )
        }
    }
    
    /**
     * Remove item with optimistic update
     * 
     * iOS Parity behavior:
     * 1. Remove item immediately from UI
     * 2. Call toggle API
     * 3. If API fails OR returns liked=true (unexpected), restore item and show toast
     */
    fun removeItem(item: WishlistItem) {
        viewModelScope.launch {
            // Step 1: Optimistic remove - save original state
            val originalItems = allItems.toList()
            val itemIndex = allItems.indexOf(item)
            
            // Remove immediately from UI
            allItems = allItems.filter { it.id != item.id }
            updateFilteredItems()
            
            // Step 2: Call toggle API
            val url = appConfig.buildApiUrl("account", "wishlist", "toggle")
            val body = json.encodeToString(
                WishlistToggleRequest.serializer(),
                WishlistToggleRequest(
                    itemId = item.listingId ?: item.id,
                    listingId = item.listingId
                )
            )
            
            val result = apiClient.post(url, body, includeAuth = true)
            
            // Step 3: Handle result
            when (result) {
                is ApiResult.Success -> {
                    val toggleResponse = parseToggleResponse(result.data)
                    
                    // If API returns liked=true, the item was NOT removed (unexpected)
                    if (toggleResponse?.liked == true) {
                        Timber.w("Item was not removed as expected, restoring")
                        restoreItem(originalItems, itemIndex, item)
                        _toastMessages.send("Could not remove item. Please try again.")
                    } else {
                        // Successfully removed
                        Timber.d("Item successfully removed from wishlist")
                        _toastMessages.send("Removed from favorites")
                    }
                }
                is ApiResult.Failure -> {
                    // API failed, restore item
                    Timber.e("Failed to remove item: ${result.error.message}")
                    restoreItem(originalItems, itemIndex, item)
                    _toastMessages.send("Failed to remove item. Please try again.")
                }
            }
        }
    }
    
    private fun restoreItem(originalList: List<WishlistItem>, index: Int, item: WishlistItem) {
        allItems = originalList
        updateFilteredItems()
    }
    
    private fun updateFilteredItems() {
        _uiState.update { state ->
            state.copy(filteredItems = filterItems(allItems, state.selectedFilter))
        }
    }
    
    private fun filterItems(items: List<WishlistItem>, filter: WishlistFilter): List<WishlistItem> {
        return when (filter) {
            WishlistFilter.ALL -> items
            WishlistFilter.SPACES -> items.filter { it.type == "time-based" || it.type == "room-stay" }
            WishlistFilter.GEAR -> items.filter { it.type == "hourly-gear" || it.type == "gear" }
        }
    }
    
    private fun loadWishlist() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, isRefreshing = true, error = null) }
            
            val url = appConfig.buildApiUrl("account", "wishlist")
            
            when (val result = apiClient.get(url, includeAuth = true)) {
                is ApiResult.Success -> {
                    val items = parseWishlistResponse(result.data)
                    allItems = items
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isRefreshing = false,
                            filteredItems = filterItems(items, it.selectedFilter),
                            error = null
                        )
                    }
                }
                is ApiResult.Failure -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isRefreshing = false,
                            error = result.error.message
                        )
                    }
                }
            }
        }
    }
    
    /**
     * Parse wishlist response with tolerant wrapper parsing
     * Handles multiple response formats
     */
    private fun parseWishlistResponse(responseBody: String): List<WishlistItem> {
        return try {
            val element = json.parseToJsonElement(responseBody)
            val obj = element.jsonObject
            
            // Find items array in common locations
            val itemsArray = obj["data"]?.jsonArray
                ?: obj["items"]?.jsonArray
                ?: obj["wishlist"]?.jsonArray
                ?: (obj["data"] as? JsonObject)?.get("items")?.jsonArray
                ?: (obj["data"] as? JsonObject)?.get("wishlist")?.jsonArray
                ?: return emptyList()
            
            itemsArray.mapNotNull { item ->
                try {
                    val itemObj = item.jsonObject
                    
                    // Get listing data (may be nested)
                    val listingData = itemObj["listing"]?.jsonObject
                        ?: itemObj["item"]?.jsonObject
                        ?: itemObj
                    
                    val type = listingData["shareType"]?.jsonPrimitive?.content
                        ?: listingData["type"]?.jsonPrimitive?.content
                        ?: determineType(listingData)
                    
                    WishlistItem(
                        id = itemObj["_id"]?.jsonPrimitive?.content
                            ?: itemObj["id"]?.jsonPrimitive?.content
                            ?: return@mapNotNull null,
                        listingId = listingData["_id"]?.jsonPrimitive?.content
                            ?: listingData["id"]?.jsonPrimitive?.content,
                        title = listingData["name"]?.jsonPrimitive?.content
                            ?: listingData["title"]?.jsonPrimitive?.content
                            ?: "Item",
                        imageUrl = listingData["images"]?.jsonArray?.firstOrNull()?.jsonPrimitive?.content
                            ?: listingData["image"]?.jsonPrimitive?.content,
                        location = listingData["location"]?.let { loc ->
                            when (loc) {
                                is JsonObject -> loc["city"]?.jsonPrimitive?.content
                                else -> loc.jsonPrimitive.content
                            }
                        },
                        price = parsePrice(listingData),
                        rating = listingData["rating"]?.jsonPrimitive?.doubleOrNull,
                        type = type
                    )
                } catch (e: Exception) {
                    Timber.w(e, "Failed to parse wishlist item")
                    null
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to parse wishlist response")
            emptyList()
        }
    }
    
    private fun determineType(obj: JsonObject): String {
        // Try to determine type from other fields
        return when {
            obj.containsKey("dynamic_price") -> "time-based"
            obj.containsKey("rentalPrice") -> "hourly-gear"
            else -> "time-based" // Safe fallback
        }
    }
    
    private fun parsePrice(obj: JsonObject): String? {
        return try {
            obj["dynamic_price"]?.jsonArray?.firstOrNull()?.jsonObject?.let { price ->
                price["price"]?.jsonPrimitive?.content?.let { "\$$it/hr" }
            }
            ?: obj["price"]?.let { price ->
                when (price) {
                    is JsonObject -> {
                        val amount = price["amount"]?.jsonPrimitive?.content
                        val unit = price["unit"]?.jsonPrimitive?.content ?: "month"
                        amount?.let { "\$$it/$unit" }
                    }
                    else -> "\$${price.jsonPrimitive.content}/hr"
                }
            }
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Parse toggle response with tolerant parsing
     */
    private fun parseToggleResponse(responseBody: String): WishlistToggleResponse? {
        return try {
            val element = json.parseToJsonElement(responseBody)
            val obj = element.jsonObject
            
            val data = obj["data"]?.jsonObject ?: obj
            
            WishlistToggleResponse(
                liked = data["liked"]?.jsonPrimitive?.booleanOrNull ?: false
            )
        } catch (e: Exception) {
            Timber.e(e, "Failed to parse toggle response")
            null
        }
    }
}

/**
 * Wishlist UI State
 */
data class WishlistUiState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val showLockedState: Boolean = true,
    val requiresAuth: Boolean = false,
    val selectedFilter: WishlistFilter = WishlistFilter.ALL,
    val filteredItems: List<WishlistItem> = emptyList(),
    val error: String? = null
)

/**
 * Wishlist item model
 */
data class WishlistItem(
    val id: String,
    val listingId: String?,
    val title: String,
    val imageUrl: String?,
    val location: String?,
    val price: String?,
    val rating: Double?,
    val type: String
)

/**
 * Wishlist filter options
 */
enum class WishlistFilter(val displayName: String) {
    ALL("All"),
    SPACES("Spaces"),
    GEAR("Gear")
}

/**
 * Toggle request
 */
@Serializable
data class WishlistToggleRequest(
    val itemId: String,
    val listingId: String? = null
)

/**
 * Toggle response
 */
data class WishlistToggleResponse(
    val liked: Boolean
)


