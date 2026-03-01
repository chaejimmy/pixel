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
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import com.pacedream.common.icon.PaceDreamIcons
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.pacedream.common.composables.components.InlineErrorBanner
import com.pacedream.common.composables.components.PaceDreamSectionHeader
import com.pacedream.common.composables.shimmerEffect
import com.pacedream.common.composables.theme.PaceDreamAnimationDuration
import com.pacedream.common.composables.theme.PaceDreamButtonHeight
import com.pacedream.common.composables.theme.PaceDreamColors
import com.pacedream.common.composables.theme.PaceDreamEasing
import com.pacedream.common.composables.theme.PaceDreamRadius
import com.pacedream.common.composables.theme.PaceDreamSpacing
import com.pacedream.common.composables.theme.PaceDreamTypography
import com.pacedream.common.composables.theme.paceDreamDisplayFontFamily
import com.pacedream.common.composables.theme.paceDreamFontFamily
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
    val state by viewModel.state.collectAsStateWithLifecycle()
    val authState by viewModel.authState.collectAsStateWithLifecycle()
    val favoriteIds by viewModel.favoriteIds.collectAsStateWithLifecycle()
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

    var selectedCategoryFilter by remember { mutableStateOf("All") }

    PullToRefreshBox(
        isRefreshing = state.isRefreshing,
        onRefresh = { viewModel.refresh() },
        modifier = modifier.fillMaxSize()
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White),
                contentPadding = PaddingValues(bottom = PaceDreamSpacing.XXXL)
            ) {
                // iOS-style clean white header with "Discover" title and search bar
                item {
                    DiscoverHeader(
                        onSearchClick = { TabRouter.switchTo(DashboardDestination.SEARCH) },
                        onFilterClick = { /* TODO: Open filters */ }
                    )
                }

                // iOS-style horizontal category filter tabs with icons
                item {
                    CategoryFilterTabs(
                        selectedCategory = selectedCategoryFilter,
                        onCategorySelected = { selectedCategoryFilter = it },
                        modifier = Modifier.padding(top = 4.dp)
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
                                fadeIn(tween(PaceDreamAnimationDuration.FAST, easing = PaceDreamEasing.EaseInOut)) togetherWith fadeOut(tween(PaceDreamAnimationDuration.FAST, easing = PaceDreamEasing.EaseInOut))
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

// ─────────────────────────────────────────────────────────────────────────────
// iOS-style Discover Header with clean white background
// Matches iOS HomeView header pattern: Greeting + Search Bar
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun DiscoverHeader(
    onSearchClick: () -> Unit,
    onFilterClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .statusBarsPadding()
            .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 16.dp)
    ) {
        // Greeting row matching iOS layout
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Discover",
                    style = PaceDreamTypography.LargeTitle.copy(
                        fontFamily = paceDreamDisplayFontFamily,
                        letterSpacing = (-0.5).sp
                    ),
                    color = Color(0xFF1A1A1A)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Book, share, or split anything.",
                    style = PaceDreamTypography.Subheadline.copy(fontFamily = paceDreamFontFamily),
                    color = PaceDreamColors.Gray500
                )
            }
            Surface(
                modifier = Modifier.size(PaceDreamButtonHeight.MD),
                shape = CircleShape,
                color = PaceDreamColors.Gray50
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = PaceDreamIcons.Notifications,
                        contentDescription = "Notifications",
                        tint = Color(0xFF1A1A1A),
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // iOS-style search bar with shadow
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(
                    elevation = 6.dp,
                    shape = RoundedCornerShape(PaceDreamRadius.LG),
                    ambientColor = Color.Black.copy(alpha = 0.06f),
                    spotColor = Color.Black.copy(alpha = 0.08f)
                )
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onSearchClick
                ),
            shape = RoundedCornerShape(PaceDreamRadius.LG),
            color = Color.White,
            tonalElevation = 0.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            PaceDreamColors.Primary.copy(alpha = 0.08f),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = PaceDreamIcons.Search,
                        contentDescription = "Search",
                        tint = PaceDreamColors.Primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Where to?",
                        style = PaceDreamTypography.Headline.copy(
                            fontFamily = paceDreamFontFamily,
                            fontSize = 15.sp
                        ),
                        color = Color(0xFF1A1A1A)
                    )
                    Text(
                        text = "Anywhere \u00B7 Any time \u00B7 Any type",
                        style = PaceDreamTypography.Caption.copy(fontFamily = paceDreamFontFamily),
                        color = PaceDreamColors.Gray400
                    )
                }
                Surface(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onFilterClick),
                    shape = CircleShape,
                    color = PaceDreamColors.Gray50
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = PaceDreamIcons.Tune,
                            contentDescription = "Filters",
                            tint = Color(0xFF1A1A1A),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// iOS-style Category Filter Tabs (horizontal scrollable row with icons)
// Matches iOS category filter pattern from HomeScreen
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun CategoryFilterTabs(
    selectedCategory: String,
    onCategorySelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val categories = listOf(
        Triple("All", PaceDreamIcons.AppsOutlined, PaceDreamIcons.Apps),
        Triple("Entire Home", PaceDreamIcons.HomeOutlined, PaceDreamIcons.Home),
        Triple("Private Room", PaceDreamIcons.MeetingRoomOutlined, PaceDreamIcons.MeetingRoom),
        Triple("Restroom", PaceDreamIcons.WcOutlined, PaceDreamIcons.Wc),
        Triple("Nap Pod", PaceDreamIcons.BedOutlined, PaceDreamIcons.Bed),
        Triple("Meeting Room", PaceDreamIcons.BusinessOutlined, PaceDreamIcons.Business),
        Triple("Workspace", PaceDreamIcons.LaptopOutlined, PaceDreamIcons.Laptop),
        Triple("EV Parking", PaceDreamIcons.ElectricCarOutlined, PaceDreamIcons.ElectricCar),
        Triple("Study Room", PaceDreamIcons.SchoolOutlined, PaceDreamIcons.School),
        Triple("Short Stay", PaceDreamIcons.HotelOutlined, PaceDreamIcons.Hotel),
        Triple("Apartment", PaceDreamIcons.ApartmentOutlined, PaceDreamIcons.Apartment),
        Triple("Parking", PaceDreamIcons.LocalParkingOutlined, PaceDreamIcons.LocalParking),
        Triple("Luxury Room", PaceDreamIcons.StarOutlined, PaceDreamIcons.Star),
        Triple("Storage", PaceDreamIcons.StorageOutlined, PaceDreamIcons.Storage)
    )

    Column(modifier = modifier) {
        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(categories) { (name, outlinedIcon, filledIcon) ->
                val isSelected = selectedCategory == name
                CategoryTab(
                    name = name,
                    icon = if (isSelected) filledIcon else outlinedIcon,
                    isSelected = isSelected,
                    onClick = { onCategorySelected(name) }
                )
            }
        }
        HorizontalDivider(
            thickness = 0.5.dp,
            color = PaceDreamColors.Gray100
        )
    }
}

@Composable
private fun CategoryTab(
    name: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 10.dp, vertical = 12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = name,
            tint = if (isSelected) PaceDreamColors.Primary else PaceDreamColors.Gray400,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = name,
            style = PaceDreamTypography.Caption2.copy(
                fontFamily = paceDreamFontFamily,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
            ),
            color = if (isSelected) PaceDreamColors.Primary else PaceDreamColors.Gray500,
            maxLines = 1
        )
        Spacer(modifier = Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .width(32.dp)
                .height(2.5.dp)
                .background(
                    color = if (isSelected) PaceDreamColors.Primary else Color.Transparent,
                    shape = RoundedCornerShape(2.dp)
                )
        )
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
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
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
                            (fadeIn(tween(PaceDreamAnimationDuration.FAST, easing = PaceDreamEasing.EaseInOut)) + scaleIn(initialScale = 0.85f, animationSpec = tween(PaceDreamAnimationDuration.FAST, easing = PaceDreamEasing.EaseInOut))) togetherWith
                                (fadeOut(tween(PaceDreamAnimationDuration.FAST, easing = PaceDreamEasing.EaseInOut)) + scaleOut(targetScale = 0.9f, animationSpec = tween(PaceDreamAnimationDuration.FAST, easing = PaceDreamEasing.EaseInOut)))
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

