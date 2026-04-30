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
 * Best-effort host profile fetcher.
 *
 * The backend does not yet expose a unified `GET /v1/hosts/:id` endpoint, so
 * the repository attempts a small set of plausible candidates and returns the
 * first usable response. When no candidate succeeds, callers fall back to seed
 * data already available on the listing detail (avatar, name, etc.).
 *
 * No fake fields are synthesized — trust signals (rating, review count,
 * verified badges, response time, joined date) are only populated when the
 * backend supplies real values.
 */
@Singleton
class HostProfileRepository @Inject constructor(
    private val apiClient: ApiClient,
    private val appConfig: AppConfig,
    private val json: Json,
) {
    suspend fun fetchHostProfile(hostId: String): ApiResult<HostProfileModel> {
        val candidates = listOf(
            // Preferred future shape — matches the documented MVP API.
            appConfig.buildApiUrl("hosts", hostId),
            // Common alternates seen in other modules (`user/get/profile` is the
            // current-user variant; these are best-effort guesses for parity).
            appConfig.buildApiUrl("users", hostId),
            appConfig.buildApiUrl("user", hostId),
            appConfig.buildApiUrl("user", "profile", hostId),
        )

        var lastError: ApiError = ApiError.NotFound
        for (url in candidates) {
            when (val result = apiClient.get(url, includeAuth = true)) {
                is ApiResult.Success -> {
                    val parsed = parseHostProfile(hostId, result.data)
                    if (parsed != null) return ApiResult.Success(parsed)
                    Timber.d("Host profile parse failed for url=$url; trying next candidate")
                }
                is ApiResult.Failure -> {
                    Timber.d("Host profile endpoint failed: $url — ${result.error.message}")
                    lastError = result.error
                }
            }
        }
        return ApiResult.Failure(lastError)
    }

    /**
     * Fetch listings owned by a host. Returns an empty list (not failure) when
     * no endpoint is available so the screen can render its empty state cleanly.
     */
    suspend fun fetchHostListings(hostId: String): List<HostListingSummary> {
        val candidates = listOf(
            // Search-style queries that several backends support.
            appConfig.buildApiUrl("listings", queryParams = mapOf("owner" to hostId)),
            appConfig.buildApiUrl("listings", queryParams = mapOf("hostId" to hostId)),
            appConfig.buildApiUrl("poc", "listings", queryParams = mapOf("owner" to hostId)),
            // Per-resource variants — properties is the time-based listing collection.
            appConfig.buildApiUrl("properties", queryParams = mapOf("owner" to hostId)),
        )

        for (url in candidates) {
            val result = apiClient.get(url, includeAuth = true)
            if (result is ApiResult.Success) {
                val parsed = parseListings(result.data)
                if (parsed.isNotEmpty()) return parsed
            } else if (result is ApiResult.Failure) {
                Timber.d("Host listings endpoint failed: $url — ${result.error.message}")
            }
        }
        return emptyList()
    }

    private fun parseHostProfile(hostId: String, body: String): HostProfileModel? {
        return try {
            val root = json.parseToJsonElement(body).jsonObject
            val obj = unwrapPayload(root)
            // Some backends nest the user under userId/user.
            val user = obj["userId"]?.asObjectOrNull()
                ?: obj["user"]?.asObjectOrNull()
                ?: obj

            val firstName = user.string("first_name", "firstName")
            val lastName = user.string("last_name", "lastName")
            val combinedName = listOfNotNull(firstName, lastName)
                .joinToString(" ").trim().takeIf { it.isNotBlank() }
            val name = user.string("name", "fullName")
                ?: combinedName
                ?: user.string("username")
                ?: obj.string("name", "fullName")

            val avatar = user.string("avatar", "avatarUrl", "profilePic", "profile_pic")
                ?: user["profilePic"]?.asArrayOrNull()?.firstOrNull()?.asStringOrNull()
                ?: obj.string("avatar", "avatarUrl", "profilePic", "profile_pic")

            val locationObj = user["location"]?.asObjectOrNull()
                ?: obj["location"]?.asObjectOrNull()
            val location = locationObj?.let {
                listOfNotNull(
                    it.string("city"),
                    it.string("state", "region"),
                    it.string("country"),
                ).joinToString(", ").takeIf { s -> s.isNotBlank() }
            } ?: user.string("city")

            val verificationState = user["verificationState"]?.asObjectOrNull()
                ?: obj["verificationState"]?.asObjectOrNull()
            val verifiedBadges = buildList {
                (user["verifications"]?.asArrayOrNull() ?: obj["verifications"]?.asArrayOrNull())
                    ?.forEach { el ->
                        el.asStringOrNull()?.let { add(it) }
                            ?: el.asObjectOrNull()?.string("type", "method")?.let { add(it) }
                    }
                if (verificationState?.boolean("verified") == true) {
                    if (none { it.equals("identity", ignoreCase = true) }) add("Identity")
                }
            }.distinct()

            HostProfileModel(
                id = user.string("_id", "id") ?: obj.string("_id", "id") ?: hostId,
                name = name,
                avatarUrl = avatar,
                location = location,
                bio = user.string("bio", "about", "description")
                    ?: obj.string("bio", "about", "description"),
                listings = emptyList(), // populated by fetchHostListings
                rating = obj.double("rating", "averageRating")
                    ?: user.double("rating", "averageRating"),
                reviewCount = obj.int("reviewCount", "reviewsCount", "numReviews")
                    ?: user.int("reviewCount", "reviewsCount", "numReviews"),
                completedBookings = obj.int("completedBookings", "completed_bookings")
                    ?: user.int("completedBookings", "completed_bookings"),
                verifiedBadges = verifiedBadges,
                responseTime = obj.string("responseTime", "response_time")
                    ?: user.string("responseTime", "response_time"),
                joinedAt = obj.string("joinedAt", "joined_at", "createdAt", "created_at")
                    ?: user.string("joinedAt", "joined_at", "createdAt", "created_at"),
            )
        } catch (e: Exception) {
            Timber.d(e, "Failed to parse host profile body")
            null
        }
    }

    private fun parseListings(body: String): List<HostListingSummary> {
        return try {
            val root = json.parseToJsonElement(body)
            val array = when {
                root is JsonArray -> root
                root is JsonObject -> {
                    val obj = unwrapPayload(root)
                    obj["listings"]?.asArrayOrNull()
                        ?: obj["items"]?.asArrayOrNull()
                        ?: obj["data"]?.asArrayOrNull()
                        ?: obj["results"]?.asArrayOrNull()
                        ?: return emptyList()
                }
                else -> return emptyList()
            }
            array.mapNotNull { el ->
                val o = el.asObjectOrNull() ?: return@mapNotNull null
                val id = o.string("_id", "id", "listingId") ?: return@mapNotNull null
                val title = o.string("title", "name") ?: return@mapNotNull null
                val image = extractFirstImage(o)
                val location = o["location"]?.asObjectOrNull()?.let { loc ->
                    listOfNotNull(loc.string("city"), loc.string("state", "region"))
                        .joinToString(", ").takeIf { it.isNotBlank() }
                }
                HostListingSummary(
                    id = id,
                    title = title,
                    imageUrl = image,
                    location = location,
                    priceLabel = null,
                    type = o.string("type", "category", "listingType") ?: "",
                )
            }
        } catch (e: Exception) {
            Timber.d(e, "Failed to parse host listings body")
            emptyList()
        }
    }

    private fun extractFirstImage(o: JsonObject): String? {
        o.string("imageUrl", "image", "thumbnail")?.let { return it }
        val images = o["images"]?.asArrayOrNull()
            ?: o["photos"]?.asArrayOrNull()
            ?: o["gallery"]?.asArrayOrNull()
        images?.forEach { el ->
            el.asStringOrNull()?.let { return it }
            el.asObjectOrNull()?.string("url", "src", "secure_url")?.let { return it }
        }
        return null
    }

    private fun unwrapPayload(obj: JsonObject): JsonObject {
        val data = obj["data"]?.asObjectOrNull() ?: return obj
        return data
    }
}

private fun JsonElement.asObjectOrNull(): JsonObject? = this as? JsonObject
private fun JsonElement.asArrayOrNull(): JsonArray? = this as? JsonArray
private fun JsonElement.asStringOrNull(): String? =
    runCatching { this.jsonPrimitive.content.takeIf { it.isNotBlank() } }.getOrNull()

private fun JsonObject.string(vararg keys: String): String? =
    keys.firstNotNullOfOrNull { k -> this[k]?.asStringOrNull() }

private fun JsonObject.double(vararg keys: String): Double? =
    keys.firstNotNullOfOrNull { k -> this[k]?.jsonPrimitive?.doubleOrNull }

private fun JsonObject.int(vararg keys: String): Int? =
    keys.firstNotNullOfOrNull { k -> this[k]?.jsonPrimitive?.intOrNull }

private fun JsonObject.boolean(vararg keys: String): Boolean? =
    keys.firstNotNullOfOrNull { k -> this[k]?.jsonPrimitive?.booleanOrNull }
