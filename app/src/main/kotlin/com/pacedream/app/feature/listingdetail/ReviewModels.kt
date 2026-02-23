package com.pacedream.app.feature.listingdetail

/**
 * Review data models for listing detail screen.
 * Matches the web platform's review/rating system.
 */
data class ReviewModel(
    val id: String,
    val userId: String? = null,
    val userName: String = "Guest",
    val userAvatarUrl: String? = null,
    val rating: Double = 0.0,
    val comment: String = "",
    val createdAt: String? = null,
    val categoryRatings: CategoryRatings? = null
)

data class CategoryRatings(
    val cleanliness: Double? = null,
    val accuracy: Double? = null,
    val communication: Double? = null,
    val location: Double? = null,
    val checkIn: Double? = null,
    val value: Double? = null
)

data class ReviewSummary(
    val averageRating: Double = 0.0,
    val totalCount: Int = 0,
    val categoryAverages: CategoryRatings? = null,
    val ratingDistribution: Map<Int, Int> = emptyMap() // star count -> number of reviews
)

data class CreateReviewRequest(
    val listingId: String,
    val rating: Double,
    val comment: String,
    val categoryRatings: CategoryRatings? = null
)
