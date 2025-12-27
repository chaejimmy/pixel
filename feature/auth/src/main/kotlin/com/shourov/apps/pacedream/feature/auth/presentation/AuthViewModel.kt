package com.shourov.apps.pacedream.feature.auth.presentation

import android.app.Activity
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shourov.apps.pacedream.core.network.auth.AuthSession
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AuthUiState(
    val isLoginMode: Boolean = true,
    val firstName: String = "",
    val lastName: String = "",
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authSession: AuthSession,
    @ApplicationContext private val context: Context
) : ViewModel() {
    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()
    
    fun updateFirstName(firstName: String) {
        _uiState.value = _uiState.value.copy(firstName = firstName, error = null)
    }
    
    fun updateLastName(lastName: String) {
        _uiState.value = _uiState.value.copy(lastName = lastName, error = null)
    }
    
    fun updateEmail(email: String) {
        _uiState.value = _uiState.value.copy(email = email, error = null)
    }
    
    fun updatePassword(password: String) {
        _uiState.value = _uiState.value.copy(password = password, error = null)
    }
    
    fun updateConfirmPassword(confirmPassword: String) {
        _uiState.value = _uiState.value.copy(confirmPassword = confirmPassword, error = null)
    }
    
    fun switchToLogin() {
        _uiState.value = _uiState.value.copy(
            isLoginMode = true,
            error = null
        )
    }
    
    fun switchToRegister() {
        _uiState.value = _uiState.value.copy(
            isLoginMode = false,
            error = null
        )
    }
    
    /**
     * Login with Auth0 Universal Login
     * This is the primary authentication method matching iOS behavior
     */
    fun loginWithAuth0(onSuccess: () -> Unit) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                // Get Activity context for Auth0 WebAuth
                val activity = context as? Activity
                if (activity != null) {
                    authSession.loginWithAuth0(activity)
                        .onSuccess {
                            _uiState.value = _uiState.value.copy(isLoading = false)
                            onSuccess()
                        }
                        .onFailure { exception ->
                            _uiState.value = _uiState.value.copy(
                                error = exception.message ?: "Auth0 login failed",
                                isLoading = false
                            )
                        }
                } else {
                    // Fallback: Auth0 login requires Activity context
                    // This will be called from a Composable with LocalContext
                    _uiState.value = _uiState.value.copy(
                        error = "Unable to launch Auth0 login",
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = e.message ?: "Authentication failed",
                    isLoading = false
                )
            }
        }
    }
    
    /**
     * Login with Auth0 using Activity context
     * Call this from Composable with LocalContext.current as Activity
     */
    fun loginWithAuth0(activity: Activity, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                authSession.loginWithAuth0(activity)
                    .onSuccess {
                        _uiState.value = _uiState.value.copy(isLoading = false)
                        onSuccess()
                    }
                    .onFailure { exception ->
                        _uiState.value = _uiState.value.copy(
                            error = exception.message ?: "Auth0 login failed",
                            isLoading = false
                        )
                    }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = e.message ?: "Authentication failed",
                    isLoading = false
                )
            }
        }
    }
    
    fun login(onSuccess: () -> Unit) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                // Validate input
                if (_uiState.value.email.isBlank() || _uiState.value.password.isBlank()) {
                    _uiState.value = _uiState.value.copy(
                        error = "Please fill in all fields",
                        isLoading = false
                    )
                    return@launch
                }
                
                // Validate email format
                if (!android.util.Patterns.EMAIL_ADDRESS.matcher(_uiState.value.email).matches()) {
                    _uiState.value = _uiState.value.copy(
                        error = "Please enter a valid email address",
                        isLoading = false
                    )
                    return@launch
                }
                
                // Call backend login API
                authSession.loginWithEmailPassword(
                    email = _uiState.value.email,
                    password = _uiState.value.password
                ).onSuccess {
                    _uiState.value = _uiState.value.copy(isLoading = false)
                    onSuccess()
                }.onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        error = exception.message ?: "Login failed",
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = e.message ?: "Login failed",
                    isLoading = false
                )
            }
        }
    }
    
    fun register(onSuccess: () -> Unit) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                // Validate input
                if (_uiState.value.firstName.isBlank() || 
                    _uiState.value.lastName.isBlank() || 
                    _uiState.value.email.isBlank() || 
                    _uiState.value.password.isBlank() || 
                    _uiState.value.confirmPassword.isBlank()) {
                    _uiState.value = _uiState.value.copy(
                        error = "Please fill in all fields",
                        isLoading = false
                    )
                    return@launch
                }
                
                // Validate email format
                if (!android.util.Patterns.EMAIL_ADDRESS.matcher(_uiState.value.email).matches()) {
                    _uiState.value = _uiState.value.copy(
                        error = "Please enter a valid email address",
                        isLoading = false
                    )
                    return@launch
                }
                
                // Validate passwords match
                if (_uiState.value.password != _uiState.value.confirmPassword) {
                    _uiState.value = _uiState.value.copy(
                        error = "Passwords do not match",
                        isLoading = false
                    )
                    return@launch
                }
                
                // Validate password strength
                if (_uiState.value.password.length < 8) {
                    _uiState.value = _uiState.value.copy(
                        error = "Password must be at least 8 characters",
                        isLoading = false
                    )
                    return@launch
                }
                
                // Call backend registration API
                authSession.registerWithEmailPassword(
                    email = _uiState.value.email,
                    password = _uiState.value.password,
                    firstName = _uiState.value.firstName,
                    lastName = _uiState.value.lastName
                ).onSuccess {
                    _uiState.value = _uiState.value.copy(isLoading = false)
                    onSuccess()
                }.onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        error = exception.message ?: "Registration failed",
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = e.message ?: "Registration failed",
                    isLoading = false
                )
            }
        }
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
