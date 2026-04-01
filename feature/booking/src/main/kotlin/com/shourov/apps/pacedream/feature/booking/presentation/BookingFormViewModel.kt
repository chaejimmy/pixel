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
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@HiltViewModel
class BookingFormViewModel @Inject constructor(
    private val bookingRepository: BookingRepository,
    private val propertyRepository: PropertyRepository,
    private val authSession: AuthSession
) : ViewModel() {

    private val _uiState = MutableStateFlow(BookingFormUiState())
    val uiState: StateFlow<BookingFormUiState> = _uiState.asStateFlow()

    fun loadProperty(propertyId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, propertyId = propertyId)

            propertyRepository.getPropertyById(propertyId).collect { result ->
                when (result) {
                    is Result.Success -> {
                        val property = result.data
                        if (property != null) {
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                propertyName = property.name ?: "Property",
                                propertyImage = property.images?.firstOrNull(),
                                basePrice = (property.dynamic_price?.firstOrNull()?.price ?: 0).toDouble(),
                                currency = "USD"
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
                            error = result.exception.message
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
        calculateTotalPrice()
    }

    fun onStartDateChange(date: String) {
        _uiState.value = _uiState.value.copy(startDate = date, endDate = date)
        calculateTotalPrice()
    }

    fun onEndDateChange(date: String) {
        _uiState.value = _uiState.value.copy(endDate = date)
        calculateTotalPrice()
    }

    fun onStartTimeChange(time: String) {
        _uiState.value = _uiState.value.copy(startTime = time)
        calculateEndTime()
    }

    fun onEndTimeChange(time: String) {
        _uiState.value = _uiState.value.copy(endTime = time)
    }

    fun onSpecialRequestsChange(requests: String) {
        _uiState.value = _uiState.value.copy(specialRequests = requests)
    }

    fun onGuestCountChange(count: Int) {
        _uiState.value = _uiState.value.copy(guestCount = count.coerceIn(1, 20))
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
        val durationHours = currentState.selectedDuration / 60.0
        val totalPrice = currentState.basePrice * durationHours
        _uiState.value = _uiState.value.copy(totalPrice = totalPrice)
    }

    fun createBooking(onSuccess: (String) -> Unit) {
        // Prevent double-submit — ignore while already submitting
        if (_uiState.value.isSubmitting) return

        viewModelScope.launch {
            val currentState = _uiState.value

            if (currentState.startDate.isEmpty()) {
                _uiState.value = currentState.copy(error = "Please select a date")
                return@launch
            }

            if (currentState.startTime.isEmpty()) {
                _uiState.value = currentState.copy(error = "Please select a start time")
                return@launch
            }

            if (currentState.selectedDuration <= 0) {
                _uiState.value = currentState.copy(error = "Please select a duration")
                return@launch
            }

            if (authSession.authState.value == AuthState.Unauthenticated) {
                _uiState.value = currentState.copy(error = "Please sign in to book.")
                return@launch
            }

            // Lock submission state
            _uiState.value = currentState.copy(isSubmitting = true, error = null)

            val user = authSession.currentUser.value
            val userId = user?.id.orEmpty()

            val booking = BookingModel(
                id = UUID.randomUUID().toString(),
                userName = userId.ifBlank { user?.displayName },
                userId = userId,
                propertyId = currentState.propertyId,
                propertyName = currentState.propertyName,
                propertyImage = currentState.propertyImage,
                startDate = currentState.startDate,
                endDate = currentState.endDate.ifEmpty { currentState.startDate },
                totalPrice = currentState.totalPrice,
                currency = currentState.currency,
                status = BookingStatus.PENDING,
                hostName = "",
                checkInTime = currentState.startTime.takeIf { it.isNotEmpty() },
                checkOutTime = currentState.endTime.takeIf { it.isNotEmpty() }
            )

            when (val result = bookingRepository.createBooking(booking)) {
                is Result.Success -> {
                    _uiState.value = _uiState.value.copy(isSubmitting = false)
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
        val msg = exception.message ?: "Failed to create booking"
        val lower = msg.lowercase()
        return when {
            lower.contains("fraud") || lower.contains("blocked") ->
                "This booking has been blocked for security reasons. Please contact support."
            lower.contains("review") || lower.contains("paused") ->
                "Your booking is paused for review. We'll notify you once it's approved."
            lower.contains("restricted") || lower.contains("suspended") ->
                "Your account is currently restricted. Please contact support."
            lower.contains("rate") || lower.contains("too many") ->
                "Too many booking attempts. Please wait a moment and try again."
            else -> msg
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
