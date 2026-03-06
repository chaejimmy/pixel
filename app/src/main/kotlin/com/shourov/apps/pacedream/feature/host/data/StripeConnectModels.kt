package com.shourov.apps.pacedream.feature.host.data

import com.google.gson.annotations.SerializedName

// Stripe Connect Account
data class ConnectAccount(
    val id: String = "",
    val status: ConnectAccountStatus = ConnectAccountStatus.NOT_CREATED,
    val country: String = "US",
    val email: String = "",
    @SerializedName("business_type") val businessType: String? = null,
    @SerializedName("charges_enabled") val chargesEnabled: Boolean = false,
    @SerializedName("payouts_enabled") val payoutsEnabled: Boolean = false,
    @SerializedName("details_submitted") val detailsSubmitted: Boolean = false,
    val requirements: ConnectRequirements? = null,
    @SerializedName("created_at") val createdAt: String = ""
)

enum class ConnectAccountStatus {
    @SerializedName("not_created") NOT_CREATED,
    @SerializedName("pending") PENDING,
    @SerializedName("under_review") UNDER_REVIEW,
    @SerializedName("enabled") ENABLED,
    @SerializedName("restricted") RESTRICTED,
    @SerializedName("rejected") REJECTED
}

data class ConnectRequirements(
    @SerializedName("currently_due") val currentlyDue: List<String> = emptyList(),
    @SerializedName("eventually_due") val eventuallyDue: List<String> = emptyList(),
    @SerializedName("past_due") val pastDue: List<String> = emptyList(),
    @SerializedName("pending_verification") val pendingVerification: List<String> = emptyList()
)

data class AccountLink(
    val url: String,
    @SerializedName("expires_at") val expiresAt: Long
)

data class LoginLink(
    val url: String,
    @SerializedName("expires_at") val expiresAt: Long
)

// Stripe Connect Balance
data class ConnectBalance(
    val available: List<BalanceAmount> = emptyList(),
    val pending: List<BalanceAmount> = emptyList(),
    @SerializedName("instant_available") val instantAvailable: List<BalanceAmount> = emptyList()
)

data class BalanceAmount(
    val amount: Int = 0,
    val currency: String = "usd",
    @SerializedName("source_types") val sourceTypes: Map<String, Int> = emptyMap()
)

// Stripe Connect Transfer
data class Transfer(
    val id: String,
    val amount: Int,
    val currency: String,
    val destination: String = "",
    val description: String? = null,
    val status: String = "pending",
    @SerializedName("booking_id") val bookingId: String? = null,
    @SerializedName("created_at") val createdAt: String = ""
)

// Stripe Connect Payout
data class Payout(
    val id: String,
    val amount: Int,
    val currency: String,
    val status: String = "pending",
    @SerializedName("arrival_date") val arrivalDate: String = "",
    val description: String? = null,
    @SerializedName("created_at") val createdAt: String = ""
)

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
    CONNECTED, PENDING, NOT_CONNECTED
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
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val errorMessage: String? = null,
    val showPayoutSheet: Boolean = false,
    val payoutAmount: String = ""
)
