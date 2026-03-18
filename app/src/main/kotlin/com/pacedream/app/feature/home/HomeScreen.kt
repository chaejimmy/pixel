package com.pacedream.app.feature.home

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
                        title = "Hourly Spaces",
                        subtitle = "Flexible spaces for short stays",
                        items = uiState.filteredHourlySpaces,
                        isLoading = uiState.isLoadingHourlySpaces,
                        onViewAllClick = { onSectionViewAll("hourly-spaces") },
                        onItemClick = onListingClick,
                        modifier = Modifier.padding(top = 32.dp)
                    )
                }
            }

            // ── Rent Gear ──
            if (uiState.filteredRentGear.isNotEmpty() || uiState.isLoadingRentGear) {
                item {
                    ListingSection(
                        title = "Rent Gear",
                        subtitle = "Equipment and tools for every need",
                        items = uiState.filteredRentGear,
                        isLoading = uiState.isLoadingRentGear,
                        onViewAllClick = { onSectionViewAll("rent-gear") },
                        onItemClick = onListingClick,
                        modifier = Modifier.padding(top = 32.dp)
                    )
                }
            }

            // ── Split Stays ──
            if (uiState.filteredSplitStays.isNotEmpty() || uiState.isLoadingSplitStays) {
                item {
                    ListingSection(
                        title = "Split Stays",
                        subtitle = "Flexible long-term rentals",
                        items = uiState.filteredSplitStays,
                        isLoading = uiState.isLoadingSplitStays,
                        onViewAllClick = { onSectionViewAll("split-stays") },
                        onItemClick = onListingClick,
                        modifier = Modifier.padding(top = 32.dp)
                    )
                }
            }

            // ── Find by Type Section (iOS parity) ──
            item {
                ExploreByTypeRow(
                    onTypeTap = { type -> onCategoryClick(type) },
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
                            Color(0xFF6B5CE7), // Brand purple top
                            Color(0xFF4A3ABA), // Brand purple mid
                            Color(0xFF3D2D9C)  // Brand purple bottom
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
                    Surface(
                        modifier = Modifier
                            .size(42.dp)
                            .clickable(onClick = onNotificationClick),
                        shape = CircleShape,
                        color = Color.White.copy(alpha = 0.18f)
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
                }

                Spacer(modifier = Modifier.weight(1f))

                // Main headline
                Text(
                    text = "Find your perfect stay!",
                    style = DSTypo.Title1.copy(
                        fontFamily = paceDreamDisplayFontFamily,
                        fontSize = 28.sp,
                        letterSpacing = (-0.3).sp
                    ),
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Book, share, or split anything.",
                    style = DSTypo.Subheadline.copy(fontFamily = paceDreamFontFamily),
                    color = Color.White.copy(alpha = 0.8f)
                )
                Spacer(modifier = Modifier.height(20.dp))
            }
        }

        // Overlapping Search Bar (matches iOS BlurredSearchBar)
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 20.dp)
                .fillMaxWidth()
                .shadow(
                    elevation = 8.dp,
                    shape = RoundedCornerShape(PaceDreamRadius.LG),
                    ambientColor = Color.Black.copy(alpha = 0.08f),
                    spotColor = Color.Black.copy(alpha = 0.12f)
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
                        style = DSTypo.Headline.copy(
                            fontFamily = paceDreamFontFamily,
                            fontSize = 15.sp
                        ),
                        color = Color(0xFF1A1A1A)
                    )
                    Text(
                        text = "Anywhere \u00B7 Any time \u00B7 Any type",
                        style = DSTypo.Caption.copy(fontFamily = paceDreamFontFamily),
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
            style = DSTypo.Caption2.copy(
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
        Spacer(modifier = Modifier.height(14.dp))
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
        color = category.bgColor.copy(alpha = 0.06f)
    ) {
        Row(
            modifier = Modifier.padding(start = 6.dp, end = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(category.bgColor.copy(alpha = 0.10f), CircleShape),
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
                style = DSTypo.Footnote.copy(
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
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = DSTypo.Title2.copy(
                    fontFamily = paceDreamDisplayFontFamily,
                    letterSpacing = (-0.3).sp
                ),
                color = Color(0xFF1A1A1A)
            )
            if (subtitle != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    style = DSTypo.Subheadline.copy(fontFamily = paceDreamFontFamily),
                    color = PaceDreamColors.Gray500
                )
            }
        }
        if (onViewAllClick != null) {
            TextButton(onClick = onViewAllClick) {
                Text(
                    text = "View All",
                    style = DSTypo.Subheadline.copy(
                        fontFamily = paceDreamFontFamily,
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = PaceDreamColors.Secondary
                )
            }
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

        Spacer(modifier = Modifier.height(16.dp))

        if (isLoading) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items(4) { ShimmerCard() }
            }
        } else {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
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
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = tween(durationMillis = 100),
        label = "cardScale"
    )

    Surface(
        modifier = Modifier
            .width(260.dp)
            .scale(scale)
            .shadow(
                elevation = if (isPressed) 12.dp else 8.dp,
                shape = RoundedCornerShape(PaceDreamRadius.LG),
                ambientColor = Color.Black.copy(alpha = 0.10f),
                spotColor = Color.Black.copy(alpha = if (isPressed) 0.15f else 0.10f)
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
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
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

                // Bottom scrim
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.30f)
                                )
                            )
                        )
                )

                // Type badge
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(10.dp),
                    shape = RoundedCornerShape(PaceDreamRadius.XS),
                    color = Color.White
                ) {
                    Text(
                        text = when (item.type) {
                            "time-based" -> "Hourly"
                            "gear" -> "Gear"
                            "split-stay" -> "Split"
                            else -> item.type.replaceFirstChar { it.uppercase() }
                        },
                        style = DSTypo.Caption2.copy(
                            fontFamily = paceDreamFontFamily,
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = PaceDreamColors.Primary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                // Heart (iOS parity: dark overlay circle)
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(10.dp)
                        .size(34.dp),
                    shape = CircleShape,
                    color = Color.Black.copy(alpha = 0.35f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = PaceDreamIcons.FavoriteBorderOutlined,
                            contentDescription = "Add to favorites",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                // Rating badge (bottom-right)
                item.rating?.let { rating ->
                    Surface(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(10.dp),
                        shape = RoundedCornerShape(PaceDreamRadius.XS),
                        color = Color.White
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = PaceDreamIcons.Star,
                                contentDescription = null,
                                tint = Color(0xFFFFBE0B),
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "%.1f".format(rating),
                                style = DSTypo.Caption2.copy(
                                    fontFamily = paceDreamFontFamily,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = Color(0xFF1A1A1A)
                            )
                        }
                    }
                }
            }

            // Content below image (inside card surface)
            Column(
                modifier = Modifier.padding(
                    start = 14.dp,
                    end = 14.dp,
                    top = 12.dp,
                    bottom = 14.dp
                )
            ) {
                Text(
                    text = item.title,
                    style = DSTypo.Callout.copy(
                        fontFamily = paceDreamFontFamily,
                        fontWeight = FontWeight.Medium
                    ),
                    color = Color(0xFF1A1A1A),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                item.location?.let { location ->
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = PaceDreamIcons.LocationOn,
                            contentDescription = null,
                            tint = PaceDreamColors.Gray400,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = location,
                            style = DSTypo.Footnote.copy(fontFamily = paceDreamFontFamily),
                            color = PaceDreamColors.Gray500,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                item.price?.let { price ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = price,
                            style = DSTypo.Headline.copy(
                                fontFamily = paceDreamFontFamily,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = PaceDreamColors.Primary
                            )
                        )
                        item.rating?.let { rating ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = PaceDreamIcons.Star,
                                    contentDescription = null,
                                    tint = Color(0xFFFFBE0B),
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "%.1f".format(rating),
                                    style = DSTypo.Footnote.copy(
                                        fontFamily = paceDreamFontFamily,
                                        fontWeight = FontWeight.Medium
                                    ),
                                    color = Color(0xFF1A1A1A)
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
// Explore by Type (iOS parity: horizontal row of gradient icon cards)
// ─────────────────────────────────────────────────────────────────────────────

private data class ExploreTypeData(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val gradientColors: List<Color>
)

private fun getExploreTypes(): List<ExploreTypeData> = listOf(
    ExploreTypeData(
        "Romantic", "Cozy getaways", PaceDreamIcons.Favorite,
        listOf(Color(0xFFFF6B6B), Color(0xFFEE5A24))
    ),
    ExploreTypeData(
        "Adventure", "Thrilling stays", PaceDreamIcons.DirectionsBike,
        listOf(Color(0xFF4ECDC4), Color(0xFF2ECC71))
    ),
    ExploreTypeData(
        "Nature", "Peaceful retreats", PaceDreamIcons.Yard,
        listOf(Color(0xFF45B649), Color(0xFF2E8B57))
    ),
    ExploreTypeData(
        "Urban", "City living", PaceDreamIcons.Apartment,
        listOf(Color(0xFF667EEA), Color(0xFF764BA2))
    ),
    ExploreTypeData(
        "Solo", "Me time", PaceDreamIcons.Person,
        listOf(Color(0xFFF093FB), Color(0xFFF5576C))
    )
)

@Composable
private fun ExploreByTypeRow(
    onTypeTap: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        SectionHeader(
            title = "Find by Type",
            subtitle = "Discover stays that match your vibe",
            modifier = Modifier.padding(horizontal = 20.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            items(getExploreTypes()) { type ->
                ExploreTypeCard(
                    type = type,
                    onClick = { onTypeTap(type.title) }
                )
            }
        }
    }
}

@Composable
private fun ExploreTypeCard(
    type: ExploreTypeData,
    onClick: () -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.94f else 1f,
        animationSpec = tween(durationMillis = 100),
        label = "typeScale"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(110.dp)
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
            }
    ) {
        // Gradient icon container (72dp matching iOS)
        Box(
            modifier = Modifier
                .size(72.dp)
                .shadow(
                    elevation = 8.dp,
                    shape = RoundedCornerShape(PaceDreamRadius.LG),
                    ambientColor = type.gradientColors.first().copy(alpha = 0.30f),
                    spotColor = type.gradientColors.first().copy(alpha = 0.30f)
                )
                .background(
                    Brush.linearGradient(
                        colors = type.gradientColors,
                        start = Offset(0f, 0f),
                        end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                    ),
                    RoundedCornerShape(PaceDreamRadius.LG)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = type.icon,
                contentDescription = type.title,
                tint = Color.White,
                modifier = Modifier.size(28.dp)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = type.title,
            style = DSTypo.Footnote.copy(
                fontFamily = paceDreamFontFamily,
                fontWeight = FontWeight.SemiBold
            ),
            color = Color(0xFF1A1A1A),
            textAlign = TextAlign.Center,
            maxLines = 1
        )
        Text(
            text = type.subtitle,
            style = DSTypo.Caption2.copy(fontFamily = paceDreamFontFamily),
            color = PaceDreamColors.Gray500,
            textAlign = TextAlign.Center,
            maxLines = 2
        )
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
        modifier = Modifier.width(260.dp),
        shape = RoundedCornerShape(PaceDreamRadius.LG),
        color = Color.White,
        shadowElevation = 2.dp
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
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
                        .height(14.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(shimmerBrush)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.55f)
                        .height(12.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(shimmerBrush)
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Box(
                        modifier = Modifier
                            .width(60.dp)
                            .height(14.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(shimmerBrush)
                    )
                    Box(
                        modifier = Modifier
                            .width(40.dp)
                            .height(14.dp)
                            .clip(RoundedCornerShape(4.dp))
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
