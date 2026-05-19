package com.shourov.apps.pacedream.feature.wanted.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pacedream.common.composables.theme.PaceDreamColors
import com.pacedream.common.composables.theme.PaceDreamRadius
import com.shourov.apps.pacedream.feature.wanted.model.MyRequestsTab
import com.shourov.apps.pacedream.feature.wanted.model.MyRequestsUiState
import com.shourov.apps.pacedream.feature.wanted.model.RequestStatus
import com.shourov.apps.pacedream.feature.wanted.model.WantedRequest
import com.shourov.apps.pacedream.feature.wanted.presentation.components.RequestCard

/**
 * "Mine" tab content (guest mode): the requester's posted requests, split
 * into Active / Expired / Fulfilled / Cancelled sub-tabs.
 *
 * Active hosts the renew / fulfill / cancel actions. Expired keeps history
 * around and surfaces a "Renew" affordance. Fulfilled and Cancelled are
 * read-only.
 *
 * Clears the unread-offer dot the first time the user sees this screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyRequestsScreen(
    onRequestClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MyRequestsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val refreshing by viewModel.refreshing.collectAsStateWithLifecycle()
    val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()

    PullToRefreshBox(
        isRefreshing = refreshing,
        onRefresh = { viewModel.refresh() },
        modifier = modifier.fillMaxSize(),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            val counts = (state as? MyRequestsUiState.Content)?.counts.orEmpty()
            InnerTabRow(
                selected = selectedTab,
                counts = counts,
                onTabSelected = viewModel::selectTab,
            )
            (state as? MyRequestsUiState.Content)?.actionError?.let { msg ->
                ActionErrorBanner(message = msg, onDismiss = viewModel::dismissActionError)
            }
            Box(modifier = Modifier.fillMaxSize()) {
                when (val s = state) {
                    MyRequestsUiState.Loading -> CenteredFill { CircularProgressIndicator() }
                    is MyRequestsUiState.Error -> CenteredFill {
                        MineError(s.message, onRetry = { viewModel.load() })
                    }
                    is MyRequestsUiState.Content -> if (s.visible.isEmpty()) {
                        CenteredFill {
                            EmptyTabMessage(tab = s.selectedTab)
                        }
                    } else {
                        TabRequestList(
                            content = s,
                            onRequestClick = onRequestClick,
                            onRenew = viewModel::renew,
                            onMarkFulfilled = viewModel::markFulfilled,
                            onCancel = viewModel::cancel,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun InnerTabRow(
    selected: MyRequestsTab,
    counts: Map<MyRequestsTab, Int>,
    onTabSelected: (MyRequestsTab) -> Unit,
) {
    val entries = remember { MyRequestsTab.entries }
    ScrollableTabRow(
        selectedTabIndex = entries.indexOf(selected).coerceAtLeast(0),
        edgePadding = 0.dp,
    ) {
        entries.forEach { tab ->
            val count = counts[tab] ?: 0
            val label = remember(tab, count) {
                if (count > 0) "${tab.label} ($count)" else tab.label
            }
            Tab(
                selected = selected == tab,
                onClick = { onTabSelected(tab) },
                text = { Text(label) },
            )
        }
    }
}

@Composable
private fun TabRequestList(
    content: MyRequestsUiState.Content,
    onRequestClick: (String) -> Unit,
    onRenew: (String) -> Unit,
    onMarkFulfilled: (String) -> Unit,
    onCancel: (String) -> Unit,
) {
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
        items(items = content.visible, key = { it.id }) { request ->
            MyRequestRow(
                request = request,
                isPending = content.pendingActionId == request.id,
                onClick = { onRequestClick(request.id) },
                onRenew = { onRenew(request.id) },
                onMarkFulfilled = { onMarkFulfilled(request.id) },
                onCancel = { onCancel(request.id) },
            )
        }
    }
}

@Composable
private fun MyRequestRow(
    request: WantedRequest,
    isPending: Boolean,
    onClick: () -> Unit,
    onRenew: () -> Unit,
    onMarkFulfilled: () -> Unit,
    onCancel: () -> Unit,
) {
    Box(modifier = Modifier.fillMaxWidth()) {
        RequestCard(request = request, onClick = onClick)
        if (isPending) {
            CircularProgressIndicator(
                strokeWidth = 2.dp,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(end = 16.dp, top = 16.dp),
            )
        } else {
            RequestActionsMenu(
                request = request,
                onRenew = onRenew,
                onMarkFulfilled = onMarkFulfilled,
                onCancel = onCancel,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(end = 4.dp, top = 4.dp),
            )
        }
    }
}

@Composable
private fun RequestActionsMenu(
    request: WantedRequest,
    onRenew: () -> Unit,
    onMarkFulfilled: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val actions = remember(request.status) { actionsFor(request.status) }
    if (actions.isEmpty()) return

    var expanded by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        IconButton(onClick = { expanded = true }) {
            Icon(
                imageVector = Icons.Filled.MoreVert,
                contentDescription = "Request actions",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            actions.forEach { action ->
                DropdownMenuItem(
                    text = { Text(action.label) },
                    onClick = {
                        expanded = false
                        when (action) {
                            RowAction.Renew -> onRenew()
                            RowAction.MarkFulfilled -> onMarkFulfilled()
                            RowAction.Cancel -> onCancel()
                        }
                    },
                )
            }
        }
    }
}

private enum class RowAction(val label: String) {
    Renew("Renew Request"),
    MarkFulfilled("Mark as Fulfilled"),
    Cancel("Cancel Request"),
}

private fun actionsFor(status: RequestStatus): List<RowAction> = when (status) {
    RequestStatus.Active -> listOf(RowAction.MarkFulfilled, RowAction.Cancel)
    RequestStatus.Expired -> listOf(RowAction.Renew, RowAction.Cancel)
    RequestStatus.Fulfilled, RequestStatus.Cancelled -> emptyList()
}

@Composable
private fun EmptyTabMessage(tab: MyRequestsTab) {
    val message = remember(tab) {
        when (tab) {
            MyRequestsTab.Active ->
                "You don't have any active requests right now."
            MyRequestsTab.Expired ->
                "Nothing expired yet — your active requests will land here when they age out."
            MyRequestsTab.Fulfilled ->
                "Once you accept an offer and mark a request fulfilled, it'll show up here."
            MyRequestsTab.Cancelled ->
                "Cancelled requests will appear here for your records."
        }
    }
    Text(
        text = message,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun ActionErrorBanner(message: String, onDismiss: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(PaceDreamRadius.MD))
            .background(PaceDreamColors.Error.copy(alpha = 0.12f))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = onDismiss) {
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = "Dismiss",
            )
        }
    }
}

@Composable
private fun CenteredFill(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) { content() }
}

@Composable
private fun MineError(message: String, onRetry: () -> Unit) {
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
        TextButton(onClick = onRetry) { Text("Retry") }
    }
}
