package com.shourov.apps.pacedream.feature.wishlist.model

/**
 * Wishlist item model
 */
data class WishlistItem(
    val id: String,
    val listingId: String,
    val title: String,
    val description: String? = null,
    val imageUrl: String? = null,
    val price: Double? = null,
    val itemType: WishlistItemType = WishlistItemType.TIME_BASED,
    val location: String? = null,
    val rating: Double? = null
) {
    val formattedPrice: String
        get() = price?.let { "$${String.format("%.2f", it)}" } ?: ""
    
    val formattedRating: String
        get() = rating?.let { String.format("%.1f", it) } ?: ""
}

/**
 * Wishlist item types matching iOS routing behavior
 */
enum class WishlistItemType(val apiValue: String, val displayName: String) {
    TIME_BASED("time-based", "Hourly Spaces"),
    HOURLY_GEAR("hourly-gear", "Rent Gear"),
    SPLIT_STAY("room-stay", "Split Stays"),
    OTHER("other", "Other");
    
    companion object {
        fun fromString(value: String?): WishlistItemType? {
            if (value == null) return null
            val normalized = value.lowercase().trim()
            return when {
                normalized.contains("time") || normalized == "use" -> TIME_BASED
                normalized.contains("gear") || normalized == "borrow" || 
                    normalized.contains("car") || normalized.contains("vehicle") -> HOURLY_GEAR
                normalized.contains("split") || normalized.contains("room") ||
                    normalized.contains("stay") || normalized.contains("roommate") -> SPLIT_STAY
                else -> null
            }
        }
    }
}

/**
 * Filter option for wishlist
 */
enum class WishlistFilter(val displayName: String) {
    ALL("All"),
    HOURLY("Hourly"),
    GEAR("Gear"),
    SPLIT("Split");
    
    fun matches(item: WishlistItem): Boolean {
        return when (this) {
            ALL -> true
            HOURLY -> item.itemType == WishlistItemType.TIME_BASED
            GEAR -> item.itemType == WishlistItemType.HOURLY_GEAR
            SPLIT -> item.itemType == WishlistItemType.SPLIT_STAY
        }
    }
}

/**
 * UI state for wishlist screen
 */
sealed class WishlistUiState {
    object Loading : WishlistUiState()
    data class Success(
        val items: List<WishlistItem>,
        val selectedFilter: WishlistFilter = WishlistFilter.ALL
    ) : WishlistUiState() {
        val filteredItems: List<WishlistItem>
            get() = items.filter { selectedFilter.matches(it) }
        
        val isEmpty: Boolean
            get() = filteredItems.isEmpty()
    }
    data class Error(val message: String) : WishlistUiState()
    object Empty : WishlistUiState()
    object RequiresAuth : WishlistUiState()
}

/**
 * Events from wishlist UI
 */
sealed class WishlistEvent {
    object Refresh : WishlistEvent()
    data class FilterSelected(val filter: WishlistFilter) : WishlistEvent()
    data class RemoveItem(val item: WishlistItem) : WishlistEvent()
    data class ItemClicked(val item: WishlistItem) : WishlistEvent()
    data class BookNowClicked(val item: WishlistItem) : WishlistEvent()
}

/**
 * Navigation actions from wishlist
 */
sealed class WishlistNavigation {
    data class ToTimeBasedDetail(val itemId: String) : WishlistNavigation()
    data class ToHourlyGearDetail(val gearId: String) : WishlistNavigation()
    object ShowAuthSheet : WishlistNavigation()
}

