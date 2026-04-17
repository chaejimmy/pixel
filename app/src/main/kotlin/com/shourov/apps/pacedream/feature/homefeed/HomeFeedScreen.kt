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
import coil.request.ImageRequest
import androidx.compose.ui.platform.LocalContext
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
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeFeedScreen(
    onListingClick: (String) -> Unit,
    onSeeAll: (HomeSectionKey) -> Unit,
    onShowAuthSheet: () -> Unit = {},
    onSearchClick: () -> Unit = {},
    onWhereClick: () -> Unit = onSearchClick,
    onWhenClick: () -> Unit = onSearchClick,
    onWhoClick: () -> Unit = onSearchClick,
    onNotificationClick: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: HomeFeedViewModel = hiltViewModel()
) {
    val state by viewModel.filteredState.collectAsStateWithLifecycle()
    val authState by viewModel.authState.collectAsStateWithLifecycle()
    val favoriteIds by viewModel.favoriteIds.collectAsStateWithLifecycle()
    val selectedCategoryFilter by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    fun onFavorite(listingId: String) {
        if (authState == AuthState.Unauthenticated) {
            onShowAuthSheet()
            return
        }
        scope.launch {
            try {
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
            } catch (_: Exception) {
                snackbarHostState.showSnackbar("Failed to save")
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
                // iOS-style clean white header with "Discover" title and search bar
                item(key = "discover_header", contentType = "header") {
                    DiscoverHeader(
                        onSearchClick = onSearchClick,
                        onFilterClick = onSearchClick,
                        onWhereClick = onWhereClick,
                        onWhenClick = onWhenClick,
                        onWhoClick = onWhoClick,
                        onNotificationClick = onNotificationClick
                    )
                }

                // iOS-style horizontal category filter tabs with icons
                item(key = "category_tabs", contentType = "filter") {
                    CategoryFilterTabs(
                        selectedCategory = selectedCategoryFilter,
                        onCategorySelected = { viewModel.selectCategory(it) },
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
                    item(key = "section_header_${section.key.name}") {
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
                                    message = if (section.key == HomeSectionKey.SERVICES)
                                        "No services available yet. Be the first to offer a service."
                                    else
                                        "Nothing here yet. Pull to refresh to try again.",
                                    onRefresh = { viewModel.refresh() }
                                )
                                else -> if (section.key == HomeSectionKey.SERVICES) {
                                    ServicesGrid(
                                        section.items,
                                        onListingClick,
                                        favoriteIds = favoriteIds,
                                        onFavorite = { onFavorite(it.id) }
                                    )
                                } else {
                                    CardsRow(
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
    onFilterClick: () -> Unit,
    onWhereClick: () -> Unit = onSearchClick,
    onWhenClick: () -> Unit = onSearchClick,
    onWhoClick: () -> Unit = onSearchClick,
    onNotificationClick: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(PaceDreamColors.Background)
            .statusBarsPadding()
            .padding(start = PaceDreamSpacing.LG, end = PaceDreamSpacing.LG, top = PaceDreamSpacing.MD, bottom = PaceDreamSpacing.MD)
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
                    color = PaceDreamColors.TextPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Find spaces, items, and services — only for the time you need.",
                    style = PaceDreamTypography.Subheadline.copy(fontFamily = paceDreamFontFamily),
                    color = PaceDreamColors.Gray500
                )
            }
            Surface(
                modifier = Modifier
                    .size(PaceDreamButtonHeight.MD)
                    .clickable(onClick = onNotificationClick),
                shape = CircleShape,
                color = PaceDreamColors.Gray50
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = PaceDreamIcons.Notifications,
                        contentDescription = "Notifications",
                        tint = PaceDreamColors.TextPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Airbnb-style structured search pill: three tappable segments
        // (Where / When / Who).  Tapping the trailing primary circle
        // opens the full search (and also serves as "go to filters").
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(
                    elevation = 6.dp,
                    shape = RoundedCornerShape(PaceDreamRadius.LG),
                    ambientColor = Color.Black.copy(alpha = 0.06f),
                    spotColor = Color.Black.copy(alpha = 0.08f)
                ),
            shape = RoundedCornerShape(PaceDreamRadius.LG),
            color = Color.White,
            tonalElevation = 0.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SearchSegment(
                    label = "Where",
                    value = "Anywhere",
                    onClick = onWhereClick,
                    modifier = Modifier.weight(1.2f)
                )
                SegmentDivider()
                SearchSegment(
                    label = "When",
                    value = "Any time",
                    onClick = onWhenClick,
                    modifier = Modifier.weight(1f)
                )
                SegmentDivider()
                SearchSegment(
                    label = "Who",
                    value = "Add guests",
                    onClick = onWhoClick,
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(4.dp))

                Surface(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onFilterClick),
                    shape = CircleShape,
                    color = PaceDreamColors.Primary
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = PaceDreamIcons.Search,
                            contentDescription = "Search",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Airbnb-style two-line segment inside the hero search pill.  Each
 * segment is an independent click target routed to its own picker.
 */
@Composable
private fun SearchSegment(
    label: String,
    value: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(PaceDreamRadius.MD))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(
            text = label,
            style = PaceDreamTypography.Caption.copy(fontFamily = paceDreamFontFamily),
            fontWeight = FontWeight.SemiBold,
            color = PaceDreamColors.TextPrimary,
            maxLines = 1
        )
        Spacer(modifier = Modifier.height(1.dp))
        Text(
            text = value,
            style = PaceDreamTypography.Caption.copy(
                fontFamily = paceDreamFontFamily,
                fontSize = 12.sp
            ),
            color = PaceDreamColors.Gray500,
            maxLines = 1
        )
    }
}

@Composable
private fun SegmentDivider() {
    Box(
        modifier = Modifier
            .height(28.dp)
            .width(1.dp)
            .background(PaceDreamColors.Gray200)
    )
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
    val categories = remember {
        listOf(
            Triple("All", PaceDreamIcons.AppsOutlined, PaceDreamIcons.Apps),
            Triple("Restroom", PaceDreamIcons.WcOutlined, PaceDreamIcons.Wc),
            Triple("Nap Pod", PaceDreamIcons.BedOutlined, PaceDreamIcons.Bed),
            Triple("Meeting Room", PaceDreamIcons.MeetingRoomOutlined, PaceDreamIcons.MeetingRoom),
            Triple("Gym", PaceDreamIcons.FitnessCenterOutlined, PaceDreamIcons.FitnessCenter),
            Triple("Short Stay", PaceDreamIcons.ScheduleOutlined, PaceDreamIcons.Schedule),
            Triple("WIFI", PaceDreamIcons.WifiOutlined, PaceDreamIcons.Wifi),
            Triple("Parking", PaceDreamIcons.LocalParkingOutlined, PaceDreamIcons.LocalParking),
            Triple("Storage Space", PaceDreamIcons.StorageOutlined, PaceDreamIcons.Storage)
        )
    }

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
private fun ServicesGrid(
    items: List<HomeCard>,
    onClick: (String) -> Unit,
    favoriteIds: Set<String>,
    onFavorite: (HomeCard) -> Unit
) {
    val chunkedItems = items.chunked(2)
    Column(
        modifier = Modifier.padding(horizontal = PaceDreamSpacing.LG),
        verticalArrangement = Arrangement.spacedBy(PaceDreamSpacing.SM)
    ) {
        chunkedItems.forEach { rowItems ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(PaceDreamSpacing.SM),
                modifier = Modifier.fillMaxWidth()
            ) {
                rowItems.forEach { item ->
                    Box(modifier = Modifier.weight(1f)) {
                        ListingCard(
                            item = item,
                            onClick = { onClick(item.id) },
                            isFavorited = favoriteIds.contains(item.id),
                            onFavorite = { onFavorite(item) },
                            modifier = Modifier.fillMaxWidth().height(280.dp)
                        )
                    }
                }
                // Fill empty space if odd number
                if (rowItems.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
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
    onFavorite: () -> Unit,
    modifier: Modifier = Modifier.width(280.dp).height(320.dp)
) {
    Card(
        onClick = {
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
        },
        modifier = modifier,
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
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(item.imageUrl?.takeIf { it.isNotBlank() })
                        .crossfade(200)
                        .size(coil.size.Size(560, 360))
                        .build(),
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
                    style = PaceDreamTypography.Headline,
                    color = PaceDreamColors.TextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(PaceDreamSpacing.XS))
                item.location?.takeIf { it.isNotBlank() }?.let {
                    Text(
                        text = it,
                        style = PaceDreamTypography.Caption,
                        color = PaceDreamColors.TextSecondary,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))
                item.rating?.takeIf { it > 0.0 }?.let { r ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = PaceDreamIcons.Star,
                            contentDescription = null,
                            tint = PaceDreamColors.StarRating,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = String.format("%.1f", r),
                            style = PaceDreamTypography.Caption,
                            color = PaceDreamColors.TextSecondary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
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

