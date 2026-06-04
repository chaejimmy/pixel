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
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import com.pacedream.common.icon.PaceDreamIcons
import com.pacedream.common.composables.designsystem.modifier.pressable
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.pacedream.common.composables.designsystem.state.EmptyState
import com.shourov.apps.pacedream.designsystem.CategoryColor
import com.shourov.apps.pacedream.designsystem.CategoryColors
import com.shourov.apps.pacedream.designsystem.FavoriteIconButton
import com.shourov.apps.pacedream.designsystem.NotificationBellButton
import com.shourov.apps.pacedream.designsystem.OnBrandSurface
import com.shourov.apps.pacedream.designsystem.modifier.adaptiveShadow
import com.shourov.apps.pacedream.designsystem.badgeOnImageColor
import com.shourov.apps.pacedream.designsystem.scrimOnImage
import com.shourov.apps.pacedream.feature.notification.presentation.UnreadNotificationsViewModel
import com.shourov.apps.pacedream.R

object HomeTestTags {
    const val Root = "home_screen_root"
    const val ListingFeed = "home_listing_feed"
    const val SearchBar = "home_search_bar"
    const val FilterButton = "home_filter_button"
    const val NotificationButton = "home_notification_button"
    const val CategoryTabs = "home_category_tabs"
    // Per-card primary CTA + favourite — shared across all three card
    // variants (Featured / Grid / Listing). Tests should use
    // onAllNodesWithTag(...) when asserting card counts.
    const val ListingCard = "home_listing_card"
    const val FavoriteButton = "home_favorite_button"
    // FAQ + Support sections — registered ahead of implementation
    // (DESIGN_QA_REPORT_ANDROID.md §P2-#10) so the tag string is locked
    // in before the section composables land.
    const val FaqSection = "home_faq_section"
    const val SupportSection = "home_support_section"
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
    onNotificationClick: () -> Unit,
    // TODO(design): replace null default with the production 1920×1080 hero
    //  painter once design ships the asset; until then, callers can pass
    //  painterResource(R.drawable.home_hero) here and we'll skip the gradient.
    heroAsset: Painter? = null,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val selectedCategoryFilter = uiState.selectedCategory

    // Resolve the unread-notifications VM at the stable screen scope rather than
    // inside the hero LazyColumn item. Keeping it here ties it to HomeScreen's
    // ViewModelStoreOwner and confines unread-count recompositions to the hero
    // item we pass it into, instead of churning VM resolution as the list scrolls.
    val unreadVm: UnreadNotificationsViewModel = hiltViewModel()
    val unreadCount by unreadVm.unreadCount.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is HomeViewModel.Effect.ShowAuthRequired -> onShowAuthSheet()
                is HomeViewModel.Effect.ShowToast -> snackbarHostState.showSnackbar(effect.message)
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .testTag(HomeTestTags.Root)
    ) {
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
            item(key = "hero", contentType = "hero") {
                HeroHeaderSection(
                    heroAsset = heroAsset,
                    heroImageUrl = uiState.heroImageUrl,
                    unreadCount = unreadCount,
                    onSearchClick = onSearchClick,
                    onFilterClick = onSearchClick,
                    onNotificationClick = onNotificationClick,
                    onMarkAllSeen = { unreadVm.markAllAsSeen() },
                    onAboutClick = onAboutClick,
                )
            }

            // ── Category Filter Tabs ──
            item(key = "categoryTabs", contentType = "categoryTabs") {
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
                item(key = "warningBanner", contentType = "warningBanner") {
                    WarningBanner(
                        message = "Some content couldn't load.",
                        onRefresh = { viewModel.refresh() },
                        modifier = Modifier.padding(horizontal = PaceDreamSpacing.Layout.HomeGutter, vertical = PaceDreamSpacing.SM2)
                    )
                }
            }

            // ── Extended Categories (iOS parity) ──
            item(key = "categories", contentType = "categories") {
                ExtendedCategoriesSection(
                    onCategoryClick = onCategoryClick,
                    modifier = Modifier.padding(top = PaceDreamSpacing.MD)
                )
            }

            // ── Hourly Spaces ──
            if (uiState.filteredHourlySpaces.isNotEmpty() || uiState.isLoadingHourlySpaces) {
                item(key = "spaces", contentType = "spaces") {
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
                item(key = "items", contentType = "items") {
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
                item(key = "services", contentType = "services") {
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
            item(key = "browseByType", contentType = "browseByType") {
                SectionSurface {
                    BrowseByTypeSection(
                        onTypeTap = { type -> onCategoryClick(type) },
                        onSubcategoryTap = { _, subcategory -> onCategoryClick(subcategory) },
                    )
                }
            }

            // ── Trending Destinations (derived from live listings) ──
            if (uiState.trendingDestinations.isNotEmpty()) {
                item(key = "destinations", contentType = "destinations") {
                    SectionSurface {
                        TrendingDestinationsSection(
                            destinations = uiState.trendingDestinations,
                            onDestinationTap = { destination -> onCategoryClick(destination) },
                            onViewAllTap = { onSectionViewAll("destinations") },
                        )
                    }
                }
            }

            // ── 3 Steps CTA (iOS parity) ──
            item(key = "threeSteps", contentType = "threeSteps") {
                ThreeStepsCTASection(
                    onGetStarted = { onCategoryClick("create-listing") },
                    modifier = Modifier.padding(top = PaceDreamSpacing.SM)
                )
            }

            // ── Empty state ──
            if (!uiState.isLoading && uiState.isEmpty) {
                item(key = "empty", contentType = "empty") {
                    EmptyState(
                        title = "Nothing to show right now",
                        subtitle = "Check back later for new spaces and rentals",
                        icon = PaceDreamIcons.Search,
                        ctaLabel = "Retry",
                        onCta = { viewModel.refresh() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(PaceDreamSpacing.XXL)
                    )
                }
            }
        }
    }
    }
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
    unreadCount: Int,
    onSearchClick: () -> Unit,
    onFilterClick: () -> Unit,
    onNotificationClick: () -> Unit,
    onMarkAllSeen: () -> Unit,
    onAboutClick: () -> Unit,
    heroAsset: Painter? = null,
) {
    // Brand purple → blue gradient used both as the hero background fallback
    // and on the primary "Get to know PaceDream" CTA. Endpoints come from
    // PaceDreamColors so the values stay tied to the brand tokens (iOS parity).
    val brandGradient = Brush.linearGradient(
        colors = listOf(PaceDreamColors.GradientStart, PaceDreamColors.GradientEnd)
    )
    val hasImage = heroAsset != null || !heroImageUrl.isNullOrBlank()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(360.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(328.dp)
                // Always paint the brand gradient as the base layer. When a
                // hero image is supplied it's rendered on top and covers the
                // gradient; without one, the gradient acts as the fallback so
                // the header still reads as "PaceDream".
                .background(brandGradient)
        ) {
            when {
                heroAsset != null -> {
                    Image(
                        painter = heroAsset,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
                !heroImageUrl.isNullOrBlank() -> {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(heroImageUrl)
                            .crossfade(true)
                            // Distinct cache key for the hero render slot so the
                            // full-bleed decode doesn't evict (or get evicted by)
                            // the same URL rendered at card/grid size elsewhere.
                            .memoryCacheKey("$heroImageUrl@hero")
                            .build(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
            if (hasImage) {
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
                    .padding(horizontal = PaceDreamSpacing.Layout.HomeGutter, vertical = PaceDreamSpacing.Layout.HomeGutter)
            ) {
                // Top row: notification bell (right-aligned; hero copy takes over greeting).
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    NotificationBellButton(
                        unreadCount = unreadCount,
                        onClick = {
                            onNotificationClick()
                            onMarkAllSeen()
                        },
                        // .size(44.dp) constrains only the visual purple circle;
                        // the component prepends touchTargetSize() so the hit
                        // area is floored at 48dp regardless.
                        modifier = Modifier
                            .size(44.dp)
                            .testTag(HomeTestTags.NotificationButton),
                        containerColor = OnBrandSurface.copy(alpha = 0.20f),
                        iconTint = OnBrandSurface,
                    )
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
                        .background(
                            brush = brandGradient,
                            shape = RoundedCornerShape(PaceDreamRadius.LG)
                        )
                        .semantics { role = Role.Button },
                    shape = RoundedCornerShape(PaceDreamRadius.LG),
                    // Container is transparent so the brand gradient applied
                    // via the modifier shows through; Material still owns the
                    // ripple, focus state, and elevation.
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent,
                        contentColor = OnBrandSurface
                    ),
                    contentPadding = PaddingValues(horizontal = PaceDreamSpacing.Layout.HomeGutter, vertical = PaceDreamSpacing.SM2),
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

        // Overlapping Search Bar \u2014 two sibling click regions (search + filter)
        // separated by a hairline divider so a tap can't be ambiguous.
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = PaceDreamSpacing.Layout.HomeGutter)
                .fillMaxWidth()
                .adaptiveShadow(
                    elevation = 10.dp,
                    shape = RoundedCornerShape(PaceDreamRadius.LG)
                ),
            shape = RoundedCornerShape(PaceDreamRadius.LG),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 0.dp
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // LEFT \u2014 search affordance, takes remaining width
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .testTag(HomeTestTags.SearchBar)
                        .semantics(mergeDescendants = true) {}
                        .clickable(
                            role = Role.Button,
                            onClickLabel = "Search",
                            onClick = onSearchClick,
                        )
                        .padding(
                            horizontal = PaceDreamSpacing.MD,
                            vertical = PaceDreamSpacing.SM2
                        ),
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
                        Spacer(modifier = Modifier.height(PaceDreamSpacing.Layout.Hairline))
                        Text(
                            text = "Spaces \u00B7 Items \u00B7 Services",
                            style = DSTypo.Caption.copy(fontFamily = paceDreamFontFamily),
                            color = PaceDreamColors.IconNeutral
                        )
                    }
                }

                // hairline divider \u2014 visually separates the two touch regions
                Box(
                    Modifier
                        .height(28.dp)
                        .width(0.5.dp)
                        .background(PaceDreamColors.DividerNeutral)
                )

                // RIGHT \u2014 filter button, fixed width
                IconButton(
                    onClick = onFilterClick,
                    modifier = Modifier
                        .size(48.dp)
                        .padding(end = PaceDreamSpacing.SM)
                        .testTag(HomeTestTags.FilterButton)
                ) {
                    Icon(
                        imageVector = PaceDreamIcons.Tune,
                        contentDescription = "Filters",
                        tint = PaceDreamColors.IconNeutral
                    )
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
    // Category chips matching pacedream.com filter order exactly:
    //   All · Restroom · Nap Pod · Meeting Room · Study Room · Short Stay ·
    //   Apartment · Luxury Room · Parking · Storage Space
    val categories = listOf(
        Triple("All", PaceDreamIcons.AppsOutlined, PaceDreamIcons.Apps),
        Triple("Restroom", PaceDreamIcons.WcOutlined, PaceDreamIcons.Wc),
        Triple("Nap Pod", PaceDreamIcons.BedOutlined, PaceDreamIcons.Bed),
        Triple("Meeting Room", PaceDreamIcons.MeetingRoomOutlined, PaceDreamIcons.MeetingRoom),
        Triple("Study Room", PaceDreamIcons.MenuBookOutlined, PaceDreamIcons.MenuBook),
        Triple("Short Stay", PaceDreamIcons.BedtimeOutlined, PaceDreamIcons.Bedtime),
        Triple("Apartment", PaceDreamIcons.ApartmentOutlined, PaceDreamIcons.Apartment),
        Triple("Luxury Room", PaceDreamIcons.DiamondOutlined, PaceDreamIcons.Diamond),
        Triple("Parking", PaceDreamIcons.LocalParkingOutlined, PaceDreamIcons.LocalParking),
        Triple("Storage Space", PaceDreamIcons.StorageOutlined, PaceDreamIcons.Storage),
    )

    Column(modifier = modifier.testTag(HomeTestTags.CategoryTabs)) {
        LazyRow(
            contentPadding = PaddingValues(horizontal = PaceDreamSpacing.MD),
            horizontalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            items(categories, key = { it.first }, contentType = { "categoryTab" }) { (name, outlinedIcon, filledIcon) ->
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
            color = PaceDreamColors.DividerNeutral
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
                onClickLabel = "Filter by $name",
                onClick = onClick
            )
            .padding(horizontal = PaceDreamSpacing.MD)
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
            modifier = Modifier.padding(horizontal = PaceDreamSpacing.Layout.HomeGutter)
        )
        Spacer(modifier = Modifier.height(PaceDreamSpacing.SM2))
        LazyRow(
            contentPadding = PaddingValues(horizontal = PaceDreamSpacing.Layout.HomeGutter),
            horizontalArrangement = Arrangement.spacedBy(PaceDreamSpacing.SM)
        ) {
            items(getCategoryCards(), key = { it.name }, contentType = { "categoryChip" }) { category ->
                QuickCategoryChip(
                    category = category,
                    onClick = { onCategoryClick(category.name) }
                )
            }
        }
    }
}

@Composable
internal fun QuickCategoryChip(
    category: CategoryCardData,
    onClick: () -> Unit
) {
    val tint = category.color.tint
    Surface(
        modifier = Modifier
            .height(48.dp)
            .pressable(
                onClick = onClick,
                onClickLabel = "Open ${category.name}",
                pressedScale = 0.95f,
            ),
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

internal data class CategoryCardData(
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
    modifier: Modifier = Modifier,
    onRefresh: (() -> Unit)? = null,
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
                color = PaceDreamColors.OnWarningContainer,
                modifier = Modifier.weight(1f)
            )
            if (onRefresh != null) {
                Spacer(modifier = Modifier.width(PaceDreamSpacing.SM))
                TextButton(
                    onClick = onRefresh,
                    contentPadding = PaddingValues(horizontal = PaceDreamSpacing.SM, vertical = PaceDreamSpacing.XS)
                ) {
                    Text(
                        text = "Refresh",
                        style = DSTypo.Subheadline.copy(
                            fontFamily = paceDreamFontFamily,
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = PaceDreamColors.OnWarningContainer
                    )
                }
            }
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
                color = PaceDreamColors.IconNeutral
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
            modifier = Modifier.padding(horizontal = PaceDreamSpacing.Layout.HomeGutter)
        )

        Spacer(modifier = Modifier.height(PaceDreamSpacing.MD))

        if (isLoading) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = PaceDreamSpacing.Layout.HomeGutter),
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
                    modifier = Modifier.padding(horizontal = PaceDreamSpacing.Layout.HomeGutter)
                )
            }
            // Two items: side-by-side compact grid
            items.size == 2 -> {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = PaceDreamSpacing.Layout.HomeGutter),
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
                    contentPadding = PaddingValues(horizontal = PaceDreamSpacing.Layout.HomeGutter),
                    horizontalArrangement = Arrangement.spacedBy(PaceDreamSpacing.SM2)
                ) {
                    items(items, key = { it.id }, contentType = { "listingCard" }) { item ->
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
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .animatedCardEntry()
            .adaptiveShadow(
                elevation = 4.dp,
                shape = RoundedCornerShape(PaceDreamRadius.LG),
            )
            .testTag(HomeTestTags.ListingCard)
            .pressable(
                onClick = onClick,
                onClickLabel = item.title,
                pressedScale = 0.98f,
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
                        .crossfade(true)
                        .memoryCacheKey("${item.id}@featured")
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
                        .size(34.dp)
                        .testTag(HomeTestTags.FavoriteButton),
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
                            tint = PaceDreamColors.IconMuted,
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
            modifier = Modifier.padding(horizontal = PaceDreamSpacing.Layout.HomeGutter)
        )

        Spacer(modifier = Modifier.height(PaceDreamSpacing.MD))

        if (isLoading) {
            // 2-column shimmer grid (4 skeleton cards)
            Column(
                modifier = Modifier.padding(horizontal = PaceDreamSpacing.Layout.HomeGutter),
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
                modifier = Modifier.padding(horizontal = PaceDreamSpacing.Layout.HomeGutter),
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

/**
 * Booking-mode badge rendered on listing cards. Conveys at-a-glance whether
 * the listing is "Instant Book" (Airbnb-parity), needs request-to-book, or
 * is fully unavailable. Returns no UI when both flags are unknown so we
 * don't add visual noise for legacy backend rows.
 */
@Composable
private fun BookingModeBadge(
    instantBook: Boolean?,
    available: Boolean?
) {
    when {
        available == false -> {
            Surface(
                shape = RoundedCornerShape(PaceDreamRadius.SM),
                color = scrimOnImage(0.65f)
            ) {
                Text(
                    text = "Unavailable",
                    style = DSTypo.Caption2.copy(
                        fontFamily = paceDreamFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.3.sp
                    ),
                    color = OnBrandSurface,
                    modifier = Modifier.padding(
                        horizontal = PaceDreamSpacing.SM,
                        vertical = PaceDreamSpacing.XS
                    )
                )
            }
        }
        instantBook == true -> {
            Surface(
                shape = RoundedCornerShape(PaceDreamRadius.SM),
                color = PaceDreamColors.Primary
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(
                        horizontal = PaceDreamSpacing.SM,
                        vertical = PaceDreamSpacing.XS
                    )
                ) {
                    Icon(
                        imageVector = PaceDreamIcons.Bolt,
                        contentDescription = null,
                        tint = OnBrandSurface,
                        modifier = Modifier.size(10.dp)
                    )
                    Spacer(modifier = Modifier.width(PaceDreamSpacing.XXS))
                    Text(
                        text = "Instant Book",
                        style = DSTypo.Caption2.copy(
                            fontFamily = paceDreamFontFamily,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.3.sp
                        ),
                        color = OnBrandSurface
                    )
                }
            }
        }
        instantBook == false -> {
            Surface(
                shape = RoundedCornerShape(PaceDreamRadius.SM),
                color = badgeOnImageColor()
            ) {
                Text(
                    text = "Request to Book",
                    style = DSTypo.Caption2.copy(
                        fontFamily = paceDreamFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.3.sp
                    ),
                    color = PaceDreamColors.TextPrimary,
                    modifier = Modifier.padding(
                        horizontal = PaceDreamSpacing.SM,
                        vertical = PaceDreamSpacing.XS
                    )
                )
            }
        }
        else -> Unit
    }
}

@Composable
private fun GridListingCard(
    item: HomeListingItem,
    isFavorite: Boolean = false,
    onClick: () -> Unit,
    onFavoriteClick: () -> Unit = {}
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .adaptiveShadow(
                elevation = 4.dp,
                shape = RoundedCornerShape(PaceDreamRadius.LG),
            )
            .testTag(HomeTestTags.ListingCard)
            .pressable(
                onClick = onClick,
                onClickLabel = item.title,
                pressedScale = 0.97f,
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
                        .crossfade(true)
                        .memoryCacheKey("${item.id}@grid")
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

                // Type + booking-mode badges (top-left, stacked)
                Column(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(PaceDreamSpacing.SM),
                    verticalArrangement = Arrangement.spacedBy(PaceDreamSpacing.XS)
                ) {
                    Surface(
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
                    BookingModeBadge(instantBook = item.instantBook, available = item.available)
                }

                // Heart button (top-right)
                FavoriteIconButton(
                    isFavorite = isFavorite,
                    onToggle = onFavoriteClick,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(PaceDreamSpacing.SM)
                        .size(30.dp)
                        .testTag(HomeTestTags.FavoriteButton),
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
                            tint = PaceDreamColors.IconMuted,
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
    Surface(
        modifier = modifier
            .widthIn(min = 200.dp)
            .adaptiveShadow(
                elevation = 4.dp,
                shape = RoundedCornerShape(PaceDreamRadius.LG),
            )
            .testTag(HomeTestTags.ListingCard)
            .pressable(
                onClick = onClick,
                onClickLabel = item.title,
                pressedScale = 0.97f,
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
                        .crossfade(true)
                        .memoryCacheKey("${item.id}@card")
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

                // Type + booking-mode badges (top-left, stacked)
                Column(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(PaceDreamSpacing.SM),
                    verticalArrangement = Arrangement.spacedBy(PaceDreamSpacing.XS)
                ) {
                    Surface(
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
                    BookingModeBadge(instantBook = item.instantBook, available = item.available)
                }

                // Heart button (top-right)
                FavoriteIconButton(
                    isFavorite = isFavorite,
                    onToggle = onFavoriteClick,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(PaceDreamSpacing.SM)
                        .size(32.dp)
                        .testTag(HomeTestTags.FavoriteButton),
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
                            tint = PaceDreamColors.IconMuted,
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
            modifier = Modifier.padding(horizontal = PaceDreamSpacing.Layout.HomeGutter)
        )

        Spacer(modifier = Modifier.height(PaceDreamSpacing.SM2))

        // Segmented pill selector
        Row(
            modifier = Modifier
                .padding(horizontal = PaceDreamSpacing.Layout.HomeGutter)
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
            contentPadding = PaddingValues(horizontal = PaceDreamSpacing.Layout.HomeGutter),
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
            .pressable(
                onClick = onClick,
                onClickLabel = type.displayTitle,
                pressedScale = 0.95f,
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
                tint = if (isSelected) OnBrandSurface else PaceDreamColors.IconNeutral,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(PaceDreamSpacing.SM))
            Text(
                text = type.displayTitle,
                style = DSTypo.Footnote.copy(
                    fontFamily = paceDreamFontFamily,
                    fontWeight = FontWeight.SemiBold
                ),
                color = if (isSelected) OnBrandSurface else PaceDreamColors.IconNeutral
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
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .background(
                MaterialTheme.colorScheme.surface,
                RoundedCornerShape(PaceDreamRadius.XL)
            )
            .border(
                width = 1.dp,
                color = PaceDreamColors.DividerNeutral,
                shape = RoundedCornerShape(PaceDreamRadius.XL)
            )
            .clip(RoundedCornerShape(PaceDreamRadius.XL))
            .pressable(
                onClick = onClick,
                onClickLabel = "Open ${subcategory.title}",
                pressedScale = 0.95f,
            )
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

@Composable
private fun TrendingDestinationsSection(
    destinations: List<HomeDestination>,
    onDestinationTap: (String) -> Unit,
    onViewAllTap: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        SectionHeader(
            title = "Trending Destinations",
            subtitle = "Popular places our community loves",
            onViewAllClick = onViewAllTap,
            modifier = Modifier.padding(horizontal = PaceDreamSpacing.Layout.HomeGutter)
        )
        Spacer(modifier = Modifier.height(PaceDreamSpacing.SM2))

        Column(
            modifier = Modifier.padding(horizontal = PaceDreamSpacing.Layout.HomeGutter),
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
    destination: HomeDestination,
    isLarge: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(if (isLarge) 170.dp else 130.dp)
            .clip(RoundedCornerShape(PaceDreamRadius.LG))
            .pressable(
                onClick = onClick,
                onClickLabel = "Open ${destination.title}",
                pressedScale = 0.96f,
            )
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(destination.imageUrl)
                .crossfade(true)
                .memoryCacheKey("${destination.title}@destination")
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
                // Token-driven sizes: large cards get Headline (17sp Bold), small
                // cards get Subheadline (15sp Bold). Was inline 18.sp/15.sp.
                style = (if (isLarge) DSTypo.Headline else DSTypo.Subheadline).copy(
                    fontFamily = paceDreamFontFamily,
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
            .padding(horizontal = PaceDreamSpacing.Layout.HomeGutter),
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
                color = PaceDreamColors.IconNeutral,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(PaceDreamSpacing.MD))

            // Steps
            Column(
                modifier = Modifier.padding(horizontal = PaceDreamSpacing.Layout.HomeGutter),
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
                    .padding(horizontal = PaceDreamSpacing.Layout.HomeGutter),
                shape = RoundedCornerShape(PaceDreamRadius.MD),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PaceDreamColors.Primary
                ),
                contentPadding = PaddingValues(vertical = PaceDreamSpacing.SM2)
            ) {
                Text(
                    text = "Get Started",
                    style = DSTypo.CalloutBold.copy(fontFamily = paceDreamFontFamily),
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
                    style = DSTypo.CalloutBold.copy(fontFamily = paceDreamFontFamily),
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
                    color = PaceDreamColors.IconNeutral
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
                    unreadCount = 3,
                    onSearchClick = {},
                    onFilterClick = {},
                    onNotificationClick = { /* preview no-op; production wired via NotificationRoutes.NOTIFICATIONS */ },
                    onMarkAllSeen = { /* preview no-op; production calls UnreadNotificationsViewModel.markAllAsSeen() */ },
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
