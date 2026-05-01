package com.shourov.apps.pacedream.feature.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shourov.apps.pacedream.core.network.api.ApiError
import com.shourov.apps.pacedream.core.network.api.ApiResult
import com.shourov.apps.pacedream.core.network.auth.AuthSession
import com.shourov.apps.pacedream.core.network.auth.AuthState
import com.shourov.apps.pacedream.feature.home.presentation.components.FilterCriteria
import com.shourov.apps.pacedream.feature.wishlist.data.WishlistRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val repo: SearchRepository,
    private val authSession: AuthSession,
    private val wishlistRepository: WishlistRepository,
    private val filtersStore: SearchFiltersStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        SearchUiState(filterCriteria = filtersStore.criteria.value)
    )
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private var autocompleteJob: Job? = null

    val authState: StateFlow<AuthState> = authSession.authState

    private val _favoriteIds = MutableStateFlow<Set<String>>(emptySet())
    val favoriteIds: StateFlow<Set<String>> = _favoriteIds.asStateFlow()

    init {
        // iOS/web parity: auto-load initial results when search opens
        // (browse by default mode without requiring a query).  Initial
        // load uses whatever criteria the store already holds — sticky
        // across navigation so reopening Search after applying a filter
        // shows the filtered set immediately.
        submitSearch()

        // Re-run search whenever the user applies new filters via the
        // FilterScreen.  drop(1) skips the current value so we don't
        // double-fetch on init (submitSearch above already covered it).
        viewModelScope.launch {
            filtersStore.criteria
                .drop(1)
                .distinctUntilChanged()
                .collectLatest { applied ->
                    _uiState.update {
                        it.copy(
                            filterCriteria = applied,
                            // Mirror filter-derived primitives onto the
                            // existing date fields so they show up in the
                            // search summary bar / chips that already read
                            // SearchUiState.startDate / endDate.
                            startDate = applied.checkInEpochDay?.toIsoDate(),
                            endDate = applied.checkOutEpochDay?.toIsoDate(),
                            page0 = 0,
                            items = emptyList(),
                            hasMore = false,
                            phase = SearchPhase.Loading,
                            errorMessage = null,
                        )
                    }
                    loadPage(reset = true)
                }
        }

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

    /**
     * Clear the active bbox and re-run the search unbounded.  Used by
     * the "Results in this area" chip's dismiss affordance so the user
     * has a one-tap escape hatch back to global results without having
     * to retype the query.  All other filters are preserved.
     */
    fun clearMapBounds() {
        if (_uiState.value.mapBounds == null) return
        _uiState.update {
            it.copy(
                mapBounds = null,
                page0 = 0,
                items = emptyList(),
                hasMore = false,
                phase = SearchPhase.Loading,
                errorMessage = null,
            )
        }
        loadPage(reset = true)
    }

    /**
     * Apply Airbnb-style filters from the FilterScreen.  Writes through
     * to the singleton [SearchFiltersStore] so reopening the filter sheet
     * shows the same selection, and so the criteria persist across
     * navigation away from the Search screen.  The store's StateFlow
     * triggers the actual query reload via the collector wired in init.
     */
    fun applyFilters(criteria: FilterCriteria) {
        filtersStore.update(criteria)
    }

    /** Reset all FilterScreen-sourced filters and re-run the query. */
    fun clearFilters() {
        filtersStore.clear()
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

                // FilterScreen criteria takes precedence over the legacy
                // SearchUiState.adultGuests / startDate / endDate fields:
                // when both are set, the structured filter wins.  Guest
                // count combines adults + children (infants / pets are
                // tracked but not currently sent — the backend ignores
                // unknown keys, so they can be added when the API
                // contract grows).
                val filters = current.filterCriteria
                val effectiveStartDate = filters.checkInEpochDay?.toIsoDate()
                    ?: current.startDate?.takeIf { it.isNotBlank() }
                val effectiveEndDate = filters.checkOutEpochDay?.toIsoDate()
                    ?: current.endDate?.takeIf { it.isNotBlank() }
                val filterGuests = (filters.adults + filters.children).takeIf { it > 0 }
                val effectiveGuests = filterGuests
                    ?: current.adultGuests.takeIf { it > 0 }

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
                    startDate = effectiveStartDate,
                    endDate = effectiveEndDate,
                    swLat = current.mapBounds?.swLat,
                    swLng = current.mapBounds?.swLng,
                    neLat = current.mapBounds?.neLat,
                    neLng = current.mapBounds?.neLng,
                    guests = effectiveGuests,
                    bedrooms = filters.bedrooms,
                    beds = filters.beds,
                    bathrooms = filters.bathrooms,
                    instantBook = filters.instantBookOnly.takeIf { it },
                    minPrice = filters.minPrice,
                    maxPrice = filters.maxPrice,
                    amenities = filters.amenities,
                    propertyType = filters.propertyType,
                )

                when (res) {
                    is ApiResult.Success -> {
                        // Backend /v1/poc/listings and /v1/search match the
                        // query with a case-insensitive regex on title OR
                        // description.  A short query like "gym" will
                        // therefore surface any listing whose description
                        // merely mentions the word (e.g. a salon listing
                        // that says "near the gym").  Re-rank on the
                        // client to prefer structured matches (title,
                        // category, location) and drop the description-
                        // only false positives when at least one
                        // structured match exists in this page.
                        val effectiveQuery = (current.whatQuery?.takeIf { it.isNotBlank() }
                            ?: whereQuery.takeIf { it.isNotBlank() })
                            ?.trim()
                        val ranked = if (!effectiveQuery.isNullOrBlank()) {
                            rankByRelevance(res.data.items, effectiveQuery)
                        } else {
                            res.data.items
                        }
                        _uiState.update { s ->
                            val newItems = if (reset) ranked else (s.items + ranked)
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

    private fun Long.toIsoDate(): String =
        LocalDate.ofEpochDay(this).format(ISO_DATE_FORMATTER)

    companion object {
        private val ISO_DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE

        /**
         * Client-side relevance pass over a single page of search
         * results.  Two responsibilities:
         *
         * 1. Drop items that only matched the query on description —
         *    those leak in because the backend regex matches title OR
         *    description indifferently for short queries like "gym"
         *    (PR #457).  If no item matches a structured field
         *    (title / category / location) we fall back to the raw
         *    server page so the user still sees results.
         *
         * 2. Within the kept set, sort by where the query landed
         *    (title.startsWith > title word-boundary > title.contains
         *    > category > location).  The default-sort search path
         *    cannot rely on the backend returning results in
         *    relevance order, so without this boost a `location`
         *    match could appear above a `title.startsWith` match
         *    within the same page.
         *
         * Known limitation: this runs per-page, so a strong match
         * arriving on page 2 is still appended after page 1 in the
         * accumulated UI list — see BUG_TESTING_REPORT_2026-04-28.md
         * §2.3.  Cross-page reordering would require re-ranking the
         * merged list on every page load and is left as follow-up
         * work; the per-page boost is the higher-value half of
         * PR #457 and is preserved here.
         *
         * Package-private + @JvmStatic so the helper can be covered
         * by a plain JVM unit test without constructing the full
         * Hilt graph.
         */
        @JvmStatic
        internal fun rankByRelevance(
            items: List<SearchResultItem>,
            query: String
        ): List<SearchResultItem> {
            val q = query.trim().lowercase()
            if (q.isEmpty()) return items

            val wordBoundary = Regex("\\b${Regex.escape(q)}\\b")

            fun score(item: SearchResultItem): Int {
                val title = item.title.lowercase()
                val category = item.category?.lowercase().orEmpty()
                val location = item.location?.lowercase().orEmpty()
                return when {
                    title.startsWith(q) -> 100
                    wordBoundary.containsMatchIn(title) -> 80
                    title.contains(q) -> 60
                    category.contains(q) -> 40
                    location.contains(q) -> 20
                    else -> 0
                }
            }

            val scored = items.map { it to score(it) }
            val matched = scored.filter { it.second > 0 }
            if (matched.isEmpty()) return items
            return matched.sortedByDescending { it.second }.map { it.first }
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
    val mapBounds: MapBounds? = null,
    /**
     * Currently-applied Airbnb-parity filter criteria sourced from the
     * FilterScreen.  Forwarded to [SearchRepository.search] on each
     * page load.  Re-emitted into this state by the collector that
     * observes [SearchFiltersStore] so the search summary bar / chips
     * can read the active selection without reaching across modules.
     */
    val filterCriteria: FilterCriteria = FilterCriteria(),
) {
    /**
     * Count of distinct filter categories the user has actively constrained.
     * Used to badge the "Filters" entry-point in the search header so the
     * user can tell at a glance how many filters are applied without
     * opening the sheet.
     */
    val activeFilterCount: Int
        get() = with(filterCriteria) {
            var n = 0
            if (checkInEpochDay != null || checkOutEpochDay != null) n++
            if (totalGuests > 0 || infants > 0 || pets > 0) n++
            if (!propertyType.isNullOrBlank()) n++
            if (minPrice != null || maxPrice != null) n++
            if (bedrooms != null) n++
            if (beds != null) n++
            if (bathrooms != null) n++
            if (instantBookOnly) n++
            if (amenities.isNotEmpty()) n++
            n
        }
}

enum class SearchPhase {
    Idle,
    Loading,
    LoadingMore,
    Success,
    Empty,
    Error
}
