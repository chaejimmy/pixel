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

            val selectedFilter = uiState.value.selectedFilter
            val sort = uiState.value.selectedSort.lowercase()

            // iOS parity: always fetch all listings, then filter client-side.
            // The backend may not support filter params or may not account for
            // admin-approval overrides that happen during client-side parsing.
            hostRepository.getHostListings(null, sort)
                .onSuccess { allListings ->
                    val filtered = when (selectedFilter.lowercase()) {
                        "active" -> allListings.filter { it.isActiveStatus }
                        "pending" -> allListings.filter { it.isPendingReview }
                        "unavailable" -> allListings.filter { !it.isActiveStatus && !it.isPendingReview && !it.isRejected }
                        else -> allListings // "all"
                    }
                    _uiState.value = _uiState.value.copy(
                        listings = filtered,
                        isLoading = false,
                        error = null
                    )
                }
                .onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = com.pacedream.common.util.UserFacingErrorMapper.forLoadProperties(exception)
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
