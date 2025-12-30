package com.shourov.apps.pacedream.feature.homefeed

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.pacedream.common.composables.components.PaceDreamSectionHeader
import com.pacedream.common.composables.components.PaceDreamSearchBar
import com.pacedream.common.composables.theme.PaceDreamColors
import com.pacedream.common.composables.theme.PaceDreamRadius
import com.pacedream.common.composables.theme.PaceDreamSpacing
import com.pacedream.common.composables.theme.PaceDreamTypography
import com.shourov.apps.pacedream.core.network.api.ApiResult
import com.shourov.apps.pacedream.core.network.auth.AuthState
import com.shourov.apps.pacedream.listing.ListingPreview
import com.shourov.apps.pacedream.listing.ListingPreviewStore
import com.shourov.apps.pacedream.navigation.TabRouter
import com.shourov.apps.pacedream.navigation.DashboardDestination
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeFeedScreen(
    onListingClick: (String) -> Unit,
    onSeeAll: (HomeSectionKey) -> Unit,
    onShowAuthSheet: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: HomeFeedViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val authState by viewModel.authState.collectAsState()
    val favoriteIds by viewModel.favoriteIds.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    fun onFavorite(listingId: String) {
        if (authState == AuthState.Unauthenticated) {
            onShowAuthSheet()
            return
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

    PullToRefreshBox(
        isRefreshing = state.isRefreshing,
        onRefresh = { viewModel.refresh() },
        modifier = modifier.fillMaxSize()
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(PaceDreamColors.Background),
                contentPadding = PaddingValues(bottom = PaceDreamSpacing.XXXL)
            ) {
                item {
                    Header(
                        title = state.headerTitle,
                        onSearchClick = { TabRouter.switchTo(DashboardDestination.SEARCH) }
                    )
                }

                state.sections.forEach { section ->
                    item {
                        Spacer(modifier = Modifier.height(PaceDreamSpacing.LG))
                        PaceDreamSectionHeader(
                            title = section.key.displayTitle,
                            onViewAllClick = { onSeeAll(section.key) }
                        )
                        Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))

                        section.errorMessage?.takeIf { !section.isLoading }?.let { err ->
                            ErrorInline(err)
                            Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))
                        }

                        when {
                            section.isLoading -> SkeletonRow()
                            section.items.isEmpty() -> EmptyInline("Pull to refresh to try again.")
                            else -> CardsRow(
                                section.items,
                                onListingClick,
                                favoriteIds = favoriteIds,
                                onFavorite = { onFavorite(it.id) }
                            )
                        }
                    }
                }
            }

            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(PaceDreamSpacing.LG)
            )
        }
    }
}

@Composable
private fun Header(title: String, onSearchClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        PaceDreamColors.Primary,
                        PaceDreamColors.Primary.copy(alpha = 0.6f)
                    )
                ),
                RoundedCornerShape(bottomStart = PaceDreamRadius.LG, bottomEnd = PaceDreamRadius.LG)
            )
            .padding(PaceDreamSpacing.LG)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Text(
                text = title,
                style = PaceDreamTypography.Title1,
                color = Color.White,
                fontWeight = FontWeight.ExtraBold
            )
            Spacer(modifier = Modifier.height(PaceDreamSpacing.MD))
            PaceDreamSearchBar(
                query = "",
                onQueryChange = { },
                onSearchClick = onSearchClick,
                onFilterClick = { },
                placeholder = "Where to?",
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun CardsRow(
    items: List<HomeCard>,
    onClick: (String) -> Unit,
    favoriteIds: Set<String>,
    onFavorite: (HomeCard) -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = PaceDreamSpacing.LG),
        horizontalArrangement = Arrangement.spacedBy(PaceDreamSpacing.SM)
    ) {
        items(items, key = { it.id }) { item ->
            ListingCard(
                item = item,
                onClick = { onClick(item.id) },
                isFavorited = favoriteIds.contains(item.id),
                onFavorite = { onFavorite(item) }
            )
        }
    }
}

@Composable
private fun ListingCard(
    item: HomeCard,
    isFavorited: Boolean,
    onClick: () -> Unit,
    onFavorite: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(280.dp)
            .height(320.dp)
            .clickable(onClick = {
                ListingPreviewStore.put(
                    ListingPreview(
                        id = item.id,
                        title = item.title,
                        location = item.location,
                        imageUrl = item.imageUrl,
                        priceText = item.priceText,
                        rating = item.rating
                    )
                )
                onClick()
            }),
        colors = CardDefaults.cardColors(containerColor = PaceDreamColors.Card),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        shape = RoundedCornerShape(PaceDreamRadius.LG)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
            ) {
                AsyncImage(
                    model = item.imageUrl,
                    contentDescription = item.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                IconButton(
                    onClick = onFavorite,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(PaceDreamSpacing.SM)
                        .clip(RoundedCornerShape(PaceDreamRadius.MD))
                        .background(Color.Black.copy(alpha = 0.25f))
                ) {
                    Icon(
                        imageVector = if (isFavorited) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = if (isFavorited) "Remove from favorites" else "Save to favorites",
                        tint = if (isFavorited) PaceDreamColors.Error else Color.White
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.35f))
                            )
                        )
                )
                item.priceText?.takeIf { it.isNotBlank() }?.let { price ->
                    Card(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(PaceDreamSpacing.SM),
                        colors = CardDefaults.cardColors(containerColor = PaceDreamColors.Primary),
                        shape = RoundedCornerShape(PaceDreamRadius.SM)
                    ) {
                        Text(
                            text = price,
                            style = PaceDreamTypography.Callout,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(
                                horizontal = PaceDreamSpacing.SM,
                                vertical = PaceDreamSpacing.XS
                            )
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(PaceDreamSpacing.MD)
            ) {
                Text(
                    text = item.title,
                    style = PaceDreamTypography.Title3,
                    color = PaceDreamColors.TextPrimary,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2
                )
                Spacer(modifier = Modifier.height(PaceDreamSpacing.XS))
                item.location?.let {
                    Text(
                        text = it,
                        style = PaceDreamTypography.Callout,
                        color = PaceDreamColors.TextSecondary,
                        maxLines = 1
                    )
                }
                Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))
                item.rating?.let { r ->
                    Text(
                        text = "★ ${String.format("%.1f", r)}",
                        style = PaceDreamTypography.Callout,
                        color = PaceDreamColors.TextSecondary
                    )
                }
            }
        }
    }
}

@Composable
private fun SkeletonRow() {
    LazyRow(
        contentPadding = PaddingValues(horizontal = PaceDreamSpacing.LG),
        horizontalArrangement = Arrangement.spacedBy(PaceDreamSpacing.SM)
    ) {
        items(6) { _ ->
            Card(
                modifier = Modifier
                    .width(280.dp)
                    .height(320.dp),
                colors = CardDefaults.cardColors(containerColor = PaceDreamColors.Card)
            ) {
                Column(modifier = Modifier.padding(PaceDreamSpacing.MD)) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clip(RoundedCornerShape(PaceDreamRadius.LG))
                            .background(PaceDreamColors.Border.copy(alpha = 0.3f))
                    )
                    Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.7f)
                            .height(16.dp)
                            .background(PaceDreamColors.Border.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.45f)
                            .height(12.dp)
                            .background(PaceDreamColors.Border.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                    )
                }
            }
        }
    }
}

@Composable
private fun ErrorInline(message: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = PaceDreamSpacing.LG),
        colors = CardDefaults.cardColors(containerColor = PaceDreamColors.Error.copy(alpha = 0.08f))
    ) {
        Text(
            text = message,
            style = PaceDreamTypography.Body,
            color = PaceDreamColors.Error,
            modifier = Modifier.padding(PaceDreamSpacing.MD)
        )
    }
}

@Composable
private fun EmptyInline(message: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = PaceDreamSpacing.LG),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(PaceDreamSpacing.SM)
    ) {
        Icon(Icons.Default.Search, contentDescription = null, tint = PaceDreamColors.TextSecondary)
        Text(message, style = PaceDreamTypography.Body, color = PaceDreamColors.TextSecondary)
    }
}

