package com.shourov.apps.pacedream.feature.homefeed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shourov.apps.pacedream.core.network.api.ApiResult
import com.shourov.apps.pacedream.core.network.api.ApiError
import com.shourov.apps.pacedream.core.network.auth.AuthSession
import com.shourov.apps.pacedream.core.network.auth.AuthState
import com.shourov.apps.pacedream.feature.wishlist.data.FavoritesCache
import com.shourov.apps.pacedream.feature.wishlist.data.WishlistRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import timber.log.Timber
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
    private val wishlistRepository: WishlistRepository,
    private val favoritesCache: FavoritesCache
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
        // Load cached favorites immediately so hearts render before network response
        _favoriteIds.value = favoritesCache.getCachedFavoriteIds()

        loadAll()

        // Keep filteredState in sync whenever raw state or selected category changes
        viewModelScope.launch {
            try {
                kotlinx.coroutines.flow.combine(_state, _selectedCategory) { raw, cat ->
                    applyFilter(raw, cat)
                }.collectLatest { filtered ->
                    _filteredState.value = filtered
                }
            } catch (_: Exception) { /* filter sync failed; UI shows unfiltered state */ }
        }

        viewModelScope.launch {
            try {
                authSession.authState.collectLatest { st ->
                    if (st == AuthState.Unauthenticated) {
                        _favoriteIds.value = emptySet()
                        favoritesCache.saveFavoriteIds(emptySet())
                    } else {
                        syncPendingToggles()
                        refreshFavorites()
                    }
                }
            } catch (_: Exception) { /* auth observation failed */ }
        }

        viewModelScope.launch {
            try {
                wishlistRepository.changes.collectLatest {
                    if (authSession.authState.value != AuthState.Unauthenticated) {
                        // Let the server reflect the toggle before GET; avoids stale wishlist wiping UI.
                        delay(400)
                        refreshFavorites()
                    }
                }
            } catch (_: Exception) { /* wishlist observation failed */ }
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
            "fitness", "learning", "creative", "other_service",
        )

        /** Service shareCategory values (uppercase) from backend taxonomy. */
        private val SERVICE_SHARE_CATEGORIES = setOf(
            "HOME_HELP", "MOVING_HELP", "CLEANING_ORGANIZING", "EVERYDAY_HELP",
            "FITNESS", "LEARNING", "CREATIVE", "OTHER_SERVICE",
        )

        private fun isServiceListing(card: HomeCard): Boolean {
            if (card.shareCategory?.uppercase() in SERVICE_SHARE_CATEGORIES) return true
            if (card.subCategory?.lowercase() in SERVICE_SUBCATEGORY_IDS) return true
            return false
        }

        /** Map UI category chip names to backend subCategory keywords (iOS parity). */
        private val CATEGORY_TO_KEYWORD = mapOf(
            "Restroom" to "restroom",
            "Nap Pod" to "nap_pod",
            "Meeting Room" to "meeting_room",
            "Gym" to "gym",
            "Short Stay" to "short_stay",
            "WIFI" to "wifi",
            "Parking" to "parking",
            "Storage Space" to "storage_space",
        )
    }

    fun refresh() {
        loadAll(refresh = true)
    }

    suspend fun toggleFavorite(listingId: String): ApiResult<Boolean> {
        val wasFavorited = _favoriteIds.value.contains(listingId)
        // Optimistic UI update — persist to cache immediately
        _favoriteIds.value = if (wasFavorited) _favoriteIds.value - listingId else _favoriteIds.value + listingId
        favoritesCache.saveFavoriteIds(_favoriteIds.value)

        val res = if (wasFavorited) {
            wishlistRepository.removeFromWishlist(propertyId = listingId)
        } else {
            wishlistRepository.addToWishlist(propertyId = listingId)
        }

        return when (res) {
            is ApiResult.Success -> {
                // Clear any pending toggle for this item since it succeeded
                favoritesCache.removePendingToggle(listingId)
                ApiResult.Success(!wasFavorited)
            }
            is ApiResult.Failure -> {
                if (res.error is ApiError.NetworkError || res.error is ApiError.Timeout) {
                    // Network error: keep the optimistic update, queue for retry
                    favoritesCache.addPendingToggle(listingId)
                    Timber.w("Favorite toggle queued for retry (offline): $listingId")
                    // Return the failure so the UI can show an offline indicator
                    res
                } else {
                    // Non-network error (auth, server, etc.): roll back
                    _favoriteIds.value = if (wasFavorited) _favoriteIds.value + listingId else _favoriteIds.value - listingId
                    favoritesCache.saveFavoriteIds(_favoriteIds.value)
                    res
                }
            }
        }
    }

    fun refreshFavorites() {
        viewModelScope.launch {
            try {
                when (val res = wishlistRepository.getWishlist()) {
                    is ApiResult.Success -> {
                        val serverIds = res.data.map { it.listingId.ifBlank { it.id } }.toSet()
                        _favoriteIds.value = serverIds
                        favoritesCache.saveFavoriteIds(serverIds)
                    }
                    is ApiResult.Failure -> {
                        if (res.error is ApiError.Unauthorized) {
                            _favoriteIds.value = emptySet()
                            favoritesCache.saveFavoriteIds(emptySet())
                        }
                        // On network error: keep cached favorites (already loaded in init)
                    }
                }
            } catch (_: Exception) { /* keep last known favorites from cache */ }
        }
    }

    /**
     * Replay any pending toggle operations that failed due to network errors.
     * Called when network becomes available (auth state changes to authenticated).
     */
    private suspend fun syncPendingToggles() {
        val pending = favoritesCache.getPendingToggles()
        if (pending.isEmpty()) return

        Timber.d("Syncing ${pending.size} pending favorite toggles")
        for (propertyId in pending) {
            try {
                val isFavorited = _favoriteIds.value.contains(propertyId)
                val res = if (isFavorited) {
                    wishlistRepository.addToWishlist(propertyId = propertyId)
                } else {
                    wishlistRepository.removeFromWishlist(propertyId = propertyId)
                }
                if (res is ApiResult.Success) {
                    favoritesCache.removePendingToggle(propertyId)
                }
                // If still failing, leave it in queue for next sync
            } catch (e: Exception) {
                Timber.w(e, "Failed to sync pending toggle for $propertyId")
            }
        }
    }

    private var loadJob: kotlinx.coroutines.Job? = null

    private fun loadAll(refresh: Boolean = false) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            try {
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
            } catch (_: kotlinx.coroutines.CancellationException) {
                throw kotlinx.coroutines.CancellationException("loadAll cancelled")
            } catch (_: Exception) {
                _state.update { it.copy(globalErrorMessage = "Something went wrong. Pull to refresh.") }
            } finally {
                _state.update { it.copy(isRefreshing = false) }
            }
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
                                !isServiceListing(it)
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
                                    isServiceListing(it)
                                }
                                HomeSectionKey.SPACES -> res.data.filter {
                                    !isServiceListing(it)
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

