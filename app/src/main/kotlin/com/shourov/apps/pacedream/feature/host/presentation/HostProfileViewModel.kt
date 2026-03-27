package com.shourov.apps.pacedream.feature.host.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pacedream.app.core.auth.SessionManager
import com.pacedream.app.core.auth.AuthState
import com.shourov.apps.pacedream.feature.host.data.HostDashboardData
import com.shourov.apps.pacedream.feature.host.data.HostRepository
import com.shourov.apps.pacedream.feature.host.domain.HostModeManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * HostProfileViewModel - iOS parity with HostProfileView + HostDataStore.
 *
 * Provides: user identity (name, email, avatar), host stats (listings count,
 * monthly earnings), loading/refresh, logout, and switch-to-guest.
 */
@HiltViewModel
class HostProfileViewModel @Inject constructor(
    private val sessionManager: SessionManager,
    private val hostRepository: HostRepository,
    private val hostModeManager: HostModeManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(HostProfileUiState())
    val uiState: StateFlow<HostProfileUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            sessionManager.currentUser.collect { user ->
                _uiState.update {
                    it.copy(
                        userName = user?.displayName ?: "",
                        userEmail = user?.email,
                        userAvatar = user?.profileImage,
                        firstName = user?.firstName ?: "",
                        lastName = user?.lastName ?: ""
                    )
                }
            }
        }
        loadHostData()
    }

    private fun loadHostData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val result = try {
                hostRepository.loadDashboard()
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false) }
                return@launch
            }
            _uiState.update {
                it.copy(
                    activeListingsCount = result.overview?.activeListings
                        ?: result.listings.count { p -> p.isAvailable },
                    monthlyEarnings = HostDashboardData(
                        bookings = result.bookings,
                        listings = result.listings,
                        activeListings = result.overview?.activeListings ?: 0
                    ).monthlyEarnings,
                    isLoading = false
                )
            }
        }
    }

    fun refresh() {
        loadHostData()
    }

    fun switchToGuestMode() {
        hostModeManager.switchToGuest()
    }

    fun logout() {
        hostModeManager.signOutFromHostMode()
        sessionManager.signOut()
    }
}

data class HostProfileUiState(
    val userName: String = "",
    val userEmail: String? = null,
    val userAvatar: String? = null,
    val firstName: String = "",
    val lastName: String = "",
    val activeListingsCount: Int = 0,
    val monthlyEarnings: Double = 0.0,
    val isLoading: Boolean = false
) {
    val initials: String
        get() {
            val parts = userName.split(" ").filter { it.isNotBlank() }
            val a = parts.firstOrNull()?.firstOrNull()?.uppercase() ?: ""
            val b = parts.drop(1).firstOrNull()?.firstOrNull()?.uppercase() ?: ""
            val result = a + b
            return result.ifEmpty { "H" }
        }
}
