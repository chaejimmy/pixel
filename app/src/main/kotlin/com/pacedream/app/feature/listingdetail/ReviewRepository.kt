package com.pacedream.app.feature.listingdetail

import com.pacedream.app.core.config.AppConfig
import com.pacedream.app.core.network.ApiClient
import com.pacedream.app.core.network.ApiError
import com.pacedream.app.core.network.ApiResult
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReviewRepository @Inject constructor(
    private val apiClient: ApiClient,
    private val appConfig: AppConfig,
    private val json: Json
) {
    /**
     * Fetch reviews for a listing.
     * GET /v1/reviews/property/{propertyId}
     */
    suspend fun fetchReviews(listingId: String): ApiResult<Pair<ReviewSummary, List<ReviewModel>>> {
        val url = appConfig.buildApiUrl("reviews", "property", listingId)
        return when (val result = apiClient.get(url, includeAuth = false)) {
            is ApiResult.Success -> {
                val parsed = parseReviewsResponse(result.data)
                if (parsed != null) ApiResult.Success(parsed)
                else ApiResult.Success(Pair(ReviewSummary(), emptyList()))
            }
            is ApiResult.Failure -> result
        }
    }

    /**
     * Submit a new review.
     * POST /v1/reviews
     */
    suspend fun createReview(request: CreateReviewRequest): ApiResult<ReviewModel> {
        val url = appConfig.buildApiUrl("reviews")
        val body = buildJsonObject {
            put("listingId", request.listingId)
            put("rating", request.rating)
            put("comment", request.comment)
            request.categoryRatings?.let { cats ->
                putJsonObject("categoryRatings") {
                    cats.cleanliness?.let { put("cleanliness", it) }
                    cats.accuracy?.let { put("accuracy", it) }
                    cats.communication?.let { put("communication", it) }
                    cats.location?.let { put("location", it) }
                    cats.checkIn?.let { put("checkIn", it) }
                    cats.value?.let { put("value", it) }
                }
            }
        }.toString()

        return when (val result = apiClient.post(url, body, includeAuth = true)) {
            is ApiResult.Success -> {
                val review = parseSingleReview(result.data)
                if (review != null) ApiResult.Success(review)
                else ApiResult.Failure(ApiError.DecodingError())
            }
            is ApiResult.Failure -> result
        }
    }

    private fun parseReviewsResponse(responseBody: String): Pair<ReviewSummary, List<ReviewModel>>? {
        return try {
            val root = json.parseToJsonElement(responseBody).jsonObject
            val data = root["data"]?.asObj() ?: root

            val reviewsArray = data["reviews"]?.asArr()
                ?: data["items"]?.asArr()
                ?: data["results"]?.asArr()
                ?: (root as? JsonArray)

            val reviews = reviewsArray?.mapNotNull { parseReviewElement(it) } ?: emptyList()

            val summaryObj = data["summary"]?.asObj() ?: data["stats"]?.asObj()
            val avgRating = summaryObj?.dbl("averageRating", "average", "rating")
                ?: data.dbl("averageRating", "rating")
                ?: reviews.takeIf { it.isNotEmpty() }?.map { it.rating }?.average()
                ?: 0.0
            val totalCount = summaryObj?.integer("totalCount", "total", "count")
                ?: data.integer("totalCount", "count")
                ?: reviews.size

            val catAvgObj = summaryObj?.get("categoryAverages")?.asObj()
                ?: summaryObj?.get("categories")?.asObj()
            val categoryAverages = catAvgObj?.let {
                CategoryRatings(
                    cleanliness = it.dbl("cleanliness"),
                    accuracy = it.dbl("accuracy"),
                    communication = it.dbl("communication"),
                    location = it.dbl("location"),
                    checkIn = it.dbl("checkIn", "check_in"),
                    value = it.dbl("value")
                )
            }

            val distribution = mutableMapOf<Int, Int>()
            summaryObj?.get("distribution")?.asObj()?.let { dist ->
                for (star in 1..5) {
                    dist.integer(star.toString())?.let { distribution[star] = it }
                }
            }

            val summary = ReviewSummary(
                averageRating = avgRating,
                totalCount = totalCount,
                categoryAverages = categoryAverages,
                ratingDistribution = distribution
            )

            Pair(summary, reviews)
        } catch (e: Exception) {
            Timber.e(e, "Failed to parse reviews response")
            null
        }
    }

    private fun parseReviewElement(element: JsonElement): ReviewModel? {
        val obj = element.asObj() ?: return null
        val id = obj.str("id", "_id") ?: return null
        val userObj = obj["user"]?.asObj() ?: obj["author"]?.asObj() ?: obj["reviewer"]?.asObj()
        val userName = userObj?.str("name")
            ?: listOfNotNull(
                userObj?.str("firstName", "first_name"),
                userObj?.str("lastName", "last_name")
            ).joinToString(" ").ifBlank { null }
            ?: obj.str("userName", "authorName")
            ?: "Guest"

        return ReviewModel(
            id = id,
            userId = userObj?.str("id", "_id") ?: obj.str("userId"),
            userName = userName,
            userAvatarUrl = userObj?.str("avatarUrl", "avatar", "profileImage"),
            rating = obj.dbl("rating") ?: 0.0,
            comment = obj.str("comment", "text", "review", "body") ?: "",
            createdAt = obj.str("createdAt", "created_at", "date")
        )
    }

    private fun parseSingleReview(responseBody: String): ReviewModel? {
        return try {
            val root = json.parseToJsonElement(responseBody).jsonObject
            val data = root["data"]?.asObj() ?: root["review"]?.asObj() ?: root
            parseReviewElement(data)
        } catch (e: Exception) {
            Timber.e(e, "Failed to parse single review")
            null
        }
    }
}

// Extension helpers
private fun JsonElement.asObj(): JsonObject? = this as? JsonObject
private fun JsonElement.asArr(): JsonArray? = this as? JsonArray

private fun JsonObject.str(vararg keys: String): String? =
    keys.firstNotNullOfOrNull { k ->
        this[k]?.let { runCatching { it.jsonPrimitive.content }.getOrNull() }?.takeIf { it.isNotBlank() }
    }

private fun JsonObject.dbl(vararg keys: String): Double? =
    keys.firstNotNullOfOrNull { k -> this[k]?.jsonPrimitive?.doubleOrNull }

private fun JsonObject.integer(vararg keys: String): Int? =
    keys.firstNotNullOfOrNull { k -> this[k]?.jsonPrimitive?.intOrNull }
