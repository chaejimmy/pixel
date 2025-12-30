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
import com.shourov.apps.pacedream.core.network.auth.AuthSession
import com.shourov.apps.pacedream.core.network.auth.AuthState
import com.shourov.apps.pacedream.model.BookingModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BookingViewModel @Inject constructor(
    private val bookingRepository: BookingRepository,
    private val authSession: AuthSession
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(BookingUiState())
    val uiState: StateFlow<BookingUiState> = _uiState.asStateFlow()
    
    fun loadBookings() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            if (authSession.authState.value == AuthState.Unauthenticated) {
                _uiState.value = _uiState.value.copy(isLoading = false, bookings = emptyList(), error = "Please sign in.")
                return@launch
            }

            val userId = authSession.currentUser.value?.id.orEmpty()
            
            bookingRepository.getUserBookings(userId).collect { result ->
                when (result) {
                    is Result.Success -> {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            bookings = result.data,
                            error = null
                        )
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
    
    fun onBookingClick(bookingId: String) {
        // Navigate to booking detail screen
        // This would be handled by navigation
    }
    
    fun cancelBooking(bookingId: String) {
        viewModelScope.launch {
            when (val result = bookingRepository.cancelBooking(bookingId)) {
                is Result.Success -> {
                    // Refresh bookings
                    loadBookings()
                }
                is Result.Error -> {
                    _uiState.value = _uiState.value.copy(
                        error = result.exception.message
                    )
                }
                is Result.Loading -> { /* No-op */ }
            }
        }
    }
    
    fun confirmBooking(bookingId: String) {
        viewModelScope.launch {
            when (val result = bookingRepository.confirmBooking(bookingId)) {
                is Result.Success -> {
                    // Refresh bookings
                    loadBookings()
                }
                is Result.Error -> {
                    _uiState.value = _uiState.value.copy(
                        error = result.exception.message
                    )
                }
                is Result.Loading -> { /* No-op */ }
            }
        }
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}

data class BookingUiState(
    val isLoading: Boolean = false,
    val bookings: List<BookingModel> = emptyList(),
    val error: String? = null
)
