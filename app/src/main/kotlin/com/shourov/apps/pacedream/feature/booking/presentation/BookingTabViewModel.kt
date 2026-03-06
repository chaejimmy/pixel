package com.shourov.apps.pacedream.feature.booking.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shourov.apps.pacedream.core.network.api.ApiError
import com.shourov.apps.pacedream.core.network.api.ApiResult
import com.shourov.apps.pacedream.core.network.auth.AuthSession
import com.shourov.apps.pacedream.core.network.auth.AuthState
import com.shourov.apps.pacedream.feature.booking.data.BookingTabRepository
import com.shourov.apps.pacedream.feature.booking.model.BookingRole
import com.shourov.apps.pacedream.feature.booking.model.BookingStatusFilter
import com.shourov.apps.pacedream.feature.booking.model.BookingTabEvent
import com.shourov.apps.pacedream.feature.booking.model.BookingTabUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel for the Bookings tab.
 *
 * Mirrors the web platform's Trips / Hosting tabs which fetch from:
 *   GET /account/bookings?role=renter   (Trips)
 *   GET /account/bookings?role=host     (Hosting)
 */
@HiltViewModel
class BookingTabViewModel @Inject constructor(
    private val bookingTabRepository: BookingTabRepository,
    private val authSession: AuthSession
) : ViewModel() {

    private val _uiState = MutableStateFlow<BookingTabUiState>(BookingTabUiState.Loading)
    val uiState: StateFlow<BookingTabUiState> = _uiState.asStateFlow()

    private val _navigation = Channel<BookingTabNavigation>(Channel.BUFFERED)
    val navigation = _navigation.receiveAsFlow()

    private var currentRole = BookingRole.RENTER
    private var currentStatusFilter = BookingStatusFilter.ALL
    private var currentOffset = 0

    companion object {
        private const val PAGE_SIZE = 20
    }

    init {
        checkAuthAndLoad()
    }

    fun onEvent(event: BookingTabEvent) {
        when (event) {
            is BookingTabEvent.Refresh -> refresh()
            is BookingTabEvent.RoleChanged -> onRoleChanged(event.role)
            is BookingTabEvent.StatusFilterChanged -> onStatusFilterChanged(event.filter)
            is BookingTabEvent.BookingClicked -> onBookingClicked(event.bookingId)
            is BookingTabEvent.LoadMore -> loadMore()
        }
    }

    private fun checkAuthAndLoad() {
        viewModelScope.launch {
            if (authSession.authState.value == AuthState.Unauthenticated) {
                _uiState.value = BookingTabUiState.RequiresAuth
                return@launch
            }
            loadBookings()
        }
    }

    private fun refresh() {
        viewModelScope.launch {
            if (authSession.authState.value == AuthState.Unauthenticated) {
                _uiState.value = BookingTabUiState.RequiresAuth
                return@launch
            }

            (_uiState.value as? BookingTabUiState.Success)?.let { current ->
                _uiState.value = current.copy(isRefreshing = true)
            }

            currentOffset = 0
            loadBookings()
        }
    }

    private suspend fun loadBookings() {
        val result = bookingTabRepository.getBookings(
            role = currentRole,
            limit = PAGE_SIZE,
            offset = 0
        )

        when (result) {
            is ApiResult.Success -> {
                currentOffset = result.data.bookings.size

                val allBookings = result.data.bookings
                val filtered = applyStatusFilter(allBookings, currentStatusFilter)

                if (allBookings.isEmpty()) {
                    _uiState.value = BookingTabUiState.Empty
                } else {
                    _uiState.value = BookingTabUiState.Success(
                        bookings = allBookings,
                        role = currentRole,
                        isRefreshing = false,
                        hasMore = result.data.hasMore,
                        statusFilter = currentStatusFilter,
                        filteredBookings = filtered
                    )
                }
            }
            is ApiResult.Failure -> {
                when (result.error) {
                    is ApiError.Unauthorized -> {
                        _uiState.value = BookingTabUiState.RequiresAuth
                    }
                    else -> {
                        _uiState.value = BookingTabUiState.Error(
                            result.error.message ?: "Failed to load bookings"
                        )
                    }
                }
            }
        }
    }

    private fun loadMore() {
        val currentState = _uiState.value as? BookingTabUiState.Success ?: return
        if (!currentState.hasMore) return

        viewModelScope.launch {
            val result = bookingTabRepository.getBookings(
                role = currentRole,
                limit = PAGE_SIZE,
                offset = currentOffset
            )

            when (result) {
                is ApiResult.Success -> {
                    currentOffset += result.data.bookings.size
                    _uiState.value = currentState.copy(
                        bookings = currentState.bookings + result.data.bookings,
                        hasMore = result.data.hasMore
                    )
                }
                is ApiResult.Failure -> {
                    Timber.e("Failed to load more bookings: ${result.error.message}")
                }
            }
        }
    }

    private fun onStatusFilterChanged(filter: BookingStatusFilter) {
        currentStatusFilter = filter
        val currentState = _uiState.value as? BookingTabUiState.Success ?: return
        val filtered = applyStatusFilter(currentState.bookings, filter)
        _uiState.value = currentState.copy(
            statusFilter = filter,
            filteredBookings = filtered
        )
    }

    private fun applyStatusFilter(
        bookings: List<com.shourov.apps.pacedream.feature.booking.model.BookingItem>,
        filter: BookingStatusFilter
    ): List<com.shourov.apps.pacedream.feature.booking.model.BookingItem> {
        if (filter == BookingStatusFilter.ALL) return bookings
        return bookings.filter { filter.matches(it.status, it.endDate) }
    }

    private fun onRoleChanged(role: BookingRole) {
        if (role == currentRole) return

        currentRole = role
        currentOffset = 0
        _uiState.value = BookingTabUiState.Loading

        viewModelScope.launch {
            loadBookings()
        }
    }

    private fun onBookingClicked(bookingId: String) {
        viewModelScope.launch {
            _navigation.send(BookingTabNavigation.ToBookingDetail(bookingId))
        }
    }

    fun onAuthCompleted() {
        checkAuthAndLoad()
    }
}

sealed class BookingTabNavigation {
    data class ToBookingDetail(val bookingId: String) : BookingTabNavigation()
}
