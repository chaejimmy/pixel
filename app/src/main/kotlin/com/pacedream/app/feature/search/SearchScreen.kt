package com.pacedream.app.feature.search

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.layout.statusBarsPadding
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.pacedream.common.composables.animations.animatedCardEntry
import com.pacedream.common.composables.theme.PaceDreamColors
import com.pacedream.common.composables.theme.PaceDreamRadius
import com.pacedream.common.composables.theme.PaceDreamSpacing
import com.pacedream.common.composables.theme.PaceDreamTypography
import com.pacedream.common.composables.theme.paceDreamDisplayFontFamily
import com.pacedream.common.composables.theme.paceDreamFontFamily

object SearchTestTags {
    const val Root = "search_screen_root"
    const val Input = "search_input"
    const val Tabs = "search_tabs"
    const val ResultsList = "search_results_list"
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun SearchScreen(
    viewModel: SearchViewModel = hiltViewModel(),
    onListingClick: (SearchResultItem) -> Unit = {},
    initialCategory: String? = null
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag(SearchTestTags.Root)
            .background(PaceDreamColors.Background)
            .statusBarsPadding()
    ) {
        // ── Search header (iOS parity: title + search bar + tab picker) ──
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(PaceDreamColors.Background)
                .padding(horizontal = PaceDreamSpacing.MD, vertical = PaceDreamSpacing.MD)
        ) {
            // Title row with close button (matches iOS header)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "Explore",
                        style = PaceDreamTypography.Title1.copy(
                            fontFamily = paceDreamDisplayFontFamily,
                            letterSpacing = (-0.5).sp
                        ),
                        color = PaceDreamColors.TextPrimary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        "Spaces \u00B7 Items \u00B7 Services",
                        style = PaceDreamTypography.Footnote.copy(
                            fontFamily = paceDreamFontFamily
                        ),
                        color = PaceDreamColors.TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(PaceDreamSpacing.MD))

            // Search bar (iOS parity: filled background, rounded, leading icon)
            TextField(
                value = uiState.query,
                onValueChange = { viewModel.onQueryChanged(it) },
                placeholder = {
                    Text(
                        "Search spaces, items, and services...",
                        style = PaceDreamTypography.Callout.copy(
                            fontFamily = paceDreamFontFamily
                        ),
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
                modifier = Modifier.fillMaxWidth().testTag(SearchTestTags.Input),
                shape = RoundedCornerShape(PaceDreamRadius.MD),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = {
                    keyboardController?.hide()
                    focusManager.clearFocus()
                    viewModel.search()
                }),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = PaceDreamColors.Surface,
                    unfocusedContainerColor = PaceDreamColors.Surface,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedTextColor = PaceDreamColors.TextPrimary,
                    unfocusedTextColor = PaceDreamColors.TextPrimary
                ),
                textStyle = PaceDreamTypography.Callout.copy(
                    fontFamily = paceDreamFontFamily
                )
            )

            Spacer(modifier = Modifier.height(PaceDreamSpacing.MD))

            // Segmented tab control (iOS parity: capsule shape, icon + text, animated)
            val tabs = SearchTab.entries
            Surface(
                modifier = Modifier.testTag(SearchTestTags.Tabs),
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
                            color = if (isSelected) PaceDreamColors.Primary else Color.Transparent,
                            shadowElevation = if (isSelected) 1.dp else 0.dp
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.padding(
                                    horizontal = PaceDreamSpacing.MD,
                                    vertical = 10.dp
                                )
                            ) {
                                Text(
                                    tab.label,
                                    style = PaceDreamTypography.Footnote.copy(
                                        fontFamily = paceDreamFontFamily,
                                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                                    ),
                                    color = if (isSelected) Color.White else PaceDreamColors.TextSecondary
                                )
                            }
                        }
                    }
                }
            }
        }

        HorizontalDivider(
            thickness = 0.5.dp,
            color = PaceDreamColors.Gray100
        )

        // ── Category filter chips (iOS parity: pill style, toggle selection) ──
        val categories = listOf(
            "Studio", "Meeting Room", "Podcast Studio", "Photo Studio",
            "Music Studio", "Event Space", "Camera", "Lighting", "Audio"
        )
        LazyRow(
            contentPadding = PaddingValues(
                horizontal = PaceDreamSpacing.MD,
                vertical = PaceDreamSpacing.SM
            ),
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
                            style = PaceDreamTypography.Caption.copy(
                                fontFamily = paceDreamFontFamily,
                                fontWeight = if (uiState.selectedCategory == category) FontWeight.SemiBold else FontWeight.Normal
                            )
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = PaceDreamColors.Surface,
                        selectedContainerColor = PaceDreamColors.Primary.copy(alpha = 0.12f),
                        selectedLabelColor = PaceDreamColors.Primary,
                        labelColor = PaceDreamColors.TextSecondary
                    ),
                    shape = RoundedCornerShape(PaceDreamRadius.Round),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = uiState.selectedCategory == category,
                        borderColor = PaceDreamColors.BorderLight,
                        selectedBorderColor = Color.Transparent,
                        borderWidth = 0.5.dp,
                        selectedBorderWidth = 0.dp
                    )
                )
            }
        }

        // ── Content area (iOS parity: shimmer loading, proper error/empty states) ──
        when {
            uiState.isLoading -> {
                // Shimmer grid loading (iOS parity: skeleton cards)
                SearchShimmerGrid()
            }

            uiState.errorMessage != null -> {
                // Error state (iOS parity: icon + title + message + retry)
                SearchErrorState(
                    message = uiState.errorMessage ?: "",
                    onRetry = { viewModel.search() }
                )
            }

            uiState.results.isEmpty() && uiState.query.isNotBlank() -> {
                // No results state (iOS parity: magnifyingglass + title + subtitle)
                SearchNoResultsState(query = uiState.query)
            }

            uiState.results.isEmpty() -> {
                // Empty initial state (iOS parity: large icon surface + title + hint)
                SearchEmptyState()
            }

            else -> {
                // Results grid (iOS parity: 2-column with proper card styling)
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(
                        start = PaceDreamSpacing.MD,
                        end = PaceDreamSpacing.MD,
                        top = PaceDreamSpacing.SM,
                        bottom = PaceDreamSpacing.XL
                    ),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
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

// ─────────────────────────────────────────────────────────────────────────────
// Search Result Card (iOS parity: proper card surface, shadow, badge, spacing)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SearchResultCard(
    item: SearchResultItem,
    onClick: () -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = tween(durationMillis = 100),
        label = "searchCardScale"
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .animatedCardEntry()
            .scale(scale)
            .shadow(
                elevation = if (isPressed) 8.dp else 4.dp,
                shape = RoundedCornerShape(PaceDreamRadius.MD),
                ambientColor = Color.Black.copy(alpha = 0.06f),
                spotColor = Color.Black.copy(alpha = 0.06f)
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
        shape = RoundedCornerShape(PaceDreamRadius.MD),
        color = PaceDreamColors.Card,
        tonalElevation = 0.dp
    ) {
        Column {
            // Image
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(
                        RoundedCornerShape(
                            topStart = PaceDreamRadius.MD,
                            topEnd = PaceDreamRadius.MD
                        )
                    )
            ) {
                if (!item.imageUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(item.imageUrl)
                            .crossfade(200)
                            .build(),
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
                            PaceDreamIcons.Image,
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
                        .height(48.dp)
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

                // Type badge (iOS parity: ultraThinMaterial capsule)
                Surface(
                    shape = RoundedCornerShape(PaceDreamRadius.XS),
                    color = Color.Black.copy(alpha = 0.45f),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(PaceDreamSpacing.SM)
                ) {
                    Text(
                        text = when (item.type) {
                            "space" -> "Space"
                            "item" -> "Item"
                            "service" -> "Service"
                            else -> item.type.replaceFirstChar { it.uppercase() }
                        },
                        style = PaceDreamTypography.Caption2.copy(
                            fontFamily = paceDreamFontFamily,
                            fontWeight = FontWeight.Medium
                        ),
                        color = Color.White,
                        modifier = Modifier.padding(
                            horizontal = PaceDreamSpacing.SM,
                            vertical = PaceDreamSpacing.XS
                        )
                    )
                }

                // Rating badge (iOS parity: top-left capsule with star)
                item.rating?.let { rating ->
                    Surface(
                        shape = RoundedCornerShape(PaceDreamRadius.XS),
                        color = Color.Black.copy(alpha = 0.45f),
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(PaceDreamSpacing.SM)
                    ) {
                        Row(
                            modifier = Modifier.padding(
                                horizontal = PaceDreamSpacing.SM,
                                vertical = PaceDreamSpacing.XS
                            ),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                PaceDreamIcons.Star,
                                contentDescription = null,
                                tint = Color(0xFFFFBE0B),
                                modifier = Modifier.size(10.dp)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = String.format("%.1f", rating),
                                style = PaceDreamTypography.Caption2.copy(
                                    fontFamily = paceDreamFontFamily,
                                    fontWeight = FontWeight.Medium
                                ),
                                color = Color.White
                            )
                        }
                    }
                }
            }

            // Info section (iOS parity: proper spacing, font sizes)
            Column(
                modifier = Modifier.padding(
                    horizontal = 10.dp,
                    vertical = 10.dp
                )
            ) {
                Text(
                    text = item.title,
                    style = PaceDreamTypography.Subheadline.copy(
                        fontFamily = paceDreamFontFamily,
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = PaceDreamColors.TextPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                item.location?.let { loc ->
                    Spacer(modifier = Modifier.height(3.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            PaceDreamIcons.LocationOn,
                            contentDescription = null,
                            tint = PaceDreamColors.TextTertiary,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = loc,
                            style = PaceDreamTypography.Caption.copy(
                                fontFamily = paceDreamFontFamily
                            ),
                            color = PaceDreamColors.TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    item.price?.let { price ->
                        Text(
                            text = price,
                            style = PaceDreamTypography.Subheadline.copy(
                                fontFamily = paceDreamFontFamily,
                                fontWeight = FontWeight.Bold
                            ),
                            color = PaceDreamColors.Primary
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Shimmer Grid for search loading (iOS parity: skeleton cards in grid)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SearchShimmerGrid() {
    val transition = rememberInfiniteTransition(label = "searchShimmer")
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

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(
            horizontal = PaceDreamSpacing.MD,
            vertical = PaceDreamSpacing.SM
        ),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(6) {
            Surface(
                shape = RoundedCornerShape(PaceDreamRadius.MD),
                color = Color.White,
                shadowElevation = 1.dp
            ) {
                Column {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .clip(
                                RoundedCornerShape(
                                    topStart = PaceDreamRadius.MD,
                                    topEnd = PaceDreamRadius.MD
                                )
                            )
                            .background(shimmerBrush)
                    )
                    Column(modifier = Modifier.padding(10.dp)) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.85f)
                                .height(12.dp)
                                .clip(RoundedCornerShape(PaceDreamRadius.XS))
                                .background(shimmerBrush)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.6f)
                                .height(10.dp)
                                .clip(RoundedCornerShape(PaceDreamRadius.XS))
                                .background(shimmerBrush)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.4f)
                                .height(12.dp)
                                .clip(RoundedCornerShape(PaceDreamRadius.XS))
                                .background(shimmerBrush)
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Error State (iOS parity: icon + title + message + retry button)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SearchErrorState(
    message: String,
    onRetry: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(PaceDreamSpacing.Page),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(
                        PaceDreamColors.Error.copy(alpha = 0.08f),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    PaceDreamIcons.ErrorOutline,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = PaceDreamColors.Error
                )
            }
            Spacer(modifier = Modifier.height(PaceDreamSpacing.LG))
            Text(
                "Something went wrong",
                style = PaceDreamTypography.Title3.copy(
                    fontFamily = paceDreamDisplayFontFamily,
                    fontWeight = FontWeight.SemiBold
                ),
                color = PaceDreamColors.TextPrimary,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))
            Text(
                message,
                style = PaceDreamTypography.Subheadline.copy(
                    fontFamily = paceDreamFontFamily
                ),
                color = PaceDreamColors.TextSecondary,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(PaceDreamSpacing.XL))
            Button(
                onClick = onRetry,
                shape = RoundedCornerShape(PaceDreamRadius.MD),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PaceDreamColors.Primary
                ),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
            ) {
                Text(
                    "Retry",
                    style = PaceDreamTypography.Headline.copy(
                        fontFamily = paceDreamFontFamily,
                        fontSize = 15.sp
                    ),
                    color = Color.White
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// No Results State (iOS parity: magnifyingglass + title + adjust hint)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SearchNoResultsState(query: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(PaceDreamSpacing.Page),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(
                        PaceDreamColors.Surface,
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    PaceDreamIcons.Search,
                    contentDescription = null,
                    modifier = Modifier.size(28.dp),
                    tint = PaceDreamColors.TextTertiary
                )
            }
            Spacer(modifier = Modifier.height(PaceDreamSpacing.LG))
            Text(
                "No results found",
                style = PaceDreamTypography.Title3.copy(
                    fontFamily = paceDreamDisplayFontFamily,
                    fontWeight = FontWeight.SemiBold
                ),
                color = PaceDreamColors.TextPrimary,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))
            Text(
                "Try adjusting your search or filters",
                style = PaceDreamTypography.Subheadline.copy(
                    fontFamily = paceDreamFontFamily
                ),
                color = PaceDreamColors.TextSecondary,
                textAlign = TextAlign.Center
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Empty State (iOS parity: large icon surface + title + subtitle)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SearchEmptyState() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(PaceDreamSpacing.Page),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Surface(
                shape = CircleShape,
                color = PaceDreamColors.Surface,
                modifier = Modifier.size(80.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
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
                "Search for spaces, items, and services",
                style = PaceDreamTypography.Title3.copy(
                    fontFamily = paceDreamDisplayFontFamily,
                    fontWeight = FontWeight.SemiBold
                ),
                color = PaceDreamColors.TextPrimary,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))
            Text(
                "Find spaces, items, and services near you",
                style = PaceDreamTypography.Subheadline.copy(
                    fontFamily = paceDreamFontFamily
                ),
                color = PaceDreamColors.TextSecondary,
                textAlign = TextAlign.Center
            )
        }
    }
}
