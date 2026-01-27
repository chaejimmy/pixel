package com.pacedream.app.feature.verification

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shourov.apps.pacedream.core.network.model.verification.VerificationStatusResponse
import com.shourov.apps.pacedream.core.network.repository.VerificationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class IdentityVerificationViewModel @Inject constructor(
    private val repository: VerificationRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(IdentityVerificationUiState())
    val uiState: StateFlow<IdentityVerificationUiState> = _uiState.asStateFlow()
    
    fun loadStatus() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            repository.getVerificationStatus().fold(
                onSuccess = { response ->
                    response.data?.let { data ->
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            verificationLevel = data.level,
                            phoneNumber = data.phone?.phoneE164 ?: "",
                            isPhoneVerified = data.phone?.verified ?: false,
                            isIDVerified = data.id?.status == "approved",
                            idStatus = data.id
                        )
                    } ?: run {
                        _uiState.value = _uiState.value.copy(isLoading = false)
                    }
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = error.message
                    )
                }
            )
        }
    }
}

data class IdentityVerificationUiState(
    val verificationLevel: Int = 0,
    val phoneNumber: String = "",
    val isPhoneVerified: Boolean = false,
    val isIDVerified: Boolean = false,
    val idStatus: VerificationStatusResponse.VerificationStatusData.IDStatus? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)
