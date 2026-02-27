package com.pacedream.app.feature.listingdetail

import com.google.android.gms.maps.model.LatLng

data class ListingDetailUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val inlineErrorMessage: String? = null,
    val listing: ListingDetailModel? = null,
    val isFavorite: Boolean = false,
    val isTogglingFavorite: Boolean = false,
    val wishlistItemId: String? = null,
    val mapCoordinate: LatLng? = null,
    val isGeocoding: Boolean = false,
    // Reviews
    val reviews: List<ReviewModel> = emptyList(),
    val reviewSummary: ReviewSummary? = null,
    val isLoadingReviews: Boolean = false,
    val isSubmittingReview: Boolean = false,
    val reviewSubmitSuccess: Boolean = false
)

