package com.shourov.apps.pacedream.feature.host.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shourov.apps.pacedream.feature.host.data.HostDashboardData
import com.shourov.apps.pacedream.feature.host.data.HostRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HostDashboardViewModel @Inject constructor(
    private val hostRepository: HostRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(HostDashboardData())
    val uiState: StateFlow<HostDashboardData> = _uiState.asStateFlow()
    
    init {
        loadHostDashboardData()
    }
    
    private fun loadHostDashboardData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            hostRepository.getHostDashboard()
                .onSuccess { dashboardData ->
                    _uiState.value = dashboardData
                }
                .onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = exception.message ?: "Unknown error occurred"
                    )
                }
        }
    }
    
    fun refreshData() {
        loadHostDashboardData()
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
