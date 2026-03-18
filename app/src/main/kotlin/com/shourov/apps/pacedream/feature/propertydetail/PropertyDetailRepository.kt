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

            // Host profile details (iOS parity)
            val hostBio = hostObj?.string("bio", "about", "description")
            val hostIsSuperhost = hostObj?.bool("isSuperhost", "is_superhost")
            val hostIsVerified = hostObj?.bool("isVerified", "is_verified")

            // Property details (iOS parity)
            val propertyType = listing.string("propertyType", "property_type", "type", "category")
            val maxGuests = listing.int("maxGuests", "max_guests", "guests", "capacity")
            val bedrooms = listing.int("bedrooms", "bedroom_count")
            val beds = listing.int("beds", "bed_count")
            val bathrooms = listing.int("bathrooms", "bathroom_count")

            // House rules (iOS parity)
            val houseRules = (listing["houseRules"]?.asArrayOrNull() ?: listing["house_rules"]?.asArrayOrNull())
                ?.mapNotNull { el -> el.asStringOrNull() ?: el.asObjectOrNull()?.string("rule", "text", "name") }
                ?.filter { it.isNotBlank() }
                ?: emptyList()
            val checkInTime = listing.string("checkInTime", "check_in_time", "checkIn")
            val checkOutTime = listing.string("checkOutTime", "check_out_time", "checkOut")

            // Safety features (iOS parity)
            val safetyFeatures = (listing["safetyFeatures"]?.asArrayOrNull() ?: listing["safety_features"]?.asArrayOrNull())
                ?.mapNotNull { el -> el.asStringOrNull() ?: el.asObjectOrNull()?.string("name", "feature") }
                ?.filter { it.isNotBlank() }
                ?: emptyList()

            // Pricing breakdown (iOS parity)
            val basePrice = pricingObj?.double("basePrice", "base_price", "price")
                ?: listing.double("basePrice", "base_price", "price")
            val cleaningFee = pricingObj?.double("cleaningFee", "cleaning_fee")
            val serviceFee = pricingObj?.double("serviceFee", "service_fee")
            val weeklyDiscountPercent = pricingObj?.int("weeklyDiscountPercent", "weekly_discount_percent", "weeklyDiscount")
            val frequencyLabel = pricingObj?.string("frequencyLabel", "frequency_label", "frequency")

            // Status (iOS parity)
            val available = listing.bool("available", "isAvailable", "is_available")
            val instantBook = listing.bool("instantBook", "instant_book", "instantBooking")

            // Cancellation policy (iOS parity)
            val cancelObj = listing["cancellationPolicy"]?.asObjectOrNull()
                ?: listing["cancellation_policy"]?.asObjectOrNull()
            val cancellationPolicyType = cancelObj?.string("type") ?: listing.string("cancellationPolicy", "cancellation_policy")
            val cancellationPolicyDescription = cancelObj?.string("description")
            val cancellationFreeHoursBefore = cancelObj?.int("freeHoursBefore", "free_hours_before")

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
                hostBio = hostBio,
                hostIsSuperhost = hostIsSuperhost,
                hostIsVerified = hostIsVerified,
                amenities = amenities,
                rating = rating,
                reviewCount = reviewCount,
                propertyType = propertyType,
                maxGuests = maxGuests,
                bedrooms = bedrooms,
                beds = beds,
                bathrooms = bathrooms,
                houseRules = houseRules,
                checkInTime = checkInTime,
                checkOutTime = checkOutTime,
                safetyFeatures = safetyFeatures,
                basePrice = basePrice,
                cleaningFee = cleaningFee,
                serviceFee = serviceFee,
                weeklyDiscountPercent = weeklyDiscountPercent,
                frequencyLabel = frequencyLabel,
                available = available,
                instantBook = instantBook,
                cancellationPolicyType = cancellationPolicyType,
                cancellationPolicyDescription = cancellationPolicyDescription,
                cancellationFreeHoursBefore = cancellationFreeHoursBefore
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

private fun JsonObject.bool(vararg keys: String): Boolean? =
    keys.firstNotNullOfOrNull { k -> this[k]?.jsonPrimitive?.booleanOrNull }

