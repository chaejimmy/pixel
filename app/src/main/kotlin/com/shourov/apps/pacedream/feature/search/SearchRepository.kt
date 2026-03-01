package com.shourov.apps.pacedream.feature.search

import com.shourov.apps.pacedream.core.network.api.ApiClient
import com.shourov.apps.pacedream.core.network.api.ApiResult
import com.shourov.apps.pacedream.core.network.config.AppConfig
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.contentOrNull
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
     * - Fallback: GET /v1/search?... (backend) when primary fails.
     *
     * The web /api/search endpoint requires:
     *   category = time-based | hourly-rental-gears | room-stays | find-roommates
     *   q        = text/keyword search (WHAT field)
     *   location = city/area filter   (WHERE field)
     *   limit / offset  for pagination (NOT page/perPage)
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
        // Map shareType to the web API's required "category" parameter
        val webCategory = category?.takeIf { it in VALID_WEB_CATEGORIES }
            ?: mapShareTypeToCategory(shareType)

        val offset = page0 * perPage

        // Build the text query: prefer whatQuery, fall back to q (WHERE)
        val textQuery = whatQuery?.takeIf { it.isNotBlank() }
            ?: q.takeIf { it.isNotBlank() }

        val primaryUrl = appConfig.frontendBaseUrl.newBuilder()
            .addPathSegment("api")
            .addPathSegment("search")
            .addQueryParameter("category", webCategory)
            .addQueryParameter("limit", perPage.toString())
            .addQueryParameter("offset", offset.toString())
            .apply {
                if (!textQuery.isNullOrBlank()) addQueryParameter("q", textQuery)
                if (!city.isNullOrBlank()) addQueryParameter("location", city)
                if (!sort.isNullOrBlank()) addQueryParameter("sort", sort)
                if (!shareType.isNullOrBlank()) addQueryParameter("shareType", shareType)
                if (!startDate.isNullOrBlank() && !endDate.isNullOrBlank()) {
                    addQueryParameter("date", "$startDate,$endDate")
                } else if (!startDate.isNullOrBlank()) {
                    addQueryParameter("date", startDate)
                }
            }
            .build()

        Timber.d("Search primary URL: $primaryUrl")
        val primary = apiClient.get(primaryUrl, includeAuth = false)
        if (primary is ApiResult.Success) {
            return ApiResult.Success(parseSearchPage(primary.data, perPage))
        }

        // Fallback: backend /v1/search endpoint
        val fallbackUrl = appConfig.buildApiUrlWithQuery(
            "search",
            queryParams = mapOf(
                "q" to (textQuery ?: ""),
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

        Timber.d("Search fallback URL: $fallbackUrl")
        return when (val fallback = apiClient.get(fallbackUrl, includeAuth = false)) {
            is ApiResult.Success -> ApiResult.Success(parseSearchPage(fallback.data, perPage))
            is ApiResult.Failure -> fallback
        }
    }

    companion object {
        /** Valid category values accepted by the web /api/search endpoint */
        private val VALID_WEB_CATEGORIES = setOf(
            "time-based", "hourly-rental-gears", "room-stays", "find-roommates"
        )

        /** Map shareType (USE/BORROW/SPLIT) to the web API category */
        private fun mapShareTypeToCategory(shareType: String?): String {
            return when (shareType?.uppercase()) {
                "BORROW" -> "hourly-rental-gears"
                "SPLIT" -> "room-stays"
                else -> "time-based" // USE or default
            }
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
        return try {
            val root = json.parseToJsonElement(body)
            val arr = findArray(
                root,
                listOf("items"),
                listOf("listings"),
                listOf("data", "listings"),
                listOf("data", "items"),
                listOf("data")
            ) ?: JsonArray(emptyList())
            val items = arr.mapNotNull { el -> parseSearchItem(el) }

            // Prefer the explicit hasMore from the web API response; fall back to heuristic
            val hasMore = (root as? JsonObject)?.get("hasMore")
                ?.jsonPrimitive?.booleanOrNull
                ?: (items.size >= perPage)

            SearchPage(items = items, hasMore = hasMore)
        } catch (e: Exception) {
            Timber.e(e, "Failed to parse search results")
            SearchPage(items = emptyList(), hasMore = false)
        }
    }

    @VisibleForTesting
    internal fun parseSearchPageForTest(body: String, perPage: Int): SearchPage = parseSearchPage(body, perPage)

    private fun parseSearchItem(el: JsonElement): SearchResultItem? = try {
        val obj = el as? JsonObject ?: return null
        val id = obj["_id"].stringOrNull()
            ?: obj["id"].stringOrNull()
            ?: obj["listingId"].stringOrNull()
            ?: return null

        val title = obj["title"].stringOrNull()
            ?: obj["name"].stringOrNull()
            ?: "Listing"

        val location = obj["city"].stringOrNull()
            ?: (obj["location"] as? JsonObject)?.get("city").stringOrNull()
            ?: obj["location"].stringOrNull()
            ?: (obj["address"] as? JsonObject)?.get("city").stringOrNull()

        val imageUrl = obj["image"].stringOrNull()
            ?: obj["imageUrl"].stringOrNull()
            ?: obj["thumbnail"].stringOrNull()
            ?: (obj["images"] as? JsonArray)?.firstOrNull().stringOrNull()
            ?: (obj["gallery"] as? JsonObject)?.let { gallery ->
                (gallery["images"] as? JsonArray)?.firstOrNull().stringOrNull()
            }
            ?: (obj["galleryImages"] as? JsonArray)?.firstOrNull().stringOrNull()

        val rating = obj["rating"]?.jsonPrimitive?.doubleOrNull
            ?: obj["avgRating"]?.jsonPrimitive?.doubleOrNull

        val priceText = obj["priceText"].stringOrNull()
            ?: (obj["price"] as? kotlinx.serialization.json.JsonPrimitive)?.doubleOrNull?.let { normalizePriceNumber(it) }
            ?: obj["price"].stringOrNull()?.let { normalizePriceText(it) }
            ?: (obj["price"] as? JsonObject)?.get("amount")?.stringOrNull()?.let { normalizePriceText(it) }
            ?: (obj["pricing"] as? JsonObject)?.get("price")?.stringOrNull()?.let { normalizePriceText(it) }

        SearchResultItem(
            id = id,
            title = title,
            location = location,
            imageUrl = imageUrl,
            priceText = priceText,
            rating = rating
        )
    } catch (e: Exception) {
        Timber.w(e, "Skipping unparseable search item")
        null
    }

    private fun normalizePriceText(raw: String): String {
        val s = raw.trim()
        if (s.isBlank()) return ""
        val numeric = s.toDoubleOrNull()
        return if (numeric != null) normalizePriceNumber(numeric) else s
    }

    private fun normalizePriceNumber(value: Double): String {
        if (value <= 0.0) return ""
        return if (value == value.toLong().toDouble()) {
            "$${value.toLong()}"
        } else {
            "$${String.format("%.2f", value)}"
        }
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

