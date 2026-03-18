package com.shourov.apps.pacedream.feature.propertydetail

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import com.pacedream.common.icon.PaceDreamIcons
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import android.content.Intent
import android.net.Uri
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.compose.ui.platform.LocalContext
import com.pacedream.common.composables.theme.PaceDreamButtonHeight
import com.pacedream.common.composables.theme.PaceDreamColors
import com.pacedream.common.composables.theme.PaceDreamGlass
import com.pacedream.common.composables.theme.PaceDreamRadius
import com.pacedream.common.composables.theme.PaceDreamSpacing
import com.pacedream.common.composables.theme.PaceDreamTypography
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
    val authState by viewModel.authState.collectAsStateWithLifecycle()
    val favoriteIds by viewModel.favoriteIds.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val preview = remember(propertyId) { ListingPreviewStore.get(propertyId) }
    val isFavorited = favoriteIds.contains(propertyId)
    val detail = uiState.detail

    var showAboutSheet by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
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
                        style = PaceDreamTypography.Body,
                        color = PaceDreamColors.TextSecondary
                    )
                    Spacer(modifier = Modifier.height(PaceDreamSpacing.MD))
                    Button(
                        onClick = { viewModel.refreshDetail() },
                        colors = ButtonDefaults.buttonColors(containerColor = PaceDreamColors.Primary),
                        shape = RoundedCornerShape(PaceDreamGlass.ButtonRadius),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
                    ) { Text("Retry", style = PaceDreamTypography.Button) }
                }
            }
            return@Scaffold
        }

        val title = detail?.title ?: preview?.title ?: "Listing"
        val locationText = detail?.cityState ?: preview?.location
        // Build price text from detail hourlyFrom or preview
        val priceText = detail?.hourlyFrom?.let { hourly ->
            val symbol = when ((detail.currency ?: "USD").uppercase()) {
                "USD" -> "$"; "EUR" -> "€"; "GBP" -> "£"; "INR" -> "₹"; else -> "$"
            }
            "$symbol${String.format("%.0f", hourly)}/hr"
        } ?: preview?.priceText?.takeIf { it.isNotBlank() }
        val rating = detail?.rating ?: preview?.rating
        val images = detail?.imageUrls?.takeIf { it.isNotEmpty() } ?: listOfNotNull(preview?.imageUrl)

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            androidx.compose.foundation.lazy.LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 120.dp)
            ) {
            item {
                // Image gallery with overlaid back/share/favorite buttons (iOS parity)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(330.dp)
                ) {
                    HeroGallery(
                        title = title,
                        imageUrls = images,
                        modifier = Modifier.fillMaxSize()
                    )
                    // Back button (top-left)
                    androidx.compose.material3.Surface(
                        shape = androidx.compose.foundation.shape.CircleShape,
                        color = Color.Black.copy(alpha = 0.35f),
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(PaceDreamSpacing.LG)
                            .padding(top = PaceDreamSpacing.XL)
                    ) {
                        IconButton(onClick = onBackClick) {
                            Icon(PaceDreamIcons.ArrowBack, contentDescription = "Back", tint = Color.White)
                        }
                    }
                    // Share + Favorite (top-right)
                    Row(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(PaceDreamSpacing.LG)
                            .padding(top = PaceDreamSpacing.XL),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        androidx.compose.material3.Surface(
                            shape = androidx.compose.foundation.shape.CircleShape,
                            color = Color.Black.copy(alpha = 0.35f)
                        ) {
                            IconButton(onClick = onShareClick) {
                                Icon(PaceDreamIcons.Share, contentDescription = "Share", tint = Color.White)
                            }
                        }
                        androidx.compose.material3.Surface(
                            shape = androidx.compose.foundation.shape.CircleShape,
                            color = Color.Black.copy(alpha = 0.35f)
                        ) {
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
                                    imageVector = if (isFavorited) PaceDreamIcons.Favorite else PaceDreamIcons.FavoriteBorder,
                                    contentDescription = if (isFavorited) "Unfavorite" else "Favorite",
                                    tint = if (isFavorited) PaceDreamColors.Error else Color.White
                                )
                            }
                        }
                    }
                }
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
                        style = PaceDreamTypography.Title2,
                        color = PaceDreamColors.TextPrimary
                    )
                    Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(PaceDreamIcons.Star, contentDescription = null, tint = PaceDreamColors.Warning, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(PaceDreamSpacing.XS))
                        Text(
                            text = rating?.let { String.format("%.1f", it) } ?: "—",
                            style = PaceDreamTypography.Callout,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.width(PaceDreamSpacing.XS))
                        Text(
                            text = detail?.reviewCount?.let { "($it)" } ?: "(No reviews yet)",
                            style = PaceDreamTypography.Callout,
                            color = PaceDreamColors.TextSecondary
                        )
                        if (!locationText.isNullOrBlank()) {
                            Spacer(modifier = Modifier.width(PaceDreamSpacing.SM))
                            Text(
                                text = "· $locationText",
                                style = PaceDreamTypography.Callout,
                                color = PaceDreamColors.TextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    val hourlyFrom = detail?.hourlyFrom
                    if (hourlyFrom != null) {
                        Spacer(modifier = Modifier.height(PaceDreamSpacing.MD))
                        val symbol = when ((detail.currency ?: "USD").uppercase()) {
                            "USD" -> "$"
                            else -> "$"
                        }
                        Card(
                            shape = RoundedCornerShape(PaceDreamRadius.Round),
                            colors = CardDefaults.cardColors(containerColor = PaceDreamColors.Gray100),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                        ) {
                            Text(
                                text = "From $symbol${String.format("%.0f", hourlyFrom)} / hour",
                                modifier = Modifier.padding(horizontal = PaceDreamSpacing.MD, vertical = PaceDreamSpacing.SM),
                                style = PaceDreamTypography.Footnote,
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
                    text = "About this space",
                    style = PaceDreamTypography.Title3,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = PaceDreamSpacing.LG)
                )
                Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))
                val desc = detail?.description?.trim().orEmpty()
                if (desc.isBlank()) {
                    Text(
                        text = "No description available.",
                        color = PaceDreamColors.TextSecondary,
                        style = PaceDreamTypography.Body,
                        modifier = Modifier.padding(horizontal = PaceDreamSpacing.LG)
                    )
                } else {
                    Text(
                        text = desc,
                        maxLines = 5,
                        overflow = TextOverflow.Ellipsis,
                        style = PaceDreamTypography.Body,
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
                    title = "What this place offers",
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
                    Text("Reviews", style = PaceDreamTypography.Title3, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.weight(1f))
                    TextButton(
                        onClick = {
                            scope.launch { snackbarHostState.showSnackbar("Reviews list coming soon.") }
                        },
                        enabled = (detail?.reviewCount ?: 0) > 0
                    ) { Text("See all", color = PaceDreamColors.Primary) }
                }
                Text(
                    text = if ((detail?.reviewCount ?: 0) > 0) "${detail?.reviewCount} reviews" else "No reviews yet",
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
                    Text("Where you'll be", style = PaceDreamTypography.Title3, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.weight(1f))
                    val context = LocalContext.current
                    TextButton(onClick = {
                        val lat = detail?.latitude
                        val lng = detail?.longitude
                        val label = detail?.fullAddress ?: title
                        if (lat != null && lng != null) {
                            val uri = Uri.parse("geo:$lat,$lng?q=$lat,$lng(${Uri.encode(label)})")
                            val intent = Intent(Intent.ACTION_VIEW, uri)
                            if (intent.resolveActivity(context.packageManager) != null) {
                                context.startActivity(intent)
                            } else {
                                // Fallback to browser-based maps
                                val webUri = Uri.parse("https://www.google.com/maps/search/?api=1&query=$lat,$lng")
                                context.startActivity(Intent(Intent.ACTION_VIEW, webUri))
                            }
                        }
                    }) { Text("Open in Maps", color = PaceDreamColors.Primary) }
                }
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .padding(horizontal = PaceDreamSpacing.LG),
                    shape = RoundedCornerShape(PaceDreamRadius.LG),
                    colors = CardDefaults.cardColors(containerColor = PaceDreamColors.Gray100)
                ) {
                    val lat = detail?.latitude
                    val lng = detail?.longitude
                    val address = detail?.fullAddress ?: detail?.cityState
                    if (lat != null && lng != null) {
                        // Show static map using coordinates
                        Box(modifier = Modifier.fillMaxSize()) {
                            val staticMapUrl = "https://staticmap.openstreetmap.de/staticmap.php" +
                                "?center=$lat,$lng&zoom=15&size=600x400&maptype=mapnik" +
                                "&markers=$lat,$lng,red-pushpin"
                            AsyncImage(
                                model = staticMapUrl,
                                contentDescription = "Map location",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                            Icon(
                                PaceDreamIcons.LocationOn,
                                contentDescription = null,
                                tint = Color(0xFFEF4444),
                                modifier = Modifier
                                    .size(36.dp)
                                    .align(Alignment.Center)
                            )
                        }
                    } else {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(PaceDreamIcons.LocationOn, contentDescription = null, tint = PaceDreamColors.TextSecondary)
                                Spacer(modifier = Modifier.height(6.dp))
                                Text("Location not available", color = PaceDreamColors.TextSecondary)
                            }
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

            // Sticky bottom booking bar - iOS 26 Liquid Glass style
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(WindowInsets.navigationBars.asPaddingValues())
                    .padding(horizontal = PaceDreamSpacing.MD, vertical = PaceDreamSpacing.SM),
                shape = RoundedCornerShape(PaceDreamGlass.CardRadius),
                colors = CardDefaults.cardColors(
                    containerColor = PaceDreamColors.Card.copy(alpha = PaceDreamGlass.ThickAlpha)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = PaceDreamSpacing.LG, vertical = PaceDreamSpacing.MD),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = priceText ?: "Select time",
                            style = PaceDreamTypography.Headline,
                            color = if (priceText != null) PaceDreamColors.TextPrimary else PaceDreamColors.TextSecondary,
                        )
                        Text(
                            "You won't be charged yet",
                            style = PaceDreamTypography.Caption,
                            color = PaceDreamColors.TextSecondary
                        )
                    }

                    Button(
                        onClick = {
                            if (authState == AuthState.Unauthenticated) onShowAuthSheet() else onBookClick()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PaceDreamColors.Primary),
                        contentPadding = PaddingValues(horizontal = PaceDreamSpacing.LG, vertical = PaceDreamSpacing.SM),
                        shape = RoundedCornerShape(PaceDreamGlass.ButtonRadius),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
                    ) {
                        Text(
                            if (priceText != null) "Reserve" else "Select time",
                            style = PaceDreamTypography.Button,
                        )
                    }
                }
            }
        }

        if (showAboutSheet) {
            ModalBottomSheet(
                onDismissRequest = { showAboutSheet = false },
                containerColor = PaceDreamColors.Background,
                shape = RoundedCornerShape(topStart = PaceDreamRadius.XL, topEnd = PaceDreamRadius.XL),
            ) {
                Column(modifier = Modifier.padding(PaceDreamSpacing.MD)) {
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text("About", style = PaceDreamTypography.Title3, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.weight(1f))
                        TextButton(onClick = { showAboutSheet = false }) {
                            Text("Done", style = PaceDreamTypography.Callout, color = PaceDreamColors.Primary)
                        }
                    }
                    Spacer(modifier = Modifier.height(PaceDreamSpacing.MD))
                    Text(
                        detail?.description?.trim().orEmpty().ifBlank { "No description available." },
                        style = PaceDreamTypography.Body,
                        color = PaceDreamColors.TextPrimary,
                    )
                    Spacer(modifier = Modifier.height(PaceDreamSpacing.XL))
                }
            }
        }
    }
}

@Composable
private fun ChipPill(text: String) {
    Card(
        shape = RoundedCornerShape(PaceDreamRadius.Round),
        colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = PaceDreamGlass.ThinAlpha)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = PaceDreamSpacing.SM, vertical = PaceDreamSpacing.XS),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = text,
                style = PaceDreamTypography.Caption2,
                color = Color.White,
                fontWeight = FontWeight.SemiBold
            )
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
        shape = RoundedCornerShape(PaceDreamRadius.XL),
        colors = CardDefaults.cardColors(containerColor = PaceDreamColors.Card),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
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
                            imageVector = PaceDreamIcons.Image,
                            contentDescription = null,
                            tint = PaceDreamColors.TextSecondary,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                } else {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(url)
                            .crossfade(200)
                            .size(coil.size.Size(800, 600))
                            .build(),
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
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
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
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(hostAvatarUrl)
                        .crossfade(200)
                        .size(coil.size.Size(96, 96))
                        .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(PaceDreamButtonHeight.MD)
                        .clip(RoundedCornerShape(PaceDreamRadius.Round))
                        .background(PaceDreamColors.Gray100, shape = RoundedCornerShape(PaceDreamRadius.Round))
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(PaceDreamButtonHeight.MD)
                        .background(PaceDreamColors.Gray100, shape = RoundedCornerShape(PaceDreamRadius.Round)),
                    contentAlignment = Alignment.Center
                ) {
                    val initials = name.split(" ").filter { it.isNotBlank() }.take(2).map { it.first().toString() }.joinToString("").uppercase()
                    Text(initials.ifBlank { "H" }, style = PaceDreamTypography.Headline)
                }
            }
            Spacer(modifier = Modifier.width(PaceDreamSpacing.MD))
            Column(modifier = Modifier.weight(1f)) {
                Text("Hosted by", style = PaceDreamTypography.Caption, color = PaceDreamColors.TextSecondary)
                Text(name, style = PaceDreamTypography.Headline)
            }
            Button(
                onClick = onContact,
                colors = ButtonDefaults.buttonColors(containerColor = PaceDreamColors.Gray100),
                contentPadding = PaddingValues(horizontal = PaceDreamSpacing.LG, vertical = PaceDreamSpacing.SM),
                shape = RoundedCornerShape(PaceDreamGlass.ButtonRadius),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
            ) {
                Text("Contact", color = PaceDreamColors.Primary, style = PaceDreamTypography.Callout, fontWeight = FontWeight.SemiBold)
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
                style = PaceDreamTypography.Callout
            )
            TextButton(onClick = onRetry) { Text("Retry", color = PaceDreamColors.Primary, style = PaceDreamTypography.Callout) }
        }
    }
}

@Composable
private fun SectionChips(title: String, items: List<String>, modifier: Modifier = Modifier) {
    var showAllSheet by remember { mutableStateOf(false) }
    Column(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(title, style = PaceDreamTypography.Title3, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.weight(1f))
            TextButton(onClick = { showAllSheet = true }, enabled = items.isNotEmpty()) { Text("See all", color = PaceDreamColors.Primary) }
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

        if (showAllSheet) {
            ModalBottomSheet(
                onDismissRequest = { showAllSheet = false },
                containerColor = PaceDreamColors.Background,
                shape = RoundedCornerShape(topStart = PaceDreamRadius.XL, topEnd = PaceDreamRadius.XL),
            ) {
                Column(modifier = Modifier.padding(PaceDreamSpacing.MD)) {
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(title, style = PaceDreamTypography.Title3, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.weight(1f))
                        TextButton(onClick = { showAllSheet = false }) {
                            Text("Done", style = PaceDreamTypography.Callout, color = PaceDreamColors.Primary)
                        }
                    }
                    Spacer(modifier = Modifier.height(PaceDreamSpacing.MD))
                    items.forEach { item ->
                        Chip(text = item)
                        Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))
                    }
                    Spacer(modifier = Modifier.height(PaceDreamSpacing.XL))
                }
            }
        }
    }
}

@Composable
private fun Chip(text: String) {
    Card(
        shape = RoundedCornerShape(PaceDreamRadius.Round),
        colors = CardDefaults.cardColors(containerColor = PaceDreamColors.Gray100),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = PaceDreamSpacing.MD, vertical = PaceDreamSpacing.SM),
            style = PaceDreamTypography.Footnote,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

