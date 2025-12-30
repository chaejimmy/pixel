package com.shourov.apps.pacedream.feature.bookingdetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shourov.apps.pacedream.core.common.result.Result
import com.shourov.apps.pacedream.core.data.repository.BookingRepository
import com.shourov.apps.pacedream.model.BookingModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
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
            _uiState.value = BookingDetailUiState(isLoading = false, booking = null, error = "Missing booking id")
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            bookingRepository.getBookingById(bookingId).collectLatest { result ->
                when (result) {
                    is Result.Success -> _uiState.value = BookingDetailUiState(
                        isLoading = false,
                        booking = result.data,
                        error = if (result.data == null) "Booking not found" else null
                    )
                    is Result.Error -> _uiState.value = BookingDetailUiState(
                        isLoading = false,
                        booking = null,
                        error = result.exception.message ?: "Failed to load booking"
                    )
                    is Result.Loading -> _uiState.value = _uiState.value.copy(isLoading = true)
                }
            }
        }
    }
}

