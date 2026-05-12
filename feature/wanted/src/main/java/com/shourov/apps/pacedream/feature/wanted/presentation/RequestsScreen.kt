@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.shourov.apps.pacedream.feature.wanted.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shourov.apps.pacedream.feature.wanted.model.RequestsListUiState
import com.shourov.apps.pacedream.feature.wanted.model.RequestsTab
import com.shourov.apps.pacedream.feature.wanted.presentation.components.RequestCard

/**
 * Top-level Wanted entry point.
 *
 * Two tabs: **Browse** (the public feed every signed-in user can scroll)
 * and **Mine** (the requester's own posts) / **My offers** (the
 * provider's submitted offers). The Mine label is bound to [isHostMode]
 * so the same screen serves both roles.
 *
 * Tab selection survives process death via the
 * [RequestsTabsViewModel]'s SavedStateHandle.
 */
@Composable
fun RequestsScreen(
    onRequestClick: (String) -> Unit,
    onCreateClick: () -> Unit,
    isHostMode: Boolean = false,
    /**
     * Tab to land on the first time the screen composes. Used by the
     * post-success "Track my requests" CTA so the user always arrives on
     * Mine even when the previously-saved tab was Browse. Subsequent
     * compositions ignore this value — the ViewModel owns the source of
     * truth for tab selection across process death.
     */
    initialTab: RequestsTab? = null,
    tabsViewModel: RequestsTabsViewModel = hiltViewModel(),
) {
    val selectedTab by tabsViewModel.selectedTab.collectAsStateWithLifecycle()

    // Apply the one-shot landing tab only once per ViewModel instance.
    LaunchedEffect(initialTab, tabsViewModel) {
        if (initialTab != null) tabsViewModel.selectTab(initialTab)
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(title = { Text("Requests") })
                RequestsTabs(
                    selected = selectedTab,
                    mineLabel = if (isHostMode) "My offers" else "Mine",
                    onTabSelected = tabsViewModel::selectTab,
                )
            }
        },
        floatingActionButton = {
            if (selectedTab == RequestsTab.Browse) {
                ExtendedFloatingActionButton(
                    onClick = onCreateClick,
                    icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                    text = { Text("Post a request") },
                )
            }
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            when (selectedTab) {
                RequestsTab.Browse -> BrowseTab(onRequestClick = onRequestClick)
                RequestsTab.Mine -> if (isHostMode) {
                    MyOffersScreen(onViewRequest = onRequestClick)
                } else {
                    MyRequestsScreen(onRequestClick = onRequestClick)
                }
            }
        }
    }
}

@Composable
private fun RequestsTabs(
    selected: RequestsTab,
    mineLabel: String,
    onTabSelected: (RequestsTab) -> Unit,
    mineViewModel: MyRequestsViewModel = hiltViewModel(),
) {
    val hasUnreadOffers by mineViewModel.hasUnreadOffers.collectAsStateWithLifecycle()
    // Clear the dot as soon as the user is on Mine.
    LaunchedEffect(selected) {
        if (selected == RequestsTab.Mine) mineViewModel.markOffersSeen()
    }
    TabRow(selectedTabIndex = if (selected == RequestsTab.Browse) 0 else 1) {
        Tab(
            selected = selected == RequestsTab.Browse,
            onClick = { onTabSelected(RequestsTab.Browse) },
            text = { Text("Browse") },
        )
        Tab(
            selected = selected == RequestsTab.Mine,
            onClick = { onTabSelected(RequestsTab.Mine) },
            text = {
                if (hasUnreadOffers && selected != RequestsTab.Mine) {
                    BadgedBox(badge = { Badge(modifier = Modifier.size(8.dp)) }) {
                        Text(mineLabel)
                    }
                } else {
                    Text(mineLabel)
                }
            },
        )
    }
}

@Composable
private fun BrowseTab(
    onRequestClick: (String) -> Unit,
    viewModel: RequestsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val refreshing by viewModel.refreshing.collectAsStateWithLifecycle()

    PullToRefreshBox(
        isRefreshing = refreshing,
        onRefresh = { viewModel.refresh() },
        modifier = Modifier.fillMaxSize(),
    ) {
        RequestsContent(
            state = state,
            onRequestClick = onRequestClick,
            onRetry = { viewModel.load() },
        )
    }
}

@Composable
private fun RequestsContent(
    state: RequestsListUiState,
    onRequestClick: (String) -> Unit,
    onRetry: () -> Unit,
) {
    when (state) {
        RequestsListUiState.Loading -> CenteredBox {
            CircularProgressIndicator()
        }
        is RequestsListUiState.Error -> CenteredBox {
            ErrorMessage(state.message, onRetry)
        }
        is RequestsListUiState.Content -> {
            if (state.requests.isEmpty()) {
                CenteredBox {
                    Text(
                        text = "No requests yet — be the first to post one.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 12.dp,
                        bottom = 96.dp,
                    ),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(items = state.requests, key = { it.id }) { request ->
                        RequestCard(
                            request = request,
                            onClick = { onRequestClick(request.id) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CenteredBox(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) { content() }
}

@Composable
private fun ErrorMessage(message: String, onRetry: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center,
        )
        TextButton(onClick = onRetry) {
            Text("Retry")
        }
    }
}

