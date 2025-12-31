package com.shourov.apps.pacedream.feature.propertydetail

import com.shourov.apps.pacedream.core.network.api.ApiClient
import com.shourov.apps.pacedream.core.network.api.ApiError
import com.shourov.apps.pacedream.core.network.api.ApiResult
import com.shourov.apps.pacedream.core.network.config.AppConfig
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
class PropertyDetailRepository @Inject constructor(
    private val apiClient: ApiClient,
    private val appConfig: AppConfig,
    private val json: Json
) {
    suspend fun getListingDetail(listingId: String): ApiResult<PropertyDetailModel> {
        val url = appConfig.buildApiUrl("listings", listingId)
        return when (val res = apiClient.get(url, includeAuth = true)) {
            is ApiResult.Success -> {
                val parsed = parse(res.data)
                if (parsed != null) {
                    ApiResult.Success(parsed)
                } else {
                    ApiResult.Failure(ApiError.DecodingError("Failed to parse listing detail response."))
                }
            }
            is ApiResult.Failure -> res
        }
    }

    private fun parse(body: String): PropertyDetailModel? {
        return try {
            val root = json.parseToJsonElement(body).jsonObject
            val data = root["data"]?.asObjectOrNull() ?: root
            val listing = data["listing"]?.asObjectOrNull()
                ?: data["item"]?.asObjectOrNull()
                ?: data

            val id = listing.string("_id", "id") ?: return null
            val title = listing.string("title", "name") ?: "Listing"

            val description = listing.string(
                "description",
                "about",
                "details",
                "summary",
                "longDescription",
                "long_description",
                "shortDescription",
                "short_description",
                "desc"
            )

            val images = buildList {
                val imagesEl = listing["images"]
                when (imagesEl) {
                    is JsonArray -> imagesEl.forEach { el ->
                        el.asStringOrNull()?.let { add(it) }
                        el.asObjectOrNull()?.string("url", "src")?.let { add(it) }
                    }
                    else -> Unit
                }
                listing.string("cover", "coverImage", "image")?.let { add(0, it) }
            }.distinct().filter { it.isNotBlank() }

            val locationObj = listing["location"]?.asObjectOrNull()
            val addressObj = listing["address"]?.asObjectOrNull()
            val city = locationObj?.string("city") ?: addressObj?.string("city") ?: listing.string("city")
            val state = locationObj?.string("state") ?: addressObj?.string("state") ?: listing.string("state")
            val address = locationObj?.string("address", "street")
                ?: addressObj?.string("address", "street")
                ?: listing.string("address")

            val lat = locationObj?.double("latitude", "lat")
                ?: addressObj?.double("latitude", "lat")
                ?: listing.double("latitude", "lat")
            val lng = locationObj?.double("longitude", "lng", "lon")
                ?: addressObj?.double("longitude", "lng", "lon")
                ?: listing.double("longitude", "lng", "lon")

            // Pricing
            val pricingObj = listing["pricing"]?.asObjectOrNull()
                ?: listing["price"]?.asObjectOrNull()
            val currency = pricingObj?.string("currency") ?: listing.string("currency")
            val hourlyFrom = pricingObj?.double("hourlyFrom", "hourly_from")
                ?: listing.double("hourlyFrom", "hourly_from")
                ?: listing["dynamic_price"]?.asArrayOrNull()?.firstOrNull()?.asObjectOrNull()?.double("price")

            // Host
            val hostObj = listing["host"]?.asObjectOrNull()
                ?: listing["user"]?.asObjectOrNull()
                ?: listing["owner"]?.asObjectOrNull()
            val hostName = hostObj?.string("name")
                ?: hostObj?.let {
                    val first = it.string("firstName", "first_name")
                    val last = it.string("lastName", "last_name")
                    listOfNotNull(first, last).joinToString(" ").trim().ifBlank { null }
                }
            val hostAvatar = hostObj?.string("avatarUrl", "avatar", "profileImage", "profile_image")

            // Amenities
            val amenities = (listing["amenities"]?.asArrayOrNull() ?: listing["highlights"]?.asArrayOrNull())
                ?.mapNotNull { el -> el.asStringOrNull() ?: el.asObjectOrNull()?.string("name", "title", "label") }
                ?.map { it.trim() }
                ?.filter { it.isNotBlank() }
                ?.distinct()
                ?: emptyList()

            val rating = listing.double("rating")
            val reviewCount = listing.int("reviewCount", "reviewsCount", "review_count")

            PropertyDetailModel(
                id = id,
                title = title,
                description = description,
                imageUrls = images,
                city = city,
                state = state,
                address = address,
                latitude = lat,
                longitude = lng,
                hourlyFrom = hourlyFrom,
                currency = currency,
                hostName = hostName,
                hostAvatarUrl = hostAvatar,
                amenities = amenities,
                rating = rating,
                reviewCount = reviewCount
            )
        } catch (e: Exception) {
            Timber.e(e, "Failed to parse listing detail")
            null
        }
    }
}

private fun JsonElement.asObjectOrNull(): JsonObject? = this as? JsonObject
private fun JsonElement.asArrayOrNull(): JsonArray? = this as? JsonArray
private fun JsonElement.asStringOrNull(): String? = runCatching { this.jsonPrimitive.content }.getOrNull()

private fun JsonObject.string(vararg keys: String): String? =
    keys.firstNotNullOfOrNull { k -> this[k]?.asStringOrNull()?.takeIf { it.isNotBlank() } }

private fun JsonObject.double(vararg keys: String): Double? =
    keys.firstNotNullOfOrNull { k -> this[k]?.jsonPrimitive?.doubleOrNull }

private fun JsonObject.int(vararg keys: String): Int? =
    keys.firstNotNullOfOrNull { k -> this[k]?.jsonPrimitive?.intOrNull }

