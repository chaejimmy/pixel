package com.pacedream.app.feature.roommate

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.pacedream.common.composables.theme.PaceDreamColors
import com.pacedream.common.composables.theme.PaceDreamRadius
import com.pacedream.common.composables.theme.PaceDreamSpacing
import com.pacedream.common.composables.theme.PaceDreamTypography

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoommateFinderScreen(
    onBackClick: () -> Unit,
    onListingClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("All") }
    val filterOptions = listOf("All", "Looking", "Offering", "Near Me")

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Find Roommate",
                        style = PaceDreamTypography.Title2,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = PaceDreamColors.Background
                )
            )
        },
        containerColor = PaceDreamColors.Background
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(PaceDreamSpacing.LG),
            verticalArrangement = Arrangement.spacedBy(PaceDreamSpacing.MD)
        ) {
            // Search Bar
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search by location, budget...") },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null)
                    },
                    trailingIcon = {
                        IconButton(onClick = { /* TODO: Open advanced filters */ }) {
                            Icon(Icons.Default.FilterList, contentDescription = "Filters")
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(PaceDreamRadius.LG),
                    singleLine = true
                )
            }

            // Filter Chips
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(PaceDreamSpacing.SM)
                ) {
                    items(filterOptions) { filter ->
                        FilterChip(
                            selected = selectedFilter == filter,
                            onClick = { selectedFilter = filter },
                            label = { Text(filter) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PaceDreamColors.Primary,
                                selectedLabelColor = PaceDreamColors.OnPrimary
                            )
                        )
                    }
                }
            }

            // Info card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(PaceDreamRadius.LG),
                    colors = CardDefaults.cardColors(
                        containerColor = PaceDreamColors.Primary.copy(alpha = 0.08f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(PaceDreamSpacing.MD),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.People,
                            contentDescription = null,
                            tint = PaceDreamColors.Primary,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(PaceDreamSpacing.MD))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Find Your Perfect Roommate",
                                style = PaceDreamTypography.Headline,
                                fontWeight = FontWeight.SemiBold,
                                color = PaceDreamColors.TextPrimary
                            )
                            Text(
                                text = "Post listings or search for roommates by location, budget, lifestyle, and move-in dates. Connect with verified users.",
                                style = PaceDreamTypography.Caption,
                                color = PaceDreamColors.TextSecondary
                            )
                        }
                    }
                }
            }

            // Roommate Preferences Section
            item {
                Text(
                    text = "Filter by Preferences",
                    style = PaceDreamTypography.Title3,
                    fontWeight = FontWeight.Bold,
                    color = PaceDreamColors.TextPrimary
                )
                Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(PaceDreamSpacing.SM)
                ) {
                    RoommatePreferenceChip(
                        label = "Budget",
                        modifier = Modifier.weight(1f)
                    )
                    RoommatePreferenceChip(
                        label = "Location",
                        modifier = Modifier.weight(1f)
                    )
                    RoommatePreferenceChip(
                        label = "Move-in",
                        modifier = Modifier.weight(1f)
                    )
                    RoommatePreferenceChip(
                        label = "Lifestyle",
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Listings section
            item {
                Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))
                Text(
                    text = "Available Listings",
                    style = PaceDreamTypography.Title3,
                    fontWeight = FontWeight.Bold,
                    color = PaceDreamColors.TextPrimary
                )
            }

            // Placeholder listings - will connect to API when available
            items(5) { index ->
                RoommateListingCard(
                    index = index,
                    onClick = { onListingClick("roommate-$index") }
                )
            }

            // Post a listing CTA
            item {
                Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))
                Button(
                    onClick = { /* TODO: Navigate to post roommate listing */ },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(PaceDreamRadius.MD),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PaceDreamColors.Primary
                    ),
                    contentPadding = PaddingValues(vertical = 14.dp)
                ) {
                    Icon(Icons.Default.People, contentDescription = null)
                    Spacer(modifier = Modifier.width(PaceDreamSpacing.SM))
                    Text("Post a Roommate Listing", style = PaceDreamTypography.Button)
                }
            }
        }
    }
}

@Composable
private fun RoommatePreferenceChip(
    label: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(PaceDreamRadius.MD),
        color = PaceDreamColors.Card,
        shadowElevation = 1.dp
    ) {
        Text(
            text = label,
            style = PaceDreamTypography.Caption,
            fontWeight = FontWeight.Medium,
            color = PaceDreamColors.TextPrimary,
            modifier = Modifier.padding(horizontal = PaceDreamSpacing.SM, vertical = PaceDreamSpacing.SM),
            maxLines = 1
        )
    }
}

@Composable
private fun RoommateListingCard(
    index: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val locations = listOf("New York, NY", "San Francisco, CA", "Los Angeles, CA", "Chicago, IL", "Austin, TX")
    val budgets = listOf("$800/mo", "$1,200/mo", "$950/mo", "$700/mo", "$1,100/mo")
    val moveInDates = listOf("Immediately", "March 2026", "April 2026", "Flexible", "May 2026")
    val lifestyles = listOf("Quiet & Clean", "Social", "Pet-Friendly", "Early Riser", "Night Owl")

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(PaceDreamRadius.LG),
        colors = CardDefaults.cardColors(containerColor = PaceDreamColors.Card),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(PaceDreamSpacing.MD),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar placeholder
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(PaceDreamColors.Primary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.People,
                    contentDescription = null,
                    tint = PaceDreamColors.Primary,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.width(PaceDreamSpacing.MD))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Looking for a roommate",
                    style = PaceDreamTypography.Headline,
                    fontWeight = FontWeight.SemiBold,
                    color = PaceDreamColors.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(2.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.LocationOn,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = PaceDreamColors.TextSecondary
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = locations[index % locations.size],
                        style = PaceDreamTypography.Caption,
                        color = PaceDreamColors.TextSecondary
                    )
                }

                Spacer(modifier = Modifier.height(PaceDreamSpacing.XS))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(PaceDreamSpacing.SM)
                ) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = PaceDreamColors.Primary.copy(alpha = 0.1f)
                    ) {
                        Text(
                            text = budgets[index % budgets.size],
                            style = PaceDreamTypography.Caption,
                            fontWeight = FontWeight.Medium,
                            color = PaceDreamColors.Primary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = PaceDreamColors.TextSecondary.copy(alpha = 0.1f)
                    ) {
                        Text(
                            text = moveInDates[index % moveInDates.size],
                            style = PaceDreamTypography.Caption,
                            color = PaceDreamColors.TextSecondary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = PaceDreamColors.TextSecondary.copy(alpha = 0.1f)
                    ) {
                        Text(
                            text = lifestyles[index % lifestyles.size],
                            style = PaceDreamTypography.Caption,
                            color = PaceDreamColors.TextSecondary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }
}
