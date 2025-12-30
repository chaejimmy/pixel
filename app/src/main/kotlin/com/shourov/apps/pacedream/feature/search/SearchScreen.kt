package com.shourov.apps.pacedream.feature.search

import androidx.compose.animation.Crossfade
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.pacedream.common.composables.components.PaceDreamEmptyState
import com.pacedream.common.composables.components.PaceDreamErrorState
import com.pacedream.common.composables.components.PaceDreamSearchBar
import com.pacedream.common.composables.shimmerEffect
import com.pacedream.common.composables.theme.PaceDreamColors
import com.pacedream.common.composables.theme.PaceDreamRadius
import com.pacedream.common.composables.theme.PaceDreamSpacing
import com.pacedream.common.composables.theme.PaceDreamTypography
import com.shourov.apps.pacedream.core.network.api.ApiResult
import com.shourov.apps.pacedream.core.network.auth.AuthState
import com.shourov.apps.pacedream.listing.ListingPreview
import com.shourov.apps.pacedream.listing.ListingPreviewStore
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    onBackClick: () -> Unit,
    onListingClick: (String) -> Unit,
    initialQuery: String? = null,
    onShowAuthSheet: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: SearchViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val authState by viewModel.authState.collectAsState()
    val favoriteIds by viewModel.favoriteIds.collectAsState()
    var mapMode by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(initialQuery) {
        val q = initialQuery?.trim().orEmpty()
        if (q.isNotBlank() && viewModel.uiState.value.query.isBlank()) {
            viewModel.onQueryChanged(q)
            viewModel.submitSearch()
        }
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Search", style = PaceDreamTypography.Title2, fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = { mapMode = !mapMode }) {
                        Icon(Icons.Default.Map, contentDescription = "Map toggle")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PaceDreamColors.Background)
            )
        },
        containerColor = PaceDreamColors.Background
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = state.phase == SearchPhase.Loading && state.items.isNotEmpty(),
            onRefresh = { viewModel.refresh() },
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Search bar always visible (iOS parity)
                PaceDreamSearchBar(
                    query = state.query,
                    onQueryChange = { viewModel.onQueryChanged(it) },
                    onSearchClick = { viewModel.submitSearch() },
                    onFilterClick = { /* phase 2 */ },
                    placeholder = "Where to?",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = PaceDreamSpacing.LG, vertical = PaceDreamSpacing.MD)
                )

                // Autocomplete suggestions (simple list)
                if (state.suggestions.isNotEmpty() && state.query.length >= 2 && state.phase == SearchPhase.Idle) {
                    SuggestionsList(
                        suggestions = state.suggestions,
                        onClick = { suggestion ->
                            viewModel.onQueryChanged(suggestion.value)
                            viewModel.submitSearch()
                        }
                    )
                    return@PullToRefreshBox
                }

                // Smoothly crossfade between list UI and map placeholder
                Crossfade(
                    targetState = mapMode,
                    label = "search_map_mode"
                ) { isMap ->
                    if (isMap) {
                        MapPlaceholder()
                    } else {
                        // Filters row (UI scaffold; can be wired later)
                        FiltersRow()

                        when (state.phase) {
                            SearchPhase.Idle -> IdleState()
                            SearchPhase.Loading -> SearchSkeleton()
                            SearchPhase.Error -> ErrorState(
                                message = state.errorMessage ?: "Search failed",
                                onRetry = { viewModel.submitSearch() }
                            )
                            SearchPhase.Empty -> EmptyState()
                            SearchPhase.Success, SearchPhase.LoadingMore -> ResultsList(
                                items = state.items,
                                isLoadingMore = state.phase == SearchPhase.LoadingMore,
                                hasMore = state.hasMore,
                                onLoadMore = { viewModel.loadMoreIfNeeded() },
                                onItemClick = onListingClick,
                                favoriteIds = favoriteIds,
                                onFavoriteClick = { listingId ->
                                    if (authState == AuthState.Unauthenticated) {
                                        onShowAuthSheet()
                                        return@ResultsList
                                    }
                                    scope.launch {
                                        val wasFavorited = favoriteIds.contains(listingId)
                                        when (val res = viewModel.toggleFavorite(listingId)) {
                                            is ApiResult.Success -> snackbarHostState.showSnackbar(if (wasFavorited) "Removed from Favorites" else "Saved to Favorites")
                                            is ApiResult.Failure -> {
                                                if (res.error is com.shourov.apps.pacedream.core.network.api.ApiError.Unauthorized) {
                                                    onShowAuthSheet()
                                                } else {
                                                    snackbarHostState.showSnackbar(res.error.message ?: "Failed to save")
                                                }
                                            }
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SuggestionsList(
    suggestions: List<AutocompleteSuggestion>,
    onClick: (AutocompleteSuggestion) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = PaceDreamSpacing.LG, vertical = PaceDreamSpacing.SM)
    ) {
        items(suggestions, key = { it.value }) { s ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onClick(s) },
                colors = CardDefaults.cardColors(containerColor = PaceDreamColors.Card),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Row(
                    modifier = Modifier.padding(PaceDreamSpacing.MD),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Search, contentDescription = null, tint = PaceDreamColors.TextSecondary)
                    Spacer(modifier = Modifier.width(PaceDreamSpacing.SM))
                    Text(s.value, style = PaceDreamTypography.Body, color = PaceDreamColors.TextPrimary)
                }
            }
            Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))
        }
    }
}

@Composable
private fun FiltersRow() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = PaceDreamSpacing.LG),
        horizontalArrangement = Arrangement.spacedBy(PaceDreamSpacing.SM)
    ) {
        FilterChip(
            selected = false,
            onClick = { /* TODO */ },
            label = { Text("Sort", style = PaceDreamTypography.Caption) },
            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = PaceDreamColors.Primary)
        )
        FilterChip(
            selected = false,
            onClick = { /* TODO */ },
            label = { Text("Filters", style = PaceDreamTypography.Caption) }
        )
    }
    Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))
}

@Composable
private fun IdleState() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(PaceDreamSpacing.XL),
        contentAlignment = Alignment.Center
    ) {
        PaceDreamEmptyState(
            title = "Start exploring",
            description = "Search for a city, neighborhood, or listing.",
            icon = Icons.Default.Search,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun EmptyState() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(PaceDreamSpacing.XL),
        contentAlignment = Alignment.Center
    ) {
        PaceDreamEmptyState(
            title = "No results",
            description = "Try a different search or pull to refresh.",
            icon = Icons.Default.Search,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun ErrorState(message: String, onRetry: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(PaceDreamSpacing.XL),
        contentAlignment = Alignment.Center
    ) {
        PaceDreamErrorState(
            title = "Search failed",
            description = message,
            onRetryClick = onRetry,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun SearchSkeleton() {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = PaceDreamSpacing.LG, vertical = PaceDreamSpacing.SM),
        verticalArrangement = Arrangement.spacedBy(PaceDreamSpacing.SM)
    ) {
        items(8) { _ ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = PaceDreamColors.Card)
            ) {
                Row(modifier = Modifier.padding(PaceDreamSpacing.MD)) {
                    Box(
                        modifier = Modifier
                            .size(96.dp)
                            .clip(RoundedCornerShape(PaceDreamRadius.MD))
                            .background(PaceDreamColors.Border.copy(alpha = 0.35f))
                            .shimmerEffect()
                    )
                    Spacer(modifier = Modifier.width(PaceDreamSpacing.MD))
                    Column(modifier = Modifier.weight(1f)) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.7f)
                                .height(16.dp)
                                .background(PaceDreamColors.Border.copy(alpha = 0.35f), RoundedCornerShape(6.dp))
                                .shimmerEffect()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.5f)
                                .height(12.dp)
                                .background(PaceDreamColors.Border.copy(alpha = 0.25f), RoundedCornerShape(6.dp))
                                .shimmerEffect()
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.35f)
                                .height(12.dp)
                                .background(PaceDreamColors.Border.copy(alpha = 0.25f), RoundedCornerShape(6.dp))
                                .shimmerEffect()
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ResultsList(
    items: List<SearchResultItem>,
    isLoadingMore: Boolean,
    hasMore: Boolean,
    onLoadMore: () -> Unit,
    onItemClick: (String) -> Unit,
    favoriteIds: Set<String>,
    onFavoriteClick: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = PaceDreamSpacing.LG, vertical = PaceDreamSpacing.SM),
        verticalArrangement = Arrangement.spacedBy(PaceDreamSpacing.SM)
    ) {
        items(items, key = { it.id }) { item ->
            SearchResultCard(
                item = item,
                onClick = { onItemClick(item.id) },
                isFavorited = favoriteIds.contains(item.id),
                onFavorite = { onFavoriteClick(item.id) },
                modifier = Modifier.animateItemPlacement()
            )
        }

        if (hasMore) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(PaceDreamSpacing.MD),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = PaceDreamColors.Primary, modifier = Modifier.size(22.dp))
                }
                LaunchedEffect(Unit) { onLoadMore() }
            }
        } else if (isLoadingMore) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(PaceDreamSpacing.MD),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = PaceDreamColors.Primary, modifier = Modifier.size(22.dp))
                }
            }
        }
    }
}

@Composable
private fun SearchResultCard(
    item: SearchResultItem,
    isFavorited: Boolean,
    onClick: () -> Unit,
    onFavorite: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = {
                ListingPreviewStore.put(
                    ListingPreview(
                        id = item.id,
                        title = item.title.ifBlank { "Listing" },
                        location = item.location?.takeIf { it.isNotBlank() },
                        imageUrl = item.imageUrl?.takeIf { it.isNotBlank() },
                        priceText = item.priceText?.takeIf { it.isNotBlank() },
                        rating = item.rating
                    )
                )
                onClick()
            }),
        colors = CardDefaults.cardColors(containerColor = PaceDreamColors.Card),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(modifier = Modifier.padding(PaceDreamSpacing.MD)) {
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(RoundedCornerShape(PaceDreamRadius.MD))
            ) {
                if (item.imageUrl.isNullOrBlank()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(PaceDreamColors.Border.copy(alpha = 0.25f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = PaceDreamColors.TextSecondary
                        )
                    }
                } else {
                    AsyncImage(
                        model = item.imageUrl,
                        contentDescription = item.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                IconButton(
                    onClick = onFavorite,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .clip(RoundedCornerShape(PaceDreamRadius.SM))
                        .background(Color.Black.copy(alpha = 0.25f))
                        .size(28.dp)
                ) {
                    AnimatedContent(
                        targetState = isFavorited,
                        transitionSpec = {
                            (fadeIn(tween(120)) + scaleIn(initialScale = 0.85f, animationSpec = tween(180))) togetherWith
                                (fadeOut(tween(90)) + scaleOut(targetScale = 0.9f, animationSpec = tween(90)))
                        },
                        label = "favorite_toggle"
                    ) { favored ->
                        Icon(
                            imageVector = if (favored) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                            contentDescription = if (favored) "Remove from favorites" else "Save to favorites",
                            tint = if (favored) PaceDreamColors.Error else Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp)
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.35f))
                            )
                        )
                )
            }

            Spacer(modifier = Modifier.width(PaceDreamSpacing.MD))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title.ifBlank { "Listing" },
                    style = PaceDreamTypography.Headline,
                    color = PaceDreamColors.TextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                item.location?.takeIf { it.isNotBlank() }?.let {
                    Text(it, style = PaceDreamTypography.Caption, color = PaceDreamColors.TextSecondary, maxLines = 1)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    val price = item.priceText?.takeIf { it.isNotBlank() }
                    Text(
                        text = price ?: "Price unavailable",
                        style = PaceDreamTypography.Caption,
                        color = if (price != null) PaceDreamColors.Primary else PaceDreamColors.TextSecondary,
                        fontWeight = if (price != null) FontWeight.Bold else FontWeight.Normal,
                        modifier = Modifier.weight(1f)
                    )
                    item.rating?.let { r ->
                        Text(
                            text = String.format("%.1f", r),
                            style = PaceDreamTypography.Caption,
                            color = PaceDreamColors.TextSecondary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MapPlaceholder() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(PaceDreamSpacing.XL),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Map view (coming next)",
            style = PaceDreamTypography.Body,
            color = PaceDreamColors.TextSecondary
        )
    }
}

