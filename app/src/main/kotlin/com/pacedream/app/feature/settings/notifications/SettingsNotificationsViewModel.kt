package com.pacedream.app.feature.settings.notifications

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pacedream.app.core.auth.SessionManager
import com.pacedream.app.core.network.ApiError
import com.pacedream.app.core.network.ApiResult
import com.pacedream.app.feature.settings.AccountSettingsRepository
import com.shourov.apps.pacedream.core.common.network.di.ApplicationScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import timber.log.Timber
import javax.inject.Inject

/**
 * UI state matching iOS NotificationsSettingsView (iOS parity).
 *
 * Includes all notification categories from iOS: email, push, messages,
 * booking updates/alerts, marketing, reviews, friend requests, system,
 * SMS, and quiet hours.
 */
data class NotificationsUiState(
    val isLoading: Boolean = false,
    val emailGeneral: Boolean = true,
    val pushGeneral: Boolean = true,
    val messageNotifications: Boolean = true,
    val bookingUpdates: Boolean = true,
    val bookingAlerts: Boolean = true,
    val marketingPromotions: Boolean = false,
    // iOS parity: additional categories
    val reviewNotifications: Boolean = true,
    val friendRequestNotifications: Boolean = true,
    val systemNotifications: Boolean = true,
    val smsNotifications: Boolean = false,
    // iOS parity: quiet hours
    val quietHoursEnabled: Boolean = false,
    val quietHoursStart: String = "22:00",
    val quietHoursEnd: String = "08:00",
    val errorMessage: String? = null,
    val successMessage: String? = null
)

@HiltViewModel
class SettingsNotificationsViewModel @Inject constructor(
    private val repository: AccountSettingsRepository,
    private val sessionManager: SessionManager,
    private val json: Json,
    @ApplicationContext private val context: Context,
    @ApplicationScope private val applicationScope: CoroutineScope
) : ViewModel() {

    private val _uiState = MutableStateFlow(NotificationsUiState(isLoading = true))
    val uiState: StateFlow<NotificationsUiState> = _uiState.asStateFlow()

    private var autoSaveJob: Job? = null

    /** Snapshot of the last confirmed-saved state, used for rollback on failure. */
    private var lastSavedState: NotificationsUiState? = null

    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME, Context.MODE_PRIVATE
    )

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, successMessage = null) }

            // Load cached local state first for instant display
            val cached = loadFromLocal()
            val hasPendingSync = isPendingSync()
            if (cached != null) {
                val cachedUiState = cached.toUiState()
                _uiState.update { cachedUiState.copy(isLoading = true) }
                lastSavedState = cachedUiState
            }

            // If there are unsynced local changes, don't let the backend fetch
            // overwrite them — kick off a retry sync instead.
            if (hasPendingSync && cached != null) {
                _uiState.update { it.copy(isLoading = false) }
                scheduleRemoteSync()
                return@launch
            }

            // Then fetch from backend
            try {
                when (val result = repository.getNotificationSettings()) {
                    is ApiResult.Success -> {
                        val s = result.data
                        val newState = _uiState.value.copy(
                            isLoading = false,
                            emailGeneral = s.emailGeneral,
                            pushGeneral = s.pushGeneral,
                            messageNotifications = s.messageNotifications,
                            bookingUpdates = s.bookingUpdates,
                            bookingAlerts = s.bookingAlerts,
                            marketingPromotions = s.marketingPromotions,
                            // These fields are not in backend yet — keep local values
                            reviewNotifications = cached?.reviewNotifications ?: s.reviewNotifications,
                            friendRequestNotifications = cached?.friendRequestNotifications ?: s.friendRequestNotifications,
                            systemNotifications = cached?.systemNotifications ?: s.systemNotifications,
                            smsNotifications = cached?.smsNotifications ?: s.smsNotifications,
                            quietHoursEnabled = cached?.quietHoursEnabled ?: false,
                            quietHoursStart = s.quietHoursStart ?: "22:00",
                            quietHoursEnd = s.quietHoursEnd ?: "08:00"
                        )
                        _uiState.value = newState
                        lastSavedState = newState
                        saveToLocal(newState.toSettings())
                    }
                    is ApiResult.Failure -> {
                        if (result.error is ApiError.Unauthorized) {
                            sessionManager.signOut()
                            return@launch
                        }
                        // If we have cached data, use it (already loaded above).
                        // Only show error if no cache.
                        if (cached == null) {
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    errorMessage = com.pacedream.common.util.UserFacingErrorMapper.map(result.error, "We couldn't load your notification settings. Please try again.")
                                )
                            }
                        } else {
                            _uiState.update { it.copy(isLoading = false) }
                            Timber.w("Backend load failed, using cached notification settings: ${result.error.message}")
                        }
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Unexpected error loading notification settings")
                if (cached == null) {
                    _uiState.update { it.copy(isLoading = false, errorMessage = "Unable to load notification settings.") }
                } else {
                    _uiState.update { it.copy(isLoading = false) }
                }
            }
        }
    }

    fun toggleEmailGeneral() = applyToggle { it.copy(emailGeneral = !it.emailGeneral) }

    fun togglePushGeneral() = applyToggle { it.copy(pushGeneral = !it.pushGeneral) }

    fun toggleMessageNotifications() = applyToggle { it.copy(messageNotifications = !it.messageNotifications) }

    fun toggleBookingUpdates() = applyToggle { it.copy(bookingUpdates = !it.bookingUpdates) }

    fun toggleBookingAlerts() = applyToggle { it.copy(bookingAlerts = !it.bookingAlerts) }

    fun toggleMarketingPromotions() = applyToggle { it.copy(marketingPromotions = !it.marketingPromotions) }

    fun toggleReviewNotifications() = applyToggle { it.copy(reviewNotifications = !it.reviewNotifications) }

    fun toggleFriendRequestNotifications() = applyToggle { it.copy(friendRequestNotifications = !it.friendRequestNotifications) }

    fun toggleSystemNotifications() = applyToggle { it.copy(systemNotifications = !it.systemNotifications) }

    fun toggleSmsNotifications() = applyToggle { it.copy(smsNotifications = !it.smsNotifications) }

    fun toggleQuietHours() = applyToggle { it.copy(quietHoursEnabled = !it.quietHoursEnabled) }

    /**
     * Apply a toggle change: update UI state, persist to local cache IMMEDIATELY
     * (so the change survives navigation/VM destruction), and schedule a debounced
     * remote sync on the application scope (so the PATCH request is not cancelled
     * when the user leaves the screen).
     */
    private inline fun applyToggle(transform: (NotificationsUiState) -> NotificationsUiState) {
        val next = transform(_uiState.value).copy(
            successMessage = null,
            errorMessage = null
        )
        _uiState.value = next
        // Persist locally right away — never lose a user toggle to VM death.
        saveToLocal(next.toSettings())
        setPendingSync(true)
        scheduleRemoteSync()
    }

    /**
     * Debounce the remote sync so rapid toggle changes are batched into a single
     * API call. Runs on [applicationScope] so it survives ViewModel destruction
     * (e.g. when the user navigates back immediately after toggling).
     */
    private fun scheduleRemoteSync() {
        autoSaveJob?.cancel()
        autoSaveJob = applicationScope.launch {
            delay(500L)
            performRemoteSync()
        }
    }

    /** Force an immediate remote sync (used by manual retry). */
    fun save() {
        autoSaveJob?.cancel()
        autoSaveJob = applicationScope.launch { performRemoteSync() }
    }

    private suspend fun performRemoteSync() {
        val state = _uiState.value
        _uiState.update { it.copy(isLoading = true, errorMessage = null, successMessage = null) }

        try {
            val settings = state.toSettings()
            when (val result = repository.updateNotificationSettings(settings)) {
                is ApiResult.Success -> {
                    lastSavedState = state
                    setPendingSync(false)
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            successMessage = "Settings saved"
                        )
                    }
                }
                is ApiResult.Failure -> {
                    if (result.error is ApiError.Unauthorized) {
                        sessionManager.signOut()
                        return
                    }
                    // Local cache already reflects the user's intent; keep it.
                    // Surface an error so they know the server is out of sync.
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = "Saved on this device. We'll retry syncing with the server."
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Unexpected error saving notification settings")
            _uiState.update {
                it.copy(
                    isLoading = false,
                    errorMessage = "Saved on this device. We'll retry syncing with the server."
                )
            }
        }
    }

    /** Called by the UI after a snackbar is displayed to prevent message replay. */
    fun consumeMessages() {
        _uiState.update { it.copy(errorMessage = null, successMessage = null) }
    }

    // ── Local persistence ────────────────────────────────────────────────────

    private fun saveToLocal(settings: AccountSettingsRepository.NotificationSettings) {
        try {
            val jsonString = json.encodeToString(settings)
            prefs.edit().putString(KEY_NOTIFICATION_SETTINGS, jsonString).apply()
        } catch (e: Exception) {
            Timber.e(e, "Failed to save notification settings to local cache")
        }
    }

    private fun loadFromLocal(): AccountSettingsRepository.NotificationSettings? {
        return try {
            val jsonString = prefs.getString(KEY_NOTIFICATION_SETTINGS, null) ?: return null
            json.decodeFromString<AccountSettingsRepository.NotificationSettings>(jsonString)
        } catch (e: Exception) {
            Timber.e(e, "Failed to load notification settings from local cache")
            null
        }
    }

    private fun isPendingSync(): Boolean = prefs.getBoolean(KEY_PENDING_SYNC, false)

    private fun setPendingSync(pending: Boolean) {
        prefs.edit().putBoolean(KEY_PENDING_SYNC, pending).apply()
    }

    // ── Mapping helpers ──────────────────────────────────────────────────────

    private fun NotificationsUiState.toSettings() = AccountSettingsRepository.NotificationSettings(
        emailGeneral = emailGeneral,
        pushGeneral = pushGeneral,
        messageNotifications = messageNotifications,
        bookingUpdates = bookingUpdates,
        bookingAlerts = bookingAlerts,
        marketingPromotions = marketingPromotions,
        instantMessages = messageNotifications, // mirror messages toggle
        productUpdatesEnabled = systemNotifications, // mirror system toggle
        reviewNotifications = reviewNotifications,
        friendRequestNotifications = friendRequestNotifications,
        systemNotifications = systemNotifications,
        smsNotifications = smsNotifications,
        quietHoursEnabled = quietHoursEnabled,
        quietHoursStart = quietHoursStart,
        quietHoursEnd = quietHoursEnd
    )

    private fun AccountSettingsRepository.NotificationSettings.toUiState() = NotificationsUiState(
        emailGeneral = emailGeneral,
        pushGeneral = pushGeneral,
        messageNotifications = messageNotifications,
        bookingUpdates = bookingUpdates,
        bookingAlerts = bookingAlerts,
        marketingPromotions = marketingPromotions,
        reviewNotifications = reviewNotifications,
        friendRequestNotifications = friendRequestNotifications,
        systemNotifications = systemNotifications,
        smsNotifications = smsNotifications,
        quietHoursEnabled = quietHoursEnabled,
        quietHoursStart = quietHoursStart ?: "22:00",
        quietHoursEnd = quietHoursEnd ?: "08:00"
    )

    companion object {
        private const val PREFS_NAME = "pacedream_notification_settings"
        private const val KEY_NOTIFICATION_SETTINGS = "notification_settings_v1"
        private const val KEY_PENDING_SYNC = "notification_settings_pending_sync_v1"
    }
}
