package com.shourov.apps.pacedream.feature.bookingdetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pacedream.common.util.UserFacingErrorMapper
import com.shourov.apps.pacedream.core.common.result.Result
import com.shourov.apps.pacedream.core.data.repository.BookingRepository
import com.shourov.apps.pacedream.model.BookingModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

data class BookingDetailUiState(
    val isLoading: Boolean = true,
    val booking: BookingModel? = null,
    val error: String? = null
)

@HiltViewModel
class BookingDetailViewModel @Inject constructor(
    private val bookingRepository: BookingRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val bookingId: String = savedStateHandle.get<String>("bookingId").orEmpty()

    private val _uiState = MutableStateFlow(BookingDetailUiState())
    val uiState: StateFlow<BookingDetailUiState> = _uiState.asStateFlow()

    init {
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
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)
                bookingRepository.getBookingById(bookingId).collectLatest { result ->
                    when (result) {
                        is Result.Success -> _uiState.value = BookingDetailUiState(
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
                        is Result.Error -> {
                            // Never surface raw exception.message: it can be an
                            // IOException, a server JSON body, or a "Server error
                            // 500: …" string.  Log the technical detail and show
                            // the user a friendly message.
                            Timber.w(result.exception, "BookingDetail load failed")
                            _uiState.value = BookingDetailUiState(
                                isLoading = false,
                                booking = null,
                                error = UserFacingErrorMapper.forLoadBookings(result.exception),
                            )
                        }
                        is Result.Loading -> _uiState.value = _uiState.value.copy(isLoading = true)
                    }
                }
            } catch (e: Exception) {
                Timber.w(e, "BookingDetail load threw")
                _uiState.value = BookingDetailUiState(
                    isLoading = false,
                    booking = null,
                    error = UserFacingErrorMapper.forLoadBookings(e),
                )
            }
        }
    }

    fun cancelBooking() {
        if (bookingId.isBlank()) return
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)
                when (val result = bookingRepository.cancelBooking(bookingId)) {
                    is Result.Success -> {
                        // Reload to reflect cancelled status
                        load()
                    }
                    is Result.Error -> {
                        Timber.w(result.exception, "BookingDetail cancel failed")
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = UserFacingErrorMapper.forBookingCancel(result.exception),
                        )
                    }
                    is Result.Loading -> { /* no-op */ }
                }
            } catch (e: Exception) {
                Timber.w(e, "BookingDetail cancel threw")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = UserFacingErrorMapper.forBookingCancel(e),
                )
            }
        }
    }
}

