package com.shourov.apps.pacedream.feature.host.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import com.pacedream.common.icon.PaceDreamIcons
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pacedream.common.composables.components.*
import com.pacedream.common.composables.theme.*
import com.shourov.apps.pacedream.feature.host.presentation.components.*
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "My Listings",
                            style = PaceDreamTypography.Title1,
                            color = PaceDreamColors.TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${uiState.listings.size} properties listed",
                            style = PaceDreamTypography.Caption,
                            color = PaceDreamColors.TextSecondary
                        )
                    }
                },
                actions = {
                    FilledIconButton(
                        onClick = onAddListingClick,
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = PaceDreamColors.HostAccent
                        ),
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = PaceDreamIcons.Add,
                            contentDescription = "Add Listing",
                            tint = Color.White,
                            modifier = Modifier.size(PaceDreamIconSize.SM)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PaceDreamColors.Background)
            )
        },
        containerColor = PaceDreamColors.Background
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(bottom = PaceDreamSpacing.XXL)
        ) {
            // Filter Section
            item {
                ListingsFilterSection(
                    selectedFilter = uiState.selectedFilter,
                    selectedSort = uiState.selectedSort,
                    onFilterChanged = { viewModel.updateFilter(it) },
                    onSortChanged = { viewModel.updateSort(it) }
                )
            }

            // Content
            if (uiState.listings.isEmpty()) {
                item {
                    HostEmptyState(
                        icon = PaceDreamIcons.Home,
                        title = "No listings yet",
                        subtitle = "Start earning by listing your space on PaceDream",
                        ctaLabel = "Add Your First Listing",
                        onCta = onAddListingClick
                    )
                }
            } else {
                items(uiState.listings) { listing ->
                    HostListingCard(
                        listing = listing,
                        onListingClick = onListingClick,
                        onEditClick = onEditListingClick,
                        onDeleteClick = onDeleteListingClick
                    )
                }
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
        modifier = Modifier.padding(horizontal = PaceDreamSpacing.MD)
    ) {
        // Filter Chips
        HostSectionHeader(title = "Status")
        Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))

        val filters = listOf("All", "Active", "Pending", "Unavailable")
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(PaceDreamSpacing.SM)
        ) {
            items(filters) { filter ->
                HostFilterChip(
                    label = filter,
                    selected = selectedFilter == filter,
                    onClick = { onFilterChanged(filter) }
                )
            }
        }

        Spacer(modifier = Modifier.height(PaceDreamSpacing.MD))

        // Sort Options
        HostSectionHeader(title = "Sort by")
        Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))

        val sortOptions = listOf("Newest", "Oldest", "Price (High)", "Price (Low)", "Rating")
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(PaceDreamSpacing.SM)
        ) {
            items(sortOptions) { sort ->
                HostFilterChip(
                    label = sort,
                    selected = selectedSort == sort,
                    onClick = { onSortChanged(sort) }
                )
            }
        }

        Spacer(modifier = Modifier.height(PaceDreamSpacing.MD))
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
            .padding(horizontal = PaceDreamSpacing.MD, vertical = PaceDreamSpacing.XS),
        onClick = { onListingClick(listing.id) },
        shape = RoundedCornerShape(PaceDreamRadius.LG),
        colors = CardDefaults.cardColors(containerColor = PaceDreamColors.Card),
        elevation = CardDefaults.cardElevation(defaultElevation = PaceDreamElevation.XS)
    ) {
        Column {
            // Property Image
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp),
                contentAlignment = Alignment.Center,
            ) {
                if (listing.images.isNotEmpty()) {
                    AsyncImage(
                        model = listing.images.first(),
                        contentDescription = listing.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        PaceDreamColors.HostAccent.copy(alpha = 0.08f),
                                        PaceDreamColors.HostAccent.copy(alpha = 0.03f)
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = PaceDreamIcons.Home,
                            contentDescription = "Property",
                            tint = PaceDreamColors.HostAccent.copy(alpha = 0.3f),
                            modifier = Modifier.size(PaceDreamIconSize.XXL)
                        )
                    }
                }

                // Status badge overlay
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(PaceDreamSpacing.SM)
                ) {
                    val statusColor = if (listing.isAvailable) PaceDreamColors.HostAccent else PaceDreamColors.TextSecondary
                    Text(
                        text = if (listing.isAvailable) "Active" else "Unavailable",
                        style = PaceDreamTypography.Caption2.copy(fontWeight = FontWeight.SemiBold),
                        color = Color.White,
                        modifier = Modifier
                            .clip(RoundedCornerShape(PaceDreamRadius.Round))
                            .background(statusColor)
                            .padding(horizontal = PaceDreamSpacing.SM, vertical = PaceDreamSpacing.XS)
                    )
                }
            }

            Column(modifier = Modifier.padding(PaceDreamSpacing.MD)) {
                // Title and actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = listing.title,
                        style = PaceDreamTypography.Headline.copy(fontWeight = FontWeight.SemiBold),
                        color = PaceDreamColors.TextPrimary,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(PaceDreamSpacing.XS)) {
                        IconButton(
                            onClick = { onEditClick(listing.id) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = PaceDreamIcons.Edit,
                                contentDescription = "Edit",
                                tint = PaceDreamColors.HostAccent,
                                modifier = Modifier.size(PaceDreamIconSize.XS)
                            )
                        }
                        IconButton(
                            onClick = { onDeleteClick(listing.id) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = PaceDreamIcons.Delete,
                                contentDescription = "Delete",
                                tint = PaceDreamColors.Error,
                                modifier = Modifier.size(PaceDreamIconSize.XS)
                            )
                        }
                    }
                }

                // Location
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = PaceDreamIcons.LocationOn,
                        contentDescription = null,
                        tint = PaceDreamColors.TextTertiary,
                        modifier = Modifier.size(PaceDreamIconSize.XS)
                    )
                    Spacer(modifier = Modifier.width(PaceDreamSpacing.XS))
                    Text(
                        text = "${listing.location.city}, ${listing.location.state}".trim(' ', ','),
                        style = PaceDreamTypography.Caption,
                        color = PaceDreamColors.TextSecondary
                    )
                }

                Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))

                // Price and Rating
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "$${listing.pricing.basePrice.toInt()}/${listing.pricing.unit.ifBlank { "hr" }}",
                        style = PaceDreamTypography.Headline.copy(fontWeight = FontWeight.Bold),
                        color = PaceDreamColors.HostAccent
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = PaceDreamIcons.Star,
                            contentDescription = null,
                            tint = PaceDreamColors.Warning,
                            modifier = Modifier.size(PaceDreamIconSize.XS)
                        )
                        Spacer(modifier = Modifier.width(PaceDreamSpacing.XS))
                        Text(
                            text = String.format("%.1f", listing.rating),
                            style = PaceDreamTypography.Callout.copy(fontWeight = FontWeight.SemiBold),
                            color = PaceDreamColors.TextPrimary
                        )
                    }
                }
            }
        }
    }
}
