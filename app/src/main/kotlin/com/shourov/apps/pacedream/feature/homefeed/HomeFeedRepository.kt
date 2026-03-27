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
        fun backendUrl(params: Map<String, String?>): HttpUrl =
            appConfig.buildApiUrlWithQuery("listings", queryParams = params)

        suspend fun tryBackend(params: Map<String, String?>): ApiResult<List<HomeCard>> {
            val url = backendUrl(params)
            Timber.d("HomeFeed listings: ${url} (params=$params)")
            val res = apiClient.get(url, includeAuth = true)
            return when (res) {
                is ApiResult.Success -> ApiResult.Success(parseListingsToCards(res.data))
                is ApiResult.Failure -> res
            }
        }

        // Backend attempts: primary + fallback with status filter (avoid excessive retries)
        val attempts = listOf(
            mapOf("page" to page1.toString(), "limit" to limit.toString(), "shareType" to shareType),
            mapOf("page" to page1.toString(), "limit" to limit.toString(), "shareType" to shareType, "status" to "published"),
        )

        var lastFailure: ApiResult.Failure? = null
        for (params in attempts) {
            when (val tried = tryBackend(params)) {
                is ApiResult.Success -> if (tried.data.isNotEmpty()) return tried
                is ApiResult.Failure -> lastFailure = tried
            }
        }

        // Return whatever the backend gave us (even empty) rather than spamming fallbacks
        return lastFailure ?: ApiResult.Success(emptyList())
    }

    /**
     * Curated hourly payload: /v1/poc/listings?shareType=USE&status=published&limit=24&skip_pagination=true
     * Matches website endpoint for spaces data.
     * Fallback: standard /v1/listings?shareType=USE if poc endpoint is empty.
     */
    suspend fun getCuratedHourly(limit: Int = 24): ApiResult<List<HomeCard>> {
        val curatedUrl = appConfig.buildApiUrlWithQuery(
            "poc", "listings",
            queryParams = mapOf(
                "shareType" to "USE",
                "status" to "published",
                "limit" to limit.toString(),
                "skip_pagination" to "true"
            )
        )
        Timber.d("HomeFeed spaces: $curatedUrl")
        val curated = apiClient.get(curatedUrl, includeAuth = false)
        if (curated is ApiResult.Success) {
            val cards = parseListingsToCards(curated.data)
            if (cards.isNotEmpty()) return ApiResult.Success(cards)
        }

        // Fallback: standard listings endpoint with shareType=USE
        return getListingsShareTypePage(
            shareType = "USE",
            page1 = 1,
            limit = limit
        )
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
        return try {
            val obj = el as? JsonObject ?: return null
            val id = obj["_id"].stringOrNull()
                ?: obj["id"].stringOrNull()
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
                ?: (obj["gallery"] as? JsonObject)?.get("images")?.jsonArray?.firstOrNull().stringOrNull()
                ?: (obj["gallery"] as? JsonObject)?.get("thumbnail").stringOrNull()
                ?: (obj["galleryImages"] as? JsonArray)?.firstOrNull().stringOrNull()

            val rating = (obj["rating"] as? kotlinx.serialization.json.JsonPrimitive)?.doubleOrNull
                ?: (obj["avgRating"] as? kotlinx.serialization.json.JsonPrimitive)?.doubleOrNull

            // Try to extract frequency from any available source on the listing object.
            // pricingUnit is the top-level field the backend sends in list responses (iOS reads this).
            val standaloneFrequency = obj["pricingUnit"]?.stringOrNull()?.let { formatPriceUnit(it) }
                ?: obj["frequency"]?.stringOrNull()?.let { formatPriceUnit(it) }
                ?: (obj["pricing"] as? JsonObject)?.let { p ->
                    p["pricing_type"]?.stringOrNull()?.let { formatPriceUnit(it) }
                        ?: p["frequency"]?.stringOrNull()?.let { formatPriceUnit(it) }
                }
                ?: obj["dynamic_price"]?.let { dp ->
                    (dp as? JsonArray)?.firstOrNull()?.jsonObject?.get("frequency")?.stringOrNull()?.let { formatPriceUnit(it) }
                }

            val priceText = obj["priceText"].stringOrNull()
                ?: (obj["price"] as? JsonObject)?.let { p ->
                    val amount = p["amount"]?.stringOrNull()?.let { normalizePriceText(it) }
                    val frequency = p["frequency"]?.stringOrNull()?.let { formatPriceUnit(it) } ?: standaloneFrequency
                    if (amount != null && frequency != null) "$amount/$frequency" else amount
                }
                ?: (obj["price"] as? JsonArray)?.firstOrNull()?.jsonObject?.let { p ->
                    val amount = p["amount"]?.stringOrNull()?.let { normalizePriceText(it) }
                    val frequency = p["frequency"]?.stringOrNull()?.let { formatPriceUnit(it) } ?: standaloneFrequency
                    if (amount != null && frequency != null) "$amount/$frequency" else amount
                }
                ?: obj["price"].stringOrNull()?.let { raw ->
                    val amount = normalizePriceText(raw)
                    if (amount.isNotBlank() && standaloneFrequency != null) "$amount/$standaloneFrequency" else amount
                }
                ?: (obj["pricing"] as? JsonObject)?.let { pricing ->
                    val amount = (pricing["base_price"] ?: pricing["price"])?.stringOrNull()?.let { normalizePriceText(it) }
                    val frequency = pricing["frequency"]?.stringOrNull()?.let { formatPriceUnit(it) } ?: standaloneFrequency
                    if (amount != null && frequency != null) "$amount/$frequency" else amount
                }
                ?: obj["dynamic_price"]?.let { dp ->
                    (dp as? JsonArray)?.firstOrNull()?.jsonObject?.let { p ->
                        val amount = p["price"]?.stringOrNull()?.let { normalizePriceText(it) }
                        val frequency = p["frequency"]?.stringOrNull()?.let { formatPriceUnit(it) } ?: standaloneFrequency
                        if (amount != null && frequency != null) "$amount/$frequency" else amount
                    }
                }

            // Extract subcategory from multiple possible fields for resource type filtering.
            val subCategory = obj["subCategory"].stringOrNull()
                ?: obj["item_type"].stringOrNull()
                ?: (obj["details"] as? JsonObject)?.get("room_type").stringOrNull()
                ?: obj["roomType"].stringOrNull()

            val shareCategory = obj["shareCategory"].stringOrNull()

            HomeCard(
                id = id,
                title = title,
                location = location,
                imageUrl = imageUrl,
                priceText = priceText,
                rating = rating,
                subCategory = subCategory,
                shareCategory = shareCategory,
            )
        } catch (e: Exception) {
            Timber.w(e, "Failed to parse single card")
            null
        }
    }

    private fun normalizePriceText(raw: String): String {
        val s = raw.trim()
        if (s.isBlank()) return ""
        // If backend returns a plain number string, prefix with $
        val numeric = s.toDoubleOrNull()
        return if (numeric != null) "$${s}" else s
    }

    private fun formatPriceUnit(frequency: String): String {
        return when (frequency.lowercase().trim()) {
            "hourly", "hour", "hr" -> "hr"
            "daily", "day" -> "day"
            "weekly", "week", "wk" -> "wk"
            "monthly", "month", "mo" -> "mo"
            "once" -> "total"
            else -> frequency.lowercase()
        }
    }

    private fun JsonElement?.stringOrNull(): String? {
        if (this == null || this !is kotlinx.serialization.json.JsonPrimitive) return null
        val s = this.contentOrNull?.trim()
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

