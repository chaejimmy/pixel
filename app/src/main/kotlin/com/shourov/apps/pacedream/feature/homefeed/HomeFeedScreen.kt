package com.shourov.apps.pacedream.feature.homefeed

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import com.pacedream.common.icon.PaceDreamIcons
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import com.pacedream.common.composables.components.InlineErrorBanner
import com.pacedream.common.composables.components.PaceDreamSectionHeader
import com.pacedream.common.composables.shimmerEffect
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
                        subtitle = state.headerSubtitle,
                        onSearchClick = { TabRouter.switchTo(DashboardDestination.SEARCH) },
                        onGetToKnowClick = { /* TODO: Navigate to about/intro screen */ }
                    )
                }

                state.globalErrorMessage?.let { globalErr ->
                    item {
                        Spacer(modifier = Modifier.height(PaceDreamSpacing.MD))
                        InlineErrorBanner(
                            message = globalErr,
                            onAction = { viewModel.refresh() },
                            actionText = "Retry",
                            modifier = Modifier.padding(horizontal = PaceDreamSpacing.LG)
                        )
                    }
                }

                state.sections.forEach { section ->
                    item {
                        Spacer(modifier = Modifier.height(PaceDreamSpacing.XL))
                        PaceDreamSectionHeader(
                            title = section.key.displayTitle,
                            onViewAllClick = { onSeeAll(section.key) },
                            modifier = Modifier.padding(horizontal = PaceDreamSpacing.LG)
                        )
                        Spacer(modifier = Modifier.height(PaceDreamSpacing.MD))

                        section.errorMessage?.takeIf { !section.isLoading }?.let { err ->
                            InlineErrorBanner(
                                message = err,
                                onAction = { viewModel.refresh() },
                                actionText = "Retry",
                                modifier = Modifier.padding(horizontal = PaceDreamSpacing.LG)
                            )
                            Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))
                        }

                        AnimatedContent(
                            targetState = SectionContentState(
                                isLoading = section.isLoading,
                                isEmpty = section.items.isEmpty(),
                            ),
                            transitionSpec = {
                                fadeIn(tween(200)) togetherWith fadeOut(tween(200))
                            },
                            label = "section_content"
                        ) { contentState ->
                            when {
                                contentState.isLoading -> SkeletonRow()
                                contentState.isEmpty -> EmptyInline(
                                    message = "Nothing here yet. Pull to refresh to try again.",
                                    onRefresh = { viewModel.refresh() }
                                )
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

private data class SectionContentState(
    val isLoading: Boolean,
    val isEmpty: Boolean,
)

@Composable
private fun Header(
    title: String,
    subtitle: String,
    onSearchClick: () -> Unit,
    onGetToKnowClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .clip(RoundedCornerShape(bottomStart = PaceDreamRadius.XL, bottomEnd = PaceDreamRadius.XL))
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        PaceDreamColors.Primary,
                        PaceDreamColors.Primary.copy(alpha = 0.75f)
                    )
                )
            )
            .padding(
                start = PaceDreamSpacing.LG,
                end = PaceDreamSpacing.LG,
                top = PaceDreamSpacing.LG,
                bottom = PaceDreamSpacing.XL
            )
    ) {
        Column {
            Text(
                text = title,
                style = PaceDreamTypography.Title1,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(PaceDreamSpacing.XS))
            Text(
                text = subtitle,
                style = PaceDreamTypography.Body,
                color = Color.White.copy(alpha = 0.9f)
            )
            Spacer(modifier = Modifier.height(PaceDreamSpacing.LG))

            OutlinedButton(
                onClick = onGetToKnowClick,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = Color.White.copy(alpha = 0.15f),
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(PaceDreamRadius.MD)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(PaceDreamSpacing.SM)
                ) {
                    Icon(
                        imageVector = PaceDreamIcons.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Get to know PaceDream",
                        style = PaceDreamTypography.Callout,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(PaceDreamSpacing.MD))
            HomeSearchPill(onClick = onSearchClick)
        }
    }
}

@Composable
private fun HomeSearchPill(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(PaceDreamRadius.LG),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.95f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .padding(horizontal = PaceDreamSpacing.MD),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(PaceDreamSpacing.SM)
        ) {
            Icon(
                imageVector = PaceDreamIcons.Search,
                contentDescription = "Search",
                tint = PaceDreamColors.Primary,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = "Where to?",
                style = PaceDreamTypography.Body,
                color = PaceDreamColors.TextSecondary,
                modifier = Modifier.weight(1f)
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
                    model = item.imageUrl?.takeIf { it.isNotBlank() },
                    contentDescription = item.title.ifBlank { "Listing" },
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                if (item.imageUrl.isNullOrBlank()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(PaceDreamColors.Border.copy(alpha = 0.25f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = PaceDreamIcons.Search,
                            contentDescription = null,
                            tint = PaceDreamColors.TextSecondary
                        )
                    }
                }
                IconButton(
                    onClick = onFavorite,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(PaceDreamSpacing.SM)
                        .clip(RoundedCornerShape(PaceDreamRadius.MD))
                        .background(Color.Black.copy(alpha = 0.25f))
                ) {
                    AnimatedContent(
                        targetState = isFavorited,
                        transitionSpec = {
                            (fadeIn(tween(200)) + scaleIn(initialScale = 0.85f, animationSpec = tween(200))) togetherWith
                                (fadeOut(tween(200)) + scaleOut(targetScale = 0.9f, animationSpec = tween(200)))
                        },
                        label = "favorite_toggle"
                    ) { favored ->
                        Icon(
                            imageVector = if (favored) PaceDreamIcons.Favorite else PaceDreamIcons.FavoriteBorder,
                            contentDescription = if (favored) "Remove from favorites" else "Save to favorites",
                            tint = if (favored) PaceDreamColors.Error else Color.White
                        )
                    }
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
                    text = item.title.ifBlank { "Listing" },
                    style = PaceDreamTypography.Title3,
                    color = PaceDreamColors.TextPrimary,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2
                )
                Spacer(modifier = Modifier.height(PaceDreamSpacing.XS))
                item.location?.takeIf { it.isNotBlank() }?.let {
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
                            .shimmerEffect()
                    )
                    Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.7f)
                            .height(16.dp)
                            .background(PaceDreamColors.Border.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                            .shimmerEffect()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.45f)
                            .height(12.dp)
                            .background(PaceDreamColors.Border.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                            .shimmerEffect()
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyInline(
    message: String,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = PaceDreamSpacing.LG)
            .then(modifier),
        colors = CardDefaults.cardColors(containerColor = PaceDreamColors.Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(PaceDreamRadius.MD)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(PaceDreamSpacing.MD),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(PaceDreamSpacing.SM)
        ) {
            Icon(PaceDreamIcons.Search, contentDescription = null, tint = PaceDreamColors.TextSecondary)
            Text(
                message,
                style = PaceDreamTypography.Body,
                color = PaceDreamColors.TextSecondary,
                modifier = Modifier.weight(1f)
            )
            OutlinedButton(
                onClick = onRefresh,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = PaceDreamColors.Primary)
            ) {
                Text("Refresh")
            }
        }
    }
}

