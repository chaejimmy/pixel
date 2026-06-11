package com.pacedream.app.ui.components

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pacedream.app.core.auth.Auth0Connection
import com.pacedream.app.core.auth.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

enum class AuthFlowMode { Chooser, SignIn, SignUp }

data class AuthFlowSheetUiState(
    val mode: AuthFlowMode = AuthFlowMode.Chooser,
    val firstName: String = "",
    val lastName: String = "",
    val email: String = "",
    val password: String = "",
    val isEmailLoading: Boolean = false,
    val isGoogleLoading: Boolean = false,
    val isAppleLoading: Boolean = false,
    val isResetLoading: Boolean = false,
    val error: String? = null,
    /** Non-error status message, e.g. "Password reset link sent to your email". */
    val info: String? = null,
    val success: Boolean = false
)

@HiltViewModel
class AuthFlowSheetViewModel @Inject constructor(
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthFlowSheetUiState())
    val uiState: StateFlow<AuthFlowSheetUiState> = _uiState.asStateFlow()

    fun onPresented() {
        _uiState.update {
            it.copy(
                mode = AuthFlowMode.Chooser,
                isEmailLoading = false,
                isGoogleLoading = false,
                isAppleLoading = false,
                isResetLoading = false,
                error = null,
                info = null,
                success = false
            )
        }
    }

    fun consumeSuccess() {
        _uiState.update { it.copy(success = false) }
    }

    fun onNotNow() {
        _uiState.update {
            it.copy(
                isEmailLoading = false,
                isGoogleLoading = false,
                isAppleLoading = false,
                error = null
            )
        }
    }

    fun goToSignIn() {
        _uiState.update { it.copy(mode = AuthFlowMode.SignIn, error = null, info = null) }
    }

    fun goToSignUp() {
        _uiState.update { it.copy(mode = AuthFlowMode.SignUp, error = null, info = null) }
    }

    fun updateFirstName(value: String) = _uiState.update { it.copy(firstName = value, error = null) }
    fun updateLastName(value: String) = _uiState.update { it.copy(lastName = value, error = null) }
    fun updateEmail(value: String) = _uiState.update { it.copy(email = value, error = null, info = null) }
    fun updatePassword(value: String) = _uiState.update { it.copy(password = value, error = null) }

    private val AuthFlowSheetUiState.isAnyLoading: Boolean
        get() = isEmailLoading || isGoogleLoading || isAppleLoading || isResetLoading

    private fun String?.sanitizeAuthError(): String? =
        this?.removePrefix("Server error 200: ")

    fun loginWithApple(activity: Activity) = loginWithAuth0(activity, Auth0Connection.Apple)

    fun loginWithAuth0(activity: Activity, connection: Auth0Connection) {
        if (_uiState.value.isAnyLoading) return
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isGoogleLoading = connection == Auth0Connection.Google,
                    isAppleLoading = connection == Auth0Connection.Apple,
                    isEmailLoading = false,
                    error = null
                )
            }

            try {
                val result = sessionManager.loginWithAuth0(activity, connection)
                when (result) {
                    SessionManager.AuthActionResult.Success -> {
                        _uiState.update {
                            it.copy(
                                isGoogleLoading = false,
                                isAppleLoading = false,
                                success = true
                            )
                        }
                    }
                    SessionManager.AuthActionResult.Cancelled -> {
                        // Cancelling is not an error (iOS parity) - no-op.
                        _uiState.update { it.copy(isGoogleLoading = false, isAppleLoading = false) }
                    }
                    is SessionManager.AuthActionResult.Error -> {
                        _uiState.update {
                            it.copy(
                                isGoogleLoading = false,
                                isAppleLoading = false,
                                error = result.message.sanitizeAuthError()
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isGoogleLoading = false,
                        isAppleLoading = false,
                        error = (e.message ?: "Authentication failed").sanitizeAuthError()
                    )
                }
            }
        }
    }

    fun loginWithEmail() {
        if (_uiState.value.isAnyLoading) return
        viewModelScope.launch {
            val email = _uiState.value.email.trim()
            val password = _uiState.value.password

            _uiState.update { it.copy(isEmailLoading = true, error = null) }

            try {
                Timber.d("AuthFlowSheet: loginWithEmail starting")
                val result = sessionManager.loginWithEmailPassword(email, password)
                result.fold(
                    onSuccess = {
                        Timber.d("AuthFlowSheet: login success received, dismissing sheet")
                        _uiState.update { it.copy(isEmailLoading = false, success = true) }
                    },
                    onFailure = { e ->
                        Timber.e(e, "AuthFlowSheet: login failed")
                        _uiState.update { it.copy(isEmailLoading = false, error = (e.message ?: "Sign in failed").sanitizeAuthError()) }
                    }
                )
            } catch (e: Exception) {
                Timber.e(e, "AuthFlowSheet: login exception")
                _uiState.update { it.copy(isEmailLoading = false, error = (e.message ?: "Sign in failed").sanitizeAuthError()) }
            }
        }
    }

    /**
     * Send a password-reset link to the email entered on the sign-in form.
     * Requires the email field to be filled; surfaces the backend's
     * confirmation message inline on success.
     */
    fun sendPasswordReset() {
        if (_uiState.value.isAnyLoading) return
        val email = _uiState.value.email.trim()
        if (email.isBlank()) {
            _uiState.update { it.copy(error = "Enter your email above to reset your password") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isResetLoading = true, error = null, info = null) }
            try {
                sessionManager.forgotPassword(email).fold(
                    onSuccess = { message ->
                        _uiState.update { it.copy(isResetLoading = false, info = message) }
                    },
                    onFailure = { e ->
                        Timber.e(e, "AuthFlowSheet: password reset failed")
                        _uiState.update {
                            it.copy(
                                isResetLoading = false,
                                error = (e.message ?: "Failed to send reset link").sanitizeAuthError()
                            )
                        }
                    }
                )
            } catch (e: Exception) {
                Timber.e(e, "AuthFlowSheet: password reset exception")
                _uiState.update {
                    it.copy(
                        isResetLoading = false,
                        error = (e.message ?: "Failed to send reset link").sanitizeAuthError()
                    )
                }
            }
        }
    }

    fun signUpWithEmail() {
        if (_uiState.value.isAnyLoading) return
        viewModelScope.launch {
            val first = _uiState.value.firstName.trim()
            val last = _uiState.value.lastName.trim()
            val email = _uiState.value.email.trim()
            val password = _uiState.value.password

            _uiState.update { it.copy(isEmailLoading = true, error = null) }

            try {
                Timber.d("AuthFlowSheet: signUpWithEmail starting")
                val result = sessionManager.registerWithEmailPassword(
                    email = email,
                    password = password,
                    firstName = first,
                    lastName = last
                )

                result.fold(
                    onSuccess = {
                        Timber.d("AuthFlowSheet: signup success received, dismissing sheet")
                        _uiState.update { it.copy(isEmailLoading = false, success = true) }
                    },
                    onFailure = { e ->
                        Timber.e(e, "AuthFlowSheet: signup failed")
                        _uiState.update { it.copy(isEmailLoading = false, error = (e.message ?: "Create account failed").sanitizeAuthError()) }
                    }
                )
            } catch (e: Exception) {
                Timber.e(e, "AuthFlowSheet: signup exception")
                _uiState.update { it.copy(isEmailLoading = false, error = (e.message ?: "Create account failed").sanitizeAuthError()) }
            }
        }
    }
}

