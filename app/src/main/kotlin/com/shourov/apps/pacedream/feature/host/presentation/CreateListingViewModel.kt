package com.shourov.apps.pacedream.feature.host.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pacedream.app.core.auth.SessionManager
import com.shourov.apps.pacedream.feature.host.data.CreateListingRequest
import com.shourov.apps.pacedream.feature.host.data.HostRepository
import com.shourov.apps.pacedream.feature.host.data.StripeConnectRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.inject.Inject

@HiltViewModel
class CreateListingViewModel @Inject constructor(
    private val hostRepository: HostRepository,
    private val stripeConnectRepository: StripeConnectRepository,
    sessionManager: SessionManager,
) : ViewModel() {

    /**
     * The authenticated user id (or null when unauthenticated). Exposed so the
     * create-listing screen can scope the draft store per-user and prevent
     * drafts from leaking across accounts on shared devices.
     */
    val currentUserId: StateFlow<String?> = sessionManager.currentUser
        .map { it?.id }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = null,
        )

    sealed class Effect {
        data class PublishSuccess(
            val listingId: String,
            val title: String,
            val coverUrl: String? = null,
        ) : Effect()
        data class PublishError(val message: String) : Effect()

        /**
         * Raised before the network call when the host has not yet completed
         * Stripe Connect payout onboarding. The screen should surface this as
         * a blocking prompt that routes to /host earnings > Setup payouts
         * instead of hitting the listings API (which would create a listing
         * that can take bookings but cannot actually pay out).
         */
        data class PayoutSetupRequired(val reason: String) : Effect()
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

                // Payout gate: refuse to publish a monetized listing when the
                // host has not completed Stripe Connect onboarding. Without
                // this check a new host can take bookings against a Connect
                // account that cannot receive transfers, and we would owe
                // them money we cannot actually pay out. We treat a network
                // failure on the status lookup as *non-fatal* — the backend
                // is the real enforcement point, and punishing the host for
                // a transient network blip would be worse than the rare
                // case where our client cache disagrees with Stripe.
                val status = stripeConnectRepository.getConnectAccountStatus()
                val canPayout = status.getOrNull()?.resolvedPayoutsEnabled
                if (canPayout == false) {
                    Timber.w("Publish blocked: Stripe Connect payouts not enabled")
                    _effects.send(
                        Effect.PayoutSetupRequired(
                            "Finish your payout setup before publishing. You won\u2019t be able to receive payments from bookings until your Stripe account is active."
                        )
                    )
                    return@launch
                }
                if (canPayout == null) {
                    Timber.w("Publish allowed but Stripe status fetch failed; relying on backend enforcement")
                }

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

    /**
     * Checks current Stripe Connect payout eligibility without publishing.
     * Used by the Create Listing screen to render a warning banner before
     * the host fills out the wizard so they are not surprised at Publish.
     */
    suspend fun isPayoutReady(): Boolean {
        return try {
            stripeConnectRepository.getConnectAccountStatus()
                .getOrNull()?.resolvedPayoutsEnabled == true
        } catch (e: Exception) {
            Timber.w(e, "isPayoutReady check failed; assuming ready")
            true
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
