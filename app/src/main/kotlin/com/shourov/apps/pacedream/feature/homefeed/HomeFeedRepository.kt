package com.shourov.apps.pacedream.feature.homefeed

import com.shourov.apps.pacedream.core.network.api.ApiClient
import com.shourov.apps.pacedream.core.network.api.ApiError
import com.shourov.apps.pacedream.core.network.api.ApiResult
import com.shourov.apps.pacedream.core.network.config.AppConfig
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.contentOrNull
import okhttp3.HttpUrl
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HomeFeedRepository @Inject constructor(
    private val apiClient: ApiClient,
    private val appConfig: AppConfig,
    private val json: Json
) {
    /**
     * Primary: /v1/listings?page=1&limit=...&shareType=USE|BORROW|SPLIT
     * Fallback (only if backend listings fails): https://www.pacedream.com/api/proxy/listings?...&skip_pagination=true
     */
    suspend fun getListingsShareTypePage(
        shareType: String,
        page1: Int,
        limit: Int
    ): ApiResult<List<HomeCard>> {
        val backendUrl = appConfig.buildApiUrlWithQuery(
            "listings",
            queryParams = mapOf(
                "page" to page1.toString(),
                "limit" to limit.toString(),
                "shareType" to shareType
            )
        )

        val backend = apiClient.get(backendUrl, includeAuth = false)
        if (backend is ApiResult.Success) {
            return ApiResult.Success(parseListingsToCards(backend.data))
        }

        // Fallback only if backend fails
        val fallbackUrl = appConfig.buildFrontendUrl("api", "proxy", "listings")
            .newBuilder()
            .addQueryParameter("shareType", shareType)
            .addQueryParameter("status", "published")
            .addQueryParameter("limit", limit.toString())
            .addQueryParameter("skip_pagination", "true")
            .build()

        val fallback = apiClient.get(fallbackUrl, includeAuth = false)
        return when (fallback) {
            is ApiResult.Success -> ApiResult.Success(parseListingsToCards(fallback.data))
            is ApiResult.Failure -> fallback
        }
    }

    /**
     * Curated hourly payload: /v1/properties/filter-rentable-items-by-group/time_based?item_type=room
     * Fallback: /v1/rooms/search if time_based is empty.
     */
    suspend fun getCuratedHourly(limit: Int = 24): ApiResult<List<HomeCard>> {
        val curatedUrl = appConfig.buildApiUrlWithQuery(
            "properties", "filter-rentable-items-by-group", "time_based",
            queryParams = mapOf("item_type" to "room")
        )
        val curated = apiClient.get(curatedUrl, includeAuth = false)
        if (curated is ApiResult.Success) {
            val cards = parseListingsToCards(curated.data)
            if (cards.isNotEmpty()) return ApiResult.Success(cards)
        }

        val fallbackUrl = appConfig.buildApiUrlWithQuery(
            "rooms", "search",
            queryParams = mapOf("limit" to limit.toString())
        )
        val fallback = apiClient.get(fallbackUrl, includeAuth = false)
        return when (fallback) {
            is ApiResult.Success -> ApiResult.Success(parseListingsToCards(fallback.data))
            is ApiResult.Failure -> fallback
        }
    }

    /**
     * Tolerant parsing for listing-like responses:
     * - { data: [] } or { data: { listings: [] } } or { listings: [] } or { items: [] } or raw []
     */
    fun parseListingsToCards(body: String): List<HomeCard> {
        return try {
            val root = json.parseToJsonElement(body)
            val arr = findAnyArray(root) ?: return emptyList()
            arr.mapNotNull { parseCard(it) }
        } catch (e: Exception) {
            Timber.e(e, "Failed to parse listings/cards")
            emptyList()
        }
    }

    private fun parseCard(el: JsonElement): HomeCard? {
        val obj = el as? JsonObject ?: return null
        val id = obj["_id"].stringOrNull()
            ?: obj["id"].stringOrNull()
            ?: return null

        val title = obj["title"].stringOrNull()
            ?: obj["name"].stringOrNull()
            ?: "Listing"

        val location = obj["city"].stringOrNull()
            ?: obj["location"]?.jsonObject?.get("city").stringOrNull()
            ?: obj["location"].stringOrNull()
            ?: obj["address"]?.jsonObject?.get("city").stringOrNull()

        val imageUrl = obj["image"].stringOrNull()
            ?: obj["imageUrl"].stringOrNull()
            ?: obj["thumbnail"].stringOrNull()
            ?: obj["images"]?.jsonArray?.firstOrNull().stringOrNull()
            ?: obj["gallery"]?.jsonObject?.get("images")?.jsonArray?.firstOrNull().stringOrNull()
            ?: obj["galleryImages"]?.jsonArray?.firstOrNull().stringOrNull()

        val rating = obj["rating"]?.jsonPrimitive?.doubleOrNull
            ?: obj["avgRating"]?.jsonPrimitive?.doubleOrNull

        val priceText = obj["priceText"].stringOrNull()
            ?: obj["price"].stringOrNull()?.let { normalizePriceText(it) }
            ?: obj["price"]?.jsonObject?.get("amount")?.stringOrNull()?.let { normalizePriceText(it) }
            ?: obj["pricing"]?.jsonObject?.get("price")?.stringOrNull()?.let { normalizePriceText(it) }

        return HomeCard(
            id = id,
            title = title,
            location = location,
            imageUrl = imageUrl,
            priceText = priceText,
            rating = rating
        )
    }

    private fun normalizePriceText(raw: String): String {
        val s = raw.trim()
        if (s.isBlank()) return ""
        // If backend returns a plain number string, prefix with $
        val numeric = s.toDoubleOrNull()
        return if (numeric != null) "$${s}" else s
    }

    private fun JsonElement?.stringOrNull(): String? {
        val s = this?.jsonPrimitive?.contentOrNull?.trim()
        return s?.takeIf { it.isNotBlank() }
    }

    private fun findAnyArray(root: JsonElement): JsonArray? {
        return when (root) {
            is JsonArray -> root
            is JsonObject -> {
                // common keys
                listOf("listings", "items", "data").forEach { key ->
                    val v = root[key]
                    when (v) {
                        is JsonArray -> return v
                        is JsonObject -> {
                            v["listings"]?.let { if (it is JsonArray) return it }
                            v["items"]?.let { if (it is JsonArray) return it }
                            v["data"]?.let { if (it is JsonArray) return it }
                        }
                        else -> Unit
                    }
                }
                // deep scan
                root.values.forEach { child ->
                    val found = findAnyArray(child)
                    if (found != null) return found
                }
                null
            }
            else -> null
        }
    }
}

