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
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.pacedream.common.util.UserFacingErrorMapper
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel for OTP Verification Screen.
 * Handles OTP verification, login, and ViewModel-managed resend cooldown.
 */
@HiltViewModel
class OtpVerificationViewModel @Inject constructor(
    private val otpRepository: OtpRepository,
    private val tokenStorage: TokenStorage,
    private val authSession: AuthSession
) : ViewModel() {

    private val _uiState = MutableStateFlow(OtpVerificationUiState())
    val uiState: StateFlow<OtpVerificationUiState> = _uiState.asStateFlow()

    private var cooldownJob: Job? = null

    init {
        // Start initial cooldown when the screen first appears
        // (OTP was just sent by the phone entry screen)
        startResendCooldown(DEFAULT_RESEND_COOLDOWN)
    }

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
     * Verify OTP and login.
     * Prevents rapid repeated taps — ignored while a request is already in flight.
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

        // Prevent rapid repeated taps
        if (_uiState.value.isLoading) return

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
                                try {
                                    tokenStorage.storeTokens(
                                        data.accessToken,
                                        data.refreshToken
                                    )
                                    tokenStorage.userId = data.user?.id

                                    // Store user data as JSON
                                    val userJson = data.user?.let { com.google.gson.Gson().toJson(it) }
                                    tokenStorage.cachedUserSummary = userJson
                                } catch (e: Exception) {
                                    Timber.e(e, "Failed to store auth tokens")
                                }

                                // Refresh auth session to fetch full profile
                                try {
                                    authSession.refreshProfile()
                                } catch (e: Exception) {
                                    Timber.e(e, "Failed to refresh profile after login")
                                }

                                _uiState.value = _uiState.value.copy(isLoading = false)
                                onSuccess(data.user)
                            } else {
                                val errorMsg = "We couldn't complete the login. Please try again."
                                _uiState.value = _uiState.value.copy(
                                    isLoading = false,
                                    otpError = errorMsg
                                )
                                onError(errorMsg)
                            }
                        },
                        onFailure = { error ->
                            Timber.e(error, "OTP login failed")
                            val errorMessage = when (error) {
                                is OtpError -> error.getUserMessage()
                                else -> UserFacingErrorMapper.forLogin(error)
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
                    Timber.e(error, "OTP verification failed")
                    val errorMessage = when (error) {
                        is OtpError -> error.getUserMessage()
                        else -> UserFacingErrorMapper.map(error, "Verification failed. Please try again.")
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
     * Resend OTP.
     * Cooldown is managed by the ViewModel (survives recomposition/rotation).
     */
    fun resendOTP(
        phoneNumber: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        // Block during active cooldown or in-flight request
        if (_uiState.value.isLoading || _uiState.value.resendCooldownSeconds > 0) return

        _uiState.value = _uiState.value.copy(isLoading = true)

        viewModelScope.launch {
            val result = otpRepository.sendOTP(phoneNumber)
            result.fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(isLoading = false)
                    startResendCooldown(DEFAULT_RESEND_COOLDOWN)
                    onSuccess()
                },
                onFailure = { error ->
                    Timber.e(error, "OTP resend failed")
                    val errorMessage = when (error) {
                        is OtpError -> error.getUserMessage()
                        else -> UserFacingErrorMapper.map(error, "We couldn't resend the code. Please try again.")
                    }
                    // Apply server Retry-After if available, otherwise default cooldown
                    val cooldown = (error as? OtpError.RateLimited)?.retryAfterSeconds
                        ?: DEFAULT_RESEND_COOLDOWN
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        otpError = errorMessage
                    )
                    // Always apply cooldown on failure to prevent hammering
                    startResendCooldown(cooldown)
                    if (error is OtpError.RateLimited) {
                        Timber.w("[SECURITY] OTP resend rate limited for $phoneNumber")
                    }
                    onError(errorMessage)
                }
            )
        }
    }

    /**
     * Start a ViewModel-managed cooldown that survives recomposition and rotation.
     */
    private fun startResendCooldown(seconds: Int) {
        cooldownJob?.cancel()
        _uiState.value = _uiState.value.copy(resendCooldownSeconds = seconds)
        cooldownJob = viewModelScope.launch {
            var remaining = seconds
            while (remaining > 0) {
                delay(1_000)
                remaining--
                _uiState.value = _uiState.value.copy(resendCooldownSeconds = remaining)
            }
        }
    }

    companion object {
        const val DEFAULT_RESEND_COOLDOWN = 60
    }
}

/**
 * UI State for OTP Verification Screen.
 * Cooldown is managed by the ViewModel, not local Composable state.
 */
data class OtpVerificationUiState(
    val otpCode: String = "",
    val otpError: String? = null,
    val isLoading: Boolean = false,
    /** Remaining resend cooldown seconds managed by ViewModel. 0 = can resend. */
    val resendCooldownSeconds: Int = 0
)
