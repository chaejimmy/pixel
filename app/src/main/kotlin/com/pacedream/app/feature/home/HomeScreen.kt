package com.pacedream.app.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage

/**
 * HomeScreen - Main dashboard with 3 sections
 * 
 * iOS Parity:
 * - 3 sections: Hourly Spaces, Rent Gear, Split Stays
 * - Fetch sections concurrently
 * - Hide sections that fail
 * - Show inline warning banner if some sections failed
 * - Pull to refresh
 * - View All navigates to section list
 */
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
            // Hero Section
            item {
                HeroSection(
                    onSearchClick = onSearchClick,
                    onFilterClick = { /* TODO: Open filters */ },
                    heroImageUrl = uiState.heroImageUrl
                )
            }
            
            // Category Filter Buttons
            item {
                CategoryFilterButtons(
                    selectedCategory = selectedCategoryFilter,
                    onCategorySelected = { category ->
                        selectedCategoryFilter = category
                        onCategoryFilterClick(category)
                    },
                    modifier = Modifier.padding(vertical = 16.dp)
                )
            }
            
            // Categories Section - Always visible
            item {
                CategoriesSection(
                    onViewAllClick = { onSectionViewAll("categories") },
                    onCategoryClick = onCategoryClick,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
            
            // Warning banner if some sections failed
            if (uiState.hasErrors) {
                item {
                    WarningBanner(
                        message = "Some content couldn't load. Pull to refresh.",
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
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
            
            // Empty state if all sections are empty
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

/**
 * Hero Section with background image, overlay text, and search bar
 * Matches iOS design
 */
@Composable
private fun HeroSection(
    onSearchClick: () -> Unit,
    onFilterClick: () -> Unit,
    heroImageUrl: String? = null
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(320.dp)
    ) {
        // Background Image - use actual image if provided, otherwise use gradient
        if (heroImageUrl != null) {
            AsyncImage(
                model = heroImageUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            // Fallback gradient (can be replaced with actual hero image URL from API/config)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF1E88E5),
                                Color(0xFF1976D2)
                            )
                        )
                    )
            )
        }
        
        // Dark overlay for better text readability
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.3f),
                            Color.Black.copy(alpha = 0.5f)
                        )
                    )
                )
        )
        
        // Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 60.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Hero Text
            Column {
                Text(
                    text = "One place to share it all",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    lineHeight = MaterialTheme.typography.displaySmall.lineHeight
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Book, share, or split stays, time, and spaces—on one platform.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White.copy(alpha = 0.9f)
                )
                Spacer(modifier = Modifier.height(24.dp))
                // Get to know PaceDream CTA Button
                Button(
                    onClick = { /* TODO: Navigate to about/onboarding */ },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF5527D7), // PaceDream Primary
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Get to know PaceDream",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            
            // Search Bar
            SearchBarCard(
                onSearchClick = onSearchClick,
                onFilterClick = onFilterClick
            )
        }
    }
}

/**
 * Search Bar Card
 */
@Composable
private fun SearchBarCard(
    onSearchClick: () -> Unit,
    onFilterClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSearchClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Search",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Search anywhere",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Share • Borrow • Split",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onFilterClick) {
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = "Filters",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Category Filter Buttons (All, Restroom, Nap Pod, etc.)
 */
@Composable
private fun CategoryFilterButtons(
    selectedCategory: String,
    onCategorySelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val categories = listOf(
        "All" to Icons.Default.Apps, // Using Apps icon instead of GridView
        "Restroom" to Icons.Default.Wc,
        "Nap Pod" to Icons.Default.Bed,
        "Meeting Room" to Icons.Default.Business,
        "Study Room" to Icons.Default.School,
        "Short Stay" to Icons.Default.Hotel,
        "Apartment" to Icons.Default.Apartment,
        "Luxury Room" to Icons.Default.Star,
        "Parking" to Icons.Default.LocalParking,
        "Storage Space" to Icons.Default.Storage
    )
    
    LazyRow(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(categories) { (name, icon) ->
            FilterChip(
                selected = selectedCategory == name,
                onClick = { onCategorySelected(name) },
                label = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(name)
                    }
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    }
}

/**
 * Categories Section with large category cards
 */
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
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Categories",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            TextButton(onClick = onViewAllClick) {
                Text(
                    text = "View All",
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // Category Cards
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
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

/**
 * Category Card (large card with icon and title)
 */
@Composable
private fun CategoryCard(
    category: CategoryCardData,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(160.dp)
            .height(120.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = category.color
        )
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
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
                    modifier = Modifier.size(40.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = category.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

/**
 * Category Card Data
 */
private data class CategoryCardData(
    val name: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val color: Color
)

/**
 * Get category cards data
 */
private fun getCategoryCards(): List<CategoryCardData> {
    return listOf(
        CategoryCardData(
            name = "Rest Room",
            icon = Icons.Default.Bed,
            color = Color(0xFF2196F3) // Blue
        ),
        CategoryCardData(
            name = "Time-Based",
            icon = Icons.Default.Schedule,
            color = Color(0xFF9C27B0) // Purple
        ),
        CategoryCardData(
            name = "Storage",
            icon = Icons.Default.Storage,
            color = Color(0xFFFF9800) // Orange
        ),
        CategoryCardData(
            name = "Parking",
            icon = Icons.Default.LocalParking,
            color = Color(0xFF4CAF50) // Green
        ),
        CategoryCardData(
            name = "Meeting",
            icon = Icons.Default.Business,
            color = Color(0xFFF44336) // Red
        )
    )
}

@Composable
private fun WarningBanner(
    message: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
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
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
        }
    }
}

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
            .padding(vertical = 12.dp)
    ) {
        // Section Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clickable(onClick = onViewAllClick),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "View All",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // Items Row
        if (isLoading) {
            // Skeleton loading
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(5) {
                    SkeletonCard()
                }
            }
        } else {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
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

@Composable
private fun ListingCard(
    item: HomeListingItem,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(200.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column {
            AsyncImage(
                model = item.imageUrl,
                contentDescription = item.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
            )
            
            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                
                item.location?.let { location ->
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = location,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item.price?.let { price ->
                        Text(
                            text = price,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    
                    item.rating?.let { rating ->
                        Text(
                            text = "★ ${"%.1f".format(rating)}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SkeletonCard() {
    Card(
        modifier = Modifier.width(200.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            )
            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .height(16.dp)
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant,
                            RoundedCornerShape(4.dp)
                        )
                )
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.5f)
                        .height(12.dp)
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant,
                            RoundedCornerShape(4.dp)
                        )
                )
            }
        }
    }
}

@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "No listings available",
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Check back later for new spaces and rentals",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
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


