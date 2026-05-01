package com.pacedream.app.feature.hostprofile

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pacedream.app.core.auth.AuthState
import com.pacedream.app.core.auth.SessionManager
import com.pacedream.app.core.config.AppConfig
import com.pacedream.app.core.network.ApiClient
import com.pacedream.app.core.network.ApiResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import timber.log.Timber
import javax.inject.Inject

const val HOST_PROFILE_ID_ARG = "hostId"

/** One-shot UI message surfaced to the screen via SnackbarHost. */
data class SnackbarMessage(val text: String)

@HiltViewModel
class HostProfileViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: HostProfileRepository,
    private val sessionManager: SessionManager,
    private val apiClient: ApiClient,
    private val appConfig: AppConfig,
    private val json: Json,
) : ViewModel() {

    sealed class Effect {
        data object ShowAuthRequired : Effect()
        data class NavigateToThread(val threadId: String) : Effect()
    }

    private val hostId: String = savedStateHandle.get<String>(HOST_PROFILE_ID_ARG).orEmpty()

    private val _uiState = MutableStateFlow<HostProfileUiState>(HostProfileUiState.Loading)
    val uiState: StateFlow<HostProfileUiState> = _uiState.asStateFlow()

    private val _snackbar = Channel<SnackbarMessage>(Channel.BUFFERED)
    val snackbar = _snackbar.receiveAsFlow()

    private val _effects = Channel<Effect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    init {
        refresh()
    }

    fun refresh() {
        if (hostId.isBlank()) {
            _uiState.value = HostProfileUiState.NotFound
            return
        }
        viewModelScope.launch {
            _uiState.value = HostProfileUiState.Loading
            when (val result = repository.fetchHostProfile(hostId)) {
                is HostProfileResult.Success ->
                    _uiState.value = HostProfileUiState.Content(host = result.host)
                is HostProfileResult.NotFound ->
                    _uiState.value = HostProfileUiState.NotFound
                is HostProfileResult.Error ->
                    _uiState.value = HostProfileUiState.Error(result.message)
            }
        }
    }

    fun isAuthenticated(): Boolean = sessionManager.authState.value == AuthState.Authenticated

    /**
     * Open or create a 1:1 conversation with this host.
     * Mirrors ListingDetailViewModel.contactHost so the messaging contract
     * stays a single source of truth on the backend
     * (POST /v1/inbox/thread).
     */
    fun contactHost() {
        val state = _uiState.value as? HostProfileUiState.Content ?: return
        val targetUserId = state.host.userId?.takeIf { it.isNotBlank() }
            ?: state.host.hostId
            ?: hostId
        if (!isAuthenticated()) {
            viewModelScope.launch { _effects.send(Effect.ShowAuthRequired) }
            return
        }
        viewModelScope.launch {
            _uiState.value = state.copy(isContactingHost = true)
            val url = appConfig.buildApiUrl("inbox", "thread")
            val body = "{\"otherUserId\":\"$targetUserId\",\"mode\":\"guest\"}"
            when (val result = apiClient.post(url, body, includeAuth = true)) {
                is ApiResult.Success -> {
                    val threadId = try {
                        val obj = json.parseToJsonElement(result.data).jsonObject
                        obj["data"]?.jsonObject?.get("_id")?.jsonPrimitive?.content
                            ?: obj["_id"]?.jsonPrimitive?.content
                            ?: obj["data"]?.jsonObject?.get("id")?.jsonPrimitive?.content
                    } catch (e: Exception) {
                        Timber.e(e, "Failed to parse thread response")
                        null
                    }
                    _uiState.update { current ->
                        (current as? HostProfileUiState.Content)?.copy(isContactingHost = false)
                            ?: current
                    }
                    if (threadId != null) {
                        _effects.send(Effect.NavigateToThread(threadId))
                    } else {
                        _snackbar.send(SnackbarMessage("Could not start conversation"))
                    }
                }
                is ApiResult.Failure -> {
                    Timber.e("Failed to create thread: ${result.error.message}")
                    _uiState.update { current ->
                        (current as? HostProfileUiState.Content)?.copy(isContactingHost = false)
                            ?: current
                    }
                    _snackbar.send(SnackbarMessage("We couldn't start this conversation. Please try again."))
                }
            }
        }
    }
}
