package com.shourov.apps.pacedream.feature.host.data

import androidx.compose.ui.graphics.vector.ImageVector
import com.shourov.apps.pacedream.model.BookingModel
import com.shourov.apps.pacedream.model.Property

/**
 * Host Dashboard UI State - iOS parity.
 *
 * Matches iOS HostDashboardView + HostDataStore combined state.
 */
data class HostDashboardData(
    val userName: String = "Host",
    // From HostDashboardOverview (iOS)
    val totalBookings: Int = 0,
    val totalRevenue: Double = 0.0,
    val averageRating: Double = 0.0,
    val totalReviews: Int = 0,
    val occupancyRate: Double = 0.0,
    val responseRate: Double = 0.0,
    val activeListings: Int = 0,
    val pendingBookings: Int = 0,
    // From HostDataStore (iOS)
    val bookings: List<HostBookingDTO> = emptyList(),
    val listings: List<Property> = emptyList(),
    // Payout status (iOS: PayoutsService.PayoutStatus)
    val payoutState: PayoutConnectionState = PayoutConnectionState.NOT_CONNECTED,
    val payoutDetails: String? = null,
    // Server-driven payout setup prompt eligibility
    val shouldShowPayoutSetupPrompt: Boolean = false,
    val payoutPromptReason: String? = null,
    // UI state
    val isLoading: Boolean = false,
    val hasLoaded: Boolean = false,
    val error: String? = null
) {
    // ── KPI computations (iOS parity: HostDataStore) ──────────

    val activeListingsCount: Int get() = activeListings

    val underReviewListingsCount: Int get() = listings.count { listing ->
        val s = listing.status.trim().lowercase()
        s == "pending_review" || s == "pending" || s == "under_review" || s == "in_review"
    }

    val pendingRequestsCount: Int get() = bookings.count { booking ->
        isPendingStatus(booking.status)
    }

    val upcomingBookingsCount: Int get() = bookings.count { booking ->
        isConfirmedBooking(booking) && isUpcoming(booking)
    }

    val monthlyEarnings: Double get() {
        val cal = java.util.Calendar.getInstance()
        val currentMonth = cal.get(java.util.Calendar.MONTH)
        val currentYear = cal.get(java.util.Calendar.YEAR)

        return bookings.filter { isConfirmedBooking(it) }
            .mapNotNull { booking ->
                val created = parseDate(booking.createdAt) ?: return@mapNotNull null
                cal.timeInMillis = created
                if (cal.get(java.util.Calendar.MONTH) == currentMonth &&
                    cal.get(java.util.Calendar.YEAR) == currentYear) {
                    // iOS parity: use hostEarnings (net) when available, fall back to total
                    booking.resolvedHostEarnings ?: booking.resolvedTotal
                } else null
            }.sum()
    }

    val topUpcomingBookings: List<HostBookingDTO> get() =
        bookings.filter { isConfirmedBooking(it) && isUpcoming(it) }
            .sortedBy { parseDate(it.resolvedStart) ?: Long.MAX_VALUE }
            .take(5)

    val topActiveListings: List<Property> get() =
        listings.filter { it.isAvailable }.take(5)

    /** All listings regardless of status — used for "new host" detection */
    val hasAnyListings: Boolean get() = listings.isNotEmpty()

    // ── History events (iOS parity: DashboardEvent) ──────────

    data class DashboardEvent(
        val id: String,
        val title: String,
        val subtitle: String,
        val createdAt: Long
    )

    val recentEvents: List<DashboardEvent> get() =
        bookings.mapNotNull { booking ->
            val created = parseDate(booking.createdAt) ?: return@mapNotNull null
            val statusLower = (booking.status ?: "").lowercase()
            val listingTitle = booking.resolvedListingTitle
            val guest = booking.resolvedGuestName

            when {
                isPendingStatus(booking.status) -> DashboardEvent(
                    id = "pending-${booking.id}",
                    title = "New booking request",
                    subtitle = "$guest requested $listingTitle",
                    createdAt = created
                )
                // iOS parity: show confirmed AND past bookings in history
                isConfirmedBooking(booking) && (statusLower.contains("confirm") ||
                    statusLower.contains("book") || statusLower.contains("active") ||
                    statusLower.contains("accept")) -> {
                    // iOS parity: use payoutStatus to determine label
                    val paymentTitle = when (booking.resolvedPayoutStatus?.lowercase()) {
                        "transferred" -> "Payment received"
                        "blocked" -> "Payout on hold"
                        else -> "Earning pending"
                    }
                    // iOS parity: use hostEarnings (net) when available
                    val displayAmount = booking.resolvedHostEarnings ?: booking.resolvedTotal
                    DashboardEvent(
                        id = "confirmed-${booking.id}",
                        title = paymentTitle,
                        subtitle = "$listingTitle • $${String.format("%.0f", displayAmount)}",
                        createdAt = created
                    )
                }
                else -> null
            }
        }
        .sortedByDescending { it.createdAt }
        .take(6)

    // ── Helpers ──────────────────────────────────────────────

    private fun isPendingStatus(status: String?): Boolean {
        val s = (status ?: "").trim().lowercase()
        if (s.isEmpty()) return false
        if (s.contains("pending")) return true
        return s == "requires_capture" || s == "created" || s == "pending_host"
    }

    private fun isConfirmedBooking(booking: HostBookingDTO): Boolean {
        val s = (booking.status ?: "").trim().lowercase()
        return s.contains("confirm") || s.contains("accept") ||
            s.contains("active") || s == "booked"
    }

    private fun isUpcoming(booking: HostBookingDTO): Boolean {
        val startMs = parseDate(booking.resolvedStart) ?: return false
        return startMs > System.currentTimeMillis()
    }
}

/** Payout connection state matching iOS PayoutsService.PayoutStatus.State */
enum class PayoutConnectionState {
    CONNECTED,
    PENDING,
    NOT_CONNECTED
}

// ── Quick Action Item ───────────────────────────────────────────

data class QuickAction(
    val title: String,
    val icon: ImageVector,
    val onClick: () -> Unit
)

// ── Host Earnings UI State (iOS parity: Stripe Connect) ─────────

data class HostEarningsData(
    val connectionState: PayoutConnectionState = PayoutConnectionState.NOT_CONNECTED,
    val payoutMethods: List<PayoutMethod> = emptyList(),
    val requirementsCurrentlyDue: List<String> = emptyList(),
    // Revenue data from /hosts/dashboard/revenue
    val totalRevenue: Double = 0.0,
    val grossRevenue: Double = 0.0,
    val platformFees: Double = 0.0,
    val netRevenue: Double = 0.0,
    val revenueByMonth: List<RevenueByMonth> = emptyList(),
    val revenueByListing: List<RevenueByListing> = emptyList(),
    val isLoading: Boolean = false,
    val isBusy: Boolean = false,
    val error: String? = null,
    val onboardingUrl: String? = null,
    val loginUrl: String? = null
)

// ── Host Listings UI State ──────────────────────────────────────

data class HostListingsData(
    val listings: List<Property> = emptyList(),
    val selectedFilter: String = "All",
    val selectedSort: String = "Date (Newest)",
    val isLoading: Boolean = false,
    val error: String? = null
)

// ── Host Bookings UI State (iOS parity: segments) ───────────────

data class HostBookingsData(
    val totalBookings: Int = 0,
    val pendingBookings: Int = 0,
    val selectedStatus: String = "Pending",
    val bookings: List<HostBookingDTO> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

/** Booking status segments matching iOS HostBookingsView */
enum class BookingSegment {
    PENDING, CONFIRMED, PAST, CANCELLED
}

enum class ListingFilter {
    ALL, ACTIVE, PENDING, UNAVAILABLE
}

enum class ListingSortOption {
    DATE_NEWEST, DATE_OLDEST, PRICE_HIGH_LOW, PRICE_LOW_HIGH, RATING_HIGH_LOW
}

// ── Date parsing utility ────────────────────────────────────────

fun parseDate(dateString: String?): Long? {
    if (dateString.isNullOrBlank()) return null
    val formats = listOf(
        "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
        "yyyy-MM-dd'T'HH:mm:ss'Z'",
        "yyyy-MM-dd'T'HH:mm:ssZ",
        "yyyy-MM-dd'T'HH:mm:ss",
        "yyyy-MM-dd",
        "MM/dd/yyyy"
    )
    for (fmt in formats) {
        try {
            val sdf = java.text.SimpleDateFormat(fmt, java.util.Locale.US)
            sdf.timeZone = java.util.TimeZone.getTimeZone("UTC")
            return sdf.parse(dateString)?.time
        } catch (_: Exception) { /* try next format */ }
    }
    return null
}
