package com.pacedream.app.feature.collections

import com.pacedream.app.core.config.AppConfig
import com.pacedream.app.core.network.ApiClient
import com.pacedream.app.core.network.ApiError
import com.pacedream.app.core.network.ApiResult
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CollectionRepository @Inject constructor(
    private val apiClient: ApiClient,
    private val appConfig: AppConfig,
    private val json: Json
) {
    /**
     * Fetch user's collections.
     * GET /v1/collections
     */
    suspend fun fetchCollections(): ApiResult<List<UserCollection>> {
        val url = appConfig.buildApiUrl("collections")
        return when (val result = apiClient.get(url, includeAuth = true)) {
            is ApiResult.Success -> {
                val collections = parseCollections(result.data)
                ApiResult.Success(collections)
            }
            is ApiResult.Failure -> result
        }
    }

    /**
     * Fetch a single collection with items.
     * GET /v1/collections/{id}
     */
    suspend fun fetchCollectionDetail(collectionId: String): ApiResult<UserCollection> {
        val url = appConfig.buildApiUrl("collections", collectionId)
        return when (val result = apiClient.get(url, includeAuth = true)) {
            is ApiResult.Success -> {
                val collection = parseSingleCollection(result.data)
                if (collection != null) ApiResult.Success(collection)
                else ApiResult.Failure(ApiError.DecodingError())
            }
            is ApiResult.Failure -> result
        }
    }

    /**
     * Create a new collection.
     * POST /v1/collections
     */
    suspend fun createCollection(request: CreateCollectionRequest): ApiResult<UserCollection> {
        val url = appConfig.buildApiUrl("collections")
        val body = buildJsonObject {
            put("name", request.name)
            request.description?.let { put("description", it) }
            put("isPublic", request.isPublic)
        }.toString()

        return when (val result = apiClient.post(url, body, includeAuth = true)) {
            is ApiResult.Success -> {
                val collection = parseSingleCollection(result.data)
                if (collection != null) ApiResult.Success(collection)
                else ApiResult.Failure(ApiError.DecodingError())
            }
            is ApiResult.Failure -> result
        }
    }

    /**
     * Add a listing to a collection.
     * POST /v1/collections/{id}/items
     */
    suspend fun addToCollection(request: AddToCollectionRequest): ApiResult<String> {
        val url = appConfig.buildApiUrl("collections", request.collectionId, "items")
        val body = buildJsonObject {
            put("listingId", request.listingId)
        }.toString()

        return when (val result = apiClient.post(url, body, includeAuth = true)) {
            is ApiResult.Success -> ApiResult.Success("Added to collection")
            is ApiResult.Failure -> result
        }
    }

    /**
     * Remove a listing from a collection.
     * DELETE /v1/collections/{collectionId}/items/{itemId}
     */
    suspend fun removeFromCollection(collectionId: String, itemId: String): ApiResult<String> {
        val url = appConfig.buildApiUrl("collections", collectionId, "items", itemId)
        return when (val result = apiClient.delete(url, includeAuth = true)) {
            is ApiResult.Success -> ApiResult.Success("Removed from collection")
            is ApiResult.Failure -> result
        }
    }

    /**
     * Delete a collection.
     * DELETE /v1/collections/{id}
     */
    suspend fun deleteCollection(collectionId: String): ApiResult<String> {
        val url = appConfig.buildApiUrl("collections", collectionId)
        return when (val result = apiClient.delete(url, includeAuth = true)) {
            is ApiResult.Success -> ApiResult.Success("Collection deleted")
            is ApiResult.Failure -> result
        }
    }

    private fun parseCollections(responseBody: String): List<UserCollection> {
        return try {
            val root = json.parseToJsonElement(responseBody).jsonObject
            val data = root["data"]
            val array = when (data) {
                is JsonArray -> data
                is JsonObject -> data["collections"]?.asArr() ?: data["items"]?.asArr()
                else -> null
            } ?: root["collections"]?.asArr() ?: return emptyList()

            array.mapNotNull { parseCollectionElement(it) }
        } catch (e: Exception) {
            Timber.e(e, "Failed to parse collections")
            emptyList()
        }
    }

    private fun parseSingleCollection(responseBody: String): UserCollection? {
        return try {
            val root = json.parseToJsonElement(responseBody).jsonObject
            val data = root["data"]?.asObj() ?: root["collection"]?.asObj() ?: root
            parseCollectionElement(data)
        } catch (e: Exception) {
            Timber.e(e, "Failed to parse single collection")
            null
        }
    }

    private fun parseCollectionElement(element: JsonElement): UserCollection? {
        val obj = element.asObj() ?: return null
        val id = obj.str("id", "_id") ?: return null
        val name = obj.str("name", "title") ?: "Untitled"

        val itemsArray = obj["items"]?.asArr() ?: obj["listings"]?.asArr()
        val items = itemsArray?.mapNotNull { parseCollectionItem(it) } ?: emptyList()

        return UserCollection(
            id = id,
            name = name,
            description = obj.str("description"),
            coverImageUrl = obj.str("coverImage", "cover", "image")
                ?: items.firstOrNull()?.imageUrl,
            itemCount = obj.integer("itemCount", "count") ?: items.size,
            isPublic = obj["isPublic"]?.jsonPrimitive?.content?.toBooleanStrictOrNull() ?: false,
            createdAt = obj.str("createdAt", "created_at"),
            items = items
        )
    }

    private fun parseCollectionItem(element: JsonElement): CollectionItem? {
        val obj = element.asObj() ?: return null
        val id = obj.str("id", "_id") ?: return null
        val listingId = obj.str("listingId", "listing_id") ?: id

        return CollectionItem(
            id = id,
            listingId = listingId,
            title = obj.str("title", "name") ?: "Listing",
            imageUrl = obj.str("image", "imageUrl", "cover")
                ?: obj["images"]?.asArr()?.firstOrNull()?.jsonPrimitive?.content,
            location = obj["location"]?.let {
                when (it) {
                    is JsonObject -> it.str("city")
                    else -> runCatching { it.jsonPrimitive.content }.getOrNull()
                }
            },
            price = obj.str("price"),
            rating = obj["rating"]?.jsonPrimitive?.doubleOrNull,
            type = obj.str("type") ?: ""
        )
    }
}

// Extension helpers
private fun JsonElement.asObj(): JsonObject? = this as? JsonObject
private fun JsonElement.asArr(): JsonArray? = this as? JsonArray

private fun JsonObject.str(vararg keys: String): String? =
    keys.firstNotNullOfOrNull { k ->
        this[k]?.let { runCatching { it.jsonPrimitive.content }.getOrNull() }?.takeIf { it.isNotBlank() }
    }

private fun JsonObject.integer(vararg keys: String): Int? =
    keys.firstNotNullOfOrNull { k -> this[k]?.jsonPrimitive?.intOrNull }
