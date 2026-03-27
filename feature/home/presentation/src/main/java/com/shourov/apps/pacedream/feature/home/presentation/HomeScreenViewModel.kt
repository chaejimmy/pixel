/*
 * Copyright 2024 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.shourov.apps.pacedream.feature.home.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shourov.apps.pacedream.core.common.result.Result.Error
import com.shourov.apps.pacedream.core.common.result.Result.Loading
import com.shourov.apps.pacedream.core.common.result.Result.Success
import com.shourov.apps.pacedream.core.common.result.asResult
import com.shourov.apps.pacedream.core.network.api.ApiResult
import com.shourov.apps.pacedream.feature.home.domain.repository.HomeRepository
import com.shourov.apps.pacedream.feature.home.presentation.HomeScreenEvent.GetRentedGears
import com.shourov.apps.pacedream.feature.home.presentation.HomeScreenEvent.GetSplitStays
import com.shourov.apps.pacedream.feature.home.presentation.HomeScreenEvent.GetTimeBasedRooms
import com.shourov.apps.pacedream.feature.home.presentation.HomeScreenEvent.LoadAllSections
import com.shourov.apps.pacedream.feature.home.presentation.HomeScreenEvent.NavigateToSection
import com.shourov.apps.pacedream.feature.home.presentation.HomeScreenEvent.RefreshAll
import com.shourov.apps.pacedream.feature.home.presentation.HomeScreenEvent.ToggleFavorite
import com.shourov.apps.pacedream.feature.wishlist.data.FavoritesCache
import com.shourov.apps.pacedream.feature.wishlist.data.WishlistRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import timber.log.Timber
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeScreenViewModel @Inject constructor(
    private val homeRepository: HomeRepository,
    private val wishlistRepository: WishlistRepository,
    private val favoritesCache: FavoritesCache,
) : ViewModel() {

    private var _homeScreenRoomsState: MutableStateFlow<HomeScreenRoomsState> =
        MutableStateFlow(HomeScreenRoomsState())
    val homeScreenRoomsState: StateFlow<HomeScreenRoomsState> =
        _homeScreenRoomsState.asStateFlow()

    private var _homeScreenRentedGearsState: MutableStateFlow<HomeScreenRentedGearsState> =
        MutableStateFlow(HomeScreenRentedGearsState())
    val homeScreenRentedGearsState: StateFlow<HomeScreenRentedGearsState> =
        _homeScreenRentedGearsState.asStateFlow()

    private var _homeScreenSplitStaysState: MutableStateFlow<HomeScreenSplitStaysState> =
        MutableStateFlow(HomeScreenSplitStaysState())
    val homeScreenSplitStaysState: StateFlow<HomeScreenSplitStaysState> =
        _homeScreenSplitStaysState.asStateFlow()

    private var _isRefreshing: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    // Favorite/wishlist state — set of property IDs that are favorited
    private val _favoriteIds = MutableStateFlow<Set<String>>(emptySet())
    val favoriteIds: StateFlow<Set<String>> = _favoriteIds.asStateFlow()

    // Navigation events for View All
    private val _navigationEvent = MutableSharedFlow<NavigationEvent>()
    val navigationEvent: SharedFlow<NavigationEvent> = _navigationEvent.asSharedFlow()

    // Track current filter types for refresh
    private var currentRoomType: String = "room"
    private var currentGearType: String = "tech_gear"

    init {
        // Load cached favorites immediately so hearts render before network response
        _favoriteIds.value = favoritesCache.getCachedFavoriteIds()
        loadFavoriteIds()
    }

    fun onEvent(event: HomeScreenEvent) {
        viewModelScope.launch(Dispatchers.IO) {
            when (event) {
                is GetRentedGears -> {
                    currentGearType = event.type
                    loadRentedGears(event.type)
                }

                is GetTimeBasedRooms -> {
                    currentRoomType = event.type
                    loadTimeBasedRooms(event.type)
                }

                is GetSplitStays -> {
                    loadSplitStays()
                }

                is LoadAllSections -> {
                    currentRoomType = event.roomType
                    currentGearType = event.gearType
                    loadAllSectionsInParallel(event.roomType, event.gearType)
                }

                is RefreshAll -> {
                    refreshAllSections()
                }

                is ToggleFavorite -> {
                    toggleFavorite(event.propertyId)
                }

                is NavigateToSection -> {
                    _navigationEvent.emit(NavigationEvent.ToSectionList(event.section))
                }
            }
        }
    }

    /**
     * Load all sections in parallel for initial load - matches iOS parallel loading behavior
     */
    private suspend fun loadAllSectionsInParallel(roomType: String, gearType: String) {
        viewModelScope.launch(Dispatchers.IO) {
            // Launch all three sections in parallel using async
            val roomsDeferred = async { loadTimeBasedRooms(roomType) }
            val gearsDeferred = async { loadRentedGears(gearType) }
            val splitStaysDeferred = async { loadSplitStays() }

            // Wait for all to complete (they each handle their own state updates)
            roomsDeferred.await()
            gearsDeferred.await()
            splitStaysDeferred.await()
        }
    }

    /**
     * Refresh all sections - for pull-to-refresh
     */
    private suspend fun refreshAllSections() {
        _isRefreshing.value = true
        
        // Reset errors before refresh
        _homeScreenRoomsState.update { it.copy(error = null) }
        _homeScreenRentedGearsState.update { it.copy(error = null) }
        _homeScreenSplitStaysState.update { it.copy(error = null) }

        viewModelScope.launch(Dispatchers.IO) {
            val roomsDeferred = async { loadTimeBasedRooms(currentRoomType) }
            val gearsDeferred = async { loadRentedGears(currentGearType) }
            val splitStaysDeferred = async { loadSplitStays() }

            roomsDeferred.await()
            gearsDeferred.await()
            splitStaysDeferred.await()

            _isRefreshing.value = false
        }
    }

    private suspend fun loadTimeBasedRooms(type: String) {
        homeRepository.getTimeBasedRooms(type).asResult()
            .collectLatest { result ->
                when (result) {
                    is Error -> _homeScreenRoomsState.update {
                        it.copy(
                            loading = false,
                            error = result.exception.message ?: "Failed to load properties",
                        )
                    }

                    Loading -> _homeScreenRoomsState.update { it.copy(loading = true, error = null) }
                    is Success -> _homeScreenRoomsState.update {
                        it.copy(
                            loading = false,
                            rooms = result.data,
                            error = null,
                        )
                    }
                }
            }
    }

    private suspend fun loadRentedGears(type: String) {
        homeRepository.getRentedGears(type).asResult().collectLatest { result ->
            when (result) {
                is Error -> _homeScreenRentedGearsState.update {
                    it.copy(
                        loading = false,
                        error = result.exception.message ?: "Failed to load gear",
                    )
                }

                Loading -> _homeScreenRentedGearsState.update { it.copy(loading = true, error = null) }
                is Success -> _homeScreenRentedGearsState.update {
                    it.copy(
                        loading = false,
                        rentedGears = result.data,
                        error = null,
                    )
                }
            }
        }
    }

    private suspend fun loadSplitStays() {
        homeRepository.getSplitStays().asResult().collectLatest { result ->
            when (result) {
                is Error -> _homeScreenSplitStaysState.update {
                    it.copy(
                        loading = false,
                        error = result.exception.message ?: "Failed to load split stays",
                    )
                }

                Loading -> _homeScreenSplitStaysState.update { it.copy(loading = true, error = null) }
                is Success -> _homeScreenSplitStaysState.update {
                    it.copy(
                        loading = false,
                        splitStays = result.data,
                        error = null,
                    )
                }
            }
        }
    }

    /**
     * Load favorite IDs from the wishlist so hearts render correctly on home cards.
     */
    private fun loadFavoriteIds() {
        viewModelScope.launch(Dispatchers.IO) {
            when (val result = wishlistRepository.getWishlist()) {
                is ApiResult.Success -> {
                    val ids = result.data.mapNotNull { item ->
                        item.id.takeIf { it.isNotEmpty() } ?: item.listingId.takeIf { it.isNotEmpty() }
                    }.toSet()
                    _favoriteIds.value = ids
                    favoritesCache.saveFavoriteIds(ids)
                }
                is ApiResult.Failure -> {
                    Timber.w("Failed to load wishlist for home favorites, using cached data")
                    // Keep cached favorites (already loaded in init)
                }
            }
        }
    }

    /**
     * Toggle favorite with optimistic UI update.
     * On network errors, keeps the optimistic state and queues for retry.
     */
    private suspend fun toggleFavorite(propertyId: String) {
        val wasFavorited = _favoriteIds.value.contains(propertyId)

        // Optimistic update — persist to cache immediately
        _favoriteIds.update { current ->
            if (wasFavorited) current - propertyId else current + propertyId
        }
        favoritesCache.saveFavoriteIds(_favoriteIds.value)

        // Call API
        val result = wishlistRepository.toggleWishlistItem(itemId = propertyId)
        when (result) {
            is ApiResult.Success -> {
                // Sync with server response — server is the source of truth
                _favoriteIds.update { current ->
                    if (result.data.liked) current + propertyId else current - propertyId
                }
                favoritesCache.saveFavoriteIds(_favoriteIds.value)
                favoritesCache.removePendingToggle(propertyId)
            }
            is ApiResult.Failure -> {
                if (result.error is com.shourov.apps.pacedream.core.network.api.ApiError.NetworkError ||
                    result.error is com.shourov.apps.pacedream.core.network.api.ApiError.Timeout) {
                    // Network error: keep optimistic update, queue for retry
                    favoritesCache.addPendingToggle(propertyId)
                    Timber.w("Favorite toggle queued for retry (offline): $propertyId")
                } else {
                    // Non-network error: revert optimistic update
                    Timber.w("Failed to toggle favorite for $propertyId, reverting")
                    _favoriteIds.update { current ->
                        if (wasFavorited) current + propertyId else current - propertyId
                    }
                    favoritesCache.saveFavoriteIds(_favoriteIds.value)
                }
            }
        }
    }
}

/**
 * Navigation events for the Home screen
 */
sealed class NavigationEvent {
    data class ToSectionList(val section: String) : NavigationEvent()
    data class ToPropertyDetail(val propertyId: String) : NavigationEvent()
    data object ToSearch : NavigationEvent()
    data object ToNotifications : NavigationEvent()
}