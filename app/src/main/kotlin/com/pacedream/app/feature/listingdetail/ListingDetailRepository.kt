package com.pacedream.app.feature.listingdetail

import com.pacedream.app.core.config.AppConfig
import com.pacedream.app.core.network.ApiClient
import com.pacedream.app.core.network.ApiError
import com.pacedream.app.core.network.ApiResult
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ListingDetailRepository @Inject constructor(
    private val apiClient: ApiClient,
    private val appConfig: AppConfig,
    private val json: Json
) {
    suspend fun fetchListingDetail(listingId: String): ApiResult<ListingDetailModel> {
        val url = appConfig.buildApiUrl("listings", listingId)
        return when (val result = apiClient.get(url, includeAuth = true)) {
            is ApiResult.Success -> {
                val parsed = parseListingDetail(result.data)
                if (parsed != null) ApiResult.Success(parsed) else ApiResult.Failure(ApiError.DecodingError())
            }

            is ApiResult.Failure -> result
        }
    }

    /**
     * Tolerant parsing for GET /v1/listings/{id}
     */
    private fun parseListingDetail(responseBody: String): ListingDetailModel? {
        return try {
            val root = json.parseToJsonElement(responseBody)
            val obj = root.jsonObject

            val data = obj["data"]?.asObjectOrNull() ?: obj
            val listing = data["listing"]?.asObjectOrNull()
                ?: data["item"]?.asObjectOrNull()
                ?: data

            val id = listing.string("id", "_id") ?: return null
            val title = listing.string("title", "name") ?: "Listing"

            val description = listing.string("description", "about")

            val imageUrls = buildList {
                val images = listing["images"]
                when (images) {
                    is JsonArray -> images.forEach { el ->
                        el.asStringOrNull()?.let { add(it) }
                        el.asObjectOrNull()?.string("url", "src")?.let { add(it) }
                    }
                    else -> Unit
                }
                listing.string("cover", "coverImage", "image")?.let { add(0, it) }
            }.distinct().filter { it.isNotBlank() }

            val locationObj = listing["location"]?.asObjectOrNull()
            val location = ListingLocation(
                city = locationObj?.string("city") ?: listing.string("city"),
                state = locationObj?.string("state") ?: listing.string("state"),
                address = locationObj?.string("address", "street") ?: listing.string("address"),
                latitude = locationObj?.double("latitude", "lat") ?: listing.double("latitude", "lat"),
                longitude = locationObj?.double("longitude", "lng", "lon") ?: listing.double("longitude", "lng", "lon")
            )

            val pricing = parsePricing(listing)

            val hostObj = listing["host"]?.asObjectOrNull()
                ?: listing["user"]?.asObjectOrNull()
                ?: listing["owner"]?.asObjectOrNull()
            val host = hostObj?.let {
                ListingHost(
                    id = it.string("id", "_id"),
                    name = it.string("name") ?: listOfNotNull(
                        it.string("firstName", "first_name"),
                        it.string("lastName", "last_name")
                    ).joinToString(" ").ifBlank { null },
                    avatarUrl = it.string("avatarUrl", "avatar", "profileImage", "profile_image")
                )
            }

            val amenities = parseAmenities(listing)

            val rating = listing.double("rating") ?: listing["reviews"]?.asObjectOrNull()?.double("rating")
            val reviewCount = listing.int("reviewCount", "reviewsCount", "review_count")
                ?: listing["reviews"]?.asObjectOrNull()?.int("count")

            val isFavorite = listing.boolean("liked", "isLiked", "isFavorited", "is_wishlisted")

            ListingDetailModel(
                id = id,
                title = title,
                description = description,
                imageUrls = imageUrls,
                location = location.takeIf { it.cityState != null || it.fullAddress != null || it.latitude != null || it.longitude != null },
                pricing = pricing,
                host = host,
                amenities = amenities,
                rating = rating,
                reviewCount = reviewCount,
                isFavorite = isFavorite
            )
        } catch (e: Exception) {
            Timber.e(e, "Failed to parse listing detail")
            null
        }
    }

    private fun parsePricing(listing: JsonObject): ListingPricing? {
        return try {
            val pricingObj = listing["pricing"]?.asObjectOrNull() ?: listing["price"]?.asObjectOrNull()
            val currency = pricingObj?.string("currency") ?: listing.string("currency")

            val hourlyFrom =
                pricingObj?.double("hourlyFrom", "hourly_from")
                    ?: listing.double("hourlyFrom", "hourly_from")
                    ?: listing["dynamic_price"]?.asArrayOrNull()?.firstOrNull()?.asObjectOrNull()?.double("price")

            val basePrice =
                pricingObj?.double("base_price", "basePrice", "amount")
                    ?: listing.double("base_price", "basePrice", "amount")

            val frequency =
                pricingObj?.string("frequency", "unit", "frequencyLabel")
                    ?: listing.string("frequency", "unit", "frequencyLabel")

            ListingPricing(
                hourlyFrom = hourlyFrom,
                basePrice = basePrice,
                currency = currency,
                frequencyLabel = frequency
            ).takeIf { it.hourlyFrom != null || it.basePrice != null }
        } catch (_: Exception) {
            null
        }
    }

    private fun parseAmenities(listing: JsonObject): List<String> {
        val raw = listing["amenities"] ?: listing["highlights"] ?: return emptyList()
        val array = raw.asArrayOrNull() ?: return emptyList()
        return array.mapNotNull { el ->
            el.asStringOrNull()
                ?: el.asObjectOrNull()?.string("name", "title", "label")
        }.map { it.trim() }.filter { it.isNotBlank() }.distinct()
    }
}

private fun JsonElement.asObjectOrNull(): JsonObject? = this as? JsonObject
private fun JsonElement.asArrayOrNull(): JsonArray? = this as? JsonArray

private fun JsonObject.string(vararg keys: String): String? =
    keys.firstNotNullOfOrNull { k -> this[k]?.asStringOrNull()?.takeIf { it.isNotBlank() } }

private fun JsonObject.double(vararg keys: String): Double? =
    keys.firstNotNullOfOrNull { k -> this[k]?.jsonPrimitive?.doubleOrNull }

private fun JsonObject.int(vararg keys: String): Int? =
    keys.firstNotNullOfOrNull { k -> this[k]?.jsonPrimitive?.intOrNull }

private fun JsonObject.boolean(vararg keys: String): Boolean? =
    keys.firstNotNullOfOrNull { k -> this[k]?.jsonPrimitive?.booleanOrNull }

private fun JsonElement.asStringOrNull(): String? = runCatching { this.jsonPrimitive.content }.getOrNull()

