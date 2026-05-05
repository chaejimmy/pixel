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
                            error = e.message ?: "Couldn't submit offer",
                        )
                    }
                }
        }
    }
}
