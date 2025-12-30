package com.shourov.apps.pacedream.feature.homefeed

data class HomeCard(
    val id: String,
    val title: String,
    val location: String?,
    val imageUrl: String?,
    val priceText: String?,
    val rating: Double?
)

enum class HomeSectionKey(val displayTitle: String, val shareType: String?) {
    HOURLY("Hourly spaces", "USE"),
    GEAR("Rent gear", "BORROW"),
    SPLIT("Split stays", "SPLIT")
}

data class HomeSection(
    val key: HomeSectionKey,
    val items: List<HomeCard>,
    val isLoading: Boolean,
    val errorMessage: String? = null
)

data class HomeFeedState(
    val isRefreshing: Boolean = false,
    val headerTitle: String = "Find your perfect stay",
    val sections: List<HomeSection> = listOf(
        HomeSection(HomeSectionKey.HOURLY, emptyList(), isLoading = true),
        HomeSection(HomeSectionKey.GEAR, emptyList(), isLoading = true),
        HomeSection(HomeSectionKey.SPLIT, emptyList(), isLoading = true),
    ),
    val globalErrorMessage: String? = null
)

