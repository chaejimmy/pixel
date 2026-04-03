package com.shourov.apps.pacedream.feature.host.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shourov.apps.pacedream.feature.host.data.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@HiltViewModel
class ListingCalendarViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val hostRepository: HostRepository
) : ViewModel() {

    private val listingId: String = savedStateHandle["listingId"] ?: ""

    private val _uiState = MutableStateFlow(ListingCalendarUiState(listingId = listingId))
    val uiState: StateFlow<ListingCalendarUiState> = _uiState.asStateFlow()

    // In-memory storage for blocked ranges (per listing)
    private val blockedRanges = mutableListOf<BlockedTimeRange>()

    // Cached bookings for this listing
    private val listingBookings = mutableListOf<HostBookingDTO>()

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.US)

    init {
        val cal = Calendar.getInstance()
        val today = dateFormat.format(cal.time)
        _uiState.value = _uiState.value.copy(
            selectedDate = today,
            currentMonth = cal.get(Calendar.MONTH),
            currentYear = cal.get(Calendar.YEAR)
        )
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                // Load host bookings and filter for this listing
                val dashResult = hostRepository.loadDashboard()
                val allBookings = dashResult.bookings
                val listingName = dashResult.listings.find { it.id == listingId }?.title ?: ""

                // Filter bookings for this listing
                listingBookings.clear()
                listingBookings.addAll(
                    allBookings.filter { booking ->
                        booking.listing?.id == listingId ||
                            booking.resolvedListingTitle.equals(listingName, ignoreCase = true)
                    }
                )

                _uiState.value = _uiState.value.copy(
                    listingTitle = listingName,
                    bookings = listingBookings,
                    isLoading = false,
                    error = null
                )
                rebuildDaySlots()
                rebuildEventDates()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load calendar data"
                )
            }
        }
    }

    fun selectDate(date: String) {
        _uiState.value = _uiState.value.copy(selectedDate = date)
        rebuildDaySlots()
    }

    fun changeMonth(month: Int, year: Int) {
        _uiState.value = _uiState.value.copy(currentMonth = month, currentYear = year)
    }

    // ── Block Time Flow ────────────────────────────────────────

    fun showBlockTimeSheet() {
        _uiState.value = _uiState.value.copy(
            showBlockTimeSheet = true,
            blockStartTime = "09:00",
            blockEndTime = "17:00",
            blockReason = ""
        )
    }

    fun dismissBlockTimeSheet() {
        _uiState.value = _uiState.value.copy(showBlockTimeSheet = false)
    }

    fun updateBlockStartTime(time: String) {
        _uiState.value = _uiState.value.copy(blockStartTime = time)
    }

    fun updateBlockEndTime(time: String) {
        _uiState.value = _uiState.value.copy(blockEndTime = time)
    }

    fun updateBlockReason(reason: String) {
        _uiState.value = _uiState.value.copy(blockReason = reason)
    }

    fun saveBlockedTime() {
        val state = _uiState.value
        if (state.blockStartTime.isBlank() || state.blockEndTime.isBlank()) return

        // Check for overlaps with existing bookings
        if (hasOverlap(state.selectedDate, state.blockStartTime, state.blockEndTime)) {
            _uiState.value = state.copy(error = "Cannot block time that overlaps with a booking")
            return
        }

        val blocked = BlockedTimeRange(
            listingId = listingId,
            date = state.selectedDate,
            startTime = state.blockStartTime,
            endTime = state.blockEndTime,
            reason = state.blockReason.ifBlank { "Blocked by host" }
        )
        blockedRanges.add(blocked)

        _uiState.value = state.copy(
            showBlockTimeSheet = false,
            blockedRanges = blockedRanges.toList()
        )
        rebuildDaySlots()
        rebuildEventDates()
    }

    fun removeBlockedTime(blockId: String) {
        blockedRanges.removeAll { it.id == blockId }
        _uiState.value = _uiState.value.copy(blockedRanges = blockedRanges.toList())
        rebuildDaySlots()
        rebuildEventDates()
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    // ── Slot Generation ────────────────────────────────────────

    private fun rebuildDaySlots() {
        val selectedDate = _uiState.value.selectedDate
        if (selectedDate.isBlank()) return

        val slots = mutableListOf<CalendarTimeSlot>()

        // Generate hourly slots from 00:00 to 23:00
        for (hour in 0..23) {
            val startTime = String.format("%02d:00", hour)
            val endTime = String.format("%02d:00", (hour + 1) % 24)
            val endLabel = if (hour == 23) "00:00" else endTime

            // Check if this slot is booked
            val matchingBooking = findBookingForSlot(selectedDate, startTime)
            // Check if this slot is blocked
            val matchingBlock = findBlockForSlot(selectedDate, startTime)

            val status: TimeSlotStatus
            val label: String
            val bookingId: String?

            when {
                matchingBooking != null -> {
                    status = TimeSlotStatus.BOOKED
                    label = matchingBooking.resolvedGuestName.ifBlank { "Booked" }
                    bookingId = matchingBooking.id
                }
                matchingBlock != null -> {
                    status = TimeSlotStatus.BLOCKED
                    label = matchingBlock.reason
                    bookingId = null
                }
                else -> {
                    status = TimeSlotStatus.AVAILABLE
                    label = "Available"
                    bookingId = null
                }
            }

            slots.add(
                CalendarTimeSlot(
                    id = "$selectedDate-$startTime",
                    startTime = startTime,
                    endTime = endLabel,
                    status = status,
                    label = label,
                    bookingId = bookingId
                )
            )
        }

        _uiState.value = _uiState.value.copy(timeSlots = slots)
    }

    private fun findBookingForSlot(date: String, slotStart: String): HostBookingDTO? {
        val slotMinutes = timeToMinutes(slotStart)
        return listingBookings.find { booking ->
            val bookingDate = extractDate(booking.resolvedStart)
            if (bookingDate != date) return@find false
            val bStart = extractTime(booking.resolvedStart)
            val bEnd = extractTime(booking.resolvedEnd)
            if (bStart == null || bEnd == null) return@find false
            val bStartMin = timeToMinutes(bStart)
            val bEndMin = timeToMinutes(bEnd)
            slotMinutes in bStartMin until bEndMin
        }
    }

    private fun findBlockForSlot(date: String, slotStart: String): BlockedTimeRange? {
        val slotMinutes = timeToMinutes(slotStart)
        return blockedRanges.find { block ->
            block.date == date &&
                slotMinutes >= timeToMinutes(block.startTime) &&
                slotMinutes < timeToMinutes(block.endTime)
        }
    }

    private fun hasOverlap(date: String, startTime: String, endTime: String): Boolean {
        val newStart = timeToMinutes(startTime)
        val newEnd = timeToMinutes(endTime)
        return listingBookings.any { booking ->
            val bookingDate = extractDate(booking.resolvedStart)
            if (bookingDate != date) return@any false
            val bStart = extractTime(booking.resolvedStart) ?: return@any false
            val bEnd = extractTime(booking.resolvedEnd) ?: return@any false
            val bStartMin = timeToMinutes(bStart)
            val bEndMin = timeToMinutes(bEnd)
            newStart < bEndMin && newEnd > bStartMin
        }
    }

    private fun rebuildEventDates() {
        val dates = mutableSetOf<String>()
        // Dates with bookings
        listingBookings.forEach { booking ->
            extractDate(booking.resolvedStart)?.let { dates.add(it) }
        }
        // Dates with blocks
        blockedRanges.forEach { dates.add(it.date) }
        _uiState.value = _uiState.value.copy(datesWithEvents = dates)
    }

    // ── Date/Time Helpers ──────────────────────────────────────

    private fun timeToMinutes(time: String): Int {
        val parts = time.split(":")
        if (parts.size < 2) return 0
        return (parts[0].toIntOrNull() ?: 0) * 60 + (parts[1].toIntOrNull() ?: 0)
    }

    private fun extractDate(dateTimeStr: String?): String? {
        if (dateTimeStr.isNullOrBlank()) return null
        // Try ISO format first: "2026-04-03T09:00:00..."
        return dateTimeStr.take(10).takeIf { it.length == 10 && it[4] == '-' }
    }

    private fun extractTime(dateTimeStr: String?): String? {
        if (dateTimeStr.isNullOrBlank()) return null
        val tIndex = dateTimeStr.indexOf('T')
        if (tIndex >= 0 && dateTimeStr.length >= tIndex + 6) {
            return dateTimeStr.substring(tIndex + 1, tIndex + 6)
        }
        return null
    }
}
