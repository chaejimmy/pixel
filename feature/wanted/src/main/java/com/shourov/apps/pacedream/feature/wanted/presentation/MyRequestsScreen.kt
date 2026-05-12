package com.shourov.apps.pacedream.feature.wanted.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shourov.apps.pacedream.feature.wanted.model.RequestsListUiState
import com.shourov.apps.pacedream.feature.wanted.presentation.components.RequestCard

/**
 * "Mine" tab content (guest mode): the requester's posted requests.
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

    PullToRefreshBox(
        isRefreshing = refreshing,
        onRefresh = { viewModel.refresh() },
        modifier = modifier.fillMaxSize(),
    ) {
        when (val s = state) {
            RequestsListUiState.Loading -> CenteredFill { CircularProgressIndicator() }
            is RequestsListUiState.Error -> CenteredFill {
                MineError(s.message, onRetry = { viewModel.load() })
            }
            is RequestsListUiState.Content -> if (s.requests.isEmpty()) {
                CenteredFill {
                    Text(
                        text = "You haven't posted any requests yet.",
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
                    items(items = s.requests, key = { it.id }) { request ->
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
