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
        var lastError: ApiError = ApiError.NotFound

        for (url in urls) {
            when (val result = apiClient.get(url, includeAuth = true)) {
                is ApiResult.Success -> {
                    val parsed = parseListingDetail(result.data)
                    if (parsed != null) return ApiResult.Success(parsed)
                }
                is ApiResult.Failure -> {
                    Timber.d("Endpoint failed: $url — ${result.error.message}. Trying next…")
                    lastError = result.error
                }
            }
        }
        return ApiResult.Failure(lastError)
    }

    /**
     * Build detail URLs in priority order matching the website:
     * 1. /v1/poc/listings/{id} (primary for MongoDB ObjectIds, matches website)
     * 2. /v1/listings/{id} (unified endpoint)
     * 3. Type-specific endpoints as fallback
     */
    private fun buildDetailUrls(
        listingId: String,
        listingType: String
    ): List<okhttp3.HttpUrl> {
        // Primary: unified endpoints (matching website priority)
        val unified = listOf(
            appConfig.buildApiUrl("poc", "listings", listingId),
            appConfig.buildApiUrl("listings", listingId)
        )

        // Fallback: type-specific endpoints
        val typeSpecific = when (listingType) {
            "time-based" -> listOf(
                appConfig.buildApiUrl("properties", listingId),
                appConfig.buildApiUrl("properties", "get-rentable-item", listingId)
            )
            "gear" -> listOf(
                appConfig.buildApiUrl("gear-rentals", "hourly-rental-gear", listingId),
                appConfig.buildApiUrl("gear-rentals", listingId)
            )
            "split-stay" -> listOf(
                appConfig.buildApiUrl("roommate", listingId),
                appConfig.buildApiUrl("roommate", "get", listingId)
            )
            else -> listOf(
                appConfig.buildApiUrl("properties", "get-rentable-item", listingId)
            )
        }
        return unified + typeSpecific
    }

    /**
     * Tolerant JSON parsing – handles multiple response shapes.
     * Matches iOS TimeBasedListingsService.normalizeListing parity.
     */
    private fun parseListingDetail(responseBody: String): ListingDetailModel? {
        return try {
            val root = json.parseToJsonElement(responseBody)
            val obj = root.jsonObject

            // iOS parity: unwrap { status: true, data: {...} } and { data: {...} } wrappers
            val unwrapped = unwrapPayload(obj)
            val listing = unwrapped["listing"]?.asObjectOrNull()
                ?: unwrapped["item"]?.asObjectOrNull()
                ?: unwrapped["detail"]?.asObjectOrNull()
                ?: unwrapped["rentableItem"]?.asObjectOrNull()
                ?: unwrapped

            val id = listing.string("_id", "id") ?: return null
            val title = listing.string("title", "name", "listing_title", "headline") ?: "Listing"

            val description = listing.string("description", "summary", "about", "details", "roomType")

            val imageUrls = extractImages(listing)

            val locationObj = listing["location"]?.asObjectOrNull()
                ?: listing["address"]?.asObjectOrNull()
            val location = ListingLocation(
                city = listing.string("city") ?: locationObj?.string("city"),
                state = listing.string("state") ?: locationObj?.string("state", "region"),
                address = listing.string("address")
                    ?: locationObj?.string("address", "street_address", "streetAddress", "full"),
                latitude = locationObj?.double("latitude", "lat") ?: listing.double("latitude", "lat"),
                longitude = locationObj?.double("longitude", "lng", "lon") ?: listing.double("longitude", "lng", "lon"),
                country = locationObj?.string("country") ?: listing.string("country"),
                zipCode = locationObj?.string("zipCode", "zip_code", "postalCode", "postal_code") ?: listing.string("zipCode", "zip_code"),
                neighborhood = locationObj?.string("neighborhood", "neighbourhood", "area") ?: listing.string("neighborhood")
            )

            val pricing = parsePricing(listing)

            val host = extractHost(listing)

            val amenities = parseAmenities(listing)

            // Rating: check top-level, then nested ratings/review_summary objects (iOS parity)
            val ratingsObj = listing["ratings"]?.asObjectOrNull()
                ?: listing["review_summary"]?.asObjectOrNull()
                ?: listing["reviewSummary"]?.asObjectOrNull()
            val rating = listing.double("average_rating", "averageRating", "rating")
                ?: ratingsObj?.double("average", "averageRating")
            val reviewCount = listing.int("reviewCount", "reviews_count", "reviewsCount", "total_reviews", "totalReviews")
                ?: listing["reviews"]?.asArrayOrNull()?.size
                ?: ratingsObj?.int("count", "totalReviews")
                ?: detailsObj?.int("reviewCount", "reviews_count")

            val isFavorite = listing.boolean("liked", "isLiked", "isFavorited", "is_wishlisted")

            // Property details - also check nested details object (website parity)
            val detailsObj = listing["details"]?.asObjectOrNull()
            val propertyType = listing.string("propertyType", "property_type", "type", "spaceType", "space_type", "item_type", "listing_type")
            val maxGuests = listing.int("maxGuests", "max_guests", "guests", "capacity")
                ?: detailsObj?.int("seats", "capacity", "maxGuests")
            val bedrooms = listing.int("bedrooms", "bedroom_count")
                ?: detailsObj?.int("bedrooms", "bedroom_count")
            val beds = listing.int("beds", "bed_count")
                ?: detailsObj?.int("beds", "bed_count")
            val bathrooms = listing.int("bathrooms", "bathroom_count")
                ?: detailsObj?.int("bathrooms", "bathroom_count")

            // House rules
            val houseRules = parseStringList(listing, "houseRules", "house_rules", "rules")
            val checkInTime = listing.string("checkInTime", "check_in_time", "checkIn")
            val checkOutTime = listing.string("checkOutTime", "check_out_time", "checkOut")

            // Safety features
            val safetyFeatures = parseStringList(listing, "safetyFeatures", "safety_features", "safety")

            // Status - also check if status field equals "published" or "active" (website parity)
            val available = listing.boolean("available", "is_available", "isAvailable")
                ?: listing.string("status")?.let { it == "published" || it == "active" }
            val instantBook = listing.boolean("instantBook", "instant_book", "instantBooking")

            // Split listing fields (website parity)
            val shareType = listing.string("shareType", "share_type")
            val totalCost = listing.double("totalCost", "total_cost")
            val slotsTotal = listing.int("slotsTotal", "slots_total")
            val slotsFilled = listing.int("slotsFilled", "slots_filled")
            val splitStatus = listing.string("splitStatus", "split_status")
            val deadlineAt = listing.string("deadlineAt", "deadline_at")

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
                instantBook = instantBook,
                shareType = shareType,
                totalCost = totalCost,
                slotsTotal = slotsTotal,
                slotsFilled = slotsFilled,
                splitStatus = splitStatus,
                deadlineAt = deadlineAt
            )
        } catch (e: Exception) {
            Timber.e(e, "Failed to parse listing detail")
            null
        }
    }

    /**
     * iOS parity: unwrap common backend wrapper patterns.
     * { status: true, data: {...} } → data
     * { success: true, data: {...} } → data
     * { data: {...} } → data
     */
    private fun unwrapPayload(obj: JsonObject): JsonObject {
        val hasStatusTrue = obj.boolean("status") == true
        val hasSuccessTrue = obj.boolean("success") == true
        val data = obj["data"]?.asObjectOrNull()
        if (data != null && (hasStatusTrue || hasSuccessTrue || obj.containsKey("data"))) {
            return data
        }
        return obj
    }

    /**
     * iOS parity: extract images from gallery, images, photos, cover, image, imageUrl
     */
    private fun extractImages(listing: JsonObject): List<String> {
        return buildList {
            // Gallery object (iOS: gallery.thumbnail + gallery.images)
            listing["gallery"]?.asObjectOrNull()?.let { gallery ->
                gallery.string("thumbnail")?.let { add(it) }
                gallery["images"]?.asArrayOrNull()?.forEach { el ->
                    el.asStringOrNull()?.let { add(it) }
                    el.asObjectOrNull()?.string("url", "src")?.let { add(it) }
                }
            }

            // Direct images array
            listing["images"]?.asArrayOrNull()?.forEach { el ->
                el.asStringOrNull()?.let { add(it) }
                el.asObjectOrNull()?.string("url", "src")?.let { add(it) }
            }

            // Photos array (iOS parity)
            listing["photos"]?.asArrayOrNull()?.forEach { el ->
                el.asStringOrNull()?.let { add(it) }
                el.asObjectOrNull()?.string("url", "src")?.let { add(it) }
            }

            // Single image keys
            listing.string("cover")?.let { add(it) }
            listing.string("image")?.let { add(it) }
            listing.string("imageUrl")?.let { add(it) }
            listing.string("coverImage")?.let { add(it) }
        }.map { it.trim() }.filter { it.isNotBlank() }.distinct()
    }

    /**
     * iOS parity: extract host from owner/host/hostInfo/host_details/user/host_id
     * with nested userId object support.
     */
    private fun extractHost(listing: JsonObject): ListingHost? {
        val owner = listing["owner"]?.asObjectOrNull()
            ?: listing["host"]?.asObjectOrNull()
            ?: listing["hostInfo"]?.asObjectOrNull()
            ?: listing["host_details"]?.asObjectOrNull()
            ?: listing["user"]?.asObjectOrNull()
            ?: listing["host_id"]?.asObjectOrNull()
            ?: return null

        // iOS parity: host_id can be { _id, userId: {...} }
        val hostUser = owner["userId"]?.asObjectOrNull()
            ?: owner["user"]?.asObjectOrNull()
            ?: owner

        val id = hostUser.string("_id", "id", "userId", "user_id")
            ?: owner.string("userId", "user_id")
            ?: owner.string("_id", "id")
            ?: listing.string("host_id", "userId", "user_id")

        val firstName = hostUser.string("first_name", "firstName")
            ?: owner.string("first_name", "firstName")
        val lastName = hostUser.string("last_name", "lastName")
            ?: owner.string("last_name", "lastName")
        val combined = listOfNotNull(firstName, lastName)
            .map { it.trim() }.filter { it.isNotBlank() }.joinToString(" ")

        val name = hostUser.string("name", "fullName")
            ?: combined.ifBlank { null }
            ?: hostUser.string("username")
            ?: owner.string("name", "fullName", "firstName")
            ?: listing.string("hostName", "host_name")

        // iOS parity: profilePic can be a string or array
        val avatar = hostUser.string("avatar", "profilePic", "profile_pic", "avatarUrl")
            ?: hostUser["profilePic"]?.asArrayOrNull()?.firstOrNull()?.asStringOrNull()
            ?: owner.string("avatar", "profilePic", "profile_pic", "avatarUrl")
            ?: owner["profilePic"]?.asArrayOrNull()?.firstOrNull()?.asStringOrNull()
            ?: listing.string("hostAvatar", "host_avatar")

        val verifications = buildList {
            owner["verifications"]?.asArrayOrNull()?.forEach { v ->
                v.asStringOrNull()?.let { s -> add(s) }
                v.asObjectOrNull()?.string("type", "method")?.let { s -> add(s) }
            }
            if (owner.boolean("emailVerified", "email_verified") == true) add("email")
            if (owner.boolean("phoneVerified", "phone_verified") == true) add("phone")
            if (owner.boolean("identityVerified", "identity_verified") == true) add("identity")
        }.distinct()

        return ListingHost(
            id = id,
            name = name,
            avatarUrl = avatar,
            bio = owner.string("bio", "about", "description"),
            isSuperhost = owner.boolean("isSuperhost", "is_superhost", "superhost"),
            isVerified = owner.boolean("isVerified", "is_verified", "verified"),
            responseRate = owner.int("responseRate", "response_rate"),
            responseTime = owner.string("responseTime", "response_time"),
            listingCount = owner.int("listingCount", "listing_count", "listingsCount"),
            joinedDate = owner.string("joinedDate", "joined_date", "createdAt", "created_at"),
            verifications = verifications
        )
    }

    private fun parsePricing(listing: JsonObject): ListingPricing? {
        return try {
            val pricingObj = listing["pricing"]?.asObjectOrNull() ?: listing["price"]?.asObjectOrNull()

            // Website parity: handle price as array of pricing objects
            // e.g. price: [{ pricing_type: "hourly", amount: 25, currency: "USD", frequency: "HOUR" }]
            val priceArray = listing["price"]?.asArrayOrNull()
            val firstPrice = priceArray?.firstOrNull()?.asObjectOrNull()

            val currency = pricingObj?.string("currency")
                ?: firstPrice?.string("currency")
                ?: listing.string("currency")

            val hourlyFrom =
                pricingObj?.double("hourlyFrom", "hourly_from")
                    ?: listing.double("hourlyFrom", "hourly_from")
                    ?: listing["dynamic_price"]?.asArrayOrNull()?.firstOrNull()?.asObjectOrNull()?.double("price")
                    // Website: price array with pricing_type "hourly" or frequency "HOUR"
                    ?: priceArray?.mapNotNull { it.asObjectOrNull() }
                        ?.firstOrNull { el ->
                            el.string("pricing_type")?.lowercase() in listOf("hourly", "hour")
                                    || el.string("frequency")?.uppercase() == "HOUR"
                        }?.double("amount")

            val basePrice =
                pricingObj?.double("base_price", "basePrice", "amount")
                    ?: listing.double("base_price", "basePrice")
                    // Website: first price entry amount as base price
                    ?: firstPrice?.double("amount")

            val frequency =
                pricingObj?.string("frequency", "unit", "frequencyLabel", "pricing_type")
                    ?: firstPrice?.string("frequency", "pricing_type")
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
        // Website parity: amenities can be at top-level, or under details.features / details.amenities
        val detailsObj = listing["details"]?.asObjectOrNull()
        val raw = listing["amenities"]
            ?: listing["highlights"]
            ?: detailsObj?.get("features")
            ?: detailsObj?.get("amenities")
            ?: return emptyList()
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

