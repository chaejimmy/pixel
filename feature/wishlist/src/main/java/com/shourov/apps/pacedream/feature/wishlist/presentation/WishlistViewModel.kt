package com.shourov.apps.pacedream.feature.wishlist.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shourov.apps.pacedream.core.network.api.ApiError
import com.shourov.apps.pacedream.core.network.api.ApiResult
import com.shourov.apps.pacedream.core.network.auth.AuthSession
import com.shourov.apps.pacedream.core.network.auth.AuthState
import com.shourov.apps.pacedream.feature.wishlist.data.WishlistRepository
import com.shourov.apps.pacedream.feature.wishlist.model.WishlistEvent
import com.shourov.apps.pacedream.feature.wishlist.model.WishlistFilter
import com.shourov.apps.pacedream.feature.wishlist.model.WishlistItem
import com.shourov.apps.pacedream.feature.wishlist.model.WishlistItemType
import com.shourov.apps.pacedream.feature.wishlist.model.WishlistNavigation
import com.shourov.apps.pacedream.feature.wishlist.model.WishlistUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel for Wishlist screen with optimistic remove behavior
 * 
 * Optimistic remove:
 * - Remove item immediately from grid
 * - Call toggle endpoint
 * - If API fails OR returns liked=true unexpectedly, restore item and show toast
 */
@HiltViewModel
class WishlistViewModel @Inject constructor(
    private val wishlistRepository: WishlistRepository,
    private val authSession: AuthSession
) : ViewModel() {
    
    private val _uiState = MutableStateFlow<WishlistUiState>(WishlistUiState.Loading)
    val uiState: StateFlow<WishlistUiState> = _uiState.asStateFlow()
    
    private val _navigation = Channel<WishlistNavigation>(Channel.BUFFERED)
    val navigation = _navigation.receiveAsFlow()
    
    private val _toastMessage = Channel<String>(Channel.BUFFERED)
    val toastMessage = _toastMessage.receiveAsFlow()
    
    // Store full items list for optimistic updates
    private var fullItemsList = mutableListOf<WishlistItem>()
    private var currentFilter = WishlistFilter.ALL
    
    init {
        checkAuthAndLoad()
    }
    
    /**
     * Handle UI events
     */
    fun onEvent(event: WishlistEvent) {
        when (event) {
            is WishlistEvent.Refresh -> refresh()
            is WishlistEvent.FilterSelected -> onFilterSelected(event.filter)
            is WishlistEvent.RemoveItem -> onRemoveItem(event.item)
            is WishlistEvent.ItemClicked -> onItemClicked(event.item)
            is WishlistEvent.BookNowClicked -> onBookNowClicked(event.item)
        }
    }
    
    /**
     * Check authentication and load wishlist
     */
    private fun checkAuthAndLoad() {
        viewModelScope.launch {
            if (authSession.authState.value == AuthState.Unauthenticated) {
                _uiState.value = WishlistUiState.RequiresAuth
                return@launch
            }
            
            loadWishlist()
        }
    }
    
    /**
     * Refresh wishlist
     */
    private fun refresh() {
        viewModelScope.launch {
            if (authSession.authState.value == AuthState.Unauthenticated) {
                _uiState.value = WishlistUiState.RequiresAuth
                return@launch
            }
            
            loadWishlist()
        }
    }
    
    /**
     * Load wishlist from API
     */
    private suspend fun loadWishlist() {
        _uiState.value = WishlistUiState.Loading
        
        when (val result = wishlistRepository.getWishlist()) {
            is ApiResult.Success -> {
                fullItemsList = result.data.toMutableList()
                updateUiWithCurrentFilter()
            }
            is ApiResult.Failure -> {
                when (result.error) {
                    is ApiError.Unauthorized -> {
                        _uiState.value = WishlistUiState.RequiresAuth
                    }
                    else -> {
                        _uiState.value = WishlistUiState.Error(
                            result.error.message ?: "Failed to load wishlist"
                        )
                    }
                }
            }
        }
    }
    
    /**
     * Update UI state with current filter
     */
    private fun updateUiWithCurrentFilter() {
        if (fullItemsList.isEmpty()) {
            _uiState.value = WishlistUiState.Empty
        } else {
            _uiState.value = WishlistUiState.Success(
                items = fullItemsList.toList(),
                selectedFilter = currentFilter
            )
        }
    }
    
    /**
     * Handle filter selection
     */
    private fun onFilterSelected(filter: WishlistFilter) {
        currentFilter = filter
        updateUiWithCurrentFilter()
    }
    
    /**
     * Handle item removal with optimistic update
     * 
     * Steps:
     * 1. Remove item immediately from grid
     * 2. Call toggle endpoint
     * 3. If API fails OR returns liked=true unexpectedly, restore item and show toast
     */
    private fun onRemoveItem(item: WishlistItem) {
        viewModelScope.launch {
            // Store original list for potential restoration
            val originalList = fullItemsList.toList()
            val itemIndex = fullItemsList.indexOfFirst { it.id == item.id }
            
            if (itemIndex == -1) {
                Timber.w("Item not found in list: ${item.id}")
                return@launch
            }
            
            // Step 1: Optimistically remove item from UI
            fullItemsList.removeAt(itemIndex)
            updateUiWithCurrentFilter()
            
            // Step 2: Call API to toggle (remove) the item
            val result = wishlistRepository.removeFromWishlist(
                propertyId = item.listingId.ifBlank { item.id }
            )
            
            // Step 3: Handle result
            when (result) {
                is ApiResult.Success -> Timber.d("Item successfully removed from wishlist")
                is ApiResult.Failure -> {
                    // API failed, restore item
                    Timber.e("Failed to remove item: ${result.error.message}")
                    restoreItem(originalList, itemIndex, item)
                    _toastMessage.send("Failed to remove item. Please try again.")
                }
            }
        }
    }
    
    /**
     * Restore item to list after failed removal
     */
    private fun restoreItem(originalList: List<WishlistItem>, index: Int, item: WishlistItem) {
        // Restore the full list to original state
        fullItemsList.clear()
        fullItemsList.addAll(originalList)
        updateUiWithCurrentFilter()
    }
    
    /**
     * Handle item click - navigate to detail screen
     */
    private fun onItemClicked(item: WishlistItem) {
        viewModelScope.launch {
            val navigation = routeToDetailScreen(item)
            _navigation.send(navigation)
        }
    }
    
    /**
     * Handle "Book Now" click - route based on item type
     * 
     * Routing rules:
     * - time-based → TimeBasedDetailScreen(itemId = listingId ?? _id)
     * - hourly-gear → HourlyGearDetailScreen(gearId = listingId ?? _id)
     * - room-stay → TimeBasedDetailScreen (safe fallback)
     * - fallback → TimeBasedDetailScreen
     */
    private fun onBookNowClicked(item: WishlistItem) {
        viewModelScope.launch {
            val navigation = routeToDetailScreen(item)
            _navigation.send(navigation)
        }
    }
    
    /**
     * Route to appropriate detail screen based on item type
     */
    private fun routeToDetailScreen(item: WishlistItem): WishlistNavigation {
        val itemId = item.listingId.ifBlank { item.id }
        
        return when (item.itemType) {
            WishlistItemType.TIME_BASED -> WishlistNavigation.ToTimeBasedDetail(itemId)
            WishlistItemType.HOURLY_GEAR -> WishlistNavigation.ToHourlyGearDetail(itemId)
            WishlistItemType.SPLIT_STAY -> WishlistNavigation.ToTimeBasedDetail(itemId) // Safe fallback
            WishlistItemType.OTHER -> WishlistNavigation.ToTimeBasedDetail(itemId) // Default fallback
        }
    }
    
    /**
     * Called when auth is completed (from auth sheet)
     */
    fun onAuthCompleted() {
        checkAuthAndLoad()
    }
}


