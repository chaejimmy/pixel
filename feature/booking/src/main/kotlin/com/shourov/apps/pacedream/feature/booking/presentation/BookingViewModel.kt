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
import com.shourov.apps.pacedream.model.BookingStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

import com.pacedream.common.util.UserFacingErrorMapper
import timber.log.Timber
import java.util.*
import javax.inject.Inject

enum class BookingTab(val label: String) {
    ALL("All"),
    UPCOMING("Upcoming"),
    PAST("Past"),
    CANCELLED("Cancelled");
}

enum class BookingFilterCategory {
    UPCOMING, PAST, CANCELLED
}

data class BookingStatusConfig(
    val label: String,
    val filterCategory: BookingFilterCategory,
    val badgeColor: String
)

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
                _uiState.value = _uiState.value.copy(isLoading = false, allBookings = emptyList(), error = "Please sign in.")
                return@launch
            }

            // Try API first, fall back to Room cache
            when (val apiResult = bookingRepository.fetchMyBookings()) {
                is Result.Success -> {
                    val bookings = apiResult.data
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        allBookings = bookings,
                        error = null
                    )
                    rebuildCategoryCaches(bookings)
                }
                is Result.Error -> {
                    // Fall back to Room cache
                    val userId = authSession.currentUser.value?.id.orEmpty()
                    bookingRepository.getUserBookings(userId).collect { result ->
                        when (result) {
                            is Result.Success -> {
                                _uiState.value = _uiState.value.copy(
                                    isLoading = false,
                                    allBookings = result.data,
                                    error = null
                                )
                                rebuildCategoryCaches(result.data)
                            }
                            is Result.Error -> {
                                Timber.e(result.exception, "Failed to load bookings from cache")
                                _uiState.value = _uiState.value.copy(
                                    isLoading = false,
                                    error = UserFacingErrorMapper.forLoadBookings(result.exception)
                                )
                            }
                            is Result.Loading -> {
                                _uiState.value = _uiState.value.copy(isLoading = true)
                            }
                        }
                    }
                }
                is Result.Loading -> { /* No-op */ }
            }
        }
    }

    fun selectTab(tab: BookingTab) {
        _uiState.value = _uiState.value.copy(selectedTab = tab)
    }

    fun statusConfig(booking: BookingModel): BookingStatusConfig {
        val status = (booking.bookingStatus ?: booking.status.name).lowercase()

        // The backend status field is the source of truth; we do not
        // override it with client-side date logic.

        // Pending statuses → Upcoming
        val pendingStatuses = setOf(
            "pending", "pending_host", "requires_capture",
            "created", "requires_payment_method", "processing",
            "unverified", "awaiting_confirmation"
        )
        if (pendingStatuses.contains(status) || status.contains("pending")) {
            return BookingStatusConfig("Pending", BookingFilterCategory.UPCOMING, "yellow")
        }

        // Confirmed/active statuses → Upcoming
        val confirmedStatuses = setOf(
            "confirmed", "upcoming", "active", "ongoing",
            "booked", "paid", "succeeded", "captured", "accepted"
        )
        if (confirmedStatuses.contains(status)) {
            return BookingStatusConfig("Confirmed", BookingFilterCategory.UPCOMING, "blue")
        }

        // Completed statuses → Past
        val completedStatuses = setOf("completed", "finished")
        if (completedStatuses.contains(status)) {
            return BookingStatusConfig("Completed", BookingFilterCategory.PAST, "green")
        }

        // Cancelled statuses → Cancelled
        val cancelledStatuses = setOf(
            "canceled", "cancelled", "refunded", "failed",
            "expired", "void", "declined", "rejected"
        )
        if (cancelledStatuses.contains(status)) {
            return BookingStatusConfig("Cancelled", BookingFilterCategory.CANCELLED, "red")
        }

        if (status.isEmpty()) {
            return BookingStatusConfig("Pending", BookingFilterCategory.UPCOMING, "yellow")
        }

        return BookingStatusConfig(
            status.replaceFirstChar { it.uppercase() },
            BookingFilterCategory.UPCOMING,
            "gray"
        )
    }

    private fun rebuildCategoryCaches(bookings: List<BookingModel>) {
        val upcoming = mutableListOf<BookingModel>()
        val past = mutableListOf<BookingModel>()
        val cancelled = mutableListOf<BookingModel>()

        for (booking in bookings) {
            when (statusConfig(booking).filterCategory) {
                BookingFilterCategory.UPCOMING -> upcoming.add(booking)
                BookingFilterCategory.PAST -> past.add(booking)
                BookingFilterCategory.CANCELLED -> cancelled.add(booking)
            }
        }

        _uiState.value = _uiState.value.copy(
            upcomingBookings = upcoming,
            pastBookings = past,
            cancelledBookings = cancelled
        )
    }

    fun onBookingClick(bookingId: String) {
        // Navigate to booking detail screen
        // This would be handled by navigation
    }

    fun cancelBooking(bookingId: String) {
        if (_uiState.value.actionInFlight) return
        _uiState.value = _uiState.value.copy(actionInFlight = true)
        viewModelScope.launch {
            when (val result = bookingRepository.cancelBooking(bookingId)) {
                is Result.Success -> {
                    _uiState.value = _uiState.value.copy(actionInFlight = false)
                    loadBookings()
                }
                is Result.Error -> {
                    Timber.e(result.exception, "Failed to cancel booking")
                    _uiState.value = _uiState.value.copy(
                        actionInFlight = false,
                        error = UserFacingErrorMapper.forBookingCancel(result.exception)
                    )
                }
                is Result.Loading -> { /* No-op */ }
            }
        }
    }

    fun confirmBooking(bookingId: String) {
        if (_uiState.value.actionInFlight) return
        _uiState.value = _uiState.value.copy(actionInFlight = true)
        viewModelScope.launch {
            when (val result = bookingRepository.confirmBooking(bookingId)) {
                is Result.Success -> {
                    _uiState.value = _uiState.value.copy(actionInFlight = false)
                    loadBookings()
                }
                is Result.Error -> {
                    Timber.e(result.exception, "Failed to confirm booking")
                    _uiState.value = _uiState.value.copy(
                        actionInFlight = false,
                        error = UserFacingErrorMapper.forBookingConfirm(result.exception)
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
    val selectedTab: BookingTab = BookingTab.ALL,
    val allBookings: List<BookingModel> = emptyList(),
    val upcomingBookings: List<BookingModel> = emptyList(),
    val pastBookings: List<BookingModel> = emptyList(),
    val cancelledBookings: List<BookingModel> = emptyList(),
    val error: String? = null,
    /** True while a confirm/cancel action is in flight — prevents duplicate requests. */
    val actionInFlight: Boolean = false
) {
    val filteredBookings: List<BookingModel>
        get() = when (selectedTab) {
            BookingTab.ALL -> allBookings
            BookingTab.UPCOMING -> upcomingBookings
            BookingTab.PAST -> pastBookings
            BookingTab.CANCELLED -> cancelledBookings
        }

    fun count(tab: BookingTab): Int = when (tab) {
        BookingTab.ALL -> allBookings.size
        BookingTab.UPCOMING -> upcomingBookings.size
        BookingTab.PAST -> pastBookings.size
        BookingTab.CANCELLED -> cancelledBookings.size
    }
}
