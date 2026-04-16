package com.shourov.apps.pacedream.signin

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shourov.apps.pacedream.core.network.auth.AuthSession
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.pacedream.common.util.UserFacingErrorMapper
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class EmailSignInViewModel @Inject constructor(
    private val authSession: AuthSession
) : ViewModel() {

    private val _uiState = MutableStateFlow(EmailSignInUiState())
    val uiState: StateFlow<EmailSignInUiState> = _uiState.asStateFlow()

    fun loginWithEmail(
        email: String,
        password: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        if (email.isBlank() || password.isBlank()) {
            onError("Email and password cannot be empty")
            return
        }
        if (_uiState.value.isLoading || _uiState.value.isGoogleLoading || _uiState.value.isAppleLoading) return

        _uiState.value = _uiState.value.copy(isLoading = true, error = null)

        viewModelScope.launch {
            val result = authSession.loginWithEmailPassword(email, password)
            result.fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(isLoading = false)
                    onSuccess()
                },
                onFailure = { error ->
                    Timber.e(error, "Email login failed")
                    val errorMessage = UserFacingErrorMapper.forLogin(error)
                    _uiState.value = _uiState.value.copy(isLoading = false, error = errorMessage)
                    onError(errorMessage)
                }
            )
        }
    }

    fun registerWithEmail(
        email: String,
        password: String,
        firstName: String = "",
        lastName: String = "",
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        if (email.isBlank() || password.isBlank()) {
            onError("Email and password cannot be empty")
            return
        }
        if (_uiState.value.isLoading || _uiState.value.isGoogleLoading || _uiState.value.isAppleLoading) return

        _uiState.value = _uiState.value.copy(isLoading = true, error = null)

        viewModelScope.launch {
            val result = authSession.registerWithEmailPassword(email, password, firstName, lastName)
            result.fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(isLoading = false)
                    onSuccess()
                },
                onFailure = { error ->
                    Timber.e(error, "Email registration failed")
                    val errorMessage = UserFacingErrorMapper.forRegistration(error)
                    _uiState.value = _uiState.value.copy(isLoading = false, error = errorMessage)
                    onError(errorMessage)
                }
            )
        }
    }

    fun loginWithGoogle(
        activity: Activity,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        if (_uiState.value.isLoading || _uiState.value.isGoogleLoading || _uiState.value.isAppleLoading) return
        _uiState.value = _uiState.value.copy(isGoogleLoading = true, error = null)

        viewModelScope.launch {
            try {
                val result = authSession.loginWithAuth0(activity, "google-oauth2")
                result.fold(
                    onSuccess = {
                        _uiState.value = _uiState.value.copy(isGoogleLoading = false)
                        onSuccess()
                    },
                    onFailure = { error ->
                        Timber.e(error, "Google login failed")
                        val errorMessage = UserFacingErrorMapper.forGoogleLogin(error)
                        _uiState.value = _uiState.value.copy(isGoogleLoading = false, error = errorMessage)
                        onError(errorMessage)
                    }
                )
            } catch (e: Exception) {
                Timber.e(e, "Google login failed")
                val errorMessage = UserFacingErrorMapper.forGoogleLogin(e)
                _uiState.value = _uiState.value.copy(isGoogleLoading = false, error = errorMessage)
                onError(errorMessage)
            }
        }
    }

    fun loginWithApple(
        activity: Activity,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        if (_uiState.value.isLoading || _uiState.value.isGoogleLoading || _uiState.value.isAppleLoading) return
        _uiState.value = _uiState.value.copy(isAppleLoading = true, error = null)

        viewModelScope.launch {
            try {
                val result = authSession.loginWithAuth0(activity, "apple")
                result.fold(
                    onSuccess = {
                        _uiState.value = _uiState.value.copy(isAppleLoading = false)
                        onSuccess()
                    },
                    onFailure = { error ->
                        Timber.e(error, "Apple login failed")
                        val errorMessage = UserFacingErrorMapper.forAppleLogin(error)
                        _uiState.value = _uiState.value.copy(isAppleLoading = false, error = errorMessage)
                        onError(errorMessage)
                    }
                )
            } catch (e: Exception) {
                Timber.e(e, "Apple login failed")
                val errorMessage = UserFacingErrorMapper.forAppleLogin(e)
                _uiState.value = _uiState.value.copy(isAppleLoading = false, error = errorMessage)
                onError(errorMessage)
            }
        }
    }

    /** Cooldown tracking for forgot-password from this screen. */
    private var forgotPasswordCooldownEndMs: Long = 0L

    fun forgotPassword(
        email: String,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        if (_uiState.value.isLoading) return

        // Enforce cooldown
        val now = System.currentTimeMillis()
        if (now < forgotPasswordCooldownEndMs) {
            val remainingSec = ((forgotPasswordCooldownEndMs - now) / 1000).toInt()
            onError("Please wait $remainingSec seconds before requesting another reset link.")
            return
        }

        val trimmedEmail = email.trim()
        if (trimmedEmail.isBlank()) {
            onError("Please enter your email address")
            return
        }
        val emailRegex = "^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$".toRegex()
        if (!emailRegex.matches(trimmedEmail)) {
            onError("Please enter a valid email address")
            return
        }

        _uiState.value = _uiState.value.copy(isLoading = true, error = null)

        viewModelScope.launch {
            val result = authSession.forgotPassword(trimmedEmail)
            // Start 60s cooldown regardless of outcome
            forgotPasswordCooldownEndMs = System.currentTimeMillis() + 60_000L

            result.fold(
                onSuccess = { _ ->
                    _uiState.value = _uiState.value.copy(isLoading = false)
                    // Uniform message to prevent account enumeration
                    onSuccess("If an account with this email exists, you'll receive a password reset link shortly.")
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(isLoading = false)
                    val errorMessage = when {
                        error.message?.contains("429", ignoreCase = true) == true ||
                        error.message?.contains("rate", ignoreCase = true) == true -> {
                            forgotPasswordCooldownEndMs = System.currentTimeMillis() + 120_000L
                            "Too many requests. Please try again later."
                        }
                        error.message?.contains("network", ignoreCase = true) == true ->
                            "Something went wrong. Please check your connection."
                        else -> {
                            // Uniform message for non-network errors to prevent enumeration
                            onSuccess("If an account with this email exists, you'll receive a password reset link shortly.")
                            return@launch
                        }
                    }
                    _uiState.value = _uiState.value.copy(error = errorMessage)
                    onError(errorMessage)
                }
            )
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}

data class EmailSignInUiState(
    val isLoading: Boolean = false,
    val isGoogleLoading: Boolean = false,
    val isAppleLoading: Boolean = false,
    val error: String? = null,
)
