package com.pacedream.app.feature.verification

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shourov.apps.pacedream.core.network.repository.VerificationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PhoneVerificationViewModel @Inject constructor(
    private val repository: VerificationRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(PhoneVerificationUiState())
    val uiState: StateFlow<PhoneVerificationUiState> = _uiState.asStateFlow()
    
    private var cooldownTimer: Job? = null
    
    fun updatePhoneNumber(phone: String) {
        _uiState.value = _uiState.value.copy(
            phoneNumber = phone,
            canSendCode = isValidPhoneNumber(phone)
        )
    }
    
    fun updateOtpCode(code: String) {
        _uiState.value = _uiState.value.copy(otpCode = code)
    }
    
    fun sendCode() {
        val phone = _uiState.value.phoneNumber
        if (!isValidPhoneNumber(phone)) return
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            repository.sendPhoneVerificationCode(phone).fold(
                onSuccess = { response ->
                    if (response.success) {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            cooldown = 60
                        )
                        startCooldownTimer()
                    } else {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = response.message ?: "Failed to send code"
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = error.message ?: "Network error"
                    )
                }
            )
        }
    }
    
    fun verifyCode() {
        val phone = _uiState.value.phoneNumber
        val code = _uiState.value.otpCode
        if (code.length != 6) return
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            repository.confirmPhoneVerification(phone, code).fold(
                onSuccess = { response ->
                    if (response.success) {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            isVerified = true
                        )
                    } else {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = response.message ?: "Invalid verification code"
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = error.message ?: "Network error"
                    )
                }
            )
        }
    }
    
    private fun isValidPhoneNumber(phone: String): Boolean {
        return phone.startsWith("+") && phone.length >= 10
    }
    
    private fun startCooldownTimer() {
        cooldownTimer?.cancel()
        cooldownTimer = viewModelScope.launch {
            while (_uiState.value.cooldown > 0) {
                delay(1000)
                _uiState.value = _uiState.value.copy(cooldown = _uiState.value.cooldown - 1)
            }
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        cooldownTimer?.cancel()
    }
}

data class PhoneVerificationUiState(
    val phoneNumber: String = "",
    val otpCode: String = "",
    val isLoading: Boolean = false,
    val cooldown: Int = 0,
    val canSendCode: Boolean = false,
    val isVerified: Boolean = false,
    val error: String? = null
)
