package com.shourov.apps.pacedream.listing

import java.util.concurrent.ConcurrentHashMap

/**
 * Lightweight in-memory listing preview cache to avoid showing fake Property Detail content.
 *
 * We populate this from Home/Search cards right before navigating to detail. Detail screens
 * then render real preview data immediately even if a full "listing detail" endpoint isn't
 * available yet.
 */
data class ListingPreview(
    val id: String,
    val title: String,
    val location: String?,
    val imageUrl: String?,
    val priceText: String?,
    val rating: Double?
)

object ListingPreviewStore {
    private val map = ConcurrentHashMap<String, ListingPreview>()

    fun put(preview: ListingPreview) {
        if (preview.id.isBlank()) return
        map[preview.id] = preview
    }

    fun get(id: String): ListingPreview? = map[id]
}

