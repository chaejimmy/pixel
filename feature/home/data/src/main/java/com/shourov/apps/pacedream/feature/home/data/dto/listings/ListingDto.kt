package com.shourov.apps.pacedream.feature.home.data.dto.listings

import com.google.gson.annotations.SerializedName
import com.shourov.apps.pacedream.feature.home.domain.models.SplitStayModel

data class ListingsResponse(
    val status: Boolean = false,
    val ok: Boolean = false,
    val message: String? = null,
    val data: List<ListingDto>? = null,
    val results: List<ListingDto>? = null,
    val listings: List<ListingDto>? = null,
    val total: Int? = null,
    val page: Int? = null,
    val limit: Int? = null,
) {
    val allListings: List<ListingDto>
        get() = data ?: results ?: listings ?: emptyList()

    val isSuccessful: Boolean
        get() = status || ok || allListings.isNotEmpty()
}

data class ListingDto(
    @SerializedName("_id")
    val id: String? = null,
    val title: String? = null,
    val name: String? = null,
    val description: String? = null,
    val category: String? = null,
    @SerializedName("shareType")
    val shareType: String? = null,
    @SerializedName("shareCategory")
    val shareCategory: String? = null,
    @SerializedName("subCategory")
    val subCategory: String? = null,
    @SerializedName("listingType")
    val listingType: String? = null,
    @SerializedName("roomType")
    val roomType: String? = null,
    @SerializedName("item_type")
    val itemType: String? = null,
    val location: ListingLocationDto? = null,
    val city: String? = null,
    val pricing: ListingPricingDto? = null,
    val price: Any? = null,
    @SerializedName("pricingUnit")
    val pricingUnit: String? = null,
    val rating: Float? = null,
    @SerializedName("avgRating")
    val avgRating: Float? = null,
    @SerializedName("reviewCount")
    val reviewCount: Int? = null,
    val image: String? = null,
    @SerializedName("imageUrl")
    val imageUrl: String? = null,
    val thumbnail: String? = null,
    val images: List<String>? = null,
    val gallery: ListingGalleryDto? = null,
)

data class ListingLocationDto(
    val city: String? = null,
    val state: String? = null,
    val country: String? = null,
    val address: String? = null,
)

data class ListingPricingDto(
    @SerializedName("base_price")
    val basePrice: Double? = null,
    val price: Double? = null,
    @SerializedName("pricing_type")
    val pricingType: String? = null,
    val frequency: String? = null,
)

data class ListingGalleryDto(
    val images: List<String>? = null,
    val thumbnail: String? = null,
)

/**
 * Service subcategory IDs matching iOS HomeService.swift filtering logic.
 * Listings with these values in roomType, subCategory, or itemType are services.
 */
private val SERVICE_SUBCATEGORY_IDS = setOf(
    "home_help", "moving_help", "cleaning_organizing", "everyday_help",
    "fitness", "learning", "creative", "other_service"
)

/**
 * Service shareCategory values (uppercase) from backend taxonomy, matching iOS.
 */
private val SERVICE_SHARE_CATEGORIES = setOf(
    "HOME_HELP", "MOVING_HELP", "CLEANING_ORGANIZING", "EVERYDAY_HELP",
    "FITNESS", "LEARNING", "CREATIVE", "OTHER_SERVICE"
)

fun ListingDto.isServiceListing(): Boolean {
    val shareCat = (shareCategory ?: "").uppercase()
    val room = (roomType ?: "").lowercase()
    val sub = (subCategory ?: "").lowercase()
    val item = (itemType ?: "").lowercase()
    return SERVICE_SHARE_CATEGORIES.contains(shareCat)
            || SERVICE_SUBCATEGORY_IDS.contains(room)
            || SERVICE_SUBCATEGORY_IDS.contains(sub)
            || SERVICE_SUBCATEGORY_IDS.contains(item)
}

fun ListingDto.toSplitStayModel(): SplitStayModel {
    val resolvedTitle = title ?: name
    val resolvedCity = location?.city ?: city
    val resolvedLocation = location?.address
        ?: listOfNotNull(location?.city, location?.state)
            .joinToString(", ")
            .ifEmpty { null }

    val primaryImage = image
        ?: imageUrl
        ?: thumbnail
        ?: images?.firstOrNull()
        ?: gallery?.images?.firstOrNull()
        ?: gallery?.thumbnail

    val resolvedPrice = pricing?.basePrice ?: pricing?.price
    val resolvedRating = rating ?: avgRating
    val resolvedPriceUnit = pricingUnit ?: pricing?.pricingType ?: pricing?.frequency ?: "hour"

    return SplitStayModel(
        _id = id,
        name = resolvedTitle,
        description = description,
        location = resolvedLocation,
        city = resolvedCity,
        price = resolvedPrice,
        priceUnit = resolvedPriceUnit,
        rating = resolvedRating,
        reviewCount = reviewCount,
        images = listOfNotNull(primaryImage) + (images ?: emptyList()).filter { it != primaryImage },
        roomType = roomType,
        isAvailable = true,
    )
}
