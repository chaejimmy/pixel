package com.shourov.apps.pacedream.feature.host.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shourov.apps.pacedream.feature.host.data.ConnectAccount
import com.shourov.apps.pacedream.feature.host.data.ConnectBalance
import com.shourov.apps.pacedream.feature.host.data.StripeConnectRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
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

    private fun loadConnectAccountStatus() {
        viewModelScope.launch {
            stripeConnectRepository.getConnectAccountStatus()
                .onSuccess { account ->
                    _uiState.value = _uiState.value.copy(connectAccount = account)
                }
                .onFailure {
                    // Account might not exist yet
                    _uiState.value = _uiState.value.copy(connectAccount = null)
                }

            // Also load balance
            stripeConnectRepository.getBalance()
                .onSuccess { balance ->
                    _uiState.value = _uiState.value.copy(balance = balance)
                }
        }
    }

    fun createConnectAccount() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            val email = authSession.currentUser.value?.email ?: run {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Please add an email to your profile first"
                )
                return@launch
            }
            stripeConnectRepository.createConnectAccount(email)
                .onSuccess { account ->
                    _uiState.value = _uiState.value.copy(
                        connectAccount = account,
                        isLoading = false
                    )
                }
                .onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = exception.message ?: "Failed to create account"
                    )
                }
        }
    }

    fun startOnboarding(openUrl: (String) -> Unit) {
        viewModelScope.launch {
            stripeConnectRepository.createOnboardingLink()
                .onSuccess { link ->
                    val url = link.resolvedUrl
                    if (url != null) openUrl(url)
                }
                .onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        errorMessage = exception.message ?: "Failed to create onboarding link"
                    )
                }
        }
    }

    fun openDashboard(openUrl: (String) -> Unit) {
        viewModelScope.launch {
            stripeConnectRepository.createLoginLink()
                .onSuccess { link ->
                    val url = link.resolvedUrl
                    if (url != null) openUrl(url)
                }
                .onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        errorMessage = exception.message ?: "Failed to open dashboard"
                    )
                }
        }
    }

    fun refreshAccountStatus() {
        _uiState.value = _uiState.value.copy(isRefreshing = true)
        viewModelScope.launch {
            loadConnectAccountStatus()
            _uiState.value = _uiState.value.copy(isRefreshing = false)
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
}
