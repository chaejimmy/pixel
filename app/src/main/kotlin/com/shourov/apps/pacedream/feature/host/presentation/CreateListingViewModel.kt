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
import timber.log.Timber
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
                Timber.d(
                    "Publishing listing: type=%s sub=%s title='%s' price=%.2f images=%d address='%s'",
                    request.listing_type, request.subCategory, request.title,
                    request.price, request.images?.size ?: 0, request.address ?: "",
                )
                val result = hostRepository.createListing(request)
                result.onSuccess { property ->
                    Timber.d("Listing created successfully: id=%s title=%s images=%d", property.id, property.title, property.images.size)
                    _effects.send(
                        Effect.PublishSuccess(
                            listingId = property.id.ifEmpty { "created" },
                            title = property.title.ifEmpty { request.title },
                            coverUrl = property.images.firstOrNull()
                                ?: request.images?.firstOrNull(),
                        )
                    )
                }
                result.onFailure { error ->
                    Timber.w(error, "Listing creation failed")
                    _effects.send(Effect.PublishError(friendlyError(error)))
                }
            } catch (e: Exception) {
                Timber.e(e, "Listing creation exception")
                _effects.send(Effect.PublishError(friendlyError(e)))
            } finally {
                _isPublishing.value = false
            }
        }
    }

    companion object {
        /**
         * Map errors to user-friendly messages.
         * The backend returns specific messages for 400 errors (e.g., "Missing required fields: title or price",
         * "Price must be at least $1"), so we surface those directly when available.
         */
        fun friendlyError(error: Throwable): String {
            val msg = error.message ?: ""
            return when {
                error is UnknownHostException ->
                    "No internet connection. Please reconnect and try again."
                error is SocketTimeoutException ->
                    "Network timeout. Please try again."
                msg.contains("401") || msg.contains("Unauthorized", ignoreCase = true) ->
                    "You\u2019re not signed in. Please sign in again and retry."
                msg.contains("413") || msg.contains("Request Entity Too Large", ignoreCase = true) ->
                    "Your photos are too large. Remove a photo or pick smaller ones, then try again."
                msg.matches(Regex(".*\\b5\\d{2}\\b.*")) ->
                    "Server error. Please try again later."
                // Backend 400 errors include specific validation messages — surface them directly
                msg.contains("Missing required fields", ignoreCase = true) ||
                    msg.contains("Price must be", ignoreCase = true) ||
                    msg.contains("Title", ignoreCase = true) ||
                    msg.contains("Description must be", ignoreCase = true) ->
                    msg
                msg.isNotBlank() ->
                    msg
                else ->
                    "Failed to create listing. Please try again."
            }
        }
    }
}
