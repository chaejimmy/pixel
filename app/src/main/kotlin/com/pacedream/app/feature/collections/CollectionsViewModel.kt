package com.pacedream.app.feature.collections

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pacedream.app.core.auth.AuthState
import com.pacedream.app.core.auth.SessionManager
import com.pacedream.app.core.network.ApiResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class CollectionsViewModel @Inject constructor(
    private val repository: CollectionRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    sealed class Effect {
        data object ShowAuthRequired : Effect()
        data class ShowToast(val message: String) : Effect()
    }

    // Snackbar copy is intentionally short and user-facing; the matching
    // analytics / log lines on the failure path live alongside the
    // viewModelScope.launch blocks below.
    internal companion object Copy {
        const val DELETE_SUCCESS = "Deleted"
        const val DELETE_FAILURE = "Couldn't delete — try again."
        const val CREATE_SUCCESS = "List created!"
        const val CREATE_FAILURE = "Couldn't create that list. Please try again."
        const val ADD_SUCCESS = "Added to list!"
        const val ADD_FAILURE = "Couldn't add to that list. Please try again."
    }

    private val _uiState = MutableStateFlow(CollectionsUiState())
    val uiState: StateFlow<CollectionsUiState> = _uiState.asStateFlow()

    private val _effects = Channel<Effect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    init {
        loadCollections()
    }

    fun loadCollections() {
        if (sessionManager.authState.value != AuthState.Authenticated) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            when (val result = repository.fetchCollections()) {
                is ApiResult.Success -> {
                    _uiState.update {
                        it.copy(isLoading = false, collections = result.data)
                    }
                }
                is ApiResult.Failure -> {
                    _uiState.update {
                        it.copy(isLoading = false, errorMessage = com.pacedream.common.util.UserFacingErrorMapper.map(result.error, "We couldn't load your lists. Please try again."))
                    }
                }
            }
        }
    }

    fun createCollection(name: String, description: String?, isPublic: Boolean) {
        viewModelScope.launch {
            if (sessionManager.authState.value != AuthState.Authenticated) {
                _effects.send(Effect.ShowAuthRequired)
                return@launch
            }

            _uiState.update { it.copy(isCreating = true) }
            val request = CreateCollectionRequest(name, description, isPublic)
            when (val result = repository.createCollection(request)) {
                is ApiResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isCreating = false,
                            collections = listOf(result.data) + it.collections
                        )
                    }
                    _effects.send(Effect.ShowToast(CREATE_SUCCESS))
                }
                is ApiResult.Failure -> {
                    _uiState.update { it.copy(isCreating = false) }
                    Timber.w("Collection create failed: ${result.error}")
                    _effects.send(Effect.ShowToast(CREATE_FAILURE))
                }
            }
        }
    }

    /**
     * Delete a collection after the user has confirmed in the screen-level
     * AlertDialog. Failure is *not* swallowed: it is logged via Timber and
     * surfaced through the [Effect.ShowToast] channel so the screen can
     * raise a Snackbar.
     *
     * NOTE on Undo: the backend has no restore endpoint
     * (`DELETE /v1/collections/{id}` is destructive; `POST /v1/collections`
     * only creates an empty list with a fresh id and would not restore the
     * saved items). Until that lands, we deliberately surface a plain
     * "Deleted" toast without an Undo action.
     */
    fun deleteCollection(collectionId: String) {
        viewModelScope.launch {
            when (val result = repository.deleteCollection(collectionId)) {
                is ApiResult.Success -> {
                    _uiState.update {
                        it.copy(
                            collections = it.collections.filter { c -> c.id != collectionId }
                        )
                    }
                    _effects.send(Effect.ShowToast(DELETE_SUCCESS))
                }
                is ApiResult.Failure -> {
                    Timber.w("Collection delete failed: ${result.error}")
                    _effects.send(Effect.ShowToast(DELETE_FAILURE))
                }
            }
        }
    }

    fun addToCollection(collectionId: String, listingId: String) {
        viewModelScope.launch {
            if (sessionManager.authState.value != AuthState.Authenticated) {
                _effects.send(Effect.ShowAuthRequired)
                return@launch
            }

            val request = AddToCollectionRequest(collectionId, listingId)
            when (val result = repository.addToCollection(request)) {
                is ApiResult.Success -> {
                    _effects.send(Effect.ShowToast(ADD_SUCCESS))
                    loadCollections() // Refresh
                }
                is ApiResult.Failure -> {
                    Timber.w("Add-to-collection failed: ${result.error}")
                    _effects.send(Effect.ShowToast(ADD_FAILURE))
                }
            }
        }
    }

    fun isAuthenticated(): Boolean = sessionManager.authState.value == AuthState.Authenticated
}

data class CollectionsUiState(
    val isLoading: Boolean = false,
    val isCreating: Boolean = false,
    val collections: List<UserCollection> = emptyList(),
    val errorMessage: String? = null
)
