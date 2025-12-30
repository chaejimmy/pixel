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
    val isFavorite: Boolean? = null
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

data class ListingPricing(
    val hourlyFrom: Double? = null,
    val basePrice: Double? = null,
    val currency: String? = null,
    val frequencyLabel: String? = null
) {
    /**
     * e.g. "$120 / hr" or "$80 / night" depending on data.
     */
    val displayPrimary: String?
        get() {
            val amount = hourlyFrom ?: basePrice ?: return null
            val symbol = when ((currency ?: "USD").uppercase()) {
                "USD" -> "$"
                else -> "$"
            }
            val freq = frequencyLabel?.takeIf { it.isNotBlank() }
                ?: if (hourlyFrom != null) "hr" else null
            return if (freq != null) "$symbol${trimTrailingZeros(amount)} / $freq" else "$symbol${trimTrailingZeros(amount)}"
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

