package com.shourov.apps.pacedream.signin.screens.signIn

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
import javax.inject.Inject

@HiltViewModel
class SignInViewModel @Inject constructor(
    private val authSession: AuthSession
) : ViewModel() {

    private val _uiState = MutableStateFlow(SignInUiState())
    val uiState: StateFlow<SignInUiState> = _uiState.asStateFlow()

    fun updateEmail(email: String) {
        _uiState.value = _uiState.value.copy(email = email, error = null)
    }

    fun updatePassword(password: String) {
        _uiState.value = _uiState.value.copy(password = password, error = null)
    }

    fun login(onSuccess: () -> Unit) {
        val state = _uiState.value
        if (state.isLoading) return // Prevent duplicate submissions
        if (state.email.isBlank() || state.password.isBlank()) {
            _uiState.value = state.copy(error = "Email and password are required")
            return
        }
        // Website parity: basic email format validation
        val emailRegex = "^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$".toRegex()
        if (!emailRegex.matches(state.email.trim())) {
            _uiState.value = state.copy(error = "Please enter a valid email address")
            return
        }
        // Website parity: password minimum 6 characters
        if (state.password.length < 6) {
            _uiState.value = state.copy(error = "Password must be at least 6 characters")
            return
        }

        _uiState.value = state.copy(isLoading = true, error = null)

        viewModelScope.launch {
            val result = authSession.loginWithEmailPassword(state.email, state.password)
            result.fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(isLoading = false)
                    onSuccess()
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = UserFacingErrorMapper.forLogin(error)
                    )
                }
            )
        }
    }

    fun forgotPassword(onSuccess: (String) -> Unit) {
        if (_uiState.value.isLoading) return // Prevent duplicate submissions
        val email = _uiState.value.email.trim()
        if (email.isBlank()) {
            _uiState.value = _uiState.value.copy(error = "Please enter your email first")
            return
        }
        val emailRegex = "^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$".toRegex()
        if (!emailRegex.matches(email)) {
            _uiState.value = _uiState.value.copy(error = "Please enter a valid email address")
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
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = UserFacingErrorMapper.forPasswordReset(error)
                    )
                }
            )
        }
    }

    fun loginWithGoogle(activity: Activity, onSuccess: () -> Unit) {
        if (_uiState.value.isGoogleLoading) return // Prevent duplicate submissions
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
                        _uiState.value = _uiState.value.copy(
                            isGoogleLoading = false,
                            error = UserFacingErrorMapper.forGoogleLogin(error)
                        )
                    }
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isGoogleLoading = false,
                    error = UserFacingErrorMapper.forGoogleLogin(e)
                )
            }
        }
    }
}

data class SignInUiState(
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val isGoogleLoading: Boolean = false,
    val error: String? = null,
)
