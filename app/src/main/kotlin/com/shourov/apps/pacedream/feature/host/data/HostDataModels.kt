package com.shourov.apps.pacedream.feature.host.data

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddHome
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Money
import androidx.compose.ui.graphics.vector.ImageVector
import com.shourov.apps.pacedream.model.BookingModel
import com.shourov.apps.pacedream.model.Property

// Host Dashboard UI State
data class HostDashboardData(
    val userName: String = "Host",
    val totalEarnings: Double = 0.0,
    val activeListings: Int = 0,
    val monthlyEarnings: Double = 0.0,
    val occupancyRate: Double = 0.0,
    val averageRating: Double = 0.0,
    val totalBookings: Int = 0,
    val recentBookings: List<BookingModel> = emptyList(),
    val myListings: List<Property> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

// Quick Action Item
data class QuickAction(
    val title: String,
    val icon: ImageVector,
    val onClick: () -> Unit
)

// Performance Metric Item
data class PerformanceMetric(
    val title: String,
    val value: String,
    val icon: ImageVector,
    val color: androidx.compose.ui.graphics.Color
)

// Host Earnings UI State
data class HostEarningsData(
    val totalEarnings: Double = 0.0,
    val availableBalance: Double = 0.0,
    val selectedTimeRange: String = "Month",
    val earningsData: List<Pair<String, Double>> = emptyList(), // e.g., ("Jan", 1200.0)
    val earningsBreakdown: List<EarningsBreakdownItem> = emptyList(),
    val recentTransactions: List<Transaction> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

data class EarningsBreakdownItem(
    val category: String,
    val amount: Double,
    val percentage: Double
)

data class Transaction(
    val id: String,
    val description: String,
    val amount: Double,
    val date: String,
    val type: TransactionType
)

// Host Listings UI State
data class HostListingsData(
    val listings: List<Property> = emptyList(),
    val selectedFilter: String = "All",
    val selectedSort: String = "Date (Newest)",
    val isLoading: Boolean = false,
    val error: String? = null
)

// Host Bookings UI State
data class HostBookingsData(
    val totalBookings: Int = 0,
    val pendingBookings: Int = 0,
    val selectedStatus: String = "All",
    val bookings: List<BookingModel> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

enum class TimeRange {
    WEEK, MONTH, QUARTER, YEAR, ALL_TIME
}

enum class ListingFilter {
    ALL, ACTIVE, PENDING, UNAVAILABLE
}

enum class ListingSortOption {
    DATE_NEWEST, DATE_OLDEST, PRICE_HIGH_LOW, PRICE_LOW_HIGH, RATING_HIGH_LOW
}

// Note: Use com.shourov.apps.pacedream.model.BookingStatus for booking status values

enum class TransactionType {
    BOOKING, WITHDRAWAL, FEE
}
