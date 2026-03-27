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
    val subCategory: String? = null
)
