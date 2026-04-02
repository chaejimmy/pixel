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
    val error: String? = null,
    // ── Pre-calculated fields to avoid UI thread jank ──
    val monthlyEarnings: Double = 0.0,
    val underReviewListingsCount: Int = 0,
    val pendingRequestsCount: Int = 0,
    val upcomingBookingsCount: Int = 0,
    val topUpcomingBookings: List<HostBookingDTO> = emptyList(),
    val pendingListings: List<Property> = emptyList(),
    val topActiveListings: List<Property> = emptyList(),
    val recentEvents: List<DashboardEvent> = emptyList()
) {
    // ── History events (iOS parity: DashboardEvent) ──────────

    data class DashboardEvent(
        val id: String,
        val title: String,
        val subtitle: String,
        val createdAt: Long
    )
}

/** 
 * Computes dashboard summaries - iOS parity: HostDataStore.
 * Must be called from a background thread to avoid ANR.
 */
fun computeDashboardData(
    data: HostDashboardData,
    bookings: List<HostBookingDTO>,
    listings: List<Property>
): HostDashboardData {
    val cal = java.util.Calendar.getInstance()
    val currentMonth = cal.get(java.util.Calendar.MONTH)
    val currentYear = cal.get(java.util.Calendar.YEAR)

    val monthlyEarnings = bookings.filter { isConfirmedBooking(it) }
        .mapNotNull { booking ->
            val created = parseDate(booking.createdAt) ?: return@mapNotNull null
            cal.timeInMillis = created
            if (cal.get(java.util.Calendar.MONTH) == currentMonth &&
                cal.get(java.util.Calendar.YEAR) == currentYear) {
                booking.resolvedHostEarnings ?: booking.resolvedTotal
            } else null
        }.sum()

    val underReviewListingsCount = listings.count { it.isPendingReview }
    val pendingRequestsCount = bookings.count { isPendingStatus(it.status) }
    val upcomingBookingsCount = bookings.count { isConfirmedBooking(it) && isUpcoming(it) }

    val topUpcomingBookings = bookings.filter { isConfirmedBooking(it) && isUpcoming(it) }
        .sortedBy { parseDate(it.resolvedStart) ?: Long.MAX_VALUE }
        .take(5)

    val underReviewListings = listings.filter { it.isPendingReview }
    val activeListings = listings.filter { it.isActiveStatus }
    // Pending listings for the dedicated dashboard preview section
    val pendingListings = underReviewListings.take(5)
    // iOS parity: show under-review first, then active, then any other listings.
    // Include ALL listings so the dashboard doesn't show "Welcome to hosting" when
    // the host has listings in any state (draft, unrecognized status, etc.).
    val topActiveListings = (underReviewListings + activeListings +
        listings.filter { !it.isPendingReview && !it.isActiveStatus }).distinctBy { it.id }.take(5)

    val recentEvents = bookings.mapNotNull { booking ->
        val created = parseDate(booking.createdAt) ?: return@mapNotNull null
        val statusLower = (booking.status ?: "").lowercase()
        val listingTitle = booking.resolvedListingTitle
        val guest = booking.resolvedGuestName

        when {
            isPendingStatus(booking.status) -> HostDashboardData.DashboardEvent(
                id = "pending-${booking.id}",
                title = "New booking request",
                subtitle = "$guest requested $listingTitle",
                createdAt = created
            )
            isConfirmedBooking(booking) && (statusLower.contains("confirm") ||
                statusLower.contains("book") || statusLower.contains("active") ||
                statusLower.contains("accept")) -> {
                val paymentTitle = when (booking.resolvedPayoutStatus?.lowercase()) {
                    "transferred" -> "Payment received"
                    "blocked" -> "Payout on hold"
                    else -> "Earning pending"
                }
                val displayAmount = booking.resolvedHostEarnings ?: booking.resolvedTotal
                HostDashboardData.DashboardEvent(
                    id = "confirmed-${booking.id}",
                    title = paymentTitle,
                    subtitle = "$listingTitle • $${String.format("%.0f", displayAmount)}",
                    createdAt = created
                )
            }
            else -> null
        }
    }.sortedByDescending { it.createdAt }.take(6)

    return data.copy(
        bookings = bookings,
        listings = listings,
        monthlyEarnings = monthlyEarnings,
        underReviewListingsCount = underReviewListingsCount,
        pendingRequestsCount = pendingRequestsCount,
        upcomingBookingsCount = upcomingBookingsCount,
        topUpcomingBookings = topUpcomingBookings,
        pendingListings = pendingListings,
        topActiveListings = topActiveListings,
        recentEvents = recentEvents
    )
}

// ── Helpers ──────────────────────────────────────────────

fun isPendingStatus(status: String?): Boolean {
    val s = (status ?: "").trim().lowercase()
    if (s.isEmpty()) return false
    if (s.contains("pending")) return true
    return s == "requires_capture" || s == "created" || s == "pending_host"
}

fun isConfirmedBooking(booking: HostBookingDTO): Boolean {
    val s = (booking.status ?: "").trim().lowercase()
    return s.contains("confirm") || s.contains("accept") ||
        s.contains("active") || s == "booked"
}

fun isUpcoming(booking: HostBookingDTO): Boolean {
    val startMs = parseDate(booking.resolvedStart) ?: return false
    return startMs > System.currentTimeMillis()
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

private val threadLocalSdfCache = ThreadLocal.withInitial {
    mutableMapOf<String, java.text.SimpleDateFormat>()
}

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
    val cache = threadLocalSdfCache.get()
    for (fmt in formats) {
        try {
            val sdf = cache.getOrPut(fmt) {
                java.text.SimpleDateFormat(fmt, java.util.Locale.US).apply {
                    timeZone = java.util.TimeZone.getTimeZone("UTC")
                }
            }
            return sdf.parse(dateString)?.time
        } catch (_: Exception) { /* try next format */ }
    }
    return null
}
