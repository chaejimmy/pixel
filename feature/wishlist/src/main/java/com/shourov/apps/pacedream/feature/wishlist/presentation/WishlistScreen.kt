package com.shourov.apps.pacedream.feature.wishlist.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import com.pacedream.common.icon.PaceDreamIcons
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
import androidx.compose.runtime.remember
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
import com.pacedream.common.composables.theme.PaceDreamColors
import com.pacedream.common.composables.theme.PaceDreamRadius
import com.pacedream.common.composables.theme.PaceDreamSpacing
import com.pacedream.common.composables.theme.PaceDreamTypography
import com.shourov.apps.pacedream.feature.wishlist.model.WishlistEvent
import com.shourov.apps.pacedream.feature.wishlist.model.WishlistFilter
import com.shourov.apps.pacedream.feature.wishlist.model.WishlistItem
import com.shourov.apps.pacedream.feature.wishlist.model.WishlistNavigation
import com.shourov.apps.pacedream.feature.wishlist.model.WishlistUiState
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WishlistScreen(
    viewModel: WishlistViewModel = hiltViewModel(),
    onNavigateToTimeBasedDetail: (String) -> Unit,
    onNavigateToGearDetail: (String) -> Unit,
    onShowAuthSheet: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    
    // Handle navigation events
    LaunchedEffect(Unit) {
        viewModel.navigation.collectLatest { navigation ->
            when (navigation) {
                is WishlistNavigation.ToTimeBasedDetail -> onNavigateToTimeBasedDetail(navigation.itemId)
                is WishlistNavigation.ToHourlyGearDetail -> onNavigateToGearDetail(navigation.gearId)
                WishlistNavigation.ShowAuthSheet -> onShowAuthSheet()
            }
        }
    }
    
    // Handle toast messages
    LaunchedEffect(Unit) {
        viewModel.toastMessage.collectLatest { message ->
            snackbarHostState.showSnackbar(message)
        }
    }
    
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Favorites",
                        style = PaceDreamTypography.Title2
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = PaceDreamColors.Background
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = PaceDreamColors.Background
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (val state = uiState) {
                is WishlistUiState.Loading -> LoadingState()
                is WishlistUiState.Success -> SuccessState(
                    state = state,
                    onEvent = viewModel::onEvent
                )
                is WishlistUiState.Error -> ErrorState(
                    message = state.message,
                    onRetry = { viewModel.onEvent(WishlistEvent.Refresh) }
                )
                is WishlistUiState.Empty -> EmptyState()
                is WishlistUiState.RequiresAuth -> RequiresAuthState(
                    onSignIn = onShowAuthSheet
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SuccessState(
    state: WishlistUiState.Success,
    onEvent: (WishlistEvent) -> Unit
) {
    val isRefreshing = false // Could add refresh state tracking
    
    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = { onEvent(WishlistEvent.Refresh) },
        modifier = Modifier.fillMaxSize()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Filter chips row
            FilterChipsRow(
                selectedFilter = state.selectedFilter,
                onFilterSelected = { onEvent(WishlistEvent.FilterSelected(it)) }
            )
            
            if (state.isEmpty) {
                EmptyFilteredState(filter = state.selectedFilter)
            } else {
                // Wishlist grid
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(PaceDreamSpacing.MD),
                    horizontalArrangement = Arrangement.spacedBy(PaceDreamSpacing.SM),
                    verticalArrangement = Arrangement.spacedBy(PaceDreamSpacing.SM),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(
                        items = state.filteredItems,
                        key = { it.id }
                    ) { item ->
                        WishlistItemCard(
                            item = item,
                            onItemClick = { onEvent(WishlistEvent.ItemClicked(item)) },
                            onRemoveClick = { onEvent(WishlistEvent.RemoveItem(item)) }
                        )
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
        contentPadding = PaddingValues(horizontal = PaceDreamSpacing.MD),
        horizontalArrangement = Arrangement.spacedBy(PaceDreamSpacing.SM),
        modifier = Modifier.fillMaxWidth()
    ) {
        items(WishlistFilter.entries) { filter ->
            FilterChip(
                selected = filter == selectedFilter,
                onClick = { onFilterSelected(filter) },
                label = {
                    Text(
                        text = filter.displayName,
                        style = PaceDreamTypography.Caption
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = PaceDreamColors.Primary,
                    selectedLabelColor = Color.White
                )
            )
        }
    }
    
    Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))
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
        shape = RoundedCornerShape(PaceDreamRadius.MD),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = PaceDreamColors.Card)
    ) {
        Column {
            // Image with remove button overlay
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.2f)
            ) {
                AsyncImage(
                    model = item.imageUrl,
                    contentDescription = item.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(topStart = PaceDreamRadius.MD, topEnd = PaceDreamRadius.MD))
                )
                
                // Gradient overlay at bottom
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.3f))
                            )
                        )
                )
                
                // Heart/remove button
                IconButton(
                    onClick = onRemoveClick,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(PaceDreamSpacing.XS)
                        .size(36.dp)
                        .background(Color.White.copy(alpha = 0.9f), CircleShape)
                ) {
                    Icon(
                        imageVector = PaceDreamIcons.Favorite,
                        contentDescription = "Remove from favorites",
                        tint = PaceDreamColors.Error,
                        modifier = Modifier.size(20.dp)
                    )
                }
                
                // Type badge
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(PaceDreamSpacing.SM)
                        .background(
                            PaceDreamColors.Primary.copy(alpha = 0.9f),
                            RoundedCornerShape(PaceDreamRadius.SM)
                        )
                        .padding(horizontal = PaceDreamSpacing.SM, vertical = PaceDreamSpacing.XS)
                ) {
                    Text(
                        text = item.itemType.displayName,
                        style = PaceDreamTypography.Caption,
                        color = Color.White
                    )
                }
            }
            
            // Content
            Column(
                modifier = Modifier.padding(PaceDreamSpacing.SM)
            ) {
                Text(
                    text = item.title,
                    style = PaceDreamTypography.Headline,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                
                item.location?.let { location ->
                    Spacer(modifier = Modifier.height(PaceDreamSpacing.XS))
                    Text(
                        text = location,
                        style = PaceDreamTypography.Caption,
                        color = PaceDreamColors.TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                
                Spacer(modifier = Modifier.height(PaceDreamSpacing.XS))
                
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Price
                    if (item.price != null) {
                        Text(
                            text = item.formattedPrice,
                            style = PaceDreamTypography.Headline.copy(
                                fontWeight = FontWeight.Bold,
                                color = PaceDreamColors.Primary
                            )
                        )
                    }
                    
                    // Rating
                    item.rating?.let { rating ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = PaceDreamIcons.Star,
                                contentDescription = null,
                                tint = PaceDreamColors.Warning,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = item.formattedRating,
                                style = PaceDreamTypography.Caption
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LoadingState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(color = PaceDreamColors.Primary)
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
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = PaceDreamSpacing.LG)
        ) {
            Icon(
                imageVector = PaceDreamIcons.FavoriteBorder,
                contentDescription = "No favorites",
                tint = PaceDreamColors.TextSecondary,
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(PaceDreamSpacing.LG))
            Text(
                text = "No favorites yet",
                style = PaceDreamTypography.Title3,
                color = PaceDreamColors.TextPrimary,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))
            Text(
                text = "Tap the heart on any listing to save it here for easy access later.",
                style = PaceDreamTypography.Body,
                color = PaceDreamColors.TextSecondary,
                modifier = Modifier.fillMaxWidth(),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

@Composable
private fun EmptyFilteredState(filter: WishlistFilter) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(PaceDreamSpacing.XL),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = PaceDreamIcons.FavoriteBorder,
                contentDescription = null,
                tint = PaceDreamColors.TextSecondary,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(PaceDreamSpacing.MD))
            Text(
                text = "No ${filter.displayName.lowercase()} favorites",
                style = PaceDreamTypography.Headline,
                color = PaceDreamColors.TextSecondary
            )
        }
    }
}

@Composable
private fun ErrorState(
    message: String,
    onRetry: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(PaceDreamSpacing.XL),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = message,
                style = PaceDreamTypography.Body,
                color = PaceDreamColors.Error
            )
            Spacer(modifier = Modifier.height(PaceDreamSpacing.MD))
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(containerColor = PaceDreamColors.Primary)
            ) {
                Text("Retry")
            }
        }
    }
}

@Composable
private fun RequiresAuthState(
    onSignIn: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(PaceDreamSpacing.XL),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = PaceDreamSpacing.LG)
        ) {
            Icon(
                imageVector = PaceDreamIcons.Lock,
                contentDescription = null,
                tint = PaceDreamColors.TextSecondary,
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(PaceDreamSpacing.LG))
            Text(
                text = "Sign in to view favorites",
                style = PaceDreamTypography.Title3,
                color = PaceDreamColors.TextPrimary,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))
            Text(
                text = "Save your favorite spaces and access them anywhere",
                style = PaceDreamTypography.Body,
                color = PaceDreamColors.TextSecondary,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(modifier = Modifier.height(PaceDreamSpacing.XL))
            Button(
                onClick = onSignIn,
                colors = ButtonDefaults.buttonColors(containerColor = PaceDreamColors.Primary),
                shape = RoundedCornerShape(PaceDreamRadius.MD),
                modifier = Modifier.fillMaxWidth(0.6f)
            ) {
                Text("Sign In", style = PaceDreamTypography.Headline)
            }
        }
    }
}


