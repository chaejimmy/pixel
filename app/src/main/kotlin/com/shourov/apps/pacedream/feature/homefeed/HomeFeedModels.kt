package com.shourov.apps.pacedream.feature.homefeed

data class HomeCard(
    val id: String,
    val title: String,
    val location: String?,
    val imageUrl: String?,
    val priceText: String?,
    val rating: Double?,
    /** Subcategory / room_type / item_type — used for client-side resource type filtering. */
    val subCategory: String? = null,
    /** Backend shareCategory (uppercase taxonomy value) for service identification. */
    val shareCategory: String? = null,
)

enum class HomeSectionKey(val displayTitle: String, val shareType: String?) {
    SPACES("Spaces", "SHARE"),
    ITEMS("Items", "BORROW"),
    SERVICES("Services", "SHARE"),
}

data class HomeSection(
    val key: HomeSectionKey,
    val items: List<HomeCard>,
    val isLoading: Boolean,
    val errorMessage: String? = null
)

data class HomeFeedState(
    val isRefreshing: Boolean = false,
    val headerTitle: String = "Discover",
    val headerSubtitle: String = "Find spaces, items, and services — only for the time you need.",
    val sections: List<HomeSection> = listOf(
        HomeSection(HomeSectionKey.SPACES, emptyList(), isLoading = true),
        HomeSection(HomeSectionKey.ITEMS, emptyList(), isLoading = true),
        HomeSection(HomeSectionKey.SERVICES, emptyList(), isLoading = true),
    ),
    val globalErrorMessage: String? = null
)

