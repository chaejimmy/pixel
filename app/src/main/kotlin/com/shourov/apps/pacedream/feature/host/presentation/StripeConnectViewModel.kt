package com.shourov.apps.pacedream.feature.host.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shourov.apps.pacedream.feature.host.data.ConnectAccount
import com.shourov.apps.pacedream.feature.host.data.ConnectAccountStatus
import com.shourov.apps.pacedream.feature.host.data.ConnectBalance
import com.shourov.apps.pacedream.feature.host.data.StripeConnectRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

data class StripeConnectUiState(
    val connectAccount: ConnectAccount? = null,
    val balance: ConnectBalance? = null,
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class StripeConnectViewModel @Inject constructor(
    private val stripeConnectRepository: StripeConnectRepository,
    private val authSession: com.shourov.apps.pacedream.core.network.auth.AuthSession
) : ViewModel() {

    private val _uiState = MutableStateFlow(StripeConnectUiState())
    val uiState: StateFlow<StripeConnectUiState> = _uiState.asStateFlow()

    init {
        loadConnectAccountStatus()
    }

    /**
     * iOS parity: resolve connect state from /host/payouts/status (PayoutsService.fetchPayoutStatus).
     * The legacy /host/stripe/connect/status endpoint is not available on the backend.
     */
    private fun loadConnectAccountStatus() {
        viewModelScope.launch {
            stripeConnectRepository.getPayoutStatus()
                .onSuccess { status ->
                    val rawStatus = (status.status.ifBlank { status.payoutStatus ?: "" }).trim().lowercase()
                    val resolvedStatus = when {
                        (rawStatus == "active" || rawStatus.contains("connect")) && status.resolvedPayoutsEnabled ->
                            ConnectAccountStatus.ENABLED
                        rawStatus.contains("pending") || rawStatus.contains("action") ||
                            (status.resolvedDetailsSubmitted && !status.resolvedPayoutsEnabled) ->
                            ConnectAccountStatus.PENDING
                        status.resolvedChargesEnabled || status.resolvedDetailsSubmitted ->
                            ConnectAccountStatus.PENDING
                        else -> ConnectAccountStatus.NOT_CREATED
                    }
                    _uiState.value = _uiState.value.copy(
                        connectAccount = ConnectAccount(
                            status = resolvedStatus,
                            chargesEnabled = status.resolvedChargesEnabled,
                            payoutsEnabled = status.resolvedPayoutsEnabled,
                            detailsSubmitted = status.resolvedDetailsSubmitted
                        )
                    )
                }
                .onFailure { e ->
                    Timber.d("Payout status unavailable, treating as not connected: ${e.message}")
                    _uiState.value = _uiState.value.copy(connectAccount = null)
                }
        }
    }

    /**
     * iOS parity: for all non-connected states (NOT_CREATED, PENDING, UNDER_REVIEW),
     * call createOnboardingLink() directly. The backend creates the Stripe Connect
     * account as part of the onboarding link flow — no separate create step needed.
     * This matches iOS StripeConnectOnboardingView.startOnboarding().
     */
    fun startOnboarding(openUrl: (String) -> Unit) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            stripeConnectRepository.createOnboardingLink()
                .onSuccess { link ->
                    _uiState.value = _uiState.value.copy(isLoading = false)
                    val url = link.resolvedUrl
                    if (url != null) {
                        openUrl(url)
                        // iOS parity: refresh status after returning from Stripe (2-second delay)
                        delay(2000)
                        loadConnectAccountStatus()
                    }
                }
                .onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = exception.message ?: "Couldn't start Stripe setup. Please try again."
                    )
                }
        }
    }

    fun openDashboard(openUrl: (String) -> Unit) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            stripeConnectRepository.createLoginLink()
                .onSuccess { link ->
                    _uiState.value = _uiState.value.copy(isLoading = false)
                    val url = link.resolvedUrl
                    if (url != null) openUrl(url)
                }
                .onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = exception.message ?: "Couldn't open Stripe dashboard. Please try again."
                    )
                }
        }
    }

    fun refreshAccountStatus() {
        _uiState.value = _uiState.value.copy(isRefreshing = true)
        viewModelScope.launch {
            try {
                loadConnectAccountStatus()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = e.message ?: "Failed to refresh account status"
                )
            } finally {
                _uiState.value = _uiState.value.copy(isRefreshing = false)
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
}
