package com.shourov.apps.pacedream.feature.host.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shourov.apps.pacedream.feature.host.data.HostListingsData
import com.shourov.apps.pacedream.feature.host.data.HostRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HostListingsViewModel @Inject constructor(
    private val hostRepository: HostRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(HostListingsData())
    val uiState: StateFlow<HostListingsData> = _uiState.asStateFlow()
    
    init {
        loadHostListings()
    }
    
    private fun loadHostListings() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            hostRepository.getHostListings(uiState.value.selectedFilter, uiState.value.selectedSort)
                .onSuccess { listings ->
                    _uiState.value = _uiState.value.copy(
                        listings = listings,
                        isLoading = false,
                        error = null
                    )
                }
                .onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = exception.message ?: "Unknown error occurred"
                    )
                }
        }
    }
    
    fun updateFilter(filter: String) {
        _uiState.value = _uiState.value.copy(selectedFilter = filter)
        loadHostListings()
    }
    
    fun updateSort(sort: String) {
        _uiState.value = _uiState.value.copy(selectedSort = sort)
        loadHostListings()
    }
    
    fun refreshData() {
        loadHostListings()
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
