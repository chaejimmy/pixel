package com.shourov.apps.pacedream.feature.webflow.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shourov.apps.pacedream.core.network.api.ApiResult
import com.shourov.apps.pacedream.feature.webflow.data.BookingConfirmation
import com.shourov.apps.pacedream.feature.webflow.data.BookingRepository
import com.shourov.apps.pacedream.feature.webflow.data.BookingType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class BookingConfirmationViewModel @Inject constructor(
    private val bookingRepository: BookingRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow<BookingConfirmationUiState>(
        BookingConfirmationUiState.Loading
    )
    val uiState: StateFlow<BookingConfirmationUiState> = _uiState.asStateFlow()
    
    /**
     * Confirm booking based on session ID and booking type
     */
    fun confirmBooking(sessionId: String, bookingTypeString: String) {
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
                is ApiResult.Success -> BookingConfirmationUiState.Success(result.data)
                is ApiResult.Failure -> BookingConfirmationUiState.Error(
                    result.error.message ?: "Failed to confirm booking"
                )
            }
        }
    }
}

/**
 * UI State for booking confirmation
 */
sealed class BookingConfirmationUiState {
    object Loading : BookingConfirmationUiState()
    data class Success(val confirmation: BookingConfirmation) : BookingConfirmationUiState()
    data class Error(val message: String) : BookingConfirmationUiState()
}

