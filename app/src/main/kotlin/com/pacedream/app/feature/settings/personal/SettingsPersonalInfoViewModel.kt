package com.pacedream.app.feature.settings.personal

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

data class PersonalInfoUiState(
    val isLoading: Boolean = false,
    val firstName: String = "",
    val lastName: String = "",
    val email: String = "",
    val phoneNumber: String = "",
    val errorMessage: String? = null,
    val successMessage: String? = null
)

@HiltViewModel
class SettingsPersonalInfoViewModel @Inject constructor(
    private val repository: AccountSettingsRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(PersonalInfoUiState(isLoading = true))
    val uiState: StateFlow<PersonalInfoUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, successMessage = null) }
            when (val result = repository.getAccount()) {
                is ApiResult.Success -> {
                    val profile = result.data
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            firstName = profile.firstName.orEmpty(),
                            lastName = profile.lastName.orEmpty(),
                            email = profile.email.orEmpty(),
                            phoneNumber = profile.phoneNumber.orEmpty()
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

    fun onFirstNameChange(value: String) {
        _uiState.update { it.copy(firstName = value, successMessage = null, errorMessage = null) }
    }

    fun onLastNameChange(value: String) {
        _uiState.update { it.copy(lastName = value, successMessage = null, errorMessage = null) }
    }

    fun onEmailChange(value: String) {
        _uiState.update { it.copy(email = value, successMessage = null, errorMessage = null) }
    }

    fun onPhoneChange(value: String) {
        _uiState.update { it.copy(phoneNumber = value, successMessage = null, errorMessage = null) }
    }

    fun save() {
        val state = _uiState.value
        if (state.firstName.isBlank() || state.lastName.isBlank() || state.email.isBlank()) {
            _uiState.update { it.copy(errorMessage = "First name, last name, and email are required.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, successMessage = null) }
            when (val result = repository.updateProfile(
                firstName = state.firstName.trim(),
                lastName = state.lastName.trim(),
                email = state.email.trim(),
                phoneNumber = state.phoneNumber.trim().ifEmpty { null }
            )) {
                is ApiResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            successMessage = "Profile updated successfully."
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

