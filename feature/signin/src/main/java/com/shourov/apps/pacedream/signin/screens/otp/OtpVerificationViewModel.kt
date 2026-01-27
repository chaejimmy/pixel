package com.shourov.apps.pacedream.signin.screens.otp

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shourov.apps.pacedream.core.network.auth.AuthSession
import com.shourov.apps.pacedream.core.network.auth.TokenStorage
import com.shourov.apps.pacedream.core.network.repository.OtpRepository
import com.shourov.apps.pacedream.model.response.otp.OtpError
import com.shourov.apps.pacedream.model.response.otp.OtpUserData
import com.shourov.apps.pacedream.model.response.otp.getUserMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel for OTP Verification Screen
 * Handles OTP verification and login
 */
@HiltViewModel
class OtpVerificationViewModel @Inject constructor(
    private val otpRepository: OtpRepository,
    private val tokenStorage: TokenStorage,
    private val authSession: AuthSession
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(OtpVerificationUiState())
    val uiState: StateFlow<OtpVerificationUiState> = _uiState.asStateFlow()
    
    /**
     * Update OTP code
     */
    fun updateOtpCode(code: String) {
        // Only allow numeric input, max 6 digits
        val numericCode = code.filter { it.isDigit() }.take(6)
        _uiState.value = _uiState.value.copy(
            otpCode = numericCode,
            otpError = null
        )
    }
    
    /**
     * Verify OTP and login
     */
    fun verifyAndLogin(
        phoneNumber: String,
        onSuccess: (OtpUserData) -> Unit,
        onError: (String) -> Unit
    ) {
        val code = _uiState.value.otpCode
        if (code.length != 6) {
            onError("Please enter a 6-digit code")
            return
        }
        
        _uiState.value = _uiState.value.copy(isLoading = true, otpError = null)
        
        viewModelScope.launch {
            // Step 1: Verify OTP
            val verifyResult = otpRepository.verifyOTP(phoneNumber, code)
            verifyResult.fold(
                onSuccess = { verifyResponse ->
                    // Step 2: Login
                    val loginResult = otpRepository.login(phoneNumber)
                    loginResult.fold(
                        onSuccess = { loginResponse ->
                            // Store tokens
                            val data = loginResponse.data
                            if (data != null) {
                                tokenStorage.storeTokens(
                                    data.accessToken,
                                    data.refreshToken
                                )
                                tokenStorage.userId = data.user.id
                                
                                // Store user data as JSON
                                val userJson = com.google.gson.Gson().toJson(data.user)
                                tokenStorage.cachedUserSummary = userJson
                                
                                // Refresh auth session to fetch full profile
                                authSession.refreshProfile()
                                
                                _uiState.value = _uiState.value.copy(isLoading = false)
                                onSuccess(data.user)
                            } else {
                                val errorMsg = "Login response missing user data"
                                _uiState.value = _uiState.value.copy(
                                    isLoading = false,
                                    otpError = errorMsg
                                )
                                onError(errorMsg)
                            }
                        },
                        onFailure = { error ->
                            val errorMessage = when (error) {
                                is OtpError -> error.getUserMessage()
                                else -> error.message ?: "Failed to login"
                            }
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                otpError = errorMessage
                            )
                            onError(errorMessage)
                        }
                    )
                },
                onFailure = { error ->
                    val errorMessage = when (error) {
                        is OtpError -> error.getUserMessage()
                        else -> error.message ?: "Failed to verify OTP"
                    }
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        otpError = errorMessage
                    )
                    onError(errorMessage)
                }
            )
        }
    }
    
    /**
     * Resend OTP
     */
    fun resendOTP(
        phoneNumber: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        _uiState.value = _uiState.value.copy(isLoading = true)
        
        viewModelScope.launch {
            val result = otpRepository.sendOTP(phoneNumber)
            result.fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(isLoading = false)
                    onSuccess()
                },
                onFailure = { error ->
                    val errorMessage = when (error) {
                        is OtpError -> error.getUserMessage()
                        else -> error.message ?: "Failed to resend OTP"
                    }
                    _uiState.value = _uiState.value.copy(isLoading = false)
                    onError(errorMessage)
                }
            )
        }
    }
}

/**
 * UI State for OTP Verification Screen
 */
data class OtpVerificationUiState(
    val otpCode: String = "",
    val otpError: String? = null,
    val isLoading: Boolean = false
)
