package com.pacedream.app.feature.roommate

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import com.pacedream.common.icon.PaceDreamIcons
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
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
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Find Roommate",
                        style = PaceDreamTypography.Headline
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = PaceDreamIcons.ArrowBack,
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
                        Icon(PaceDreamIcons.Search, contentDescription = null)
                    },
                    trailingIcon = {
                        IconButton(onClick = {
                            scope.launch { snackbarHostState.showSnackbar("Advanced filters coming soon") }
                        }) {
                            Icon(PaceDreamIcons.FilterList, contentDescription = "Filters")
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
                            imageVector = PaceDreamIcons.People,
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

            // Empty state - will connect to roommate API when available
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(PaceDreamRadius.LG),
                    colors = CardDefaults.cardColors(
                        containerColor = PaceDreamColors.Card
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(PaceDreamSpacing.XL),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = PaceDreamIcons.People,
                            contentDescription = null,
                            tint = PaceDreamColors.TextTertiary,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(PaceDreamSpacing.MD))
                        Text(
                            text = "No roommate listings yet",
                            style = PaceDreamTypography.Headline,
                            fontWeight = FontWeight.SemiBold,
                            color = PaceDreamColors.TextPrimary
                        )
                        Spacer(modifier = Modifier.height(PaceDreamSpacing.XS))
                        Text(
                            text = "Be the first to post a roommate listing in your area",
                            style = PaceDreamTypography.Caption,
                            color = PaceDreamColors.TextSecondary,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }

            // Post a listing CTA
            item {
                Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))
                Button(
                    onClick = {
                        scope.launch { snackbarHostState.showSnackbar("Roommate listings coming soon") }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(PaceDreamRadius.MD),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PaceDreamColors.Primary
                    ),
                    contentPadding = PaddingValues(vertical = 14.dp)
                ) {
                    Icon(PaceDreamIcons.People, contentDescription = null)
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

// RoommateListingCard removed - was showing hardcoded sample data.
// Will be replaced with real API-driven listing cards when the roommate API is available.
