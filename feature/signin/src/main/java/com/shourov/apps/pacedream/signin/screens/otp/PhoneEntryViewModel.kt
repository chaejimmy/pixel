package com.shourov.apps.pacedream.signin.screens.otp

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shourov.apps.pacedream.core.network.repository.OtpRepository
import com.shourov.apps.pacedream.model.response.otp.OtpError
import com.shourov.apps.pacedream.model.response.otp.getUserMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel for Phone Entry Screen
 * Handles phone number validation and OTP sending
 */
@HiltViewModel
class PhoneEntryViewModel @Inject constructor(
    private val otpRepository: OtpRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(PhoneEntryUiState())
    val uiState: StateFlow<PhoneEntryUiState> = _uiState.asStateFlow()
    
    /**
     * Validate phone number format.
     * Website parity: currently US/Canada only (+1 followed by 10 digits).
     */
    fun validatePhone(phone: String): Boolean {
        val usCanadaRegex = "^\\+1\\d{10}$".toRegex()
        return usCanadaRegex.matches(phone)
    }

    /**
     * Update phone number and validate
     */
    fun updatePhoneNumber(phone: String) {
        val isValid = validatePhone(phone)
        _uiState.value = _uiState.value.copy(
            phoneNumber = phone,
            isValidPhone = isValid,
            phoneError = if (!isValid && phone.isNotBlank()) {
                // Website parity: specific message for non-US/CA numbers
                if (phone.startsWith("+1") || phone.startsWith("1")) {
                    "Please enter a valid US or Canada phone number"
                } else {
                    "Phone verification is currently available in the United States and Canada only."
                }
            } else null
        )
    }
    
    /**
     * Send OTP to phone number
     */
    fun sendOTP(onSuccess: (String) -> Unit, onError: (String) -> Unit) {
        val phone = _uiState.value.phoneNumber
        if (!validatePhone(phone)) {
            onError("Invalid phone number format")
            return
        }
        
        _uiState.value = _uiState.value.copy(isLoading = true, phoneError = null)
        
        viewModelScope.launch {
            val result = otpRepository.sendOTP(phone)
            result.fold(
                onSuccess = { response ->
                    _uiState.value = _uiState.value.copy(isLoading = false)
                    onSuccess(phone)
                },
                onFailure = { error ->
                    val errorMessage = when (error) {
                        is OtpError -> error.getUserMessage()
                        else -> error.message ?: "Failed to send OTP"
                    }
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        phoneError = errorMessage
                    )
                    onError(errorMessage)
                }
            )
        }
    }
}

/**
 * UI State for Phone Entry Screen
 */
data class PhoneEntryUiState(
    val phoneNumber: String = "",
    val isValidPhone: Boolean = false,
    val phoneError: String? = null,
    val isLoading: Boolean = false
)
