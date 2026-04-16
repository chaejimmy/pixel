package com.shourov.apps.pacedream.feature.host.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shourov.apps.pacedream.feature.host.data.EarningsConnectionState
import com.shourov.apps.pacedream.feature.host.data.EarningsScreenState
import com.shourov.apps.pacedream.feature.host.data.HostEarningsData
import com.shourov.apps.pacedream.feature.host.data.HostEarningsUiState
import com.shourov.apps.pacedream.feature.host.data.HostRepository
import com.shourov.apps.pacedream.feature.host.data.StripeConnectRepository
import com.shourov.apps.pacedream.core.network.auth.AuthSession
import com.shourov.apps.pacedream.core.network.auth.AuthState
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
    private val authSession: AuthSession
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
        _earningsUiState.value = _earningsUiState.value.copy(showPayoutSheet = false, payoutAmount = "")
    }

    fun requestPayout(amount: Double) {
        if (_earningsUiState.value.isRequestingPayout) return

        // Guard: block payout requests when Stripe payouts are not enabled.
        // This prevents confusing API errors when the host's account is restricted.
        val dashboard = _earningsUiState.value.dashboard
        val payoutsEnabled = dashboard?.stripe?.payoutsEnabled ?: false
        if (!payoutsEnabled) {
            Timber.w("[Earnings] Payout request blocked: payoutsEnabled=false")
            _earningsUiState.value = _earningsUiState.value.copy(
                payoutError = "Payouts are not yet enabled on your account. Please complete Stripe setup first."
            )
            return
        }

        viewModelScope.launch {
            _earningsUiState.value = _earningsUiState.value.copy(
                isRequestingPayout = true, payoutError = null
            )

            val amountInCents = Math.round(amount * 100).toInt()
            stripeConnectRepository.createPayout(amountInCents)
                .onSuccess {
                    _earningsUiState.value = _earningsUiState.value.copy(
                        isRequestingPayout = false,
                        showPayoutSheet = false,
                        payoutAmount = ""
                    )
                    refreshData()
                }
                .onFailure { exception ->
                    Timber.e(exception, "[Earnings] Payout request failed")
                    _earningsUiState.value = _earningsUiState.value.copy(
                        isRequestingPayout = false,
                        payoutError = com.pacedream.common.util.UserFacingErrorMapper.map(exception, "We couldn't process your payout. Please try again.")
                    )
                }
        }
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
}
