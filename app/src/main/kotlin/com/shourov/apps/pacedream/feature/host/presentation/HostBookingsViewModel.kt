package com.shourov.apps.pacedream.feature.host.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shourov.apps.pacedream.feature.host.data.HostBookingsData
import com.shourov.apps.pacedream.feature.host.data.HostRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HostBookingsViewModel @Inject constructor(
    private val hostRepository: HostRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(HostBookingsData())
    val uiState: StateFlow<HostBookingsData> = _uiState.asStateFlow()
    
    init {
        loadHostBookings()
    }
    
    private fun loadHostBookings() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            val statusFilter = if (uiState.value.selectedStatus == "All") null else uiState.value.selectedStatus
            hostRepository.getHostBookings(statusFilter)
                .onSuccess { bookings ->
                    val totalBookings = bookings.size
                    val pendingBookings = bookings.count { it.status == com.shourov.apps.pacedream.model.BookingStatus.PENDING }
                    _uiState.value = _uiState.value.copy(
                        totalBookings = totalBookings,
                        pendingBookings = pendingBookings,
                        bookings = bookings,
                        isLoading = false,
                        error = null
                    )
                }
                .onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = exception.message ?: "Unknown error occurred"
                    )
                }
        }
    }
    
    fun updateStatus(status: String) {
        _uiState.value = _uiState.value.copy(selectedStatus = status)
        loadHostBookings()
    }
    
    fun acceptBooking(bookingId: String) {
        viewModelScope.launch {
            hostRepository.updateBookingStatus(bookingId, "Confirmed")
                .onSuccess {
                    refreshData()
                }
                .onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        error = exception.message ?: "Failed to accept booking"
                    )
                }
        }
    }
    
    fun rejectBooking(bookingId: String) {
        viewModelScope.launch {
            hostRepository.updateBookingStatus(bookingId, "Rejected")
                .onSuccess {
                    refreshData()
                }
                .onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        error = exception.message ?: "Failed to reject booking"
                    )
                }
        }
    }
    
    fun cancelBooking(bookingId: String) {
        viewModelScope.launch {
            hostRepository.updateBookingStatus(bookingId, "Cancelled")
                .onSuccess {
                    refreshData()
                }
                .onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        error = exception.message ?: "Failed to cancel booking"
                    )
                }
        }
    }
    
    fun refreshData() {
        loadHostBookings()
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
