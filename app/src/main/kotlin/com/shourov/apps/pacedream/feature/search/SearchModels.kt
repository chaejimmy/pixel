package com.shourov.apps.pacedream.feature.search

/**
 * Search result item - website parity with SearchCardItem.
 * Website fields: id, title, price, location, image, images[], rating, reviewCount,
 * category, available, isNew, hostName, hostAvatar
 */
data class SearchResultItem(
    val id: String,
    val title: String,
    val location: String?,
    val imageUrl: String?,
    val images: List<String> = emptyList(),
    val priceText: String?,
    val rating: Double?,
    val reviewCount: Int? = null,
    val category: String? = null,
    val available: Boolean = true,
    val isNew: Boolean = false,
    val hostName: String? = null,
    val hostAvatar: String? = null,
    /**
     * Listing coordinates used by the map results mode.  Populated only
     * when the backend response carries geocoded coordinates — items
     * without valid coords are hidden from the map and a banner tells
     * the user how many of the total results are mappable.
     */
    val latitude: Double? = null,
    val longitude: Double? = null,
    /**
     * True when the listing is bookable without host approval.  Populated
     * only when the backend response surfaces the flag; null means
     * unknown (the badge is hidden rather than assumed false).
     * No search-query filter is sent — see SearchRepository.search().
     */
    val instantBook: Boolean? = null
)

data class SearchPage(
    val items: List<SearchResultItem>,
    val hasMore: Boolean
)

data class AutocompleteSuggestion(
    val value: String
)

/**
 * Visible-area bounds emitted on "Search this area".  Stored in
 * SearchUiState after a successful bbox search so the map composable
 * can compare against the current camera view and decide whether the
 * affordance is still useful.  Matches the backend's swLat / swLng /
 * neLat / neLng contract on the existing search endpoints.
 */
data class MapBounds(
    val swLat: Double,
    val swLng: Double,
    val neLat: Double,
    val neLng: Double,
) {
    val centerLat: Double get() = (swLat + neLat) / 2.0
    val centerLng: Double get() = (swLng + neLng) / 2.0
    val latSpan: Double get() = (neLat - swLat).coerceAtLeast(0.0)
    val lngSpan: Double get() = (neLng - swLng).coerceAtLeast(0.0)
}

