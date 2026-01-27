package com.pacedream.app.feature.settings.preferences

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

data class PreferencesUiState(
    val isLoading: Boolean = false,
    val language: String = "",
    val currency: String = "",
    val timezone: String = "",
    val errorMessage: String? = null,
    val successMessage: String? = null
)

@HiltViewModel
class SettingsPreferencesViewModel @Inject constructor(
    private val repository: AccountSettingsRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(PreferencesUiState(isLoading = true))
    val uiState: StateFlow<PreferencesUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, successMessage = null) }
            when (val result = repository.getPreferences()) {
                is ApiResult.Success -> {
                    val prefs = result.data
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            language = prefs.language.orEmpty(),
                            currency = prefs.currency.orEmpty(),
                            timezone = prefs.timezone.orEmpty()
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

    fun onLanguageChange(value: String) {
        _uiState.update { it.copy(language = value, errorMessage = null, successMessage = null) }
    }

    fun onCurrencyChange(value: String) {
        _uiState.update { it.copy(currency = value, errorMessage = null, successMessage = null) }
    }

    fun onTimezoneChange(value: String) {
        _uiState.update { it.copy(timezone = value, errorMessage = null, successMessage = null) }
    }

    fun save() {
        val state = _uiState.value
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, successMessage = null) }
            val prefs = AccountSettingsRepository.Preferences(
                language = state.language.takeIf { it.isNotBlank() },
                currency = state.currency.takeIf { it.isNotBlank() },
                timezone = state.timezone.takeIf { it.isNotBlank() }
            )
            when (val result = repository.updatePreferences(prefs)) {
                is ApiResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            successMessage = "Preferences updated successfully"
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

