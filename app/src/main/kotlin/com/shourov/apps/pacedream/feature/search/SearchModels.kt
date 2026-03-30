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
    val hostAvatar: String? = null
)

data class SearchPage(
    val items: List<SearchResultItem>,
    val hasMore: Boolean
)

data class AutocompleteSuggestion(
    val value: String
)

