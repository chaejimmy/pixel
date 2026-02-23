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
                        it.copy(isLoading = false, errorMessage = result.error.message)
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
                    _effects.send(Effect.ShowToast("List created!"))
                }
                is ApiResult.Failure -> {
                    _uiState.update { it.copy(isCreating = false) }
                    _effects.send(Effect.ShowToast(result.error.message))
                }
            }
        }
    }

    fun deleteCollection(collectionId: String) {
        viewModelScope.launch {
            when (val result = repository.deleteCollection(collectionId)) {
                is ApiResult.Success -> {
                    _uiState.update {
                        it.copy(
                            collections = it.collections.filter { c -> c.id != collectionId }
                        )
                    }
                    _effects.send(Effect.ShowToast("List deleted"))
                }
                is ApiResult.Failure -> {
                    _effects.send(Effect.ShowToast(result.error.message))
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
                    _effects.send(Effect.ShowToast("Added to list!"))
                    loadCollections() // Refresh
                }
                is ApiResult.Failure -> {
                    _effects.send(Effect.ShowToast(result.error.message))
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
