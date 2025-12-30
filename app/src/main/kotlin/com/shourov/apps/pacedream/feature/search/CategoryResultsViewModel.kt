package com.shourov.apps.pacedream.feature.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shourov.apps.pacedream.core.network.api.ApiResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CategoryResultsViewModel @Inject constructor(
    private val repo: SearchRepository
) : ViewModel() {

    private val _state = MutableStateFlow(CategoryResultsState())
    val state: StateFlow<CategoryResultsState> = _state.asStateFlow()

    fun start(category: String) {
        // Only start once per category
        if (_state.value.category == category && _state.value.started) return
        _state.value = CategoryResultsState(category = category, started = true)
        refresh()
    }

    fun refresh() {
        val category = _state.value.category
        if (category.isBlank()) return

        viewModelScope.launch {
            _state.update { it.copy(isRefreshing = true, errorMessage = null, page1 = 1, hasMore = true, items = emptyList()) }
            loadMoreInternal(reset = true)
            _state.update { it.copy(isRefreshing = false) }
        }
    }

    fun loadMoreIfNeeded() {
        val s = _state.value
        if (s.isLoadingMore || !s.hasMore || s.category.isBlank()) return
        viewModelScope.launch { loadMoreInternal(reset = false) }
    }

    private suspend fun loadMoreInternal(reset: Boolean) {
        val current = _state.value
        val nextPage = if (reset) 1 else current.page1 + 1
        _state.update { it.copy(isLoading = reset, isLoadingMore = !reset, errorMessage = null) }

        val res = repo.categoryResults(
            page1 = nextPage,
            limit = current.limit,
            shareType = current.shareType,
            category = current.category,
            city = current.city,
            sort = current.sort
        )

        when (res) {
            is ApiResult.Success -> {
                _state.update { s ->
                    val merged = if (reset) res.data.items else (s.items + res.data.items)
                    s.copy(
                        items = merged,
                        page1 = nextPage,
                        hasMore = res.data.hasMore,
                        isLoading = false,
                        isLoadingMore = false,
                        errorMessage = null
                    )
                }
            }
            is ApiResult.Failure -> {
                _state.update { s ->
                    val hasPrior = s.items.isNotEmpty()
                    s.copy(
                        isLoading = false,
                        isLoadingMore = false,
                        hasMore = false,
                        errorMessage = if (hasPrior) null else (res.error.message ?: "Failed to load"),
                    )
                }
            }
        }
    }
}

data class CategoryResultsState(
    val category: String = "",
    val started: Boolean = false,
    val shareType: String? = null,
    val city: String? = null,
    val sort: String? = null,
    val limit: Int = 24,
    val page1: Int = 1,
    val items: List<SearchResultItem> = emptyList(),
    val hasMore: Boolean = true,
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val isLoadingMore: Boolean = false,
    val errorMessage: String? = null
)

