package com.pacedream.app.feature.settings.notifications

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

data class NotificationsUiState(
    val isLoading: Boolean = false,
    val emailGeneral: Boolean = false,
    val pushGeneral: Boolean = false,
    val messageNotifications: Boolean = false,
    val bookingUpdates: Boolean = false,
    val bookingAlerts: Boolean = false,
    val marketingPromotions: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

@HiltViewModel
class SettingsNotificationsViewModel @Inject constructor(
    private val repository: AccountSettingsRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(NotificationsUiState(isLoading = true))
    val uiState: StateFlow<NotificationsUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, successMessage = null) }
            when (val result = repository.getNotificationSettings()) {
                is ApiResult.Success -> {
                    val s = result.data
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            emailGeneral = s.emailGeneral,
                            pushGeneral = s.pushGeneral,
                            messageNotifications = s.messageNotifications,
                            bookingUpdates = s.bookingUpdates,
                            bookingAlerts = s.bookingAlerts,
                            marketingPromotions = s.marketingPromotions
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

    fun toggleEmailGeneral() {
        _uiState.update { it.copy(emailGeneral = !it.emailGeneral) }
    }

    fun togglePushGeneral() {
        _uiState.update { it.copy(pushGeneral = !it.pushGeneral) }
    }

    fun toggleMessageNotifications() {
        _uiState.update { it.copy(messageNotifications = !it.messageNotifications) }
    }

    fun toggleBookingUpdates() {
        _uiState.update { it.copy(bookingUpdates = !it.bookingUpdates) }
    }

    fun toggleBookingAlerts() {
        _uiState.update { it.copy(bookingAlerts = !it.bookingAlerts) }
    }

    fun toggleMarketingPromotions() {
        _uiState.update { it.copy(marketingPromotions = !it.marketingPromotions) }
    }

    fun save() {
        val state = _uiState.value
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, successMessage = null) }
            val settings = AccountSettingsRepository.NotificationSettings(
                emailGeneral = state.emailGeneral,
                pushGeneral = state.pushGeneral,
                messageNotifications = state.messageNotifications,
                bookingUpdates = state.bookingUpdates,
                bookingAlerts = state.bookingAlerts,
                marketingPromotions = state.marketingPromotions
            )
            when (val result = repository.updateNotificationSettings(settings)) {
                is ApiResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            successMessage = "Notification settings updated successfully"
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

