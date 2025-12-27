package com.shourov.apps.pacedream.feature.wishlist.data

import com.shourov.apps.pacedream.core.network.api.ApiClient
import com.shourov.apps.pacedream.core.network.api.ApiError
import com.shourov.apps.pacedream.core.network.api.ApiResult
import com.shourov.apps.pacedream.core.network.config.AppConfig
import com.shourov.apps.pacedream.feature.wishlist.model.WishlistItem
import com.shourov.apps.pacedream.feature.wishlist.model.WishlistItemType
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import timber.log.Timber
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
    
    /**
     * Fetch wishlist items with tolerant parsing
     */
    suspend fun getWishlist(): ApiResult<List<WishlistItem>> {
        val url = appConfig.buildApiUrl("account", "wishlist")
        
        return when (val result = apiClient.get(url, includeAuth = true)) {
            is ApiResult.Success -> {
                try {
                    val items = parseWishlistResponse(result.data)
                    ApiResult.Success(items)
                } catch (e: Exception) {
                    Timber.e(e, "Failed to parse wishlist response")
                    ApiResult.Failure(ApiError.DecodingError("Failed to parse wishlist", e))
                }
            }
            is ApiResult.Failure -> result
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
                    ApiResult.Success(toggleResult)
                } catch (e: Exception) {
                    Timber.e(e, "Failed to parse toggle response")
                    ApiResult.Failure(ApiError.DecodingError("Failed to parse toggle result", e))
                }
            }
            is ApiResult.Failure -> result
        }
    }
    
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

