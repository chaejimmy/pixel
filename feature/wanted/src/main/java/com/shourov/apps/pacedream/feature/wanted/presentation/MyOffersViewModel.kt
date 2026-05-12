package com.shourov.apps.pacedream.feature.wanted.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shourov.apps.pacedream.feature.wanted.data.WantedRepository
import com.shourov.apps.pacedream.feature.wanted.model.MyOffersUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * Backs the "My offers" tab in host mode — the provider's view of every
 * offer they've submitted, grouped by request with a status pill.
 */
@HiltViewModel
class MyOffersViewModel @Inject constructor(
    private val repository: WantedRepository,
) : ViewModel() {

    private val _state = MutableStateFlow<MyOffersUiState>(MyOffersUiState.Loading)
    val state: StateFlow<MyOffersUiState> = _state.asStateFlow()

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
            _state.value = MyOffersUiState.Loading
            fetch()
        }
    }

    private suspend fun fetch() {
        repository.getMyOffers()
            .onSuccess { items -> _state.value = MyOffersUiState.Content(items) }
            .onFailure { e ->
                Timber.e(e, "Failed to load my offers")
                _state.value = MyOffersUiState.Error(
                    e.message ?: "Couldn't load your offers"
                )
            }
    }
}
