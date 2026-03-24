package com.shourov.apps.pacedream.feature.host.data

// Stripe Connect Account (dual camelCase/snake_case for backend compatibility)
data class ConnectAccount(
    val id: String = "",
    val status: ConnectAccountStatus = ConnectAccountStatus.NOT_CREATED,
    val country: String = "US",
    val email: String = "",
    val businessType: String? = null,
    val business_type: String? = null,
    val chargesEnabled: Boolean = false,
    val charges_enabled: Boolean = false,
    val payoutsEnabled: Boolean = false,
    val payouts_enabled: Boolean = false,
    val detailsSubmitted: Boolean = false,
    val details_submitted: Boolean = false,
    val requirements: ConnectRequirements? = null,
    val createdAt: String = "",
    val created_at: String? = null
) {
    val resolvedBusinessType: String? get() = businessType ?: business_type
    val resolvedChargesEnabled: Boolean get() = chargesEnabled || charges_enabled
    val resolvedPayoutsEnabled: Boolean get() = payoutsEnabled || payouts_enabled
    val resolvedDetailsSubmitted: Boolean get() = detailsSubmitted || details_submitted
    val resolvedCreatedAt: String get() = createdAt.ifEmpty { created_at ?: "" }
}

enum class ConnectAccountStatus {
    NOT_CREATED, PENDING, UNDER_REVIEW, ENABLED, RESTRICTED, REJECTED;

    companion object {
        fun fromString(value: String): ConnectAccountStatus {
            val normalized = value.trim().uppercase().replace("-", "_").replace(" ", "_")
            return entries.firstOrNull { it.name == normalized } ?: NOT_CREATED
        }
    }
}

data class ConnectRequirements(
    val currentlyDue: List<String>? = null,
    val currently_due: List<String>? = null,
    val eventuallyDue: List<String>? = null,
    val eventually_due: List<String>? = null,
    val pastDue: List<String>? = null,
    val past_due: List<String>? = null,
    val pendingVerification: List<String>? = null,
    val pending_verification: List<String>? = null
) {
    val resolvedCurrentlyDue: List<String> get() = currentlyDue ?: currently_due ?: emptyList()
    val resolvedEventuallyDue: List<String> get() = eventuallyDue ?: eventually_due ?: emptyList()
    val resolvedPastDue: List<String> get() = pastDue ?: past_due ?: emptyList()
    val resolvedPendingVerification: List<String> get() = pendingVerification ?: pending_verification ?: emptyList()
}

data class AccountLink(
    val url: String? = null,
    val expiresAt: Long? = null,
    val expires_at: Long? = null
) {
    val resolvedUrl: String? get() = url
    val resolvedExpiresAt: Long? get() = expiresAt ?: expires_at
}

data class LoginLink(
    val url: String? = null,
    val expiresAt: Long? = null,
    val expires_at: Long? = null
) {
    val resolvedUrl: String? get() = url
    val resolvedExpiresAt: Long? get() = expiresAt ?: expires_at
}

// Stripe Connect Balance
data class ConnectBalance(
    val available: List<BalanceAmount> = emptyList(),
    val pending: List<BalanceAmount> = emptyList(),
    val instantAvailable: List<BalanceAmount> = emptyList(),
    val instant_available: List<BalanceAmount>? = null
) {
    val resolvedInstantAvailable: List<BalanceAmount> get() =
        instantAvailable.ifEmpty { instant_available ?: emptyList() }
}

data class BalanceAmount(
    val amount: Int = 0,
    val currency: String = "usd",
    val sourceTypes: Map<String, Int>? = null,
    val source_types: Map<String, Int>? = null
) {
    val resolvedSourceTypes: Map<String, Int> get() = sourceTypes ?: source_types ?: emptyMap()
}

// Stripe Connect Transfer
data class Transfer(
    val id: String,
    val amount: Int,
    val currency: String,
    val destination: String = "",
    val description: String? = null,
    val status: String = "pending",
    val bookingId: String? = null,
    val booking_id: String? = null,
    val createdAt: String = "",
    val created_at: String? = null
) {
    val resolvedBookingId: String? get() = bookingId ?: booking_id
    val resolvedCreatedAt: String get() = createdAt.ifEmpty { created_at ?: "" }
}

// Stripe Connect Payout
data class Payout(
    val id: String,
    val amount: Int,
    val currency: String,
    val status: String = "pending",
    val arrivalDate: String = "",
    val arrival_date: String? = null,
    val description: String? = null,
    val createdAt: String = "",
    val created_at: String? = null
) {
    val resolvedArrivalDate: String get() = arrivalDate.ifEmpty { arrival_date ?: "" }
    val resolvedCreatedAt: String get() = createdAt.ifEmpty { created_at ?: "" }
}

// Payout Status from backend
data class PayoutStatus(
    val state: PayoutState = PayoutState.NOT_CONNECTED,
    val details: String? = null,
    val chargesEnabled: Boolean = false,
    val payoutsEnabled: Boolean = false,
    val detailsSubmitted: Boolean = false,
    val requirementsCurrentlyDue: List<String> = emptyList()
)

enum class PayoutState {
    CONNECTED, PENDING, NOT_CONNECTED;

    companion object {
        fun fromString(value: String): PayoutState {
            val normalized = value.trim().uppercase().replace("-", "_").replace(" ", "_")
            return entries.firstOrNull { it.name == normalized } ?: NOT_CONNECTED
        }
    }
}

// Payout Method
data class PayoutMethod(
    val id: String,
    val type: String,
    val label: String,
    val isPrimary: Boolean = false
)

// Earnings UI State for tabbed view (matching iOS)
data class HostEarningsUiState(
    val selectedTab: Int = 0,
    val balance: ConnectBalance? = null,
    val transfers: List<Transfer> = emptyList(),
    val payouts: List<Payout> = emptyList(),
    val connectAccount: ConnectAccount? = null,
    // Comprehensive dashboard data (from /host/earnings/dashboard - iOS parity)
    val dashboardData: EarningsDashboardResponse? = null,
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val errorMessage: String? = null,
    val showPayoutSheet: Boolean = false,
    val payoutAmount: String = ""
)
