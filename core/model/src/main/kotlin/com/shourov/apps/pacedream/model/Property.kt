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
    val updatedAt: String = ""
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
        return s in listOf("published", "active", "approved", "true")
    }

    val isRejected: Boolean get() {
        val s = (status ?: "").trim().lowercase()
        return s == "rejected"
    }

    val displayStatus: String get() = when {
        isPendingReview -> "Under Review"
        isActiveStatus -> "Active"
        isRejected -> "Rejected"
        else -> status?.replaceFirstChar { it.uppercase() } ?: "Unknown"
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
 * Per-unit price map used when switching between pricing modes.
 * Mirrors the web platform's `prices: { hour, day, month }` field.
 */
data class PricingPrices(
    val hour: Double = 0.0,
    val day: Double = 0.0,
    val week: Double = 0.0,
    val month: Double = 0.0,
)
