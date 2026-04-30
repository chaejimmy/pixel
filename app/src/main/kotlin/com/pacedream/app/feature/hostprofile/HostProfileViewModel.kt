package com.pacedream.app.feature.hostprofile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pacedream.app.core.auth.AuthState
import com.pacedream.app.core.auth.SessionManager
import com.pacedream.app.core.config.AppConfig
import com.pacedream.app.core.network.ApiClient
import com.pacedream.app.core.network.ApiResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
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

@HiltViewModel
class HostProfileViewModel @Inject constructor(
    private val repository: HostProfileRepository,
    private val sessionManager: SessionManager,
    private val apiClient: ApiClient,
    private val appConfig: AppConfig,
    private val json: Json,
) : ViewModel() {

    sealed class Effect {
        data object ShowAuthRequired : Effect()
        data class ShowToast(val message: String) : Effect()
        data class NavigateToThread(val threadId: String) : Effect()
    }

    private val _uiState = MutableStateFlow(HostProfileUiState())
    val uiState: StateFlow<HostProfileUiState> = _uiState.asStateFlow()

    private val _effects = Channel<Effect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    private var hostId: String? = null

    /**
     * Load a host profile.
     *
     * @param id           Host (user) id.
     * @param seed         Optional preview pulled from the listing detail (avatar, name).
     *                     Used to render the screen instantly while the fetch is in flight,
     *                     and as a graceful fallback if the backend has no host endpoint yet.
     */
    fun load(id: String, seed: HostProfileModel? = null) {
        if (id.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Host not found", isLoading = false) }
            return
        }
        if (hostId == id && _uiState.value.host != null) return
        hostId = id

        if (seed != null) {
            _uiState.update { it.copy(host = seed) }
        }
        refresh()
    }

    fun refresh() {
        val id = hostId ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val profileDeferred = async { repository.fetchHostProfile(id) }
            val listingsDeferred = async { repository.fetchHostListings(id) }
            val profile = profileDeferred.await()
            val hostListings = listingsDeferred.await()

            when (profile) {
                is ApiResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            host = profile.data.copy(listings = hostListings),
                            errorMessage = null,
                        )
                    }
                }
                is ApiResult.Failure -> {
                    val seeded = _uiState.value.host
                    if (seeded != null) {
                        // Fall back to the seed (from listing detail) and any listings we found.
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                host = seeded.copy(listings = hostListings),
                                errorMessage = null,
                            )
                        }
                    } else {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                errorMessage = "We couldn't load this host. Please try again.",
                            )
                        }
                    }
                }
            }
        }
    }

    fun isAuthenticated(): Boolean = sessionManager.authState.value == AuthState.Authenticated

    /**
     * Open or create a conversation with this host using the existing inbox flow.
     * Mirrors ListingDetailViewModel.contactHost so the messaging contract stays
     * a single source of truth on the backend (POST /v1/inbox/thread).
     */
    fun contactHost() {
        val id = hostId ?: return
        if (!isAuthenticated()) {
            viewModelScope.launch { _effects.send(Effect.ShowAuthRequired) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isContactingHost = true) }
            val url = appConfig.buildApiUrl("inbox", "thread")
            val body = "{\"otherUserId\":\"$id\",\"mode\":\"guest\"}"
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
                    _uiState.update { it.copy(isContactingHost = false) }
                    if (threadId != null) {
                        _effects.send(Effect.NavigateToThread(threadId))
                    } else {
                        _effects.send(Effect.ShowToast("Could not start conversation"))
                    }
                }
                is ApiResult.Failure -> {
                    Timber.e("Failed to create thread: ${result.error.message}")
                    _uiState.update { it.copy(isContactingHost = false) }
                    _effects.send(Effect.ShowToast("We couldn't start this conversation. Please try again."))
                }
            }
        }
    }
}
