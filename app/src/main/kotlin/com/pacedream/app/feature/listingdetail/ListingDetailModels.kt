package com.pacedream.app.feature.listingdetail

import kotlinx.serialization.Serializable

/**
 * Lightweight listing card model used for cached/partial rendering when detail fetch fails.
 */
@Serializable
data class ListingCardModel(
    val id: String,
    val title: String,
    val imageUrl: String? = null,
    val location: String? = null,
    val priceLabel: String? = null,
    val rating: Double? = null,
    val type: String = ""
)

data class ListingDetailModel(
    val id: String,
    val title: String,
    val description: String? = null,
    val imageUrls: List<String> = emptyList(),
    val location: ListingLocation? = null,
    val pricing: ListingPricing? = null,
    val host: ListingHost? = null,
    val amenities: List<String> = emptyList(),
    val rating: Double? = null,
    val reviewCount: Int? = null,
    val isFavorite: Boolean? = null,
    val cancellationPolicy: CancellationPolicy? = null,
    val category: String? = null,
    // Web parity: property details
    val propertyType: String? = null,
    val maxGuests: Int? = null,
    val bedrooms: Int? = null,
    val beds: Int? = null,
    val bathrooms: Int? = null,
    // Web parity: house rules
    val houseRules: List<String> = emptyList(),
    val checkInTime: String? = null,
    val checkOutTime: String? = null,
    // Web parity: safety features
    val safetyFeatures: List<String> = emptyList(),
    // Web parity: status
    val available: Boolean? = null,
    val instantBook: Boolean? = null,
    // Web parity: split listing fields
    val shareType: String? = null,
    val totalCost: Double? = null,
    val slotsTotal: Int? = null,
    val slotsFilled: Int? = null,
    val splitStatus: String? = null,
    val deadlineAt: String? = null,
    // Host visibility: listing moderation status
    val listingStatus: String? = null,
    /**
     * Service delivery mode — "online", "in_person", or "both".  Null
     * for non-service listings and legacy service listings created
     * before this field existed.  Guest-facing detail screen uses this
     * to show a delivery badge and to suppress the map for online-only
     * services.
     */
    val sessionType: String? = null,
    val onlineSession: OnlineSessionInfo? = null
) {
    val hasPropertyDetails: Boolean
        get() = propertyType != null || maxGuests != null || bedrooms != null || beds != null || bathrooms != null

    val isSplitListing: Boolean
        get() = shareType?.uppercase() == "SPLIT"

    val isPendingReview: Boolean
        get() {
            val s = (listingStatus ?: "").trim().lowercase()
            return s == "pending_review" || s == "pending" || s == "awaiting_approval" ||
                s == "under_review" || s == "in_review" || s == "submitted"
        }

    /** True when the listing publishes itself as online-only. */
    val isOnlineOnly: Boolean
        get() = (sessionType ?: "").trim().lowercase() == "online"

    /** True when the listing supports both online and in-person bookings. */
    val isHybridDelivery: Boolean
        get() = (sessionType ?: "").trim().lowercase() == "both"

    /** Human-readable label for the delivery mode, or null if absent. */
    val deliveryModeLabel: String?
        get() = when ((sessionType ?: "").trim().lowercase()) {
            "online" -> "Online"
            "in_person", "inperson", "physical" -> "In person"
            "both", "hybrid" -> "Online + In person"
            else -> null
        }
}

/**
 * Online-session configuration surfaced on the guest detail screen.
 * Mirror of [com.shourov.apps.pacedream.model.OnlineSession] adapted
 * to the detail-screen's Kotlin-stdlib-only data layer.
 */
data class OnlineSessionInfo(
    val platforms: List<String> = emptyList(),
    val sessionLink: String? = null,
    val shareLinkAfterBooking: Boolean = true,
    val timeZone: String? = null,
    val meetingInstructions: String? = null,
    val notes: String? = null,
)

data class ListingLocation(
    val city: String? = null,
    val state: String? = null,
    val address: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val country: String? = null,
    val zipCode: String? = null,
    val neighborhood: String? = null
) {
    val cityState: String?
        get() = listOfNotNull(city?.trim()?.takeIf { it.isNotBlank() }, state?.trim()?.takeIf { it.isNotBlank() })
            .joinToString(", ")
            .takeIf { it.isNotBlank() }

    val fullAddress: String?
        get() = listOfNotNull(
            address?.takeIf { it.isNotBlank() },
            city?.takeIf { it.isNotBlank() },
            state?.takeIf { it.isNotBlank() }
        ).joinToString(", ").takeIf { it.isNotBlank() }
}

data class CancellationPolicy(
    val type: String = "flexible",
    val freeHoursBefore: Int = 2,
    val description: String? = null
) {
    val displayText: String
        get() = description ?: when (type) {
            "flexible" -> "Free cancellation up to $freeHoursBefore hours before check-in"
            "moderate" -> "Free cancellation up to 24 hours before check-in. 50% refund after."
            "strict" -> "50% refund up to 48 hours before check-in. No refund after."
            else -> "Cancellation policy varies. Check listing details."
        }
}

data class ListingPricing(
    val hourlyFrom: Double? = null,
    val basePrice: Double? = null,
    val currency: String? = null,
    val frequencyLabel: String? = null,
    val cleaningFee: Double? = null,
    val weeklyDiscountPercent: Int? = null,
    val serviceFee: Double? = null,
    val monthlyDiscountPercent: Int? = null
) {
    /**
     * Format price to match iOS: "$12/hr" or "$80/night" (no spaces, lowercase unit)
     */
    val displayPrimary: String?
        get() {
            val amount = hourlyFrom ?: basePrice ?: return null
            val symbol = when ((currency ?: "USD").uppercase()) {
                "USD" -> "$"
                "EUR" -> "€"
                "GBP" -> "£"
                "AED" -> "د.إ"
                "BDT" -> "৳"
                "INR" -> "₹"
                "JPY" -> "¥"
                "CAD" -> "CA$"
                "AUD" -> "A$"
                else -> "$"
            }
            val freq = frequencyLabel?.takeIf { it.isNotBlank() }?.let { normalizeUnit(it) }
                ?: if (hourlyFrom != null) "hr" else null
            val formattedAmount = trimTrailingZeros(amount)
            return if (freq != null) "$symbol$formattedAmount/$freq" else "$symbol$formattedAmount"
        }

    private fun trimTrailingZeros(value: Double): String {
        val asLong = value.toLong()
        return if (value == asLong.toDouble()) asLong.toString() else value.toString()
    }

    /** Normalize backend frequency strings to short display labels matching iOS. */
    private fun normalizeUnit(raw: String): String = when (raw.lowercase().trim()) {
        "hourly", "hour", "hr" -> "hr"
        "daily", "day" -> "day"
        "weekly", "week", "wk" -> "wk"
        "monthly", "month", "mo" -> "mo"
        "once" -> "total"
        else -> raw.lowercase()
    }
}

data class ListingHost(
    val id: String? = null,
    val name: String? = null,
    val avatarUrl: String? = null,
    // Web parity: host profile details
    val bio: String? = null,
    val isSuperhost: Boolean? = null,
    val isVerified: Boolean? = null,
    val responseRate: Int? = null,
    val responseTime: String? = null,
    val listingCount: Int? = null,
    val joinedDate: String? = null,
    val verifications: List<String> = emptyList()
)
