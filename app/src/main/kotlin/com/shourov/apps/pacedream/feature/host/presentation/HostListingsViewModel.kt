package com.shourov.apps.pacedream.feature.host.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shourov.apps.pacedream.feature.host.data.HostListingsData
import com.shourov.apps.pacedream.feature.host.data.HostRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class HostListingsViewModel @Inject constructor(
    private val hostRepository: HostRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HostListingsData())
    val uiState: StateFlow<HostListingsData> = _uiState.asStateFlow()

    /** Transient UI events (snackbar messages) that should fire once. */
    sealed class Effect {
        data class DeleteSucceeded(val listingId: String, val title: String) : Effect()
        data class DeleteFailed(val listingId: String, val message: String) : Effect()
    }

    private val _effects = Channel<Effect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    /** Set while a delete request is in flight — used to disable the button. */
    private val _deletingIds = MutableStateFlow<Set<String>>(emptySet())
    val deletingIds: StateFlow<Set<String>> = _deletingIds.asStateFlow()

    init {
        loadHostListings()
    }

    private fun loadHostListings() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            val selectedFilter = uiState.value.selectedFilter
            val sort = uiState.value.selectedSort.lowercase()

            // iOS parity: always fetch all listings, then filter client-side.
            // The backend may not support filter params or may not account for
            // admin-approval overrides that happen during client-side parsing.
            hostRepository.getHostListings(null, sort)
                .onSuccess { allListings ->
                    val filtered = when (selectedFilter.lowercase()) {
                        "active" -> allListings.filter { it.isActiveStatus }
                        "pending" -> allListings.filter { it.isPendingReview }
                        "unavailable" -> allListings.filter { !it.isActiveStatus && !it.isPendingReview && !it.isRejected }
                        else -> allListings // "all"
                    }
                    _uiState.value = _uiState.value.copy(
                        listings = filtered,
                        isLoading = false,
                        error = null
                    )
                }
                .onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = com.pacedream.common.util.UserFacingErrorMapper.forLoadProperties(exception)
                    )
                }
        }
    }

    /**
     * Delete a listing via the existing HostRepository.deleteListing endpoint.
     * Optimistically removes the listing from the visible list so the host
     * gets immediate feedback; on failure the list is reloaded and an error
     * effect is emitted so the screen can show a snackbar with Retry.
     */
    fun deleteListing(listingId: String) {
        if (listingId.isBlank()) return
        if (_deletingIds.value.contains(listingId)) return

        val snapshot = _uiState.value
        val target = snapshot.listings.firstOrNull { it.id == listingId }
        val displayTitle = target?.title?.takeIf { it.isNotBlank() } ?: "Listing"

        _deletingIds.value = _deletingIds.value + listingId
        // Optimistic removal — restored if the API call fails.
        _uiState.value = snapshot.copy(
            listings = snapshot.listings.filterNot { it.id == listingId }
        )

        viewModelScope.launch {
            hostRepository.deleteListing(listingId)
                .onSuccess {
                    Timber.d("Deleted listing %s", listingId)
                    _effects.trySend(Effect.DeleteSucceeded(listingId, displayTitle))
                    // Refetch so pagination/counters stay truthful.
                    loadHostListings()
                }
                .onFailure { e ->
                    Timber.w(e, "Delete listing failed for %s", listingId)
                    // Rollback the optimistic change.
                    _uiState.value = snapshot
                    val message = com.pacedream.common.util.UserFacingErrorMapper
                        .map(e, "We couldn't delete this listing. Please try again.")
                    _effects.trySend(Effect.DeleteFailed(listingId, message))
                }
            _deletingIds.value = _deletingIds.value - listingId
        }
    }

    fun updateFilter(filter: String) {
        _uiState.value = _uiState.value.copy(selectedFilter = filter)
        loadHostListings()
    }

    fun updateSort(sort: String) {
        _uiState.value = _uiState.value.copy(selectedSort = sort)
        loadHostListings()
    }

    fun refreshData() {
        loadHostListings()
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
