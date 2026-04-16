package com.shourov.apps.pacedream.feature.webflow.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shourov.apps.pacedream.core.data.repository.BookingRepository as CoreBookingRepository
import com.shourov.apps.pacedream.core.network.api.ApiResult
import com.shourov.apps.pacedream.feature.webflow.data.BookingConfirmation
import com.shourov.apps.pacedream.feature.webflow.data.BookingRepository
import com.shourov.apps.pacedream.feature.webflow.data.BookingType
import com.shourov.apps.pacedream.model.BookingModel
import com.shourov.apps.pacedream.model.BookingStatus
import com.pacedream.common.util.UserFacingErrorMapper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class BookingConfirmationViewModel @Inject constructor(
    private val bookingRepository: BookingRepository,
    private val coreBookingRepository: CoreBookingRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<BookingConfirmationUiState>(
        BookingConfirmationUiState.Idle
    )
    val uiState: StateFlow<BookingConfirmationUiState> = _uiState.asStateFlow()

    /**
     * Confirm booking based on session ID and booking type
     */
    fun confirmBooking(sessionId: String, bookingTypeString: String) {
        // Prevent double-tap — ignore if already in-flight loading
        val current = _uiState.value
        if (current is BookingConfirmationUiState.Loading) return
        viewModelScope.launch {
            _uiState.value = BookingConfirmationUiState.Loading

            val bookingType = try {
                BookingType.valueOf(bookingTypeString.uppercase())
            } catch (e: Exception) {
                // Default to time-based if unknown
                BookingType.TIME_BASED
            }

            Timber.d("Confirming booking: sessionId=$sessionId, type=$bookingType")

            val result = when (bookingType) {
                BookingType.TIME_BASED -> bookingRepository.confirmTimeBasedBooking(sessionId)
                BookingType.GEAR -> bookingRepository.confirmGearBooking(sessionId)
            }

            _uiState.value = when (result) {
                is ApiResult.Success -> {
                    // Cache the confirmed booking to Room so BookingDetailView can find it
                    cacheConfirmedBooking(result.data)
                    BookingConfirmationUiState.Success(result.data)
                }
                is ApiResult.Failure -> BookingConfirmationUiState.Error(
                    UserFacingErrorMapper.forBookingConfirm(result.error)
                )
            }
        }
    }

    /**
     * Save the confirmed booking to the local Room cache so it's immediately
     * available when the user navigates to booking detail.
     */
    private suspend fun cacheConfirmedBooking(confirmation: BookingConfirmation) {
        if (confirmation.bookingId.isBlank()) return
        try {
            val booking = BookingModel(
                id = confirmation.bookingId,
                propertyName = confirmation.itemTitle ?: "",
                startDate = confirmation.startDate ?: "",
                endDate = confirmation.endDate ?: "",
                totalPrice = confirmation.amount ?: 0.0,
                price = confirmation.amount?.toString(),
                status = BookingStatus.fromString(confirmation.status),
                bookingStatus = confirmation.status,
                checkInTime = confirmation.startDate,
                checkOutTime = confirmation.endDate,
                currency = "USD"
            )
            coreBookingRepository.cacheBooking(booking)
            Timber.d("Cached confirmed booking ${confirmation.bookingId} to Room")
        } catch (e: Exception) {
            Timber.w(e, "Failed to cache confirmed booking to Room")
        }
    }
}

/**
 * UI State for booking confirmation
 */
sealed class BookingConfirmationUiState {
    /** Initial state before confirmBooking() is called. */
    object Idle : BookingConfirmationUiState()
    object Loading : BookingConfirmationUiState()
    data class Success(val confirmation: BookingConfirmation) : BookingConfirmationUiState()
    data class Error(val message: String) : BookingConfirmationUiState()
}
