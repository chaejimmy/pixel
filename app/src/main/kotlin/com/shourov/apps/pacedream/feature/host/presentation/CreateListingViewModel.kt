package com.shourov.apps.pacedream.feature.host.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pacedream.app.core.auth.SessionManager
import com.shourov.apps.pacedream.feature.host.data.CreateListingRequest
import com.shourov.apps.pacedream.feature.host.data.HostRepository
import com.shourov.apps.pacedream.feature.host.data.StripeConnectRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
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
    private val savedStateHandle: SavedStateHandle,
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
         * Stripe Connect payout onboarding. The screen surfaces this as a
         * blocking [PayoutRequiredDialog] whose primary CTA routes to
         * [Effect.NavigateToPayoutSetup] instead of hitting the listings API.
         */
        data class PayoutSetupRequired(val reason: String) : Effect()

        /** Tell the host screen to navigate to Stripe Connect onboarding. */
        data object NavigateToPayoutSetup : Effect()
    }

    // ── Publish state ─────────────────────────────────────────────────────

    private val _isPublishing = MutableStateFlow(false)
    val isPublishing: StateFlow<Boolean> = _isPublishing.asStateFlow()

    private val _effects = Channel<Effect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    // Photo uploads are handled inside the wizard composable (see
    // startPhotoUpload in CreateListingScreen) so the launcher results can
    // be bound to Compose thumbnail state without a VM round-trip. The
    // [ImageUploadManager] class stays in the codebase as the foundation
    // for a future, deeper hoist of form state into the VM.

    // ── Phase / step / dirty persisted via SavedStateHandle ───────────────
    //
    // SavedStateHandle is intentionally narrow: it only survives process
    // death for small UI-state flags. The durable restore is the draft
    // store on disk (SharedPreferences) — see [CreateListingDraftStore].

    fun getPhase(): String = savedStateHandle[SSH_PHASE] ?: "entry"
    fun setPhase(phase: String) { savedStateHandle[SSH_PHASE] = phase }

    fun getCurrentStep(): Int = savedStateHandle[SSH_STEP] ?: 0
    fun setCurrentStep(step: Int) { savedStateHandle[SSH_STEP] = step }

    fun isDirty(): Boolean = savedStateHandle[SSH_DIRTY] ?: false
    fun markDirty(dirty: Boolean = true) { savedStateHandle[SSH_DIRTY] = dirty }

    // ── Payout readiness ──────────────────────────────────────────────────

    private val _payoutReady = MutableStateFlow<Boolean?>(null)
    /** null = unknown (still checking); true/false = resolved. */
    val payoutReady: StateFlow<Boolean?> = _payoutReady.asStateFlow()

    private var payoutCheckJob: Job? = null

    /**
     * Check payout readiness proactively (called from the screen on
     * wizard entry).  The banner reacts to the result; publish also
     * re-checks synchronously so a transient banner miss cannot let a
     * listing slip past.
     */
    fun refreshPayoutReadiness() {
        payoutCheckJob?.cancel()
        payoutCheckJob = viewModelScope.launch {
            val ready = try {
                stripeConnectRepository.getConnectAccountStatus()
                    .getOrNull()?.resolvedPayoutsEnabled
            } catch (e: Exception) {
                Timber.w(e, "Payout readiness check failed; assuming ready")
                true
            }
            _payoutReady.value = ready
        }
    }

    fun onRequestPayoutSetup() {
        viewModelScope.launch { _effects.send(Effect.NavigateToPayoutSetup) }
    }

    // ── Publish ───────────────────────────────────────────────────────────

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
                // host has not completed Stripe Connect onboarding. A network
                // failure on the status lookup is *non-fatal* — the backend
                // is the authoritative check.
                val status = stripeConnectRepository.getConnectAccountStatus()
                val canPayout = status.getOrNull()?.resolvedPayoutsEnabled
                if (canPayout == false) {
                    Timber.w("Publish blocked: Stripe Connect payouts not enabled")
                    _payoutReady.value = false
                    _effects.send(
                        Effect.PayoutSetupRequired(
                            "Finish your payout setup before publishing. You won’t be able to receive payments from bookings until your Stripe account is active."
                        )
                    )
                    return@launch
                }
                if (canPayout == null) {
                    Timber.w("Publish allowed but Stripe status fetch failed; relying on backend enforcement")
                } else {
                    _payoutReady.value = true
                }

                val result = hostRepository.createListing(request)
                result.onSuccess { property ->
                    Timber.d(
                        "Listing created successfully: id=%s title=%s images=%d",
                        property.id, property.title, property.images.size,
                    )
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
     * Retained for callers that prefer a suspend-style check.
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

        // SavedStateHandle keys. Kept small and primitive-only so we do
        // not accidentally serialize a massive form blob.
        private const val SSH_PHASE = "cl_phase"
        private const val SSH_STEP = "cl_step"
        private const val SSH_DIRTY = "cl_dirty"

        /**
         * Map errors to user-friendly messages. Backend 400 errors carry
         * useful messages (missing fields, price validation) and we surface
         * those directly when present.
         */
        fun friendlyError(error: Throwable): String {
            val msg = error.message ?: ""
            return when {
                error is UnknownHostException ->
                    "No internet connection. Please reconnect and try again."
                error is SocketTimeoutException ->
                    "Network timeout. Please try again."
                msg.contains("401") || msg.contains("Unauthorized", ignoreCase = true) ->
                    "You’re not signed in. Please sign in again and retry."
                msg.contains("413") || msg.contains("Request Entity Too Large", ignoreCase = true) ->
                    "Your photos are too large. Remove a photo or pick smaller ones, then try again."
                msg.matches(Regex(".*\\b5\\d{2}\\b.*")) ->
                    "Server error. Please try again later."
                msg.contains("Missing required fields", ignoreCase = true) ||
                    msg.contains("Price must be", ignoreCase = true) ||
                    msg.contains("Title", ignoreCase = true) ||
                    msg.contains("Description must be", ignoreCase = true) ->
                    msg
                msg.isNotBlank() -> msg
                else -> "Failed to create listing. Please try again."
            }
        }
    }
}
