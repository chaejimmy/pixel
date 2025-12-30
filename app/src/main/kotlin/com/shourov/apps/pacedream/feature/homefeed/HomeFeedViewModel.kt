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

    val authState: StateFlow<AuthState> = authSession.authState

    private val _favoriteIds = MutableStateFlow<Set<String>>(emptySet())
    val favoriteIds: StateFlow<Set<String>> = _favoriteIds.asStateFlow()

    init {
        loadAll()

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

    private fun loadAll(refresh: Boolean = false) {
        viewModelScope.launch {
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
            val gearDeferred = async { loadSection(HomeSectionKey.GEAR) }
            val splitDeferred = async { loadSection(HomeSectionKey.SPLIT) }

            hourlyDeferred.await()
            gearDeferred.await()
            splitDeferred.await()

            _state.update { it.copy(isRefreshing = false) }
        }
    }

    private suspend fun loadHourly() {
        val res = repo.getCuratedHourly()
        _state.update { s ->
            s.copy(
                headerTitle = headerTitle(),
                sections = s.sections.map { section ->
                    if (section.key != HomeSectionKey.HOURLY) section
                    else when (res) {
                        is ApiResult.Success -> section.copy(items = res.data, isLoading = false, errorMessage = null)
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
                headerTitle = headerTitle(),
                sections = s.sections.map { section ->
                    if (section.key != key) section
                    else when (res) {
                        is ApiResult.Success -> section.copy(items = res.data, isLoading = false, errorMessage = null)
                        is ApiResult.Failure -> section.copy(
                            isLoading = false,
                            errorMessage = res.error.message ?: "Failed to load ${key.displayTitle.lowercase()}"
                        )
                    }
                }
            )
        }
    }

    private fun headerTitle(): String {
        val user = authSession.currentUser.value
        return if (user != null) "Find your perfect stay, ${user.displayName}"
        else "Find your perfect stay"
    }
}

