package com.pacedream.app.feature.listingdetail

import com.pacedream.app.core.config.AppConfig
import com.pacedream.app.core.network.ApiError
import com.pacedream.app.core.network.ApiClient
import com.pacedream.app.core.network.ApiResult
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ListingWishlistRepository @Inject constructor(
    private val apiClient: ApiClient,
    private val appConfig: AppConfig,
    private val json: Json
) {
    data class ToggleResult(
        val isFavorite: Boolean,
        val wishlistItemId: String? = null
    )

    suspend fun addToWishlist(listingId: String): ApiResult<ToggleResult> {
        // Preferred endpoints from spec.
        val addUrl = appConfig.buildApiUrl("wishlists", "add")

        val tryListingId = postJson(addUrl, mapOf("listing_id" to listingId))
        when (tryListingId) {
            is ApiResult.Success -> return ApiResult.Success(parseWishlistToggleLike(tryListingId.data, defaultLiked = true))
            is ApiResult.Failure -> {
                if (!isLikelyContractMismatch(tryListingId.error)) {
                    return ApiResult.Failure(tryListingId.error)
                }
            }
        }

        val tryPropertyId = postJson(addUrl, mapOf("property_id" to listingId))
        when (tryPropertyId) {
            is ApiResult.Success -> return ApiResult.Success(parseWishlistToggleLike(tryPropertyId.data, defaultLiked = true))
            is ApiResult.Failure -> {
                if (!isLikelyContractMismatch(tryPropertyId.error)) {
                    return ApiResult.Failure(tryPropertyId.error)
                }
            }
        }

        // Fallback to the existing app endpoint used by Favorites tab.
        return toggleViaAccountEndpoint(listingId, desiredLiked = true)
    }

    suspend fun removeFromWishlist(listingId: String, wishlistItemId: String?): ApiResult<ToggleResult> {
        // If we happen to know the wishlist item id, attempt DELETE /v1/wishlists/{id}.
        if (!wishlistItemId.isNullOrBlank()) {
            val deleteUrl = appConfig.buildApiUrl("wishlists", wishlistItemId)
            when (val res = apiClient.delete(deleteUrl, includeAuth = true)) {
                is ApiResult.Success -> return ApiResult.Success(parseWishlistToggleLike(res.data, defaultLiked = false))
                is ApiResult.Failure -> {
                    // If delete doesn't exist on backend, fall back to toggle.
                    if (!isLikelyContractMismatch(res.error)) {
                        return ApiResult.Failure(res.error)
                    }
                }
            }
        }

        // Existing backend in this repo supports POST /v1/account/wishlist/toggle.
        return toggleViaAccountEndpoint(listingId, desiredLiked = false)
    }

    private suspend fun toggleViaAccountEndpoint(listingId: String, desiredLiked: Boolean): ApiResult<ToggleResult> {
        val url = appConfig.buildApiUrl("account", "wishlist", "toggle")
        val body = buildJsonObject {
            put("itemId", listingId)
            put("listingId", listingId)
        }.toString()

        return when (val res = apiClient.post(url, body, includeAuth = true)) {
            is ApiResult.Success -> ApiResult.Success(parseWishlistToggleLike(res.data, defaultLiked = desiredLiked))
            is ApiResult.Failure -> ApiResult.Failure(res.error)
        }
    }

    private suspend fun postJson(url: okhttp3.HttpUrl, bodyMap: Map<String, String>): ApiResult<String> {
        val body = buildJsonObject {
            bodyMap.forEach { (k, v) -> put(k, v) }
        }.toString()
        return apiClient.post(url, body, includeAuth = true)
    }

    private fun parseWishlistToggleLike(responseBody: String, defaultLiked: Boolean): ToggleResult {
        return try {
            val root = json.parseToJsonElement(responseBody).jsonObject
            val data = (root["data"] as? kotlinx.serialization.json.JsonObject) ?: root

            val liked = data["liked"]?.jsonPrimitive?.content?.toBooleanStrictOrNull()
                ?: data["isFavorite"]?.jsonPrimitive?.content?.toBooleanStrictOrNull()
                ?: data["is_favorite"]?.jsonPrimitive?.content?.toBooleanStrictOrNull()
                ?: defaultLiked

            val id = data["_id"]?.jsonPrimitive?.content
                ?: data["id"]?.jsonPrimitive?.content
                ?: data["wishlistId"]?.jsonPrimitive?.content
                ?: (data["wishlist"] as? kotlinx.serialization.json.JsonObject)?.get("_id")?.jsonPrimitive?.content

            ToggleResult(isFavorite = liked, wishlistItemId = id)
        } catch (_: Exception) {
            ToggleResult(isFavorite = defaultLiked, wishlistItemId = null)
        }
    }

    private fun isLikelyContractMismatch(error: ApiError): Boolean {
        // Treat missing endpoints / schema mismatches as contract mismatch.
        return when (error) {
            is ApiError.NotFound -> true
            is ApiError.ServerError -> error.code == 400 || error.code == 404
            else -> false
        }
    }
}

