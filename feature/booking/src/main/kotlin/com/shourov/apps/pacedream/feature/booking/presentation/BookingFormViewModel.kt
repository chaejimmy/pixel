/*
 * Copyright 2024 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.shourov.apps.pacedream.feature.booking.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shourov.apps.pacedream.core.common.result.Result
import com.shourov.apps.pacedream.core.data.repository.BookingRepository
import com.shourov.apps.pacedream.core.data.repository.PropertyRepository
import com.shourov.apps.pacedream.core.network.auth.AuthSession
import com.shourov.apps.pacedream.core.network.auth.AuthState
import com.shourov.apps.pacedream.model.BookingModel
import com.shourov.apps.pacedream.model.BookingStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.pacedream.common.util.UserFacingErrorMapper
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@HiltViewModel
class BookingFormViewModel @Inject constructor(
    private val bookingRepository: BookingRepository,
    private val propertyRepository: PropertyRepository,
    private val authSession: AuthSession,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    // Restore user-typed fields from SavedStateHandle so a process death (low
    // memory kill, OS update) mid-form doesn't wipe the user's selections.
    // Pricing-derived fields (basePrice, currency, etc.) are intentionally not
    // restored — they get re-fetched in loadProperty().
    private val _uiState = MutableStateFlow(
        BookingFormUiState(
            startDate = savedStateHandle.get<String>(KEY_START_DATE).orEmpty(),
            endDate = savedStateHandle.get<String>(KEY_END_DATE).orEmpty(),
            startTime = savedStateHandle.get<String>(KEY_START_TIME).orEmpty(),
            endTime = savedStateHandle.get<String>(KEY_END_TIME).orEmpty(),
            selectedDuration = savedStateHandle.get<Int>(KEY_DURATION) ?: 60,
            specialRequests = savedStateHandle.get<String>(KEY_SPECIAL_REQUESTS).orEmpty(),
            guestCount = savedStateHandle.get<Int>(KEY_GUEST_COUNT) ?: 1,
            selectedMonths = savedStateHandle.get<Int>(KEY_SELECTED_MONTHS),
            customMonthsInput = savedStateHandle.get<String>(KEY_CUSTOM_MONTHS).orEmpty()
        )
    )
    val uiState: StateFlow<BookingFormUiState> = _uiState.asStateFlow()

    private companion object {
        const val KEY_START_DATE = "booking_start_date"
        const val KEY_END_DATE = "booking_end_date"
        const val KEY_START_TIME = "booking_start_time"
        const val KEY_END_TIME = "booking_end_time"
        const val KEY_DURATION = "booking_duration"
        const val KEY_SPECIAL_REQUESTS = "booking_special_requests"
        const val KEY_GUEST_COUNT = "booking_guest_count"
        const val KEY_SELECTED_MONTHS = "booking_selected_months"
        const val KEY_CUSTOM_MONTHS = "booking_custom_months"
    }

    fun loadProperty(propertyId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, propertyId = propertyId)

            propertyRepository.getPropertyById(propertyId).collect { result ->
                when (result) {
                    is Result.Success -> {
                        val property = result.data
                        if (property != null) {
                            val dynamicPrice = property.dynamic_price?.firstOrNull()
                            // Monthly is detected when the listing carries a usable monthly
                            // price — same fallback the web uses in pricingUnit.ts when no
                            // explicit pricing_type is present on the payload.
                            val monthlyPrice = dynamicPrice?.monthly?.price?.takeIf { it > 0 }
                            val hourlyPrice = dynamicPrice?.hourly?.price?.takeIf { it > 0 }
                            val isMonthly = monthlyPrice != null && hourlyPrice == null
                            val basePrice = (monthlyPrice
                                ?: hourlyPrice
                                ?: dynamicPrice?.price
                                ?: 0).toDouble()

                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                propertyName = property.name ?: "Property",
                                propertyImage = property.images?.firstOrNull(),
                                basePrice = basePrice,
                                currency = dynamicPrice?.currency ?: "USD",
                                isMonthly = isMonthly
                            )
                            calculateTotalPrice()
                        } else {
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                error = "Property not found"
                            )
                        }
                    }
                    is Result.Error -> {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = UserFacingErrorMapper.forLoadProperties(result.exception)
                        )
                    }
                    is Result.Loading -> {
                        _uiState.value = _uiState.value.copy(isLoading = true)
                    }
                }
            }
        }
    }

    fun onDurationChange(durationMinutes: Int) {
        _uiState.value = _uiState.value.copy(selectedDuration = durationMinutes)
        savedStateHandle[KEY_DURATION] = durationMinutes
        calculateTotalPrice()
    }

    fun onStartDateChange(date: String) {
        _uiState.value = _uiState.value.copy(startDate = date, endDate = date)
        savedStateHandle[KEY_START_DATE] = date
        savedStateHandle[KEY_END_DATE] = date
        calculateTotalPrice()
    }

    fun onEndDateChange(date: String) {
        _uiState.value = _uiState.value.copy(endDate = date)
        savedStateHandle[KEY_END_DATE] = date
        calculateTotalPrice()
    }

    fun onStartTimeChange(time: String) {
        _uiState.value = _uiState.value.copy(startTime = time)
        savedStateHandle[KEY_START_TIME] = time
        calculateEndTime()
    }

    fun onEndTimeChange(time: String) {
        _uiState.value = _uiState.value.copy(endTime = time)
        savedStateHandle[KEY_END_TIME] = time
    }

    fun onSpecialRequestsChange(requests: String) {
        _uiState.value = _uiState.value.copy(specialRequests = requests)
        savedStateHandle[KEY_SPECIAL_REQUESTS] = requests
    }

    fun onGuestCountChange(count: Int) {
        val clamped = count.coerceIn(1, 20)
        _uiState.value = _uiState.value.copy(guestCount = clamped)
        savedStateHandle[KEY_GUEST_COUNT] = clamped
    }

    // ── Monthly rental handlers ───────────────────────────────────

    fun onMonthlyDurationChange(months: Int) {
        val clamped = months.coerceAtLeast(1)
        _uiState.value = _uiState.value.copy(
            selectedMonths = clamped,
            customMonthsInput = ""
        )
        savedStateHandle[KEY_SELECTED_MONTHS] = clamped
        savedStateHandle[KEY_CUSTOM_MONTHS] = ""
        calculateTotalPrice()
    }

    fun onCustomMonthsChange(raw: String) {
        val digits = raw.filter { it.isDigit() }.take(3)
        val parsed = digits.toIntOrNull()
        _uiState.value = _uiState.value.copy(
            customMonthsInput = digits,
            selectedMonths = parsed?.takeIf { it >= 1 }
        )
        savedStateHandle[KEY_CUSTOM_MONTHS] = digits
        savedStateHandle[KEY_SELECTED_MONTHS] = parsed?.takeIf { it >= 1 }
        calculateTotalPrice()
    }

    /**
     * Clear persisted form draft after a successful booking so the next
     * booking flow starts fresh.
     */
    private fun clearSavedDraft() {
        savedStateHandle.remove<String>(KEY_START_DATE)
        savedStateHandle.remove<String>(KEY_END_DATE)
        savedStateHandle.remove<String>(KEY_START_TIME)
        savedStateHandle.remove<String>(KEY_END_TIME)
        savedStateHandle.remove<Int>(KEY_DURATION)
        savedStateHandle.remove<String>(KEY_SPECIAL_REQUESTS)
        savedStateHandle.remove<Int>(KEY_GUEST_COUNT)
        savedStateHandle.remove<Int>(KEY_SELECTED_MONTHS)
        savedStateHandle.remove<String>(KEY_CUSTOM_MONTHS)
    }

    private fun calculateEndTime() {
        val currentState = _uiState.value
        if (currentState.startTime.isNotEmpty() && currentState.selectedDuration > 0) {
            try {
                val fmt = SimpleDateFormat("HH:mm", Locale.getDefault())
                val startCal = Calendar.getInstance()
                startCal.time = fmt.parse(currentState.startTime) ?: return
                startCal.add(Calendar.MINUTE, currentState.selectedDuration)
                val endTime = fmt.format(startCal.time)
                _uiState.value = currentState.copy(endTime = endTime)
            } catch (_: Exception) { }
        }
    }

    private fun calculateTotalPrice() {
        val currentState = _uiState.value
        val totalPrice = if (currentState.isMonthly) {
            val months = currentState.selectedMonths ?: 0
            currentState.basePrice * months
        } else {
            val durationHours = currentState.selectedDuration / 60.0
            currentState.basePrice * durationHours
        }
        _uiState.value = _uiState.value.copy(totalPrice = totalPrice)
    }

    fun createBooking(onSuccess: (String) -> Unit) {
        // Prevent double-submit — ignore while already submitting
        if (_uiState.value.isSubmitting) return

        viewModelScope.launch {
            val currentState = _uiState.value

            if (currentState.startDate.isEmpty()) {
                _uiState.value = currentState.copy(error = "Please select a start date")
                return@launch
            }

            if (currentState.isMonthly) {
                val months = currentState.selectedMonths
                if (months == null || months < 1) {
                    _uiState.value = currentState.copy(
                        error = "Please choose a rental duration of at least 1 month"
                    )
                    return@launch
                }
            } else {
                if (currentState.startTime.isEmpty()) {
                    _uiState.value = currentState.copy(error = "Please select a start time")
                    return@launch
                }
                if (currentState.selectedDuration <= 0) {
                    _uiState.value = currentState.copy(error = "Please select a duration")
                    return@launch
                }
            }

            if (authSession.authState.value == AuthState.Unauthenticated) {
                _uiState.value = currentState.copy(error = "Please sign in to book.")
                return@launch
            }

            // Lock submission state
            _uiState.value = currentState.copy(isSubmitting = true, error = null)

            val startDateTime: String
            val endDateTime: String
            if (currentState.isMonthly) {
                val months = currentState.selectedMonths ?: 1
                val cal = Calendar.getInstance()
                try {
                    val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    val start = fmt.parse(currentState.startDate) ?: cal.time
                    cal.time = start
                    startDateTime = "${fmt.format(cal.time)}T00:00:00Z"
                    cal.add(Calendar.MONTH, months)
                    endDateTime = "${fmt.format(cal.time)}T00:00:00Z"
                } catch (_: Exception) {
                    _uiState.value = _uiState.value.copy(
                        isSubmitting = false,
                        error = "Please choose a valid start date"
                    )
                    return@launch
                }
            } else {
                startDateTime = "${currentState.startDate}T${currentState.startTime}:00Z"
                endDateTime = if (currentState.endTime.isNotEmpty()) {
                    "${currentState.endDate.ifEmpty { currentState.startDate }}T${currentState.endTime}:00Z"
                } else {
                    startDateTime
                }
            }

            // ── Step 1: Check availability with backend (source of truth) ──
            val availResult = bookingRepository.checkAvailability(
                listingId = currentState.propertyId,
                startDate = startDateTime,
                endDate = endDateTime
            )

            when (availResult) {
                is Result.Success -> {
                    val check = availResult.data
                    if (!check.available) {
                        _uiState.value = _uiState.value.copy(
                            isSubmitting = false,
                            error = check.displayReason
                        )
                        return@launch
                    }
                    if (!check.listingBookable) {
                        _uiState.value = _uiState.value.copy(
                            isSubmitting = false,
                            error = "This listing is not currently accepting bookings"
                        )
                        return@launch
                    }
                }
                is Result.Error -> {
                    // If availability check fails, still proceed with booking creation
                    // so the backend can enforce its own validation (defense in depth).
                    android.util.Log.w(
                        "BookingFormVM",
                        "Availability pre-check failed, proceeding with booking creation",
                        availResult.exception
                    )
                }
                is Result.Loading -> { /* No-op */ }
            }

            // ── Step 2: Create booking ──
            val user = authSession.currentUser.value
            val userId = user?.id.orEmpty()

            val monthlyEndDate: String? = if (currentState.isMonthly) {
                try {
                    val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    val cal = Calendar.getInstance()
                    cal.time = fmt.parse(currentState.startDate) ?: cal.time
                    cal.add(Calendar.MONTH, currentState.selectedMonths ?: 1)
                    fmt.format(cal.time)
                } catch (_: Exception) { null }
            } else null

            val booking = BookingModel(
                id = UUID.randomUUID().toString(),
                userName = userId.ifBlank { user?.displayName },
                userId = userId,
                propertyId = currentState.propertyId,
                propertyName = currentState.propertyName,
                propertyImage = currentState.propertyImage,
                startDate = currentState.startDate,
                endDate = monthlyEndDate ?: currentState.endDate.ifEmpty { currentState.startDate },
                totalPrice = currentState.totalPrice,
                currency = currentState.currency,
                status = BookingStatus.PENDING,
                hostName = "",
                checkInTime = if (currentState.isMonthly) null else currentState.startTime.takeIf { it.isNotEmpty() },
                checkOutTime = if (currentState.isMonthly) null else currentState.endTime.takeIf { it.isNotEmpty() }
            )

            when (val result = bookingRepository.createBooking(booking)) {
                is Result.Success -> {
                    _uiState.value = _uiState.value.copy(isSubmitting = false)
                    clearSavedDraft()
                    onSuccess(booking.id)
                }
                is Result.Error -> {
                    val errorMsg = mapBookingError(result.exception)
                    _uiState.value = _uiState.value.copy(
                        isSubmitting = false,
                        error = errorMsg
                    )
                }
                is Result.Loading -> { /* No-op */ }
            }
        }
    }

    /**
     * Map booking errors, including security-related responses from the backend.
     */
    private fun mapBookingError(exception: Throwable): String {
        return UserFacingErrorMapper.forBookingCreate(exception)
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
