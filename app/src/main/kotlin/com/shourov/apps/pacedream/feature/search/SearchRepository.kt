package com.shourov.apps.pacedream.feature.search

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
import androidx.annotation.VisibleForTesting
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SearchRepository @Inject constructor(
    private val apiClient: ApiClient,
    private val appConfig: AppConfig,
    private val json: Json
) {
    suspend fun autocompleteWhere(query: String): ApiResult<List<AutocompleteSuggestion>> {
        val url = appConfig.buildApiUrlWithQuery(
            "search", "autocomplete",
            queryParams = mapOf("q" to query)
        )

        return when (val res = apiClient.get(url, includeAuth = false)) {
            is ApiResult.Success -> ApiResult.Success(parseAutocomplete(res.data))
            is ApiResult.Failure -> res
        }
    }

    /**
     * iOS parity:
     * - Primary: GET https://www.pacedream.com/api/search?... (frontend)
     * - Fallback: GET /v1/search?... (backend) ONLY when q is non-empty and primary fails.
     */
    suspend fun search(
        q: String,
        city: String? = null,
        category: String? = null,
        page0: Int,
        perPage: Int,
        sort: String? = null,
        shareType: String? = null, // USE, BORROW, or SPLIT
        whatQuery: String? = null, // Keywords search
        startDate: String? = null, // ISO date string
        endDate: String? = null // ISO date string
    ): ApiResult<SearchPage> {
        val primaryUrl = appConfig.frontendBaseUrl.newBuilder()
            .addPathSegment("api")
            .addPathSegment("search")
            .addQueryParameter("q", q)
            .addQueryParameter("page", page0.toString())
            .addQueryParameter("perPage", perPage.toString())
            .apply {
                if (!city.isNullOrBlank()) addQueryParameter("city", city)
                if (!category.isNullOrBlank()) addQueryParameter("category", category)
                if (!sort.isNullOrBlank()) addQueryParameter("sort", sort)
                if (!shareType.isNullOrBlank()) addQueryParameter("shareType", shareType)
                if (!whatQuery.isNullOrBlank()) addQueryParameter("what", whatQuery)
                if (!startDate.isNullOrBlank()) addQueryParameter("startDate", startDate)
                if (!endDate.isNullOrBlank()) addQueryParameter("endDate", endDate)
            }
            .build()

        val primary = apiClient.get(primaryUrl, includeAuth = false)
        if (primary is ApiResult.Success) {
            return ApiResult.Success(parseSearchPage(primary.data, perPage))
        }

        // Fallback only when q is non-empty (per requirements)
        if (q.isBlank()) {
            return (primary as? ApiResult.Failure) ?: ApiResult.Failure(ApiError.Unknown("Search failed"))
        }

        val fallbackUrl = appConfig.buildApiUrlWithQuery(
            "search",
            queryParams = mapOf(
                "q" to q,
                "page" to page0.toString(),
                "perPage" to perPage.toString(),
                "city" to city,
                "category" to category,
                "sort" to sort,
                "shareType" to shareType,
                "what" to whatQuery,
                "startDate" to startDate,
                "endDate" to endDate
            )
        )

        return when (val fallback = apiClient.get(fallbackUrl, includeAuth = false)) {
            is ApiResult.Success -> ApiResult.Success(parseSearchPage(fallback.data, perPage))
            is ApiResult.Failure -> fallback
        }
    }

    /**
     * Category results (iOS parity):
     * GET /v1/listings?page=<page1>&limit=24&shareType=<USE/BORROW/SPLIT>&category=<...>&city=<...>&sort=<...>
     * Tolerant response parsing (data.listings, listings, data array, etc.)
     */
    suspend fun categoryResults(
        page1: Int,
        limit: Int,
        shareType: String?,
        listingType: String? = null,
        category: String? = null,
        city: String? = null,
        sort: String? = null
    ): ApiResult<SearchPage> {
        val url = appConfig.buildApiUrlWithQuery(
            "listings",
            queryParams = mapOf(
                "page" to page1.toString(),
                "limit" to limit.toString(),
                "shareType" to shareType,
                "listing_type" to listingType,
                "category" to category,
                "city" to city,
                "sort" to sort
            )
        )

        return when (val res = apiClient.get(url, includeAuth = false)) {
            is ApiResult.Success -> ApiResult.Success(parseSearchPage(res.data, limit))
            is ApiResult.Failure -> res
        }
    }

    private fun parseAutocomplete(body: String): List<AutocompleteSuggestion> {
        return try {
            val root = json.parseToJsonElement(body)
            val items = when (root) {
                is JsonArray -> root
                is JsonObject -> {
                    val data = root["data"]
                    when (data) {
                        is JsonArray -> data
                        is JsonObject -> (data["items"] as? JsonArray) ?: (data["suggestions"] as? JsonArray)
                        else -> (root["items"] as? JsonArray) ?: (root["suggestions"] as? JsonArray)
                    }
                }
                else -> null
            } ?: return emptyList()

            items.mapNotNull { el ->
                val obj = el as? JsonObject
                val value = obj?.get("value")?.jsonPrimitive?.content
                    ?: obj?.get("name")?.jsonPrimitive?.content
                    ?: (el as? kotlinx.serialization.json.JsonPrimitive)?.content
                value?.takeIf { it.isNotBlank() }?.let { AutocompleteSuggestion(it) }
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to parse autocomplete")
            emptyList()
        }
    }

    /**
     * Tolerant search results parsing.
     * Accepts:
     * - direct array
     * - { items: [] } / { listings: [] } / { data: [] }
     * - { data: { items: [] } } etc.
     */
    private fun parseSearchPage(body: String, perPage: Int): SearchPage {
        val items = try {
            val root = json.parseToJsonElement(body)
            val arr = findArray(
                root,
                listOf("items"),
                listOf("listings"),
                listOf("data", "listings"),
                listOf("data", "items"),
                listOf("data")
            ) ?: JsonArray(emptyList())
            arr.mapNotNull { el -> parseSearchItem(el) }
        } catch (e: Exception) {
            Timber.e(e, "Failed to parse search results")
            emptyList()
        }

        // Conservative hasMore: if we received a full page, assume more.
        val hasMore = items.size >= perPage
        return SearchPage(items = items, hasMore = hasMore)
    }

    @VisibleForTesting
    internal fun parseSearchPageForTest(body: String, perPage: Int): SearchPage = parseSearchPage(body, perPage)

    private fun parseSearchItem(el: JsonElement): SearchResultItem? {
        val obj = el as? JsonObject ?: return null
        val id = obj["_id"].stringOrNull()
            ?: obj["id"].stringOrNull()
            ?: obj["listingId"].stringOrNull()
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

        return SearchResultItem(
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
        val numeric = s.toDoubleOrNull()
        return if (numeric != null) "$${s}" else s
    }

    private fun JsonElement?.stringOrNull(): String? {
        val s = this?.jsonPrimitive?.contentOrNull?.trim()
        return s?.takeIf { it.isNotBlank() }
    }

    private fun findArray(root: JsonElement, vararg paths: List<String>): JsonArray? {
        for (path in paths) {
            val found = navigate(root, path)
            when (found) {
                is JsonArray -> return found
                is JsonObject -> {
                    // If path points to object, try common nested keys
                    found["items"]?.let { if (it is JsonArray) return it }
                    found["listings"]?.let { if (it is JsonArray) return it }
                    found["data"]?.let { if (it is JsonArray) return it }
                }
                else -> Unit
            }
        }
        return null
    }

    private fun navigate(root: JsonElement, path: List<String>): JsonElement? {
        var current: JsonElement? = root
        for (key in path) {
            val obj = current as? JsonObject ?: return null
            current = obj[key]
        }
        return current
    }
}

