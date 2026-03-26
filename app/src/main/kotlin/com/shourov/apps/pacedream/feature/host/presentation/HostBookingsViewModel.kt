package com.shourov.apps.pacedream.feature.host.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shourov.apps.pacedream.feature.host.data.BookingSegment
import com.shourov.apps.pacedream.feature.host.data.HostBookingDTO
import com.shourov.apps.pacedream.feature.host.data.HostBookingsData
import com.shourov.apps.pacedream.feature.host.data.HostRepository
import com.shourov.apps.pacedream.feature.host.data.parseDate
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Host Bookings ViewModel - iOS parity.
 *
 * Matches iOS HostBookingsView with segments: Pending, Confirmed, Past, Cancelled.
 * Uses PATCH /bookings/host/:id with { status: "accepted" | "declined" | "cancelled" }.
 */
@HiltViewModel
class HostBookingsViewModel @Inject constructor(
    private val hostRepository: HostRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HostBookingsData())
    val uiState: StateFlow<HostBookingsData> = _uiState.asStateFlow()

    private var allBookings: List<HostBookingDTO> = emptyList()

    init {
        loadHostBookings()
    }

    private fun loadHostBookings() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            hostRepository.getHostBookings()
                .onSuccess { bookings ->
                    allBookings = bookings
                    applyFilter()
                }
                .onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = exception.message ?: "Failed to load bookings"
                    )
                }
        }
    }

    fun updateStatus(status: String) {
        _uiState.value = _uiState.value.copy(selectedStatus = status)
        applyFilter()
    }

    private fun applyFilter() {
        val filtered = when (_uiState.value.selectedStatus) {
            "Pending" -> allBookings.filter { segmentFor(it) == BookingSegment.PENDING }
            "Confirmed" -> allBookings.filter { segmentFor(it) == BookingSegment.CONFIRMED }
            "Past" -> allBookings.filter { segmentFor(it) == BookingSegment.PAST }
            "Cancelled" -> allBookings.filter { segmentFor(it) == BookingSegment.CANCELLED }
            else -> allBookings
        }
        _uiState.value = _uiState.value.copy(
            totalBookings = allBookings.size,
            pendingBookings = allBookings.count { segmentFor(it) == BookingSegment.PENDING },
            bookings = filtered,
            isLoading = false,
            error = null
        )
    }

    /** iOS parity: HostBookingStatusMapper.segment */
    private fun segmentFor(booking: HostBookingDTO): BookingSegment {
        val s = (booking.status ?: "").trim().lowercase()
        if (s.isEmpty()) return BookingSegment.PENDING

        // Cancelled
        if (s.contains("cancel") || s == "declined") return BookingSegment.CANCELLED

        // Pending
        if (s.contains("pending") || s == "created" || s == "requires_capture" || s == "pending_host") {
            return BookingSegment.PENDING
        }

        // Confirmed but check if past
        if (s.contains("confirm") || s.contains("accept") || s.contains("active") || s == "booked") {
            val endMs = parseDate(booking.resolvedEnd)
            if (endMs != null && endMs < System.currentTimeMillis()) {
                return BookingSegment.PAST
            }
            return BookingSegment.CONFIRMED
        }

        // Completed
        if (s.contains("complete") || s.contains("checked_out")) return BookingSegment.PAST

        return BookingSegment.PENDING
    }

    fun acceptBooking(bookingId: String) {
        viewModelScope.launch {
            hostRepository.acceptBooking(bookingId)
                .onSuccess { refreshData() }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(
                        error = e.message ?: "Failed to accept booking"
                    )
                }
        }
    }

    fun declineBooking(bookingId: String) {
        viewModelScope.launch {
            hostRepository.declineBooking(bookingId)
                .onSuccess { refreshData() }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(
                        error = e.message ?: "Failed to decline booking"
                    )
                }
        }
    }

    fun cancelBooking(bookingId: String, reason: String? = null) {
        viewModelScope.launch {
            hostRepository.cancelBooking(bookingId, reason)
                .onSuccess { refreshData() }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(
                        error = e.message ?: "Failed to cancel booking"
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
