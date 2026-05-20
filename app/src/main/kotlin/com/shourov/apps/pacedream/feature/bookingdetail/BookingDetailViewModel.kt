package com.shourov.apps.pacedream.feature.bookingdetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pacedream.app.feature.checkout.PendingNativePayment
import com.pacedream.app.feature.checkout.PendingPaymentStore
import com.pacedream.app.feature.checkout.ReconciliationScheduler
import com.pacedream.common.util.UserFacingErrorMapper
import com.shourov.apps.pacedream.core.common.result.Result
import com.shourov.apps.pacedream.core.data.repository.BookingRepository
import com.shourov.apps.pacedream.model.BookingModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

data class BookingDetailUiState(
    val isLoading: Boolean = true,
    val booking: BookingModel? = null,
    val error: String? = null,
    /**
     * When the previous app session captured a payment but the booking
     * has not yet been confirmed on the backend, the persisted
     * [PendingNativePayment] is surfaced here.  Drives the
     * "Payment pending" banner above the booking content and the
     * "Check status" CTA that re-fires the reconciliation worker.
     */
    val pendingPayment: PendingNativePayment? = null,
)

@HiltViewModel
class BookingDetailViewModel @Inject constructor(
    private val bookingRepository: BookingRepository,
    private val pendingPaymentStore: PendingPaymentStore,
    private val reconciliationScheduler: ReconciliationScheduler,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val bookingId: String = savedStateHandle.get<String>("bookingId").orEmpty()

    private val _uiState = MutableStateFlow(BookingDetailUiState())
    val uiState: StateFlow<BookingDetailUiState> = _uiState.asStateFlow()

    init {
        refreshPendingPayment()
        load()
    }

    /**
     * Hydrate the "Payment pending" banner state from disk.  Surfaces a
     * captured-but-not-yet-confirmed payment so the user has visibility
     * into the reconciliation worker's progress instead of staring at a
     * booking that may or may not appear.
     */
    fun refreshPendingPayment() {
        val record = pendingPaymentStore.load()
        // Only surface records where Stripe actually captured funds.
        // Sheet-abandoned records (paymentSucceededLocally == false) are
        // harmless and don't deserve a banner.
        val visible = record?.takeIf { it.paymentSucceededLocally }
        _uiState.update { it.copy(pendingPayment = visible) }
    }

    /**
     * Kick the background reconciliation worker and refresh the
     * banner.  Wired to the "Check status" CTA in the
     * "Payment pending" banner so the user can prod reconciliation on
     * demand instead of waiting for the worker's backoff schedule.
     */
    fun checkPendingPaymentStatus() {
        reconciliationScheduler.enqueue()
        refreshPendingPayment()
        // Reload the booking — the worker may have already confirmed it,
        // in which case the next /booking GET resolves the pending state.
        load()
    }

    fun load() {
        if (bookingId.isBlank()) {
            // Defensive: this should not happen if navigation passes a valid id,
            // but keep the message friendly rather than echoing internal state.
            _uiState.value = BookingDetailUiState(
                isLoading = false,
                booking = null,
                error = "We couldn't open this booking. Please try again from your bookings list.",
            )
            return
        }
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true, error = null) }
                bookingRepository.getBookingById(bookingId).collectLatest { result ->
                    when (result) {
                        is Result.Success -> _uiState.update {
                            it.copy(
                                isLoading = false,
                                booking = result.data,
                                // "Not found" is a real product-level state — keep its
                                // copy explicit and friendly.  Anything else routes
                                // through UserFacingErrorMapper.
                                error = if (result.data == null) {
                                    "We couldn't find this booking. It may have been cancelled or removed."
                                } else {
                                    null
                                },
                            )
                        }
                        is Result.Error -> {
                            // Never surface raw exception.message: it can be an
                            // IOException, a server JSON body, or a "Server error
                            // 500: …" string.  Log the technical detail and show
                            // the user a friendly message.
                            Timber.w(result.exception, "BookingDetail load failed")
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    booking = null,
                                    error = UserFacingErrorMapper.forLoadBookings(result.exception),
                                )
                            }
                        }
                        is Result.Loading -> _uiState.update { it.copy(isLoading = true) }
                    }
                }
            } catch (e: Exception) {
                Timber.w(e, "BookingDetail load threw")
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        booking = null,
                        error = UserFacingErrorMapper.forLoadBookings(e),
                    )
                }
            }
        }
    }

    fun cancelBooking() {
        if (bookingId.isBlank()) return
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true, error = null) }
                when (val result = bookingRepository.cancelBooking(bookingId)) {
                    is Result.Success -> {
                        // Reload to reflect cancelled status
                        load()
                    }
                    is Result.Error -> {
                        Timber.w(result.exception, "BookingDetail cancel failed")
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                error = UserFacingErrorMapper.forBookingCancel(result.exception),
                            )
                        }
                    }
                    is Result.Loading -> { /* no-op */ }
                }
            } catch (e: Exception) {
                Timber.w(e, "BookingDetail cancel threw")
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = UserFacingErrorMapper.forBookingCancel(e),
                    )
                }
            }
        }
    }
}

