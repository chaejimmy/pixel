package com.shourov.apps.pacedream.feature.homefeed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shourov.apps.pacedream.core.network.api.ApiResult
import com.shourov.apps.pacedream.core.network.api.ApiError
import com.shourov.apps.pacedream.core.network.auth.AuthSession
import com.shourov.apps.pacedream.core.network.auth.AuthState
import com.shourov.apps.pacedream.feature.wishlist.data.WishlistRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeFeedViewModel @Inject constructor(
    private val repo: HomeFeedRepository,
    private val authSession: AuthSession,
    private val wishlistRepository: WishlistRepository
) : ViewModel() {

    private val _state = MutableStateFlow(HomeFeedState())
    val state: StateFlow<HomeFeedState> = _state.asStateFlow()

    /** Currently selected category filter (e.g. "All", "Restroom", "Nap Pod"). */
    private val _selectedCategory = MutableStateFlow("All")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    /**
     * Derived view of sections, filtered by the selected category chip.
     * Matches iOS/web behaviour: "All" shows everything; any other chip filters
     * items whose subCategory (or title, as fallback) contains the category keyword.
     */
    private val _filteredState = MutableStateFlow(HomeFeedState())
    val filteredState: StateFlow<HomeFeedState> = _filteredState.asStateFlow()

    val authState: StateFlow<AuthState> = authSession.authState

    private val _favoriteIds = MutableStateFlow<Set<String>>(emptySet())
    val favoriteIds: StateFlow<Set<String>> = _favoriteIds.asStateFlow()

    init {
        loadAll()

        // Keep filteredState in sync whenever raw state or selected category changes
        viewModelScope.launch {
            kotlinx.coroutines.flow.combine(_state, _selectedCategory) { raw, cat ->
                applyFilter(raw, cat)
            }.collectLatest { filtered ->
                _filteredState.value = filtered
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
                    // Let the server reflect the toggle before GET; avoids stale wishlist wiping UI.
                    delay(400)
                    refreshFavorites()
                }
            }
        }
    }

    fun selectCategory(category: String) {
        _selectedCategory.value = category
    }

    /**
     * Filter sections by the selected category chip.
     * "All" returns unfiltered data; other chips match against subCategory or title.
     */
    private fun applyFilter(raw: HomeFeedState, category: String): HomeFeedState {
        if (category == "All") return raw
        val keyword = CATEGORY_TO_KEYWORD[category] ?: category.lowercase().replace(" ", "_")
        return raw.copy(
            sections = raw.sections.map { section ->
                section.copy(
                    items = section.items.filter { card ->
                        val sub = card.subCategory?.lowercase() ?: ""
                        val title = card.title.lowercase()
                        sub.contains(keyword) || title.contains(keyword)
                    }
                )
            }
        )
    }

    companion object {
        /** Subcategory IDs that identify service listings. */
        private val SERVICE_SUBCATEGORY_IDS = setOf(
            "home_help", "moving_help", "cleaning_organizing", "everyday_help",
            "fitness", "learning", "creative",
        )

        /** Map UI category chip names to backend subCategory keywords (iOS/web parity). */
        private val CATEGORY_TO_KEYWORD = mapOf(
            "Entire Home" to "entire_home",
            "Private Room" to "private_room",
            "Restroom" to "restroom",
            "Nap Pod" to "nap_pod",
            "Meeting Room" to "meeting_room",
            "Workspace" to "workspace",
            "EV Parking" to "ev_parking",
            "Study Room" to "study_room",
            "Short Stay" to "short_stay",
            "Apartment" to "apartment",
            "Parking" to "parking",
            "Luxury Room" to "luxury_room",
            "Storage" to "storage",
        )
    }

    fun refresh() {
        loadAll(refresh = true)
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
                    // Non-blocking: keep last known favorites (no silent fake fallback).
                    if (res.error is ApiError.Unauthorized) _favoriteIds.value = emptySet()
                }
            }
        }
    }

    private var loadJob: kotlinx.coroutines.Job? = null

    private fun loadAll(refresh: Boolean = false) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            if (refresh) _state.update { it.copy(isRefreshing = true, globalErrorMessage = null) }

            // Set all sections to loading if initial load
            if (!refresh) {
                _state.update { s ->
                    s.copy(
                        sections = s.sections.map { it.copy(isLoading = true, errorMessage = null) },
                        globalErrorMessage = null
                    )
                }
            }

            val hourlyDeferred = async { loadHourly() }
            val gearDeferred = async { loadSection(HomeSectionKey.ITEMS) }
            val servicesDeferred = async { loadSection(HomeSectionKey.SERVICES) }

            hourlyDeferred.await()
            gearDeferred.await()
            servicesDeferred.await()

            _state.update { it.copy(isRefreshing = false) }
        }
    }

    private suspend fun loadHourly() {
        val res = repo.getCuratedHourly()
        _state.update { s ->
            s.copy(
                sections = s.sections.map { section ->
                    if (section.key != HomeSectionKey.SPACES) section
                    else when (res) {
                        is ApiResult.Success -> {
                            // Exclude service listings from Spaces section
                            val spacesOnly = res.data.filter {
                                it.subCategory?.lowercase() !in SERVICE_SUBCATEGORY_IDS
                            }
                            section.copy(items = spacesOnly, isLoading = false, errorMessage = null)
                        }
                        is ApiResult.Failure -> {
                            // Keep last good state if present
                            val keep = section.items.isNotEmpty()
                            section.copy(
                                isLoading = false,
                                errorMessage = res.error.message ?: "Failed to load hourly spaces"
                            ).let { if (keep) it else it }
                        }
                    }
                }
            )
        }
    }

    private suspend fun loadSection(key: HomeSectionKey) {
        val shareType = key.shareType ?: return
        val res = repo.getListingsShareTypePage(shareType = shareType, page1 = 1, limit = 24)
        _state.update { s ->
            s.copy(
                sections = s.sections.map { section ->
                    if (section.key != key) section
                    else when (res) {
                        is ApiResult.Success -> {
                            // Client-side resource type filtering: services and spaces both
                            // use shareType=SHARE, so we separate them by subcategory.
                            val filtered = when (key) {
                                HomeSectionKey.SERVICES -> res.data.filter {
                                    it.subCategory?.lowercase() in SERVICE_SUBCATEGORY_IDS
                                }
                                HomeSectionKey.SPACES -> res.data.filter {
                                    it.subCategory?.lowercase() !in SERVICE_SUBCATEGORY_IDS
                                }
                                else -> res.data
                            }
                            section.copy(items = filtered, isLoading = false, errorMessage = null)
                        }
                        is ApiResult.Failure -> section.copy(
                            isLoading = false,
                            errorMessage = res.error.message ?: "Failed to load ${key.displayTitle.lowercase()}"
                        )
                    }
                }
            )
        }
    }
}

