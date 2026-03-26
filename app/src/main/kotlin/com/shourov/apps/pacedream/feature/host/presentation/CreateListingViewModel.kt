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
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.inject.Inject

@HiltViewModel
class CreateListingViewModel @Inject constructor(
    private val hostRepository: HostRepository
) : ViewModel() {

    sealed class Effect {
        data class PublishSuccess(
            val listingId: String,
            val title: String,
            val coverUrl: String? = null,
        ) : Effect()
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
                            title = property.title.ifEmpty { request.title },
                            coverUrl = request.images?.firstOrNull(),
                        )
                    )
                }
                result.onFailure { error ->
                    _effects.send(Effect.PublishError(friendlyError(error)))
                }
            } catch (e: Exception) {
                _effects.send(Effect.PublishError(friendlyError(e)))
            } finally {
                _isPublishing.value = false
            }
        }
    }

    /**
     * iOS parity: map common errors to user-friendly messages
     * (see CreateListingFlowCoordinator.friendlyPublishError).
     */
    companion object {
        fun friendlyError(error: Throwable): String {
            return when {
                error is UnknownHostException ->
                    "No internet connection. Please reconnect and try again."
                error is SocketTimeoutException ->
                    "Network timeout. Please try again."
                error.message?.contains("401") == true ||
                    error.message?.contains("Unauthorized", ignoreCase = true) == true ->
                    "You\u2019re not signed in. Please sign in again and retry."
                error.message?.contains("413") == true ||
                    error.message?.contains("Request Entity Too Large", ignoreCase = true) == true ->
                    "Your photos are too large to upload in one request. Remove a few photos or pick smaller ones, then try again."
                error.message?.contains("5") == true &&
                    error.message?.matches(Regex(".*\\b5\\d{2}\\b.*")) == true ->
                    "Server error. Please try again."
                else ->
                    error.message ?: "Failed to create listing. Please try again."
            }
        }
    }
}
