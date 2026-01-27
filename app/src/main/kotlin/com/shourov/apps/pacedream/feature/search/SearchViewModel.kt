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
            when (val res = wishlistRepository.getWishlist()) {
                is ApiResult.Success -> {
                    _favoriteIds.value = res.data.map { it.listingId.ifBlank { it.id } }.toSet()
                }
                is ApiResult.Failure -> {
                    // Non-blocking: keep last known favorites.
                    if (res.error is ApiError.Unauthorized) _favoriteIds.value = emptySet()
                }
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
        endDate: String? = null
    ) {
        _uiState.update { current ->
            current.copy(
                shareType = shareType ?: current.shareType,
                whatQuery = whatQuery ?: current.whatQuery,
                city = city ?: current.city,
                startDate = startDate ?: current.startDate,
                endDate = endDate ?: current.endDate,
                errorMessage = null
            )
        }
    }

    fun submitSearch() {
        val q = uiState.value.query.trim()
        _uiState.update {
            it.copy(
                phase = SearchPhase.Loading,
                page0 = 0,
                items = emptyList(),
                hasMore = false,
                errorMessage = null
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
        viewModelScope.launch {
            val current = uiState.value
            val q = current.query.trim()

            if (q.isBlank()) {
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

            val res = repo.search(
                q = q,
                city = current.city?.takeIf { it.isNotBlank() },
                category = current.category?.takeIf { it.isNotBlank() },
                page0 = page,
                perPage = current.perPage,
                sort = current.sort,
                shareType = current.shareType?.takeIf { it.isNotBlank() },
                whatQuery = current.whatQuery?.takeIf { it.isNotBlank() },
                startDate = current.startDate?.takeIf { it.isNotBlank() },
                endDate = current.endDate?.takeIf { it.isNotBlank() }
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
        }
    }

    private fun fetchAutocompleteDebounced(q: String) {
        autocompleteJob?.cancel()
        if (q.trim().length < 2) {
            _uiState.update { it.copy(suggestions = emptyList()) }
            return
        }

        autocompleteJob = viewModelScope.launch {
            delay(250)
            val res = repo.autocompleteWhere(q.trim())
            when (res) {
                is ApiResult.Success -> _uiState.update { it.copy(suggestions = res.data) }
                is ApiResult.Failure -> {
                    // Non-blocking; keep suggestions empty on failure.
                    _uiState.update { it.copy(suggestions = emptyList()) }
                }
            }
        }
    }
}

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
    val shareType: String? = null, // USE, BORROW, or SPLIT
    val whatQuery: String? = null, // Keywords search
    val startDate: String? = null, // ISO date string
    val endDate: String? = null // ISO date string
)

enum class SearchPhase {
    Idle,
    Loading,
    LoadingMore,
    Success,
    Empty,
    Error
}

