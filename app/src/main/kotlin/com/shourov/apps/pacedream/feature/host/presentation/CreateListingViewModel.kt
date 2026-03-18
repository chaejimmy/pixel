package com.shourov.apps.pacedream.feature.host.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shourov.apps.pacedream.feature.host.data.CreateListingRequest
import com.shourov.apps.pacedream.feature.host.data.HostRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CreateListingViewModel @Inject constructor(
    private val hostRepository: HostRepository
) : ViewModel() {

    sealed class Effect {
        data class PublishSuccess(val listingId: String, val title: String) : Effect()
        data class PublishError(val message: String) : Effect()
    }

    private val _isPublishing = MutableStateFlow(false)
    val isPublishing: StateFlow<Boolean> = _isPublishing.asStateFlow()

    private val _effects = Channel<Effect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    fun publishListing(request: CreateListingRequest) {
        viewModelScope.launch {
            _isPublishing.value = true
            try {
                val result = hostRepository.createListing(request)
                result.onSuccess { property ->
                    _effects.send(
                        Effect.PublishSuccess(
                            listingId = property.id.ifEmpty { "created" },
                            title = property.title.ifEmpty { request.title }
                        )
                    )
                }
                result.onFailure { error ->
                    _effects.send(Effect.PublishError(error.message ?: "Failed to create listing"))
                }
            } catch (e: Exception) {
                _effects.send(Effect.PublishError(e.message ?: "Failed to create listing"))
            } finally {
                _isPublishing.value = false
            }
        }
    }
}
