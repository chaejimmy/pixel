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

                // Fetch calendar from backend — this is the source of truth
                // Backend returns listingTitle directly in the response
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

                        // Use listingTitle from backend response (falls back to current value)
                        val title = data.listingTitle.ifBlank { state.listingTitle }

                        _uiState.value = state.copy(
                            listingTitle = title,
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
                    onFailure = { err ->
                        Timber.e(err, "Failed to load calendar from backend")
                        _uiState.value = state.copy(
                            isLoading = false,
                            error = err.message ?: "Failed to load calendar"
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

    // ── Slot Generation (from backend days map) ──────────────

    /**
     * Rebuild time slots for the selected date using the backend `days` map.
     *
     * Backend returns: data.days["2026-04-03"] = { date, status, bookings[], blocks[] }
     * Each day already has its bookings/blocks pre-filtered by the backend.
     *
     * Slot generation also considers listing availability settings
     * (available_days, start_time, end_time) for showing off-hours/unavailable days.
     */
    private fun rebuildDaySlots() {
        val data = calendarData ?: return
        val selectedDate = _uiState.value.selectedDate
        if (selectedDate.isBlank()) return

        val dayData = data.days[selectedDate]
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

        // Backend day-level status (pre-computed): "available", "blocked", "booked", "pending"
        val dayStatus = dayData?.status ?: "available"

        // Generate hourly slots from 00:00 to 23:00
        for (hour in 0..23) {
            val startTime = String.format("%02d:00", hour)
            val endTime = String.format("%02d:00", (hour + 1) % 24)
            val endLabel = if (hour == 23) "00:00" else endTime
            val slotMinutes = hour * 60

            // Check this day's bookings and blocks from backend
            val matchingBooking = findBookingForSlot(dayData, slotMinutes)
            val matchingBlock = findBlockForSlot(dayData, slotMinutes)

            val status: TimeSlotStatus
            val label: String
            val bookingId: String?

            when {
                // Active booking takes priority
                matchingBooking != null -> {
                    status = TimeSlotStatus.BOOKED
                    label = "Booked (${matchingBooking.status ?: ""})"
                    bookingId = matchingBooking.id
                }
                // Blocked time range
                matchingBlock != null -> {
                    status = TimeSlotStatus.BLOCKED
                    label = matchingBlock.reason?.ifBlank { "Blocked" } ?: "Blocked"
                    bookingId = matchingBlock.id // Use block ID for removal
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
                // Backend says day is blocked
                dayStatus == "blocked" -> {
                    status = TimeSlotStatus.BLOCKED
                    label = "Blocked"
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
     * Find a booking in this day's data that covers the given slot.
     * Backend already filters to only active bookings for the calendar.
     */
    private fun findBookingForSlot(
        dayData: CalendarDayData?,
        slotMinutes: Int
    ): CalendarDayBooking? {
        if (dayData == null) return null
        return dayData.bookings.find { booking ->
            val bStart = extractTime(booking.startTime)
            val bEnd = extractTime(booking.endTime)
            if (bStart == null || bEnd == null) return@find false

            val bStartMin = timeToMinutes(bStart)
            val bEndMin = timeToMinutes(bEnd)
            slotMinutes in bStartMin until bEndMin
        }
    }

    /**
     * Find a block in this day's data that covers the given slot.
     * Backend pre-expands blocks per day, so we just check time range.
     */
    private fun findBlockForSlot(
        dayData: CalendarDayData?,
        slotMinutes: Int
    ): CalendarDayBlock? {
        if (dayData == null) return null
        return dayData.blocks.find { block ->
            // If block has no time fields, it's a full-day block
            val startStr = block.startDate ?: return@find true
            val endStr = block.endDate ?: return@find true

            // If startDate/endDate look like date strings (not times), it's a full-day block
            if (startStr.length > 5) return@find true

            // Otherwise treat as time range
            val bStartMin = timeToMinutes(startStr)
            val bEndMin = timeToMinutes(endStr)
            slotMinutes in bStartMin until bEndMin
        }
    }

    /**
     * Rebuild the set of dates in the current month that have events.
     * Uses the backend `days` map — any day with status != "available" or
     * with bookings/blocks is an event day.
     */
    private fun rebuildEventDates() {
        val data = calendarData ?: return
        val dates = mutableSetOf<String>()

        data.days.forEach { (dateStr, dayData) ->
            if (dayData.status != "available" ||
                dayData.bookings.isNotEmpty() ||
                dayData.blocks.isNotEmpty()
            ) {
                dates.add(dateStr)
            }
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

}
