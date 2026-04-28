package com.pacedream.app.feature.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import com.pacedream.common.icon.PaceDreamIcons
import com.pacedream.common.composables.theme.PaceDreamColors
import com.pacedream.common.composables.theme.PaceDreamRadius
import com.pacedream.common.composables.theme.PaceDreamSpacing
import com.pacedream.common.composables.theme.PaceDreamTypography as DSTypo
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import android.content.res.Configuration
import androidx.compose.ui.tooling.preview.Preview
import com.pacedream.common.composables.animations.animatedCardEntry
import com.pacedream.common.composables.theme.PaceDreamTheme
import com.pacedream.common.composables.theme.paceDreamDisplayFontFamily
import com.pacedream.common.composables.theme.paceDreamFontFamily
import com.shourov.apps.pacedream.designsystem.CategoryColor
import com.shourov.apps.pacedream.designsystem.CategoryColors
import com.shourov.apps.pacedream.designsystem.FavoriteIconButton
import com.shourov.apps.pacedream.designsystem.OnBrandSurface
import com.shourov.apps.pacedream.designsystem.adaptiveShadow
import com.shourov.apps.pacedream.designsystem.badgeOnImageColor
import com.shourov.apps.pacedream.designsystem.scrimOnImage
import com.shourov.apps.pacedream.R

// intentional: 20.dp page gutter is off the 4/8/16/24 scale but is a deliberate
// design decision for Home's edge-to-edge photographic hero layout. Keep it
// exported as a private token so future token sweeps recognise it.
private val HomeHorizontalGutter = 20.dp

object HomeTestTags {
    const val Root = "home_screen_root"
    const val ListingFeed = "home_listing_feed"
    const val SearchBar = "home_search_bar"
    const val NotificationButton = "home_notification_button"
    const val CategoryTabs = "home_category_tabs"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel(),
    onSectionViewAll: (String) -> Unit,
    onListingClick: (HomeListingItem) -> Unit,
    onAboutClick: () -> Unit,
    onSearchClick: () -> Unit = {},
    onCategoryClick: (String) -> Unit = {},
    onCategoryFilterClick: (String) -> Unit = {},
    onShowAuthSheet: () -> Unit = {},
    onNotificationClick: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val selectedCategoryFilter = uiState.selectedCategory

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is HomeViewModel.Effect.ShowAuthRequired -> onShowAuthSheet()
                is HomeViewModel.Effect.ShowToast -> snackbarHostState.showSnackbar(effect.message)
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().testTag(HomeTestTags.Root)) {
    PullToRefreshBox(
        isRefreshing = uiState.isRefreshing,
        onRefresh = { viewModel.refresh() },
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().testTag(HomeTestTags.ListingFeed),
            contentPadding = PaddingValues(bottom = PaceDreamSpacing.LG)
        ) {
            // ── Hero Header with Gradient + Overlapping Search Bar ──
            item {
                HeroHeaderSection(
                    heroImageUrl = uiState.heroImageUrl,
                    onSearchClick = onSearchClick,
                    onFilterClick = onSearchClick,
                    onNotificationClick = onNotificationClick,
                    onAboutClick = onAboutClick,
                )
            }

            // ── Category Filter Tabs ──
            item {
                CategoryFilterTabs(
                    selectedCategory = selectedCategoryFilter,
                    onCategorySelected = { category ->
                        viewModel.selectCategory(category)
                        onCategoryFilterClick(category)
                    },
                    modifier = Modifier.padding(top = PaceDreamSpacing.XS)
                )
            }

            // ── Warning banner ──
            if (uiState.hasErrors) {
                item {
                    WarningBanner(
                        message = "Some content couldn't load. Pull to refresh.",
                        modifier = Modifier.padding(horizontal = HomeHorizontalGutter, vertical = PaceDreamSpacing.SM2)
                    )
                }
            }

            // ── Extended Categories (iOS parity) ──
            item {
                ExtendedCategoriesSection(
                    onCategoryClick = onCategoryClick,
                    modifier = Modifier.padding(top = PaceDreamSpacing.MD)
                )
            }

            // ── Hourly Spaces ──
            if (uiState.filteredHourlySpaces.isNotEmpty() || uiState.isLoadingHourlySpaces) {
                item {
                    SectionSurface {
                        ListingSection(
                            title = "Spaces",
                            subtitle = "Popular nearby",
                            items = uiState.filteredHourlySpaces,
                            isLoading = uiState.isLoadingHourlySpaces,
                            favoriteIds = uiState.favoriteListingIds,
                            onViewAllClick = { onSectionViewAll("hourly-spaces") },
                            onItemClick = onListingClick,
                            onFavoriteClick = { viewModel.toggleFavorite(it) },
                        )
                    }
                }
            }

            // ── Items ──
            if (uiState.filteredRentGear.isNotEmpty() || uiState.isLoadingRentGear) {
                item {
                    SectionSurface {
                        ListingSection(
                            title = "Items",
                            subtitle = "Available now",
                            items = uiState.filteredRentGear,
                            isLoading = uiState.isLoadingRentGear,
                            favoriteIds = uiState.favoriteListingIds,
                            onViewAllClick = { onSectionViewAll("rent-gear") },
                            onItemClick = onListingClick,
                            onFavoriteClick = { viewModel.toggleFavorite(it) },
                        )
                    }
                }
            }

            // ── Services (2-column vertical grid — iOS parity) ──
            if (uiState.filteredSplitStays.isNotEmpty() || uiState.isLoadingSplitStays) {
                item {
                    SectionSurface {
                        ServicesGridSection(
                            title = "Services",
                            subtitle = "Book help when you need it",
                            items = uiState.filteredSplitStays.take(10),
                            isLoading = uiState.isLoadingSplitStays,
                            favoriteIds = uiState.favoriteListingIds,
                            onViewAllClick = { onSectionViewAll("services") },
                            onItemClick = onListingClick,
                            onFavoriteClick = { viewModel.toggleFavorite(it) },
                        )
                    }
                }
            }

            // ── Browse by Type Section (Marketplace taxonomy) ──
            item {
                SectionSurface {
                    BrowseByTypeSection(
                        onTypeTap = { type -> onCategoryClick(type) },
                        onSubcategoryTap = { _, subcategory -> onCategoryClick(subcategory) },
                    )
                }
            }

            // ── Trending Destinations (iOS parity) ──
            item {
                SectionSurface {
                    TrendingDestinationsSection(
                        onDestinationTap = { destination -> onCategoryClick(destination) },
                        onViewAllTap = { onSectionViewAll("destinations") },
                    )
                }
            }

            // ── 3 Steps CTA (iOS parity) ──
            item {
                ThreeStepsCTASection(
                    onGetStarted = { onCategoryClick("create-listing") },
                    modifier = Modifier.padding(top = PaceDreamSpacing.SM)
                )
            }

            // ── Empty state ──
            if (!uiState.isLoading && uiState.isEmpty) {
                item {
                    EmptyState(
                        onRetry = { viewModel.refresh() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(PaceDreamSpacing.XXL)
                    )
                }
            }
        }
    }
    SnackbarHost(
        hostState = snackbarHostState,
        modifier = Modifier.align(Alignment.BottomCenter)
    )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Section Surface — white card on neutral background, consistent vertical rhythm
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SectionSurface(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = PaceDreamSpacing.SM),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Box(modifier = Modifier.padding(vertical = PaceDreamSpacing.MD)) {
            content()
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Hero Header Section (pacedream.com parity: photographic hero with CTA)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun HeroHeaderSection(
    heroImageUrl: String?,
    onSearchClick: () -> Unit,
    onFilterClick: () -> Unit,
    onNotificationClick: () -> Unit,
    onAboutClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(360.dp)
    ) {
        // TODO(product): swap this dark-photographic placeholder for the real
        //  pacedream.com hero image once the asset ships. The design calls for
        //  an AsyncImage of that photo; until then we render a subtle deep-grey
        //  fill so the UI doesn't regress to the old vivid gradient.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(328.dp)
                .background(PaceDreamColors.Gray900)
        ) {
            if (!heroImageUrl.isNullOrBlank()) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(heroImageUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
                // Scrim to keep copy readable over photography. Routes through
                // the design-system helper so the overlay tones down in dark
                // mode rather than crushing the image into a black rectangle.
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(scrimOnImage(0.45f))
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .padding(horizontal = HomeHorizontalGutter, vertical = HomeHorizontalGutter)
            ) {
                // Top row: notification bell (right-aligned; hero copy takes over greeting).
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        modifier = Modifier
                            .size(44.dp)
                            .testTag(HomeTestTags.NotificationButton)
                            .semantics { role = Role.Button }
                            .clickable(onClick = onNotificationClick),
                        shape = CircleShape,
                        color = OnBrandSurface.copy(alpha = 0.20f)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = PaceDreamIcons.Notifications,
                                contentDescription = "Notifications",
                                tint = OnBrandSurface,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                Text(
                    text = stringResource(R.string.home_hero_title),
                    style = DSTypo.Title1.copy(
                        fontFamily = paceDreamDisplayFontFamily,
                        letterSpacing = (-0.5).sp,
                        lineHeight = 34.sp
                    ),
                    color = OnBrandSurface
                )
                Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))
                Text(
                    text = stringResource(R.string.home_hero_subtitle),
                    style = DSTypo.Body.copy(
                        fontFamily = paceDreamFontFamily,
                        lineHeight = 22.sp
                    ),
                    color = OnBrandSurface.copy(alpha = 0.92f)
                )
                Spacer(modifier = Modifier.height(PaceDreamSpacing.MD))

                Button(
                    onClick = onAboutClick,
                    modifier = Modifier
                        .defaultMinSize(minHeight = 50.dp)
                        .semantics { role = Role.Button },
                    shape = RoundedCornerShape(PaceDreamRadius.LG),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PaceDreamColors.Primary,
                        contentColor = OnBrandSurface
                    ),
                    contentPadding = PaddingValues(horizontal = HomeHorizontalGutter, vertical = PaceDreamSpacing.SM2),
                ) {
                    Icon(
                        imageVector = PaceDreamIcons.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(PaceDreamSpacing.SM))
                    Text(
                        text = stringResource(R.string.home_hero_cta),
                        style = DSTypo.Callout.copy(
                            fontFamily = paceDreamFontFamily,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }
                Spacer(modifier = Modifier.height(PaceDreamSpacing.LG))
            }
        }

        // Overlapping Search Bar
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = HomeHorizontalGutter)
                .fillMaxWidth()
                .testTag(HomeTestTags.SearchBar)
                .adaptiveShadow(
                    elevation = 10.dp,
                    shape = RoundedCornerShape(PaceDreamRadius.LG)
                )
                .semantics { role = Role.Button }
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onSearchClick
                ),
            shape = RoundedCornerShape(PaceDreamRadius.LG),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 0.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = PaceDreamSpacing.MD, vertical = PaceDreamSpacing.SM2),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
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
                Spacer(modifier = Modifier.width(PaceDreamSpacing.MD))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Search anywhere",
                        style = DSTypo.Callout.copy(
                            fontFamily = paceDreamFontFamily,
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = PaceDreamColors.TextHeadline
                    )
                    // intentional: 1.dp hairline between title and subcopy; any token would be too loose
                    Spacer(modifier = Modifier.height(1.dp))
                    Text(
                        text = "Spaces \u00B7 Items \u00B7 Services",
                        style = DSTypo.Caption.copy(fontFamily = paceDreamFontFamily),
                        color = PaceDreamColors.Gray500
                    )
                }
                Spacer(modifier = Modifier.width(PaceDreamSpacing.SM))
                Surface(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .semantics { role = Role.Button }
                        .clickable(onClick = onFilterClick),
                    shape = CircleShape,
                    color = PaceDreamColors.Gray100,
                    border = BorderStroke(0.5.dp, PaceDreamColors.Gray200)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = PaceDreamIcons.Tune,
                            contentDescription = "Filters",
                            tint = PaceDreamColors.IconNeutral,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Category Filter Tabs (iOS parity: horizontal scroll with underline indicator)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun CategoryFilterTabs(
    selectedCategory: String,
    onCategorySelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    // Category chips matching iOS defaultWebCategoryChips order
    val categories = listOf(
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

    Column(modifier = modifier.testTag(HomeTestTags.CategoryTabs)) {
        LazyRow(
            contentPadding = PaddingValues(horizontal = PaceDreamSpacing.MD),
            // intentional: tabs own their own internal horizontal padding so we want zero arrangement gap
            horizontalArrangement = Arrangement.spacedBy(0.dp)
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
            color = PaceDreamColors.Gray200
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
    val contentAlpha by animateFloatAsState(
        targetValue = if (isSelected) 1f else 0.55f,
        animationSpec = tween(durationMillis = 150),
        label = "tabAlpha"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .semantics { role = Role.Button }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = PaceDreamSpacing.MD)
            // intentional: 0.dp bottom so the indicator rule underneath sits flush
            .padding(top = PaceDreamSpacing.SM2, bottom = 0.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = name,
            tint = if (isSelected) PaceDreamColors.Primary
                   else PaceDreamColors.TextSecondary, // Gray-500 for better contrast
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))
        Text(
            text = name,
            style = DSTypo.Caption.copy(
                fontFamily = paceDreamFontFamily,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                letterSpacing = if (isSelected) 0.sp else 0.1.sp
            ),
            color = if (isSelected) PaceDreamColors.Primary
                    else PaceDreamColors.TextSecondary,
            maxLines = 1
        )
        Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.5.dp)
                .background(
                    color = if (isSelected) PaceDreamColors.Primary else Color.Transparent,
                    shape = RoundedCornerShape(PaceDreamRadius.XS)
                )
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Extended Categories Section (iOS parity: horizontal chip row with gradient icons)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ExtendedCategoriesSection(
    onCategoryClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        SectionHeader(
            title = "Categories",
            modifier = Modifier.padding(horizontal = HomeHorizontalGutter)
        )
        Spacer(modifier = Modifier.height(PaceDreamSpacing.SM2))
        LazyRow(
            contentPadding = PaddingValues(horizontal = HomeHorizontalGutter),
            horizontalArrangement = Arrangement.spacedBy(PaceDreamSpacing.SM)
        ) {
            items(getCategoryCards()) { category ->
                QuickCategoryChip(
                    category = category,
                    onClick = { onCategoryClick(category.name) }
                )
            }
        }
    }
}

@Composable
private fun QuickCategoryChip(
    category: CategoryCardData,
    onClick: () -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = tween(durationMillis = 100),
        label = "chipScale"
    )

    val tint = category.color.tint
    Surface(
        modifier = Modifier
            .height(48.dp)
            .scale(scale)
            .semantics { role = Role.Button }
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        tryAwaitRelease()
                        isPressed = false
                        onClick()
                    }
                )
            },
        shape = RoundedCornerShape(PaceDreamRadius.Round),
        color = tint.copy(alpha = 0.08f),
        border = BorderStroke(0.5.dp, tint.copy(alpha = 0.12f))
    ) {
        Row(
            modifier = Modifier.padding(start = PaceDreamSpacing.SM, end = PaceDreamSpacing.MD),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(tint.copy(alpha = 0.12f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = category.icon,
                    contentDescription = category.name,
                    tint = tint,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(PaceDreamSpacing.SM))
            Text(
                text = category.name,
                style = DSTypo.Subheadline.copy(
                    fontFamily = paceDreamFontFamily,
                    fontWeight = FontWeight.Medium
                ),
                color = PaceDreamColors.TextHeadline,
                maxLines = 1
            )
        }
    }
}

private data class CategoryCardData(
    val name: String,
    val icon: ImageVector,
    val color: CategoryColor,
)

private fun getCategoryCards(): List<CategoryCardData> {
    return listOf(
        CategoryCardData("Rest Room", PaceDreamIcons.Wc, CategoryColors.Restroom),
        CategoryCardData("Time-Based", PaceDreamIcons.Schedule, CategoryColors.ShortStay),
        CategoryCardData("Parking", PaceDreamIcons.LocalParking, CategoryColors.Parking),
        CategoryCardData("Items", PaceDreamIcons.Build, CategoryColors.Items),
        CategoryCardData("EV Parking", PaceDreamIcons.ElectricCar, CategoryColors.EVParking),
        CategoryCardData("Meeting Rooms", PaceDreamIcons.MeetingRoom, CategoryColors.MeetingRoom),
        CategoryCardData("Workspace", PaceDreamIcons.Laptop, CategoryColors.Workspace),
        CategoryCardData("Storage", PaceDreamIcons.Storage, CategoryColors.StorageSpace),
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// Warning Banner (iOS parity: orange 12% background, rounded, icon + text)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun WarningBanner(
    message: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(PaceDreamRadius.MD),
        color = PaceDreamColors.Warning.copy(alpha = 0.12f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = PaceDreamSpacing.SM2, vertical = PaceDreamSpacing.SM2),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = PaceDreamIcons.Warning,
                contentDescription = null,
                tint = PaceDreamColors.Warning,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(PaceDreamSpacing.SM))
            Text(
                text = message,
                style = DSTypo.Subheadline.copy(
                    fontFamily = paceDreamFontFamily,
                    fontWeight = FontWeight.SemiBold
                ),
                color = PaceDreamColors.OnWarningContainer
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Section Header (iOS parity: title2 + subtitle + "View All" button)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SectionHeader(
    title: String,
    subtitle: String? = null,
    onViewAllClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = DSTypo.Title3.copy(
                    fontFamily = paceDreamDisplayFontFamily,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.2).sp
                ),
                color = PaceDreamColors.TextHeadline,
                modifier = Modifier.weight(1f, fill = false)
            )
            if (onViewAllClick != null) {
                Spacer(modifier = Modifier.width(PaceDreamSpacing.SM))
                TextButton(
                    onClick = onViewAllClick,
                    contentPadding = PaddingValues(horizontal = PaceDreamSpacing.SM, vertical = PaceDreamSpacing.XS)
                ) {
                    Text(
                        text = "View All",
                        style = DSTypo.Caption.copy(
                            fontFamily = paceDreamFontFamily,
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = PaceDreamColors.Primary
                    )
                    Spacer(modifier = Modifier.width(PaceDreamSpacing.XXS))
                    Icon(
                        imageVector = PaceDreamIcons.ChevronRight,
                        contentDescription = null,
                        tint = PaceDreamColors.Primary,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
        if (subtitle != null) {
            Text(
                text = subtitle,
                style = DSTypo.Caption.copy(
                    fontFamily = paceDreamFontFamily,
                    lineHeight = 16.sp
                ),
                color = PaceDreamColors.Gray500
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Listing Section (iOS parity: section header + horizontal card scroll)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ListingSection(
    title: String,
    subtitle: String,
    items: List<HomeListingItem>,
    isLoading: Boolean,
    favoriteIds: Set<String>,
    onViewAllClick: () -> Unit,
    onItemClick: (HomeListingItem) -> Unit,
    onFavoriteClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        SectionHeader(
            title = title,
            subtitle = subtitle,
            onViewAllClick = onViewAllClick,
            modifier = Modifier.padding(horizontal = HomeHorizontalGutter)
        )

        Spacer(modifier = Modifier.height(PaceDreamSpacing.MD))

        if (isLoading) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = HomeHorizontalGutter),
                horizontalArrangement = Arrangement.spacedBy(PaceDreamSpacing.SM2)
            ) {
                items(3) { ShimmerCard(modifier = Modifier.fillParentMaxWidth(0.62f)) }
            }
        } else when {
            // Single item: full-width featured card layout
            items.size == 1 -> {
                val item = items.first()
                FeaturedFullWidthCard(
                    item = item,
                    isFavorite = item.id in favoriteIds,
                    onClick = { onItemClick(item) },
                    onFavoriteClick = { onFavoriteClick(item.id) },
                    modifier = Modifier.padding(horizontal = HomeHorizontalGutter)
                )
            }
            // Two items: side-by-side compact grid
            items.size == 2 -> {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = HomeHorizontalGutter),
                    horizontalArrangement = Arrangement.spacedBy(PaceDreamSpacing.SM2)
                ) {
                    items.forEach { item ->
                        Box(modifier = Modifier.weight(1f)) {
                            GridListingCard(
                                item = item,
                                isFavorite = item.id in favoriteIds,
                                onClick = { onItemClick(item) },
                                onFavoriteClick = { onFavoriteClick(item.id) }
                            )
                        }
                    }
                }
            }
            // 3+ items: horizontal carousel with peek-optimized card width
            else -> {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = HomeHorizontalGutter),
                    horizontalArrangement = Arrangement.spacedBy(PaceDreamSpacing.SM2)
                ) {
                    items(items) { item ->
                        ListingCard(
                            item = item,
                            isFavorite = item.id in favoriteIds,
                            onClick = { onItemClick(item) },
                            onFavoriteClick = { onFavoriteClick(item.id) },
                            modifier = Modifier.fillParentMaxWidth(0.62f),
                        )
                    }
                }
            }
        }
    }
}

/**
 * Full-width featured card for single-item sections.
 * Uses full horizontal space so the section looks intentional, not empty.
 */
@Composable
private fun FeaturedFullWidthCard(
    item: HomeListingItem,
    isFavorite: Boolean = false,
    onClick: () -> Unit,
    onFavoriteClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = tween(durationMillis = 120),
        label = "featuredScale"
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .animatedCardEntry()
            .scale(scale)
            .adaptiveShadow(
                elevation = if (isPressed) 10.dp else 4.dp,
                shape = RoundedCornerShape(PaceDreamRadius.LG),
                pressed = isPressed
            )
            .semantics { role = Role.Button }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        shape = RoundedCornerShape(PaceDreamRadius.LG),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column {
            // Image — 16:9 aspect ratio for featured
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .clip(
                        RoundedCornerShape(
                            topStart = PaceDreamRadius.LG,
                            topEnd = PaceDreamRadius.LG
                        )
                    )
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(item.imageUrl)
                        .crossfade(200)
                        .build(),
                    contentDescription = item.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    scrimOnImage(0.20f)
                                )
                            )
                        )
                )

                // Type badge
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(PaceDreamSpacing.SM2),
                    shape = RoundedCornerShape(PaceDreamRadius.SM),
                    color = badgeOnImageColor()
                ) {
                    Text(
                        text = when (item.type) {
                            "time-based" -> "Space"
                            "gear" -> "Item"
                            "split-stay" -> "Service"
                            else -> item.type.replaceFirstChar { it.uppercase() }
                        },
                        style = DSTypo.Caption2.copy(
                            fontFamily = paceDreamFontFamily,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.3.sp
                        ),
                        color = PaceDreamColors.Primary,
                        modifier = Modifier.padding(horizontal = PaceDreamSpacing.SM, vertical = PaceDreamSpacing.XS)
                    )
                }

                // Heart button
                FavoriteIconButton(
                    isFavorite = isFavorite,
                    onToggle = onFavoriteClick,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(PaceDreamSpacing.SM2)
                        .size(34.dp),
                )
            }

            // Content
            Column(modifier = Modifier.padding(PaceDreamSpacing.MD)) {
                Text(
                    text = item.title,
                    style = DSTypo.Callout.copy(
                        fontFamily = paceDreamFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        lineHeight = 20.sp
                    ),
                    color = PaceDreamColors.TextBody,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                item.location?.let { location ->
                    Spacer(modifier = Modifier.height(PaceDreamSpacing.XS))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = PaceDreamIcons.LocationOn,
                            contentDescription = null,
                            tint = PaceDreamColors.Gray400,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(PaceDreamSpacing.XS))
                        Text(
                            text = location.replace(Regex(",(?!\\s)"), ", "),
                            style = DSTypo.Caption.copy(fontFamily = paceDreamFontFamily),
                            color = PaceDreamColors.TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    item.price?.let { price ->
                        Text(
                            text = price,
                            style = DSTypo.Callout.copy(
                                fontFamily = paceDreamFontFamily,
                                fontWeight = FontWeight.Bold,
                                color = PaceDreamColors.Primary
                            )
                        )
                    }
                    item.rating?.let { ratingVal ->
                        if (ratingVal > 0.0) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(PaceDreamSpacing.XS)
                            ) {
                                Icon(
                                    imageVector = PaceDreamIcons.Star,
                                    contentDescription = null,
                                    tint = PaceDreamColors.Warning,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "%.1f".format(ratingVal),
                                    style = DSTypo.Caption.copy(
                                        fontFamily = paceDreamFontFamily,
                                        fontWeight = FontWeight.SemiBold
                                    ),
                                    color = PaceDreamColors.IconNeutral
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Services Grid Section (iOS parity: 2-column vertical grid)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ServicesGridSection(
    title: String,
    subtitle: String,
    items: List<HomeListingItem>,
    isLoading: Boolean,
    favoriteIds: Set<String>,
    onViewAllClick: () -> Unit,
    onItemClick: (HomeListingItem) -> Unit,
    onFavoriteClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        SectionHeader(
            title = title,
            subtitle = subtitle,
            onViewAllClick = onViewAllClick,
            modifier = Modifier.padding(horizontal = HomeHorizontalGutter)
        )

        Spacer(modifier = Modifier.height(PaceDreamSpacing.MD))

        if (isLoading) {
            // 2-column shimmer grid (4 skeleton cards)
            Column(
                modifier = Modifier.padding(horizontal = HomeHorizontalGutter),
                verticalArrangement = Arrangement.spacedBy(PaceDreamSpacing.SM)
            ) {
                for (row in 0 until 2) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(PaceDreamSpacing.SM)
                    ) {
                        Box(modifier = Modifier.weight(1f)) { GridShimmerCard() }
                        Box(modifier = Modifier.weight(1f)) { GridShimmerCard() }
                    }
                }
            }
        } else {
            // 2-column listing grid
            Column(
                modifier = Modifier.padding(horizontal = HomeHorizontalGutter),
                verticalArrangement = Arrangement.spacedBy(PaceDreamSpacing.SM)
            ) {
                val rows = items.chunked(2)
                for (rowItems in rows) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(PaceDreamSpacing.SM)
                    ) {
                        for (item in rowItems) {
                            Box(modifier = Modifier.weight(1f)) {
                                GridListingCard(
                                    item = item,
                                    isFavorite = item.id in favoriteIds,
                                    onClick = { onItemClick(item) },
                                    onFavoriteClick = { onFavoriteClick(item.id) }
                                )
                            }
                        }
                        // If odd number of items, add empty spacer for alignment
                        if (rowItems.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GridShimmerCard() {
    val transition = rememberInfiniteTransition(label = "gridShimmer")
    val shimmerX = transition.animateFloat(
        initialValue = -300f,
        targetValue = 900f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "gridShimmerX"
    )

    val shimmerBrush = Brush.linearGradient(
        colors = listOf(
            PaceDreamColors.ShimmerHighlight,
            PaceDreamColors.ShimmerBase,
            PaceDreamColors.ShimmerHighlight,
        ),
        start = Offset(shimmerX.value, 0f),
        end = Offset(shimmerX.value + 300f, 0f)
    )

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(PaceDreamRadius.LG),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(4f / 3f)
                    .clip(
                        RoundedCornerShape(
                            topStart = PaceDreamRadius.LG,
                            topEnd = PaceDreamRadius.LG
                        )
                    )
                    .background(shimmerBrush)
            )
            Column(modifier = Modifier.padding(PaceDreamSpacing.SM2)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .height(14.dp)
                        .clip(RoundedCornerShape(PaceDreamRadius.SM))
                        .background(shimmerBrush)
                )
                Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.55f)
                        .height(10.dp)
                        .clip(RoundedCornerShape(PaceDreamRadius.SM))
                        .background(shimmerBrush)
                )
                Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))
                Box(
                    modifier = Modifier
                        .width(56.dp)
                        .height(12.dp)
                        .clip(RoundedCornerShape(PaceDreamRadius.SM))
                        .background(shimmerBrush)
                )
            }
        }
    }
}

@Composable
private fun GridListingCard(
    item: HomeListingItem,
    isFavorite: Boolean = false,
    onClick: () -> Unit,
    onFavoriteClick: () -> Unit = {}
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = tween(durationMillis = 120),
        label = "gridCardScale"
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .adaptiveShadow(
                elevation = if (isPressed) 10.dp else 4.dp,
                shape = RoundedCornerShape(PaceDreamRadius.LG),
                pressed = isPressed
            )
            .semantics { role = Role.Button }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        shape = RoundedCornerShape(PaceDreamRadius.LG),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column {
            // Image area — 4:3 aspect ratio
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(4f / 3f)
                    .clip(
                        RoundedCornerShape(
                            topStart = PaceDreamRadius.LG,
                            topEnd = PaceDreamRadius.LG
                        )
                    )
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(item.imageUrl)
                        .crossfade(200)
                        .build(),
                    contentDescription = item.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                // Subtle bottom scrim for legibility
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    scrimOnImage(0.20f)
                                )
                            )
                        )
                )

                // Type badge (top-left)
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(PaceDreamSpacing.SM),
                    shape = RoundedCornerShape(PaceDreamRadius.SM),
                    color = badgeOnImageColor()
                ) {
                    Text(
                        text = when (item.type) {
                            "time-based" -> "Space"
                            "gear" -> "Item"
                            "split-stay" -> "Service"
                            else -> item.type.replaceFirstChar { it.uppercase() }
                        },
                        style = DSTypo.Caption2.copy(
                            fontFamily = paceDreamFontFamily,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.3.sp
                        ),
                        color = PaceDreamColors.Primary,
                        modifier = Modifier.padding(horizontal = PaceDreamSpacing.SM, vertical = PaceDreamSpacing.XS)
                    )
                }

                // Heart button (top-right)
                FavoriteIconButton(
                    isFavorite = isFavorite,
                    onToggle = onFavoriteClick,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(PaceDreamSpacing.SM)
                        .size(30.dp),
                )
            }

            // Content area below image
            Column(
                modifier = Modifier.padding(PaceDreamSpacing.SM2)
            ) {
                Text(
                    text = item.title,
                    style = DSTypo.Caption.copy(
                        fontFamily = paceDreamFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        lineHeight = 18.sp
                    ),
                    color = PaceDreamColors.TextBody,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                item.location?.let { location ->
                    Spacer(modifier = Modifier.height(PaceDreamSpacing.XS))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = PaceDreamIcons.LocationOn,
                            contentDescription = null,
                            tint = PaceDreamColors.Gray400,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(PaceDreamSpacing.XXS))
                        Text(
                            text = location.replace(Regex(",(?!\\s)"), ", "),
                            style = DSTypo.Caption2.copy(fontFamily = paceDreamFontFamily),
                            color = PaceDreamColors.TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    item.price?.let { price ->
                        Text(
                            text = price,
                            style = DSTypo.Caption.copy(
                                fontFamily = paceDreamFontFamily,
                                fontWeight = FontWeight.Bold,
                                color = PaceDreamColors.Primary
                            )
                        )
                    }
                    item.rating?.let { ratingVal ->
                        if (ratingVal > 0.0) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(PaceDreamSpacing.XXS)
                            ) {
                                Icon(
                                    imageVector = PaceDreamIcons.Star,
                                    contentDescription = null,
                                    tint = PaceDreamColors.Warning,
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = "%.1f".format(ratingVal),
                                    style = DSTypo.Caption2.copy(
                                        fontFamily = paceDreamFontFamily,
                                        fontWeight = FontWeight.SemiBold
                                    ),
                                    color = PaceDreamColors.IconNeutral
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Listing Card (iOS parity: 16dp corner radius, shadow, press animation,
// favorite button, rating badge)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ListingCard(
    item: HomeListingItem,
    isFavorite: Boolean = false,
    onClick: () -> Unit,
    onFavoriteClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = tween(durationMillis = 120),
        label = "cardScale"
    )

    Surface(
        modifier = modifier
            .widthIn(min = 200.dp)
            .scale(scale)
            .adaptiveShadow(
                elevation = if (isPressed) 10.dp else 4.dp,
                shape = RoundedCornerShape(PaceDreamRadius.LG),
                pressed = isPressed
            )
            .semantics { role = Role.Button }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        shape = RoundedCornerShape(PaceDreamRadius.LG),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column {
            // Image area — 4:3 aspect ratio for consistent proportions
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(4f / 3f)
                    .clip(
                        RoundedCornerShape(
                            topStart = PaceDreamRadius.LG,
                            topEnd = PaceDreamRadius.LG
                        )
                    )
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(item.imageUrl)
                        .crossfade(200)
                        .build(),
                    contentDescription = item.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                // Subtle bottom scrim for legibility
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp)
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    scrimOnImage(0.18f)
                                )
                            )
                        )
                )

                // Type badge (top-left)
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(PaceDreamSpacing.SM),
                    shape = RoundedCornerShape(PaceDreamRadius.SM),
                    color = badgeOnImageColor()
                ) {
                    Text(
                        text = when (item.type) {
                            "time-based" -> "Space"
                            "gear" -> "Item"
                            "split-stay" -> "Service"
                            else -> item.type.replaceFirstChar { it.uppercase() }
                        },
                        style = DSTypo.Caption2.copy(
                            fontFamily = paceDreamFontFamily,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.3.sp
                        ),
                        color = PaceDreamColors.Primary,
                        modifier = Modifier.padding(horizontal = PaceDreamSpacing.SM, vertical = PaceDreamSpacing.XS)
                    )
                }

                // Heart button (top-right)
                FavoriteIconButton(
                    isFavorite = isFavorite,
                    onToggle = onFavoriteClick,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(PaceDreamSpacing.SM)
                        .size(32.dp),
                )
            }

            // Content area below image
            Column(
                modifier = Modifier.padding(horizontal = PaceDreamSpacing.SM2, vertical = PaceDreamSpacing.SM)
            ) {
                // Title — primary emphasis
                Text(
                    text = item.title,
                    style = DSTypo.Caption.copy(
                        fontFamily = paceDreamFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        lineHeight = 18.sp
                    ),
                    color = PaceDreamColors.TextBody,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                // Location — secondary
                item.location?.let { location ->
                    Spacer(modifier = Modifier.height(PaceDreamSpacing.XS))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = PaceDreamIcons.LocationOn,
                            contentDescription = null,
                            tint = PaceDreamColors.Gray400,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(PaceDreamSpacing.XXS))
                        Text(
                            text = location.replace(Regex(",(?!\\s)"), ", "),
                            style = DSTypo.Caption2.copy(fontFamily = paceDreamFontFamily),
                            color = PaceDreamColors.TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Price + Rating row — bottom emphasis
                Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    item.price?.let { price ->
                        Text(
                            text = price,
                            style = DSTypo.Callout.copy(
                                fontFamily = paceDreamFontFamily,
                                fontWeight = FontWeight.Bold,
                                color = PaceDreamColors.Primary
                            )
                        )
                    }
                    item.rating?.let { ratingVal ->
                        if (ratingVal > 0.0) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(PaceDreamSpacing.XS)
                            ) {
                                Icon(
                                    imageVector = PaceDreamIcons.Star,
                                    contentDescription = null,
                                    tint = PaceDreamColors.Warning,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "%.1f".format(ratingVal),
                                    style = DSTypo.Caption.copy(
                                        fontFamily = paceDreamFontFamily,
                                        fontWeight = FontWeight.SemiBold
                                    ),
                                    color = PaceDreamColors.IconNeutral
                                )
                            }
                        } else {
                            Surface(
                                shape = RoundedCornerShape(PaceDreamRadius.XS),
                                color = PaceDreamColors.Primary.copy(alpha = 0.08f)
                            ) {
                                Text(
                                    text = "New",
                                    style = DSTypo.Caption2.copy(
                                        fontFamily = paceDreamFontFamily,
                                        fontWeight = FontWeight.SemiBold
                                    ),
                                    color = PaceDreamColors.Primary,
                                    modifier = Modifier.padding(horizontal = PaceDreamSpacing.SM, vertical = PaceDreamSpacing.XXS)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Browse by Type (Marketplace taxonomy: Spaces, Items, Services)
// ─────────────────────────────────────────────────────────────────────────────

private enum class HomeBrowseType(
    val displayTitle: String,
    val subtitle: String,
    val icon: ImageVector,
    val categoryColor: CategoryColor,
) {
    SPACES("Spaces", "Book flexible places nearby", PaceDreamIcons.Apartment, CategoryColors.Apartment),
    ITEMS("Items", "Borrow useful things on demand", PaceDreamIcons.Category, CategoryColors.Items),
    SERVICES("Services", "Find help for everyday needs", PaceDreamIcons.Build, CategoryColors.Services);

    data class Subcategory(val id: String, val title: String, val icon: ImageVector)

    val subcategories: List<Subcategory>
        get() = when (this) {
            SPACES -> listOf(
                Subcategory("parking", "Parking", PaceDreamIcons.LocalParking),
                Subcategory("restroom", "Restroom", PaceDreamIcons.Wc),
                Subcategory("nap_pod", "Nap Pod", PaceDreamIcons.Bed),
                Subcategory("meeting_room", "Meeting Room", PaceDreamIcons.MeetingRoom),
                Subcategory("storage_space", "Storage", PaceDreamIcons.Storage),
                Subcategory("gym", "Gym", PaceDreamIcons.FitnessCenter),
            )
            ITEMS -> listOf(
                Subcategory("camera", "Camera", PaceDreamIcons.CameraAlt),
                Subcategory("sports_gear", "Sports Gear", PaceDreamIcons.SportsEsports),
                Subcategory("tools", "Tools", PaceDreamIcons.Build),
                Subcategory("tech", "Tech", PaceDreamIcons.Laptop),
                Subcategory("micromobility", "Bike", PaceDreamIcons.DirectionsBike),
                Subcategory("instrument", "Instrument", PaceDreamIcons.SmartToy),
            )
            SERVICES -> listOf(
                Subcategory("cleaning_organizing", "Cleaning", PaceDreamIcons.LocalLaundryService),
                Subcategory("moving_help", "Moving Help", PaceDreamIcons.LocalOffer),
                Subcategory("home_help", "Home Help", PaceDreamIcons.Home),
                Subcategory("everyday_help", "Errands", PaceDreamIcons.ShoppingBag),
                Subcategory("fitness", "Fitness", PaceDreamIcons.FitnessCenter),
                Subcategory("learning", "Learning", PaceDreamIcons.School),
            )
        }
}

@Composable
private fun BrowseByTypeSection(
    onTypeTap: (String) -> Unit,
    onSubcategoryTap: (String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedType by remember { mutableStateOf(HomeBrowseType.SPACES) }

    Column(modifier = modifier.fillMaxWidth()) {
        // Section header
        SectionHeader(
            title = "Browse by Type",
            subtitle = "Explore spaces, items, and services",
            modifier = Modifier.padding(horizontal = HomeHorizontalGutter)
        )

        Spacer(modifier = Modifier.height(PaceDreamSpacing.SM2))

        // Segmented pill selector
        Row(
            modifier = Modifier
                .padding(horizontal = HomeHorizontalGutter)
                .fillMaxWidth()
                .background(
                    PaceDreamColors.Gray100,
                    RoundedCornerShape(PaceDreamRadius.XL)
                )
                .padding(PaceDreamSpacing.XS)
        ) {
            HomeBrowseType.entries.forEach { type ->
                BrowseTypePill(
                    type = type,
                    isSelected = selectedType == type,
                    onClick = {
                        selectedType = type
                        onTypeTap(type.displayTitle)
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(PaceDreamSpacing.SM2))

        // Subcategory chips
        val accentColor = selectedType.categoryColor.gradientStart
        LazyRow(
            contentPadding = PaddingValues(horizontal = HomeHorizontalGutter),
            horizontalArrangement = Arrangement.spacedBy(PaceDreamSpacing.SM)
        ) {
            items(selectedType.subcategories) { sub ->
                SubcategoryChip(
                    subcategory = sub,
                    accentColor = accentColor,
                    onClick = { onSubcategoryTap(selectedType.displayTitle, sub.title) }
                )
            }
        }
    }
}

@Composable
private fun BrowseTypePill(
    type: HomeBrowseType,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bgAlpha by animateFloatAsState(
        targetValue = if (isSelected) 1f else 0f,
        animationSpec = tween(200),
        label = "pillBg"
    )

    val gradientColors = type.categoryColor.gradient
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(PaceDreamRadius.LG))
            .then(
                if (bgAlpha > 0f) {
                    Modifier.background(
                        Brush.linearGradient(
                            colors = gradientColors,
                            start = Offset.Zero,
                            end = Offset(Float.POSITIVE_INFINITY, 0f)
                        ),
                        RoundedCornerShape(PaceDreamRadius.LG)
                    )
                } else Modifier
            )
            .semantics { role = Role.Button }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(vertical = PaceDreamSpacing.SM2),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = type.icon,
                contentDescription = null,
                tint = if (isSelected) OnBrandSurface else PaceDreamColors.Gray500,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(PaceDreamSpacing.SM))
            Text(
                text = type.displayTitle,
                style = DSTypo.Footnote.copy(
                    fontFamily = paceDreamFontFamily,
                    fontWeight = FontWeight.SemiBold
                ),
                color = if (isSelected) OnBrandSurface else PaceDreamColors.Gray500
            )
        }
    }
}

@Composable
private fun SubcategoryChip(
    subcategory: HomeBrowseType.Subcategory,
    accentColor: Color,
    onClick: () -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = tween(durationMillis = 100),
        label = "chipScale"
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .scale(scale)
            .background(
                MaterialTheme.colorScheme.surface,
                RoundedCornerShape(PaceDreamRadius.XL)
            )
            .border(
                width = 1.dp,
                color = PaceDreamColors.Gray200,
                shape = RoundedCornerShape(PaceDreamRadius.XL)
            )
            .semantics { role = Role.Button }
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        tryAwaitRelease()
                        isPressed = false
                        onClick()
                    }
                )
            }
            .padding(horizontal = PaceDreamSpacing.MD, vertical = PaceDreamSpacing.SM)
    ) {
        Icon(
            imageVector = subcategory.icon,
            contentDescription = null,
            tint = accentColor,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(PaceDreamSpacing.SM))
        Text(
            text = subcategory.title,
            style = DSTypo.Caption1.copy(
                fontFamily = paceDreamFontFamily,
                fontWeight = FontWeight.Medium
            ),
            color = PaceDreamColors.TextHeadline,
            maxLines = 1
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Trending Destinations (iOS parity: 2-column grid with gradient overlays)
// ─────────────────────────────────────────────────────────────────────────────

private data class DestinationData(
    val title: String,
    val propertyCount: Int,
    val imageUrl: String
)

private fun getTrendingDestinations(): List<DestinationData> = listOf(
    DestinationData("Grand Canyon", 42, "https://images.unsplash.com/photo-1474044159687-1ee9f3a51722?w=600"),
    DestinationData("Utah", 38, "https://images.unsplash.com/photo-1469854523086-cc02fe5d8800?w=600"),
    DestinationData("Maui", 55, "https://images.unsplash.com/photo-1542259009477-d625272157b7?w=600"),
    DestinationData("Glacier", 29, "https://images.unsplash.com/photo-1464822759023-fed622ff2c3b?w=600"),
    DestinationData("Honolulu", 67, "https://images.unsplash.com/photo-1507876466758-bc54f384809c?w=600"),
    DestinationData("Sedona", 31, "https://images.unsplash.com/photo-1500534314263-e9e68e3c0849?w=600")
)

@Composable
private fun TrendingDestinationsSection(
    onDestinationTap: (String) -> Unit,
    onViewAllTap: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        SectionHeader(
            title = "Trending Destinations",
            subtitle = "Popular places our community loves",
            onViewAllClick = onViewAllTap,
            modifier = Modifier.padding(horizontal = HomeHorizontalGutter)
        )
        Spacer(modifier = Modifier.height(PaceDreamSpacing.SM2))

        val destinations = getTrendingDestinations()
        Column(
            modifier = Modifier.padding(horizontal = HomeHorizontalGutter),
            verticalArrangement = Arrangement.spacedBy(PaceDreamSpacing.SM)
        ) {
            for (rowIndex in 0 until (destinations.size + 1) / 2) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(PaceDreamSpacing.SM)
                ) {
                    for (colIndex in 0..1) {
                        val index = rowIndex * 2 + colIndex
                        if (index < destinations.size) {
                            val destination = destinations[index]
                            val isLarge = index < 2
                            TrendingDestinationCard(
                                destination = destination,
                                isLarge = isLarge,
                                onClick = { onDestinationTap(destination.title) },
                                modifier = Modifier.weight(1f)
                            )
                        } else {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TrendingDestinationCard(
    destination: DestinationData,
    isLarge: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = tween(durationMillis = 100),
        label = "destScale"
    )

    Box(
        modifier = modifier
            .height(if (isLarge) 170.dp else 130.dp)
            .scale(scale)
            .clip(RoundedCornerShape(PaceDreamRadius.LG))
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        tryAwaitRelease()
                        isPressed = false
                        onClick()
                    }
                )
            }
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(destination.imageUrl)
                .crossfade(200)
                .build(),
            contentDescription = destination.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // Gradient overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            scrimOnImage(0.55f)
                        ),
                        startY = Float.POSITIVE_INFINITY * 0.4f,
                        endY = Float.POSITIVE_INFINITY
                    )
                )
        )

        // Label
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(PaceDreamSpacing.SM2)
        ) {
            Text(
                text = destination.title,
                style = DSTypo.Headline.copy(
                    fontFamily = paceDreamFontFamily,
                    fontSize = if (isLarge) 18.sp else 15.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = OnBrandSurface,
                maxLines = 1
            )
            Text(
                text = "${destination.propertyCount} properties",
                style = DSTypo.Caption2.copy(
                    fontFamily = paceDreamFontFamily,
                    fontWeight = FontWeight.Medium
                ),
                color = OnBrandSurface.copy(alpha = 0.85f)
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 3 Steps CTA (iOS parity: card with numbered steps + Get Started button)
// ─────────────────────────────────────────────────────────────────────────────

private data class StepData(
    val icon: ImageVector,
    val title: String,
    val description: String
)

@Composable
private fun ThreeStepsCTASection(
    onGetStarted: () -> Unit,
    modifier: Modifier = Modifier
) {
    val steps = listOf(
        StepData(
            PaceDreamIcons.Add,
            "Create listing",
            "Share your space, item, or parking spot with others."
        ),
        StepData(
            PaceDreamIcons.DateRange,
            "Receive a Booking",
            "Connect with people looking for flexible rentals nearby."
        ),
        StepData(
            PaceDreamIcons.AttachMoney,
            "Earn Extra Income",
            "Turn unused space or items into earnings."
        )
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = HomeHorizontalGutter),
        shape = RoundedCornerShape(PaceDreamRadius.XL),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(vertical = PaceDreamSpacing.LG),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Text(
                text = "3 Steps Away",
                style = DSTypo.Title3.copy(
                    fontFamily = paceDreamDisplayFontFamily,
                    fontWeight = FontWeight.Bold
                ),
                color = PaceDreamColors.TextHeadline,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(PaceDreamSpacing.XS))
            Text(
                text = "From your next affordable rental",
                style = DSTypo.Caption.copy(fontFamily = paceDreamFontFamily),
                color = PaceDreamColors.Gray500,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(PaceDreamSpacing.MD))

            // Steps
            Column(
                modifier = Modifier.padding(horizontal = HomeHorizontalGutter),
                verticalArrangement = Arrangement.spacedBy(PaceDreamSpacing.SM)
            ) {
                steps.forEachIndexed { index, step ->
                    StepCard(
                        stepNumber = index + 1,
                        step = step
                    )
                }
            }

            Spacer(modifier = Modifier.height(PaceDreamSpacing.MD))

            // CTA Button
            Button(
                onClick = onGetStarted,
                modifier = Modifier
                    .widthIn(max = 260.dp)
                    .fillMaxWidth()
                    .padding(horizontal = HomeHorizontalGutter),
                shape = RoundedCornerShape(PaceDreamRadius.MD),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PaceDreamColors.Primary
                ),
                contentPadding = PaddingValues(vertical = PaceDreamSpacing.SM2)
            ) {
                Text(
                    text = "Get Started",
                    style = DSTypo.Headline.copy(
                        fontFamily = paceDreamFontFamily,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    }
}

@Composable
private fun StepCard(
    stepNumber: Int,
    step: StepData
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(PaceDreamRadius.LG),
        color = PaceDreamColors.Background
    ) {
        Row(
            modifier = Modifier.padding(PaceDreamSpacing.MD),
            horizontalArrangement = Arrangement.spacedBy(PaceDreamSpacing.MD),
            verticalAlignment = Alignment.Top
        ) {
            // Step number badge
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(PaceDreamColors.Primary, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "$stepNumber",
                    style = DSTypo.Headline.copy(
                        fontFamily = paceDreamFontFamily,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(PaceDreamSpacing.XS)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(PaceDreamSpacing.SM),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = step.icon,
                        contentDescription = null,
                        tint = PaceDreamColors.Primary,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = step.title,
                        style = DSTypo.Callout.copy(
                            fontFamily = paceDreamFontFamily,
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = PaceDreamColors.TextHeadline
                    )
                }
                Text(
                    text = step.description,
                    style = DSTypo.Caption.copy(fontFamily = paceDreamFontFamily),
                    color = PaceDreamColors.Gray500
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// FAQ & Community Section (Website parity: expandable FAQ + support card)
// ─────────────────────────────────────────────────────────────────────────────


// ─────────────────────────────────────────────────────────────────────────────
// Shimmer Loading Card (iOS parity: skeleton with shimmer animation)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ShimmerCard(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val shimmerX = transition.animateFloat(
        initialValue = -300f,
        targetValue = 900f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerX"
    )

    val shimmerBrush = Brush.linearGradient(
        colors = listOf(
            PaceDreamColors.ShimmerHighlight,
            PaceDreamColors.ShimmerBase,
            PaceDreamColors.ShimmerHighlight,
        ),
        start = Offset(shimmerX.value, 0f),
        end = Offset(shimmerX.value + 300f, 0f)
    )

    Surface(
        modifier = modifier.widthIn(min = 200.dp),
        shape = RoundedCornerShape(PaceDreamRadius.LG),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(4f / 3f)
                    .clip(
                        RoundedCornerShape(
                            topStart = PaceDreamRadius.LG,
                            topEnd = PaceDreamRadius.LG
                        )
                    )
                    .background(shimmerBrush)
            )
            Column(modifier = Modifier.padding(PaceDreamSpacing.SM2)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .height(16.dp)
                        .clip(RoundedCornerShape(PaceDreamRadius.SM))
                        .background(shimmerBrush)
                )
                Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.55f)
                        .height(12.dp)
                        .clip(RoundedCornerShape(PaceDreamRadius.SM))
                        .background(shimmerBrush)
                )
                Spacer(modifier = Modifier.height(PaceDreamSpacing.SM2))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Box(
                        modifier = Modifier
                            .width(64.dp)
                            .height(14.dp)
                            .clip(RoundedCornerShape(PaceDreamRadius.SM))
                            .background(shimmerBrush)
                    )
                    Box(
                        modifier = Modifier
                            .width(36.dp)
                            .height(14.dp)
                            .clip(RoundedCornerShape(PaceDreamRadius.SM))
                            .background(shimmerBrush)
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Empty State (iOS parity: icon + title + subtitle + retry button)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun EmptyState(
    onRetry: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .background(
                    PaceDreamColors.Primary.copy(alpha = 0.08f),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = PaceDreamIcons.Search,
                contentDescription = null,
                tint = PaceDreamColors.Primary,
                modifier = Modifier.size(36.dp)
            )
        }
        // intentional: 20.dp empty-state gap; MD/LG both fall outside 2.dp tolerance
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = "Nothing to show right now",
            style = DSTypo.Title3.copy(
                fontFamily = paceDreamDisplayFontFamily,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = (-0.2).sp
            ),
            color = PaceDreamColors.TextHeadline,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))
        Text(
            text = "Check back later for new spaces and rentals",
            style = DSTypo.Subheadline.copy(fontFamily = paceDreamFontFamily),
            color = PaceDreamColors.Gray500,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(PaceDreamSpacing.LG))
        Button(
            onClick = onRetry,
            shape = RoundedCornerShape(PaceDreamRadius.MD),
            colors = ButtonDefaults.buttonColors(
                containerColor = PaceDreamColors.Primary
            ),
            contentPadding = PaddingValues(horizontal = PaceDreamSpacing.LG, vertical = PaceDreamSpacing.SM2)
        ) {
            Text(
                text = "Retry",
                style = DSTypo.Headline.copy(
                    fontFamily = paceDreamFontFamily,
                    fontSize = 15.sp
                ),
                color = MaterialTheme.colorScheme.onPrimary
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Previews — light & dark. Exercises the dark-theme surfaces, shadows and
// overlays so regressions show up in the PR render without standing up the
// ViewModel / data layer.
// ─────────────────────────────────────────────────────────────────────────────

private val PreviewListingItems = listOf(
    HomeListingItem(
        id = "preview-1",
        title = "Bright corner meeting room",
        imageUrl = null,
        location = "Downtown, SF",
        price = "$18 / hr",
        rating = 4.8,
        type = "time-based",
    ),
    HomeListingItem(
        id = "preview-2",
        title = "DSLR camera kit",
        imageUrl = null,
        location = "Mission, SF",
        price = "$42 / day",
        rating = 4.6,
        type = "gear",
    ),
    HomeListingItem(
        id = "preview-3",
        title = "Apartment cleaning",
        imageUrl = null,
        location = "Bay Area",
        price = "$60",
        rating = 4.9,
        type = "split-stay",
    ),
)

@Composable
private fun HomeScreenPreviewBody() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = PaceDreamSpacing.LG)
        ) {
            item {
                val uriHandler = LocalUriHandler.current
                HeroHeaderSection(
                    heroImageUrl = null,
                    onSearchClick = {},
                    onFilterClick = {},
                    onNotificationClick = {},
                    onAboutClick = {
                        // Preview-only hook so the CTA isn't a no-op in isolation;
                        // the real screen wires this to the in-app About route.
                        runCatching { uriHandler.openUri("https://www.pacedream.com") }
                    },
                )
            }
            item {
                CategoryFilterTabs(
                    selectedCategory = "All",
                    onCategorySelected = {},
                    modifier = Modifier.padding(top = PaceDreamSpacing.XS),
                )
            }
            item {
                SectionSurface {
                    ListingSection(
                        title = "Spaces",
                        subtitle = "Popular nearby",
                        items = PreviewListingItems,
                        isLoading = false,
                        favoriteIds = setOf("preview-1"),
                        onViewAllClick = {},
                        onItemClick = {},
                        onFavoriteClick = {},
                    )
                }
            }
            item {
                ThreeStepsCTASection(onGetStarted = {})
            }
        }
    }
}

@Preview(name = "HomeScreen Light", showBackground = true, widthDp = 360, heightDp = 900)
@Composable
private fun HomeScreenLightPreview() {
    PaceDreamTheme(darkTheme = false) {
        HomeScreenPreviewBody()
    }
}

@Preview(
    name = "HomeScreen Dark",
    showBackground = true,
    widthDp = 360,
    heightDp = 900,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun HomeScreenDarkPreview() {
    PaceDreamTheme(darkTheme = true) {
        HomeScreenPreviewBody()
    }
}
