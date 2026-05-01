package com.pacedream.app.feature.hostprofile

/**
 * Trust-focused host profile model.
 *
 * Optional/trust fields (rating, reviewCount, completedBookings,
 * verifiedBadges, responseTime, joinedAt, superHost) are intentionally
 * nullable / empty by default — the UI must only render them when the
 * backend supplies real values, never with placeholders.
 */
data class HostProfileModel(
    /** Host._id (may be null when the id resolves only to a User). */
    val hostId: String? = null,
    /** Underlying User._id — preferred for messaging. */
    val userId: String? = null,
    val name: String? = null,
    val firstName: String? = null,
    val lastName: String? = null,
    val avatarUrl: String? = null,
    val location: HostLocation? = null,
    val bio: String? = null,
    val listings: List<HostListingSummary> = emptyList(),
    val verifiedBadges: List<String> = emptyList(),
    val rating: Double? = null,
    val reviewCount: Int? = null,
    val completedBookings: Int? = null,
    val responseTime: String? = null,
    val joinedAt: String? = null,
    val superHost: Boolean? = null,
)

data class HostLocation(
    val city: String? = null,
    val state: String? = null,
    val country: String? = null,
) {
    val display: String?
        get() = listOfNotNull(
            city?.takeIf { it.isNotBlank() },
            state?.takeIf { it.isNotBlank() },
            country?.takeIf { it.isNotBlank() },
        ).joinToString(", ").takeIf { it.isNotBlank() }
}

/**
 * Lightweight listing card model rendered on the host profile.
 * Carries enough metadata to route to the matching listing detail
 * destination (category drives the existing route mapping).
 */
data class HostListingSummary(
    val id: String,
    val title: String,
    val imageUrl: String? = null,
    val location: String? = null,
    val priceLabel: String? = null,
    val rating: Double? = null,
    /** "room", "gear", "service", "split-stay", etc. */
    val category: String? = null,
    /** "time-based" | "gear" | "split-stay" — matches ListingDetailRoute. */
    val listingType: String = "",
)
