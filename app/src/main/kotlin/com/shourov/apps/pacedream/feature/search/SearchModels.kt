package com.shourov.apps.pacedream.feature.search

data class SearchResultItem(
    val id: String,
    val title: String,
    val location: String?,
    val imageUrl: String?,
    val priceText: String?,
    val rating: Double?
)

data class SearchPage(
    val items: List<SearchResultItem>,
    val hasMore: Boolean
)

data class AutocompleteSuggestion(
    val value: String
)

