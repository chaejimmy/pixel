@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.shourov.apps.pacedream.feature.wanted.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shourov.apps.pacedream.feature.wanted.model.FilterState
import com.shourov.apps.pacedream.feature.wanted.model.RequestSort
import com.shourov.apps.pacedream.feature.wanted.model.RequestsListUiState
import com.shourov.apps.pacedream.feature.wanted.model.WantedCategoriesByType
import com.shourov.apps.pacedream.feature.wanted.model.WantedCategoryOption
import com.shourov.apps.pacedream.feature.wanted.model.WantedType
import com.shourov.apps.pacedream.feature.wanted.presentation.components.RequestCard

@Composable
fun RequestsScreen(
    onRequestClick: (String) -> Unit,
    onCreateClick: () -> Unit,
    viewModel: RequestsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val refreshing by viewModel.refreshing.collectAsStateWithLifecycle()
    val filter by viewModel.filter.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Requests") })
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onCreateClick,
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("Post a request") },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            FilterHeader(
                filter = filter,
                onTypeSelected = viewModel::setType,
                onCategorySelected = viewModel::setCategory,
                onSortSelected = viewModel::setSort,
                onClearFilters = viewModel::clearFilters,
            )
            PullToRefreshBox(
                isRefreshing = refreshing,
                onRefresh = { viewModel.refresh() },
                modifier = Modifier.fillMaxSize(),
            ) {
                RequestsContent(
                    state = state,
                    filter = filter,
                    onRequestClick = onRequestClick,
                    onRetry = { viewModel.load() },
                    onClearFilters = viewModel::clearFilters,
                )
            }
        }
    }
}

@Composable
private fun FilterHeader(
    filter: FilterState,
    onTypeSelected: (WantedType?) -> Unit,
    onCategorySelected: (String?) -> Unit,
    onSortSelected: (RequestSort) -> Unit,
    onClearFilters: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, bottom = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            TypeAndSortRow(
                selected = filter.type,
                sort = filter.sort,
                onTypeSelected = onTypeSelected,
                onSortSelected = onSortSelected,
            )
            CategoryChipRow(
                type = filter.type,
                selectedCategory = filter.category,
                onCategorySelected = onCategorySelected,
            )
            if (filter.isActive) {
                ActiveFilterStrip(
                    filter = filter,
                    onTypeCleared = { onTypeSelected(null) },
                    onCategoryCleared = { onCategorySelected(null) },
                    onSortReset = { onSortSelected(RequestSort.Newest) },
                    onClearAll = onClearFilters,
                )
            }
            HorizontalDivider()
        }
    }
}

@Composable
private fun TypeAndSortRow(
    selected: WantedType?,
    sort: RequestSort,
    onTypeSelected: (WantedType?) -> Unit,
    onSortSelected: (RequestSort) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val entries = WantedType.entries
        // 4 segments: All + one per WantedType.
        SingleChoiceSegmentedButtonRow(modifier = Modifier.weight(1f)) {
            SegmentedButton(
                selected = selected == null,
                onClick = { onTypeSelected(null) },
                shape = SegmentedButtonDefaults.itemShape(index = 0, count = entries.size + 1),
            ) { Text("All") }
            entries.forEachIndexed { i, type ->
                SegmentedButton(
                    selected = selected == type,
                    onClick = { onTypeSelected(type) },
                    shape = SegmentedButtonDefaults.itemShape(
                        index = i + 1,
                        count = entries.size + 1,
                    ),
                ) {
                    // Short labels: "Space" / "Item" / "Service" — derived from
                    // the human label by stripping the leading article.
                    Text(type.label.removePrefix("A ").removePrefix("An "))
                }
            }
        }
        SortMenuButton(current = sort, onSortSelected = onSortSelected)
    }
}

@Composable
private fun SortMenuButton(
    current: RequestSort,
    onSortSelected: (RequestSort) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(
                imageVector = Icons.Filled.Sort,
                contentDescription = "Sort requests",
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            RequestSort.entries.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.label) },
                    onClick = {
                        onSortSelected(option)
                        expanded = false
                    },
                    trailingIcon = if (option == current) {
                        { Icon(Icons.Filled.Sort, contentDescription = null) }
                    } else null,
                )
            }
        }
    }
}

@Composable
private fun CategoryChipRow(
    type: WantedType?,
    selectedCategory: String?,
    onCategorySelected: (String?) -> Unit,
) {
    // Categories are scoped to a type; when "All" is selected there is no
    // useful per-category list to show.
    val categories: List<WantedCategoryOption> =
        type?.let { WantedCategoriesByType[it].orEmpty() } ?: emptyList()
    if (categories.isEmpty()) return

    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        items(categories, key = { it.key }) { category ->
            val selected = category.key == selectedCategory
            FilterChip(
                selected = selected,
                onClick = {
                    onCategorySelected(if (selected) null else category.key)
                },
                label = { Text(category.label) },
                colors = FilterChipDefaults.filterChipColors(),
            )
        }
    }
}

@Composable
private fun ActiveFilterStrip(
    filter: FilterState,
    onTypeCleared: () -> Unit,
    onCategoryCleared: () -> Unit,
    onSortReset: () -> Unit,
    onClearAll: () -> Unit,
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
    ) {
        filter.type?.let { type ->
            item(key = "type") {
                ActiveFilterChip(
                    label = type.label.removePrefix("A ").removePrefix("An "),
                    onClear = onTypeCleared,
                )
            }
        }
        filter.category?.let { categoryKey ->
            val label = filter.type
                ?.let { WantedCategoriesByType[it] }
                ?.firstOrNull { it.key == categoryKey }
                ?.label
                ?: categoryKey
            item(key = "category") {
                ActiveFilterChip(label = label, onClear = onCategoryCleared)
            }
        }
        if (filter.sort != RequestSort.Newest) {
            item(key = "sort") {
                ActiveFilterChip(
                    label = filter.sort.label,
                    onClear = onSortReset,
                )
            }
        }
        item(key = "clear_all") {
            androidx.compose.material3.TextButton(onClick = onClearAll) {
                Text("Clear all")
            }
        }
    }
}

@Composable
private fun ActiveFilterChip(label: String, onClear: () -> Unit) {
    InputChip(
        selected = true,
        onClick = onClear,
        label = { Text(label) },
        trailingIcon = {
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = "Clear $label",
                modifier = Modifier.size(InputChipDefaults.IconSize),
            )
        },
    )
}

@Composable
private fun RequestsContent(
    state: RequestsListUiState,
    filter: FilterState,
    onRequestClick: (String) -> Unit,
    onRetry: () -> Unit,
    onClearFilters: () -> Unit,
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
                    if (filter.isActive) {
                        EmptyFilteredState(onClearFilters = onClearFilters)
                    } else {
                        Text(
                            text = "No requests yet — be the first to post one.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                    }
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
private fun EmptyFilteredState(onClearFilters: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "No matching requests — clear filters",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        androidx.compose.material3.TextButton(onClick = onClearFilters) {
            Text("Clear filters")
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
        androidx.compose.material3.TextButton(onClick = onRetry) {
            Text("Retry")
        }
    }
}
