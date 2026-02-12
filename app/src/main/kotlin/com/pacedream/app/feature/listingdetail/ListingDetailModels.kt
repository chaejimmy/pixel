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
    val category: String? = null
)

data class ListingLocation(
    val city: String? = null,
    val state: String? = null,
    val address: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null
) {
    val cityState: String?
        get() = listOfNotNull(city?.takeIf { it.isNotBlank() }, state?.takeIf { it.isNotBlank() })
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
    val weeklyDiscountPercent: Int? = null
) {
    /**
     * Format price to match iOS: "$12/hr" or "$80/night" (no spaces, lowercase unit)
     */
    val displayPrimary: String?
        get() {
            val amount = hourlyFrom ?: basePrice ?: return null
            val symbol = when ((currency ?: "USD").uppercase()) {
                "USD" -> "$"
                else -> "$"
            }
            val freq = frequencyLabel?.takeIf { it.isNotBlank() }?.lowercase()
                ?: if (hourlyFrom != null) "hr" else null
            val formattedAmount = trimTrailingZeros(amount)
            return if (freq != null) "$symbol$formattedAmount/$freq" else "$symbol$formattedAmount"
        }

    private fun trimTrailingZeros(value: Double): String {
        val asLong = value.toLong()
        return if (value == asLong.toDouble()) asLong.toString() else value.toString()
    }
}

data class ListingHost(
    val id: String? = null,
    val name: String? = null,
    val avatarUrl: String? = null
)

