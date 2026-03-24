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
 * Matches iOS HostEarningsView with Stripe Connect status,
 * balance cards, recent payouts, and booking earnings.
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
            _earningsUiState.value = _earningsUiState.value.copy(isLoading = true)

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

    fun refreshData() {
        _earningsUiState.value = _earningsUiState.value.copy(isRefreshing = true)
        loadAllData()
    }

    fun clearError() {
        _earningsUiState.value = _earningsUiState.value.copy(errorMessage = null)
    }

    // Legacy methods for backward compatibility
    fun updateTimeRange(timeRange: String) {
        refreshData()
    }

    fun withdrawEarnings(amount: Double) {
        // Payout requests are handled via Stripe dashboard
        refreshData()
    }
}
