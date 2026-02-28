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
    /**
     * Fetch listing detail using type-specific endpoints with fallback.
     *
     * The backend does not expose a unified /listings/{id} endpoint.
     * Each listing type lives under its own resource path:
     *   time-based → GET /v1/properties/{id}
     *   gear       → GET /v1/gear-rentals/{id}
     *   split-stay → GET /v1/roommate/{id}
     */
    suspend fun fetchListingDetail(
        listingId: String,
        listingType: String = ""
    ): ApiResult<ListingDetailModel> {
        val urls = buildDetailUrls(listingId, listingType)

        for (url in urls) {
            when (val result = apiClient.get(url, includeAuth = true)) {
                is ApiResult.Success -> {
                    val parsed = parseListingDetail(result.data)
                    if (parsed != null) return ApiResult.Success(parsed)
                }
                is ApiResult.Failure -> {
                    if (result.error !is ApiError.NotFound) return result
                }
            }
        }
        return ApiResult.Failure(ApiError.NotFound)
    }

    private fun buildDetailUrls(
        listingId: String,
        listingType: String
    ): List<okhttp3.HttpUrl> {
        val typeSpecific = when (listingType) {
            "time-based" -> listOf(
                appConfig.buildApiUrl("properties", listingId)
            )
            "gear" -> listOf(
                appConfig.buildApiUrl("gear-rentals", listingId),
                appConfig.buildApiUrl("gear-rentals", "get", listingId)
            )
            "split-stay" -> listOf(
                appConfig.buildApiUrl("roommate", listingId),
                appConfig.buildApiUrl("roommate", "get", listingId)
            )
            else -> emptyList()
        }
        return typeSpecific + listOf(appConfig.buildApiUrl("listings", listingId))
    }

    /**
     * Tolerant JSON parsing – handles multiple response shapes.
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
                longitude = locationObj?.double("longitude", "lng", "lon") ?: listing.double("longitude", "lng", "lon"),
                country = locationObj?.string("country") ?: listing.string("country"),
                zipCode = locationObj?.string("zipCode", "zip_code", "postalCode", "postal_code") ?: listing.string("zipCode", "zip_code"),
                neighborhood = locationObj?.string("neighborhood", "neighbourhood", "area") ?: listing.string("neighborhood")
            )

            val pricing = parsePricing(listing)

            val hostObj = listing["host"]?.asObjectOrNull()
                ?: listing["user"]?.asObjectOrNull()
                ?: listing["owner"]?.asObjectOrNull()
            val host = hostObj?.let {
                val verifications = buildList {
                    it["verifications"]?.asArrayOrNull()?.forEach { v ->
                        v.asStringOrNull()?.let { s -> add(s) }
                        v.asObjectOrNull()?.string("type", "method")?.let { s -> add(s) }
                    }
                    // Also check flat boolean fields
                    if (it.boolean("emailVerified", "email_verified") == true) add("email")
                    if (it.boolean("phoneVerified", "phone_verified") == true) add("phone")
                    if (it.boolean("identityVerified", "identity_verified") == true) add("identity")
                }.distinct()

                ListingHost(
                    id = it.string("id", "_id"),
                    name = it.string("name") ?: listOfNotNull(
                        it.string("firstName", "first_name"),
                        it.string("lastName", "last_name")
                    ).joinToString(" ").ifBlank { null },
                    avatarUrl = it.string("avatarUrl", "avatar", "profileImage", "profile_image"),
                    bio = it.string("bio", "about", "description"),
                    isSuperhost = it.boolean("isSuperhost", "is_superhost", "superhost"),
                    isVerified = it.boolean("isVerified", "is_verified", "verified"),
                    responseRate = it.int("responseRate", "response_rate"),
                    responseTime = it.string("responseTime", "response_time"),
                    listingCount = it.int("listingCount", "listing_count", "listingsCount"),
                    joinedDate = it.string("joinedDate", "joined_date", "createdAt", "created_at"),
                    verifications = verifications
                )
            }

            val amenities = parseAmenities(listing)

            val rating = listing.double("rating") ?: listing["reviews"]?.asObjectOrNull()?.double("rating")
            val reviewCount = listing.int("reviewCount", "reviewsCount", "review_count")
                ?: listing["reviews"]?.asObjectOrNull()?.int("count")

            val isFavorite = listing.boolean("liked", "isLiked", "isFavorited", "is_wishlisted")

            // Property details
            val propertyType = listing.string("propertyType", "property_type", "type", "spaceType", "space_type")
            val maxGuests = listing.int("maxGuests", "max_guests", "guests", "capacity")
            val bedrooms = listing.int("bedrooms", "bedroom_count")
            val beds = listing.int("beds", "bed_count")
            val bathrooms = listing.int("bathrooms", "bathroom_count")

            // House rules
            val houseRules = parseStringList(listing, "houseRules", "house_rules", "rules")
            val checkInTime = listing.string("checkInTime", "check_in_time", "checkIn")
            val checkOutTime = listing.string("checkOutTime", "check_out_time", "checkOut")

            // Safety features
            val safetyFeatures = parseStringList(listing, "safetyFeatures", "safety_features", "safety")

            // Status
            val available = listing.boolean("available", "is_available", "isAvailable")
            val instantBook = listing.boolean("instantBook", "instant_book", "instantBooking")

            // Cancellation policy
            val cancellationObj = listing["cancellationPolicy"]?.asObjectOrNull()
                ?: listing["cancellation_policy"]?.asObjectOrNull()
            val cancellationPolicy = cancellationObj?.let {
                CancellationPolicy(
                    type = it.string("type") ?: "flexible",
                    freeHoursBefore = it.int("freeHoursBefore", "free_hours_before") ?: 2,
                    description = it.string("description")
                )
            }

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
                isFavorite = isFavorite,
                cancellationPolicy = cancellationPolicy,
                category = listing.string("category"),
                propertyType = propertyType,
                maxGuests = maxGuests,
                bedrooms = bedrooms,
                beds = beds,
                bathrooms = bathrooms,
                houseRules = houseRules,
                checkInTime = checkInTime,
                checkOutTime = checkOutTime,
                safetyFeatures = safetyFeatures,
                available = available,
                instantBook = instantBook
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

            val cleaningFee = pricingObj?.double("cleaningFee", "cleaning_fee")
                ?: listing.double("cleaningFee", "cleaning_fee")
            val weeklyDiscount = pricingObj?.int("weeklyDiscount", "weekly_discount", "weeklyDiscountPercent")
                ?: listing.int("weeklyDiscount", "weekly_discount")
            val serviceFee = pricingObj?.double("serviceFee", "service_fee")
                ?: listing.double("serviceFee", "service_fee")
            val monthlyDiscount = pricingObj?.int("monthlyDiscount", "monthly_discount", "monthlyDiscountPercent")
                ?: listing.int("monthlyDiscount", "monthly_discount")

            ListingPricing(
                hourlyFrom = hourlyFrom,
                basePrice = basePrice,
                currency = currency,
                frequencyLabel = frequency,
                cleaningFee = cleaningFee,
                weeklyDiscountPercent = weeklyDiscount,
                serviceFee = serviceFee,
                monthlyDiscountPercent = monthlyDiscount
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

    private fun parseStringList(obj: JsonObject, vararg keys: String): List<String> {
        val raw = keys.firstNotNullOfOrNull { obj[it] } ?: return emptyList()
        val array = raw.asArrayOrNull() ?: return emptyList()
        return array.mapNotNull { el ->
            el.asStringOrNull()
                ?: el.asObjectOrNull()?.string("name", "title", "label", "text")
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

