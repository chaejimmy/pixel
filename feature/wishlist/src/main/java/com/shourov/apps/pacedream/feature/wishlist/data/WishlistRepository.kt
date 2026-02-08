package com.shourov.apps.pacedream.feature.wishlist.data

import com.shourov.apps.pacedream.core.network.api.ApiClient
import com.shourov.apps.pacedream.core.network.api.ApiError
import com.shourov.apps.pacedream.core.network.api.ApiResult
import com.shourov.apps.pacedream.core.network.config.AppConfig
import com.shourov.apps.pacedream.feature.wishlist.model.WishlistItem
import com.shourov.apps.pacedream.feature.wishlist.model.WishlistItemType
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import timber.log.Timber
import androidx.annotation.VisibleForTesting
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for wishlist operations with tolerant parsing
 * 
 * Endpoints:
 * - GET /v1/account/wishlist (Authorization required)
 *   Response wrappers vary; must find items array in common shapes:
 *   items, data.items, data.data.items
 * 
 * - POST /v1/account/wishlist/toggle (Authorization required)
 *   Body supports { itemId } OR { listingId } and optional { type }
 *   Response: { status:true, data:{ liked, ok, message } }
 */
@Singleton
class WishlistRepository @Inject constructor(
    private val apiClient: ApiClient,
    private val appConfig: AppConfig,
    private val json: Json
) {
    private val _changes = MutableSharedFlow<Unit>(replay = 0, extraBufferCapacity = 1)
    val changes: SharedFlow<Unit> = _changes.asSharedFlow()

    private fun notifyChanged() {
        _changes.tryEmit(Unit)
    }

    /**
     * iOS parity wishlist endpoints:
     * - GET /v1/wishlists
     * - POST /v1/wishlists/add
     * - DELETE /v1/wishlists/{propertyId}
     *
     * Backward-compatible fallbacks (older backend):
     * - GET /v1/account/wishlist
     * - POST /v1/account/wishlist/toggle
     */
    
    /**
     * Fetch wishlist items with tolerant parsing
     */
    suspend fun getWishlist(): ApiResult<List<WishlistItem>> {
        val primaryUrl = appConfig.buildApiUrl("wishlists")

        val primary = apiClient.get(primaryUrl, includeAuth = true)
        if (primary is ApiResult.Success) {
            try {
                val items = parseWishlistsEndpointResponse(primary.data)
                return ApiResult.Success(items)
            } catch (e: Exception) {
                Timber.e(e, "Failed to parse /wishlists response; falling back to /account/wishlist")
            }
        }

        // Fallback to legacy endpoint only when primary fails or can't be parsed.
        val fallbackUrl = appConfig.buildApiUrl("account", "wishlist")
        return when (val fallback = apiClient.get(fallbackUrl, includeAuth = true)) {
            is ApiResult.Success -> {
                try {
                    val items = parseWishlistResponse(fallback.data)
                    ApiResult.Success(items)
                } catch (e: Exception) {
                    Timber.e(e, "Failed to parse legacy wishlist response")
                    ApiResult.Failure(ApiError.DecodingError("Failed to parse wishlist", e))
                }
            }
            is ApiResult.Failure -> {
                // Prefer the primary error if we have it; otherwise return fallback error.
                (primary as? ApiResult.Failure) ?: fallback
            }
        }
    }
    
    /**
     * Add a property/listing to wishlist.
     * Primary: POST /v1/wishlists/add { propertyId }
     * Fallback: POST /v1/account/wishlist/toggle
     */
    suspend fun addToWishlist(propertyId: String): ApiResult<Unit> {
        val primaryUrl = appConfig.buildApiUrl("wishlists", "add")
        val body = buildJsonBody(mapOf("propertyId" to propertyId))
        val primary = apiClient.post(primaryUrl, body, includeAuth = true)
        if (primary is ApiResult.Success) {
            if (isSuccessResponse(primary.data)) {
                notifyChanged()
                return ApiResult.Success(Unit)
            }
        }

        // Fallback: toggle
        val fallback = toggleWishlistItem(itemId = propertyId, listingId = propertyId, type = null)
        return when (fallback) {
            is ApiResult.Success -> {
                notifyChanged()
                ApiResult.Success(Unit)
            }
            is ApiResult.Failure -> (primary as? ApiResult.Failure)?.let { it } ?: fallback
        }
    }

    /**
     * Remove a property/listing from wishlist.
     * Primary: DELETE /v1/wishlists/{propertyId}
     * Fallback: POST /v1/account/wishlist/toggle
     */
    suspend fun removeFromWishlist(propertyId: String): ApiResult<Unit> {
        val primaryUrl = appConfig.buildApiUrl("wishlists", propertyId)
        val primary = apiClient.delete(primaryUrl, body = null, includeAuth = true)
        if (primary is ApiResult.Success) {
            if (isSuccessResponse(primary.data)) {
                notifyChanged()
                return ApiResult.Success(Unit)
            }
            // Some backends return empty body on success
            if (primary.data.isBlank()) {
                notifyChanged()
                return ApiResult.Success(Unit)
            }
        }

        // Fallback: toggle (expected liked=false for removal, but we treat any success as completion)
        val fallback = toggleWishlistItem(itemId = propertyId, listingId = propertyId, type = null)
        return when (fallback) {
            is ApiResult.Success -> {
                notifyChanged()
                ApiResult.Success(Unit)
            }
            is ApiResult.Failure -> (primary as? ApiResult.Failure)?.let { it } ?: fallback
        }
    }

    /**
     * Toggle wishlist item (add/remove)
     * Returns ToggleResult with liked status
     */
    suspend fun toggleWishlistItem(
        itemId: String,
        listingId: String? = null,
        type: WishlistItemType? = null
    ): ApiResult<ToggleResult> {
        val url = appConfig.buildApiUrl("account", "wishlist", "toggle")
        
        // Build request body - supports both itemId and listingId
        val bodyMap = mutableMapOf<String, String>()
        bodyMap["itemId"] = itemId
        listingId?.let { bodyMap["listingId"] = it }
        type?.let { bodyMap["type"] = it.apiValue }
        
        val body = buildJsonBody(bodyMap)
        
        return when (val result = apiClient.post(url, body, includeAuth = true)) {
            is ApiResult.Success -> {
                try {
                    val toggleResult = parseToggleResponse(result.data)
                    if (toggleResult.success) notifyChanged()
                    ApiResult.Success(toggleResult)
                } catch (e: Exception) {
                    Timber.e(e, "Failed to parse toggle response")
                    ApiResult.Failure(ApiError.DecodingError("Failed to parse toggle result", e))
                }
            }
            is ApiResult.Failure -> result
        }
    }
    
    @VisibleForTesting
    internal fun parseWishlistResponseForTest(responseBody: String): List<WishlistItem> =
        parseWishlistsEndpointResponse(responseBody)

    @VisibleForTesting
    internal fun parseToggleResponseForTest(responseBody: String): ToggleResult =
        parseToggleResponse(responseBody)

    @VisibleForTesting
    internal fun isSuccessResponseForTest(responseBody: String): Boolean =
        isSuccessResponse(responseBody)

    /**
     * Parse wishlist response with tolerant path finding
     * Looks for items array in: items, data.items, data.data.items
     */
    private fun parseWishlistResponse(responseBody: String): List<WishlistItem> {
        val jsonElement = json.parseToJsonElement(responseBody)
        val jsonObject = jsonElement.jsonObject
        
        // Try different paths to find items array
        val itemsArray = findItemsArray(jsonObject)
        
        if (itemsArray == null) {
            Timber.w("No items array found in wishlist response")
            return emptyList()
        }
        
        return itemsArray.mapNotNull { element ->
            try {
                parseWishlistItem(element.jsonObject)
            } catch (e: Exception) {
                Timber.w(e, "Failed to parse wishlist item")
                null
            }
        }
    }
    
    /**
     * Parse /v1/wishlists response (tolerant).
     *
     * Common shapes:
     * - raw array of wishlist items
     * - { data: [] } or { wishlists: [] } or { data: { wishlists: [] } }
     * - each wishlist may contain nested arrays: properties/items/listings
     */
    private fun parseWishlistsEndpointResponse(responseBody: String): List<WishlistItem> {
        val root = json.parseToJsonElement(responseBody)

        // 1) If it's already an array, treat as list of wishlist items/listings
        if (root is kotlinx.serialization.json.JsonArray) {
            return root.mapNotNull { el ->
                val obj = el as? JsonObject ?: return@mapNotNull null
                parseWishlistItem(obj)
            }
        }

        val obj = (root as? JsonObject) ?: return emptyList()

        // 2) Try to find an array of wishlists
        val wishlistsArray = obj["wishlists"]?.jsonArray
            ?: obj["data"]?.jsonObject?.get("wishlists")?.jsonArray
            ?: obj["data"]?.jsonArray

        // If we found wishlists, flatten nested listing arrays (properties/items/listings)
        if (wishlistsArray != null) {
            val flattened = mutableListOf<WishlistItem>()
            wishlistsArray.forEach { wlEl ->
                val wlObj = wlEl as? JsonObject ?: return@forEach
                val nested = wlObj["properties"]?.jsonArray
                    ?: wlObj["items"]?.jsonArray
                    ?: wlObj["listings"]?.jsonArray
                if (nested != null) {
                    nested.forEach { itemEl ->
                        val itemObj = itemEl as? JsonObject ?: return@forEach
                        runCatching { parseWishlistItem(itemObj) }.getOrNull()?.let(flattened::add)
                    }
                } else {
                    // Sometimes wishlists are already items
                    runCatching { parseWishlistItem(wlObj) }.getOrNull()?.let(flattened::add)
                }
            }
            return flattened
        }

        // 3) Fall back to the legacy tolerant parser paths
        return parseWishlistResponse(responseBody)
    }

    private fun isSuccessResponse(responseBody: String): Boolean {
        return try {
            val root = json.parseToJsonElement(responseBody).jsonObject
            root["success"]?.jsonPrimitive?.boolean == true ||
                root["status"]?.jsonPrimitive?.boolean == true ||
                root["ok"]?.jsonPrimitive?.boolean == true
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Find items array in various response shapes
     */
    private fun findItemsArray(jsonObject: JsonObject): List<JsonElement>? {
        // Try: items
        jsonObject["items"]?.jsonArray?.let { return it.toList() }
        
        // Try: data.items
        jsonObject["data"]?.jsonObject?.get("items")?.jsonArray?.let { return it.toList() }
        
        // Try: data.data.items
        jsonObject["data"]?.jsonObject?.get("data")?.jsonObject?.get("items")?.jsonArray?.let { 
            return it.toList() 
        }
        
        // Try: data as array directly
        try {
            jsonObject["data"]?.jsonArray?.let { return it.toList() }
        } catch (e: Exception) {
            // data is not an array
        }
        
        return null
    }
    
    /**
     * Parse individual wishlist item with tolerant field extraction
     */
    private fun parseWishlistItem(itemObject: JsonObject): WishlistItem {
        val id = itemObject["_id"]?.jsonPrimitive?.content
            ?: itemObject["id"]?.jsonPrimitive?.content
            ?: ""
        
        val listingId = itemObject["listingId"]?.jsonPrimitive?.content
            ?: itemObject["listing_id"]?.jsonPrimitive?.content
            ?: id
        
        val title = itemObject["title"]?.jsonPrimitive?.content
            ?: itemObject["name"]?.jsonPrimitive?.content
            ?: ""
        
        val description = itemObject["description"]?.jsonPrimitive?.content
        
        val imageUrl = itemObject["image"]?.jsonPrimitive?.content
            ?: itemObject["imageUrl"]?.jsonPrimitive?.content
            ?: itemObject["images"]?.jsonArray?.firstOrNull()?.jsonPrimitive?.content
            ?: itemObject["thumbnail"]?.jsonPrimitive?.content
        
        val priceValue = itemObject["price"]?.jsonPrimitive?.content?.toDoubleOrNull()
            ?: itemObject["amount"]?.jsonPrimitive?.content?.toDoubleOrNull()
        
        val itemTypeString = itemObject["type"]?.jsonPrimitive?.content
            ?: itemObject["listingType"]?.jsonPrimitive?.content
            ?: itemObject["shareType"]?.jsonPrimitive?.content
            ?: itemObject["category"]?.jsonPrimitive?.content
        
        val itemType = parseItemType(itemTypeString, itemObject)
        
        val location = itemObject["location"]?.jsonPrimitive?.content
            ?: itemObject["address"]?.jsonPrimitive?.content
        
        val rating = itemObject["rating"]?.jsonPrimitive?.content?.toDoubleOrNull()
        
        return WishlistItem(
            id = id,
            listingId = listingId,
            title = title,
            description = description,
            imageUrl = imageUrl,
            price = priceValue,
            itemType = itemType,
            location = location,
            rating = rating
        )
    }
    
    /**
     * Parse item type with fallback logic
     */
    private fun parseItemType(typeString: String?, itemObject: JsonObject): WishlistItemType {
        // First try direct type string
        typeString?.let { type ->
            WishlistItemType.fromString(type)?.let { return it }
        }
        
        // Look at category or other hints
        val category = itemObject["category"]?.jsonPrimitive?.content?.lowercase() ?: ""
        val listingType = itemObject["listingType"]?.jsonPrimitive?.content?.lowercase() ?: ""
        val shareType = itemObject["shareType"]?.jsonPrimitive?.content?.lowercase() ?: ""
        
        return when {
            shareType == "split" || category.contains("split") || listingType.contains("roommate") ->
                WishlistItemType.SPLIT_STAY
            
            category.contains("gear") || category.contains("car") || 
            category.contains("vehicle") || listingType.contains("borrow") ->
                WishlistItemType.HOURLY_GEAR
            
            shareType == "use" || listingType.contains("hourly") || 
            listingType.contains("time") ->
                WishlistItemType.TIME_BASED
            
            else -> WishlistItemType.TIME_BASED // Default fallback
        }
    }
    
    /**
     * Parse toggle response
     */
    private fun parseToggleResponse(responseBody: String): ToggleResult {
        val jsonElement = json.parseToJsonElement(responseBody)
        val jsonObject = jsonElement.jsonObject
        
        val isSuccess = jsonObject["status"]?.jsonPrimitive?.boolean == true ||
                       jsonObject["success"]?.jsonPrimitive?.boolean == true
        
        val data = jsonObject["data"]?.jsonObject
        
        val liked = data?.get("liked")?.jsonPrimitive?.boolean
            ?: data?.get("ok")?.jsonPrimitive?.boolean
            ?: isSuccess
        
        val message = data?.get("message")?.jsonPrimitive?.content
        
        return ToggleResult(
            success = isSuccess,
            liked = liked,
            message = message
        )
    }
    
    /**
     * Build JSON body from map
     */
    private fun buildJsonBody(map: Map<String, String>): String {
        val entries = map.entries.joinToString(",") { (key, value) ->
            "\"$key\":\"$value\""
        }
        return "{$entries}"
    }
}

/**
 * Result of toggle operation
 */
data class ToggleResult(
    val success: Boolean,
    val liked: Boolean,
    val message: String? = null
)


