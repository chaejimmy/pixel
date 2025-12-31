package com.shourov.apps.pacedream.feature.propertydetail

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.pacedream.common.composables.theme.PaceDreamColors
import com.pacedream.common.composables.theme.PaceDreamRadius
import com.pacedream.common.composables.theme.PaceDreamSpacing
import com.shourov.apps.pacedream.core.network.api.ApiResult
import com.shourov.apps.pacedream.core.network.auth.AuthState
import com.shourov.apps.pacedream.listing.ListingPreviewStore
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PropertyDetailScreen(
    propertyId: String,
    onBackClick: () -> Unit,
    onBookClick: () -> Unit,
    onShareClick: () -> Unit,
    onShowAuthSheet: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PropertyDetailViewModel = hiltViewModel()
) {
    val authState by viewModel.authState.collectAsState()
    val favoriteIds by viewModel.favoriteIds.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val preview = remember(propertyId) { ListingPreviewStore.get(propertyId) }
    val isFavorited = favoriteIds.contains(propertyId)
    val detail = uiState.detail

    var showAboutSheet by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = detail?.title ?: preview?.title ?: "Listing",
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 1
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onShareClick) {
                        Icon(Icons.Filled.Share, contentDescription = "Share")
                    }
                    IconButton(
                        onClick = {
                            if (authState == AuthState.Unauthenticated) {
                                onShowAuthSheet()
                                return@IconButton
                            }
                            scope.launch {
                                when (val res = viewModel.toggleFavorite(propertyId)) {
                                    is ApiResult.Success -> snackbarHostState.showSnackbar(
                                        if (isFavorited) "Removed from Favorites" else "Saved to Favorites"
                                    )
                                    is ApiResult.Failure -> snackbarHostState.showSnackbar(
                                        res.error.message ?: "Failed"
                                    )
                                }
                            }
                        }
                    ) {
                        Icon(
                            imageVector = if (isFavorited) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                            contentDescription = if (isFavorited) "Unfavorite" else "Favorite",
                            tint = if (isFavorited) PaceDreamColors.Error else PaceDreamColors.TextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PaceDreamColors.Background)
            )
        },
        containerColor = PaceDreamColors.Background
    ) { padding ->
        if (preview == null && detail == null && uiState.errorMessage != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(PaceDreamSpacing.XL),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        uiState.errorMessage ?: "Unable to load listing details.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = PaceDreamColors.TextSecondary
                    )
                    Spacer(modifier = Modifier.height(PaceDreamSpacing.MD))
                    Button(
                        onClick = { viewModel.refreshDetail() },
                        colors = ButtonDefaults.buttonColors(containerColor = PaceDreamColors.Primary)
                    ) { Text("Retry") }
                }
            }
            return@Scaffold
        }

        val title = detail?.title ?: preview?.title ?: "Listing"
        val locationText = detail?.cityState ?: preview?.location
        val priceText = preview?.priceText?.takeIf { it.isNotBlank() }
        val rating = detail?.rating ?: preview?.rating
        val images = detail?.imageUrls?.takeIf { it.isNotEmpty() } ?: listOfNotNull(preview?.imageUrl)

        androidx.compose.foundation.lazy.LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(bottom = 120.dp)
        ) {
            item {
                HeroGallery(
                    title = title,
                    imageUrls = images,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(330.dp)
                        .padding(horizontal = PaceDreamSpacing.LG, vertical = PaceDreamSpacing.MD)
                )
            }

            if (uiState.inlineErrorMessage != null) {
                item {
                    InlineError(
                        message = uiState.inlineErrorMessage!!,
                        onRetry = { viewModel.refreshDetail() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = PaceDreamSpacing.LG)
                    )
                    Spacer(modifier = Modifier.height(PaceDreamSpacing.MD))
                }
            }

            item {
                Column(modifier = Modifier.padding(horizontal = PaceDreamSpacing.LG)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = PaceDreamColors.TextPrimary
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFB400), modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = rating?.let { String.format("%.1f", it) } ?: "—",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = detail?.reviewCount?.let { "($it)" } ?: "(No reviews yet)",
                            style = MaterialTheme.typography.bodyMedium,
                            color = PaceDreamColors.TextSecondary
                        )
                        if (!locationText.isNullOrBlank()) {
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "· $locationText",
                                style = MaterialTheme.typography.bodyMedium,
                                color = PaceDreamColors.TextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    val hourlyFrom = detail?.hourlyFrom
                    if (hourlyFrom != null) {
                        Spacer(modifier = Modifier.height(14.dp))
                        val symbol = when ((detail.currency ?: "USD").uppercase()) {
                            "USD" -> "$"
                            else -> "$"
                        }
                        Card(
                            shape = RoundedCornerShape(999.dp),
                            colors = CardDefaults.cardColors(containerColor = PaceDreamColors.Gray100)
                        ) {
                            Text(
                                text = "From $symbol${String.format("%.0f", hourlyFrom)} / hour",
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(PaceDreamSpacing.LG))
                HostCard(
                    hostName = detail?.hostName,
                    hostAvatarUrl = detail?.hostAvatarUrl,
                    onContact = {
                        if (authState == AuthState.Unauthenticated) onShowAuthSheet() else onBookClick()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = PaceDreamSpacing.LG)
                )
            }

            item {
                Spacer(modifier = Modifier.height(PaceDreamSpacing.LG))
                Text(
                    text = "About",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = PaceDreamSpacing.LG)
                )
                Spacer(modifier = Modifier.height(10.dp))
                val desc = detail?.description?.trim().orEmpty()
                if (desc.isBlank()) {
                    Text(
                        text = "No description available.",
                        color = PaceDreamColors.TextSecondary,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(horizontal = PaceDreamSpacing.LG)
                    )
                } else {
                    Text(
                        text = desc,
                        maxLines = 5,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(horizontal = PaceDreamSpacing.LG)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    TextButton(
                        onClick = { showAboutSheet = true },
                        modifier = Modifier.padding(horizontal = PaceDreamSpacing.LG)
                    ) { Text("Read more", color = PaceDreamColors.Primary) }
                }
            }

            item {
                Spacer(modifier = Modifier.height(PaceDreamSpacing.LG))
                HorizontalDivider(modifier = Modifier.padding(horizontal = PaceDreamSpacing.LG))
                Spacer(modifier = Modifier.height(PaceDreamSpacing.LG))
                SectionChips(
                    title = "Highlights",
                    items = detail?.amenities.orEmpty(),
                    modifier = Modifier.padding(horizontal = PaceDreamSpacing.LG)
                )
            }

            item {
                Spacer(modifier = Modifier.height(PaceDreamSpacing.LG))
                HorizontalDivider(modifier = Modifier.padding(horizontal = PaceDreamSpacing.LG))
                Spacer(modifier = Modifier.height(PaceDreamSpacing.LG))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = PaceDreamSpacing.LG),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Reviews", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.weight(1f))
                    TextButton(onClick = { /* TODO */ }) { Text("See all", color = PaceDreamColors.Primary) }
                }
                Text(
                    text = if ((detail?.reviewCount ?: 0) > 0) "Reviews preview coming soon." else "No reviews yet",
                    color = PaceDreamColors.TextSecondary,
                    modifier = Modifier.padding(horizontal = PaceDreamSpacing.LG)
                )
            }

            item {
                Spacer(modifier = Modifier.height(PaceDreamSpacing.LG))
                HorizontalDivider(modifier = Modifier.padding(horizontal = PaceDreamSpacing.LG))
                Spacer(modifier = Modifier.height(PaceDreamSpacing.LG))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = PaceDreamSpacing.LG),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Location", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.weight(1f))
                    TextButton(onClick = { /* TODO open maps */ }) { Text("Open in Maps", color = PaceDreamColors.Primary) }
                }
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .padding(horizontal = PaceDreamSpacing.LG),
                    shape = RoundedCornerShape(PaceDreamRadius.LG),
                    colors = CardDefaults.cardColors(containerColor = PaceDreamColors.Gray100)
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.LocationOn, contentDescription = null, tint = PaceDreamColors.TextSecondary)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("Map preview (hooking up next)", color = PaceDreamColors.TextSecondary)
                        }
                    }
                }
                detail?.fullAddress?.let {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = it,
                        color = PaceDreamColors.TextSecondary,
                        modifier = Modifier.padding(horizontal = PaceDreamSpacing.LG)
                    )
                }
            }
        }

        // Sticky bottom booking bar (iOS-like)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(PaceDreamSpacing.LG),
            shape = RoundedCornerShape(PaceDreamRadius.LG),
            colors = CardDefaults.cardColors(containerColor = PaceDreamColors.Card),
            elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(PaceDreamSpacing.LG),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = priceText ?: "Select time",
                        style = MaterialTheme.typography.titleMedium,
                        color = if (priceText != null) PaceDreamColors.TextPrimary else PaceDreamColors.TextSecondary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        "Taxes shown at checkout",
                        style = MaterialTheme.typography.labelMedium,
                        color = PaceDreamColors.TextSecondary
                    )
                }

                Button(
                    onClick = {
                        if (authState == AuthState.Unauthenticated) onShowAuthSheet() else onBookClick()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PaceDreamColors.Primary),
                    contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp)
                ) {
                    Text(if (priceText != null) "Reserve" else "Select time", fontWeight = FontWeight.Bold)
                }
            }
        }

        if (showAboutSheet) {
            ModalBottomSheet(onDismissRequest = { showAboutSheet = false }) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text("About", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.weight(1f))
                        TextButton(onClick = { showAboutSheet = false }) { Text("Done") }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(detail?.description?.trim().orEmpty().ifBlank { "No description available." })
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
private fun ChipPill(text: String) {
    Card(
        shape = RoundedCornerShape(999.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = text, style = PaceDreamTypography.Caption, color = Color.White, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun HeroGallery(
    title: String,
    imageUrls: List<String>,
    modifier: Modifier = Modifier
) {
    val urls = if (imageUrls.isNotEmpty()) imageUrls else listOf("")
    val pagerState = rememberPagerState(pageCount = { urls.size })

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(PaceDreamRadius.LG),
        colors = CardDefaults.cardColors(containerColor = PaceDreamColors.Card),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            HorizontalPager(state = pagerState) { page ->
                val url = urls[page]
                if (url.isBlank()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(PaceDreamColors.Border.copy(alpha = 0.25f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Image,
                            contentDescription = null,
                            tint = PaceDreamColors.TextSecondary,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                } else {
                    AsyncImage(
                        model = url,
                        contentDescription = title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            // Dots
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                repeat(urls.size) { idx ->
                    val selected = idx == pagerState.currentPage
                    Box(
                        modifier = Modifier
                            .size(if (selected) 7.dp else 6.dp)
                            .background(
                                Color.White.copy(alpha = if (selected) 0.95f else 0.55f),
                                shape = RoundedCornerShape(99.dp)
                            )
                    )
                }
            }
        }
    }
}

@Composable
private fun HostCard(
    hostName: String?,
    hostAvatarUrl: String?,
    onContact: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
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
            val name = hostName?.takeIf { it.isNotBlank() } ?: "Host"
            if (!hostAvatarUrl.isNullOrBlank()) {
                AsyncImage(
                    model = hostAvatarUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(44.dp)
                        .background(PaceDreamColors.Gray100, shape = RoundedCornerShape(999.dp))
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(PaceDreamColors.Gray100, shape = RoundedCornerShape(999.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    val initials = name.split(" ").filter { it.isNotBlank() }.take(2).map { it.first().toString() }.joinToString("").uppercase()
                    Text(initials.ifBlank { "H" }, fontWeight = FontWeight.SemiBold)
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Hosted by", style = MaterialTheme.typography.labelMedium, color = PaceDreamColors.TextSecondary)
                Text(name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }
            Button(
                onClick = onContact,
                colors = ButtonDefaults.buttonColors(containerColor = PaceDreamColors.Gray100),
                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 10.dp)
            ) {
                Text("Contact", color = PaceDreamColors.Primary, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun InlineError(message: String, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(PaceDreamRadius.MD),
        colors = CardDefaults.cardColors(containerColor = PaceDreamColors.Error.copy(alpha = 0.08f))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = message,
                modifier = Modifier.weight(1f),
                color = PaceDreamColors.TextPrimary,
                style = MaterialTheme.typography.bodyMedium
            )
            TextButton(onClick = onRetry) { Text("Retry", color = PaceDreamColors.Primary) }
        }
    }
}

@Composable
private fun SectionChips(title: String, items: List<String>, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.weight(1f))
            TextButton(onClick = { /* TODO */ }, enabled = items.isNotEmpty()) { Text("See all", color = PaceDreamColors.Primary) }
        }
        if (items.isEmpty()) {
            Text("No highlights listed.", color = PaceDreamColors.TextSecondary)
        } else {
            val shown = items.take(8)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                shown.take(3).forEach { Chip(text = it) }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                shown.drop(3).take(3).forEach { Chip(text = it) }
            }
        }
    }
}

@Composable
private fun Chip(text: String) {
    Card(
        shape = RoundedCornerShape(999.dp),
        colors = CardDefaults.cardColors(containerColor = PaceDreamColors.Gray100)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            style = MaterialTheme.typography.labelLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

