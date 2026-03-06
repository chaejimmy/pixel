package com.shourov.apps.pacedream.feature.host.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shourov.apps.pacedream.feature.host.data.HostEarningsData
import com.shourov.apps.pacedream.feature.host.data.HostEarningsUiState
import com.shourov.apps.pacedream.feature.host.data.HostRepository
import com.shourov.apps.pacedream.feature.host.data.StripeConnectRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Host Earnings ViewModel - iOS parity.
 *
 * Matches iOS HostEarningsView with Stripe Connect onboarding,
 * payout status, payout methods, and revenue data.
 */
@HiltViewModel
class HostEarningsViewModel @Inject constructor(
    private val hostRepository: HostRepository,
    private val stripeConnectRepository: StripeConnectRepository
) : ViewModel() {

    // Legacy state for backward compatibility
    private val _uiState = MutableStateFlow(HostEarningsData())
    val uiState: StateFlow<HostEarningsData> = _uiState.asStateFlow()

    // New tabbed earnings state (matching iOS EarningsView)
    private val _earningsUiState = MutableStateFlow(HostEarningsUiState())
    val earningsUiState: StateFlow<HostEarningsUiState> = _earningsUiState.asStateFlow()

    init {
        loadAllData()
    }

    private fun loadAllData() {
        viewModelScope.launch {
            _earningsUiState.value = _earningsUiState.value.copy(isLoading = true)

            // Load balance, transfers, payouts concurrently (like iOS refreshData)
            val balanceDeferred = async { stripeConnectRepository.getBalance() }
            val transfersDeferred = async { stripeConnectRepository.getTransfers() }
            val payoutsDeferred = async { stripeConnectRepository.getPayouts() }
            val accountDeferred = async { stripeConnectRepository.getConnectAccountStatus() }

            val balanceResult = balanceDeferred.await()
            val transfersResult = transfersDeferred.await()
            val payoutsResult = payoutsDeferred.await()
            val accountResult = accountDeferred.await()

            _earningsUiState.value = _earningsUiState.value.copy(
                balance = balanceResult.getOrNull(),
                transfers = transfersResult.getOrDefault(emptyList()),
                payouts = payoutsResult.getOrDefault(emptyList()),
                connectAccount = accountResult.getOrNull(),
                isLoading = false,
                isRefreshing = false,
                errorMessage = balanceResult.exceptionOrNull()?.message
                    ?: transfersResult.exceptionOrNull()?.message
                    ?: payoutsResult.exceptionOrNull()?.message
            )
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
        _uiState.value = _uiState.value.copy(selectedTimeRange = timeRange)
    }

    fun withdrawEarnings(amount: Double) {
        requestPayout(amount)
    }

    fun requestWithdrawal(amount: Double, paymentMethod: String) {
        viewModelScope.launch {
            hostRepository.requestWithdrawal(amount, paymentMethod)
                .onSuccess { refreshData() }
                .onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        error = exception.message ?: "Failed to process withdrawal",
                        isBusy = false
                    )
                }
        }
    }
}
