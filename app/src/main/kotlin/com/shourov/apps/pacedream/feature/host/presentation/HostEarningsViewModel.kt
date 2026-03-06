package com.shourov.apps.pacedream.feature.host.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shourov.apps.pacedream.feature.host.data.HostEarningsData
import com.shourov.apps.pacedream.feature.host.data.HostRepository
import com.shourov.apps.pacedream.feature.host.data.PayoutConnectionState
import com.shourov.apps.pacedream.feature.host.data.PayoutMethod
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
    private val hostRepository: HostRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HostEarningsData())
    val uiState: StateFlow<HostEarningsData> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            val payoutStateDeferred = async {
                try { hostRepository.resolvePayoutState() }
                catch (_: Exception) { PayoutConnectionState.NOT_CONNECTED }
            }

            val payoutMethodsDeferred = async {
                try { hostRepository.getPayoutMethods().getOrElse { emptyList() } }
                catch (_: Exception) { emptyList<PayoutMethod>() }
            }

            val revenueDeferred = async {
                try { hostRepository.getRevenue().getOrNull() }
                catch (_: Exception) { null }
            }

            val payoutStatusDeferred = async {
                try { hostRepository.getPayoutStatus().getOrNull() }
                catch (_: Exception) { null }
            }

            val payoutState = payoutStateDeferred.await()
            val payoutMethods = payoutMethodsDeferred.await()
            val revenue = revenueDeferred.await()
            val payoutStatus = payoutStatusDeferred.await()

            _uiState.value = _uiState.value.copy(
                connectionState = payoutState,
                payoutMethods = payoutMethods,
                requirementsCurrentlyDue = payoutStatus?.resolvedCurrentlyDue ?: emptyList(),
                totalRevenue = revenue?.totalRevenue ?: 0.0,
                grossRevenue = revenue?.grossRevenue ?: 0.0,
                platformFees = revenue?.platformFees ?: 0.0,
                netRevenue = revenue?.netRevenue ?: 0.0,
                revenueByMonth = revenue?.revenueByMonth ?: emptyList(),
                revenueByListing = revenue?.revenueByListing ?: emptyList(),
                isLoading = false
            )
        }
    }

    fun performPrimaryAction() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isBusy = true)
            when (_uiState.value.connectionState) {
                PayoutConnectionState.CONNECTED -> {
                    hostRepository.createLoginLink()
                        .onSuccess { url ->
                            _uiState.value = _uiState.value.copy(loginUrl = url, isBusy = false)
                        }
                        .onFailure { e ->
                            _uiState.value = _uiState.value.copy(
                                error = e.message ?: "Failed to open Stripe dashboard",
                                isBusy = false
                            )
                        }
                }
                PayoutConnectionState.PENDING, PayoutConnectionState.NOT_CONNECTED -> {
                    hostRepository.createOnboardingLink()
                        .onSuccess { url ->
                            _uiState.value = _uiState.value.copy(onboardingUrl = url, isBusy = false)
                        }
                        .onFailure { e ->
                            _uiState.value = _uiState.value.copy(
                                error = e.message ?: "Failed to create onboarding link",
                                isBusy = false
                            )
                        }
                }
            }
        }
    }

    fun openDashboard() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isBusy = true)
            hostRepository.createLoginLink()
                .onSuccess { url ->
                    _uiState.value = _uiState.value.copy(loginUrl = url, isBusy = false)
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(
                        error = e.message ?: "Failed to open Stripe dashboard",
                        isBusy = false
                    )
                }
        }
    }

    fun clearUrls() {
        _uiState.value = _uiState.value.copy(onboardingUrl = null, loginUrl = null)
        load()
    }

    fun refreshData() {
        load()
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
