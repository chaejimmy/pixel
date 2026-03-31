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
    val bookingId: String? = null
)

@HiltViewModel
class CheckoutViewModel @Inject constructor(
    private val apiClient: ApiClient,
    private val appConfig: AppConfig,
    private val json: Json,
    private val nativePaymentRepository: NativePaymentRepository,
    private val coreBookingRepository: CoreBookingRepository
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

    fun setDraft(draft: BookingDraft) {
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

    // ── Step 2: Create PaymentIntent & Present Sheet ──

    fun submitPayment() {
        val quote = _uiState.value.quote ?: return
        if (_uiState.value.status == CheckoutStatus.PROCESSING ||
            _uiState.value.status == CheckoutStatus.SUCCEEDED) return

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

                    _uiState.update {
                        it.copy(
                            paymentSheetConfig = config,
                            publishableKey = stripeKey
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
        viewModelScope.launch {
            confirmBookingOnBackend()
        }
    }

    fun onPaymentSheetCancelled() {
        // User cancelled — allow retry
        _uiState.update { it.copy(status = CheckoutStatus.READY, errorMessage = null) }
    }

    fun onPaymentSheetFailed(errorMessage: String) {
        _uiState.update {
            it.copy(status = CheckoutStatus.FAILED, errorMessage = errorMessage)
        }
    }

    // ── Step 4: Confirm booking on backend after successful payment ──

    private suspend fun confirmBookingOnBackend() {
        val clientSecret = currentClientSecret
        val paymentIntentId = clientSecret?.let {
            nativePaymentRepository.extractPaymentIntentId(it)
        }

        if (paymentIntentId == null) {
            // Fallback: mark as succeeded — webhook will handle booking creation
            Timber.w("Could not extract PaymentIntent ID; relying on webhook fallback")
            _uiState.update { it.copy(status = CheckoutStatus.SUCCEEDED) }
            return
        }

        when (val result = nativePaymentRepository.confirmBooking(paymentIntentId)) {
            is ApiResult.Success -> {
                val bookingId = result.data.booking?.id
                if (bookingId != null) {
                    cacheConfirmedBooking(bookingId, result.data)
                }
                _uiState.update {
                    it.copy(status = CheckoutStatus.SUCCEEDED, bookingId = bookingId)
                }
                if (bookingId != null) {
                    _effects.send(Effect.NavigateToConfirmation(bookingId))
                }
            }
            is ApiResult.Failure -> {
                // Payment succeeded but confirm call failed — still show success.
                // The webhook fallback will create the booking.
                Timber.w("confirm-booking call failed (webhook fallback): ${result.error.message}")
                _uiState.update { it.copy(status = CheckoutStatus.SUCCEEDED) }
            }
        }
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
