package com.shourov.apps.pacedream.feature.wanted.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shourov.apps.pacedream.feature.wanted.data.RequestsFiltersStore
import com.shourov.apps.pacedream.feature.wanted.data.WantedRepository
import com.shourov.apps.pacedream.feature.wanted.model.FilterState
import com.shourov.apps.pacedream.feature.wanted.model.RequestSort
import com.shourov.apps.pacedream.feature.wanted.model.RequestsListUiState
import com.shourov.apps.pacedream.feature.wanted.model.WantedRequest
import com.shourov.apps.pacedream.feature.wanted.model.WantedType
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
    private val filtersStore: RequestsFiltersStore,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {

    // The unfiltered list from the API. We hold onto this so changing the
    // filter doesn't require another network round-trip.
    private var allRequests: List<WantedRequest> = emptyList()

    private val _filter = MutableStateFlow(restoreInitialFilter())
    val filter: StateFlow<FilterState> = _filter.asStateFlow()

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

    fun setType(type: WantedType?) {
        // Changing the type invalidates the category — categories are
        // type-scoped via WantedCategoriesByType.
        update(_filter.value.copy(type = type, category = null))
    }

    fun setCategory(category: String?) {
        update(_filter.value.copy(category = category))
    }

    fun setSort(sort: RequestSort) {
        update(_filter.value.copy(sort = sort))
    }

    fun clearFilters() {
        update(FilterState())
    }

    private fun update(next: FilterState) {
        _filter.value = next
        persist(next)
        applyFilter()
    }

    private fun persist(state: FilterState) {
        savedStateHandle[KEY_TYPE] = state.type?.key
        savedStateHandle[KEY_CATEGORY] = state.category
        savedStateHandle[KEY_SORT] = state.sort.key
        filtersStore.save(state)
    }

    private suspend fun fetch() {
        repository.getRequests()
            .onSuccess { items ->
                allRequests = items
                applyFilter()
            }
            .onFailure { e ->
                Timber.e(e, "Failed to load requests")
                _state.value = RequestsListUiState.Error(
                    e.message ?: "Couldn't load requests"
                )
            }
    }

    private fun applyFilter() {
        _state.value = RequestsListUiState.Content(
            filterAndSort(allRequests, _filter.value)
        )
    }

    private fun restoreInitialFilter(): FilterState {
        // SavedStateHandle wins on rotation / back-stack restore; the on-disk
        // store only matters for a fresh session (cold start).
        val typeKey = savedStateHandle.get<String>(KEY_TYPE)
        val categoryKey = savedStateHandle.get<String>(KEY_CATEGORY)
        val sortKey = savedStateHandle.get<String>(KEY_SORT)
        val hasSavedState = sortKey != null || typeKey != null || categoryKey != null
        return if (hasSavedState) {
            FilterState(
                type = typeKey?.let { key ->
                    WantedType.entries.firstOrNull { it.key == key }
                },
                category = categoryKey,
                sort = RequestSort.fromKey(sortKey),
            )
        } else {
            filtersStore.load()
        }
    }

    private companion object {
        const val KEY_TYPE = "filter_type"
        const val KEY_CATEGORY = "filter_category"
        const val KEY_SORT = "filter_sort"

        fun filterAndSort(
            source: List<WantedRequest>,
            filter: FilterState,
        ): List<WantedRequest> {
            val filtered = source.asSequence()
                .filter { request ->
                    filter.type?.let { request.type.equals(it.key, ignoreCase = true) } ?: true
                }
                .filter { request ->
                    filter.category?.let { request.category.equals(it, ignoreCase = true) } ?: true
                }
                .toList()
            return when (filter.sort) {
                RequestSort.Newest -> filtered
                RequestSort.HighestBudget -> filtered.sortedWith(
                    // Nulls last: a missing budget shouldn't pile up at the top
                    // of a "highest budget" sort.
                    compareBy<WantedRequest> { it.budget == null }
                        .thenByDescending { it.budget }
                )
                RequestSort.Nearest -> filtered // TODO: needs geo on WantedRequest
            }
        }
    }
}
