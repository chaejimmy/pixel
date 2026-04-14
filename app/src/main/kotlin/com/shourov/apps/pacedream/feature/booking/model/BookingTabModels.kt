package com.shourov.apps.pacedream.feature.booking.model

import com.shourov.apps.pacedream.model.BookingStatus
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Booking role — used for display badges on cards (matching iOS BookingRole).
 */
enum class BookingRole(val apiValue: String, val displayName: String) {
    RENTER("renter", "Guest"),
    HOST("host", "Host")
}

/**
 * Filter category — used internally to classify each booking into a tab.
 * Matches iOS BookingFilterCategory.
 */
enum class BookingFilterCategory {
    UPCOMING, PAST, CANCELLED
}

/**
 * Status display config — matching iOS BookingStatusConfig.
 */
data class BookingStatusConfig(
    val label: String,
    val filterCategory: BookingFilterCategory,
    val badgeColor: String // "yellow", "blue", "green", "red", "gray"
)

/**
 * Status filter tabs matching iOS BookingTab: All / Upcoming / Past / Cancelled.
 * These are the primary navigation tabs (no more Trips/Hosting split).
 */
enum class BookingStatusFilter(val displayName: String) {
    ALL("All"),
    UPCOMING("Upcoming"),
    PAST("Past"),
    CANCELLED("Cancelled");

    fun matches(status: BookingStatus): Boolean {
        return when (this) {
            ALL -> true
            UPCOMING -> status == BookingStatus.CONFIRMED || status == BookingStatus.PENDING
            PAST -> status == BookingStatus.COMPLETED
            CANCELLED -> status == BookingStatus.CANCELLED || status == BookingStatus.REJECTED
        }
    }
}

/**
 * A single booking item parsed from the API.
 * Now includes [role] to support merged guest+host list (matching iOS UnifiedBookingItem).
 */
data class BookingItem(
    val id: String,
    val role: BookingRole = BookingRole.RENTER,
    val propertyId: String,
    val propertyName: String,
    val propertyImage: String?,
    val location: String?,
    val hostName: String,
    val guestName: String,
    val startDate: String,
    val endDate: String,
    val totalPrice: Double,
    val currency: String,
    val status: BookingStatus,
    val guestCount: Int,
    val createdAt: String?,
    val nightsCount: Int = 0,
    val perNightPrice: Double? = null
) {
    val formattedStartDate: String
        get() = formatDate(startDate)

    val formattedEndDate: String
        get() = formatDate(endDate)

    val formattedPrice: String
        get() {
            val symbol = when (currency.uppercase()) {
                "USD" -> "$"
                "EUR" -> "\u20AC"
                "GBP" -> "\u00A3"
                else -> currency
            }
            return "$symbol${String.format("%.0f", totalPrice)}"
        }

    val formattedDateRange: String
        get() {
            val start = formatDateMedium(startDate)
            val end = formatDateMedium(endDate)
            return if (start.isNotBlank() && end.isNotBlank()) "$start \u2013 $end" else "\u2014"
        }

    val guestLabel: String
        get() = "$guestCount guest${if (guestCount != 1) "s" else ""}"

    private fun formatDate(dateString: String): String {
        if (dateString.isBlank()) return ""
        val formats = listOf(
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
            "yyyy-MM-dd'T'HH:mm:ss'Z'",
            "yyyy-MM-dd HH:mm:ss",
            "yyyy-MM-dd"
        )
        for (fmt in formats) {
            try {
                val inputFormat = SimpleDateFormat(fmt, Locale.US)
                val date = inputFormat.parse(dateString) ?: continue
                return SimpleDateFormat("MMM dd", Locale.US).format(date)
            } catch (_: Exception) {
                continue
            }
        }
        return dateString
    }

    private fun formatDateMedium(dateString: String): String {
        if (dateString.isBlank()) return ""
        val formats = listOf(
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
            "yyyy-MM-dd'T'HH:mm:ss'Z'",
            "yyyy-MM-dd HH:mm:ss",
            "yyyy-MM-dd"
        )
        for (fmt in formats) {
            try {
                val inputFormat = SimpleDateFormat(fmt, Locale.US)
                val date = inputFormat.parse(dateString) ?: continue
                return SimpleDateFormat("MMM d, yyyy", Locale.US).format(date)
            } catch (_: Exception) {
                continue
            }
        }
        return dateString
    }
}

/**
 * UI state for the bookings tab
 */
sealed class BookingTabUiState {
    data object Loading : BookingTabUiState()
    data class Success(
        val bookings: List<BookingItem>,
        val role: BookingRole = BookingRole.RENTER,
        val isRefreshing: Boolean = false,
        val hasMore: Boolean = false,
        val statusFilter: BookingStatusFilter = BookingStatusFilter.ALL,
        val filteredBookings: List<BookingItem> = bookings,
        val upcomingBookings: List<BookingItem> = emptyList(),
        val pastBookings: List<BookingItem> = emptyList(),
        val cancelledBookings: List<BookingItem> = emptyList()
    ) : BookingTabUiState() {
        fun count(tab: BookingStatusFilter): Int = when (tab) {
            BookingStatusFilter.ALL -> bookings.size
            BookingStatusFilter.UPCOMING -> upcomingBookings.size
            BookingStatusFilter.PAST -> pastBookings.size
            BookingStatusFilter.CANCELLED -> cancelledBookings.size
        }
    }
    data class Error(val message: String) : BookingTabUiState()
    data object Empty : BookingTabUiState()
    data object RequiresAuth : BookingTabUiState()
}

/**
 * Events from the booking tab UI
 */
sealed class BookingTabEvent {
    data object Refresh : BookingTabEvent()
    data class RoleChanged(val role: BookingRole) : BookingTabEvent()
    data class StatusFilterChanged(val filter: BookingStatusFilter) : BookingTabEvent()
    data class BookingClicked(val bookingId: String) : BookingTabEvent()
    data object LoadMore : BookingTabEvent()
}
