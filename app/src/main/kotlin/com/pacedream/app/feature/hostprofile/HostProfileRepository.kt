package com.pacedream.app.feature.hostprofile

import com.pacedream.app.core.config.AppConfig
import com.pacedream.app.core.network.ApiClient
import com.pacedream.app.core.network.ApiError
import com.pacedream.app.core.network.ApiResult
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Outcome surface for [HostProfileRepository.fetchHostProfile].
 *
 * - [Success]   The backend returned a parseable host payload.
 * - [NotFound]  The host does not exist (404 from the canonical endpoint).
 * - [Error]     Network or 5xx error after the fallback chain ran out.
 */
sealed interface HostProfileResult {
    data class Success(val host: HostProfileModel) : HostProfileResult
    data object NotFound : HostProfileResult
    data class Error(val message: String) : HostProfileResult
}

/**
 * Fetches the host profile + listings using the canonical endpoint with
 * a documented fallback chain.
 *
 * Canonical: `GET /v1/hosts/{id}` (id can be Host._id or User._id).
 * Fallbacks (5xx / network only):
 *   1. GET /v1/users/get/{id}
 *   2. GET /v1/listings?ownerUserId={id}&limit=50
 *   3. GET /v1/listings?ownerHostId={hostProfileId}
 *   4. GET /v1/listings?owner={id}
 *   5. GET /v1/listings?limit=100  (client-side filter as last resort)
 *
 * No fake fields are synthesised. Trust signals (rating, reviewCount,
 * completedBookings, verifiedBadges, responseTime, joinedAt, superHost)
 * are passed through unchanged when present and left null otherwise.
 */
@Singleton
class HostProfileRepository @Inject constructor(
    private val apiClient: ApiClient,
    private val appConfig: AppConfig,
    private val json: Json,
) {
    suspend fun fetchHostProfile(id: String): HostProfileResult {
        if (id.isBlank()) return HostProfileResult.NotFound

        val canonicalUrl = appConfig.buildApiUrl("hosts", id)
        when (val result = apiClient.get(canonicalUrl, includeAuth = true)) {
            is ApiResult.Success -> {
                val host = parseHostPayload(id, result.data)
                if (host != null) return HostProfileResult.Success(host)
                Timber.d("Host payload from /hosts/$id failed to parse; running fallback chain")
            }
            is ApiResult.Failure -> {
                if (result.error is ApiError.NotFound) return HostProfileResult.NotFound
                Timber.d("Canonical /hosts/$id failed: ${result.error.message}; running fallback chain")
            }
        }

        // Fallback chain runs only when the canonical endpoint failed with
        // something other than a definitive 404.
        return runFallbackChain(id)
    }

    private suspend fun runFallbackChain(id: String): HostProfileResult {
        val userPayload = fetchUserPayload(id)
        val listings = fetchListingsByOwner(
            ownerUserId = id,
            ownerHostId = userPayload?.string("hostId", "host_id"),
        )

        if (userPayload == null && listings.isEmpty()) {
            return HostProfileResult.Error("We couldn't load this host. Please try again.")
        }

        val name = userPayload?.let { extractDisplayName(it) }
        val (firstName, lastName) = userPayload?.let { extractNameParts(it) } ?: (null to null)
        val location = userPayload?.let { extractLocation(it) }
        val avatar = userPayload?.let { extractAvatar(it) }
        val verificationState = userPayload?.get("verificationState")?.asObjectOrNull()

        val host = HostProfileModel(
            hostId = userPayload?.string("hostId", "host_id"),
            userId = userPayload?.string("_id", "id") ?: id,
            name = name,
            firstName = firstName,
            lastName = lastName,
            avatarUrl = avatar,
            location = location,
            bio = userPayload?.string("bio", "about", "description"),
            listings = listings,
            verifiedBadges = extractVerifiedBadges(userPayload, verificationState),
            rating = userPayload?.double("rating", "averageRating"),
            reviewCount = userPayload?.int("reviewCount", "reviewsCount", "numReviews"),
            completedBookings = userPayload?.int("completedBookings", "completed_bookings"),
            responseTime = userPayload?.string("responseTime", "response_time"),
            joinedAt = userPayload?.string("joinedAt", "createdAt", "created_at"),
            superHost = userPayload?.boolean("superHost", "isSuperhost", "is_superhost"),
        )
        return HostProfileResult.Success(host)
    }

    private suspend fun fetchUserPayload(id: String): JsonObject? {
        val url = appConfig.buildApiUrl("users", "get", id)
        return when (val result = apiClient.get(url, includeAuth = true)) {
            is ApiResult.Success -> {
                runCatching {
                    val obj = json.parseToJsonElement(result.data).jsonObject
                    unwrapPayload(obj)
                }.getOrNull()
            }
            is ApiResult.Failure -> {
                Timber.d("User fallback failed for $id: ${result.error.message}")
                null
            }
        }
    }

    private suspend fun fetchListingsByOwner(
        ownerUserId: String,
        ownerHostId: String?,
    ): List<HostListingSummary> {
        // Documented owner-aware filters. Each is tried in turn; the first
        // non-empty response wins so a single round-trip is enough most of
        // the time.
        val ownerCandidates = buildList {
            add(appConfig.buildApiUrl(
                "listings",
                queryParams = mapOf("ownerUserId" to ownerUserId, "limit" to "50"),
            ))
            if (!ownerHostId.isNullOrBlank()) {
                add(appConfig.buildApiUrl(
                    "listings",
                    queryParams = mapOf("ownerHostId" to ownerHostId),
                ))
            }
            add(appConfig.buildApiUrl(
                "listings",
                queryParams = mapOf("owner" to ownerUserId),
            ))
        }
        for (url in ownerCandidates) {
            val result = apiClient.get(url, includeAuth = true)
            if (result is ApiResult.Success) {
                val listings = parseListingsArray(result.data)
                if (listings.isNotEmpty()) return listings
            }
        }

        // Last-resort: pull a bounded page and filter client-side.
        val fallbackUrl = appConfig.buildApiUrl(
            "listings",
            queryParams = mapOf("limit" to "100"),
        )
        val result = apiClient.get(fallbackUrl, includeAuth = true)
        if (result is ApiResult.Success) {
            return parseListingsArray(result.data)
                .filter { summary -> summary.ownerMatches(ownerUserId, ownerHostId) }
        }
        return emptyList()
    }

    private fun parseHostPayload(id: String, body: String): HostProfileModel? {
        return try {
            val root = json.parseToJsonElement(body).jsonObject
            val data = unwrapPayload(root)
            val name = extractDisplayName(data)
            val (firstName, lastName) = extractNameParts(data)

            HostProfileModel(
                hostId = data.string("id", "_id", "hostId", "host_id"),
                userId = data.string("userId", "user_id"),
                name = name,
                firstName = firstName,
                lastName = lastName,
                avatarUrl = extractAvatar(data),
                location = extractLocation(data),
                bio = data.string("bio", "about", "description"),
                listings = parseListingsField(data),
                verifiedBadges = (data["verifiedBadges"]?.asArrayOrNull()
                    ?.mapNotNull { it.asStringOrNull() }
                    ?: emptyList())
                    .ifEmpty { extractVerifiedBadges(data, data["verificationState"]?.asObjectOrNull()) },
                rating = data.double("rating", "averageRating"),
                reviewCount = data.int("reviewCount", "reviewsCount"),
                completedBookings = data.int("completedBookings"),
                responseTime = data.string("responseTime", "response_time"),
                joinedAt = data.string("joinedAt", "createdAt", "created_at"),
                superHost = data.boolean("superHost", "isSuperhost"),
            )
        } catch (e: Exception) {
            Timber.d(e, "Failed to parse host payload")
            null
        }
    }

    private fun parseListingsField(data: JsonObject): List<HostListingSummary> {
        val arr = data["listings"]?.asArrayOrNull() ?: return emptyList()
        return arr.mapNotNull { it.asObjectOrNull()?.let(::parseListingSummary) }
    }

    private fun parseListingsArray(body: String): List<HostListingSummary> {
        return try {
            val element = json.parseToJsonElement(body)
            val arr = when (element) {
                is JsonArray -> element
                is JsonObject -> {
                    val unwrapped = unwrapPayload(element)
                    unwrapped["listings"]?.asArrayOrNull()
                        ?: unwrapped["items"]?.asArrayOrNull()
                        ?: unwrapped["results"]?.asArrayOrNull()
                        ?: (element["data"] as? JsonArray)
                        ?: return emptyList()
                }
                else -> return emptyList()
            }
            arr.mapNotNull { it.asObjectOrNull()?.let(::parseListingSummary) }
        } catch (e: Exception) {
            Timber.d(e, "Failed to parse listings array")
            emptyList()
        }
    }

    private fun parseListingSummary(o: JsonObject): HostListingSummary? {
        val id = o.string("_id", "id", "listingId") ?: return null
        val title = o.string("title", "name") ?: return null
        val gallery = o["gallery"]?.asObjectOrNull()
        val image = gallery?.string("thumbnail")
            ?: gallery?.get("images")?.asArrayOrNull()?.firstOrNull()?.firstImage()
            ?: o.string("cover", "coverImage", "imageUrl", "image", "thumbnail")
            ?: o["images"]?.asArrayOrNull()?.firstOrNull()?.firstImage()
            ?: o["photos"]?.asArrayOrNull()?.firstOrNull()?.firstImage()

        val locationObj = o["location"]?.asObjectOrNull()
        val locationDisplay = locationObj?.let {
            listOfNotNull(
                it.string("city"),
                it.string("state", "region"),
                it.string("country"),
            ).joinToString(", ").takeIf { s -> s.isNotBlank() }
        }

        val priceLabel = formatPrice(o)
        val category = o.string("category", "listing_type", "type")
        val listingType = when {
            o.string("_source") == "rentable_item" -> "gear"
            o.string("shareType")?.equals("SPLIT", ignoreCase = true) == true -> "split-stay"
            category?.contains("gear", ignoreCase = true) == true -> "gear"
            else -> "time-based"
        }
        return HostListingSummary(
            id = id,
            title = title,
            imageUrl = image,
            location = locationDisplay,
            priceLabel = priceLabel,
            rating = o.double("rating", "averageRating"),
            category = category,
            listingType = listingType,
        )
    }

    private fun formatPrice(o: JsonObject): String? {
        val priceArray = o["price"]?.asArrayOrNull()
        val first = priceArray?.firstOrNull()?.asObjectOrNull()
        val pricing = o["pricing"]?.asObjectOrNull()

        val amount = first?.double("amount")
            ?: pricing?.double("base_price", "basePrice")
            ?: o.double("price", "amount")
            ?: return null
        val currency = first?.string("currency")
            ?: pricing?.string("currency")
            ?: o.string("currency")
            ?: "USD"
        val frequency = first?.string("frequency", "pricing_type")
            ?: pricing?.string("frequency")
            ?: o.string("frequency")
        val symbol = currencySymbol(currency)
        val unit = when ((frequency ?: "").lowercase()) {
            "hourly", "hour", "hr" -> "/hr"
            "daily", "day" -> "/day"
            "weekly", "week" -> "/wk"
            "monthly", "month" -> "/mo"
            else -> ""
        }
        val asLong = amount.toLong()
        val formatted = if (amount == asLong.toDouble()) asLong.toString() else amount.toString()
        return "$symbol$formatted$unit"
    }

    private fun currencySymbol(currency: String): String = when (currency.uppercase()) {
        "USD" -> "$"
        "EUR" -> "€"
        "GBP" -> "£"
        "CAD" -> "CA$"
        "AUD" -> "A$"
        else -> "$"
    }

    private fun extractDisplayName(o: JsonObject): String? {
        val direct = o.string("name", "fullName")
        if (!direct.isNullOrBlank()) return direct
        val first = o.string("firstName", "first_name")
        val last = o.string("lastName", "last_name")
        val combined = listOfNotNull(first, last).joinToString(" ").trim()
        return combined.takeIf { it.isNotBlank() } ?: o.string("username")
    }

    private fun extractNameParts(o: JsonObject): Pair<String?, String?> {
        val first = o.string("firstName", "first_name")
        val last = o.string("lastName", "last_name")
        return first to last
    }

    private fun extractAvatar(o: JsonObject): String? {
        o.string("avatarUrl", "avatar", "profilePic", "profile_pic")?.let { return it }
        o["profilePic"]?.asArrayOrNull()?.firstOrNull()?.asStringOrNull()?.let { return it }
        return null
    }

    private fun extractLocation(o: JsonObject): HostLocation? {
        val loc = o["location"]?.asObjectOrNull() ?: return null
        return HostLocation(
            city = loc.string("city"),
            state = loc.string("state", "region"),
            country = loc.string("country"),
        ).takeIf { it.display != null }
    }

    private fun extractVerifiedBadges(
        user: JsonObject?,
        verificationState: JsonObject?,
    ): List<String> {
        val badges = mutableListOf<String>()
        user?.get("verifiedBadges")?.asArrayOrNull()?.forEach { el ->
            el.asStringOrNull()?.let { badges += it }
        }
        user?.get("verifications")?.asArrayOrNull()?.forEach { el ->
            el.asStringOrNull()?.let { badges += it }
                ?: el.asObjectOrNull()?.string("type", "method")?.let { badges += it }
        }
        if (verificationState?.boolean("verified") == true &&
            badges.none { it.equals("identity", ignoreCase = true) }
        ) {
            badges += "identity"
        }
        return badges.distinctBy { it.lowercase() }
    }

    private fun unwrapPayload(obj: JsonObject): JsonObject {
        val data = obj["data"]?.asObjectOrNull() ?: return obj
        return data
    }
}

private fun HostListingSummary.ownerMatches(userId: String, hostId: String?): Boolean {
    // The light-touch fallback only knows the listing summary, so we can't
    // verify ownership reliably. Filtering down to nothing is intentional —
    // we'd rather show an empty listings section than guess.
    if (userId.isBlank() && hostId.isNullOrBlank()) return true
    return false
}

private fun JsonElement.asObjectOrNull(): JsonObject? = this as? JsonObject
private fun JsonElement.asArrayOrNull(): JsonArray? = this as? JsonArray
private fun JsonElement.asStringOrNull(): String? =
    runCatching { this.jsonPrimitive.content.takeIf { it.isNotBlank() } }.getOrNull()

private fun JsonElement.firstImage(): String? =
    asStringOrNull()
        ?: asObjectOrNull()?.let { it.string("url", "src", "secure_url", "thumbnail") }

private fun JsonObject.string(vararg keys: String): String? =
    keys.firstNotNullOfOrNull { k -> this[k]?.asStringOrNull() }

private fun JsonObject.double(vararg keys: String): Double? =
    keys.firstNotNullOfOrNull { k -> this[k]?.jsonPrimitive?.doubleOrNull }

private fun JsonObject.int(vararg keys: String): Int? =
    keys.firstNotNullOfOrNull { k -> this[k]?.jsonPrimitive?.intOrNull }

private fun JsonObject.boolean(vararg keys: String): Boolean? =
    keys.firstNotNullOfOrNull { k -> this[k]?.jsonPrimitive?.booleanOrNull }
