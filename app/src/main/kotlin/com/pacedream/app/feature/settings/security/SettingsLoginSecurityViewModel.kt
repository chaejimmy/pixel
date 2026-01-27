package com.pacedream.app.feature.settings.security

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pacedream.app.core.auth.SessionManager
import com.pacedream.app.core.network.ApiError
import com.pacedream.app.core.network.ApiResult
import com.pacedream.app.feature.settings.AccountSettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LoginSecurityUiState(
    val currentPassword: String = "",
    val newPassword: String = "",
    val confirmPassword: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val deactivateSuccess: Boolean = false
)

@HiltViewModel
class SettingsLoginSecurityViewModel @Inject constructor(
    private val repository: AccountSettingsRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginSecurityUiState())
    val uiState: StateFlow<LoginSecurityUiState> = _uiState.asStateFlow()

    fun onCurrentPasswordChange(value: String) {
        _uiState.update { it.copy(currentPassword = value, errorMessage = null, successMessage = null) }
    }

    fun onNewPasswordChange(value: String) {
        _uiState.update { it.copy(newPassword = value, errorMessage = null, successMessage = null) }
    }

    fun onConfirmPasswordChange(value: String) {
        _uiState.update { it.copy(confirmPassword = value, errorMessage = null, successMessage = null) }
    }

    fun changePassword() {
        val state = _uiState.value
        if (state.newPassword.length < 8) {
            _uiState.update { it.copy(errorMessage = "New password must be at least 8 characters.") }
            return
        }
        if (state.newPassword != state.confirmPassword) {
            _uiState.update { it.copy(errorMessage = "New password and confirmation do not match.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, successMessage = null) }
            when (val result = repository.changePassword(state.currentPassword, state.newPassword)) {
                is ApiResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            currentPassword = "",
                            newPassword = "",
                            confirmPassword = "",
                            successMessage = "Password updated successfully"
                        )
                    }
                }
                is ApiResult.Failure -> {
                    if (result.error is ApiError.Unauthorized) {
                        sessionManager.signOut()
                    }
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = result.error.message
                        )
                    }
                }
            }
        }
    }

    fun deactivateAccount() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, successMessage = null) }
            when (val result = repository.deactivateAccount()) {
                is ApiResult.Success -> {
                    sessionManager.signOut()
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            deactivateSuccess = true,
                            successMessage = "Account deactivated."
                        )
                    }
                }
                is ApiResult.Failure -> {
                    if (result.error is ApiError.Unauthorized) {
                        sessionManager.signOut()
                    }
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = result.error.message
                        )
                    }
                }
            }
        }
    }
}

