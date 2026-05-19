package com.shourov.apps.pacedream.feature.wanted.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shourov.apps.pacedream.feature.wanted.data.OfferSeenTracker
import com.shourov.apps.pacedream.feature.wanted.data.WantedRepository
import com.shourov.apps.pacedream.feature.wanted.model.MyRequestsTab
import com.shourov.apps.pacedream.feature.wanted.model.MyRequestsUiState
import com.shourov.apps.pacedream.feature.wanted.model.RequestStatus
import com.shourov.apps.pacedream.feature.wanted.model.WantedRequest
import com.shourov.apps.pacedream.feature.wanted.presentation.util.RequestExpiryResolver
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

/**
 * Backs the "Mine" tab in guest mode — the requester's view of their own
 * posts. Splits the list into Active / Expired / Fulfilled / Cancelled
 * sub-tabs (selection persists via [SavedStateHandle]) and owns the
 * Renew / Mark as Fulfilled / Cancel actions.
 *
 * Tracks unread-offer state so the outer tab chip can show a dot when a
 * new offer has arrived since the last visit.
 */
@HiltViewModel
class MyRequestsViewModel @Inject constructor(
    private val repository: WantedRepository,
    private val offerSeenTracker: OfferSeenTracker,
    private val savedStateHandle: SavedStateHandle,
    private val clock: Clock = Clock.systemDefaultZone(),
) : ViewModel() {

    private val _selectedTab = MutableStateFlow(
        MyRequestsTab.fromKey(savedStateHandle[KEY_SELECTED_INNER_TAB])
    )
    val selectedTab: StateFlow<MyRequestsTab> = _selectedTab.asStateFlow()

    /** Raw list as returned by the API — kept here so tab switches don't refetch. */
    private var allRequests: List<WantedRequest> = emptyList()

    private val _state = MutableStateFlow<MyRequestsUiState>(MyRequestsUiState.Loading)
    val state: StateFlow<MyRequestsUiState> = _state.asStateFlow()

    private val _refreshing = MutableStateFlow(false)
    val refreshing: StateFlow<Boolean> = _refreshing.asStateFlow()

    private val _hasUnreadOffers = MutableStateFlow(false)
    val hasUnreadOffers: StateFlow<Boolean> = _hasUnreadOffers.asStateFlow()

    init {
        load()
    }

    fun selectTab(tab: MyRequestsTab) {
        if (_selectedTab.value == tab) return
        _selectedTab.value = tab
        savedStateHandle[KEY_SELECTED_INNER_TAB] = tab.key
        // Only re-slice when content is loaded; if we're still Loading or
        // showing an Error, the next successful fetch will pick up the new
        // tab via [recomputeContent] without us materialising an empty
        // Content state here.
        if (_state.value is MyRequestsUiState.Content) {
            recomputeContent()
        }
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
            _state.value = MyRequestsUiState.Loading
            fetch()
        }
    }

    /**
     * Snapshots the current offer counts as "seen" — call this when the
     * user opens the Mine tab so the dot badge clears.
     */
    fun markOffersSeen() {
        offerSeenTracker.markRequestsSeen(allRequests)
        _hasUnreadOffers.value = false
    }

    /**
     * Acceptance criteria: a Renew Request must move the post out of the
     * Expired tab and back into Active. We optimistically update the local
     * record with the new expiry, then sync with whatever the server
     * returns (which is the source of truth for the new [WantedRequest.expiresAt]).
     */
    fun renew(requestId: String, daysFromNow: Long = DEFAULT_RENEWAL_DAYS) {
        runAction(requestId) {
            val newExpiry = LocalDate.now(clock).plusDays(daysFromNow).toString()
            repository.renewRequest(id = requestId, newExpiry = newExpiry)
        }
    }

    fun markFulfilled(requestId: String) {
        runAction(requestId) {
            repository.updateRequestStatus(
                id = requestId,
                status = RequestStatus.Fulfilled,
            )
        }
    }

    fun cancel(requestId: String) {
        runAction(requestId) {
            repository.updateRequestStatus(
                id = requestId,
                status = RequestStatus.Cancelled,
            )
        }
    }

    /** Dismiss the inline action-error banner. */
    fun dismissActionError() {
        _state.update { current ->
            (current as? MyRequestsUiState.Content)?.copy(actionError = null) ?: current
        }
    }

    private fun runAction(
        requestId: String,
        block: suspend () -> Result<WantedRequest>,
    ) {
        val content = _state.value as? MyRequestsUiState.Content ?: return
        if (content.pendingActionId != null) return
        _state.value = content.copy(pendingActionId = requestId, actionError = null)
        viewModelScope.launch {
            block()
                .onSuccess { updated ->
                    allRequests = allRequests.map { existing ->
                        if (existing.id == updated.id) updated else existing
                    }
                    recomputeContent(pendingActionId = null, actionError = null)
                }
                .onFailure { e ->
                    Timber.e(e, "Request lifecycle action failed for $requestId")
                    recomputeContent(
                        pendingActionId = null,
                        actionError = friendlyActionError(e),
                    )
                }
        }
    }

    private suspend fun fetch() {
        repository.getMyRequests()
            .onSuccess { items ->
                allRequests = items
                recomputeContent()
                refreshUnreadFlag(items)
            }
            .onFailure { e ->
                Timber.e(e, "Failed to load my requests")
                _state.value = MyRequestsUiState.Error(
                    e.message ?: "Couldn't load your requests"
                )
            }
    }

    /**
     * Slot the raw list into the lifecycle buckets and emit a fresh
     * [MyRequestsUiState.Content]. Client-side expiry is applied here so a
     * stale Active row whose `expiresAt` has passed shows up in the
     * Expired tab even before the server flips the column.
     */
    private fun recomputeContent(
        pendingActionId: String? = (_state.value as? MyRequestsUiState.Content)?.pendingActionId,
        actionError: String? = (_state.value as? MyRequestsUiState.Content)?.actionError,
    ) {
        val today = LocalDate.now(clock)
        val byStatus: Map<RequestStatus, List<WantedRequest>> = allRequests.groupBy { request ->
            RequestExpiryResolver.effectiveStatus(request, today)
        }
        val counts = MyRequestsTab.entries.associateWith { tab ->
            byStatus[tab.status]?.size ?: 0
        }
        val tab = _selectedTab.value
        val visible = byStatus[tab.status].orEmpty()
        _state.value = MyRequestsUiState.Content(
            selectedTab = tab,
            visible = visible,
            counts = counts,
            pendingActionId = pendingActionId,
            actionError = actionError,
        )
    }

    private fun refreshUnreadFlag(items: List<WantedRequest>) {
        _hasUnreadOffers.value = offerSeenTracker.hasUnreadOffers(items)
    }

    private fun friendlyActionError(e: Throwable): String {
        val msg = e.message.orEmpty().lowercase()
        return when {
            msg.contains("401") || msg.contains("unauthor") ->
                "Please sign in again."
            msg.contains("403") ->
                "We can't update this request right now."
            msg.contains("timeout") || msg.contains("network") ->
                "You appear to be offline. Please check your connection."
            else -> "Couldn't update your request. Please try again."
        }
    }

    companion object {
        private const val KEY_SELECTED_INNER_TAB = "wanted.my_requests.inner_tab"
        /** Default renewal window for the Renew Request action. */
        const val DEFAULT_RENEWAL_DAYS: Long = 30
    }
}
