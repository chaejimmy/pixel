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

        _uiState.value = _uiState.value.copy(isLoading = true, error = null)

        viewModelScope.launch {
            val result = authSession.loginWithEmailPassword(email, password)
            result.fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(isLoading = false)
                    onSuccess()
                },
                onFailure = { error ->
                    val errorMessage = error.message ?: "Login failed"
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

        _uiState.value = _uiState.value.copy(isLoading = true, error = null)

        viewModelScope.launch {
            val result = authSession.registerWithEmailPassword(email, password, firstName, lastName)
            result.fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(isLoading = false)
                    onSuccess()
                },
                onFailure = { error ->
                    val errorMessage = error.message ?: "Registration failed"
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
        _uiState.value = _uiState.value.copy(isGoogleLoading = true, error = null)

        viewModelScope.launch {
            val result = authSession.loginWithAuth0(activity, "google-oauth2")
            result.fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(isGoogleLoading = false)
                    onSuccess()
                },
                onFailure = { error ->
                    val errorMessage = error.message ?: "Google login failed"
                    _uiState.value = _uiState.value.copy(isGoogleLoading = false, error = errorMessage)
                    onError(errorMessage)
                }
            )
        }
    }

    fun loginWithApple(
        activity: Activity,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        _uiState.value = _uiState.value.copy(isAppleLoading = true, error = null)

        viewModelScope.launch {
            val result = authSession.loginWithAuth0(activity, "apple")
            result.fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(isAppleLoading = false)
                    onSuccess()
                },
                onFailure = { error ->
                    val errorMessage = error.message ?: "Apple login failed"
                    _uiState.value = _uiState.value.copy(isAppleLoading = false, error = errorMessage)
                    onError(errorMessage)
                }
            )
        }
    }

    fun forgotPassword(
        email: String,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        if (email.isBlank()) {
            onError("Please enter your email address")
            return
        }

        _uiState.value = _uiState.value.copy(isLoading = true, error = null)

        viewModelScope.launch {
            val result = authSession.forgotPassword(email)
            result.fold(
                onSuccess = { message ->
                    _uiState.value = _uiState.value.copy(isLoading = false)
                    onSuccess(message)
                },
                onFailure = { error ->
                    val errorMessage = error.message ?: "Failed to send reset link"
                    _uiState.value = _uiState.value.copy(isLoading = false, error = errorMessage)
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
