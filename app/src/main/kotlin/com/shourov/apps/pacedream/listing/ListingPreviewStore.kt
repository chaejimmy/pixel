package com.shourov.apps.pacedream.listing

/**
 * Lightweight in-memory listing preview cache to avoid showing fake Property Detail content.
 *
 * We populate this from Home/Search cards right before navigating to detail. Detail screens
 * then render real preview data immediately even if a full "listing detail" endpoint isn't
 * available yet.
 *
 * Capped at [MAX_SIZE] entries to prevent unbounded memory growth during long sessions.
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
    private const val MAX_SIZE = 100

    private val map = object : LinkedHashMap<String, ListingPreview>(32, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, ListingPreview>?): Boolean {
            return size > MAX_SIZE
        }
    }

    fun put(preview: ListingPreview) {
        if (preview.id.isBlank()) return
        synchronized(map) { map[preview.id] = preview }
    }

    fun get(id: String): ListingPreview? = synchronized(map) { map[id] }
}

