package com.shourov.apps.pacedream.feature.host.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pacedream.app.core.auth.SessionManager
import com.shourov.apps.pacedream.feature.host.data.HostDashboardData
import com.shourov.apps.pacedream.feature.host.data.HostRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Host Dashboard ViewModel - iOS parity.
 *
 * Matches iOS HostDashboardViewModel: loads bookings, listings, payouts concurrently
 * with partial-success handling and inline error banners.
 */
@HiltViewModel
class HostDashboardViewModel @Inject constructor(
    private val hostRepository: HostRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(HostDashboardData())
    val uiState: StateFlow<HostDashboardData> = _uiState.asStateFlow()

    init {
        observeUser()
        loadDashboard()
    }

    private fun observeUser() {
        viewModelScope.launch {
            sessionManager.currentUser.collect { user ->
                if (user != null) {
                    _uiState.value = _uiState.value.copy(
                        userName = user.displayName
                    )
                }
            }
        }
    }

    private fun loadDashboard() {
        if (_uiState.value.isLoading) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            val result = hostRepository.loadDashboard()

            _uiState.value = _uiState.value.copy(
                bookings = result.bookings,
                listings = result.listings,
                activeListings = result.overview?.activeListings ?: result.listings.count { it.isAvailable },
                totalBookings = result.overview?.totalBookings ?: result.bookings.size,
                totalRevenue = result.overview?.totalRevenue ?: 0.0,
                averageRating = result.overview?.averageRating ?: 0.0,
                totalReviews = result.overview?.totalReviews ?: 0,
                occupancyRate = result.overview?.occupancyRate ?: 0.0,
                responseRate = result.overview?.responseRate ?: 0.0,
                pendingBookings = result.overview?.pendingBookings ?: 0,
                payoutState = result.payoutState,
                shouldShowPayoutSetupPrompt = result.payoutEligibility?.shouldShowPayoutSetupPrompt ?: false,
                payoutPromptReason = result.payoutEligibility?.payoutPromptReason,
                isLoading = false,
                hasLoaded = result.hasLoaded,
                error = result.errorMessage
            )
        }
    }

    fun refreshData() {
        loadDashboard()
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun signOut() {
        sessionManager.signOut()
    }
}
