package com.shourov.apps.pacedream.feature.propertydetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shourov.apps.pacedream.core.network.api.ApiError
import com.shourov.apps.pacedream.core.network.api.ApiResult
import com.shourov.apps.pacedream.core.network.auth.AuthSession
import com.shourov.apps.pacedream.core.network.auth.AuthState
import com.shourov.apps.pacedream.feature.wishlist.data.WishlistRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PropertyDetailViewModel @Inject constructor(
    private val authSession: AuthSession,
    private val wishlistRepository: WishlistRepository,
    private val detailRepository: PropertyDetailRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    val authState: StateFlow<AuthState> = authSession.authState

    private val listingId: String = savedStateHandle.get<String>("propertyId").orEmpty()

    private val _favoriteIds = MutableStateFlow<Set<String>>(emptySet())
    val favoriteIds: StateFlow<Set<String>> = _favoriteIds.asStateFlow()

    private val _uiState = MutableStateFlow(PropertyDetailUiState(isLoading = true))
    val uiState: StateFlow<PropertyDetailUiState> = _uiState.asStateFlow()

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
                if (authSession.authState.value != AuthState.Unauthenticated) refreshFavorites()
            }
        }

        // Load listing detail
        refreshDetail()
    }

    fun isFavorited(): Boolean = listingId.isNotBlank() && _favoriteIds.value.contains(listingId)

    fun refreshFavorites() {
        viewModelScope.launch {
            when (val res = wishlistRepository.getWishlist()) {
                is ApiResult.Success -> _favoriteIds.value =
                    res.data.map { it.listingId.ifBlank { it.id } }.toSet()
                is ApiResult.Failure -> {
                    if (res.error is ApiError.Unauthorized) _favoriteIds.value = emptySet()
                }
            }
        }
    }

    fun refreshDetail() {
        if (listingId.isBlank()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = it.detail == null, errorMessage = null, inlineErrorMessage = null) }
            when (val res = detailRepository.getListingDetail(listingId)) {
                is ApiResult.Success -> {
                    _uiState.update { it.copy(isLoading = false, detail = res.data, errorMessage = null, inlineErrorMessage = null) }
                }
                is ApiResult.Failure -> {
                    val hasCached = _uiState.value.detail != null
                    if (hasCached) {
                        _uiState.update { it.copy(isLoading = false, inlineErrorMessage = res.error.message ?: "Failed to load") }
                    } else {
                        _uiState.update { it.copy(isLoading = false, errorMessage = res.error.message ?: "Failed to load") }
                    }
                }
            }
        }
    }

    suspend fun toggleFavorite(listingId: String): ApiResult<Boolean> {
        val wasFavorited = _favoriteIds.value.contains(listingId)
        _favoriteIds.value = if (wasFavorited) _favoriteIds.value - listingId else _favoriteIds.value + listingId

        val res = if (wasFavorited) {
            wishlistRepository.removeFromWishlist(propertyId = listingId)
        } else {
            wishlistRepository.addToWishlist(propertyId = listingId)
        }

        return when (res) {
            is ApiResult.Success -> ApiResult.Success(!wasFavorited)
            is ApiResult.Failure -> {
                // rollback
                _favoriteIds.value = if (wasFavorited) _favoriteIds.value + listingId else _favoriteIds.value - listingId
                res
            }
        }
    }
}

