package com.shourov.apps.pacedream.feature.host.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shourov.apps.pacedream.feature.host.data.*
import com.shourov.apps.pacedream.model.BookingStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

/**
 * ViewModel for the host listing calendar/availability screen.
 *
 * All availability data is fetched from and mutated via the backend (source of truth).
 * Blocked times are persisted on the backend via POST/DELETE /host/listings/:id/calendar/block.
 * After every mutation, the calendar is re-fetched from the backend to ensure consistency.
 *
 * No availability state is derived or cached locally — what the backend returns is what we show.
 */
@HiltViewModel
class ListingCalendarViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val hostRepository: HostRepository
) : ViewModel() {

    private val listingId: String = savedStateHandle["listingId"] ?: ""

    private val _uiState = MutableStateFlow(ListingCalendarUiState(listingId = listingId))
    val uiState: StateFlow<ListingCalendarUiState> = _uiState.asStateFlow()

    // Backend calendar data — single source of truth
    private var calendarData: ListingCalendarData? = null

    // Listing timezone from backend availability settings
    private var listingTimezone: TimeZone = TimeZone.getTimeZone("America/New_York")

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    init {
        val cal = Calendar.getInstance()
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(cal.time)
        _uiState.value = _uiState.value.copy(
            selectedDate = today,
            currentMonth = cal.get(Calendar.MONTH) + 1, // 1-based month for backend API
            currentYear = cal.get(Calendar.YEAR)
        )
        loadCalendarFromBackend()
    }

    // ── Data Loading (backend source of truth) ─────────────────

    /**
     * Fetch calendar data from backend. This is the ONLY source of availability truth.
     * Called on init, after block create/delete, and on manual refresh.
     */
    private fun loadCalendarFromBackend() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val state = _uiState.value

                // Also fetch listing title from dashboard if we don't have it
                val listingTitle = if (state.listingTitle.isBlank()) {
                    try {
                        val dashResult = hostRepository.loadDashboard()
                        dashResult.listings.find { it.id == listingId }?.title ?: ""
                    } catch (e: Exception) {
                        Timber.d(e, "Failed to load listing title (non-fatal)")
                        ""
                    }
                } else {
                    state.listingTitle
                }

                // Fetch calendar from backend — this is the source of truth
                val result = hostRepository.getListingCalendar(
                    listingId = listingId,
                    month = state.currentMonth,
                    year = state.currentYear
                )

                result.fold(
                    onSuccess = { data ->
                        calendarData = data

                        // Extract listing timezone from availability settings
                        data.availability?.timezone?.let { tz ->
                            listingTimezone = TimeZone.getTimeZone(tz)
                        }

                        _uiState.value = state.copy(
                            listingTitle = listingTitle,
                            listingTimezone = data.availability?.timezone ?: "America/New_York",
                            availableStartTime = data.availability?.startTime ?: "09:00",
                            availableEndTime = data.availability?.endTime ?: "17:00",
                            availableDays = data.availability?.availableDays ?: listOf(1, 2, 3, 4, 5),
                            isLoading = false,
                            error = null
                        )

                        rebuildDaySlots()
                        rebuildEventDates()
                    },
                    onFailure = { error ->
                        Timber.e(error, "Failed to load calendar from backend")
                        _uiState.value = state.copy(
                            listingTitle = listingTitle,
                            isLoading = false,
                            error = error.message ?: "Failed to load calendar"
                        )
                    }
                )
            } catch (e: Exception) {
                Timber.e(e, "Calendar load exception")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load calendar data"
                )
            }
        }
    }

    fun refresh() {
        loadCalendarFromBackend()
    }

    fun selectDate(date: String) {
        _uiState.value = _uiState.value.copy(selectedDate = date)
        rebuildDaySlots()
    }

    fun changeMonth(month: Int, year: Int) {
        _uiState.value = _uiState.value.copy(currentMonth = month, currentYear = year)
        // Re-fetch from backend for the new month
        loadCalendarFromBackend()
    }

    // ── Block Time Flow (persisted via backend API) ────────────

    fun showBlockTimeSheet() {
        _uiState.value = _uiState.value.copy(
            showBlockTimeSheet = true,
            blockStartTime = "09:00",
            blockEndTime = "17:00",
            blockReason = "personal"
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

    /**
     * Save blocked time via backend API.
     * Backend validates no overlap with active bookings (409 on conflict).
     * After success, re-fetch calendar to show backend-confirmed state.
     */
    fun saveBlockedTime() {
        val state = _uiState.value
        if (state.blockStartTime.isBlank() || state.blockEndTime.isBlank()) return
        if (state.isMutating) return // Prevent double-submit

        viewModelScope.launch {
            _uiState.value = state.copy(isMutating = true, error = null)

            val request = BlockTimeRequest(
                startDate = state.selectedDate,
                endDate = state.selectedDate,
                startTime = state.blockStartTime,
                endTime = state.blockEndTime,
                reason = mapBlockReason(state.blockReason),
                repeat = "none"
            )

            val result = hostRepository.blockListingTime(listingId, request)

            result.fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(
                        showBlockTimeSheet = false,
                        isMutating = false
                    )
                    // Re-fetch from backend to show confirmed state
                    loadCalendarFromBackend()
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isMutating = false,
                        error = error.message ?: "Failed to block time"
                    )
                }
            )
        }
    }

    /**
     * Remove blocked time via backend API.
     * After success, re-fetch calendar to show confirmed state.
     */
    fun removeBlockedTime(blockId: String) {
        if (_uiState.value.isMutating) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isMutating = true, error = null)

            val result = hostRepository.removeListingBlock(listingId, blockId)

            result.fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(isMutating = false)
                    // Re-fetch from backend to show confirmed state
                    loadCalendarFromBackend()
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isMutating = false,
                        error = error.message ?: "Failed to remove block"
                    )
                }
            )
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    // ── Slot Generation (from backend data) ────────────────────

    /**
     * Rebuild time slots for the selected date using backend calendar data.
     * Time slots reflect:
     *   - Bookings with ACTIVE status (backend ACTIVE_BOOKING_STATUSES)
     *   - Blocked time ranges from backend
     *   - Holds (active booking holds)
     *   - Listing available hours and available days
     */
    private fun rebuildDaySlots() {
        val data = calendarData ?: return
        val selectedDate = _uiState.value.selectedDate
        if (selectedDate.isBlank()) return

        val slots = mutableListOf<CalendarTimeSlot>()

        // Check if this day is an available day (0=Sun, 6=Sat)
        val dayOfWeek = getDayOfWeek(selectedDate)
        val availableDays = data.availability?.availableDays ?: listOf(1, 2, 3, 4, 5)
        val isDayAvailable = dayOfWeek in availableDays

        // Get available hours from listing settings
        val availStart = data.availability?.startTime ?: "09:00"
        val availEnd = data.availability?.endTime ?: "17:00"
        val availStartMin = timeToMinutes(availStart)
        val availEndMin = timeToMinutes(availEnd)

        // Generate hourly slots from 00:00 to 23:00
        for (hour in 0..23) {
            val startTime = String.format("%02d:00", hour)
            val endTime = String.format("%02d:00", (hour + 1) % 24)
            val endLabel = if (hour == 23) "00:00" else endTime
            val slotMinutes = hour * 60

            // Check backend data for this slot
            val matchingBooking = findBookingForSlot(data, selectedDate, slotMinutes)
            val matchingBlock = findBlockForSlot(data, selectedDate, slotMinutes)
            val matchingHold = findHoldForSlot(data, selectedDate, slotMinutes)

            val status: TimeSlotStatus
            val label: String
            val bookingId: String?

            when {
                // Active booking takes priority
                matchingBooking != null -> {
                    status = TimeSlotStatus.BOOKED
                    label = matchingBooking.guestName ?: "Booked"
                    bookingId = matchingBooking.id
                }
                // Blocked time range
                matchingBlock != null -> {
                    status = TimeSlotStatus.BLOCKED
                    label = matchingBlock.reason.ifBlank { "Blocked" }
                    bookingId = matchingBlock.id // Use block ID for removal
                }
                // Active hold
                matchingHold != null -> {
                    status = TimeSlotStatus.BOOKED
                    label = "Hold (checkout in progress)"
                    bookingId = null
                }
                // Not an available day
                !isDayAvailable -> {
                    status = TimeSlotStatus.BLOCKED
                    label = "Unavailable day"
                    bookingId = null
                }
                // Outside available hours
                slotMinutes < availStartMin || slotMinutes >= availEndMin -> {
                    status = TimeSlotStatus.BLOCKED
                    label = "Outside hours"
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

    /**
     * Find an active booking that covers this slot.
     * Only considers bookings with ACTIVE statuses (backend source of truth).
     */
    private fun findBookingForSlot(
        data: ListingCalendarData,
        date: String,
        slotMinutes: Int
    ): CalendarBookingOverlay? {
        return data.bookings.find { booking ->
            // Only active bookings block availability
            val bookingStatus = BookingStatus.fromString(booking.status)
            if (!bookingStatus.isActive) return@find false

            val bookingDate = extractDate(booking.checkIn)
            if (bookingDate != date) return@find false

            val bStart = extractTime(booking.checkIn)
            val bEnd = extractTime(booking.checkOut)
            if (bStart == null || bEnd == null) return@find false

            val bStartMin = timeToMinutes(bStart)
            val bEndMin = timeToMinutes(bEnd)
            slotMinutes in bStartMin until bEndMin
        }
    }

    /**
     * Find a blocked time range that covers this slot.
     * Backend blocks have startDate/endDate range and optional startTime/endTime.
     */
    private fun findBlockForSlot(
        data: ListingCalendarData,
        date: String,
        slotMinutes: Int
    ): BackendBlock? {
        return data.blocks.find { block ->
            // Check date range (inclusive)
            if (date < block.startDate || date > block.endDate) return@find false

            // If block has time range, check it; otherwise block covers full day
            if (block.startTime != null && block.endTime != null) {
                val bStartMin = timeToMinutes(block.startTime)
                val bEndMin = timeToMinutes(block.endTime)
                slotMinutes in bStartMin until bEndMin
            } else {
                true // Full-day block
            }
        }
    }

    /**
     * Find an active hold that covers this slot.
     */
    private fun findHoldForSlot(
        data: ListingCalendarData,
        date: String,
        slotMinutes: Int
    ): CalendarHoldOverlay? {
        return data.holds.find { hold ->
            if (hold.status != "active") return@find false
            val holdDate = extractDate(hold.startTime)
            if (holdDate != date) return@find false

            val hStart = extractTime(hold.startTime)
            val hEnd = extractTime(hold.endTime)
            if (hStart == null || hEnd == null) return@find false

            val hStartMin = timeToMinutes(hStart)
            val hEndMin = timeToMinutes(hEnd)
            slotMinutes in hStartMin until hEndMin
        }
    }

    /**
     * Rebuild the set of dates in the current month that have events (bookings, blocks, holds).
     */
    private fun rebuildEventDates() {
        val data = calendarData ?: return
        val dates = mutableSetOf<String>()

        // Dates with active bookings
        data.bookings.forEach { booking ->
            val status = BookingStatus.fromString(booking.status)
            if (status.isActive) {
                extractDate(booking.checkIn)?.let { dates.add(it) }
            }
        }

        // Dates with blocks (expand date range)
        data.blocks.forEach { block ->
            // Add all dates in the block range that fall in the current month
            var current = block.startDate
            while (current <= block.endDate) {
                dates.add(current)
                current = incrementDate(current) ?: break
            }
        }

        // Dates with active holds
        data.holds.filter { it.status == "active" }.forEach { hold ->
            extractDate(hold.startTime)?.let { dates.add(it) }
        }

        _uiState.value = _uiState.value.copy(datesWithEvents = dates)
    }

    // ── Helpers ─────────────────────────────────────────────────

    /**
     * Map free-text reason to backend-accepted values.
     * Backend accepts: personal, maintenance, custom
     */
    private fun mapBlockReason(reason: String): String {
        val lower = reason.trim().lowercase()
        return when {
            lower.isEmpty() || lower.contains("personal") -> "personal"
            lower.contains("maintenance") || lower.contains("repair") -> "maintenance"
            else -> "custom"
        }
    }

    private fun timeToMinutes(time: String): Int {
        val parts = time.split(":")
        if (parts.size < 2) return 0
        return (parts[0].toIntOrNull() ?: 0) * 60 + (parts[1].toIntOrNull() ?: 0)
    }

    private fun extractDate(dateTimeStr: String?): String? {
        if (dateTimeStr.isNullOrBlank()) return null
        return dateTimeStr.take(10).takeIf { it.length == 10 && it[4] == '-' }
    }

    private fun extractTime(dateTimeStr: String?): String? {
        if (dateTimeStr.isNullOrBlank()) return null
        val tIndex = dateTimeStr.indexOf('T')
        if (tIndex >= 0 && dateTimeStr.length >= tIndex + 6) {
            return dateTimeStr.substring(tIndex + 1, tIndex + 6)
        }
        // Try HH:mm format directly
        if (dateTimeStr.length >= 5 && dateTimeStr[2] == ':') {
            return dateTimeStr.take(5)
        }
        return null
    }

    /** Get day of week for a date string (0=Sun, 6=Sat) matching backend convention. */
    private fun getDayOfWeek(dateStr: String): Int {
        val parts = dateStr.split("-")
        if (parts.size != 3) return -1
        val cal = Calendar.getInstance()
        cal.set(
            parts[0].toIntOrNull() ?: return -1,
            (parts[1].toIntOrNull() ?: return -1) - 1,
            parts[2].toIntOrNull() ?: return -1
        )
        return cal.get(Calendar.DAY_OF_WEEK) - 1 // Calendar.SUNDAY=1 → 0
    }

    /** Increment a YYYY-MM-DD date string by one day. */
    private fun incrementDate(dateStr: String): String? {
        val parts = dateStr.split("-")
        if (parts.size != 3) return null
        val cal = Calendar.getInstance()
        cal.set(
            parts[0].toIntOrNull() ?: return null,
            (parts[1].toIntOrNull() ?: return null) - 1,
            parts[2].toIntOrNull() ?: return null
        )
        cal.add(Calendar.DAY_OF_MONTH, 1)
        return String.format(
            "%04d-%02d-%02d",
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH) + 1,
            cal.get(Calendar.DAY_OF_MONTH)
        )
    }
}
