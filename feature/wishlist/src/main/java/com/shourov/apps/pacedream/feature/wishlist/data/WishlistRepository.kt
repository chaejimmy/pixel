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
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
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
     * Primary: POST /v1/wishlists/toggle { propertyId }
     * Fallback: POST /v1/wishlists/add { room_id, name }
     */
    suspend fun addToWishlist(propertyId: String): ApiResult<Unit> {
        val toggleUrl = appConfig.buildApiUrl("wishlists", "toggle")
        val toggleBody = buildJsonBody(mapOf("propertyId" to propertyId))
        val toggleResult = apiClient.post(toggleUrl, toggleBody, includeAuth = true)
        if (toggleResult is ApiResult.Success) {
            if (isSuccessResponse(toggleResult.data)) {
                notifyChanged()
                return ApiResult.Success(Unit)
            }
        }

        // Fallback: legacy add with room_id field name
        val fallbackUrl = appConfig.buildApiUrl("wishlists", "add")
        val fallbackBody = buildJsonBody(mapOf("room_id" to propertyId, "name" to "Favorites"))
        val fallback = apiClient.post(fallbackUrl, fallbackBody, includeAuth = true)
        return when (fallback) {
            is ApiResult.Success -> {
                notifyChanged()
                ApiResult.Success(Unit)
            }
            is ApiResult.Failure -> (toggleResult as? ApiResult.Failure) ?: fallback
        }
    }

    /**
     * Remove a property/listing from wishlist.
     * Primary: DELETE /v1/wishlists/{propertyId}
     * Fallback: POST /v1/wishlists/toggle { propertyId }
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

        // Fallback: toggle endpoint to remove
        val toggleUrl = appConfig.buildApiUrl("wishlists", "toggle")
        val toggleBody = buildJsonBody(mapOf("propertyId" to propertyId))
        val fallback = apiClient.post(toggleUrl, toggleBody, includeAuth = true)
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
        val url = appConfig.buildApiUrl("wishlists", "toggle")
        
        // Build request body - send propertyId (server primary key) plus itemId/listingId
        val bodyMap = mutableMapOf<String, String>()
        bodyMap["propertyId"] = itemId
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
        // Note: use safe casts to avoid "JsonArray is not a JsonObject" crash
        // when backend returns { data: [...] } (data is array, not object)
        val dataElement = obj["data"]
        val wishlistsArray = (obj["wishlists"] as? kotlinx.serialization.json.JsonArray)
            ?: ((dataElement as? JsonObject)?.get("wishlists") as? kotlinx.serialization.json.JsonArray)
            ?: (dataElement as? kotlinx.serialization.json.JsonArray)

        // If we found wishlists, flatten nested listing arrays (properties/items/listings)
        if (wishlistsArray != null) {
            val flattened = mutableListOf<WishlistItem>()
            wishlistsArray.forEach { wlEl ->
                val wlObj = wlEl as? JsonObject ?: return@forEach
                val nested = wlObj["properties"]?.jsonArray
                    ?: wlObj["items"]?.jsonArray
                    ?: wlObj["listings"]?.jsonArray
                    ?: wlObj["rooms"]?.jsonArray
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
     * Parse individual wishlist item with tolerant field extraction.
     *
     * The backend Room model uses these field names:
     * - name (not title)
     * - images[] (array of URLs)
     * - dynamic_price { amount, currency, frequency }
     * - location { city, state, country, street, zipcode }
     * - reviews[] { rating } (average must be computed)
     * - room_type / property_type (for category)
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
            ?: itemObject["summary"]?.jsonPrimitive?.content

        val imageUrl = itemObject["image"]?.jsonPrimitive?.content
            ?: itemObject["imageUrl"]?.jsonPrimitive?.content
            ?: (itemObject["images"] as? kotlinx.serialization.json.JsonArray)?.firstOrNull()?.jsonPrimitive?.content
            ?: itemObject["cover_image"]?.jsonPrimitive?.content
            ?: itemObject["coverImage"]?.jsonPrimitive?.content
            ?: (itemObject["place_image"] as? kotlinx.serialization.json.JsonArray)?.firstOrNull()?.jsonPrimitive?.content
            ?: itemObject["thumbnail"]?.jsonPrimitive?.content
            ?: (itemObject["gallery"] as? JsonObject)?.get("thumbnail")?.jsonPrimitive?.content
            ?: (itemObject["gallery"] as? JsonObject)?.get("images")?.jsonArray?.firstOrNull()?.jsonPrimitive?.content
            ?: (itemObject["galleryImages"] as? kotlinx.serialization.json.JsonArray)?.firstOrNull()?.jsonPrimitive?.content
            ?: itemObject["coverPhoto"]?.jsonPrimitive?.content
            ?: itemObject["photo"]?.jsonPrimitive?.content

        // Price: backend Room model uses dynamic_price { amount, currency, frequency }
        val dynamicPrice = itemObject["dynamic_price"] as? JsonObject
        val priceValue = dynamicPrice?.get("amount")?.jsonPrimitive?.content?.toDoubleOrNull()
            ?: itemObject["price"]?.jsonPrimitive?.content?.toDoubleOrNull()
            ?: itemObject["amount"]?.jsonPrimitive?.content?.toDoubleOrNull()
            ?: (itemObject["price"] as? JsonObject)?.get("amount")?.jsonPrimitive?.content?.toDoubleOrNull()

        val priceUnit = dynamicPrice?.get("frequency")?.jsonPrimitive?.content
            ?: (itemObject["price"] as? JsonObject)?.get("frequency")?.jsonPrimitive?.content
            ?: (itemObject["pricing"] as? JsonObject)?.get("frequency")?.jsonPrimitive?.content
            ?: (itemObject["price"] as? kotlinx.serialization.json.JsonArray)?.firstOrNull()
                ?.jsonObject?.get("frequency")?.jsonPrimitive?.content

        val itemTypeString = itemObject["type"]?.jsonPrimitive?.content
            ?: itemObject["listingType"]?.jsonPrimitive?.content
            ?: itemObject["shareType"]?.jsonPrimitive?.content
            ?: itemObject["category"]?.jsonPrimitive?.content
            ?: itemObject["room_type"]?.jsonPrimitive?.content
            ?: itemObject["property_type"]?.jsonPrimitive?.content

        val itemType = parseItemType(itemTypeString, itemObject)

        // Location: backend Room model uses location { city, state, country, ... }
        val location = extractLocation(itemObject)

        // Rating: backend Room model uses reviews[] array; compute average
        val rating = extractRating(itemObject)

        return WishlistItem(
            id = id,
            listingId = listingId,
            title = title,
            description = description,
            imageUrl = imageUrl,
            price = priceValue,
            priceUnit = priceUnit,
            itemType = itemType,
            location = location,
            rating = rating
        )
    }

    /**
     * Extract location string from various response shapes.
     * Backend Room model stores location as { city, state, country, street, zipcode, location }.
     */
    private fun extractLocation(itemObject: JsonObject): String? {
        // Try flat string first (some endpoints may return a flat value)
        try {
            itemObject["location"]?.jsonPrimitive?.content?.let { return it }
        } catch (_: Exception) {
            // location is an object, not a primitive
        }

        // Parse location object: prefer "location" sub-field, else build from parts
        val locObj = itemObject["location"] as? JsonObject
        if (locObj != null) {
            // Some backends put a display string in location.location
            try {
                locObj["location"]?.jsonPrimitive?.content?.takeIf { it.isNotBlank() }?.let { return it }
            } catch (_: Exception) { /* not a primitive */ }

            val city = locObj["city"]?.jsonPrimitive?.content?.takeIf { it.isNotBlank() }
            val state = locObj["state"]?.jsonPrimitive?.content?.takeIf { it.isNotBlank() }
            val country = locObj["country"]?.jsonPrimitive?.content?.takeIf { it.isNotBlank() }
            val parts = listOfNotNull(city, state, country)
            if (parts.isNotEmpty()) return parts.joinToString(", ")
        }

        // Fallback to address field
        return itemObject["address"]?.jsonPrimitive?.content
    }

    /**
     * Extract rating from a flat "rating" field or by averaging reviews[].rating.
     */
    private fun extractRating(itemObject: JsonObject): Double? {
        // Try flat rating first
        itemObject["rating"]?.jsonPrimitive?.content?.toDoubleOrNull()?.let { return it }

        // Compute average from reviews array (backend Room model)
        val reviews = itemObject["reviews"] as? kotlinx.serialization.json.JsonArray ?: return null
        if (reviews.isEmpty()) return null

        var sum = 0.0
        var count = 0
        for (review in reviews) {
            val reviewObj = review as? JsonObject ?: continue
            val r = reviewObj["rating"]?.jsonPrimitive?.content?.toDoubleOrNull() ?: continue
            sum += r
            count++
        }
        return if (count > 0) sum / count else null
    }
    
    /**
     * Parse item type with fallback logic.
     * Backend Room model uses room_type and property_type fields.
     */
    private fun parseItemType(typeString: String?, itemObject: JsonObject): WishlistItemType {
        // First try direct type string
        typeString?.let { type ->
            WishlistItemType.fromString(type)?.let { return it }
        }

        // Look at all available type hints
        val category = itemObject["category"]?.jsonPrimitive?.content?.lowercase() ?: ""
        val listingType = itemObject["listingType"]?.jsonPrimitive?.content?.lowercase() ?: ""
        val shareType = itemObject["shareType"]?.jsonPrimitive?.content?.lowercase() ?: ""
        val roomType = itemObject["room_type"]?.jsonPrimitive?.content?.lowercase() ?: ""
        val propertyType = itemObject["property_type"]?.jsonPrimitive?.content?.lowercase() ?: ""

        return when {
            shareType == "split" || category.contains("split") || listingType.contains("roommate") ||
                roomType.contains("roommate") || roomType.contains("split") ->
                WishlistItemType.SPLIT_STAY

            category.contains("gear") || category.contains("car") ||
                category.contains("vehicle") || listingType.contains("borrow") ||
                roomType.contains("gear") || roomType.contains("parking") ||
                propertyType.contains("gear") || propertyType.contains("vehicle") ->
                WishlistItemType.HOURLY_GEAR

            shareType == "use" || listingType.contains("hourly") ||
                listingType.contains("time") || roomType.contains("short") ||
                roomType.contains("time") || roomType.contains("hourly") ->
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
        return buildJsonObject {
            for ((key, value) in map) {
                put(key, value)
            }
        }.toString()
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


