package com.pacedream.app.feature.checkout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pacedream.app.core.config.AppConfig
import com.pacedream.app.core.network.ApiClient
import com.pacedream.app.core.network.ApiError
import com.pacedream.app.core.network.ApiResult
import com.shourov.apps.pacedream.core.data.repository.BookingRepository as CoreBookingRepository
import com.shourov.apps.pacedream.model.BookingModel
import com.shourov.apps.pacedream.model.BookingStatus
import com.pacedream.common.util.UserFacingErrorMapper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.Json
import timber.log.Timber
import java.util.UUID
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
    /**
     * X-Request-ID of the most recent confirm-booking attempt.  Surfaced
     * in the stuck-fallback support UI so a user-reported issue can be
     * traced back to a specific server log entry.  Null until the first
     * confirm attempt for the current PaymentIntent.
     */
    val lastConfirmRequestId: String? = null,
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
    /**
     * Idempotency-Key reused across every confirm-booking attempt for the
     * current PaymentIntent — including manual retries and post-launch
     * crash-recovery retries.  Hydrated from [PendingPaymentStore] on
     * recovery so a freshly-restarted process does NOT generate a new key.
     */
    private var currentConfirmIdempotencyKey: String? = null
    /**
     * Idempotency-Key for the in-flight quote → PaymentIntent pair.
     * Generated once per user "Pay" tap so a transient crash before the
     * pending record is committed still ties any retry to the same
     * PaymentIntent on the backend.
     */
    private var currentPaymentIntentIdempotencyKey: String? = null

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
                quantity = draft.guests,
                // One key per draft — tapping retry on the same draft reuses it
                // so the backend de-duplicates the quote row.  Re-derived from
                // the stable booking input; resilient to concurrent re-fetches.
                idempotencyKey = quoteIdempotencyKey(draft),
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
                            errorMessage = UserFacingErrorMapper.map(result.error, "We couldn't get a price quote. Please try again.")
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

            // ── Step 0: Availability pre-check ───────────────────────
            // Guards against the race where another guest books the same
            // slot between quote creation and PaymentIntent capture.  We
            // still let the backend enforce on createPaymentIntent — this
            // is a pre-flight UX check so we don't charge a card for a
            // slot that is already gone.  Failures here are logged and
            // we proceed (defense-in-depth) because the payment/booking
            // paths validate again server-side.
            val draft = _uiState.value.draft
            if (draft != null) {
                val startIso = draft.startTimeISO.ensureUtcSuffix()
                val endIso = draft.endTimeISO.ensureUtcSuffix()
                when (val avail = coreBookingRepository.checkAvailability(
                    listingId = draft.listingId,
                    startDate = startIso,
                    endDate = endIso,
                )) {
                    is com.shourov.apps.pacedream.core.common.result.Result.Success -> {
                        val result = avail.data
                        if (!result.available || !result.listingBookable) {
                            val reason = if (!result.listingBookable)
                                "This listing is not currently accepting bookings."
                            else
                                result.displayReason
                            _uiState.update {
                                it.copy(
                                    status = CheckoutStatus.FAILED,
                                    errorMessage = reason,
                                )
                            }
                            return@launch
                        }
                    }
                    is com.shourov.apps.pacedream.core.common.result.Result.Error -> {
                        // Log and continue — backend will enforce on
                        // PaymentIntent creation / confirm-booking.
                        Timber.w(avail.exception, "Pre-flight availability check failed, continuing")
                    }
                    is com.shourov.apps.pacedream.core.common.result.Result.Loading -> { /* n/a */ }
                }
            }

            // Mint a fresh idempotency key for this Pay tap.  Used for the
            // PaymentIntent POST and persisted with the pending record so
            // both retries and process-death recovery reuse the same key.
            val piIdempotencyKey = UUID.randomUUID().toString()
            currentPaymentIntentIdempotencyKey = piIdempotencyKey
            // confirm-booking key is also pre-minted so it can be persisted
            // BEFORE the Stripe sheet opens — crash recovery needs the same
            // key the original attempt would have used.
            val confirmKey = UUID.randomUUID().toString()
            currentConfirmIdempotencyKey = confirmKey

            // Create PaymentIntent on backend
            when (val result = nativePaymentRepository.createPaymentIntent(
                quoteId = quote.quoteId,
                idempotencyKey = piIdempotencyKey,
            )) {
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
                            confirmIdempotencyKey = confirmKey,
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
                            errorMessage = UserFacingErrorMapper.map(result.error, "We couldn't set up your payment. Please try again.")
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
        currentConfirmIdempotencyKey = null
        currentPaymentIntentIdempotencyKey = null
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
        // errorMessage has already been mapped through StripeErrorMapper at the
        // UI layer; route it through UserFacingErrorMapper as a safety net in
        // case a caller passes a raw string.
        Timber.e("PaymentSheet failed: $errorMessage")
        pendingPaymentStore.clear()
        currentClientSecret = null
        currentQuoteId = null
        currentConfirmIdempotencyKey = null
        currentPaymentIntentIdempotencyKey = null
        _uiState.update {
            it.copy(
                status = CheckoutStatus.FAILED,
                errorMessage = com.pacedream.common.util.StripeErrorMapper.mapPaymentSheetMessage(
                    errorMessage,
                    fallback = "Your payment couldn't be completed. Please try again."
                ),
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

        // Reuse a previously-persisted idempotency key when present so a
        // crash-recovery retry hits the SAME backend dedup slot as the
        // original attempt and either gets the existing booking back or
        // races safely with the original write.  Only mint a new key when
        // no record exists (e.g. a stale in-memory state with no pending
        // store entry).
        val confirmKey = currentConfirmIdempotencyKey
            ?: pendingPaymentStore.load()?.confirmIdempotencyKey
            ?: UUID.randomUUID().toString().also {
                currentConfirmIdempotencyKey = it
            }
        currentConfirmIdempotencyKey = confirmKey

        // Bump the persisted retry counter so process-death recovery
        // can see the same value the in-memory counter will show
        // after the result lands.
        pendingPaymentStore.recordRetryAttempt()

        // Mint a fresh X-Request-ID per attempt and surface it in state
        // so the support fallback UI can show the user a copy-able token
        // for the call that just failed.
        val requestId = UUID.randomUUID().toString()
        _uiState.update { it.copy(lastConfirmRequestId = requestId) }
        Timber.d("confirm-booking started pi=$paymentIntentId key=$confirmKey reqId=$requestId")

        // Wrap the call in an explicit timeout so the "Finalizing booking…"
        // spinner can never hang indefinitely (network stalls beyond the
        // OkHttp read budget would otherwise leave the user stuck).  On
        // timeout we treat the call as a transient failure and route the
        // user into the same retry/recovery branch we use for genuine
        // failures — payment IS captured, so we never flip to FAILED.
        val result: ApiResult<ConfirmBookingResponse> = try {
            withTimeout(CONFIRM_BOOKING_TIMEOUT_MS) {
                nativePaymentRepository.confirmBooking(
                    paymentIntentId = paymentIntentId,
                    idempotencyKey = confirmKey,
                    requestId = requestId,
                )
            }
        } catch (e: TimeoutCancellationException) {
            Timber.w("confirm-booking timed out after ${CONFIRM_BOOKING_TIMEOUT_MS}ms pi=$paymentIntentId reqId=$requestId")
            ApiResult.Failure(ApiError.NetworkTimeout)
        }

        when (result) {
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
                currentConfirmIdempotencyKey = null
                currentPaymentIntentIdempotencyKey = null
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
                // Payment succeeded but confirm call failed (or timed out).
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
        // Critical: rehydrate the persisted Idempotency-Key so the
        // recovery retry hits the SAME backend dedup slot as the original
        // confirm call.  Without this, recovery would generate a fresh
        // key and the backend could create a duplicate booking.
        currentConfirmIdempotencyKey = record.confirmIdempotencyKey
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
                    currentConfirmIdempotencyKey = null
                    currentPaymentIntentIdempotencyKey = null
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

        /**
         * Hard ceiling on a single confirm-booking attempt before we treat
         * the call as a transient failure and fall back to the recovery
         * UI.  Slightly larger than the OkHttp read timeout (30s) so a
         * slow-but-eventually-successful backend response still wins; we
         * only kick in when the call is truly stalled.
         */
        const val CONFIRM_BOOKING_TIMEOUT_MS = 45_000L
    }

    /**
     * Stable per-draft idempotency key for the quote POST.  Same draft
     * → same key (so a retry after a transient failure doesn't create a
     * second quote row).  Different draft (different listing / dates /
     * guests) → different key.
     */
    private fun quoteIdempotencyKey(draft: BookingDraft): String {
        val raw = listOf(
            draft.listingId,
            draft.listingType,
            draft.startTimeISO,
            draft.endTimeISO,
            draft.guests.toString(),
        ).joinToString("|")
        // Stable v3-ish UUID derived from the draft so the key is the same
        // across launches if the user comes back to the same draft.
        return UUID.nameUUIDFromBytes(raw.toByteArray(Charsets.UTF_8)).toString()
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

    /**
     * BookingDraft stores ISO times as "yyyy-MM-ddTHH:mm:ss" (no zone).
     * The backend check-availability endpoint expects a full ISO-8601 UTC
     * string, so append "Z" when no zone marker is already present.  This
     * matches the format used by BookingFormViewModel.createBooking().
     */
    private fun String.ensureUtcSuffix(): String {
        if (endsWith("Z", ignoreCase = true)) return this
        // Detect a trailing "+HH:MM" or "-HH:MM" offset after the time
        // portion (positions 10+ skip the "yyyy-MM-dd" date separators).
        val tail = if (length > 10) substring(10) else this
        if (tail.contains('+') || tail.contains('-')) return this
        return "${this}Z"
    }
}
