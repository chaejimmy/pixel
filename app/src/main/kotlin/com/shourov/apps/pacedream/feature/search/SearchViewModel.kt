package com.shourov.apps.pacedream.feature.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shourov.apps.pacedream.core.network.api.ApiError
import com.shourov.apps.pacedream.core.network.api.ApiResult
import com.shourov.apps.pacedream.core.network.auth.AuthSession
import com.shourov.apps.pacedream.core.network.auth.AuthState
import com.shourov.apps.pacedream.feature.wishlist.data.WishlistRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val repo: SearchRepository,
    private val authSession: AuthSession,
    private val wishlistRepository: WishlistRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private var autocompleteJob: Job? = null

    val authState: StateFlow<AuthState> = authSession.authState

    private val _favoriteIds = MutableStateFlow<Set<String>>(emptySet())
    val favoriteIds: StateFlow<Set<String>> = _favoriteIds.asStateFlow()

    init {
        // iOS/web parity: auto-load initial results when search opens
        // (browse by default mode without requiring a query)
        submitSearch()

        viewModelScope.launch {
            authSession.authState.collectLatest { st ->
                if (st == AuthState.Unauthenticated) {
                    _favoriteIds.value = emptySet()
                } else {
                    refreshFavorites()
                }
            }
        }

        viewModelScope.launch {
            wishlistRepository.changes.collectLatest {
                if (authSession.authState.value != AuthState.Unauthenticated) {
                    refreshFavorites()
                }
            }
        }
    }

    suspend fun toggleFavorite(listingId: String): ApiResult<Boolean> {
        val wasFavorited = _favoriteIds.value.contains(listingId)
        // Optimistic UI update
        _favoriteIds.value = if (wasFavorited) _favoriteIds.value - listingId else _favoriteIds.value + listingId

        val res = if (wasFavorited) {
            wishlistRepository.removeFromWishlist(propertyId = listingId)
        } else {
            wishlistRepository.addToWishlist(propertyId = listingId)
        }

        return when (res) {
            is ApiResult.Success -> ApiResult.Success(!wasFavorited)
            is ApiResult.Failure -> {
                // Roll back optimistic update
                _favoriteIds.value = if (wasFavorited) _favoriteIds.value + listingId else _favoriteIds.value - listingId
                res
            }
        }
    }

    fun refreshFavorites() {
        viewModelScope.launch {
            try {
                when (val res = wishlistRepository.getWishlist()) {
                    is ApiResult.Success -> {
                        _favoriteIds.value = res.data.map { it.listingId.ifBlank { it.id } }.toSet()
                    }
                    is ApiResult.Failure -> {
                        // Non-blocking: keep last known favorites.
                        if (res.error is ApiError.Unauthorized) _favoriteIds.value = emptySet()
                    }
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (_: Exception) {
                // Non-blocking: keep last known favorites on unexpected error.
            }
        }
    }

    fun onQueryChanged(q: String) {
        _uiState.update { it.copy(query = q, errorMessage = null) }
        fetchAutocompleteDebounced(q)
    }
    
    fun updateSearchParams(
        shareType: String? = null,
        whatQuery: String? = null,
        city: String? = null,
        startDate: String? = null,
        endDate: String? = null,
        category: String? = null,
        sort: String? = null
    ) {
        _uiState.update { current ->
            current.copy(
                shareType = shareType ?: current.shareType,
                whatQuery = whatQuery ?: current.whatQuery,
                city = city ?: current.city,
                startDate = startDate ?: current.startDate,
                endDate = endDate ?: current.endDate,
                category = category ?: current.category,
                sort = sort ?: current.sort,
                errorMessage = null
            )
        }
    }

    /**
     * Persist the adult guest count chosen in the hero "Who" sheet.
     * No loadPage() call — the value is not part of the backend search
     * contract today (see SearchUiState.adultGuests doc).  Kept in state
     * so the summary bar can render it and so the value survives tab
     * switches + rotation within the same SearchScreen session.
     */
    fun updateAdultGuests(count: Int) {
        val bounded = count.coerceIn(0, 16)
        _uiState.update { it.copy(adultGuests = bounded) }
    }

    /**
     * Toggle between list and map presentation of the same results.
     * Does not mutate the item list or re-fetch — purely a UI mode.
     */
    fun updateViewMode(mode: SearchViewMode) {
        _uiState.update { it.copy(viewMode = mode) }
    }

    /**
     * Re-run the current search restricted to the visible map area.
     * The bounds compose with existing filters (q / whatQuery / city /
     * category / shareType / dates / sort) — nothing is cleared so the
     * host can keep refining while they pan.  Pagination resets.
     */
    fun searchInArea(bounds: MapBounds) {
        _uiState.update {
            it.copy(
                mapBounds = bounds,
                page0 = 0,
                items = emptyList(),
                hasMore = false,
                phase = SearchPhase.Loading,
                errorMessage = null,
            )
        }
        loadPage(reset = true)
    }

    private var searchJob: Job? = null

    fun submitSearch() {
        // Fresh submission from the search bar clears any prior bbox so
        // typing a new query / location scope returns to global results
        // rather than silently staying bounded to the last panned area.
        _uiState.update {
            it.copy(
                phase = SearchPhase.Loading,
                page0 = 0,
                items = emptyList(),
                hasMore = false,
                errorMessage = null,
                mapBounds = null,
            )
        }
        loadPage(reset = true)
    }

    fun refresh() {
        loadPage(reset = true)
    }

    fun loadMoreIfNeeded() {
        val state = uiState.value
        if (state.phase == SearchPhase.LoadingMore) return
        if (!state.hasMore) return
        loadPage(reset = false)
    }

    private fun loadPage(reset: Boolean) {
        if (reset) searchJob?.cancel()
        searchJob = viewModelScope.launch {
            try {
                val current = uiState.value
                val whereQuery = current.query.trim()
                val whatQ = current.whatQuery?.trim().orEmpty()

                // Allow search if either WHERE or WHAT has content, or if shareType is set
                // (browsing by category without a query is valid)
                val hasQuery = whereQuery.isNotBlank() || whatQ.isNotBlank()
                val hasShareType = !current.shareType.isNullOrBlank()

                if (!hasQuery && !hasShareType) {
                    _uiState.update { it.copy(phase = SearchPhase.Idle, items = emptyList(), hasMore = false) }
                    return@launch
                }

                val page = if (reset) 0 else current.page0 + 1
                _uiState.update {
                    it.copy(
                        phase = if (reset) SearchPhase.Loading else SearchPhase.LoadingMore,
                        page0 = page,
                        errorMessage = null
                    )
                }

                // NOTE: current.adultGuests is intentionally NOT passed to
                // repo.search().  The Android search endpoints (/v1/poc/listings,
                // /v1/listings, /v1/search) do not yet accept a guests query
                // parameter.  When the backend adds one, extend
                // SearchRepository.search() with a `guests: Int?` param,
                // thread it through the query-param builder, and add the
                // matching argument here.
                val res = repo.search(
                    q = whereQuery,
                    city = current.city?.takeIf { it.isNotBlank() }
                        ?: whereQuery.takeIf { it.isNotBlank() },
                    category = current.category?.takeIf { it.isNotBlank() },
                    page0 = page,
                    perPage = current.perPage,
                    sort = current.sort,
                    shareType = current.shareType?.takeIf { it.isNotBlank() },
                    whatQuery = current.whatQuery?.takeIf { it.isNotBlank() },
                    startDate = current.startDate?.takeIf { it.isNotBlank() },
                    endDate = current.endDate?.takeIf { it.isNotBlank() },
                    swLat = current.mapBounds?.swLat,
                    swLng = current.mapBounds?.swLng,
                    neLat = current.mapBounds?.neLat,
                    neLng = current.mapBounds?.neLng,
                )

                when (res) {
                    is ApiResult.Success -> {
                        _uiState.update { s ->
                            val newItems = if (reset) res.data.items else (s.items + res.data.items)
                            s.copy(
                                phase = if (newItems.isEmpty()) SearchPhase.Empty else SearchPhase.Success,
                                items = newItems,
                                hasMore = res.data.hasMore,
                                errorMessage = null
                            )
                        }
                    }
                    is ApiResult.Failure -> {
                        _uiState.update { s ->
                            // No silent fake data fallback; if we have prior results keep them.
                            val hasPrior = s.items.isNotEmpty()
                            s.copy(
                                phase = if (hasPrior) SearchPhase.Success else SearchPhase.Error,
                                hasMore = false,
                                errorMessage = res.error.message ?: "Search failed"
                            )
                        }
                    }
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { s ->
                    val hasPrior = s.items.isNotEmpty()
                    s.copy(
                        phase = if (hasPrior) SearchPhase.Success else SearchPhase.Error,
                        hasMore = false,
                        errorMessage = e.message ?: "Search failed"
                    )
                }
            }
        }
    }

    private fun fetchAutocompleteDebounced(q: String) {
        autocompleteJob?.cancel()
        if (q.trim().length < 2) {
            _uiState.update { it.copy(suggestions = emptyList()) }
            return
        }

        autocompleteJob = viewModelScope.launch {
            try {
                delay(250)
                val res = repo.autocompleteWhere(q.trim())
                when (res) {
                    is ApiResult.Success -> _uiState.update { it.copy(suggestions = res.data) }
                    is ApiResult.Failure -> {
                        // Non-blocking; keep suggestions empty on failure.
                        _uiState.update { it.copy(suggestions = emptyList()) }
                    }
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (_: Exception) {
                _uiState.update { it.copy(suggestions = emptyList()) }
            }
        }
    }
}

/**
 * Presentation mode for the already-loaded search results.  Toggling
 * does not mutate the item list or re-fetch — it only changes which
 * composable renders the current [SearchUiState.items].
 */
enum class SearchViewMode { LIST, MAP }

data class SearchUiState(
    val query: String = "",
    val city: String? = null,
    val category: String? = null,
    val sort: String? = null,
    val perPage: Int = 24,
    val page0: Int = 0,
    val items: List<SearchResultItem> = emptyList(),
    val hasMore: Boolean = false,
    val suggestions: List<AutocompleteSuggestion> = emptyList(),
    val phase: SearchPhase = SearchPhase.Idle,
    val errorMessage: String? = null,
    val shareType: String? = "SHARE", // Default to SHARE (Spaces) matching iOS/web
    val whatQuery: String? = null, // Keywords search
    val startDate: String? = null, // ISO date string
    val endDate: String? = null, // ISO date string
    /**
     * Adult guest count selected via the hero "Who" picker or the in-screen
     * guests sheet.  Persisted in state and shown in the summary bar, but
     * NOT forwarded to SearchRepository.search() yet: the backend listing
     * search endpoints (/v1/poc/listings, /v1/listings, /v1/search) do not
     * currently accept a guests / guestCount / adults query parameter.
     * See the `guests` filter in SearchScreen.GuestsPickerSheet for UX.
     * 0 == "any / unspecified".
     */
    val adultGuests: Int = 0,
    /** Current presentation mode for results; list is the default. */
    val viewMode: SearchViewMode = SearchViewMode.LIST,
    /**
     * Bounds used for the most recent successful search.  Null when the
     * last search was unbounded (the default).  Populated by
     * `searchInArea(bounds)`; cleared when the user re-submits from
     * the search bar so typing a new query returns to global scope.
     */
    val mapBounds: MapBounds? = null
)

enum class SearchPhase {
    Idle,
    Loading,
    LoadingMore,
    Success,
    Empty,
    Error
}

