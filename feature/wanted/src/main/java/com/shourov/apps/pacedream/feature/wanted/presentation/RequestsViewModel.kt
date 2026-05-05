package com.shourov.apps.pacedream.feature.wanted.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shourov.apps.pacedream.feature.wanted.data.WantedRepository
import com.shourov.apps.pacedream.feature.wanted.model.RequestsListUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class RequestsViewModel @Inject constructor(
    private val repository: WantedRepository,
) : ViewModel() {

    private val _state = MutableStateFlow<RequestsListUiState>(RequestsListUiState.Loading)
    val state: StateFlow<RequestsListUiState> = _state.asStateFlow()

    private val _refreshing = MutableStateFlow(false)
    val refreshing: StateFlow<Boolean> = _refreshing.asStateFlow()

    init {
        load()
    }

    fun refresh() {
        viewModelScope.launch {
            _refreshing.value = true
            fetch()
            _refreshing.value = false
        }
    }

    fun load() {
        viewModelScope.launch {
            _state.value = RequestsListUiState.Loading
            fetch()
        }
    }

    private suspend fun fetch() {
        repository.getRequests()
            .onSuccess { items -> _state.value = RequestsListUiState.Content(items) }
            .onFailure { e ->
                Timber.e(e, "Failed to load requests")
                _state.value = RequestsListUiState.Error(
                    e.message ?: "Couldn't load requests"
                )
            }
    }
}
