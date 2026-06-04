@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.shourov.apps.pacedream.feature.wanted.presentation

import android.content.res.Configuration
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
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pacedream.common.composables.designsystem.state.EmptyState
import com.pacedream.common.composables.designsystem.state.ListShimmer
import com.pacedream.common.composables.theme.PaceDreamColors
import com.pacedream.common.composables.theme.PaceDreamSpacing
import com.pacedream.common.composables.theme.PaceDreamTheme
import com.pacedream.common.composables.theme.PaceDreamTypography
import com.pacedream.common.icon.PaceDreamIcons
import com.shourov.apps.pacedream.feature.wanted.model.FilterState
import com.shourov.apps.pacedream.feature.wanted.model.RequestSort
import com.shourov.apps.pacedream.feature.wanted.model.RequestsListUiState
import com.shourov.apps.pacedream.feature.wanted.model.RequestsTab
import com.shourov.apps.pacedream.feature.wanted.model.WantedCategoriesByType
import com.shourov.apps.pacedream.feature.wanted.model.WantedCategoryOption
import com.shourov.apps.pacedream.feature.wanted.model.WantedRequest
import com.shourov.apps.pacedream.feature.wanted.model.WantedType
import com.shourov.apps.pacedream.feature.wanted.presentation.components.RequestCard

/**
 * Top-level Wanted entry point.
 *
 * Two tabs: **Browse** (the public feed, with filter + sort) and
 * **Mine** (the requester's own posts) / **My offers** (the provider's
 * submitted offers). The Mine label is bound to [isHostMode] so the
 * same screen serves both roles.
 *
 * Tab selection survives process death via the
 * [RequestsTabsViewModel]'s SavedStateHandle.
 */
@Composable
fun RequestsScreen(
    onRequestClick: (String) -> Unit,
    onCreateClick: () -> Unit,
    onNotifyMeClick: () -> Unit = {},
    /**
     * Pops the back stack when Requests is reached as a pushed
     * destination (deep link from a "you have an offer" notification,
     * or from a detail back-stack). When null — Requests serving as a
     * root tab — no back affordance is rendered.
     */
    onBack: (() -> Unit)? = null,
    isHostMode: Boolean = false,
    /**
     * Tab to land on the first time the screen composes. Used by the
     * post-success "Track my requests" CTA so the user always arrives
     * on Mine even when the previously-saved tab was Browse. Subsequent
     * compositions ignore this value — the ViewModel owns the source
     * of truth for tab selection across process death.
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
                TopAppBar(
                    title = { Text("Requests", style = PaceDreamTypography.Headline) },
                    navigationIcon = {
                        if (onBack != null) {
                            IconButton(onClick = onBack) {
                                Icon(PaceDreamIcons.ArrowBack, contentDescription = "Back")
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = PaceDreamColors.Background,
                    ),
                )
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
                    // Merge icon + label into a single TalkBack node so the FAB
                    // is announced once as "Post a request, button" rather than
                    // walking the icon and text as separate children.
                    modifier = Modifier.semantics(mergeDescendants = true) {},
                    containerColor = PaceDreamColors.Primary,
                    contentColor = PaceDreamColors.OnPrimary,
                    icon = { Icon(PaceDreamIcons.Add, contentDescription = null) },
                    text = { Text("Post a request", style = PaceDreamTypography.Callout) },
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
                RequestsTab.Browse -> BrowseTab(
                    onRequestClick = onRequestClick,
                    onCreateClick = onCreateClick,
                    onNotifyMeClick = onNotifyMeClick,
                    isHostMode = isHostMode,
                )
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
    onCreateClick: () -> Unit,
    onNotifyMeClick: () -> Unit,
    isHostMode: Boolean,
    viewModel: RequestsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val refreshing by viewModel.refreshing.collectAsStateWithLifecycle()
    val filter by viewModel.filter.collectAsStateWithLifecycle()
    val viewModelHostMode by viewModel.isHostMode.collectAsStateWithLifecycle()

    // Mirror the app-level mode into the ViewModel whenever it changes so the
    // role-aware empty state survives configuration changes via SavedStateHandle.
    LaunchedEffect(isHostMode) {
        viewModel.setHostMode(isHostMode)
    }

    Column(modifier = Modifier.fillMaxSize()) {
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
                isHostMode = viewModelHostMode,
                onRequestClick = onRequestClick,
                onRetry = { viewModel.load() },
                onClearFilters = viewModel::clearFilters,
                onCreateClick = onCreateClick,
                onNotifyMeClick = onNotifyMeClick,
            )
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
        color = PaceDreamColors.Background,
        tonalElevation = 1.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = PaceDreamSpacing.SM, bottom = PaceDreamSpacing.SM),
            verticalArrangement = Arrangement.spacedBy(PaceDreamSpacing.SM),
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
            .padding(horizontal = PaceDreamSpacing.MD),
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
                imageVector = PaceDreamIcons.Sort,
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
                        { Icon(PaceDreamIcons.Sort, contentDescription = null) }
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
        contentPadding = PaddingValues(horizontal = PaceDreamSpacing.MD),
        horizontalArrangement = Arrangement.spacedBy(PaceDreamSpacing.SM),
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
        contentPadding = PaddingValues(horizontal = PaceDreamSpacing.MD),
        horizontalArrangement = Arrangement.spacedBy(PaceDreamSpacing.SM),
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
            TextButton(onClick = onClearAll) {
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
                imageVector = PaceDreamIcons.Close,
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
    isHostMode: Boolean,
    onRequestClick: (String) -> Unit,
    onRetry: () -> Unit,
    onClearFilters: () -> Unit,
    onCreateClick: () -> Unit,
    onNotifyMeClick: () -> Unit,
) {
    when (state) {
        RequestsListUiState.Loading -> ListShimmer()
        is RequestsListUiState.Error -> CenteredBox {
            EmptyState(
                title = "Couldn't load requests",
                subtitle = state.message,
                icon = PaceDreamIcons.Warning,
                ctaLabel = "Retry",
                onCta = onRetry,
            )
        }
        is RequestsListUiState.Content -> {
            if (state.requests.isEmpty()) {
                CenteredBox {
                    if (filter.isActive) {
                        EmptyFilteredState(onClearFilters = onClearFilters)
                    } else {
                        RoleAwareEmptyState(
                            isHostMode = isHostMode,
                            onCreateClick = onCreateClick,
                            onNotifyMeClick = onNotifyMeClick,
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = PaceDreamSpacing.MD,
                        end = PaceDreamSpacing.MD,
                        top = PaceDreamSpacing.SM2,
                        // intentional: 96dp clears the bottom nav + the
                        // overhanging "Post a request" FAB; off the 8dp grid.
                        bottom = 96.dp,
                    ),
                    verticalArrangement = Arrangement.spacedBy(PaceDreamSpacing.SM2),
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
private fun RoleAwareEmptyState(
    isHostMode: Boolean,
    onCreateClick: () -> Unit,
    onNotifyMeClick: () -> Unit,
) {
    if (isHostMode) {
        EmptyState(
            title = "No open requests",
            subtitle = "We'll let you know when new requests match your listings.",
            icon = PaceDreamIcons.Inbox,
            ctaLabel = "Notify me",
            onCta = onNotifyMeClick,
        )
    } else {
        EmptyState(
            title = "No requests yet",
            subtitle = "Be the first to post one.",
            icon = PaceDreamIcons.Inbox,
            ctaLabel = "Post a request",
            onCta = onCreateClick,
        )
    }
}

@Composable
private fun EmptyFilteredState(onClearFilters: () -> Unit) {
    EmptyState(
        title = "No matching requests",
        subtitle = "Try adjusting or clearing your filters.",
        icon = PaceDreamIcons.Search,
        ctaLabel = "Clear filters",
        onCta = onClearFilters,
    )
}

@Composable
private fun CenteredBox(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(PaceDreamSpacing.LG),
        contentAlignment = Alignment.Center,
    ) { content() }
}

// ─────────────────────────────────────────────────────────────────────────────
// Previews — light & dark, feeding hand-built state straight into the stateless
// RequestsContent so the IDE pane renders every meaningful branch (Content list,
// Empty, Error) without standing up the Hilt ViewModel. The dark pass confirms
// the surfaces use the migrated tokens.
// ─────────────────────────────────────────────────────────────────────────────

private val SampleRequests = listOf(
    WantedRequest(
        id = "preview-1",
        title = "Need a quiet meeting room for 2 hours",
        description = "Looking for a small room downtown this afternoon.",
        type = "space",
        category = "Meeting room",
        location = "Downtown, San Francisco",
        budget = 40.0,
        imageUrl = null,
    ),
    WantedRequest(
        id = "preview-2",
        title = "Borrow a DSLR camera for the weekend",
        description = "Any Canon or Nikon body with a kit lens works.",
        type = "item",
        category = "Camera",
        location = "Mission, San Francisco",
        budget = null,
        imageUrl = null,
    ),
    WantedRequest(
        id = "preview-3",
        title = "Help moving a couch on Saturday",
        description = "Two-person job, third-floor walk-up.",
        type = "service",
        category = "Moving",
        location = "Oakland, CA",
        budget = 120.0,
        imageUrl = null,
    ),
)

@Composable
private fun RequestsContentPreview(
    state: RequestsListUiState,
    darkTheme: Boolean,
) {
    PaceDreamTheme(darkTheme = darkTheme) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
        ) {
            RequestsContent(
                state = state,
                filter = FilterState(),
                isHostMode = false,
                onRequestClick = {},
                onRetry = {},
                onClearFilters = {},
                onCreateClick = {},
                onNotifyMeClick = {},
            )
        }
    }
}

@Preview(name = "Requests Content Light", showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun RequestsContentLightPreview() {
    RequestsContentPreview(RequestsListUiState.Content(SampleRequests), darkTheme = false)
}

@Preview(
    name = "Requests Content Dark",
    showBackground = true,
    widthDp = 360,
    heightDp = 800,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun RequestsContentDarkPreview() {
    RequestsContentPreview(RequestsListUiState.Content(SampleRequests), darkTheme = true)
}

@Preview(name = "Requests Empty Light", showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun RequestsEmptyLightPreview() {
    RequestsContentPreview(RequestsListUiState.Content(emptyList()), darkTheme = false)
}

@Preview(
    name = "Requests Empty Dark",
    showBackground = true,
    widthDp = 360,
    heightDp = 800,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun RequestsEmptyDarkPreview() {
    RequestsContentPreview(RequestsListUiState.Content(emptyList()), darkTheme = true)
}

@Preview(name = "Requests Error Light", showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun RequestsErrorLightPreview() {
    RequestsContentPreview(
        RequestsListUiState.Error("Couldn't load requests. Check your connection."),
        darkTheme = false,
    )
}

@Preview(
    name = "Requests Error Dark",
    showBackground = true,
    widthDp = 360,
    heightDp = 800,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun RequestsErrorDarkPreview() {
    RequestsContentPreview(
        RequestsListUiState.Error("Couldn't load requests. Check your connection."),
        darkTheme = true,
    )
}
