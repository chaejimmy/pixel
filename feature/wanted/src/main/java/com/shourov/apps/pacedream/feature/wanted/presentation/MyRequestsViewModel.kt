package com.shourov.apps.pacedream.feature.wanted.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shourov.apps.pacedream.feature.wanted.data.OfferSeenTracker
import com.shourov.apps.pacedream.feature.wanted.data.WantedRepository
import com.shourov.apps.pacedream.feature.wanted.model.RequestsListUiState
import com.shourov.apps.pacedream.feature.wanted.model.WantedRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * Backs the "Mine" tab in guest mode — the requester's view of their own
 * posts. Tracks unread-offer state so the tab chip can show a dot when a
 * new offer has arrived since the last visit.
 */
@HiltViewModel
class MyRequestsViewModel @Inject constructor(
    private val repository: WantedRepository,
    private val offerSeenTracker: OfferSeenTracker,
) : ViewModel() {

    private val _state = MutableStateFlow<RequestsListUiState>(RequestsListUiState.Loading)
    val state: StateFlow<RequestsListUiState> = _state.asStateFlow()

    private val _refreshing = MutableStateFlow(false)
    val refreshing: StateFlow<Boolean> = _refreshing.asStateFlow()

    private val _hasUnreadOffers = MutableStateFlow(false)
    val hasUnreadOffers: StateFlow<Boolean> = _hasUnreadOffers.asStateFlow()

    init {
        load()
    }

    fun refresh() {
        viewModelScope.launch {
            _refreshing.value = true
            fetch()
            _refreshing.value = false
        }
    }

    fun load() {
        viewModelScope.launch {
            _state.value = RequestsListUiState.Loading
            fetch()
        }
    }

    /**
     * Snapshots the current offer counts as "seen" — call this when the
     * user opens the Mine tab so the dot badge clears.
     */
    fun markOffersSeen() {
        val content = _state.value as? RequestsListUiState.Content ?: return
        offerSeenTracker.markRequestsSeen(content.requests)
        _hasUnreadOffers.value = false
    }

    private suspend fun fetch() {
        repository.getMyRequests()
            .onSuccess { items ->
                _state.value = RequestsListUiState.Content(items)
                refreshUnreadFlag(items)
            }
            .onFailure { e ->
                Timber.e(e, "Failed to load my requests")
                _state.value = RequestsListUiState.Error(
                    e.message ?: "Couldn't load your requests"
                )
            }
    }

    private fun refreshUnreadFlag(items: List<WantedRequest>) {
        _hasUnreadOffers.value = offerSeenTracker.hasUnreadOffers(items)
    }
}
