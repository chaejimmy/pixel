package com.shourov.apps.pacedream.feature.wanted.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shourov.apps.pacedream.feature.wanted.data.WantedRepository
import com.shourov.apps.pacedream.feature.wanted.data.dto.CreateOfferBody
import com.shourov.apps.pacedream.feature.wanted.model.OfferFormState
import com.shourov.apps.pacedream.feature.wanted.model.RequestDetailUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class RequestDetailViewModel @Inject constructor(
    private val repository: WantedRepository,
) : ViewModel() {

    private val _state = MutableStateFlow<RequestDetailUiState>(RequestDetailUiState.Loading)
    val state: StateFlow<RequestDetailUiState> = _state.asStateFlow()

    private val _offer = MutableStateFlow(OfferFormState())
    val offer: StateFlow<OfferFormState> = _offer.asStateFlow()

    private var requestId: String? = null

    fun load(id: String) {
        if (requestId == id && _state.value is RequestDetailUiState.Content) return
        requestId = id
        viewModelScope.launch {
            _state.value = RequestDetailUiState.Loading
            repository.getRequest(id)
                .onSuccess { _state.value = RequestDetailUiState.Content(it) }
                .onFailure { e ->
                    Timber.e(e, "Failed to load request $id")
                    _state.value = RequestDetailUiState.Error(
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
}
