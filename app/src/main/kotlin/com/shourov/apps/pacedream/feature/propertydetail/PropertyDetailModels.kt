package com.shourov.apps.pacedream.feature.propertydetail

data class PropertyDetailModel(
    val id: String,
    val title: String,
    val description: String? = null,
    val imageUrls: List<String> = emptyList(),
    val city: String? = null,
    val state: String? = null,
    val address: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val hourlyFrom: Double? = null,
    val currency: String? = null,
    val hostName: String? = null,
    val hostAvatarUrl: String? = null,
    val hostBio: String? = null,
    val hostIsSuperhost: Boolean? = null,
    val hostIsVerified: Boolean? = null,
    val amenities: List<String> = emptyList(),
    val rating: Double? = null,
    val reviewCount: Int? = null,
    // Property details (iOS parity)
    val propertyType: String? = null,
    val maxGuests: Int? = null,
    val bedrooms: Int? = null,
    val beds: Int? = null,
    val bathrooms: Int? = null,
    // House rules (iOS parity)
    val houseRules: List<String> = emptyList(),
    val checkInTime: String? = null,
    val checkOutTime: String? = null,
    // Safety features (iOS parity)
    val safetyFeatures: List<String> = emptyList(),
    // Pricing breakdown (iOS parity)
    val basePrice: Double? = null,
    val cleaningFee: Double? = null,
    val serviceFee: Double? = null,
    val weeklyDiscountPercent: Int? = null,
    val frequencyLabel: String? = null,
    // Status (iOS parity)
    val available: Boolean? = null,
    val instantBook: Boolean? = null,
    // Cancellation policy (iOS parity)
    val cancellationPolicyType: String? = null,
    val cancellationPolicyDescription: String? = null,
    val cancellationFreeHoursBefore: Int? = null
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

    val hasPropertyDetails: Boolean
        get() = propertyType != null || maxGuests != null || bedrooms != null || beds != null || bathrooms != null

    val currencySymbol: String
        get() = when ((currency ?: "USD").uppercase()) {
            "USD" -> "$"; "EUR" -> "€"; "GBP" -> "£"; "INR" -> "₹"
            "AED" -> "د.إ"; "CAD" -> "CA$"; "AUD" -> "A$"; "BDT" -> "৳"
            "JPY" -> "¥"; else -> "$"
        }

    val displayPrice: String?
        get() {
            val amount = hourlyFrom ?: basePrice ?: return null
            val freq = frequencyLabel?.takeIf { it.isNotBlank() }?.lowercase()
                ?: if (hourlyFrom != null) "hr" else null
            val formatted = if (amount == amount.toLong().toDouble()) amount.toLong().toString() else amount.toString()
            return if (freq != null) "$currencySymbol$formatted/$freq" else "$currencySymbol$formatted"
        }

    val cancellationPolicyText: String?
        get() {
            if (cancellationPolicyDescription != null) return cancellationPolicyDescription
            val hours = cancellationFreeHoursBefore ?: 2
            return when (cancellationPolicyType?.lowercase()) {
                "flexible" -> "Free cancellation up to $hours hours before check-in"
                "moderate" -> "Free cancellation up to 24 hours before check-in. 50% refund after."
                "strict" -> "50% refund up to 48 hours before check-in. No refund after."
                null -> null
                else -> "Cancellation policy varies. Check listing details."
            }
        }
}

data class PropertyDetailUiState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val inlineErrorMessage: String? = null,
    val detail: PropertyDetailModel? = null
)

