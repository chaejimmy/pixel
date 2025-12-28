package com.shourov.apps.pacedream.model

/**
 * Property model for host features
 */
data class Property(
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
    val bedrooms: Int = 0,
    val bathrooms: Int = 0,
    val maxGuests: Int = 1,
    val createdAt: String = "",
    val updatedAt: String = ""
)

data class PropertyLocation(
    val city: String = "",
    val country: String = "",
    val address: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0
)

data class PropertyPricing(
    val basePrice: Double = 0.0,
    val currency: String = "USD",
    val cleaningFee: Double = 0.0,
    val serviceFee: Double = 0.0
)
