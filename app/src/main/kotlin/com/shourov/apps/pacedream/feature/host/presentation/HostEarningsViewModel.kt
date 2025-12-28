package com.shourov.apps.pacedream.feature.host.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shourov.apps.pacedream.feature.host.data.HostEarningsData
import com.shourov.apps.pacedream.feature.host.data.HostRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HostEarningsViewModel @Inject constructor(
    private val hostRepository: HostRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(HostEarningsData())
    val uiState: StateFlow<HostEarningsData> = _uiState.asStateFlow()
    
    init {
        loadHostEarnings()
    }
    
    private fun loadHostEarnings() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            hostRepository.getHostEarnings(uiState.value.selectedTimeRange)
                .onSuccess { earningsData ->
                    _uiState.value = earningsData
                }
                .onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = exception.message ?: "Unknown error occurred"
                    )
                }
        }
    }
    
    fun updateTimeRange(timeRange: String) {
        _uiState.value = _uiState.value.copy(selectedTimeRange = timeRange)
        loadHostEarnings()
    }
    
    fun withdrawEarnings(amount: Double) {
        viewModelScope.launch {
            try {
                // TODO: Call repository to withdraw earnings
                refreshData()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = e.message ?: "Failed to withdraw earnings"
                )
            }
        }
    }
    
    fun requestWithdrawal(amount: Double, paymentMethod: String) {
        viewModelScope.launch {
            hostRepository.requestWithdrawal(amount, paymentMethod)
                .onSuccess {
                    refreshData()
                }
                .onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        error = exception.message ?: "Failed to request withdrawal"
                    )
                }
        }
    }
    
    fun refreshData() {
        loadHostEarnings()
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
