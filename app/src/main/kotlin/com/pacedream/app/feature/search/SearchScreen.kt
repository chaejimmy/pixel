package com.pacedream.app.feature.search

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import com.pacedream.common.icon.PaceDreamIcons
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.pacedream.common.composables.theme.PaceDreamColors
import com.pacedream.common.composables.theme.PaceDreamRadius
import com.pacedream.common.composables.theme.PaceDreamSpacing
import com.pacedream.common.composables.theme.PaceDreamTypography

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    viewModel: SearchViewModel = hiltViewModel(),
    onListingClick: (SearchResultItem) -> Unit = {},
    initialCategory: String? = null
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PaceDreamColors.Background)
    ) {
        // Search header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(PaceDreamColors.Background)
                .padding(horizontal = PaceDreamSpacing.MD, vertical = PaceDreamSpacing.MD)
        ) {
            Text(
                "Explore",
                style = PaceDreamTypography.Title1,
                fontWeight = FontWeight.Bold,
                color = PaceDreamColors.TextPrimary
            )
            Spacer(modifier = Modifier.height(PaceDreamSpacing.MD))

            // Modern search bar with filled background
            TextField(
                value = uiState.query,
                onValueChange = { viewModel.onQueryChanged(it) },
                placeholder = {
                    Text(
                        "Search spaces, gear, stays...",
                        style = PaceDreamTypography.Callout,
                        color = PaceDreamColors.TextTertiary
                    )
                },
                leadingIcon = {
                    Icon(
                        PaceDreamIcons.Search,
                        contentDescription = "Search",
                        tint = PaceDreamColors.TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                },
                trailingIcon = {
                    if (uiState.query.isNotBlank()) {
                        IconButton(onClick = { viewModel.onQueryChanged("") }) {
                            Icon(
                                PaceDreamIcons.Close,
                                contentDescription = "Clear",
                                tint = PaceDreamColors.TextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(PaceDreamRadius.MD),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { viewModel.search() }),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = PaceDreamColors.Surface,
                    unfocusedContainerColor = PaceDreamColors.Surface,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedTextColor = PaceDreamColors.TextPrimary,
                    unfocusedTextColor = PaceDreamColors.TextPrimary
                ),
                textStyle = PaceDreamTypography.Callout
            )

            Spacer(modifier = Modifier.height(PaceDreamSpacing.MD))

            // Segmented tab control
            val tabs = SearchTab.entries
            Surface(
                shape = RoundedCornerShape(PaceDreamRadius.MD),
                color = PaceDreamColors.Surface
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(PaceDreamSpacing.XS),
                    horizontalArrangement = Arrangement.spacedBy(PaceDreamSpacing.XS)
                ) {
                    tabs.forEach { tab ->
                        val isSelected = uiState.selectedTab == tab
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(PaceDreamRadius.SM))
                                .clickable { viewModel.onTabChanged(tab) },
                            shape = RoundedCornerShape(PaceDreamRadius.SM),
                            color = if (isSelected) PaceDreamColors.Card else Color.Transparent,
                            shadowElevation = if (isSelected) 1.dp else 0.dp
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.padding(
                                    horizontal = PaceDreamSpacing.MD,
                                    vertical = PaceDreamSpacing.SM
                                )
                            ) {
                                Text(
                                    tab.label,
                                    style = PaceDreamTypography.Subheadline,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                    color = if (isSelected) PaceDreamColors.Primary else PaceDreamColors.TextSecondary
                                )
                            }
                        }
                    }
                }
            }
        }

        // Category filter chips - modern pill style
        val categories = listOf(
            "Studio", "Meeting Room", "Podcast Studio", "Photo Studio",
            "Music Studio", "Event Space", "Camera", "Lighting", "Audio"
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = PaceDreamSpacing.MD, vertical = PaceDreamSpacing.SM),
            horizontalArrangement = Arrangement.spacedBy(PaceDreamSpacing.SM)
        ) {
            items(categories) { category ->
                FilterChip(
                    selected = uiState.selectedCategory == category,
                    onClick = {
                        viewModel.onCategoryChanged(
                            if (uiState.selectedCategory == category) null else category
                        )
                    },
                    label = {
                        Text(
                            category,
                            style = PaceDreamTypography.Caption,
                            fontWeight = if (uiState.selectedCategory == category) FontWeight.SemiBold else FontWeight.Normal
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = PaceDreamColors.Surface,
                        selectedContainerColor = PaceDreamColors.Primary.copy(alpha = 0.12f),
                        selectedLabelColor = PaceDreamColors.Primary,
                        labelColor = PaceDreamColors.TextSecondary
                    ),
                    shape = RoundedCornerShape(PaceDreamRadius.Round),
                    border = null
                )
            }
        }

        // Content area
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = PaceDreamColors.Primary,
                        strokeWidth = 2.dp
                    )
                }
            }

            uiState.errorMessage != null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(PaceDreamSpacing.XL),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            PaceDreamIcons.ErrorOutline,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = PaceDreamColors.TextTertiary
                        )
                        Spacer(modifier = Modifier.height(PaceDreamSpacing.MD))
                        Text(
                            "Something went wrong",
                            style = PaceDreamTypography.Headline,
                            fontWeight = FontWeight.SemiBold,
                            color = PaceDreamColors.TextPrimary
                        )
                        Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))
                        Text(
                            uiState.errorMessage ?: "",
                            style = PaceDreamTypography.Callout,
                            color = PaceDreamColors.TextSecondary
                        )
                    }
                }
            }

            uiState.results.isEmpty() && uiState.query.isNotBlank() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(PaceDreamSpacing.XL),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            PaceDreamIcons.Search,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = PaceDreamColors.TextTertiary
                        )
                        Spacer(modifier = Modifier.height(PaceDreamSpacing.MD))
                        Text(
                            "No results found",
                            style = PaceDreamTypography.Headline,
                            fontWeight = FontWeight.SemiBold,
                            color = PaceDreamColors.TextPrimary
                        )
                        Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))
                        Text(
                            "Try a different search term or category",
                            style = PaceDreamTypography.Callout,
                            color = PaceDreamColors.TextSecondary
                        )
                    }
                }
            }

            uiState.results.isEmpty() -> {
                // Empty state with suggestions
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(PaceDreamSpacing.XL),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Surface(
                            shape = CircleShape,
                            color = PaceDreamColors.Surface,
                            modifier = Modifier.size(80.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                Icon(
                                    PaceDreamIcons.Search,
                                    contentDescription = null,
                                    modifier = Modifier.size(36.dp),
                                    tint = PaceDreamColors.TextTertiary
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(PaceDreamSpacing.LG))
                        Text(
                            "Search for spaces, gear, and more",
                            style = PaceDreamTypography.Headline,
                            color = PaceDreamColors.TextPrimary
                        )
                        Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))
                        Text(
                            "Find studios, equipment, and shared stays",
                            style = PaceDreamTypography.Callout,
                            color = PaceDreamColors.TextSecondary
                        )
                    }
                }
            }

            else -> {
                // Results grid
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(
                        start = PaceDreamSpacing.MD,
                        end = PaceDreamSpacing.MD,
                        top = PaceDreamSpacing.SM,
                        bottom = PaceDreamSpacing.XL
                    ),
                    horizontalArrangement = Arrangement.spacedBy(PaceDreamSpacing.SM),
                    verticalArrangement = Arrangement.spacedBy(PaceDreamSpacing.SM),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(uiState.results) { item ->
                        SearchResultCard(
                            item = item,
                            onClick = { onListingClick(item) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchResultCard(
    item: SearchResultItem,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(PaceDreamRadius.LG),
        color = PaceDreamColors.Card,
        shadowElevation = 0.dp,
        tonalElevation = 0.dp
    ) {
        Column {
            // Image
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(PaceDreamRadius.LG))
            ) {
                if (!item.imageUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = item.imageUrl,
                        contentDescription = item.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(PaceDreamColors.Surface),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            PaceDreamIcons.Search,
                            contentDescription = null,
                            tint = PaceDreamColors.TextTertiary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                // Bottom gradient
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp)
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.25f))
                            )
                        )
                )

                // Type badge - glass style
                Surface(
                    shape = RoundedCornerShape(PaceDreamRadius.XS),
                    color = Color.Black.copy(alpha = 0.45f),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(PaceDreamSpacing.SM)
                ) {
                    Text(
                        text = when (item.type) {
                            "time-based" -> "Space"
                            "gear" -> "Gear"
                            "split-stay" -> "Stay"
                            else -> item.type
                        },
                        style = PaceDreamTypography.Caption2,
                        color = Color.White,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(
                            horizontal = PaceDreamSpacing.SM,
                            vertical = PaceDreamSpacing.XS
                        )
                    )
                }
            }

            // Info section
            Column(modifier = Modifier.padding(PaceDreamSpacing.SM)) {
                Text(
                    text = item.title,
                    style = PaceDreamTypography.Subheadline,
                    fontWeight = FontWeight.SemiBold,
                    color = PaceDreamColors.TextPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                item.location?.let { loc ->
                    Spacer(modifier = Modifier.height(PaceDreamSpacing.XXS))
                    Text(
                        text = loc,
                        style = PaceDreamTypography.Caption,
                        color = PaceDreamColors.TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
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
                            style = PaceDreamTypography.Subheadline,
                            fontWeight = FontWeight.Bold,
                            color = PaceDreamColors.Primary
                        )
                    }

                    item.rating?.let { rating ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                PaceDreamIcons.Star,
                                contentDescription = null,
                                tint = Color(0xFFFFCC00),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(PaceDreamSpacing.XXS))
                            Text(
                                text = String.format("%.1f", rating),
                                style = PaceDreamTypography.Caption,
                                fontWeight = FontWeight.Medium,
                                color = PaceDreamColors.TextPrimary
                            )
                        }
                    }
                }
            }
        }
    }
}
