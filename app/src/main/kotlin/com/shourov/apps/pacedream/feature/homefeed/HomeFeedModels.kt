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

/**
 * Primary home sections. Display names are the **new** Stays / Gear / Help
 * taxonomy — the enum identifiers stay stable so backend mapping, navigation
 * routes and persisted state survive the rename.
 *
 * `shareType` is the backend filter (`USE` | `BORROW` | `SHARE`) the section
 * pulls from. Backend buckets haven't changed; only the customer-facing labels.
 */
enum class HomeSectionKey(val displayTitle: String, val shareType: String?) {
    /** Lodging-style listings — labelled "Stays". Backed by shareType=USE. */
    SPACES("Stays", "USE"),
    /** Rentable physical items — labelled "Gear". Backed by shareType=BORROW. */
    ITEMS("Gear", "BORROW"),
    /** Local services & helpers — labelled "Help". Backed by shareType=SHARE. */
    SERVICES("Help", "SHARE"),
}

data class HomeSection(
    val key: HomeSectionKey,
    val items: List<HomeCard>,
    val isLoading: Boolean,
    val errorMessage: String? = null
)

data class HomeFeedState(
    val isRefreshing: Boolean = false,
    val headerTitle: String = "Use what you need,\nonly for the time you need it.",
    val headerSubtitle: String = "Find stays, gear, spaces, and local help nearby.",
    val sections: List<HomeSection> = listOf(
        HomeSection(HomeSectionKey.SPACES, emptyList(), isLoading = true),
        HomeSection(HomeSectionKey.ITEMS, emptyList(), isLoading = true),
        HomeSection(HomeSectionKey.SERVICES, emptyList(), isLoading = true),
    ),
    val globalErrorMessage: String? = null
)
