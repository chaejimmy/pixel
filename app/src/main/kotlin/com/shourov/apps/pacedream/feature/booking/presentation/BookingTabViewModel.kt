package com.shourov.apps.pacedream.feature.booking.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shourov.apps.pacedream.core.network.api.ApiError
import com.shourov.apps.pacedream.core.network.api.ApiResult
import com.shourov.apps.pacedream.core.network.auth.AuthSession
import com.shourov.apps.pacedream.core.network.auth.AuthState
import com.shourov.apps.pacedream.feature.booking.data.BookingTabRepository
import com.shourov.apps.pacedream.feature.booking.model.BookingFilterCategory
import com.shourov.apps.pacedream.feature.booking.model.BookingItem
import com.shourov.apps.pacedream.feature.booking.model.BookingRole
import com.shourov.apps.pacedream.feature.booking.model.BookingStatusConfig
import com.shourov.apps.pacedream.feature.booking.model.BookingStatusFilter
import com.shourov.apps.pacedream.feature.booking.model.BookingTabEvent
import com.shourov.apps.pacedream.feature.booking.model.BookingTabUiState
import com.shourov.apps.pacedream.model.BookingStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject

/**
 * ViewModel for the Bookings tab.
 *
 * iOS parity: Fetches both guest AND host bookings in parallel and merges
 * them into a single unified list, classified into Upcoming / Past / Cancelled
 * categories matching iOS GuestBookingsViewModel.
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

    private var currentStatusFilter = BookingStatusFilter.ALL

    // Cached status configs for each booking
    private val statusConfigs = mutableMapOf<String, BookingStatusConfig>()

    init {
        observeAuthState()
    }

    fun onEvent(event: BookingTabEvent) {
        when (event) {
            is BookingTabEvent.Refresh -> refresh()
            is BookingTabEvent.RoleChanged -> { /* No-op: role tabs removed in iOS parity */ }
            is BookingTabEvent.StatusFilterChanged -> onStatusFilterChanged(event.filter)
            is BookingTabEvent.BookingClicked -> onBookingClicked(event.bookingId)
            is BookingTabEvent.LoadMore -> { /* Pagination handled within initial fetch */ }
        }
    }

    /**
     * Get the status config for a booking item (matching iOS statusConfig(for:)).
     */
    fun statusConfig(item: BookingItem): BookingStatusConfig {
        return statusConfigs[item.id] ?: resolveStatusConfig(item).also {
            statusConfigs[item.id] = it
        }
    }

    /**
     * Continuously observe auth state so the screen reacts immediately
     * when the user logs in (no app restart required).
     */
    private fun observeAuthState() {
        viewModelScope.launch {
            authSession.authState.collect { state ->
                Timber.d("BookingTabVM: authState changed → $state")
                when (state) {
                    AuthState.Authenticated -> {
                        try {
                            loadAllBookings()
                        } catch (e: Exception) {
                            Timber.e(e, "Failed to load bookings after auth change")
                            _uiState.value = BookingTabUiState.Error(
                                e.message ?: "Failed to load bookings"
                            )
                        }
                    }
                    AuthState.Unauthenticated -> {
                        _uiState.value = BookingTabUiState.RequiresAuth
                    }
                    else -> { /* Unknown — wait for auth to settle */ }
                }
            }
        }
    }

    private fun refresh() {
        viewModelScope.launch {
            try {
                if (authSession.authState.value == AuthState.Unauthenticated) {
                    _uiState.value = BookingTabUiState.RequiresAuth
                    return@launch
                }

                (_uiState.value as? BookingTabUiState.Success)?.let { current ->
                    _uiState.value = current.copy(isRefreshing = true)
                }

                loadAllBookings()
            } catch (e: Exception) {
                Timber.e(e, "Failed to refresh bookings")
                _uiState.value = BookingTabUiState.Error(
                    e.message ?: "Failed to refresh bookings"
                )
            }
        }
    }

    /**
     * iOS parity: fetch both guest and host bookings in parallel, merge them,
     * then classify into Upcoming / Past / Cancelled categories.
     */
    private suspend fun loadAllBookings() {
        // Fetch guest and host bookings in parallel (matching iOS)
        val guestDeferred = viewModelScope.async {
            bookingTabRepository.getBookings(role = BookingRole.RENTER, limit = 100, offset = 0)
        }
        val hostDeferred = viewModelScope.async {
            bookingTabRepository.getBookings(role = BookingRole.HOST, limit = 100, offset = 0)
        }

        val guestResult = guestDeferred.await()
        val hostResult = hostDeferred.await()

        val allBookings = mutableListOf<BookingItem>()
        var guestError: String? = null
        var hostError: String? = null

        when (guestResult) {
            is ApiResult.Success -> {
                // Tag guest bookings with RENTER role
                allBookings.addAll(guestResult.data.bookings.map { it.copy(role = BookingRole.RENTER) })
                Timber.d("Bookings: ${guestResult.data.bookings.size} guest bookings loaded")
            }
            is ApiResult.Failure -> {
                when (guestResult.error) {
                    is ApiError.Unauthorized -> {
                        _uiState.value = BookingTabUiState.RequiresAuth
                        return
                    }
                    else -> guestError = guestResult.error.message
                }
            }
        }

        when (hostResult) {
            is ApiResult.Success -> {
                // Tag host bookings with HOST role and prefix IDs to avoid collisions
                allBookings.addAll(hostResult.data.bookings.map {
                    it.copy(
                        id = "host_${it.id}",
                        role = BookingRole.HOST
                    )
                })
                Timber.d("Bookings: ${hostResult.data.bookings.size} host bookings loaded")
            }
            is ApiResult.Failure -> {
                // Host bookings failure is non-fatal
                hostError = hostResult.error.message
                Timber.w("Bookings: host bookings failed: $hostError")
            }
        }

        // Both failed
        if (guestError != null && hostError != null) {
            _uiState.value = BookingTabUiState.Error("Couldn't load bookings.")
            return
        }

        if (allBookings.isEmpty()) {
            _uiState.value = BookingTabUiState.Empty
            return
        }

        // Classify bookings into categories (matching iOS rebuildCategoryCaches)
        statusConfigs.clear()
        val upcoming = mutableListOf<BookingItem>()
        val past = mutableListOf<BookingItem>()
        val cancelled = mutableListOf<BookingItem>()

        for (item in allBookings) {
            val config = resolveStatusConfig(item)
            statusConfigs[item.id] = config
            when (config.filterCategory) {
                BookingFilterCategory.UPCOMING -> upcoming.add(item)
                BookingFilterCategory.PAST -> past.add(item)
                BookingFilterCategory.CANCELLED -> cancelled.add(item)
            }
        }

        val filtered = when (currentStatusFilter) {
            BookingStatusFilter.ALL -> allBookings
            BookingStatusFilter.UPCOMING -> upcoming
            BookingStatusFilter.PAST -> past
            BookingStatusFilter.CANCELLED -> cancelled
        }

        _uiState.value = BookingTabUiState.Success(
            bookings = allBookings,
            isRefreshing = false,
            hasMore = false,
            statusFilter = currentStatusFilter,
            filteredBookings = filtered,
            upcomingBookings = upcoming,
            pastBookings = past,
            cancelledBookings = cancelled
        )
    }

    /**
     * Status classification matching iOS GuestBookingsViewModel.statusConfig(for:).
     *
     * Rules:
     * 1. Auto-promote: if checkout/end date has passed and status is confirmed/active → Completed (Past)
     * 2. Pending statuses → Upcoming (yellow)
     * 3. Confirmed/active → Upcoming (blue)
     * 4. Completed → Past (green)
     * 5. Cancelled → Cancelled (red)
     */
    private fun resolveStatusConfig(item: BookingItem): BookingStatusConfig {
        val status = item.status.name.lowercase()
        val now = Date()

        // Smart date logic: if checkout/end date has passed and status is still active,
        // auto-promote to "Completed" → past category (matching iOS)
        val endDate = parseIsoDate(item.endDate)
        if (endDate != null && endDate.before(now)) {
            val upcomingStatuses = setOf("confirmed", "pending")
            if (upcomingStatuses.contains(status)) {
                return BookingStatusConfig("Completed", BookingFilterCategory.PAST, "green")
            }
        }

        return when (item.status) {
            BookingStatus.PENDING -> BookingStatusConfig("Pending", BookingFilterCategory.UPCOMING, "yellow")
            BookingStatus.CONFIRMED -> BookingStatusConfig("Confirmed", BookingFilterCategory.UPCOMING, "blue")
            BookingStatus.COMPLETED -> BookingStatusConfig("Completed", BookingFilterCategory.PAST, "green")
            BookingStatus.CANCELLED -> BookingStatusConfig("Cancelled", BookingFilterCategory.CANCELLED, "red")
            BookingStatus.REJECTED -> BookingStatusConfig("Declined", BookingFilterCategory.CANCELLED, "red")
        }
    }

    private fun onStatusFilterChanged(filter: BookingStatusFilter) {
        currentStatusFilter = filter
        val currentState = _uiState.value as? BookingTabUiState.Success ?: return
        val filtered = when (filter) {
            BookingStatusFilter.ALL -> currentState.bookings
            BookingStatusFilter.UPCOMING -> currentState.upcomingBookings
            BookingStatusFilter.PAST -> currentState.pastBookings
            BookingStatusFilter.CANCELLED -> currentState.cancelledBookings
        }
        _uiState.value = currentState.copy(
            statusFilter = filter,
            filteredBookings = filtered
        )
    }

    private fun onBookingClicked(bookingId: String) {
        viewModelScope.launch {
            // Strip the host_ prefix for navigation
            val rawId = if (bookingId.startsWith("host_")) bookingId.removePrefix("host_") else bookingId
            _navigation.send(BookingTabNavigation.ToBookingDetail(rawId))
        }
    }

    fun onAuthCompleted() {
        checkAuthAndLoad()
    }

    companion object {
        private val isoFormats = listOf(
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'" to "UTC",
            "yyyy-MM-dd'T'HH:mm:ss'Z'" to "UTC",
            "yyyy-MM-dd" to null
        )

        fun parseIsoDate(raw: String?): Date? {
            if (raw.isNullOrBlank()) return null
            for ((pattern, tz) in isoFormats) {
                try {
                    val fmt = SimpleDateFormat(pattern, Locale.US)
                    if (tz != null) fmt.timeZone = TimeZone.getTimeZone(tz)
                    return fmt.parse(raw.trim())
                } catch (_: Exception) { }
            }
            return null
        }
    }
}

sealed class BookingTabNavigation {
    data class ToBookingDetail(val bookingId: String) : BookingTabNavigation()
}
