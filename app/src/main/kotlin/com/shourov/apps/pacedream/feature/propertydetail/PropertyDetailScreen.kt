package com.shourov.apps.pacedream.feature.propertydetail

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
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
    var showAmenitiesSheet by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        containerColor = PaceDreamColors.Background
    ) { padding ->
        // Full-screen error state
        if (preview == null && detail == null && uiState.errorMessage != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(PaceDreamSpacing.XL),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = PaceDreamIcons.Error,
                        contentDescription = null,
                        tint = PaceDreamColors.TextSecondary,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(PaceDreamSpacing.MD))
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

        // Loading skeleton (iOS parity)
        if (uiState.isLoading && detail == null && preview == null) {
            ListingDetailSkeleton(modifier = Modifier.fillMaxSize().padding(padding))
            return@Scaffold
        }

        val title = detail?.title ?: preview?.title ?: "Listing"
        val locationText = detail?.cityState ?: preview?.location
        val priceText = detail?.displayPrice ?: preview?.priceText?.takeIf { it.isNotBlank() }
        val rating = detail?.rating ?: preview?.rating
        val images = detail?.imageUrls?.takeIf { it.isNotEmpty() } ?: listOfNotNull(preview?.imageUrl)
        val isAvailable = detail?.available != false

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 120.dp)
            ) {
                // ── Image Gallery ──────────────────────────────────
                item {
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
                        Surface(
                            shape = CircleShape,
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
                            Surface(shape = CircleShape, color = Color.Black.copy(alpha = 0.35f)) {
                                IconButton(onClick = onShareClick) {
                                    Icon(PaceDreamIcons.Share, contentDescription = "Share", tint = Color.White)
                                }
                            }
                            Surface(shape = CircleShape, color = Color.Black.copy(alpha = 0.35f)) {
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

                // ── Inline Error Banner ────────────────────────────
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

                // ── Title / Rating / Location / Price ──────────────
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

                        // Price pill
                        if (!priceText.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(PaceDreamSpacing.MD))
                            Card(
                                shape = RoundedCornerShape(PaceDreamRadius.Round),
                                colors = CardDefaults.cardColors(containerColor = PaceDreamColors.Gray100),
                                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                            ) {
                                Text(
                                    text = "From $priceText",
                                    modifier = Modifier.padding(horizontal = PaceDreamSpacing.MD, vertical = PaceDreamSpacing.SM),
                                    style = PaceDreamTypography.Footnote,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        // Property type / Instant Book / Availability badges (iOS parity)
                        val showBadges = detail?.propertyType != null || detail?.instantBook == true || detail?.available != null
                        if (showBadges) {
                            Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                detail?.propertyType?.takeIf { it.isNotBlank() }?.let { type ->
                                    BadgePill(
                                        text = type.replaceFirstChar { it.uppercase() },
                                        icon = PaceDreamIcons.Home,
                                        color = PaceDreamColors.Primary
                                    )
                                }
                                if (detail?.instantBook == true) {
                                    BadgePill(
                                        text = "Instant Book",
                                        icon = PaceDreamIcons.Bolt,
                                        color = Color(0xFFFF9800)
                                    )
                                }
                                detail?.available?.let { avail ->
                                    val color = if (avail) Color(0xFF10B981) else Color(0xFFEF4444)
                                    Surface(
                                        shape = RoundedCornerShape(PaceDreamRadius.Round),
                                        color = color.copy(alpha = 0.1f)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(8.dp)
                                                    .clip(CircleShape)
                                                    .background(color)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = if (avail) "Available" else "Unavailable",
                                                style = PaceDreamTypography.Caption,
                                                fontWeight = FontWeight.Medium,
                                                color = color
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // ── Property Details Row (bedrooms, beds, bathrooms, guests) ──
                if (detail?.hasPropertyDetails == true) {
                    item {
                        Spacer(modifier = Modifier.height(PaceDreamSpacing.MD))
                        PropertyDetailsRow(
                            propertyType = detail.propertyType,
                            maxGuests = detail.maxGuests,
                            bedrooms = detail.bedrooms,
                            beds = detail.beds,
                            bathrooms = detail.bathrooms,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = PaceDreamSpacing.LG)
                        )
                    }
                    item { SectionDivider() }
                }

                // ── Host Card ──────────────────────────────────────
                item {
                    Spacer(modifier = Modifier.height(PaceDreamSpacing.LG))
                    HostCard(
                        hostName = detail?.hostName,
                        hostAvatarUrl = detail?.hostAvatarUrl,
                        hostIsSuperhost = detail?.hostIsSuperhost,
                        hostIsVerified = detail?.hostIsVerified,
                        onContact = {
                            if (authState == AuthState.Unauthenticated) onShowAuthSheet() else onBookClick()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = PaceDreamSpacing.LG)
                    )
                }

                // ── About This Space ───────────────────────────────
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
                        if (desc.length > 200) {
                            Spacer(modifier = Modifier.height(6.dp))
                            TextButton(
                                onClick = { showAboutSheet = true },
                                modifier = Modifier.padding(horizontal = PaceDreamSpacing.LG)
                            ) { Text("Read more", color = PaceDreamColors.Primary) }
                        }
                    }
                }

                // ── Amenities ──────────────────────────────────────
                item { SectionDivider() }
                item {
                    SectionChips(
                        title = "What this place offers",
                        items = detail?.amenities.orEmpty(),
                        onSeeAll = { showAmenitiesSheet = true },
                        modifier = Modifier.padding(horizontal = PaceDreamSpacing.LG)
                    )
                }

                // ── House Rules (iOS parity) ───────────────────────
                if (detail != null && (detail.houseRules.isNotEmpty() || detail.checkInTime != null || detail.checkOutTime != null)) {
                    item { SectionDivider() }
                    item {
                        SectionHouseRules(
                            houseRules = detail.houseRules,
                            checkInTime = detail.checkInTime,
                            checkOutTime = detail.checkOutTime,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = PaceDreamSpacing.LG)
                        )
                    }
                }

                // ── Safety Features (iOS parity) ───────────────────
                if (detail != null && detail.safetyFeatures.isNotEmpty()) {
                    item { SectionDivider() }
                    item {
                        SectionSafetyFeatures(
                            features = detail.safetyFeatures,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = PaceDreamSpacing.LG)
                        )
                    }
                }

                // ── Reviews ────────────────────────────────────────
                item { SectionDivider() }
                item {
                    SectionReviews(
                        rating = detail?.rating,
                        reviewCount = detail?.reviewCount,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = PaceDreamSpacing.LG)
                    )
                }

                // ── Cancellation Policy (iOS parity) ───────────────
                if (detail?.cancellationPolicyText != null) {
                    item { SectionDivider() }
                    item {
                        SectionCancellationPolicy(
                            policyText = detail.cancellationPolicyText!!,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = PaceDreamSpacing.LG)
                        )
                    }
                }

                // ── Pricing Breakdown (iOS parity) ─────────────────
                if (detail?.hourlyFrom != null || detail?.basePrice != null) {
                    item { SectionDivider() }
                    item {
                        SectionPricingBreakdown(
                            detail = detail,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = PaceDreamSpacing.LG)
                        )
                    }
                }

                // ── Location / Map ─────────────────────────────────
                item { SectionDivider() }
                item {
                    SectionLocation(
                        detail = detail,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = PaceDreamSpacing.LG)
                    )
                }
            }

            // ── Sticky Bottom Booking Bar (iOS 26 Liquid Glass) ────
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
                        enabled = isAvailable,
                        colors = ButtonDefaults.buttonColors(containerColor = PaceDreamColors.Primary),
                        contentPadding = PaddingValues(horizontal = PaceDreamSpacing.LG, vertical = PaceDreamSpacing.SM),
                        shape = RoundedCornerShape(PaceDreamGlass.ButtonRadius),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
                    ) {
                        Text(
                            if (isAvailable) {
                                if (priceText != null) "Reserve" else "Select time"
                            } else "Unavailable",
                            style = PaceDreamTypography.Button,
                        )
                    }
                }
            }
        }

        // ── About Bottom Sheet ─────────────────────────────────
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

        // ── Amenities Bottom Sheet ─────────────────────────────
        if (showAmenitiesSheet) {
            ModalBottomSheet(
                onDismissRequest = { showAmenitiesSheet = false },
                containerColor = PaceDreamColors.Background,
                shape = RoundedCornerShape(topStart = PaceDreamRadius.XL, topEnd = PaceDreamRadius.XL),
            ) {
                Column(modifier = Modifier.padding(PaceDreamSpacing.MD)) {
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text("What this place offers", style = PaceDreamTypography.Title3, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.weight(1f))
                        TextButton(onClick = { showAmenitiesSheet = false }) {
                            Text("Done", style = PaceDreamTypography.Callout, color = PaceDreamColors.Primary)
                        }
                    }
                    Spacer(modifier = Modifier.height(PaceDreamSpacing.MD))
                    val all = detail?.amenities.orEmpty()
                    if (all.isEmpty()) {
                        Text("No highlights listed.", color = PaceDreamColors.TextSecondary)
                    } else {
                        all.forEach { amenity ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = PaceDreamSpacing.SM),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    PaceDreamIcons.CheckCircle,
                                    contentDescription = null,
                                    tint = PaceDreamColors.Primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(PaceDreamSpacing.SM))
                                Text(amenity, style = PaceDreamTypography.Body)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(PaceDreamSpacing.XL))
                }
            }
        }
    }
}

// ── Helper Components ──────────────────────────────────────────────

@Composable
private fun SectionDivider() {
    Spacer(modifier = Modifier.height(PaceDreamSpacing.LG))
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = PaceDreamSpacing.LG),
        color = PaceDreamColors.Border.copy(alpha = 0.15f)
    )
    Spacer(modifier = Modifier.height(PaceDreamSpacing.LG))
}

@Composable
private fun BadgePill(text: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color) {
    Surface(
        shape = RoundedCornerShape(PaceDreamRadius.Round),
        color = color.copy(alpha = 0.1f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(text, style = PaceDreamTypography.Caption, fontWeight = FontWeight.Medium, color = color)
        }
    }
}

@Composable
private fun ListingDetailSkeleton(modifier: Modifier = Modifier) {
    Column(modifier = modifier.padding(PaceDreamSpacing.LG)) {
        // Image placeholder
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp),
            shape = RoundedCornerShape(PaceDreamRadius.XL),
            colors = CardDefaults.cardColors(containerColor = PaceDreamColors.Gray100)
        ) {}
        Spacer(modifier = Modifier.height(PaceDreamSpacing.LG))
        // Title placeholder
        Card(
            modifier = Modifier
                .fillMaxWidth(0.7f)
                .height(24.dp),
            colors = CardDefaults.cardColors(containerColor = PaceDreamColors.Gray100),
            shape = RoundedCornerShape(PaceDreamRadius.SM)
        ) {}
        Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))
        Card(
            modifier = Modifier
                .fillMaxWidth(0.5f)
                .height(16.dp),
            colors = CardDefaults.cardColors(containerColor = PaceDreamColors.Gray100),
            shape = RoundedCornerShape(PaceDreamRadius.SM)
        ) {}
        Spacer(modifier = Modifier.height(PaceDreamSpacing.LG))
        // Host placeholder
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp),
            colors = CardDefaults.cardColors(containerColor = PaceDreamColors.Gray100),
            shape = RoundedCornerShape(PaceDreamRadius.LG)
        ) {}
        Spacer(modifier = Modifier.height(PaceDreamSpacing.LG))
        // Description placeholder
        repeat(3) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(if (it == 2) 0.6f else 1f)
                    .height(14.dp),
                colors = CardDefaults.cardColors(containerColor = PaceDreamColors.Gray100),
                shape = RoundedCornerShape(PaceDreamRadius.SM)
            ) {}
            Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))
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

            // Page indicator dots
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val maxDots = minOf(urls.size, 7)
                repeat(maxDots) { idx ->
                    val selected = idx == pagerState.currentPage
                    Box(
                        modifier = Modifier
                            .size(if (selected) 7.dp else 6.dp)
                            .background(
                                Color.White.copy(alpha = if (selected) 0.95f else 0.55f),
                                shape = CircleShape
                            )
                    )
                }
            }

            // Image count badge (iOS parity)
            if (urls.size > 1) {
                Surface(
                    shape = RoundedCornerShape(PaceDreamRadius.Round),
                    color = Color.Black.copy(alpha = 0.55f),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(12.dp)
                ) {
                    Text(
                        text = "${pagerState.currentPage + 1}/${urls.size}",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = PaceDreamTypography.Caption,
                        color = Color.White,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun PropertyDetailsRow(
    propertyType: String?,
    maxGuests: Int?,
    bedrooms: Int?,
    beds: Int?,
    bathrooms: Int?,
    modifier: Modifier = Modifier
) {
    val details = buildList {
        maxGuests?.let { add("$it guest${if (it != 1) "s" else ""}") }
        bedrooms?.let { add("$it bedroom${if (it != 1) "s" else ""}") }
        beds?.let { add("$it bed${if (it != 1) "s" else ""}") }
        bathrooms?.let { add("$it bath${if (it != 1) "s" else ""}") }
    }
    if (details.isNotEmpty()) {
        Text(
            text = details.joinToString(" · "),
            style = PaceDreamTypography.Callout,
            color = PaceDreamColors.TextSecondary,
            modifier = modifier
        )
    }
}

@Composable
private fun HostCard(
    hostName: String?,
    hostAvatarUrl: String?,
    hostIsSuperhost: Boolean?,
    hostIsVerified: Boolean?,
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
                        .clip(CircleShape)
                        .background(PaceDreamColors.Gray100, shape = CircleShape)
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(PaceDreamButtonHeight.MD)
                        .background(PaceDreamColors.Gray100, shape = CircleShape),
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
                // Superhost / Verified badges (iOS parity)
                if (hostIsSuperhost == true || hostIsVerified == true) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (hostIsSuperhost == true) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(PaceDreamIcons.Star, contentDescription = null, tint = PaceDreamColors.Warning, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(2.dp))
                                Text("Superhost", style = PaceDreamTypography.Caption, color = PaceDreamColors.TextSecondary)
                            }
                        }
                        if (hostIsVerified == true) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(PaceDreamIcons.CheckCircle, contentDescription = null, tint = PaceDreamColors.Success, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(2.dp))
                                Text("Verified", style = PaceDreamTypography.Caption, color = PaceDreamColors.TextSecondary)
                            }
                        }
                    }
                }
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
            Icon(
                PaceDreamIcons.Error,
                contentDescription = null,
                tint = PaceDreamColors.Error,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(PaceDreamSpacing.SM))
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SectionChips(
    title: String,
    items: List<String>,
    onSeeAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(title, style = PaceDreamTypography.Title3, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.weight(1f))
            if (items.size > 6) {
                TextButton(onClick = onSeeAll) { Text("See all", color = PaceDreamColors.Primary) }
            }
        }
        if (items.isEmpty()) {
            Text("No highlights listed.", color = PaceDreamColors.TextSecondary)
        } else {
            Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items.take(8).forEach { Chip(text = it) }
            }
            if (items.size > 8) {
                Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))
                TextButton(onClick = onSeeAll) {
                    Text("+${items.size - 8} more", color = PaceDreamColors.Primary)
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

@Composable
private fun SectionHouseRules(
    houseRules: List<String>,
    checkInTime: String?,
    checkOutTime: String?,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text("House rules", style = PaceDreamTypography.Title3, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))
        if (checkInTime != null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(PaceDreamIcons.Schedule, contentDescription = null, tint = PaceDreamColors.TextSecondary, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(PaceDreamSpacing.SM))
                Text("Check-in: $checkInTime", style = PaceDreamTypography.Body)
            }
            Spacer(modifier = Modifier.height(PaceDreamSpacing.XS))
        }
        if (checkOutTime != null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(PaceDreamIcons.Schedule, contentDescription = null, tint = PaceDreamColors.TextSecondary, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(PaceDreamSpacing.SM))
                Text("Check-out: $checkOutTime", style = PaceDreamTypography.Body)
            }
            Spacer(modifier = Modifier.height(PaceDreamSpacing.XS))
        }
        houseRules.forEach { rule ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("•", color = PaceDreamColors.TextSecondary)
                Spacer(modifier = Modifier.width(PaceDreamSpacing.SM))
                Text(rule, style = PaceDreamTypography.Body)
            }
            Spacer(modifier = Modifier.height(PaceDreamSpacing.XS))
        }
    }
}

@Composable
private fun SectionSafetyFeatures(features: List<String>, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text("Safety & property", style = PaceDreamTypography.Title3, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))
        features.forEach { feature ->
            Row(
                modifier = Modifier.padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(PaceDreamIcons.CheckCircle, contentDescription = null, tint = PaceDreamColors.Success, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(PaceDreamSpacing.SM))
                Text(feature, style = PaceDreamTypography.Body)
            }
        }
    }
}

@Composable
private fun SectionReviews(
    rating: Double?,
    reviewCount: Int?,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Reviews", style = PaceDreamTypography.Title3, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.weight(1f))
        }
        Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))
        if (reviewCount != null && reviewCount > 0) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(PaceDreamIcons.Star, contentDescription = null, tint = PaceDreamColors.Warning, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(PaceDreamSpacing.XS))
                Text(
                    text = "${rating?.let { String.format("%.1f", it) } ?: "—"} · $reviewCount review${if (reviewCount != 1) "s" else ""}",
                    style = PaceDreamTypography.Callout,
                    fontWeight = FontWeight.Medium
                )
            }
        } else {
            Text(
                "No reviews yet. Be the first to share your experience!",
                color = PaceDreamColors.TextSecondary,
                style = PaceDreamTypography.Body
            )
        }
    }
}

@Composable
private fun SectionCancellationPolicy(policyText: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text("Cancellation policy", style = PaceDreamTypography.Title3, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(PaceDreamIcons.Info, contentDescription = null, tint = PaceDreamColors.TextSecondary, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(PaceDreamSpacing.SM))
            Text(policyText, style = PaceDreamTypography.Body, color = PaceDreamColors.TextPrimary)
        }
    }
}

@Composable
private fun SectionPricingBreakdown(detail: PropertyDetailModel, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text("Pricing", style = PaceDreamTypography.Title3, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))
        Card(
            shape = RoundedCornerShape(PaceDreamRadius.LG),
            colors = CardDefaults.cardColors(containerColor = PaceDreamColors.Card),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(modifier = Modifier.padding(PaceDreamSpacing.MD)) {
                val sym = detail.currencySymbol
                detail.hourlyFrom?.let {
                    PricingRow("Hourly rate", "$sym${formatPrice(it)}/hr")
                }
                detail.basePrice?.let {
                    if (it != detail.hourlyFrom) {
                        PricingRow("Base price", "$sym${formatPrice(it)}")
                    }
                }
                detail.cleaningFee?.let {
                    PricingRow("Cleaning fee", "$sym${formatPrice(it)}")
                }
                detail.serviceFee?.let {
                    PricingRow("Service fee", "$sym${formatPrice(it)}")
                }
                detail.weeklyDiscountPercent?.let {
                    if (it > 0) {
                        PricingRow("Weekly discount", "-$it%")
                    }
                }
            }
        }
    }
}

@Composable
private fun PricingRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = PaceDreamTypography.Body, color = PaceDreamColors.TextSecondary)
        Text(value, style = PaceDreamTypography.Body, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun SectionLocation(detail: PropertyDetailModel?, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Where you'll be", style = PaceDreamTypography.Title3, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.weight(1f))
        }
        Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
            shape = RoundedCornerShape(PaceDreamRadius.LG),
            colors = CardDefaults.cardColors(containerColor = PaceDreamColors.Gray100)
        ) {
            val lat = detail?.latitude
            val lng = detail?.longitude
            if (lat != null && lng != null) {
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
            Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))
            Text(
                text = it,
                color = PaceDreamColors.TextSecondary,
                style = PaceDreamTypography.Callout
            )
        }
    }
}

private fun formatPrice(value: Double): String {
    val asLong = value.toLong()
    return if (value == asLong.toDouble()) asLong.toString()
    else "%.2f".format(value).trimEnd('0').trimEnd('.')
}
