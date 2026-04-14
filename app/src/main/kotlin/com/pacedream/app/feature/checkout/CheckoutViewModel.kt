package com.pacedream.app.feature.checkout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pacedream.app.core.config.AppConfig
import com.pacedream.app.core.network.ApiClient
import com.pacedream.app.core.network.ApiResult
import com.shourov.apps.pacedream.core.data.repository.BookingRepository as CoreBookingRepository
import com.shourov.apps.pacedream.model.BookingModel
import com.shourov.apps.pacedream.model.BookingStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import timber.log.Timber
import javax.inject.Inject

/**
 * Checkout status matching iOS CheckoutStatus enum.
 */
enum class CheckoutStatus {
    IDLE,
    LOADING_QUOTE,
    READY,
    PROCESSING,
    SUCCEEDED,
    FAILED
}

data class CheckoutUiState(
    val draft: BookingDraft? = null,
    val status: CheckoutStatus = CheckoutStatus.IDLE,
    val errorMessage: String? = null,
    // Quote-based pricing from backend (iOS parity)
    val quote: QuoteResponse? = null,
    // PaymentSheet configuration from backend
    val paymentSheetConfig: PaymentSheetConfig? = null,
    val publishableKey: String? = null,
    // Booking result
    val bookingId: String? = null,
    // Confirm-booking retry state (payment succeeded but booking pending)
    val isConfirmingBooking: Boolean = false,
    val confirmRetryCount: Int = 0,
    /**
     * PaymentIntent id for the current/last-known pending payment.
     * Surfaced as a support reference when retries are exhausted.
     */
    val pendingPaymentIntentId: String? = null,
    /**
     * True once local retries are exhausted.  UI shows the stuck
     * fallback with PI reference + copy + contact support instead
     * of the regular retry row.
     */
    val hasExhaustedRetries: Boolean = false,
    /**
     * True when the booking was recovered via the server-side webhook
     * rather than explicitly confirmed by the client's confirm-booking
     * call. The UI should indicate that the booking was completed by
     * the server (e.g. "Your booking was confirmed") rather than showing
     * the normal confirmation flow, so the user understands this was a
     * recovery rather than a fresh confirmation.
     */
    val isRecoveredByServer: Boolean = false,
)

@HiltViewModel
class CheckoutViewModel @Inject constructor(
    private val apiClient: ApiClient,
    private val appConfig: AppConfig,
    private val json: Json,
    private val nativePaymentRepository: NativePaymentRepository,
    private val coreBookingRepository: CoreBookingRepository,
    private val pendingPaymentStore: PendingPaymentStore,
) : ViewModel() {

    sealed class Effect {
        data class NavigateToConfirmation(val bookingId: String) : Effect()
        /**
         * Tells the UI to present Stripe PaymentSheet with the given config.
         * The UI layer creates the PaymentSheet instance (requires Activity context).
         */
        data class PresentPaymentSheet(
            val clientSecret: String,
            val publishableKey: String,
            val merchantDisplayName: String,
            val customerId: String?,
            val ephemeralKeySecret: String?
        ) : Effect()
    }

    private val _uiState = MutableStateFlow(CheckoutUiState())
    val uiState: StateFlow<CheckoutUiState> = _uiState.asStateFlow()

    private val _effects = Channel<Effect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    /** Stored client secret to extract PaymentIntent ID after payment */
    private var currentClientSecret: String? = null
    /** Cached quoteId for the in-flight payment (survives via PendingPaymentStore). */
    private var currentQuoteId: String? = null

    init {
        // Process-death recovery: if the previous instance of the app
        // was killed between a successful PaymentSheet and a successful
        // confirm-booking call, the persisted record drives us straight
        // into the post-payment state and we auto-retry the confirm
        // call exactly once.  The user's manual retry button stays
        // available within the retry budget.
        attemptRecoveryIfNeeded()
    }

    fun setDraft(draft: BookingDraft) {
        // If recovery has already promoted this ViewModel into a
        // post-payment state for a pending PaymentIntent, do NOT
        // re-fetch a fresh quote on top of it — that would clobber
        // the recovery UI back to READY.  Only set the draft for
        // reference so we know which listing the stuck payment was
        // for.
        val current = _uiState.value
        if (current.status == CheckoutStatus.SUCCEEDED && current.pendingPaymentIntentId != null) {
            _uiState.update { it.copy(draft = draft) }
            return
        }
        _uiState.update { it.copy(draft = draft, errorMessage = null) }
        fetchQuote(draft)
    }

    // ── Step 1: Fetch Quote (iOS parity: NativeCheckoutViewModel.fetchQuote) ──

    private fun fetchQuote(draft: BookingDraft) {
        viewModelScope.launch {
            _uiState.update { it.copy(status = CheckoutStatus.LOADING_QUOTE, errorMessage = null) }

            val bookingType = when (draft.listingType) {
                "gear" -> "gear"
                "split-stay" -> "splitstay"
                else -> "timebased"
            }

            when (val result = nativePaymentRepository.createQuote(
                listingId = draft.listingId,
                bookingType = bookingType,
                startTime = draft.startTimeISO,
                endTime = draft.endTimeISO,
                quantity = draft.guests
            )) {
                is ApiResult.Success -> {
                    _uiState.update {
                        it.copy(
                            status = CheckoutStatus.READY,
                            quote = result.data,
                            errorMessage = null
                        )
                    }
                }
                is ApiResult.Failure -> {
                    Timber.e("Quote failed: ${result.error.message}")
                    _uiState.update {
                        it.copy(
                            status = CheckoutStatus.FAILED,
                            errorMessage = result.error.message
                        )
                    }
                }
            }
        }
    }

    /** Check whether the current quote has expired based on its expiresAt ISO 8601 timestamp. */
    private fun isQuoteExpired(quote: QuoteResponse): Boolean {
        val expiresAt = quote.expiresAt ?: return false
        return try {
            val expiry = java.time.Instant.parse(expiresAt)
            java.time.Instant.now() >= expiry
        } catch (e: Exception) {
            Timber.w("Could not parse quote expiresAt: $expiresAt")
            false // Assume valid — server will reject if truly stale
        }
    }

    // ── Step 2: Create PaymentIntent & Present Sheet ──

    fun submitPayment() {
        val quote = _uiState.value.quote ?: return
        if (_uiState.value.status == CheckoutStatus.PROCESSING ||
            _uiState.value.status == CheckoutStatus.SUCCEEDED) return

        // Quote expiry protection: refresh stale quotes before creating a PaymentIntent
        if (isQuoteExpired(quote)) {
            Timber.w("Quote expired, prompting user to retry")
            _uiState.update {
                it.copy(
                    status = CheckoutStatus.FAILED,
                    errorMessage = "Price quote expired. Please tap Pay again to get an updated price."
                )
            }
            // Auto-refresh the quote so the next tap uses fresh pricing
            _uiState.value.draft?.let { fetchQuote(it) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(status = CheckoutStatus.PROCESSING, errorMessage = null) }

            // Create PaymentIntent on backend
            when (val result = nativePaymentRepository.createPaymentIntent(quote.quoteId)) {
                is ApiResult.Success -> {
                    val config = result.data

                    // Resolve Stripe publishable key
                    val stripeKey = nativePaymentRepository.resolvePublishableKey(config)
                    if (stripeKey.isNullOrBlank()) {
                        _uiState.update {
                            it.copy(
                                status = CheckoutStatus.FAILED,
                                errorMessage = "Stripe is not configured. Please try again later."
                            )
                        }
                        return@launch
                    }

                    // Store for later extraction of PI ID
                    currentClientSecret = config.paymentIntentClientSecret
                    currentQuoteId = quote.quoteId
                    val piId = nativePaymentRepository.extractPaymentIntentId(
                        config.paymentIntentClientSecret
                    )

                    // Crash-resilient: persist the pending payment
                    // BEFORE we hand control to the Stripe PaymentSheet
                    // so a process death during the sheet is recoverable
                    // on next launch.  commit() (not apply()) is used
                    // inside the store so the write is on disk.
                    pendingPaymentStore.save(
                        PendingNativePayment(
                            clientSecret = config.paymentIntentClientSecret,
                            quoteId = quote.quoteId,
                            paymentIntentId = piId,
                            listingId = _uiState.value.draft?.listingId,
                            retryCount = 0,
                            paymentSucceededLocally = false,
                        )
                    )

                    _uiState.update {
                        it.copy(
                            paymentSheetConfig = config,
                            publishableKey = stripeKey,
                            pendingPaymentIntentId = piId,
                            confirmRetryCount = 0,
                            hasExhaustedRetries = false,
                        )
                    }

                    // Tell UI to present PaymentSheet
                    _effects.send(
                        Effect.PresentPaymentSheet(
                            clientSecret = config.paymentIntentClientSecret,
                            publishableKey = stripeKey,
                            merchantDisplayName = config.merchantDisplayName,
                            customerId = config.customerId,
                            ephemeralKeySecret = config.ephemeralKeySecret
                        )
                    )
                }
                is ApiResult.Failure -> {
                    Timber.e("PaymentIntent creation failed: ${result.error.message}")
                    _uiState.update {
                        it.copy(
                            status = CheckoutStatus.FAILED,
                            errorMessage = result.error.message
                        )
                    }
                }
            }
        }
    }

    // ── Step 3: Handle PaymentSheet result (called by UI) ──

    fun onPaymentSheetCompleted() {
        // Payment captured locally.  Mark the persisted record so
        // recovery on the next launch knows this is a paid-but-
        // unconfirmed session (not a sheet-abandoned one) and then
        // run confirm-booking.
        pendingPaymentStore.markPaymentSucceededLocally()
        viewModelScope.launch {
            confirmBookingOnBackend()
        }
    }

    fun onPaymentSheetCancelled() {
        // User intentionally cancelled — no money captured, clear
        // the pending record and allow a fresh attempt.
        pendingPaymentStore.clear()
        currentClientSecret = null
        currentQuoteId = null
        _uiState.update {
            it.copy(
                status = CheckoutStatus.READY,
                errorMessage = null,
                pendingPaymentIntentId = null,
                hasExhaustedRetries = false,
                confirmRetryCount = 0,
            )
        }
    }

    fun onPaymentSheetFailed(errorMessage: String) {
        // PaymentSheet-internal failure — no capture, clear state.
        pendingPaymentStore.clear()
        currentClientSecret = null
        currentQuoteId = null
        _uiState.update {
            it.copy(
                status = CheckoutStatus.FAILED,
                errorMessage = errorMessage,
                pendingPaymentIntentId = null,
                hasExhaustedRetries = false,
                confirmRetryCount = 0,
            )
        }
    }

    // ── Step 4: Confirm booking on backend after successful payment ──

    private suspend fun confirmBookingOnBackend() {
        _uiState.update { it.copy(isConfirmingBooking = true) }

        val clientSecret = currentClientSecret
        val paymentIntentId = clientSecret?.let {
            nativePaymentRepository.extractPaymentIntentId(it)
        }

        if (paymentIntentId == null) {
            // Cannot extract a PI id — we have nothing to persist or
            // report, and the webhook path is our fallback.  Clear
            // any stale record and let the user see the post-payment
            // screen with the standard "finalizing" copy.
            Timber.w("Could not extract PaymentIntent ID; relying on webhook fallback")
            pendingPaymentStore.clear()
            _uiState.update {
                it.copy(
                    status = CheckoutStatus.SUCCEEDED,
                    isConfirmingBooking = false,
                    pendingPaymentIntentId = null,
                )
            }
            return
        }

        // Bump the persisted retry counter so process-death recovery
        // can see the same value the in-memory counter will show
        // after the result lands.
        pendingPaymentStore.recordRetryAttempt()
        Timber.d("confirm-booking started for $paymentIntentId")

        when (val result = nativePaymentRepository.confirmBooking(paymentIntentId)) {
            is ApiResult.Success -> {
                val bookingId = result.data.booking?.id
                if (bookingId != null) {
                    cacheConfirmedBooking(bookingId, result.data)
                }
                Timber.d("confirm-booking succeeded, bookingId=$bookingId")
                // Booking is live on the backend — we no longer need
                // the pending-payment recovery record.
                pendingPaymentStore.clear()
                currentClientSecret = null
                currentQuoteId = null
                _uiState.update {
                    it.copy(
                        status = CheckoutStatus.SUCCEEDED,
                        bookingId = bookingId,
                        isConfirmingBooking = false,
                        hasExhaustedRetries = false,
                        pendingPaymentIntentId = paymentIntentId,
                    )
                }
                if (bookingId != null) {
                    _effects.send(Effect.NavigateToConfirmation(bookingId))
                }
            }
            is ApiResult.Failure -> {
                // Payment succeeded but confirm call failed.
                // Show success with retry option instead of infinite spinner.
                val retryCount = _uiState.value.confirmRetryCount + 1
                val errorMessage = result.error.message
                Timber.w("confirm-booking failed (attempt $retryCount): $errorMessage")
                val exhausted = retryCount >= MAX_CONFIRM_RETRIES
                _uiState.update {
                    it.copy(
                        status = CheckoutStatus.SUCCEEDED,
                        isConfirmingBooking = false,
                        confirmRetryCount = retryCount,
                        pendingPaymentIntentId = paymentIntentId,
                        hasExhaustedRetries = exhausted,
                    )
                }
                if (exhausted) {
                    sendFailureReport(
                        paymentIntentId = paymentIntentId,
                        errorMessage = errorMessage ?: "confirm-booking failed",
                    )
                }
            }
        }
    }

    /** Manually retry confirm-booking (called from the UI retry button). */
    fun retryConfirmBooking() {
        if (_uiState.value.confirmRetryCount >= MAX_CONFIRM_RETRIES) return
        viewModelScope.launch { confirmBookingOnBackend() }
    }

    // ── Recovery & failure reporting ─────────────────────────────

    /**
     * Called from `init`.  If a persisted record exists with
     * `paymentSucceededLocally == true`, hydrate in-memory state and
     * auto-retry confirm-booking exactly once.  If the persisted
     * retryCount is already at MAX, skip the retry, flip the stuck
     * UI immediately, and post the failure report.
     */
    private fun attemptRecoveryIfNeeded() {
        val record = pendingPaymentStore.load() ?: return
        if (!record.paymentSucceededLocally) {
            // Sheet was abandoned before it reported — leave the
            // record in place; a fresh submitPayment overwrites it.
            return
        }
        val piId = record.paymentIntentId
            ?: record.clientSecret.let { nativePaymentRepository.extractPaymentIntentId(it) }
        if (piId == null) {
            // Corrupt record — clear it and let the normal flow run.
            pendingPaymentStore.clear()
            return
        }

        Timber.d("Recovering pending payment pi=$piId retryCount=${record.retryCount}")

        currentClientSecret = record.clientSecret
        currentQuoteId = record.quoteId
        _uiState.update {
            it.copy(
                status = CheckoutStatus.SUCCEEDED,
                pendingPaymentIntentId = piId,
                confirmRetryCount = record.retryCount,
                hasExhaustedRetries = record.retryCount >= MAX_CONFIRM_RETRIES,
                isConfirmingBooking = false,
            )
        }

        viewModelScope.launch {
            if (record.retryCount >= MAX_CONFIRM_RETRIES) {
                // Already exhausted before the process restart — post
                // a report so the backend knows we're blocked and the
                // admin can reconcile.
                sendFailureReport(
                    paymentIntentId = piId,
                    errorMessage = "Retry exhausted before process restart",
                )
                return@launch
            }
            // Auto-retry exactly once per recovery.  The user can
            // continue tapping Retry within the remaining budget.
            confirmBookingOnBackend()
        }
    }

    /**
     * POST /payments/native/report-failure.  Best-effort — when the
     * backend answers `alreadyBooked == true`, the booking was
     * eventually created (usually by the Stripe webhook) and we can
     * clear local state + surface the booking id.
     */
    private suspend fun sendFailureReport(paymentIntentId: String, errorMessage: String) {
        val quoteId = currentQuoteId ?: pendingPaymentStore.load()?.quoteId
        val retryCount = _uiState.value.confirmRetryCount
        val result = nativePaymentRepository.reportFailure(
            paymentIntentId = paymentIntentId,
            quoteId = quoteId,
            retryCount = retryCount,
            errorMessage = errorMessage,
            errorCode = "CLIENT_CONFIRM_BOOKING_FAILED",
        )
        when (result) {
            is ApiResult.Success -> {
                val response = result.data
                if (response.alreadyBooked == true) {
                    // Webhook already created the booking. Surface the
                    // booking id but mark it as server-recovered so the
                    // UI can distinguish it from a normal client-confirmed
                    // booking. Do NOT auto-navigate — let the user
                    // acknowledge the recovery state first.
                    val bookingId = response.bookingId
                    pendingPaymentStore.clear()
                    currentClientSecret = null
                    currentQuoteId = null
                    _uiState.update {
                        it.copy(
                            bookingId = bookingId ?: it.bookingId,
                            hasExhaustedRetries = false,
                            isRecoveredByServer = true,
                        )
                    }
                }
            }
            is ApiResult.Failure -> {
                // Reporting itself failed — log and leave the pending
                // record in place so a future launch can try again.
                Timber.w("report-failure call failed: ${result.error.message}")
            }
        }
    }

    companion object {
        /** Public so the UI can reference the bound for the retry button. */
        const val MAX_CONFIRM_RETRIES = 3
    }

    private suspend fun cacheConfirmedBooking(bookingId: String, response: ConfirmBookingResponse) {
        try {
            val data = response.booking ?: return
            val booking = BookingModel(
                id = bookingId,
                propertyName = data.title ?: "",
                startDate = data.startDate ?: "",
                endDate = data.endDate ?: "",
                totalPrice = data.priceTotal ?: 0.0,
                price = (data.priceTotal ?: 0.0).toString(),
                status = BookingStatus.fromString(data.status ?: "confirmed"),
                bookingStatus = data.status ?: "confirmed",
                checkInTime = data.startDate ?: "",
                checkOutTime = data.endDate ?: "",
                currency = "USD"
            )
            coreBookingRepository.cacheBooking(booking)
            Timber.d("Cached confirmed booking $bookingId to Room")
        } catch (e: Exception) {
            Timber.w(e, "Failed to cache confirmed booking to Room")
        }
    }

    fun retryQuote() {
        val draft = _uiState.value.draft ?: return
        fetchQuote(draft)
    }
}
