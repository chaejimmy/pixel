package com.pacedream.app.feature.hostprofile

/**
 * Trust-focused host profile model.
 *
 * Optional/trust fields (rating, reviewCount, completedBookings, verifiedBadges,
 * responseTime, joinedAt) are intentionally nullable — the UI must only render
 * them when the backend supplies real values, never with placeholders.
 */
data class HostProfileModel(
    val id: String,
    val name: String? = null,
    val avatarUrl: String? = null,
    val location: String? = null,
    val bio: String? = null,
    val listings: List<HostListingSummary> = emptyList(),
    // Optional trust fields — render only when present.
    val rating: Double? = null,
    val reviewCount: Int? = null,
    val completedBookings: Int? = null,
    val verifiedBadges: List<String> = emptyList(),
    val responseTime: String? = null,
    val joinedAt: String? = null,
)

/**
 * Lightweight listing card model for the host profile listings section.
 * Mirrors the fields needed to navigate to the listing detail screen.
 */
data class HostListingSummary(
    val id: String,
    val title: String,
    val imageUrl: String? = null,
    val location: String? = null,
    val priceLabel: String? = null,
    val type: String = "",
)
