package com.shourov.apps.pacedream.feature.host.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shourov.apps.pacedream.feature.host.data.EarningsConnectionState
import com.shourov.apps.pacedream.feature.host.data.HostEarningsData
import com.shourov.apps.pacedream.feature.host.data.HostEarningsUiState
import com.shourov.apps.pacedream.feature.host.data.HostRepository
import com.shourov.apps.pacedream.feature.host.data.StripeConnectRepository
import com.shourov.apps.pacedream.feature.host.data.EarningsDashboardResponse
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
 * iOS source of truth: HostEarningsViewModel.swift + PayoutsService.swift
 */
@HiltViewModel
class HostEarningsViewModel @Inject constructor(
    private val hostRepository: HostRepository,
    private val stripeConnectRepository: StripeConnectRepository
) : ViewModel() {

    // Legacy state for backward compatibility
    private val _uiState = MutableStateFlow(HostEarningsData())
    val uiState: StateFlow<HostEarningsData> = _uiState.asStateFlow()

    private val _earningsUiState = MutableStateFlow(HostEarningsUiState())
    val earningsUiState: StateFlow<HostEarningsUiState> = _earningsUiState.asStateFlow()

    init {
        loadAllData()
    }

    private fun loadAllData() {
        viewModelScope.launch {
            // Guard against duplicate loads (iOS parity: if isLoading { return })
            if (_earningsUiState.value.isLoading && !_earningsUiState.value.isRefreshing) {
                Timber.d("[Earnings] Already loading, skipping")
                return@launch
            }

            _earningsUiState.value = _earningsUiState.value.copy(
                isLoading = true,
                errorMessage = null
            )

            Timber.d("[Earnings] Loading dashboard data...")

            // Use the single all-in-one dashboard endpoint (iOS parity)
            stripeConnectRepository.getEarningsDashboard()
                .onSuccess { dashboard ->
                    val connectionState = EarningsConnectionState.from(dashboard.stripe)
                    Timber.d(
                        "[Earnings] Dashboard loaded successfully. " +
                            "connectionState=$connectionState, " +
                            "available=${dashboard.balances?.available}, " +
                            "payouts=${dashboard.payouts.size}, " +
                            "transactions=${dashboard.transactions.size}"
                    )

                    _earningsUiState.value = _earningsUiState.value.copy(
                        dashboard = dashboard,
                        connectionState = connectionState,
                        isLoading = false,
                        isRefreshing = false,
                        errorMessage = null,
                        hasLoaded = true
                    )
                }
                .onFailure { exception ->
                    Timber.e(exception, "[Earnings] Dashboard load failed")

                    // iOS parity: abstract raw backend errors into user-friendly messages.
                    // Don't expose "Access token required" or other implementation details.
                    val rawMessage = exception.message ?: ""
                    val userMessage = when {
                        rawMessage.contains("token", ignoreCase = true) ||
                        rawMessage.contains("auth", ignoreCase = true) ||
                        rawMessage.contains("unauthorized", ignoreCase = true) ||
                        rawMessage.contains("401") ->
                            "Please sign in again to view your earnings."
                        rawMessage.contains("network", ignoreCase = true) ||
                        rawMessage.contains("connect", ignoreCase = true) ||
                        rawMessage.contains("timeout", ignoreCase = true) ->
                            "Network error. Check your connection and try again."
                        else ->
                            "Couldn't load earnings data. Pull to refresh."
                    }

                    _earningsUiState.value = _earningsUiState.value.copy(
                        isLoading = false,
                        isRefreshing = false,
                        errorMessage = userMessage,
                        hasLoaded = true
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
            _earningsUiState.value = _earningsUiState.value.copy(isLoading = true)

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
