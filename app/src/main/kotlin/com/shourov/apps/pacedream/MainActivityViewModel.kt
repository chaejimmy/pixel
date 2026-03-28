package com.shourov.apps.pacedream

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shourov.apps.pacedream.core.network.auth.AuthSession
import com.shourov.apps.pacedream.core.network.auth.AuthState
import com.shourov.apps.pacedream.feature.host.domain.HostModeManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class MainActivityViewModel @Inject constructor(
    private val authSession: AuthSession,
    private val hostModeManager: HostModeManager
) : ViewModel() {

    private val _isAuthenticated = MutableStateFlow(false)
    val isAuthenticated: StateFlow<Boolean> = _isAuthenticated.asStateFlow()

    val authState: StateFlow<AuthState> = authSession.authState

    init {
        checkAuthenticationStatus()
        syncHostModeWithBackend()
    }

    private fun checkAuthenticationStatus() {
        viewModelScope.launch {
            authSession.authState.collectLatest { state ->
                Timber.d("MainActivityVM: authState changed → $state")
                _isAuthenticated.value = state == AuthState.Authenticated
            }
        }
    }

    /**
     * iOS parity: Observe user profile changes and sync host mode state.
     *
     * When the user profile loads (from /account/me), check if the backend
     * considers them a host and update HostModeManager accordingly.
     * This ensures that a returning host never sees "Start Hosting" incorrectly.
     */
    private fun syncHostModeWithBackend() {
        viewModelScope.launch {
            authSession.currentUser.collectLatest { user ->
                if (user != null) {
                    Timber.d("[HostMode] User profile loaded — isHost=${user.isHost}, superHost=${user.superHost}, properties=${user.propertiesCount}")
                    hostModeManager.syncWithBackendHostStatus(user.isHost)
                }
            }
        }
    }

    fun setAuthenticated(authenticated: Boolean) {
        _isAuthenticated.value = authenticated
    }

    fun logout() {
        viewModelScope.launch {
            authSession.signOut()
            hostModeManager.clearHostModeData()
            _isAuthenticated.value = false
        }
    }
}
