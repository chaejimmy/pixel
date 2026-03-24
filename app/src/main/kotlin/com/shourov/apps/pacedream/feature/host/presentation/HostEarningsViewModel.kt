package com.shourov.apps.pacedream.feature.host.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shourov.apps.pacedream.feature.host.data.HostEarningsData
import com.shourov.apps.pacedream.feature.host.data.HostEarningsUiState
import com.shourov.apps.pacedream.feature.host.data.HostRepository
import com.shourov.apps.pacedream.feature.host.data.StripeConnectRepository
import com.shourov.apps.pacedream.feature.host.data.EarningsDashboardResponse
import com.shourov.apps.pacedream.feature.host.data.ConnectBalance
import com.shourov.apps.pacedream.feature.host.data.BalanceAmount
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * Host Earnings ViewModel - iOS parity.
 *
 * Primary: tries /host/earnings/dashboard (iOS PayoutsService.fetchDashboard)
 * for comprehensive earnings data including transactions, stats, and payout rules.
 *
 * Fallback: if the dashboard endpoint fails, loads data from individual
 * Stripe Connect endpoints (/host/stripe/balance, transfers, payouts, connect/status).
 */
@HiltViewModel
class HostEarningsViewModel @Inject constructor(
    private val hostRepository: HostRepository,
    private val stripeConnectRepository: StripeConnectRepository
) : ViewModel() {

    // Legacy state for backward compatibility
    private val _uiState = MutableStateFlow(HostEarningsData())
    val uiState: StateFlow<HostEarningsData> = _uiState.asStateFlow()

    // Tabbed earnings state (matching iOS EarningsView)
    private val _earningsUiState = MutableStateFlow(HostEarningsUiState())
    val earningsUiState: StateFlow<HostEarningsUiState> = _earningsUiState.asStateFlow()

    // Comprehensive dashboard data (when available)
    private val _dashboardData = MutableStateFlow<EarningsDashboardResponse?>(null)
    val dashboardData: StateFlow<EarningsDashboardResponse?> = _dashboardData.asStateFlow()

    init {
        loadAllData()
    }

    private fun loadAllData() {
        viewModelScope.launch {
            _earningsUiState.value = _earningsUiState.value.copy(isLoading = true, errorMessage = null)

            // Try the comprehensive dashboard endpoint first (iOS parity)
            val dashboardResult = stripeConnectRepository.getEarningsDashboard()

            if (dashboardResult.isSuccess) {
                val dashboard = dashboardResult.getOrNull()
                _dashboardData.value = dashboard
                Timber.d("Earnings dashboard loaded successfully, falling through to individual endpoints for tabbed UI")
            } else {
                Timber.d("Earnings dashboard not available, using individual Stripe endpoints")
            }

            // Always load individual endpoints for the tabbed UI
            // (the dashboard provides aggregate data, but we still need
            // the Stripe-specific formats for balance/transfers/payouts tabs)
            loadIndividualEndpoints()
        }
    }

    private suspend fun loadIndividualEndpoints() {
        val balanceDeferred = viewModelScope.async { stripeConnectRepository.getBalance() }
        val transfersDeferred = viewModelScope.async { stripeConnectRepository.getTransfers() }
        val payoutsDeferred = viewModelScope.async { stripeConnectRepository.getPayouts() }
        val accountDeferred = viewModelScope.async { stripeConnectRepository.getConnectAccountStatus() }

        val balanceResult = balanceDeferred.await()
        val transfersResult = transfersDeferred.await()
        val payoutsResult = payoutsDeferred.await()
        val accountResult = accountDeferred.await()

        // If individual balance fails but we have dashboard data, construct balance from dashboard
        val balance = balanceResult.getOrNull() ?: constructBalanceFromDashboard()

        val errorMessage = balanceResult.exceptionOrNull()?.message
            ?: transfersResult.exceptionOrNull()?.message
            ?: payoutsResult.exceptionOrNull()?.message

        if (errorMessage != null) {
            Timber.w("Earnings tab partial failure: $errorMessage")
        }

        _earningsUiState.value = _earningsUiState.value.copy(
            balance = balance,
            transfers = transfersResult.getOrDefault(emptyList()),
            payouts = payoutsResult.getOrDefault(emptyList()),
            connectAccount = accountResult.getOrNull(),
            isLoading = false,
            isRefreshing = false,
            errorMessage = errorMessage
        )
    }

    /**
     * If individual /host/stripe/balance fails but the comprehensive dashboard
     * endpoint succeeded, construct a ConnectBalance from the dashboard balances.
     */
    private fun constructBalanceFromDashboard(): ConnectBalance? {
        val dashboard = _dashboardData.value ?: return null
        val balances = dashboard.balances ?: return null

        return ConnectBalance(
            available = listOf(BalanceAmount(amount = (balances.available * 100).toInt(), currency = balances.currency)),
            pending = listOf(BalanceAmount(amount = (balances.pending * 100).toInt(), currency = balances.currency))
        )
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
            _earningsUiState.value = _earningsUiState.value.copy(isLoading = true)

            val amountInCents = (amount * 100).toInt()
            stripeConnectRepository.createPayout(amountInCents)
                .onSuccess {
                    Timber.d("Payout request succeeded for $amountInCents cents")
                    _earningsUiState.value = _earningsUiState.value.copy(
                        showPayoutSheet = false,
                        payoutAmount = ""
                    )
                    refreshData()
                }
                .onFailure { exception ->
                    Timber.e(exception, "Payout request failed")
                    _earningsUiState.value = _earningsUiState.value.copy(
                        isLoading = false,
                        errorMessage = exception.message ?: "Failed to request payout"
                    )
                }
        }
    }

    fun clearError() {
        _earningsUiState.value = _earningsUiState.value.copy(errorMessage = null)
    }

    // Legacy methods for backward compatibility
    fun updateTimeRange(timeRange: String) {
        refreshData()
    }

    fun withdrawEarnings(amount: Double) {
        requestPayout(amount)
    }
}
