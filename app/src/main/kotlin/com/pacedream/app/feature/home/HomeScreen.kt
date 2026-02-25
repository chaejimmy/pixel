package com.pacedream.app.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import com.pacedream.common.icon.PaceDreamIcons
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.pacedream.common.composables.theme.paceDreamDisplayFontFamily
import com.pacedream.common.composables.theme.paceDreamFontFamily

/**
 * HomeScreen - Modernized homepage inspired by Airbnb/Turo design language.
 *
 * Key design features:
 * - Immersive hero with gradient overlay and bold display typography
 * - Airbnb-style pill search bar floating between hero and content
 * - Icon-above-label category tabs with underline indicator
 * - Clean section layout with modern card treatment
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel(),
    onSectionViewAll: (String) -> Unit,
    onListingClick: (HomeListingItem) -> Unit,
    onSearchClick: () -> Unit = {},
    onCategoryClick: (String) -> Unit = {},
    onCategoryFilterClick: (String) -> Unit = {},
    onNotificationsClick: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedCategoryFilter by remember { mutableStateOf("All") }

    PullToRefreshBox(
        isRefreshing = uiState.isRefreshing,
        onRefresh = { viewModel.refresh() },
        modifier = Modifier.fillMaxSize()
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            // Hero + floating search bar
            item {
                HeroSection(
                    onSearchClick = onSearchClick,
                    onFilterClick = { /* TODO: Open filters */ },
                    onNotificationsClick = onNotificationsClick,
                    heroImageUrl = uiState.heroImageUrl
                )
            }

            // Category Filter Tabs (Airbnb-style icon tabs)
            item {
                CategoryFilterTabs(
                    selectedCategory = selectedCategoryFilter,
                    onCategorySelected = { category ->
                        selectedCategoryFilter = category
                        onCategoryFilterClick(category)
                    },
                    modifier = Modifier.padding(top = 20.dp, bottom = 8.dp)
                )
            }

            // Categories Section
            item {
                CategoriesSection(
                    onViewAllClick = { onSectionViewAll("categories") },
                    onCategoryClick = onCategoryClick,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            // Warning banner
            if (uiState.hasErrors) {
                item {
                    WarningBanner(
                        message = "Some content couldn't load. Pull to refresh.",
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                    )
                }
            }

            // Hourly Spaces Section
            if (uiState.hourlySpaces.isNotEmpty() || uiState.isLoadingHourlySpaces) {
                item {
                    HomeSection(
                        title = "Hourly Spaces",
                        subtitle = "Find flexible spaces for short stays",
                        items = uiState.hourlySpaces,
                        isLoading = uiState.isLoadingHourlySpaces,
                        onViewAllClick = { onSectionViewAll("hourly-spaces") },
                        onItemClick = onListingClick
                    )
                }
            }

            // Rent Gear Section
            if (uiState.rentGear.isNotEmpty() || uiState.isLoadingRentGear) {
                item {
                    HomeSection(
                        title = "Rent Gear",
                        subtitle = "Equipment and tools for every need",
                        items = uiState.rentGear,
                        isLoading = uiState.isLoadingRentGear,
                        onViewAllClick = { onSectionViewAll("rent-gear") },
                        onItemClick = onListingClick
                    )
                }
            }

            // Split Stays Section
            if (uiState.splitStays.isNotEmpty() || uiState.isLoadingSplitStays) {
                item {
                    HomeSection(
                        title = "Split Stays",
                        subtitle = "Flexible long-term rentals",
                        items = uiState.splitStays,
                        isLoading = uiState.isLoadingSplitStays,
                        onViewAllClick = { onSectionViewAll("split-stays") },
                        onItemClick = onListingClick
                    )
                }
            }

            // Empty state
            if (!uiState.isLoading && uiState.isEmpty) {
                item {
                    EmptyState(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp)
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Hero Section
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun HeroSection(
    onSearchClick: () -> Unit,
    onFilterClick: () -> Unit,
    onNotificationsClick: () -> Unit = {},
    heroImageUrl: String? = null
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
    ) {
        // Hero background + content
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp)
        ) {
            // Background
            if (heroImageUrl != null) {
                AsyncImage(
                    model = heroImageUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFF1A1A2E),
                                    Color(0xFF16213E),
                                    Color(0xFF0F3460)
                                )
                            )
                        )
                )
            }

            // Gradient overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.15f),
                                Color.Black.copy(alpha = 0.55f)
                            )
                        )
                    )
            )

            // Notification bell icon - top right
            IconButton(
                onClick = onNotificationsClick,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 40.dp, end = 12.dp)
                    .zIndex(2f)
            ) {
                Icon(
                    imageVector = PaceDreamIcons.Notifications,
                    contentDescription = "Notifications",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }

            // Hero text content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 24.dp, end = 24.dp, top = 56.dp, bottom = 56.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "One place to\nshare it all",
                    style = TextStyle(
                        fontFamily = paceDreamDisplayFontFamily,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 38.sp,
                        letterSpacing = (-0.5).sp
                    ),
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Book, share, or split stays and spaces.",
                    style = TextStyle(
                        fontFamily = paceDreamFontFamily,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Normal,
                        lineHeight = 22.sp,
                        letterSpacing = (-0.2).sp
                    ),
                    color = Color.White.copy(alpha = 0.85f)
                )
            }
        }

        // Floating search bar - overlaps hero bottom edge
        FloatingSearchBar(
            onSearchClick = onSearchClick,
            onFilterClick = onFilterClick,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .offset(y = 24.dp)
                .padding(horizontal = 20.dp)
                .zIndex(1f)
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Floating Search Bar (Airbnb-style pill)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun FloatingSearchBar(
    onSearchClick: () -> Unit,
    onFilterClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 12.dp,
                shape = RoundedCornerShape(28.dp),
                ambientColor = Color.Black.copy(alpha = 0.08f),
                spotColor = Color.Black.copy(alpha = 0.12f)
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onSearchClick
            ),
        shape = RoundedCornerShape(28.dp),
        color = Color.White,
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = PaceDreamIcons.Search,
                contentDescription = "Search",
                tint = Color(0xFF222222),
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Where to?",
                    style = TextStyle(
                        fontFamily = paceDreamFontFamily,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = (-0.2).sp
                    ),
                    color = Color(0xFF222222)
                )
                Text(
                    text = "Anywhere \u00B7 Any time \u00B7 Any type",
                    style = TextStyle(
                        fontFamily = paceDreamFontFamily,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Normal,
                        letterSpacing = 0.sp
                    ),
                    color = Color(0xFF717171)
                )
            }
            // Filter button
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .border(
                        width = 1.dp,
                        color = Color(0xFFDDDDDD),
                        shape = CircleShape
                    )
                    .clip(CircleShape)
                    .clickable(onClick = onFilterClick),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = PaceDreamIcons.Tune,
                    contentDescription = "Filters",
                    tint = Color(0xFF222222),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Category Filter Tabs (Airbnb-style: icon on top, label below, underline)
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

    LazyRow(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(24.dp)
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
}

@Composable
private fun CategoryTab(
    name: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
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
            .padding(vertical = 8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = name,
            tint = if (isSelected) Color(0xFF222222) else Color(0xFF717171),
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = name,
            style = TextStyle(
                fontFamily = paceDreamFontFamily,
                fontSize = 11.sp,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                letterSpacing = 0.sp
            ),
            color = if (isSelected) Color(0xFF222222) else Color(0xFF717171),
            maxLines = 1
        )
        Spacer(modifier = Modifier.height(8.dp))
        // Active indicator bar
        Box(
            modifier = Modifier
                .width(28.dp)
                .height(2.dp)
                .background(
                    color = if (isSelected) Color(0xFF222222) else Color.Transparent,
                    shape = RoundedCornerShape(1.dp)
                )
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Categories Section
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun CategoriesSection(
    onViewAllClick: () -> Unit,
    onCategoryClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
    ) {
        // Section Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Explore Categories",
                style = TextStyle(
                    fontFamily = paceDreamDisplayFontFamily,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.3).sp
                ),
                color = Color(0xFF222222)
            )
            TextButton(onClick = onViewAllClick) {
                Text(
                    text = "See all",
                    style = TextStyle(
                        fontFamily = paceDreamFontFamily,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.sp
                    ),
                    color = Color(0xFF222222),
                    textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Category Cards
        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(getCategoryCards()) { category ->
                CategoryCard(
                    category = category,
                    onClick = { onCategoryClick(category.name) }
                )
            }
        }
    }
}

@Composable
private fun CategoryCard(
    category: CategoryCardData,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(140.dp)
            .height(110.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        colors = category.gradientColors
                    ),
                    shape = RoundedCornerShape(16.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = category.icon,
                    contentDescription = category.name,
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = category.name,
                    style = TextStyle(
                        fontFamily = paceDreamFontFamily,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = (-0.1).sp
                    ),
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

private data class CategoryCardData(
    val name: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val gradientColors: List<Color>
)

private fun getCategoryCards(): List<CategoryCardData> {
    return listOf(
        CategoryCardData(
            name = "Entire Home",
            icon = PaceDreamIcons.Home,
            gradientColors = listOf(Color(0xFFEF4444), Color(0xFFDC2626))
        ),
        CategoryCardData(
            name = "Private Room",
            icon = PaceDreamIcons.MeetingRoom,
            gradientColors = listOf(Color(0xFFEC4899), Color(0xFFDB2777))
        ),
        CategoryCardData(
            name = "Restroom",
            icon = PaceDreamIcons.Wc,
            gradientColors = listOf(Color(0xFF667eea), Color(0xFF764ba2))
        ),
        CategoryCardData(
            name = "Nap Pod",
            icon = PaceDreamIcons.Bed,
            gradientColors = listOf(Color(0xFF8B5CF6), Color(0xFF7C3AED))
        ),
        CategoryCardData(
            name = "Meeting Room",
            icon = PaceDreamIcons.Business,
            gradientColors = listOf(Color(0xFF3B82F6), Color(0xFF2563EB))
        ),
        CategoryCardData(
            name = "Workspace",
            icon = PaceDreamIcons.Laptop,
            gradientColors = listOf(Color(0xFF10B981), Color(0xFF059669))
        ),
        CategoryCardData(
            name = "EV Parking",
            icon = PaceDreamIcons.ElectricCar,
            gradientColors = listOf(Color(0xFFa855f7), Color(0xFF7c3aed))
        ),
        CategoryCardData(
            name = "Study Room",
            icon = PaceDreamIcons.School,
            gradientColors = listOf(Color(0xFF059669), Color(0xFF047857))
        ),
        CategoryCardData(
            name = "Short Stay",
            icon = PaceDreamIcons.Hotel,
            gradientColors = listOf(Color(0xFFF59E0B), Color(0xFFD97706))
        ),
        CategoryCardData(
            name = "Apartment",
            icon = PaceDreamIcons.Apartment,
            gradientColors = listOf(Color(0xFFDC2626), Color(0xFFB91C1C))
        ),
        CategoryCardData(
            name = "Parking",
            icon = PaceDreamIcons.LocalParking,
            gradientColors = listOf(Color(0xFF6366F1), Color(0xFF4F46E5))
        ),
        CategoryCardData(
            name = "Luxury Room",
            icon = PaceDreamIcons.Star,
            gradientColors = listOf(Color(0xFFD97706), Color(0xFFB45309))
        ),
        CategoryCardData(
            name = "Storage Space",
            icon = PaceDreamIcons.Storage,
            gradientColors = listOf(Color(0xFF4facfe), Color(0xFF00f2fe))
        )
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// Warning Banner
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun WarningBanner(
    message: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = PaceDreamIcons.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = message,
                style = TextStyle(
                    fontFamily = paceDreamFontFamily,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Normal
                ),
                color = MaterialTheme.colorScheme.onErrorContainer
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Home Section (listing rows)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun HomeSection(
    title: String,
    subtitle: String,
    items: List<HomeListingItem>,
    isLoading: Boolean,
    onViewAllClick: () -> Unit,
    onItemClick: (HomeListingItem) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp)
    ) {
        // Section Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .clickable(onClick = onViewAllClick),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = TextStyle(
                        fontFamily = paceDreamDisplayFontFamily,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.3).sp
                    ),
                    color = Color(0xFF222222)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = TextStyle(
                        fontFamily = paceDreamFontFamily,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Normal,
                        letterSpacing = (-0.1).sp
                    ),
                    color = Color(0xFF717171)
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Show all",
                    style = TextStyle(
                        fontFamily = paceDreamFontFamily,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.sp
                    ),
                    color = Color(0xFF222222),
                    textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = PaceDreamIcons.ArrowForward,
                    contentDescription = null,
                    tint = Color(0xFF222222),
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Items Row
        if (isLoading) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items(5) {
                    SkeletonCard()
                }
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
// Listing Card
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ListingCard(
    item: HomeListingItem,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(220.dp)
            .clickable(onClick = onClick)
    ) {
        // Image with rounded corners
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
                .clip(RoundedCornerShape(14.dp))
        ) {
            AsyncImage(
                model = item.imageUrl,
                contentDescription = item.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            // Rating badge
            item.rating?.let { rating ->
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(10.dp),
                    shape = RoundedCornerShape(8.dp),
                    color = Color.White
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = PaceDreamIcons.Star,
                            contentDescription = null,
                            tint = Color(0xFF222222),
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "%.1f".format(rating),
                            style = TextStyle(
                                fontFamily = paceDreamFontFamily,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = Color(0xFF222222)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Title
        Text(
            text = item.title,
            style = TextStyle(
                fontFamily = paceDreamFontFamily,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = (-0.2).sp
            ),
            color = Color(0xFF222222),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        // Location
        item.location?.let { location ->
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = location,
                style = TextStyle(
                    fontFamily = paceDreamFontFamily,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Normal
                ),
                color = Color(0xFF717171),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Price
        item.price?.let { price ->
            Spacer(modifier = Modifier.height(4.dp))
            Row {
                Text(
                    text = price,
                    style = TextStyle(
                        fontFamily = paceDreamFontFamily,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.1).sp
                    ),
                    color = Color(0xFF222222)
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Skeleton Card
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SkeletonCard() {
    Column(modifier = Modifier.width(220.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
                .background(
                    Color(0xFFF0F0F0),
                    RoundedCornerShape(14.dp)
                )
        )
        Spacer(modifier = Modifier.height(10.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth(0.75f)
                .height(14.dp)
                .background(
                    Color(0xFFF0F0F0),
                    RoundedCornerShape(4.dp)
                )
        )
        Spacer(modifier = Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth(0.5f)
                .height(12.dp)
                .background(
                    Color(0xFFF0F0F0),
                    RoundedCornerShape(4.dp)
                )
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Empty State
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "No listings available",
            style = TextStyle(
                fontFamily = paceDreamDisplayFontFamily,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = (-0.2).sp
            ),
            color = Color(0xFF222222)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Check back later for new spaces and rentals",
            style = TextStyle(
                fontFamily = paceDreamFontFamily,
                fontSize = 15.sp,
                fontWeight = FontWeight.Normal
            ),
            color = Color(0xFF717171)
        )
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
