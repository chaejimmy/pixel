package com.pacedream.app.feature.collections

/**
 * Data models for user-created lists/collections.
 * Matches the web platform's "Create a List" feature.
 */
data class UserCollection(
    val id: String,
    val name: String,
    val description: String? = null,
    val coverImageUrl: String? = null,
    val itemCount: Int = 0,
    val isPublic: Boolean = false,
    val createdAt: String? = null,
    val items: List<CollectionItem> = emptyList()
)

data class CollectionItem(
    val id: String,
    val listingId: String,
    val title: String,
    val imageUrl: String? = null,
    val location: String? = null,
    val price: String? = null,
    val rating: Double? = null,
    val type: String = ""
)

data class CreateCollectionRequest(
    val name: String,
    val description: String? = null,
    val isPublic: Boolean = false
)

data class AddToCollectionRequest(
    val collectionId: String,
    val listingId: String
)
