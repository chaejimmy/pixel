package com.shourov.apps.pacedream.feature.host.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pacedream.app.core.auth.SessionManager
import com.shourov.apps.pacedream.feature.host.data.HostRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * Host Profile ViewModel — iOS parity.
 *
 * Provides host profile data matching iOS HostProfileView:
 * - User identity (avatar, name, email) from SessionManager
 * - Host stats (active listings, monthly earnings) from HostRepository
 * - Sign out via SessionManager
 */
data class HostProfileUiState(
    val avatarUrl: String? = null,
    val fullName: String = "Host",
    val email: String = "",
    val activeListingsCount: Int = 0,
    val monthlyEarnings: Double = 0.0,
    val isLoading: Boolean = false,
    val error: String? = null
) {
    val initials: String get() {
        val parts = fullName.split(" ").filter { it.isNotBlank() }
        val a = parts.firstOrNull()?.firstOrNull()?.uppercase() ?: ""
        val b = parts.getOrNull(1)?.firstOrNull()?.uppercase() ?: ""
        val result = "$a$b"
        return result.ifEmpty { "H" }
    }
}

@HiltViewModel
class HostProfileViewModel @Inject constructor(
    private val hostRepository: HostRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(HostProfileUiState())
    val uiState: StateFlow<HostProfileUiState> = _uiState.asStateFlow()

    init {
        observeUser()
        loadHostStats()
    }

    private fun observeUser() {
        viewModelScope.launch {
            sessionManager.currentUser.collect { user ->
                if (user != null) {
                    _uiState.value = _uiState.value.copy(
                        fullName = user.displayName,
                        email = user.email ?: "",
                        avatarUrl = user.profileImage
                    )
                }
            }
        }
    }

    private fun loadHostStats() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val result = hostRepository.loadDashboard()
                _uiState.value = _uiState.value.copy(
                    activeListingsCount = result.overview?.activeListings
                        ?: result.listings.count { it.isAvailable },
                    monthlyEarnings = result.overview?.totalRevenue ?: 0.0,
                    isLoading = false,
                    error = result.errorMessage
                )
            } catch (e: Exception) {
                Timber.e(e, "[HostProfile] Failed to load host stats")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Couldn't load profile data."
                )
            }
        }
    }

    fun refreshData() {
        loadHostStats()
    }

    fun signOut(onComplete: () -> Unit) {
        sessionManager.signOut()
        onComplete()
    }
}
