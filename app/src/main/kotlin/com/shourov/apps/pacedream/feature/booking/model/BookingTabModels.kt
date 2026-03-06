package com.shourov.apps.pacedream.feature.booking.model

import com.shourov.apps.pacedream.model.BookingStatus
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Booking role matching the web platform's ?role= query parameter.
 *
 * Web uses: /account/bookings?role=renter  (Trips tab)
 *           /account/bookings?role=host    (Hosting tab)
 */
enum class BookingRole(val apiValue: String, val displayName: String) {
    RENTER("renter", "Trips"),
    HOST("host", "Hosting")
}

/**
 * Status filter tabs matching iOS BookingsTabView (iOS parity):
 * All / Upcoming / Past / Cancelled
 */
enum class BookingStatusFilter(val displayName: String) {
    ALL("All"),
    UPCOMING("Upcoming"),
    PAST("Past"),
    CANCELLED("Cancelled");

    fun matches(status: BookingStatus, endDate: String): Boolean {
        return when (this) {
            ALL -> true
            UPCOMING -> status == BookingStatus.CONFIRMED || status == BookingStatus.PENDING
            PAST -> status == BookingStatus.COMPLETED || (status == BookingStatus.CONFIRMED && isEndDatePast(endDate))
            CANCELLED -> status == BookingStatus.CANCELLED || status == BookingStatus.REJECTED
        }
    }

    private fun isEndDatePast(endDate: String): Boolean {
        if (endDate.isBlank()) return false
        val formats = listOf(
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
            "yyyy-MM-dd'T'HH:mm:ss'Z'",
            "yyyy-MM-dd HH:mm:ss",
            "yyyy-MM-dd"
        )
        for (fmt in formats) {
            try {
                val inputFormat = SimpleDateFormat(fmt, Locale.US)
                val date = inputFormat.parse(endDate) ?: continue
                return date.before(Date())
            } catch (_: Exception) { continue }
        }
        return false
    }
}

/**
 * A single booking item parsed from the API
 */
data class BookingItem(
    val id: String,
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
    val createdAt: String?
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
}

/**
 * UI state for the bookings tab
 */
sealed class BookingTabUiState {
    data object Loading : BookingTabUiState()
    data class Success(
        val bookings: List<BookingItem>,
        val role: BookingRole,
        val isRefreshing: Boolean = false,
        val hasMore: Boolean = false,
        val statusFilter: BookingStatusFilter = BookingStatusFilter.ALL,
        val filteredBookings: List<BookingItem> = bookings
    ) : BookingTabUiState()
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
