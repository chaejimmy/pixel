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

// ── Earnings Dashboard Response (iOS parity: all-in-one endpoint) ─────

/**
 * Response from GET /host/earnings/dashboard.
 * Matches the backend response exactly. This is the single source of truth
 * endpoint that iOS uses — replaces the 4 separate stripe endpoints that
 * were returning 404s.
 */
data class EarningsDashboardResponse(
    val success: Boolean = false,
    val stripe: DashboardStripeStatus? = null,
    val balances: DashboardBalances? = null,
    val payouts: List<DashboardPayout> = emptyList(),
    val transactions: List<DashboardTransaction> = emptyList(),
    val stats: DashboardStats? = null,
    val payoutRules: DashboardPayoutRules? = null
)

data class DashboardStripeStatus(
    val connected: Boolean = false,
    val accountId: String? = null,
    val chargesEnabled: Boolean = false,
    val payoutsEnabled: Boolean = false,
    val detailsSubmitted: Boolean = false,
    val onboardingComplete: Boolean = false,
    val disabledReason: String? = null,
    val requirements: List<String> = emptyList()
)

data class DashboardBalances(
    val available: Double = 0.0,
    val pending: Double = 0.0,
    val settling: Double = 0.0,
    val readyForTransfer: Double = 0.0,
    val lifetime: Double = 0.0,
    val currency: String = "usd",
    val fundsSettling: Boolean = false,
    val settlingNote: String? = null
)

data class DashboardPayout(
    val id: String = "",
    val amount: Double = 0.0,
    val currency: String = "usd",
    val status: String = "unknown",
    val method: String? = null,
    val arrivalDate: String? = null,
    val createdAt: String? = null,
    val description: String? = null,
    val destination: DashboardPayoutDestination? = null
)

data class DashboardPayoutDestination(
    val last4: String? = null,
    val bankName: String? = null
)

data class DashboardTransaction(
    val id: String = "",
    val bookingId: String? = null,
    val bookingType: String? = null,
    val amount: Double = 0.0,
    val grossAmount: Double = 0.0,
    val stripeProcessingFee: Double = 0.0,
    val netAmount: Double = 0.0,
    val currency: String = "usd",
    val status: String? = null,
    val payoutStatus: String? = null,
    val platformFee: Double = 0.0,
    val releaseRule: String? = null,
    val payoutReleaseAt: String? = null,
    val stripeTransferId: String? = null,
    val blockedReason: String? = null,
    val createdAt: String? = null,
    val description: String? = null
)

data class DashboardStats(
    val totalTransactions: Int = 0,
    val completedPayouts: Int = 0,
    val completedAmount: Double = 0.0,
    val heldPayouts: Int = 0,
    val heldAmount: Double = 0.0,
    val settlingPayouts: Int = 0,
    val settlingAmount: Double = 0.0,
    val readyPayouts: Int = 0,
    val readyAmount: Double = 0.0,
    val blockedPayouts: Int = 0,
    val blockedAmount: Double = 0.0
)

data class DashboardPayoutRules(
    val shortBookingThresholdHours: Int = 24,
    val shortBookingRule: String = "",
    val longBookingRule: String = ""
)

// ── Connection state derived from dashboard (iOS parity) ─────

enum class EarningsConnectionState {
    NOT_CONNECTED,
    PENDING,
    CONNECTED;

    companion object {
        fun from(stripe: DashboardStripeStatus?): EarningsConnectionState {
            if (stripe == null) return NOT_CONNECTED
            return when {
                stripe.onboardingComplete && stripe.payoutsEnabled -> CONNECTED
                stripe.connected || stripe.detailsSubmitted -> PENDING
                else -> NOT_CONNECTED
            }
        }
    }
}

// ── Screen-level state: one sealed hierarchy for the entire Earnings screen ──

sealed class EarningsScreenState {
    /** Initial load in progress */
    data object Loading : EarningsScreenState()

    /** Auth token missing / expired — show sign-in prompt */
    data object SessionExpired : EarningsScreenState()

    /** Stripe account not created yet */
    data object StripeNotConnected : EarningsScreenState()

    /** Stripe onboarding started but incomplete */
    data class StripePending(
        val requirements: List<String> = emptyList(),
        val disabledReason: String? = null
    ) : EarningsScreenState()

    /** Stripe connected, dashboard data available (may have zero earnings) */
    data class Ready(
        val dashboard: EarningsDashboardResponse,
        val hasEarnings: Boolean
    ) : EarningsScreenState()

    /** Non-auth error (network, server, etc.) */
    data class Error(val message: String) : EarningsScreenState()
}

// Earnings UI State for tabbed view (matching iOS)
data class HostEarningsUiState(
    val selectedTab: Int = 0,
    val screenState: EarningsScreenState = EarningsScreenState.Loading,
    // Dashboard data (from /host/earnings/dashboard)
    val dashboard: EarningsDashboardResponse? = null,
    val connectionState: EarningsConnectionState = EarningsConnectionState.NOT_CONNECTED,
    val isRefreshing: Boolean = false,
    val showPayoutSheet: Boolean = false,
    val payoutAmount: String = "",
    val payoutError: String? = null
)
