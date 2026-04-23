package com.shourov.apps.pacedream.model

import com.google.gson.annotations.SerializedName

/**
 * Pricing unit matching the backend enum: 'hour' | 'day' | 'week' | 'month'.
 * Maps directly to the web platform's pricing_type / pricing.unit fields.
 */
enum class PricingUnit(
    val value: String,
    val displayLabel: String,
    val shortLabel: String,
    /** Backend pricing_type value (e.g. "hourly", "daily") – iOS parity. */
    val backendPricingType: String,
    /** Backend frequency value (e.g. "HOUR", "DAY") – iOS parity. */
    val backendFrequency: String,
) {
    HOUR("hour", "Hourly", "hr", "hourly", "HOUR"),
    DAY("day", "Daily", "day", "daily", "DAY"),
    WEEK("week", "Weekly", "wk", "weekly", "WEEK"),
    MONTH("month", "Monthly", "mo", "monthly", "MONTH");

    companion object {
        fun fromValue(value: String): PricingUnit =
            entries.firstOrNull { it.value == value.lowercase() } ?: HOUR
    }
}

/**
 * Property model for host features
 */
data class Property(
    @SerializedName(value = "id", alternate = ["_id", "listingId"])
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val location: PropertyLocation = PropertyLocation(),
    val pricing: PropertyPricing = PropertyPricing(),
    val images: List<String> = emptyList(),
    val rating: Double = 0.0,
    val reviewCount: Int = 0,
    val amenities: List<String> = emptyList(),
    val isAvailable: Boolean = true,
    val propertyType: String = "",
    /** Listing status from API: "published", "active", "pending_review", "rejected", "draft", etc. (iOS parity) */
    @SerializedName(value = "status", alternate = ["listingStatus"])
    val status: String? = null,
    val bedrooms: Int = 0,
    val bathrooms: Int = 0,
    val maxGuests: Int = 1,
    val createdAt: String = "",
    val updatedAt: String = "",
    /**
     * Service delivery mode — one of "online", "in_person", or "both".
     * Null / blank for non-service listings (parking, rooms, storage,
     * gear, etc.) and for legacy service listings created before this
     * field existed, which keeps old listings rendering safely.
     */
    @SerializedName(value = "sessionType", alternate = ["session_type"])
    val sessionType: String? = null,
    /** Online session configuration — present only when [sessionType] is "online" or "both". */
    @SerializedName(value = "onlineSession", alternate = ["online_session"])
    val onlineSession: OnlineSession? = null
) {
    // ── Status helpers (iOS parity: HostListingSummary) ──────────

    val isPendingReview: Boolean get() {
        val s = (status ?: "").trim().lowercase()
        return s == "pending_review" || s == "pending" ||
            s == "awaiting_approval" || s == "awaiting_review" ||
            s == "under_review" || s == "in_review" || s == "submitted"
    }

    val isActiveStatus: Boolean get() {
        val s = (status ?: "").trim().lowercase()
        return s in BOOKABLE_LISTING_STATUSES
    }

    val isRejected: Boolean get() {
        val s = (status ?: "").trim().lowercase()
        return s == "rejected"
    }

    /**
     * Whether this listing can accept bookings from guests.
     * Matches backend isListingBookable() from availabilityService.js.
     *
     * Bookable requires:
     *   - status in [active, published, approved]
     *   - not deleted, snoozed, archived, or hidden
     */
    val isBookable: Boolean get() {
        val s = (status ?: "").trim().lowercase()
        return s in BOOKABLE_LISTING_STATUSES
    }

    val displayStatus: String get() = when {
        isPendingReview -> "Under Review"
        isActiveStatus -> "Active"
        isRejected -> "Rejected"
        else -> status?.replaceFirstChar { it.uppercase() } ?: "Unknown"
    }

    companion object {
        /**
         * Backend source of truth: BOOKABLE_LISTING_STATUSES from bookingStatuses.js.
         * Only these statuses allow guest bookings.
         */
        val BOOKABLE_LISTING_STATUSES = setOf("active", "published", "approved")

        /**
         * Backend source of truth: BOOKABLE_MODERATION_STATUSES.
         * Only listings with this moderation status are bookable.
         */
        val BOOKABLE_MODERATION_STATUSES = setOf("published")

        /**
         * Non-bookable statuses that Android must NOT treat as available in guest flow.
         */
        val NON_BOOKABLE_STATUSES = setOf(
            "draft", "inactive", "pending_review", "rejected",
            "archived", "unpublished_account_deleted", "permanently_deleted"
        )
    }
}

data class PropertyLocation(
    val city: String = "",
    val country: String = "",
    val address: String = "",
    val state: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0
)

data class PropertyPricing(
    val basePrice: Double = 0.0,
    val currency: String = "USD",
    val unit: String = "hour",
    val pricingType: String = "hour",
    val cleaningFee: Double = 0.0,
    val serviceFee: Double = 0.0
)

/**
 * Online session configuration attached to service-category listings
 * whose delivery mode is "online" or "both".  All fields are optional
 * so legacy listings (which never had this block) still decode cleanly
 * and render safely — absent data falls back to sensible defaults.
 */
data class OnlineSession(
    val platforms: List<String> = emptyList(),
    @SerializedName(value = "sessionLink", alternate = ["session_link"])
    val sessionLink: String? = null,
    @SerializedName(value = "shareLinkAfterBooking", alternate = ["share_link_after_booking"])
    val shareLinkAfterBooking: Boolean = true,
    @SerializedName(value = "timeZone", alternate = ["time_zone", "timezone"])
    val timeZone: String? = null,
    @SerializedName(value = "meetingInstructions", alternate = ["meeting_instructions"])
    val meetingInstructions: String? = null,
    val notes: String? = null
)

/**
 * Per-unit price map used when switching between pricing modes.
 * Mirrors the web platform's `prices: { hour, day, month }` field.
 */
data class PricingPrices(
    val hour: Double = 0.0,
    val day: Double = 0.0,
    val week: Double = 0.0,
    val month: Double = 0.0,
)
