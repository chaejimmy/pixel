package com.shourov.apps.pacedream.feature.wanted.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shourov.apps.pacedream.core.network.auth.AuthSession
import com.shourov.apps.pacedream.feature.wanted.data.WantedRepository
import com.shourov.apps.pacedream.feature.wanted.data.dto.CreateOfferBody
import com.shourov.apps.pacedream.feature.wanted.model.OfferFormState
import com.shourov.apps.pacedream.feature.wanted.model.RequestDetailUiState
import com.shourov.apps.pacedream.feature.wanted.model.WantedOffer
import com.shourov.apps.pacedream.feature.wanted.model.WantedRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class RequestDetailViewModel @Inject constructor(
    private val repository: WantedRepository,
    private val authSession: AuthSession,
) : ViewModel() {

    private val _result = MutableStateFlow<RequestResult>(RequestResult.Loading)
    private val _offers = MutableStateFlow<List<WantedOffer>>(emptyList())

    val state: StateFlow<RequestDetailUiState> = combine(
        _result,
        authSession.currentUser,
        _offers,
    ) { result, user, offers ->
        toUiState(result, user?.id, offers)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = RequestDetailUiState.Loading,
    )

    private val _offer = MutableStateFlow(OfferFormState())
    val offer: StateFlow<OfferFormState> = _offer.asStateFlow()

    private var requestId: String? = null

    fun load(id: String) {
        if (requestId == id && _result.value is RequestResult.Content) return
        requestId = id
        _offers.value = emptyList()
        viewModelScope.launch {
            _result.value = RequestResult.Loading
            repository.getRequest(id)
                .onSuccess { request ->
                    _result.value = RequestResult.Content(request)
                    // Only the author can see offers. Skipping the
                    // fetch for non-authors saves a (likely-403) round
                    // trip and keeps their UI clean.
                    if (isOwnedByCurrentUser(request)) {
                        loadOffersForOwner(id)
                    }
                }
                .onFailure { e ->
                    Timber.e(e, "Failed to load request $id")
                    _result.value = RequestResult.Error(
                        e.message ?: "Couldn't load request"
                    )
                }
        }
    }

    fun onPriceChange(value: String) {
        _offer.update { it.copy(price = value, error = null) }
    }

    fun onMessageChange(value: String) {
        _offer.update { it.copy(message = value, error = null) }
    }

    fun resetOfferSheet() {
        _offer.value = OfferFormState()
    }

    fun submitOffer() {
        val id = requestId ?: return
        val current = _offer.value
        val price = current.price.toDoubleOrNull()
        if (price == null || price <= 0.0) {
            _offer.update { it.copy(error = "Enter a valid price") }
            return
        }
        if (current.message.isBlank()) {
            _offer.update { it.copy(error = "Add a short message") }
            return
        }
        viewModelScope.launch {
            _offer.update { it.copy(submitting = true, error = null) }
            repository.createOffer(id, CreateOfferBody(price, current.message.trim()))
                .onSuccess {
                    _offer.update {
                        it.copy(submitting = false, submitted = true)
                    }
                }
                .onFailure { e ->
                    Timber.e(e, "Failed to submit offer")
                    _offer.update {
                        it.copy(
                            submitting = false,
                            error = friendlyError(e),
                        )
                    }
                }
        }
    }

    private suspend fun loadOffersForOwner(id: String) {
        repository.getOffersForRequest(id)
            .onSuccess { _offers.value = it }
            .onFailure { e ->
                // Don't flip the whole screen to an error — the owner
                // still sees the request body. The empty-offers state
                // in OffersList communicates "no offers yet" if the
                // list legitimately came back empty.
                Timber.w(e, "Failed to load offers for request $id (owner view)")
            }
    }

    private fun isOwnedByCurrentUser(request: WantedRequest): Boolean {
        val me = authSession.currentUserId?.takeIf { it.isNotBlank() } ?: return false
        val owner = request.authorId?.takeIf { it.isNotBlank() } ?: return false
        return me == owner
    }

    /**
     * Mirrors the mapping in `CreateRequestViewModel.friendlyError`:
     * raw backend messages are often stack traces, HTML, or vendor
     * jargon that aren't actionable for users. Every known failure
     * mode maps to a short, offer-specific string instead.
     */
    private fun friendlyError(e: Throwable): String {
        val msg = e.message.orEmpty().lowercase()
        return when {
            msg.contains("401") || msg.contains("unauthor") ->
                "Please sign in again to send your offer."
            msg.contains("403") -> "We can't send this offer right now."
            msg.contains("429") -> "You're sending offers a bit too quickly. Please wait a moment."
            msg.contains("timeout") || msg.contains("unable to resolve") || msg.contains("network") ->
                "You appear to be offline. Please check your connection."
            else -> "Couldn't send your offer. Please try again."
        }
    }

    private fun toUiState(
        result: RequestResult,
        currentUserId: String?,
        offers: List<WantedOffer>,
    ): RequestDetailUiState = when (result) {
        RequestResult.Loading -> RequestDetailUiState.Loading
        is RequestResult.Error -> RequestDetailUiState.Error(result.message)
        is RequestResult.Content -> {
            val signedIn = !currentUserId.isNullOrBlank()
            val authorId = result.request.authorId
            val owner = signedIn && !authorId.isNullOrBlank() && authorId == currentUserId
            RequestDetailUiState.Content(
                request = result.request,
                isOwner = owner,
                isSignedIn = signedIn,
                offers = if (owner) offers else emptyList(),
            )
        }
    }

    private sealed interface RequestResult {
        data object Loading : RequestResult
        data class Error(val message: String) : RequestResult
        data class Content(val request: WantedRequest) : RequestResult
    }
}
