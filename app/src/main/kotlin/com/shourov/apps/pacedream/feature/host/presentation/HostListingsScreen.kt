package com.shourov.apps.pacedream.feature.host.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pacedream.common.composables.components.*
import com.pacedream.common.composables.theme.*
import com.shourov.apps.pacedream.feature.host.data.HostListingsData
import com.shourov.apps.pacedream.model.Property

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HostListingsScreen(
    onAddListingClick: () -> Unit = {},
    onListingClick: (String) -> Unit = {},
    onEditListingClick: (String) -> Unit = {},
    onDeleteListingClick: (String) -> Unit = {},
    viewModel: HostListingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(PaceDreamColors.Background)
    ) {
        // Listings Header
        item {
            ListingsHeader(
                totalListings = uiState.listings.size,
                onAddListingClick = onAddListingClick
            )
        }
        
        // Filter and Sort Section
        item {
            Spacer(modifier = Modifier.height(PaceDreamSpacing.LG))
            ListingsFilterSection(
                selectedFilter = uiState.selectedFilter,
                selectedSort = uiState.selectedSort,
                onFilterChanged = { viewModel.updateFilter(it) },
                onSortChanged = { viewModel.updateSort(it) }
            )
        }
        
        // Listings Content
        item {
            Spacer(modifier = Modifier.height(PaceDreamSpacing.LG))
            if (uiState.listings.isEmpty()) {
                EmptyListingsState(onAddListingClick = onAddListingClick)
            } else {
                ListingsContent(
                    listings = uiState.listings,
                    onListingClick = onListingClick,
                    onEditListingClick = onEditListingClick,
                    onDeleteListingClick = onDeleteListingClick
                )
            }
        }
    }
}

@Composable
fun ListingsHeader(
    totalListings: Int,
    onAddListingClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                    colors = listOf(
                        PaceDreamColors.Primary,
                        PaceDreamColors.Primary.copy(alpha = 0.9f)
                    )
                )
            )
            .padding(PaceDreamSpacing.LG)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "My Listings",
                    style = PaceDreamTypography.Title1,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                
                Text(
                    text = "$totalListings properties listed",
                    style = PaceDreamTypography.Body,
                    color = Color.White.copy(alpha = 0.9f)
                )
            }
            
            FloatingActionButton(
                onClick = onAddListingClick,
                containerColor = Color.White,
                contentColor = PaceDreamColors.Primary
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Listing"
                )
            }
        }
    }
}

@Composable
fun ListingsFilterSection(
    selectedFilter: String,
    selectedSort: String,
    onFilterChanged: (String) -> Unit,
    onSortChanged: (String) -> Unit
) {
    Column(
        modifier = Modifier.padding(horizontal = PaceDreamSpacing.LG)
    ) {
        // Filter Chips
        Text(
            text = "Filter by Status",
            style = PaceDreamTypography.Title3,
            color = PaceDreamColors.TextPrimary,
            fontWeight = FontWeight.SemiBold
        )
        
        Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))
        
        val filters = listOf("All", "Active", "Pending", "Unavailable")
        
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(PaceDreamSpacing.SM)
        ) {
            items(filters) { filter ->
                FilterChip(
                    selected = selectedFilter == filter,
                    onClick = { onFilterChanged(filter) },
                    label = {
                        Text(
                            text = filter,
                            style = PaceDreamTypography.Callout,
                            fontWeight = if (selectedFilter == filter) FontWeight.SemiBold else FontWeight.Normal
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = PaceDreamColors.Primary,
                        selectedLabelColor = Color.White,
                        containerColor = PaceDreamColors.Card,
                        labelColor = PaceDreamColors.TextPrimary
                    )
                )
            }
        }
        
        Spacer(modifier = Modifier.height(PaceDreamSpacing.MD))
        
        // Sort Options
        Text(
            text = "Sort by",
            style = PaceDreamTypography.Title3,
            color = PaceDreamColors.TextPrimary,
            fontWeight = FontWeight.SemiBold
        )
        
        Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))
        
        val sortOptions = listOf("Date (Newest)", "Date (Oldest)", "Price (High-Low)", "Price (Low-High)", "Rating")
        
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(PaceDreamSpacing.SM)
        ) {
            items(sortOptions) { sort ->
                FilterChip(
                    selected = selectedSort == sort,
                    onClick = { onSortChanged(sort) },
                    label = {
                        Text(
                            text = sort,
                            style = PaceDreamTypography.Callout,
                            fontWeight = if (selectedSort == sort) FontWeight.SemiBold else FontWeight.Normal
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = PaceDreamColors.Primary,
                        selectedLabelColor = Color.White,
                        containerColor = PaceDreamColors.Card,
                        labelColor = PaceDreamColors.TextPrimary
                    )
                )
            }
        }
    }
}

@Composable
fun EmptyListingsState(
    onAddListingClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = PaceDreamSpacing.LG)
            .clip(RoundedCornerShape(PaceDreamRadius.LG)),
        colors = CardDefaults.cardColors(containerColor = PaceDreamColors.Card),
        elevation = CardDefaults.cardElevation(defaultElevation = PaceDreamElevation.MD)
    ) {
        Column(
            modifier = Modifier.padding(PaceDreamSpacing.XXXL),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Home,
                contentDescription = "No listings",
                tint = PaceDreamColors.TextSecondary,
                modifier = Modifier.size(80.dp)
            )
            
            Spacer(modifier = Modifier.height(PaceDreamSpacing.LG))
            
            Text(
                text = "No listings yet",
                style = PaceDreamTypography.Title2,
                color = PaceDreamColors.TextPrimary,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))
            
            Text(
                text = "Start earning by listing your space on PaceDream. Share your property with travelers and start generating income.",
                style = PaceDreamTypography.Body,
                color = PaceDreamColors.TextSecondary,
                modifier = Modifier.fillMaxWidth(0.8f),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(PaceDreamSpacing.LG))
            
            Button(
                onClick = onAddListingClick,
                colors = ButtonDefaults.buttonColors(containerColor = PaceDreamColors.Primary),
                modifier = Modifier.fillMaxWidth(0.6f)
            ) {
                Text(
                    text = "Add Your First Listing",
                    style = PaceDreamTypography.Headline,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
fun ListingsContent(
    listings: List<Property>,
    onListingClick: (String) -> Unit,
    onEditListingClick: (String) -> Unit,
    onDeleteListingClick: (String) -> Unit
) {
    Column(
        modifier = Modifier.padding(horizontal = PaceDreamSpacing.LG)
    ) {
        Text(
            text = "Your Properties",
            style = PaceDreamTypography.Title3,
            color = PaceDreamColors.TextPrimary,
            fontWeight = FontWeight.SemiBold
        )
        
        Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))
        
        listings.forEach { listing ->
            HostListingCard(
                listing = listing,
                onListingClick = onListingClick,
                onEditClick = onEditListingClick,
                onDeleteClick = onDeleteListingClick
            )
            Spacer(modifier = Modifier.height(PaceDreamSpacing.MD))
        }
    }
}

@Composable
fun HostListingCard(
    listing: Property,
    onListingClick: (String) -> Unit,
    onEditClick: (String) -> Unit,
    onDeleteClick: (String) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(PaceDreamRadius.LG)),
        colors = CardDefaults.cardColors(containerColor = PaceDreamColors.Card),
        elevation = CardDefaults.cardElevation(defaultElevation = PaceDreamElevation.SM)
    ) {
        Column {
            // Property Image
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .background(PaceDreamColors.Primary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Home,
                    contentDescription = "Property",
                    tint = PaceDreamColors.Primary,
                    modifier = Modifier.size(60.dp)
                )
            }
            
            Column(
                modifier = Modifier.padding(PaceDreamSpacing.MD)
            ) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = listing.title,
                        style = PaceDreamTypography.Headline,
                        color = PaceDreamColors.TextPrimary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(PaceDreamSpacing.SM)
                    ) {
                        IconButton(
                            onClick = { onEditClick(listing.id) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit",
                                tint = PaceDreamColors.Primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        
                        IconButton(
                            onClick = { onDeleteClick(listing.id) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete",
                                tint = PaceDreamColors.Error,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))
                
                // Location
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = "Location",
                        tint = PaceDreamColors.TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                    
                    Spacer(modifier = Modifier.width(PaceDreamSpacing.XS))
                    
                    Text(
                        text = "${listing.location.city}, ${listing.location.country}",
                        style = PaceDreamTypography.Body,
                        color = PaceDreamColors.TextSecondary
                    )
                }
                
                Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))
                
                // Stats Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Price
                    Text(
                        text = "$${listing.pricing.basePrice.toInt()}/hour",
                        style = PaceDreamTypography.Title3,
                        color = PaceDreamColors.Primary,
                        fontWeight = FontWeight.Bold
                    )
                    
                    // Rating
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Rating",
                            tint = PaceDreamColors.Warning,
                            modifier = Modifier.size(16.dp)
                        )
                        
                        Spacer(modifier = Modifier.width(PaceDreamSpacing.XS))
                        
                        Text(
                            text = String.format("%.1f", listing.rating),
                            style = PaceDreamTypography.Body,
                            color = PaceDreamColors.TextPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))
                
                // Status and Action Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PaceDreamStatusChip(
                        status = if (listing.isAvailable) "Active" else "Unavailable",
                        isActive = listing.isAvailable
                    )
                    
                    TextButton(
                        onClick = { onListingClick(listing.id) }
                    ) {
                        Text(
                            text = "View Details",
                            style = PaceDreamTypography.Callout,
                            color = PaceDreamColors.Primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}
