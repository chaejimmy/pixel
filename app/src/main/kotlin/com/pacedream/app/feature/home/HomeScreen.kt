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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.pacedream.common.composables.theme.paceDreamDisplayFontFamily
import com.pacedream.common.composables.theme.paceDreamFontFamily

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel(),
    onSectionViewAll: (String) -> Unit,
    onListingClick: (HomeListingItem) -> Unit,
    onSearchClick: () -> Unit = {},
    onCategoryClick: (String) -> Unit = {},
    onCategoryFilterClick: (String) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val selectedCategoryFilter = uiState.selectedCategory

    PullToRefreshBox(
        isRefreshing = uiState.isRefreshing,
        onRefresh = { viewModel.refresh() },
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 100.dp)
        ) {
            // ── Hero Header with Gradient + Overlapping Search Bar ──
            item {
                HeroHeaderSection(
                    heroImageUrl = uiState.heroImageUrl,
                    onSearchClick = onSearchClick,
                    onFilterClick = { /* TODO: Open filters */ },
                    onNotificationClick = {}
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
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            // ── Warning banner ──
            if (uiState.hasErrors) {
                item {
                    WarningBanner(
                        message = "Some content couldn't load. Pull to refresh.",
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
                    )
                }
            }

            // ── Explore Categories (Quick chips) ──
            item {
                QuickCategoriesRow(
                    onCategoryClick = onCategoryClick,
                    modifier = Modifier.padding(top = 32.dp)
                )
            }

            // ── Hourly Spaces ──
            if (uiState.filteredHourlySpaces.isNotEmpty() || uiState.isLoadingHourlySpaces) {
                item {
                    ListingSection(
                        title = "Spaces",
                        subtitle = "Find flexible spaces — restrooms, meeting rooms, parking, and more",
                        items = uiState.filteredHourlySpaces,
                        isLoading = uiState.isLoadingHourlySpaces,
                        onViewAllClick = { onSectionViewAll("hourly-spaces") },
                        onItemClick = onListingClick,
                        modifier = Modifier.padding(top = 32.dp)
                    )
                }
            }

            // ── Items ──
            if (uiState.filteredRentGear.isNotEmpty() || uiState.isLoadingRentGear) {
                item {
                    ListingSection(
                        title = "Items",
                        subtitle = "Rent what you need — cameras, sports gear, tech, tools, and more",
                        items = uiState.filteredRentGear,
                        isLoading = uiState.isLoadingRentGear,
                        onViewAllClick = { onSectionViewAll("rent-gear") },
                        onItemClick = onListingClick,
                        modifier = Modifier.padding(top = 32.dp)
                    )
                }
            }

            // ── Services ──
            if (uiState.filteredSplitStays.isNotEmpty() || uiState.isLoadingSplitStays) {
                item {
                    ListingSection(
                        title = "Services",
                        subtitle = "Book help when you need it — cleaning, moving, fitness, and more",
                        items = uiState.filteredSplitStays,
                        isLoading = uiState.isLoadingSplitStays,
                        onViewAllClick = { onSectionViewAll("split-stays") },
                        onItemClick = onListingClick,
                        modifier = Modifier.padding(top = 32.dp)
                    )
                }
            }

            // ── Browse by Type Section (Marketplace taxonomy) ──
            item {
                BrowseByTypeSection(
                    onTypeTap = { type -> onCategoryClick(type) },
                    onSubcategoryTap = { _, subcategory -> onCategoryClick(subcategory) },
                    modifier = Modifier.padding(top = 32.dp)
                )
            }

            // ── Trending Destinations (iOS parity) ──
            item {
                TrendingDestinationsSection(
                    onDestinationTap = { destination -> onCategoryClick(destination) },
                    onViewAllTap = { onSectionViewAll("destinations") },
                    modifier = Modifier.padding(top = 32.dp)
                )
            }

            // ── 3 Steps CTA (iOS parity) ──
            item {
                ThreeStepsCTASection(
                    onGetStarted = { onCategoryClick("create-listing") },
                    modifier = Modifier.padding(top = 32.dp)
                )
            }

            // ── FAQ & Community (Website parity) ──
            item {
                FaqAndCommunitySection(
                    onContactSupport = { /* TODO: open support */ },
                    modifier = Modifier.padding(top = 32.dp)
                )
            }

            // ── Empty state ──
            if (!uiState.isLoading && uiState.isEmpty) {
                item {
                    EmptyState(
                        onRetry = { viewModel.refresh() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(48.dp)
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Hero Header Section (iOS parity: gradient hero with overlapping search bar)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun HeroHeaderSection(
    heroImageUrl: String?,
    onSearchClick: () -> Unit,
    onFilterClick: () -> Unit,
    onNotificationClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(320.dp)
    ) {
        // Gradient background (matches iOS HeroHeader purple gradient)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp)
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF4F46E5), // Indigo top (iOS parity)
                            Color(0xFF6B5CE7), // Purple mid (iOS parity)
                            Color(0xFF7B4DFF)  // Purple bottom (iOS parity)
                        ),
                        start = Offset(0f, 0f),
                        end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 20.dp)
            ) {
                // Top row: greeting + notification bell
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Welcome back",
                            style = DSTypo.Footnote.copy(
                                fontFamily = paceDreamFontFamily,
                                fontWeight = FontWeight.Medium
                            ),
                            color = Color.White.copy(alpha = 0.85f)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Discover",
                            style = DSTypo.Title1.copy(
                                fontFamily = paceDreamDisplayFontFamily,
                                letterSpacing = (-0.5).sp
                            ),
                            color = Color.White
                        )
                    }
                    Box {
                        Surface(
                            modifier = Modifier
                                .size(44.dp)
                                .clickable(onClick = onNotificationClick),
                            shape = CircleShape,
                            color = Color.White.copy(alpha = 0.20f)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = PaceDreamIcons.Notifications,
                                    contentDescription = "Notifications",
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        // Unread badge (iOS parity: small blue dot indicator)
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(PaceDreamColors.Primary, CircleShape)
                                .align(Alignment.TopEnd)
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                // Main headline (iOS parity)
                Text(
                    text = "Find spaces, items, and services — only for the time you need.",
                    style = DSTypo.Title1.copy(
                        fontFamily = paceDreamDisplayFontFamily,
                        fontSize = 28.sp,
                        letterSpacing = (-0.3).sp
                    ),
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Spaces \u00B7 Items \u00B7 Services",
                    style = DSTypo.Subheadline.copy(
                        fontFamily = paceDreamFontFamily,
                        fontWeight = FontWeight.Medium
                    ),
                    color = Color.White.copy(alpha = 0.9f)
                )
                Spacer(modifier = Modifier.height(20.dp))
            }
        }

        // Overlapping Search Bar
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 20.dp)
                .fillMaxWidth()
                .shadow(
                    elevation = 10.dp,
                    shape = RoundedCornerShape(PaceDreamRadius.LG),
                    ambientColor = Color.Black.copy(alpha = 0.06f),
                    spotColor = Color.Black.copy(alpha = 0.10f)
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
                    .padding(horizontal = 14.dp, vertical = 12.dp),
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
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Search anywhere",
                        style = DSTypo.Callout.copy(
                            fontFamily = paceDreamFontFamily,
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = Color(0xFF1A1A1A)
                    )
                    Spacer(modifier = Modifier.height(1.dp))
                    Text(
                        text = "Spaces \u00B7 Items \u00B7 Services",
                        style = DSTypo.Caption.copy(fontFamily = paceDreamFontFamily),
                        color = PaceDreamColors.Gray500
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Surface(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onFilterClick),
                    shape = CircleShape,
                    color = PaceDreamColors.Gray100,
                    border = BorderStroke(0.5.dp, PaceDreamColors.Gray200)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = PaceDreamIcons.Tune,
                            contentDescription = "Filters",
                            tint = Color(0xFF374151),
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
            contentPadding = PaddingValues(horizontal = 16.dp),
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
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 14.dp)
            .padding(top = 12.dp, bottom = 0.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = name,
            tint = if (isSelected) PaceDreamColors.Primary
                   else Color(0xFF6B7280), // Gray-500 for better contrast
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = name,
            style = DSTypo.Caption.copy(
                fontFamily = paceDreamFontFamily,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                letterSpacing = if (isSelected) 0.sp else 0.1.sp
            ),
            color = if (isSelected) PaceDreamColors.Primary
                    else Color(0xFF6B7280),
            maxLines = 1
        )
        Spacer(modifier = Modifier.height(10.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.5.dp)
                .background(
                    color = if (isSelected) PaceDreamColors.Primary else Color.Transparent,
                    shape = RoundedCornerShape(2.dp)
                )
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Quick Categories Row (iOS parity: horizontal chip row with gradient icons)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun QuickCategoriesRow(
    onCategoryClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        SectionHeader(
            title = "Explore Categories",
            modifier = Modifier.padding(horizontal = 20.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
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

    Surface(
        modifier = Modifier
            .height(48.dp)
            .scale(scale)
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
        color = category.bgColor.copy(alpha = 0.08f),
        border = BorderStroke(0.5.dp, category.bgColor.copy(alpha = 0.12f))
    ) {
        Row(
            modifier = Modifier.padding(start = 6.dp, end = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(category.bgColor.copy(alpha = 0.12f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = category.icon,
                    contentDescription = category.name,
                    tint = category.bgColor,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = category.name,
                style = DSTypo.Subheadline.copy(
                    fontFamily = paceDreamFontFamily,
                    fontWeight = FontWeight.Medium
                ),
                color = Color(0xFF1A1A1A),
                maxLines = 1
            )
        }
    }
}

private data class CategoryCardData(
    val name: String,
    val icon: ImageVector,
    val bgColor: Color
)

private fun getCategoryCards(): List<CategoryCardData> {
    return listOf(
        CategoryCardData("Entire Home", PaceDreamIcons.Home, Color(0xFFEF4444)),
        CategoryCardData("Private Room", PaceDreamIcons.MeetingRoom, Color(0xFFEC4899)),
        CategoryCardData("Nap Pod", PaceDreamIcons.Bed, Color(0xFF8B5CF6)),
        CategoryCardData("Meeting Room", PaceDreamIcons.Business, Color(0xFF3B82F6)),
        CategoryCardData("Workspace", PaceDreamIcons.Laptop, Color(0xFF10B981)),
        CategoryCardData("EV Parking", PaceDreamIcons.ElectricCar, Color(0xFFA855F7)),
        CategoryCardData("Study Room", PaceDreamIcons.School, Color(0xFF059669)),
        CategoryCardData("Short Stay", PaceDreamIcons.Hotel, Color(0xFFF59E0B)),
        CategoryCardData("Apartment", PaceDreamIcons.Apartment, Color(0xFFDC2626)),
        CategoryCardData("Parking", PaceDreamIcons.LocalParking, Color(0xFF6366F1)),
        CategoryCardData("Luxury Room", PaceDreamIcons.Star, Color(0xFFD97706)),
        CategoryCardData("Storage Space", PaceDreamIcons.Storage, Color(0xFF0EA5E9))
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
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = PaceDreamIcons.Warning,
                contentDescription = null,
                tint = PaceDreamColors.Warning,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = message,
                style = DSTypo.Subheadline.copy(
                    fontFamily = paceDreamFontFamily,
                    fontWeight = FontWeight.SemiBold
                ),
                color = Color(0xFF78350F)
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
                style = DSTypo.Title2.copy(
                    fontFamily = paceDreamDisplayFontFamily,
                    letterSpacing = (-0.3).sp
                ),
                color = Color(0xFF1A1A1A),
                modifier = Modifier.weight(1f, fill = false)
            )
            if (onViewAllClick != null) {
                Spacer(modifier = Modifier.width(12.dp))
                TextButton(
                    onClick = onViewAllClick,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "View All",
                        style = DSTypo.Subheadline.copy(
                            fontFamily = paceDreamFontFamily,
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = PaceDreamColors.Primary
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Icon(
                        imageVector = PaceDreamIcons.ChevronRight,
                        contentDescription = null,
                        tint = PaceDreamColors.Primary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
        if (subtitle != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                style = DSTypo.Footnote.copy(
                    fontFamily = paceDreamFontFamily,
                    lineHeight = 18.sp
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
    onViewAllClick: () -> Unit,
    onItemClick: (HomeListingItem) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        SectionHeader(
            title = title,
            subtitle = subtitle,
            onViewAllClick = onViewAllClick,
            modifier = Modifier.padding(horizontal = 20.dp)
        )

        Spacer(modifier = Modifier.height(18.dp))

        if (isLoading) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(4) { ShimmerCard() }
            }
        } else {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(items) { item ->
                    ListingCard(
                        item = item,
                        onClick = { onItemClick(item) }
                    )
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
    onClick: () -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = tween(durationMillis = 120),
        label = "cardScale"
    )

    Surface(
        modifier = Modifier
            .width(270.dp)
            .scale(scale)
            .shadow(
                elevation = if (isPressed) 10.dp else 4.dp,
                shape = RoundedCornerShape(PaceDreamRadius.LG),
                ambientColor = Color.Black.copy(alpha = 0.06f),
                spotColor = Color.Black.copy(alpha = if (isPressed) 0.12f else 0.08f)
            )
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
        shape = RoundedCornerShape(PaceDreamRadius.LG),
        color = Color.White
    ) {
        Column {
            // Image area — 4:3 aspect ratio (270 × 202)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(202.dp)
                    .clip(
                        RoundedCornerShape(
                            topStart = PaceDreamRadius.LG,
                            topEnd = PaceDreamRadius.LG
                        )
                    )
            ) {
                AsyncImage(
                    model = item.imageUrl,
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
                                    Color.Black.copy(alpha = 0.20f)
                                )
                            )
                        )
                )

                // Type badge (top-left)
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(12.dp),
                    shape = RoundedCornerShape(PaceDreamRadius.SM),
                    color = Color.White.copy(alpha = 0.95f)
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
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                // Heart button (top-right)
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp)
                        .size(34.dp),
                    shape = CircleShape,
                    color = Color.Black.copy(alpha = 0.30f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = PaceDreamIcons.FavoriteBorderOutlined,
                            contentDescription = "Add to favorites",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Content area below image
            Column(
                modifier = Modifier.padding(
                    start = 14.dp,
                    end = 14.dp,
                    top = 14.dp,
                    bottom = 14.dp
                )
            ) {
                // Title — primary emphasis
                Text(
                    text = item.title,
                    style = DSTypo.Callout.copy(
                        fontFamily = paceDreamFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        lineHeight = 20.sp
                    ),
                    color = Color(0xFF111827),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                // Location — secondary
                item.location?.let { location ->
                    Spacer(modifier = Modifier.height(5.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = PaceDreamIcons.LocationOn,
                            contentDescription = null,
                            tint = PaceDreamColors.Gray400,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = location.replace(Regex(",(?!\\s)"), ", "),
                            style = DSTypo.Caption.copy(fontFamily = paceDreamFontFamily),
                            color = Color(0xFF6B7280),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Price + Rating row — bottom emphasis
                Spacer(modifier = Modifier.height(10.dp))
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
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Icon(
                                    imageVector = PaceDreamIcons.Star,
                                    contentDescription = null,
                                    tint = Color(0xFFF59E0B),
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "%.1f".format(ratingVal),
                                    style = DSTypo.Caption.copy(
                                        fontFamily = paceDreamFontFamily,
                                        fontWeight = FontWeight.SemiBold
                                    ),
                                    color = Color(0xFF374151)
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
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
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
    val gradientColors: List<Color>
) {
    SPACES("Spaces", "Book flexible places nearby", PaceDreamIcons.Apartment, listOf(Color(0xFF5527D7), Color(0xFF7C5CE7))),
    ITEMS("Items", "Borrow useful things on demand", PaceDreamIcons.Category, listOf(Color(0xFF3B82F6), Color(0xFF60A5FA))),
    SERVICES("Services", "Find help for everyday needs", PaceDreamIcons.Build, listOf(Color(0xFF10B981), Color(0xFF34D399)));

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
        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
            Text(
                text = "Browse by Type",
                style = DSTypo.Title3.copy(
                    fontFamily = paceDreamDisplayFontFamily,
                    fontWeight = FontWeight.Bold
                ),
                color = Color(0xFF1A1A1A)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Explore spaces, items, and services near you",
                style = DSTypo.Footnote.copy(fontFamily = paceDreamFontFamily),
                color = PaceDreamColors.Gray500
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Segmented pill selector
        Row(
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .fillMaxWidth()
                .background(
                    PaceDreamColors.Gray100,
                    RoundedCornerShape(PaceDreamRadius.XL)
                )
                .padding(4.dp)
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

        Spacer(modifier = Modifier.height(16.dp))

        // Subcategory chips
        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(selectedType.subcategories) { sub ->
                SubcategoryChip(
                    subcategory = sub,
                    accentColor = selectedType.gradientColors.first(),
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

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(PaceDreamRadius.LG))
            .then(
                if (bgAlpha > 0f) {
                    Modifier.background(
                        Brush.linearGradient(
                            colors = type.gradientColors,
                            start = Offset.Zero,
                            end = Offset(Float.POSITIVE_INFINITY, 0f)
                        ),
                        RoundedCornerShape(PaceDreamRadius.LG)
                    )
                } else Modifier
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = type.icon,
                contentDescription = null,
                tint = if (isSelected) Color.White else PaceDreamColors.Gray500,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = type.displayTitle,
                style = DSTypo.Footnote.copy(
                    fontFamily = paceDreamFontFamily,
                    fontWeight = FontWeight.SemiBold
                ),
                color = if (isSelected) Color.White else PaceDreamColors.Gray500
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
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Icon(
            imageVector = subcategory.icon,
            contentDescription = null,
            tint = accentColor,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = subcategory.title,
            style = DSTypo.Caption1.copy(
                fontFamily = paceDreamFontFamily,
                fontWeight = FontWeight.Medium
            ),
            color = Color(0xFF1A1A1A),
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
            modifier = Modifier.padding(horizontal = 20.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))

        val destinations = getTrendingDestinations()
        Column(
            modifier = Modifier.padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            for (rowIndex in 0 until (destinations.size + 1) / 2) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
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
            .height(if (isLarge) 200.dp else 150.dp)
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
            model = destination.imageUrl,
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
                            Color.Black.copy(alpha = 0.55f)
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
                .padding(12.dp)
        ) {
            Text(
                text = destination.title,
                style = DSTypo.Headline.copy(
                    fontFamily = paceDreamFontFamily,
                    fontSize = if (isLarge) 18.sp else 15.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = Color.White,
                maxLines = 1
            )
            Text(
                text = "${destination.propertyCount} properties",
                style = DSTypo.Caption2.copy(
                    fontFamily = paceDreamFontFamily,
                    fontWeight = FontWeight.Medium
                ),
                color = Color.White.copy(alpha = 0.85f)
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
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(24.dp),
        color = PaceDreamColors.Card,
        shadowElevation = 4.dp
    ) {
        Column(
            modifier = Modifier.padding(vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Text(
                text = "3 Steps Away",
                style = DSTypo.Title2.copy(
                    fontFamily = paceDreamDisplayFontFamily,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = Color(0xFF1A1A1A),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "From your next affordable rental",
                style = DSTypo.Subheadline.copy(fontFamily = paceDreamFontFamily),
                color = PaceDreamColors.Gray500,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Steps
            Column(
                modifier = Modifier.padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                steps.forEachIndexed { index, step ->
                    StepCard(
                        stepNumber = index + 1,
                        step = step
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // CTA Button
            Button(
                onClick = onGetStarted,
                modifier = Modifier
                    .widthIn(max = 280.dp)
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PaceDreamColors.Primary
                ),
                contentPadding = PaddingValues(vertical = 14.dp)
            ) {
                Text(
                    text = "Get Started",
                    style = DSTypo.Headline.copy(
                        fontFamily = paceDreamFontFamily,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = Color.White
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
        shape = RoundedCornerShape(14.dp),
        color = PaceDreamColors.Background
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
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
                    color = Color.White
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
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
                        color = Color(0xFF1A1A1A)
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

private data class FaqItem(val question: String, val answer: String)

private fun getFaqItems(): List<FaqItem> = listOf(
    FaqItem(
        "What is PaceDream?",
        "PaceDream is a marketplace where you can rent spaces, borrow items, and book services — only for the time you need them."
    ),
    FaqItem(
        "How do I book a space or item?",
        "Browse listings on our platform, select what you need, choose your dates and times, and complete the booking. It's that simple!"
    ),
    FaqItem(
        "How does pricing work?",
        "Pricing varies by listing and is set by the host. You'll see the hourly, daily, or per-use rate on each listing page before you book."
    ),
    FaqItem(
        "Is my payment secure?",
        "Yes! We use Stripe for all transactions, ensuring your payment information is always encrypted and secure."
    ),
    FaqItem(
        "How do I become a host?",
        "Switch to Host mode from your profile, then create a listing by adding photos, setting your price, and describing what you're offering."
    ),
    FaqItem(
        "What if I need to cancel?",
        "You can cancel your booking from the Bookings tab. Cancellation policies vary by listing — check the listing details for specifics."
    )
)

@Composable
private fun FaqAndCommunitySection(
    onContactSupport: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        // Section header
        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
            Text(
                text = "Frequently Asked Questions",
                style = DSTypo.Title3.copy(
                    fontFamily = paceDreamDisplayFontFamily,
                    fontWeight = FontWeight.Bold
                ),
                color = Color(0xFF1A1A1A)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Everything you need to know about PaceDream",
                style = DSTypo.Footnote.copy(fontFamily = paceDreamFontFamily),
                color = PaceDreamColors.Gray500
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // FAQ accordion
        val faqItems = getFaqItems()
        Column(
            modifier = Modifier.padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            faqItems.forEachIndexed { index, faq ->
                FaqAccordionItem(faq = faq)
                if (index < faqItems.size - 1) {
                    HorizontalDivider(
                        thickness = 0.5.dp,
                        color = PaceDreamColors.Gray100
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Get in Touch card (Website parity: purple gradient card with support buttons)
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            shape = RoundedCornerShape(PaceDreamRadius.LG),
            color = Color.Transparent
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF5527D7),
                                Color(0xFF4F46E5)
                            ),
                            start = Offset.Zero,
                            end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                        ),
                        RoundedCornerShape(PaceDreamRadius.LG)
                    )
                    .padding(24.dp)
            ) {
                Column {
                    Text(
                        text = "Get in Touch",
                        style = DSTypo.Title3.copy(
                            fontFamily = paceDreamDisplayFontFamily,
                            fontWeight = FontWeight.Bold
                        ),
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Have questions or need help? Our support team is here for you.",
                        style = DSTypo.Subheadline.copy(fontFamily = paceDreamFontFamily),
                        color = Color.White.copy(alpha = 0.85f)
                    )
                    Spacer(modifier = Modifier.height(20.dp))

                    // Message Support button
                    Button(
                        onClick = onContactSupport,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(PaceDreamRadius.MD),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White
                        ),
                        contentPadding = PaddingValues(vertical = 14.dp)
                    ) {
                        Icon(
                            imageVector = PaceDreamIcons.Chat,
                            contentDescription = null,
                            tint = PaceDreamColors.Primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Message Support",
                            style = DSTypo.Callout.copy(
                                fontFamily = paceDreamFontFamily,
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = PaceDreamColors.Primary
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Send Email button
                    OutlinedButton(
                        onClick = { /* TODO: open email */ },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(PaceDreamRadius.MD),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color.White
                        ),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.5f)),
                        contentPadding = PaddingValues(vertical = 14.dp)
                    ) {
                        Icon(
                            imageVector = PaceDreamIcons.Email,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Send us an Email",
                            style = DSTypo.Callout.copy(
                                fontFamily = paceDreamFontFamily,
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FaqAccordionItem(faq: FaqItem) {
    var isExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = { isExpanded = !isExpanded }
            )
            .padding(vertical = 14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = faq.question,
                style = DSTypo.Callout.copy(
                    fontFamily = paceDreamFontFamily,
                    fontWeight = FontWeight.SemiBold
                ),
                color = Color(0xFF1A1A1A),
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = if (isExpanded) PaceDreamIcons.ExpandLess else PaceDreamIcons.ExpandMore,
                contentDescription = if (isExpanded) "Collapse" else "Expand",
                tint = PaceDreamColors.Gray400,
                modifier = Modifier.size(20.dp)
            )
        }
        AnimatedVisibility(visible = isExpanded) {
            Text(
                text = faq.answer,
                style = DSTypo.Footnote.copy(fontFamily = paceDreamFontFamily),
                color = PaceDreamColors.Gray500,
                modifier = Modifier.padding(top = 8.dp, end = 28.dp)
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Shimmer Loading Card (iOS parity: skeleton with shimmer animation)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ShimmerCard() {
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
            Color(0xFFF0F0F0),
            Color(0xFFE0E0E0),
            Color(0xFFF0F0F0)
        ),
        start = Offset(shimmerX.value, 0f),
        end = Offset(shimmerX.value + 300f, 0f)
    )

    Surface(
        modifier = Modifier.width(270.dp),
        shape = RoundedCornerShape(PaceDreamRadius.LG),
        color = Color.White,
        shadowElevation = 2.dp
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(202.dp)
                    .clip(
                        RoundedCornerShape(
                            topStart = PaceDreamRadius.LG,
                            topEnd = PaceDreamRadius.LG
                        )
                    )
                    .background(shimmerBrush)
            )
            Column(modifier = Modifier.padding(14.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .height(16.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(shimmerBrush)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.55f)
                        .height(12.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(shimmerBrush)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Box(
                        modifier = Modifier
                            .width(64.dp)
                            .height(14.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(shimmerBrush)
                    )
                    Box(
                        modifier = Modifier
                            .width(36.dp)
                            .height(14.dp)
                            .clip(RoundedCornerShape(6.dp))
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
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = "Nothing to show right now",
            style = DSTypo.Title3.copy(
                fontFamily = paceDreamDisplayFontFamily,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = (-0.2).sp
            ),
            color = Color(0xFF1A1A1A),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Check back later for new spaces and rentals",
            style = DSTypo.Subheadline.copy(fontFamily = paceDreamFontFamily),
            color = PaceDreamColors.Gray500,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onRetry,
            shape = RoundedCornerShape(PaceDreamRadius.MD),
            colors = ButtonDefaults.buttonColors(
                containerColor = PaceDreamColors.Primary
            ),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
        ) {
            Text(
                text = "Retry",
                style = DSTypo.Headline.copy(
                    fontFamily = paceDreamFontFamily,
                    fontSize = 15.sp
                ),
                color = Color.White
            )
        }
    }
}

/**
 * Home listing item model
 */
data class HomeListingItem(
    val id: String,
    val title: String,
    val imageUrl: String?,
    val location: String?,
    val price: String?,
    val rating: Double?,
    val type: String
)
