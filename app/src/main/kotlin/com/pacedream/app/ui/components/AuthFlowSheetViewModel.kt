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
    val error: String? = null,
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
                error = null,
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
        _uiState.update { it.copy(mode = AuthFlowMode.SignIn, error = null) }
    }

    fun goToSignUp() {
        _uiState.update { it.copy(mode = AuthFlowMode.SignUp, error = null) }
    }

    fun updateFirstName(value: String) = _uiState.update { it.copy(firstName = value, error = null) }
    fun updateLastName(value: String) = _uiState.update { it.copy(lastName = value, error = null) }
    fun updateEmail(value: String) = _uiState.update { it.copy(email = value, error = null) }
    fun updatePassword(value: String) = _uiState.update { it.copy(password = value, error = null) }

    fun loginWithAuth0(activity: Activity, connection: Auth0Connection) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isGoogleLoading = connection == Auth0Connection.Google,
                    isAppleLoading = connection == Auth0Connection.Apple,
                    isEmailLoading = false,
                    error = null
                )
            }

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
                            error = result.message
                        )
                    }
                }
            }
        }
    }

    fun loginWithEmail() {
        viewModelScope.launch {
            val email = _uiState.value.email.trim()
            val password = _uiState.value.password

            _uiState.update { it.copy(isEmailLoading = true, error = null) }

            val result = sessionManager.loginWithEmailPassword(email, password)
            result.fold(
                onSuccess = {
                    _uiState.update { it.copy(isEmailLoading = false, success = true) }
                },
                onFailure = { e ->
                    _uiState.update { it.copy(isEmailLoading = false, error = e.message ?: "Sign in failed") }
                }
            )
        }
    }

    fun signUpWithEmail() {
        viewModelScope.launch {
            val first = _uiState.value.firstName.trim()
            val last = _uiState.value.lastName.trim()
            val email = _uiState.value.email.trim()
            val password = _uiState.value.password

            _uiState.update { it.copy(isEmailLoading = true, error = null) }

            val result = sessionManager.registerWithEmailPassword(
                email = email,
                password = password,
                firstName = first,
                lastName = last
            )

            result.fold(
                onSuccess = {
                    _uiState.update { it.copy(isEmailLoading = false, success = true) }
                },
                onFailure = { e ->
                    _uiState.update { it.copy(isEmailLoading = false, error = e.message ?: "Create account failed") }
                }
            )
        }
    }
}

