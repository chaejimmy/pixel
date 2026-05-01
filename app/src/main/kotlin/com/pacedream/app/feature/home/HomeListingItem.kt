package com.pacedream.app.feature.home

/**
 * Home listing item model
 */
data class HomeListingItem(
    val id: String,
    val title: String,
    val imageUrl: String?,
    val location: String?,
    val price: String?,
    val rating: Double?,
    val type: String,
    val shareCategory: String? = null,
    val subCategory: String? = null,
    /**
     * True when the host has enabled instant booking and we should render
     * an "Instant Book" affordance on the card. Null = unknown (treat as
     * request-to-book), false = explicitly request-to-book.
     */
    val instantBook: Boolean? = null,
    /**
     * Listing availability for the currently-selected dates (or overall, if
     * no dates are filtered). False marks the card as fully booked / out of
     * stock so the UI can dim it and disable the click. Null = unknown.
     */
    val available: Boolean? = null,
    val latitude: Double? = null,
    val longitude: Double? = null
)
