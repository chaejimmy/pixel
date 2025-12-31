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
    val amenities: List<String> = emptyList(),
    val rating: Double? = null,
    val reviewCount: Int? = null
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

data class PropertyDetailUiState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val inlineErrorMessage: String? = null,
    val detail: PropertyDetailModel? = null
)

