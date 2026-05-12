package com.shourov.apps.pacedream.feature.wanted.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.shourov.apps.pacedream.feature.wanted.model.RequestsTab
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

/**
 * Owns the selected Browse/Mine tab. Persists through process death via
 * [SavedStateHandle] so the user lands on the same tab on relaunch.
 */
@HiltViewModel
class RequestsTabsViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val _selectedTab = MutableStateFlow(
        RequestsTab.fromKey(savedStateHandle[KEY_SELECTED_TAB])
    )
    val selectedTab: StateFlow<RequestsTab> = _selectedTab.asStateFlow()

    fun selectTab(tab: RequestsTab) {
        _selectedTab.value = tab
        savedStateHandle[KEY_SELECTED_TAB] = tab.key
    }

    /**
     * Used by the navigation host to land the user on Mine after they
     * finish posting a request, even if Browse was the last-selected tab.
     */
    fun selectMine() = selectTab(RequestsTab.Mine)

    companion object {
        private const val KEY_SELECTED_TAB = "wanted.requests.selected_tab"
    }
}
