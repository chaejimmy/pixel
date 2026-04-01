package com.pacedream.app.feature.wishlist

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Image
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import com.pacedream.common.icon.PaceDreamIcons
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pacedream.common.composables.theme.PaceDreamColors
import com.pacedream.common.composables.theme.PaceDreamTypography
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage

/**
 * WishlistScreen - Favorites tab with optimistic remove
 * 
 * iOS Parity:
 * - If logged out, show locked state and trigger auth modal
 * - Filter chips for filtering by type
 * - Optimistic remove: remove item immediately, restore on failure or unexpected liked=true
 * - Toast feedback on actions
 * - Routing based on item type for Book Now
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WishlistScreen(
    viewModel: WishlistViewModel = hiltViewModel(),
    onItemClick: (String, String) -> Unit,
    onLoginRequired: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    
    // Handle toast messages
    LaunchedEffect(Unit) {
        viewModel.toastMessages.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }
    
    // Handle login required
    LaunchedEffect(uiState.requiresAuth) {
        if (uiState.requiresAuth) {
            onLoginRequired()
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Favorites",
                        style = PaceDreamTypography.Title1,
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PaceDreamColors.Background)
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                uiState.showLockedState -> {
                    PaceDreamLockedState(
                        title = "Sign in to view your favorites",
                        description = "Save your favorite spaces, items, and services to access them anytime.",
                        onActionClick = onLoginRequired,
                        actionText = "Sign In / Create Account",
                        icon = PaceDreamIcons.Shield,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                
                uiState.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = PaceDreamColors.Primary)
                    }
                }
                
                uiState.error != null -> {
                    PaceDreamErrorState(
                        title = "Something went wrong",
                        description = uiState.error ?: "An unexpected error occurred",
                        onRetryClick = { viewModel.refresh() },
                        modifier = Modifier.fillMaxSize()
                    )
                }
                
                else -> {
                    PullToRefreshBox(
                        isRefreshing = uiState.isRefreshing,
                        onRefresh = { viewModel.refresh() },
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Column(modifier = Modifier.fillMaxSize()) {
                            // Filter chips
                            FilterChipsRow(
                                selectedFilter = uiState.selectedFilter,
                                onFilterSelected = { viewModel.setFilter(it) }
                            )
                            
                            if (uiState.filteredItems.isEmpty()) {
                                PaceDreamEmptyState(
                                    title = "No favorites yet",
                                    description = "Start exploring and save your favorite spaces, items, and services",
                                    icon = PaceDreamIcons.Favorite,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f)
                                )
                            } else {
                                LazyVerticalGrid(
                                    columns = GridCells.Fixed(2),
                                    contentPadding = PaddingValues(16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    items(
                                        items = uiState.filteredItems,
                                        key = { it.id }
                                    ) { item ->
                                        WishlistItemCard(
                                            item = item,
                                            onItemClick = { onItemClick(item.listingId ?: item.id, item.type) },
                                            onRemoveClick = { viewModel.removeItem(item) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterChipsRow(
    selectedFilter: WishlistFilter,
    onFilterSelected: (WishlistFilter) -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(WishlistFilter.entries) { filter ->
            FilterChip(
                selected = selectedFilter == filter,
                onClick = { onFilterSelected(filter) },
                label = { Text(filter.displayName) }
            )
        }
    }
}

@Composable
private fun WishlistItemCard(
    item: WishlistItem,
    onItemClick: () -> Unit,
    onRemoveClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onItemClick),
        shape = RoundedCornerShape(PaceDreamRadius.LG),
        colors = CardDefaults.cardColors(containerColor = PaceDreamColors.Background),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 0.5.dp,
                    color = PaceDreamColors.Border.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(PaceDreamRadius.LG)
                )
        ) {
            Column {
                if (!item.imageUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = item.imageUrl,
                        contentDescription = item.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(topStart = PaceDreamRadius.LG, topEnd = PaceDreamRadius.LG))
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(topStart = PaceDreamRadius.LG, topEnd = PaceDreamRadius.LG))
                            .background(PaceDreamColors.Gray100),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = PaceDreamIcons.Image,
                            contentDescription = "No image",
                            tint = PaceDreamColors.TextTertiary,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
                
                Column(
                    modifier = Modifier.padding(PaceDreamSpacing.MD),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = item.title,
                        style = PaceDreamTypography.Subheadline.copy(fontWeight = FontWeight.SemiBold),
                        color = PaceDreamColors.TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    item.location?.let { location ->
                        Text(
                            text = location,
                            style = PaceDreamTypography.Caption,
                            color = PaceDreamColors.TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.height(PaceDreamSpacing.XS))

                    item.price?.let { price ->
                        Text(
                            text = price,
                            style = PaceDreamTypography.Headline,
                            color = PaceDreamColors.Primary
                        )
                    }
                }
            }
            
            // Remove button (iOS Parity: top-right heart)
            IconButton(
                onClick = onRemoveClick,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(PaceDreamSpacing.XS)
            ) {
                Icon(
                    imageVector = PaceDreamIcons.Favorite,
                    contentDescription = "Remove from favorites",
                    tint = PaceDreamColors.Error,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}



