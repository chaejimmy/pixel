package com.pacedream.app.feature.listingdetail

import android.content.Context
import android.location.Geocoder
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.model.LatLng
import com.pacedream.app.core.auth.AuthState
import com.pacedream.app.core.auth.SessionManager
import com.pacedream.app.core.auth.TokenStorage
import com.pacedream.app.core.config.AppConfig
import com.pacedream.app.core.network.ApiClient
import com.pacedream.app.core.network.ApiResult
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import timber.log.Timber
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class ListingDetailViewModel @Inject constructor(
    private val repository: ListingDetailRepository,
    private val wishlistRepository: ListingWishlistRepository,
    private val reviewRepository: ReviewRepository,
    private val sessionManager: SessionManager,
    private val apiClient: ApiClient,
    private val appConfig: AppConfig,
    private val tokenStorage: TokenStorage,
    private val json: Json,
    @ApplicationContext private val context: Context
) : ViewModel() {

    sealed class Effect {
        data object ShowAuthRequired : Effect()
        data class ShowToast(val message: String) : Effect()
        data class OpenMaps(val uri: String) : Effect()
        data class Share(val text: String) : Effect()
        data class NavigateToThread(val threadId: String) : Effect()
    }

    private val _uiState = MutableStateFlow(ListingDetailUiState(isLoading = true))
    val uiState: StateFlow<ListingDetailUiState> = _uiState.asStateFlow()

    private val _effects = Channel<Effect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    private var currentListingId: String? = null
    private var currentListingType: String = ""

    fun isAuthenticated(): Boolean = sessionManager.authState.value == AuthState.Authenticated

    fun getListingType(): String = currentListingType

    fun load(listingId: String, listingType: String = "", initialListing: ListingCardModel?) {
        if (currentListingId == listingId && _uiState.value.listing != null) return
        currentListingId = listingId
        currentListingType = listingType

        // Seed cached/partial content immediately.
        if (initialListing != null) {
            _uiState.update {
                it.copy(
                    listing = ListingDetailModel(
                        id = initialListing.id,
                        title = initialListing.title,
                        imageUrls = listOfNotNull(initialListing.imageUrl),
                        rating = initialListing.rating
                    ),
                    isFavorite = false
                )
            }
        }

        refresh()
        // Eagerly fetch review summary + first-page reviews so the listing detail
        // renders real rating + preview cards on first view (E2E 2026-04-12 review
        // flagged the section as a "Reviews preview coming soon" stub because it was
        // lazy-loaded only when the user tapped "See all"). This is the small
        // network call that gates conversion the most on first-time guests.
        loadReviews()
    }

    fun refresh() {
        val listingId = currentListingId ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, inlineErrorMessage = null) }

            when (val result = repository.fetchListingDetail(listingId, currentListingType)) {
                is ApiResult.Success -> {
                    val listing = result.data
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            listing = listing,
                            isFavorite = listing.isFavorite ?: it.isFavorite,
                            errorMessage = null,
                            inlineErrorMessage = null
                        )
                    }
                    resolveMapCoordinateIfNeeded(listing)
                }

                is ApiResult.Failure -> {
                    val hasCached = _uiState.value.listing != null
                    if (hasCached) {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                inlineErrorMessage = result.error.message ?: "Failed to load"
                            )
                        }
                    } else {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                errorMessage = result.error.message ?: "Failed to load"
                            )
                        }
                    }
                }
            }
        }
    }

    fun toggleFavorite() {
        viewModelScope.launch {
            val isAuthed = sessionManager.authState.value == AuthState.Authenticated
            if (!isAuthed) {
                _effects.send(Effect.ShowAuthRequired)
                return@launch
            }

            val listingId = currentListingId ?: return@launch
            val previous = _uiState.value.isFavorite
            val desired = !previous

            // Optimistic UI
            _uiState.update { it.copy(isTogglingFavorite = true, isFavorite = desired) }

            val result = if (desired) {
                wishlistRepository.addToWishlist(listingId)
            } else {
                wishlistRepository.removeFromWishlist(listingId, _uiState.value.wishlistItemId)
            }

            when (result) {
                is ApiResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isFavorite = result.data.isFavorite,
                            wishlistItemId = result.data.wishlistItemId ?: it.wishlistItemId,
                            isTogglingFavorite = false
                        )
                    }
                    _effects.send(
                        Effect.ShowToast(
                            if (result.data.isFavorite) "Saved to Favorites" else "Removed"
                        )
                    )
                }
                is ApiResult.Failure -> {
                    // Revert on failure
                    _uiState.update { it.copy(isFavorite = previous, isTogglingFavorite = false) }
                    _effects.send(Effect.ShowToast(result.error.message ?: "Failed to update favorite"))
                }
            }
        }
    }

    fun share() {
        viewModelScope.launch {
            val listing = _uiState.value.listing ?: return@launch
            _effects.send(Effect.Share(listing.title))
        }
    }

    fun loadReviews() {
        val listingId = currentListingId ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingReviews = true) }
            when (val result = reviewRepository.fetchReviews(listingId)) {
                is ApiResult.Success -> {
                    val (summary, reviews) = result.data
                    _uiState.update {
                        it.copy(
                            isLoadingReviews = false,
                            reviewSummary = summary,
                            reviews = reviews
                        )
                    }
                }
                is ApiResult.Failure -> {
                    _uiState.update { it.copy(isLoadingReviews = false) }
                    Timber.e("Failed to load reviews: ${result.error.message}")
                }
            }
        }
    }

    fun submitReview(rating: Double, comment: String, categoryRatings: CategoryRatings?) {
        val listingId = currentListingId ?: return
        viewModelScope.launch {
            val isAuthed = sessionManager.authState.value == AuthState.Authenticated
            if (!isAuthed) {
                _effects.send(Effect.ShowAuthRequired)
                return@launch
            }

            _uiState.update { it.copy(isSubmittingReview = true, reviewSubmitSuccess = false) }

            val request = CreateReviewRequest(
                listingId = listingId,
                rating = rating,
                comment = comment,
                categoryRatings = categoryRatings
            )

            when (val result = reviewRepository.createReview(request)) {
                is ApiResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isSubmittingReview = false,
                            reviewSubmitSuccess = true,
                            reviews = listOf(result.data) + it.reviews
                        )
                    }
                    _effects.send(Effect.ShowToast("Review submitted!"))
                    // Refresh reviews to get updated summary
                    loadReviews()
                }
                is ApiResult.Failure -> {
                    _uiState.update { it.copy(isSubmittingReview = false) }
                    _effects.send(Effect.ShowToast(result.error.message ?: "Failed to submit review"))
                }
            }
        }
    }

    /**
     * Contact host: create or find an inbox thread with the host, then navigate to it.
     * Web parity: POST /v1/inbox/thread { otherUserId, listingId, mode: "guest" }
     */
    fun contactHost() {
        val hostId = _uiState.value.listing?.host?.id
        if (hostId == null) {
            viewModelScope.launch {
                _effects.send(Effect.ShowToast("Host information not available"))
            }
            return
        }

        viewModelScope.launch {
            val url = appConfig.buildApiUrl("inbox", "thread")
            val body = buildString {
                append("{")
                append("\"otherUserId\":\"$hostId\"")
                currentListingId?.let { append(",\"listingId\":\"$it\"") }
                append(",\"mode\":\"guest\"")
                append("}")
            }

            when (val result = apiClient.post(url, body, includeAuth = true)) {
                is ApiResult.Success -> {
                    try {
                        val element = json.parseToJsonElement(result.data)
                        val obj = element.jsonObject
                        val threadId = obj["data"]?.jsonObject?.get("_id")?.jsonPrimitive?.content
                            ?: obj["_id"]?.jsonPrimitive?.content
                            ?: obj["data"]?.jsonObject?.get("id")?.jsonPrimitive?.content

                        if (threadId != null) {
                            _effects.send(Effect.NavigateToThread(threadId))
                        } else {
                            Timber.e("Thread ID not found in response: ${result.data}")
                            _effects.send(Effect.ShowToast("Could not start conversation"))
                        }
                    } catch (e: Exception) {
                        Timber.e(e, "Failed to parse thread response")
                        _effects.send(Effect.ShowToast("Could not start conversation"))
                    }
                }
                is ApiResult.Failure -> {
                    Timber.e("Failed to create thread: ${result.error.message}")
                    _effects.send(Effect.ShowToast(result.error.message ?: "Failed to contact host"))
                }
            }
        }
    }

    fun openInMaps() {
        viewModelScope.launch {
            val listing = _uiState.value.listing ?: return@launch
            val loc = listing.location

            val lat = _uiState.value.mapCoordinate?.latitude ?: loc?.latitude
            val lng = _uiState.value.mapCoordinate?.longitude ?: loc?.longitude
            val query = loc?.fullAddress ?: loc?.cityState ?: listing.title

            val uri = if (lat != null && lng != null) {
                "geo:$lat,$lng?q=$lat,$lng(${UriEncoding.encode(query)})"
            } else {
                "geo:0,0?q=${UriEncoding.encode(query)}"
            }
            _effects.send(Effect.OpenMaps(uri))
        }
    }

    private fun resolveMapCoordinateIfNeeded(listing: ListingDetailModel) {
        val loc = listing.location ?: return
        
        // First, try to use direct coordinates if available
        val lat = loc.latitude
        val lng = loc.longitude
        if (lat != null && lng != null && !(lat == 0.0 && lng == 0.0)) {
            Timber.d("Using direct coordinates: lat=$lat, lng=$lng")
            _uiState.update { it.copy(mapCoordinate = LatLng(lat, lng), isGeocoding = false) }
            return
        }

        // If no direct coordinates, try geocoding from address
        val address = loc.fullAddress ?: loc.cityState
        if (address == null) {
            Timber.w("No coordinates or address available for location")
            _uiState.update { it.copy(isGeocoding = false, mapCoordinate = null) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isGeocoding = true) }
            try {
                Timber.d("Geocoding address: $address")
                val first = withContext(Dispatchers.IO) {
                    val geocoder = Geocoder(context, Locale.getDefault())
                    @Suppress("DEPRECATION")
                    geocoder.getFromLocationName(address, 1)?.firstOrNull()
                }
                if (first != null) {
                    Timber.d("Geocoding successful: lat=${first.latitude}, lng=${first.longitude}")
                    _uiState.update {
                        it.copy(
                            mapCoordinate = LatLng(first.latitude, first.longitude),
                            isGeocoding = false
                        )
                    }
                } else {
                    Timber.w("Geocoding returned no results for address: $address")
                    _uiState.update { it.copy(isGeocoding = false) }
                }
            } catch (e: Exception) {
                Timber.e(e, "Geocoding failed for address: $address")
                _uiState.update { it.copy(isGeocoding = false) }
            }
        }
    }
}

private object UriEncoding {
    fun encode(text: String): String =
        java.net.URLEncoder.encode(text, Charsets.UTF_8.name())
}

