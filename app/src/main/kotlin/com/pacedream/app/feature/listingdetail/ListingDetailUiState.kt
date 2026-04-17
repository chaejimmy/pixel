package com.pacedream.app.feature.listingdetail

import com.google.android.gms.maps.model.LatLng

data class ListingDetailUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val inlineErrorMessage: String? = null,
    val listing: ListingDetailModel? = null,
    /**
     * True when the displayed listing data was seeded from navigation
     * arguments (list card) and has not yet been confirmed by a fresh
     * API response. The UI should show a loading overlay or shimmer
     * rather than presenting seed data as authoritative.
     */
    val isFromSeed: Boolean = false,
    val isFavorite: Boolean = false,
    val isTogglingFavorite: Boolean = false,
    val wishlistItemId: String? = null,
    val mapCoordinate: LatLng? = null,
    val isGeocoding: Boolean = false,
    // Reviews
    val reviews: List<ReviewModel> = emptyList(),
    val reviewSummary: ReviewSummary? = null,
    val isLoadingReviews: Boolean = false,
    /**
     * True when the reviews section failed to load. The UI should show a
     * subtle "Couldn't load reviews" state with a retry, instead of the
     * misleading "No reviews yet — be the first!" prompt.
     */
    val reviewsLoadFailed: Boolean = false,
    val isSubmittingReview: Boolean = false,
    val reviewSubmitSuccess: Boolean = false,
    /**
     * Id of the currently signed-in user, observed from SessionManager.
     * Drives per-row ownership detection on the reviews list so only
     * the author's own review surfaces an edit / delete affordance.
     * Null while unauthenticated or before the first emission.
     */
    val currentUserId: String? = null,
    /** True while an edit or delete request against /reviews/:id is in flight. */
    val isMutatingReview: Boolean = false,
    /** One-shot error from the last edit / delete attempt; cleared when the snackbar shows. */
    val reviewMutationError: String? = null
)

