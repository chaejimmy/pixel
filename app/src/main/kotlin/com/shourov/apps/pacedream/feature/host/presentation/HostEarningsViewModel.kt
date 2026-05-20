package com.shourov.apps.pacedream.feature.host.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shourov.apps.pacedream.feature.host.data.EarningsConnectionState
import com.shourov.apps.pacedream.feature.host.data.EarningsScreenState
import com.shourov.apps.pacedream.feature.host.data.HostEarningsData
import com.shourov.apps.pacedream.feature.host.data.HostEarningsUiState
import com.shourov.apps.pacedream.feature.host.data.HostPayoutTelemetry
import com.shourov.apps.pacedream.feature.host.data.HostRepository
import com.shourov.apps.pacedream.feature.host.data.StripeConnectRepository
import com.shourov.apps.pacedream.core.network.auth.AuthSession
import com.shourov.apps.pacedream.core.network.auth.AuthState
import com.shourov.apps.pacedream.core.network.auth.TokenStorage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * Host Earnings ViewModel - iOS parity.
 *
 * Uses the single /host/earnings/dashboard endpoint (same as iOS PayoutsService.fetchDashboard)
 * instead of 4 separate stripe endpoints that don't exist on the backend.
 *
 * Screen states are modelled as a sealed hierarchy so the UI can render each
 * scenario (session expired, Stripe not connected, pending, ready) without
 * mixing concerns.
 */
@HiltViewModel
class HostEarningsViewModel @Inject constructor(
    private val hostRepository: HostRepository,
    private val stripeConnectRepository: StripeConnectRepository,
    private val authSession: AuthSession,
    private val tokenStorage: TokenStorage,
) : ViewModel() {

    // Legacy state for backward compatibility
    private val _uiState = MutableStateFlow(HostEarningsData())
    val uiState: StateFlow<HostEarningsData> = _uiState.asStateFlow()

    private val _earningsUiState = MutableStateFlow(HostEarningsUiState())
    val earningsUiState: StateFlow<HostEarningsUiState> = _earningsUiState.asStateFlow()

    /** URL to present in Custom Tabs (iOS parity: presentingURL). */
    private val _presentingUrl = MutableStateFlow<String?>(null)
    val presentingUrl: StateFlow<String?> = _presentingUrl.asStateFlow()

    private val _isBusy = MutableStateFlow(false)
    val isBusy: StateFlow<Boolean> = _isBusy.asStateFlow()

    private val _inlineError = MutableStateFlow<String?>(null)
    val inlineError: StateFlow<String?> = _inlineError.asStateFlow()

    init {
        // Observe auth state reactively so we load data once auth is confirmed,
        // rather than racing against auth initialization on startup.
        viewModelScope.launch {
            authSession.authState.collect { state ->
                when (state) {
                    AuthState.Authenticated -> loadAllData()
                    AuthState.Unauthenticated -> {
                        _earningsUiState.value = _earningsUiState.value.copy(
                            screenState = EarningsScreenState.SessionExpired,
                            isRefreshing = false
                        )
                    }
                    else -> { /* AuthState.Unknown — still initializing, wait */ }
                }
            }
        }
    }

    private fun loadAllData() {
        viewModelScope.launch {
            val current = _earningsUiState.value

            // Guard against duplicate loads (iOS parity)
            if (current.screenState is EarningsScreenState.Loading && !current.isRefreshing) {
                if (current.screenState == EarningsScreenState.Loading && current.dashboard == null) {
                    // first load, proceed
                } else {
                    Timber.d("[Earnings] Already loading, skipping")
                    return@launch
                }
            }

            _earningsUiState.value = current.copy(
                screenState = if (current.isRefreshing) current.screenState else EarningsScreenState.Loading
            )

            Timber.d("[Earnings] Loading dashboard data...")

            stripeConnectRepository.getEarningsDashboard()
                .onSuccess { dashboard ->
                    val connectionState = EarningsConnectionState.from(dashboard.stripe)
                    Timber.d(
                        "[Earnings] Dashboard loaded. connectionState=$connectionState, " +
                            "available=${dashboard.balances?.available}, " +
                            "payouts=${dashboard.payouts.size}, " +
                            "transactions=${dashboard.transactions.size}"
                    )

                    val screenState = when (connectionState) {
                        EarningsConnectionState.NOT_CONNECTED ->
                            EarningsScreenState.StripeNotConnected

                        EarningsConnectionState.PENDING ->
                            EarningsScreenState.StripePending(
                                requirements = dashboard.stripe?.requirements ?: emptyList(),
                                disabledReason = dashboard.stripe?.disabledReason
                            )

                        EarningsConnectionState.CONNECTED -> {
                            val hasEarnings = (dashboard.balances?.lifetime ?: 0.0) > 0 ||
                                dashboard.transactions.isNotEmpty() ||
                                dashboard.payouts.isNotEmpty()
                            EarningsScreenState.Ready(
                                dashboard = dashboard,
                                hasEarnings = hasEarnings
                            )
                        }
                    }

                    _earningsUiState.value = _earningsUiState.value.copy(
                        screenState = screenState,
                        dashboard = dashboard,
                        connectionState = connectionState,
                        isRefreshing = false
                    )
                }
                .onFailure { exception ->
                    Timber.e(exception, "[Earnings] Dashboard load failed: ${exception.javaClass.simpleName}: ${exception.message}")

                    val rawMessage = exception.message ?: ""
                    val isAuthError = rawMessage.contains("unauthorized", ignoreCase = true) ||
                        rawMessage.contains("401")

                    // iOS parity: when the dashboard endpoint fails with a business
                    // error (host profile not found, no stripe account, 404, etc.),
                    // show the "Set up payouts" screen instead of an error.
                    // iOS falls back to connectionState = .notConnected when
                    // dashboard is nil, which renders the Stripe setup CTA.
                    val isBusinessError = rawMessage.contains("not found", ignoreCase = true) ||
                        rawMessage.contains("no host", ignoreCase = true) ||
                        rawMessage.contains("no stripe", ignoreCase = true) ||
                        rawMessage.contains("not a host", ignoreCase = true) ||
                        rawMessage.contains("profile", ignoreCase = true) ||
                        rawMessage.contains("HTTP 404") ||
                        rawMessage.contains("HTTP 403")

                    val screenState = when {
                        isAuthError -> EarningsScreenState.SessionExpired
                        isBusinessError -> EarningsScreenState.StripeNotConnected
                        else -> {
                            val userMessage = when {
                                exception is java.net.UnknownHostException ->
                                    "No internet connection. Check your network."
                                exception is java.net.SocketTimeoutException ->
                                    "Request timed out. The server may be starting up — try again."
                                exception is java.net.ConnectException ->
                                    "Could not connect to server. Try again."
                                exception is java.io.IOException ||
                                rawMessage.contains("network", ignoreCase = true) ||
                                rawMessage.contains("timeout", ignoreCase = true) ->
                                    "Please check your internet connection and try again."
                                else ->
                                    "Couldn't load earnings data. Pull to refresh."
                            }
                            EarningsScreenState.Error(userMessage)
                        }
                    }

                    _earningsUiState.value = _earningsUiState.value.copy(
                        screenState = screenState,
                        isRefreshing = false
                    )
                }
        }
    }

    fun selectTab(index: Int) {
        _earningsUiState.value = _earningsUiState.value.copy(selectedTab = index)
    }

    fun refreshData() {
        _earningsUiState.value = _earningsUiState.value.copy(isRefreshing = true)
        loadAllData()
    }

    fun showPayoutSheet() {
        _earningsUiState.value = _earningsUiState.value.copy(showPayoutSheet = true)
    }

    fun hidePayoutSheet() {
        // Dismissing the sheet is a fresh-start signal — drop any pending
        // idempotency key so the next "Withdraw" tap mints a new one.
        // The key is persisted (see [TokenStorage.pendingPayoutIdempotencyKey])
        // so we explicitly clear all three fields here.
        if (!tokenStorage.pendingPayoutIdempotencyKey.isNullOrBlank()) {
            HostPayoutTelemetry.pendingKeyDiscarded("sheet_dismissed")
        }
        tokenStorage.clearPendingPayout()
        _earningsUiState.value = _earningsUiState.value.copy(showPayoutSheet = false, payoutAmount = "")
    }

    /**
     * Submit a payout request.  Hardened in three places (audit F-07 / F-08):
     *
     * 1. **Fresh status check before submission.**  Cached
     *    `payoutsEnabled` from the last dashboard fetch can be minutes
     *    or hours stale.  We re-fetch the dashboard right before the
     *    POST and fail closed if the fetch fails or the fresh value is
     *    `false` — better to ask the user to retry than to fire a
     *    payout at a backend that has since restricted the account.
     *
     * 2. **Idempotency-Key persisted across process death.**  Stored
     *    in [TokenStorage] (key + amount + timestamp).  A relaunch
     *    after a crash mid-POST reuses the same key so Stripe Connect
     *    dedupes; the user gets exactly one transfer, not two.
     *
     * 3. **Amount-pinned key.**  If the persisted key was minted for a
     *    different amount than the one the user is now requesting, we
     *    rotate to a fresh key.  Without this, Stripe Connect would
     *    return the prior amount's transfer for a request the user
     *    thinks is a new amount.
     */
    fun requestPayout(amount: Double) {
        if (_earningsUiState.value.isRequestingPayout) return

        val amountInCents = Math.round(amount * 100)
        if (amountInCents <= 0) {
            _earningsUiState.value = _earningsUiState.value.copy(
                payoutError = "Enter an amount greater than zero."
            )
            return
        }

        viewModelScope.launch {
            _earningsUiState.value = _earningsUiState.value.copy(
                isRequestingPayout = true, payoutError = null
            )

            // ── F-07: fresh status check ────────────────────────────
            //
            // We deliberately refetch the full dashboard (instead of a
            // lighter status-only endpoint) for two reasons:
            //   (a) the dashboard endpoint is what populates `available`
            //       balance — re-fetching also catches the case where
            //       another device just drew the balance down to <
            //       amountInCents, which would otherwise let the POST
            //       go out only to fail at Stripe.
            //   (b) one endpoint instead of two means one auth/refresh
            //       cycle, smaller blast radius if the network is flaky.
            val freshOk = refreshStatusForPayout(amountInCents)
            if (!freshOk) {
                // refreshStatusForPayout has already populated payoutError
                // and reset the busy flag — just return.
                return@launch
            }

            // ── F-08: persisted, amount-pinned idempotency key ──────
            val acquired = acquireOrRotateIdempotencyKey(amountInCents)
            HostPayoutTelemetry.requestAttempt(amountInCents, reusedKey = acquired.reused)

            stripeConnectRepository.createPayout(amountInCents.toInt(), idempotencyKey = acquired.key)
                .onSuccess {
                    // Stripe Connect accepted the transfer; the persisted
                    // key has done its job and must be cleared so a
                    // future Withdraw mints a brand-new key.
                    tokenStorage.clearPendingPayout()
                    HostPayoutTelemetry.requestSucceeded(amountInCents)
                    _earningsUiState.value = _earningsUiState.value.copy(
                        isRequestingPayout = false,
                        showPayoutSheet = false,
                        payoutAmount = ""
                    )
                    refreshData()
                }
                .onFailure { exception ->
                    // Key intentionally kept persisted — a retry of the
                    // same amount should hit Stripe with the same key.
                    Timber.e(exception, "[Earnings] Payout request failed")
                    HostPayoutTelemetry.requestFailed(
                        amountInCents,
                        exception::class.simpleName ?: "unknown",
                    )
                    _earningsUiState.value = _earningsUiState.value.copy(
                        isRequestingPayout = false,
                        payoutError = com.pacedream.common.util.UserFacingErrorMapper.map(exception, "We couldn't process your payout. Please try again.")
                    )
                }
        }
    }

    /**
     * Re-fetches the dashboard and confirms `payoutsEnabled=true`
     * before letting the payout POST go out.  Returns `true` when it is
     * safe to proceed; otherwise populates `payoutError`, clears the
     * `isRequestingPayout` flag and returns `false`.
     */
    private suspend fun refreshStatusForPayout(amountInCents: Long): Boolean {
        val result = stripeConnectRepository.getEarningsDashboard()
        result.onSuccess { dashboard ->
            // Refresh the in-memory cache so the UI reflects the same
            // numbers the gate decided on.
            _earningsUiState.value = _earningsUiState.value.copy(
                dashboard = dashboard,
                connectionState = EarningsConnectionState.from(dashboard.stripe),
            )

            val payoutsEnabled = dashboard.stripe?.payoutsEnabled ?: false
            if (!payoutsEnabled) {
                Timber.w("[Earnings] Payout blocked by fresh status: payoutsEnabled=false")
                HostPayoutTelemetry.freshStatusBlocked("payouts_disabled")
                _earningsUiState.value = _earningsUiState.value.copy(
                    isRequestingPayout = false,
                    payoutError = "Payouts are not currently enabled on your account. Please complete Stripe setup or contact support.",
                )
                return false
            }

            val available = dashboard.balances?.available
            if (available != null) {
                val availableCents = Math.round(available * 100)
                if (amountInCents > availableCents) {
                    Timber.w(
                        "[Earnings] Payout blocked by fresh status: requested=$amountInCents > available=$availableCents"
                    )
                    HostPayoutTelemetry.freshStatusBlocked("insufficient_balance")
                    _earningsUiState.value = _earningsUiState.value.copy(
                        isRequestingPayout = false,
                        payoutError = "Your available balance has changed. Please try again with the updated amount.",
                    )
                    return false
                }
            }

            HostPayoutTelemetry.freshStatusOk()
            return true
        }
        result.onFailure { exception ->
            Timber.e(exception, "[Earnings] Could not verify payout status; refusing to submit")
            HostPayoutTelemetry.freshStatusBlocked("status_fetch_failed")
            _earningsUiState.value = _earningsUiState.value.copy(
                isRequestingPayout = false,
                payoutError = "We couldn't verify your payout status. Please check your connection and try again.",
            )
        }
        return false
    }

    /**
     * Outcome of [acquireOrRotateIdempotencyKey] — the key to send and
     * whether it was reused from persisted storage (vs freshly minted).
     * Returned as a tuple so the caller can log accurate telemetry
     * without consulting `TokenStorage` again.
     */
    private data class AcquiredIdempotencyKey(val key: String, val reused: Boolean)

    /**
     * Returns the idempotency key to send on the next POST.  Reuses
     * the persisted key when it was minted for the same amount and is
     * still within the staleness window; otherwise rotates to a fresh
     * key and updates the persisted record.  The amount is part of the
     * pinning so a retry with a different amount cannot accidentally
     * dedupe against the original transfer.
     */
    private fun acquireOrRotateIdempotencyKey(amountInCents: Long): AcquiredIdempotencyKey {
        val existingKey = tokenStorage.pendingPayoutIdempotencyKey
        val existingAmount = tokenStorage.pendingPayoutAmountCents
        val existingTs = tokenStorage.pendingPayoutTimestampMs
        val now = System.currentTimeMillis()

        if (!existingKey.isNullOrBlank() && existingAmount != null && existingTs != null) {
            val age = now - existingTs
            when {
                age >= PENDING_PAYOUT_STALE_AFTER_MS ->
                    HostPayoutTelemetry.pendingKeyExpired(age)
                existingAmount != amountInCents ->
                    HostPayoutTelemetry.pendingKeyAmountMismatch(existingAmount, amountInCents)
                else ->
                    return AcquiredIdempotencyKey(existingKey, reused = true)
            }
        }

        val fresh = java.util.UUID.randomUUID().toString()
        tokenStorage.pendingPayoutIdempotencyKey = fresh
        tokenStorage.pendingPayoutAmountCents = amountInCents
        tokenStorage.pendingPayoutTimestampMs = now
        return AcquiredIdempotencyKey(fresh, reused = false)
    }

    fun clearPayoutError() {
        _earningsUiState.value = _earningsUiState.value.copy(payoutError = null)
    }

    // ── iOS parity: open Stripe onboarding / dashboard in-app ──

    /**
     * iOS parity: HostEarningsViewModel.openOnboarding().
     * Fetches an onboarding link and exposes the URL for the UI to open in Custom Tabs.
     */
    fun openOnboarding() {
        if (_isBusy.value) return
        viewModelScope.launch {
            _isBusy.value = true
            _inlineError.value = null
            stripeConnectRepository.createOnboardingLink()
                .onSuccess { link ->
                    val url = link.resolvedUrl
                    if (url != null) {
                        _presentingUrl.value = url
                    } else {
                        _inlineError.value = "Couldn't start Stripe setup. Please try again."
                    }
                }
                .onFailure { e ->
                    Timber.e(e, "[Earnings] Failed to create onboarding link")
                    _inlineError.value = com.pacedream.common.util.UserFacingErrorMapper.map(e, "Couldn't start Stripe setup. Please try again.")
                }
            _isBusy.value = false
        }
    }

    /**
     * iOS parity: HostEarningsViewModel.openDashboard().
     * Fetches a login link for the Stripe Express dashboard.
     */
    fun openDashboard() {
        if (_isBusy.value) return
        viewModelScope.launch {
            _isBusy.value = true
            _inlineError.value = null
            stripeConnectRepository.createLoginLink()
                .onSuccess { link ->
                    val url = link.resolvedUrl
                    if (url != null) {
                        _presentingUrl.value = url
                    } else {
                        _inlineError.value = "Couldn't open Stripe dashboard. Please try again."
                    }
                }
                .onFailure { e ->
                    Timber.e(e, "[Earnings] Failed to create login link")
                    _inlineError.value = com.pacedream.common.util.UserFacingErrorMapper.map(e, "Couldn't open Stripe dashboard. Please try again.")
                }
            _isBusy.value = false
        }
    }

    /** Clear the URL after launching Custom Tabs (prevents re-launch on recomposition). */
    fun clearPresentingUrl() {
        _presentingUrl.value = null
    }

    fun clearInlineError() {
        _inlineError.value = null
    }

    // Legacy methods for backward compatibility
    fun updateTimeRange(timeRange: String) {
        refreshData()
    }

    fun withdrawEarnings(amount: Double) {
        requestPayout(amount)
    }

    private companion object {
        /**
         * After this many millis the persisted payout idempotency key
         * is treated as stale and force-rotated regardless of amount.
         * Set to 24h to comfortably outlast any normal retry / app
         * relaunch window while preventing a year-old key from
         * accidentally deduping a brand-new request.
         */
        private const val PENDING_PAYOUT_STALE_AFTER_MS: Long = 24L * 60L * 60L * 1000L
    }
}
