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
                    Timber.e(exception, "[Earnings] Dashboard load failed")

                    val rawMessage = exception.message ?: ""
                    val isAuthError = rawMessage.contains("token", ignoreCase = true) ||
                        rawMessage.contains("auth", ignoreCase = true) ||
                        rawMessage.contains("unauthorized", ignoreCase = true) ||
                        rawMessage.contains("401")

                    val screenState = if (isAuthError) {
                        EarningsScreenState.SessionExpired
                    } else {
                        val userMessage = when {
                            rawMessage.contains("network", ignoreCase = true) ||
                            rawMessage.contains("connect", ignoreCase = true) ||
                            rawMessage.contains("timeout", ignoreCase = true) ->
                                "Network error. Check your connection and try again."
                            else ->
                                "Couldn't load earnings data. Pull to refresh."
                        }
                        EarningsScreenState.Error(userMessage)
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
        viewModelScope.launch {
            _earningsUiState.value = _earningsUiState.value.copy(payoutError = null)

            val amountInCents = (amount * 100).toInt()
            stripeConnectRepository.createPayout(amountInCents)
                .onSuccess {
                    _earningsUiState.value = _earningsUiState.value.copy(
                        showPayoutSheet = false,
                        payoutAmount = ""
                    )
                    refreshData()
                }
                .onFailure { exception ->
                    _earningsUiState.value = _earningsUiState.value.copy(
                        payoutError = exception.message ?: "Failed to request payout"
                    )
                }
        }
    }

    fun clearPayoutError() {
        _earningsUiState.value = _earningsUiState.value.copy(payoutError = null)
    }

    // Legacy methods for backward compatibility
    fun updateTimeRange(timeRange: String) {
        refreshData()
    }

    fun withdrawEarnings(amount: Double) {
        requestPayout(amount)
    }
}
