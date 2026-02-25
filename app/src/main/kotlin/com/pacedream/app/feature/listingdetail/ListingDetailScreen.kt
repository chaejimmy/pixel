package com.pacedream.app.feature.listingdetail

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.android.gms.maps.model.CameraPosition
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Bed
import androidx.compose.material.icons.filled.Bathtub
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Rule
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.mutableDoubleStateOf
import com.shourov.apps.pacedream.R
import com.pacedream.app.feature.checkout.BookingDraft
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListingDetailScreen(
    uiState: ListingDetailUiState,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    onBackClick: () -> Unit,
    onRetry: () -> Unit,
    onToggleFavorite: () -> Unit,
    onShare: () -> Unit,
    onContactHost: () -> Unit,
    onOpenInMaps: () -> Unit,
    onConfirmReserve: (BookingDraft) -> Unit,
    onSubmitReview: (Double, String, CategoryRatings?) -> Unit = { _, _, _ -> },
    onLoadReviews: () -> Unit = {}
) {
    var showAboutSheet by remember { mutableStateOf(false) }
    var showAmenitiesSheet by remember { mutableStateOf(false) }
    var showReviewsSheet by remember { mutableStateOf(false) }
    var showWriteReviewSheet by remember { mutableStateOf(false) }
    var showReserveSheet by remember { mutableStateOf(false) }
    var showProposalSheet by remember { mutableStateOf(false) }

    val listing = uiState.listing

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            BookingBar(
                pricingLabel = listing?.pricing?.displayPrimary,
                onReserveClick = { showReserveSheet = true },
                onSendProposalClick = { showProposalSheet = true }
            )
        }
    ) { padding ->
        when {
            uiState.errorMessage != null && listing == null -> {
                FullErrorState(
                    message = uiState.errorMessage,
                    onRetry = onRetry,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                )
            }

            uiState.isLoading && listing == null -> {
                ListingDetailSkeleton(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                )
            }

            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    item {
                        HeroGallery(
                            imageUrls = listing?.imageUrls.orEmpty(),
                            title = listing?.title ?: "Listing",
                            isFavorite = uiState.isFavorite,
                            onBackClick = onBackClick,
                            onShare = onShare,
                            onToggleFavorite = onToggleFavorite,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    if (uiState.inlineErrorMessage != null) {
                        item {
                            InlineErrorBanner(
                                message = uiState.inlineErrorMessage,
                                onRetry = onRetry,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp)
                            )
                        }
                    }

                    item {
                        TitleMetaBlock(
                            title = listing?.title ?: "Listing",
                            rating = listing?.rating,
                            reviewCount = listing?.reviewCount,
                            cityState = listing?.location?.cityState,
                            pricePill = listing?.pricing?.displayPrimary,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 16.dp)
                        )
                    }

                    // Property details (bedrooms, beds, bathrooms, guests)
                    if (listing?.hasPropertyDetails == true) {
                        item {
                            PropertyDetailsRow(
                                propertyType = listing.propertyType,
                                maxGuests = listing.maxGuests,
                                bedrooms = listing.bedrooms,
                                beds = listing.beds,
                                bathrooms = listing.bathrooms,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 4.dp)
                            )
                        }
                        item { HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp)) }
                    }

                    item {
                        HostCard(
                            host = listing?.host,
                            onContact = onContactHost,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                        )
                    }

                    item {
                        SectionAbout(
                            description = listing?.description,
                            onReadMore = { showAboutSheet = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 20.dp)
                        )
                    }

                    item { HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp)) }

                    item {
                        SectionAmenities(
                            amenities = listing?.amenities.orEmpty(),
                            onSeeAll = { showAmenitiesSheet = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 20.dp)
                        )
                    }

                    // House Rules Section
                    if (listing != null && (listing.houseRules.isNotEmpty() || listing.checkInTime != null || listing.checkOutTime != null)) {
                        item { HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp)) }
                        item {
                            SectionHouseRules(
                                houseRules = listing.houseRules,
                                checkInTime = listing.checkInTime,
                                checkOutTime = listing.checkOutTime,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 20.dp)
                            )
                        }
                    }

                    // Safety Features Section
                    if (listing != null && listing.safetyFeatures.isNotEmpty()) {
                        item { HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp)) }
                        item {
                            SectionSafetyFeatures(
                                features = listing.safetyFeatures,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 20.dp)
                            )
                        }
                    }

                    item { HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp)) }

                    item {
                        SectionReviews(
                            rating = uiState.reviewSummary?.averageRating ?: listing?.rating,
                            reviewCount = uiState.reviewSummary?.totalCount ?: listing?.reviewCount,
                            reviews = uiState.reviews,
                            reviewSummary = uiState.reviewSummary,
                            isLoadingReviews = uiState.isLoadingReviews,
                            onSeeAll = { showReviewsSheet = true },
                            onWriteReview = { showWriteReviewSheet = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 20.dp)
                        )
                    }

                    item { HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp)) }

                    // Cancellation Policy Section (Web parity)
                    item {
                        SectionCancellationPolicy(
                            cancellationPolicy = listing?.cancellationPolicy,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 20.dp)
                        )
                    }

                    item { HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp)) }

                    // Pricing Breakdown Section (Web parity: cleaning fee, weekly discount)
                    item {
                        SectionPricingBreakdown(
                            pricing = listing?.pricing,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 20.dp)
                        )
                    }

                    item { HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp)) }

                    item {
                        SectionLocation(
                            location = listing?.location,
                            mapCoordinate = uiState.mapCoordinate,
                            isGeocoding = uiState.isGeocoding,
                            onOpenInMaps = onOpenInMaps,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 20.dp)
                        )
                    }
                }
            }
        }

        if (showAboutSheet) {
            val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            ModalBottomSheet(
                onDismissRequest = { showAboutSheet = false },
                sheetState = sheetState
            ) {
                BottomSheetHeader(title = "About", onClose = { showAboutSheet = false })
                Text(
                    text = listing?.description?.trim().orEmpty().ifBlank { "No description available." },
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(16.dp)
                )
                Spacer(modifier = Modifier.padding(WindowInsets.navigationBars.asPaddingValues()))
            }
        }

        if (showAmenitiesSheet) {
            val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            ModalBottomSheet(
                onDismissRequest = { showAmenitiesSheet = false },
                sheetState = sheetState
            ) {
                BottomSheetHeader(title = "Highlights", onClose = { showAmenitiesSheet = false })
                Column(modifier = Modifier.padding(16.dp)) {
                    val all = listing?.amenities.orEmpty()
                    if (all.isEmpty()) {
                        Text("No highlights listed.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        all.forEach { amenity ->
                            Text(text = "• $amenity", style = MaterialTheme.typography.bodyLarge)
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
                Spacer(modifier = Modifier.padding(WindowInsets.navigationBars.asPaddingValues()))
            }
        }

        if (showReviewsSheet) {
            // Lazy-load reviews when sheet is opened (iOS parity)
            androidx.compose.runtime.LaunchedEffect(Unit) { onLoadReviews() }

            val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            ModalBottomSheet(
                onDismissRequest = { showReviewsSheet = false },
                sheetState = sheetState
            ) {
                BottomSheetHeader(title = "Reviews", onClose = { showReviewsSheet = false })
                LazyColumn(modifier = Modifier.padding(16.dp)) {
                    val summary = uiState.reviewSummary
                    val reviews = uiState.reviews

                    // Rating summary header
                    item {
                        if (summary != null && summary.totalCount > 0) {
                            ReviewSummaryHeader(summary = summary)
                            Spacer(modifier = Modifier.height(16.dp))
                            HorizontalDivider()
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }

                    // Write review button
                    item {
                        Button(
                            onClick = {
                                showReviewsSheet = false
                                showWriteReviewSheet = true
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Write a Review")
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    if (reviews.isEmpty() && !uiState.isLoadingReviews) {
                        item {
                            Text(
                                "No reviews yet. Be the first to share your experience!",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    items(reviews.size) { index ->
                        ReviewCard(review = reviews[index])
                        if (index < reviews.size - 1) {
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    }

                    if (uiState.isLoadingReviews) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp))
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.padding(WindowInsets.navigationBars.asPaddingValues()))
            }
        }

        if (showWriteReviewSheet) {
            val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            ModalBottomSheet(
                onDismissRequest = { showWriteReviewSheet = false },
                sheetState = sheetState
            ) {
                WriteReviewSheet(
                    isSubmitting = uiState.isSubmittingReview,
                    onClose = { showWriteReviewSheet = false },
                    onSubmit = { rating, comment, catRatings ->
                        onSubmitReview(rating, comment, catRatings)
                        showWriteReviewSheet = false
                    }
                )
                Spacer(modifier = Modifier.padding(WindowInsets.navigationBars.asPaddingValues()))
            }
        }

        if (showReserveSheet) {
            val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            ModalBottomSheet(
                onDismissRequest = { showReserveSheet = false },
                sheetState = sheetState
            ) {
                ReserveSheet(
                    listingId = listing?.id.orEmpty(),
                    hourlyFrom = listing?.pricing?.hourlyFrom,
                    currency = listing?.pricing?.currency,
                    onClose = { showReserveSheet = false },
                    onConfirm = { draft ->
                        showReserveSheet = false
                        onConfirmReserve(draft)
                    }
                )
                Spacer(modifier = Modifier.padding(WindowInsets.navigationBars.asPaddingValues()))
            }
        }

        if (showProposalSheet) {
            val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            ModalBottomSheet(
                onDismissRequest = { showProposalSheet = false },
                sheetState = sheetState
            ) {
                ProposalSheet(
                    listingTitle = listing?.title.orEmpty(),
                    hostName = listing?.host?.name,
                    onClose = { showProposalSheet = false },
                    onSend = { showProposalSheet = false }
                )
                Spacer(modifier = Modifier.padding(WindowInsets.navigationBars.asPaddingValues()))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReserveSheet(
    listingId: String,
    hourlyFrom: Double?,
    currency: String?,
    onClose: () -> Unit,
    onConfirm: (BookingDraft) -> Unit
) {
    var selectedDateMillis by remember { mutableStateOf<Long?>(null) }
    var startTime by remember { mutableStateOf<LocalTime?>(null) }
    var endTime by remember { mutableStateOf<LocalTime?>(null) }
    var guests by remember { mutableStateOf(1) }
    val context = androidx.compose.ui.platform.LocalContext.current

    BottomSheetHeader(title = "Reserve", onClose = onClose)

    Column(modifier = Modifier.padding(16.dp)) {
        Text("Select date", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(8.dp))

        val datePickerState = androidx.compose.material3.rememberDatePickerState()
        androidx.compose.material3.DatePicker(state = datePickerState)
        selectedDateMillis = datePickerState.selectedDateMillis

        Spacer(modifier = Modifier.height(14.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            FilledTonalButton(
                onClick = {
                    val now = startTime ?: LocalTime.of(9, 0)
                    val picker = android.app.TimePickerDialog(
                        /* context = */ context,
                        /* listener = */ { _, h, m -> startTime = LocalTime.of(h, m) },
                        /* hourOfDay = */ now.hour,
                        /* minute = */ now.minute,
                        /* is24HourView = */ false
                    )
                    picker.show()
                },
                modifier = Modifier.weight(1f)
            ) {
                Text(startTime?.let { "Start: ${it.format(DateTimeFormatter.ofPattern("h:mm a"))}" } ?: "Start time")
            }
            FilledTonalButton(
                onClick = {
                    val now = endTime ?: LocalTime.of(10, 0)
                    val picker = android.app.TimePickerDialog(
                        /* context = */ context,
                        /* listener = */ { _, h, m -> endTime = LocalTime.of(h, m) },
                        /* hourOfDay = */ now.hour,
                        /* minute = */ now.minute,
                        /* is24HourView = */ false
                    )
                    picker.show()
                },
                modifier = Modifier.weight(1f)
            ) {
                Text(endTime?.let { "End: ${it.format(DateTimeFormatter.ofPattern("h:mm a"))}" } ?: "End time")
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text("Guests", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(onClick = { guests = (guests - 1).coerceAtLeast(1) }) { Text("−") }
            Spacer(modifier = Modifier.width(12.dp))
            Text("$guests", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.width(12.dp))
            OutlinedButton(onClick = { guests = (guests + 1).coerceAtMost(20) }) { Text("+") }
        }

        Spacer(modifier = Modifier.height(18.dp))

        val date = selectedDateMillis?.toLocalDate()
        val valid = !listingId.isBlank() && date != null && startTime != null && endTime != null && endTime!!.isAfter(startTime)
        val totalEstimate = if (valid && hourlyFrom != null) {
            val minutes = java.time.Duration.between(startTime, endTime).toMinutes().toDouble()
            val hours = minutes / 60.0
            hourlyFrom * hours
        } else null

        if (!valid) {
            Text(
                "Pick a date and valid start/end time to continue.",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(10.dp))
        }

        Button(
            onClick = {
                val d = requireNotNull(date)
                val start = requireNotNull(startTime)
                val end = requireNotNull(endTime)
                val dateIso = d.format(DateTimeFormatter.ISO_LOCAL_DATE)
                val startIso = "${dateIso}T${start.format(DateTimeFormatter.ofPattern("HH:mm"))}:00"
                val endIso = "${dateIso}T${end.format(DateTimeFormatter.ofPattern("HH:mm"))}:00"
                onConfirm(
                    BookingDraft(
                        listingId = listingId,
                        date = dateIso,
                        startTimeISO = startIso,
                        endTimeISO = endIso,
                        guests = guests,
                        totalAmountEstimate = totalEstimate
                    )
                )
            },
            enabled = valid,
            modifier = Modifier.fillMaxWidth()
        ) {
            val prefix = when ((currency ?: "USD").uppercase()) {
                "USD" -> "$"
                else -> "$"
            }
            Text(
                text = when {
                    totalEstimate != null -> "Continue ($prefix${String.format("%.0f", totalEstimate)})"
                    else -> "Continue"
                }
            )
        }
    }
}

private fun Long.toLocalDate(): LocalDate {
    return Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()).toLocalDate()
}

@Composable
private fun BookingBar(
    pricingLabel: String?,
    onReserveClick: () -> Unit,
    onSendProposalClick: () -> Unit
) {
    Surface(shadowElevation = 8.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .padding(WindowInsets.navigationBars.asPaddingValues()),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = pricingLabel ?: "Select time",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Taxes shown at checkout",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            OutlinedButton(
                onClick = onSendProposalClick,
                modifier = Modifier.padding(end = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Propose")
            }
            Button(onClick = onReserveClick) {
                Text(if (pricingLabel != null) "Reserve" else "Select time")
            }
        }
    }
}

@Composable
private fun HeroGallery(
    imageUrls: List<String>,
    title: String,
    isFavorite: Boolean,
    onBackClick: () -> Unit,
    onShare: () -> Unit,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier
) {
    val urls = if (imageUrls.isNotEmpty()) imageUrls else listOf("")
    val pagerState = rememberPagerState(pageCount = { urls.size })

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(330.dp)
    ) {
        HorizontalPager(state = pagerState) { page ->
            val url = urls[page]
            if (url.isBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                )
            } else {
                AsyncImage(
                    model = url,
                    contentDescription = title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        // Back (top-left)
        Surface(
            shape = CircleShape,
            color = Color.Black.copy(alpha = 0.35f),
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
        ) {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }
        }

        // Share + Favorite (top-right)
        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(shape = CircleShape, color = Color.Black.copy(alpha = 0.35f)) {
                IconButton(onClick = onShare) {
                    Icon(Icons.Default.Share, contentDescription = "Share", tint = Color.White)
                }
            }
            Surface(shape = CircleShape, color = Color.Black.copy(alpha = 0.35f)) {
                IconButton(onClick = onToggleFavorite) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = if (isFavorite) MaterialTheme.colorScheme.error else Color.White
                    )
                }
            }
        }

        // Dots
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            repeat(urls.size) { idx ->
                val selected = idx == pagerState.currentPage
                Box(
                    modifier = Modifier
                        .size(if (selected) 7.dp else 6.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = if (selected) 0.95f else 0.55f))
                )
            }
        }
    }
}

@Composable
private fun TitleMetaBlock(
    title: String,
    rating: Double?,
    reviewCount: Int?,
    cityState: String?,
    pricePill: String?,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(10.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = rating?.let { String.format("%.1f", it) } ?: "—",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = when {
                    reviewCount != null -> "(${reviewCount})"
                    else -> "(No reviews yet)"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            cityState?.let {
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "· $it",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        if (!pricePill.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(14.dp))
            Surface(
                shape = RoundedCornerShape(999.dp),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Text(
                    text = "From $pricePill",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

@Composable
private fun HostCard(
    host: ListingHost?,
    onContact: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val initials = (host?.name ?: "Host")
                    .trim()
                    .split(" ")
                    .filter { it.isNotBlank() }
                    .take(2)
                    .mapNotNull { it.firstOrNull()?.toString() }
                    .joinToString("")
                    .uppercase()
                    .ifBlank { "H" }

                // Host avatar
                Box {
                    if (!host?.avatarUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = host?.avatarUrl,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(initials, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium)
                        }
                    }
                    // Superhost badge
                    if (host?.isSuperhost == true) {
                        Surface(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .size(20.dp),
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary
                        ) {
                            Icon(
                                Icons.Default.Star,
                                contentDescription = "Superhost",
                                tint = Color.White,
                                modifier = Modifier.padding(3.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text("Hosted by", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            host?.name ?: "Host",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        if (host?.isVerified == true || host?.verifications?.isNotEmpty() == true) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                Icons.Default.Verified,
                                contentDescription = "Verified",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    if (host?.isSuperhost == true) {
                        Text(
                            "Superhost",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                OutlinedButton(onClick = onContact) {
                    Text("Contact")
                }
            }

            // Host details row (response rate, response time, joined)
            val hasDetails = host?.responseRate != null || host?.responseTime != null || host?.joinedDate != null
            if (hasDetails) {
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    host?.responseRate?.let { rate ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                "$rate%",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Response rate",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    host?.responseTime?.let { time ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                time,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Response time",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    host?.listingCount?.let { count ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                "$count",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Listings",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Host bio
            host?.bio?.takeIf { it.isNotBlank() }?.let { bio ->
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = bio.trim(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Verification badges
            if (host?.verifications?.isNotEmpty() == true) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    host.verifications.take(4).forEach { verification ->
                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = verification.replaceFirstChar { it.uppercase() },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionAbout(
    description: String?,
    onReadMore: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text("About", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(10.dp))
        if (description.isNullOrBlank()) {
            Text("No description available.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            Text(
                text = description.trim(),
                maxLines = 5,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(modifier = Modifier.height(8.dp))
            TextButton(onClick = onReadMore) {
                Text("Read more")
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SectionAmenities(
    amenities: List<String>,
    onSeeAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Highlights", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.weight(1f))
            TextButton(onClick = onSeeAll, enabled = amenities.isNotEmpty()) {
                Text("See all")
            }
        }
        Spacer(modifier = Modifier.height(10.dp))

        if (amenities.isEmpty()) {
            Text("No highlights listed.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            val shown = amenities.take(8)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                shown.forEach { label ->
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionReviews(
    rating: Double?,
    reviewCount: Int?,
    reviews: List<ReviewModel>,
    reviewSummary: ReviewSummary?,
    isLoadingReviews: Boolean,
    onSeeAll: () -> Unit,
    onWriteReview: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Reviews", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.weight(1f))
            TextButton(onClick = onSeeAll) {
                Text("See all")
            }
        }
        Spacer(modifier = Modifier.height(10.dp))

        val displayRating = rating ?: reviewSummary?.averageRating
        val displayCount = reviewCount ?: reviewSummary?.totalCount

        if (displayRating == null || displayCount == null || displayCount == 0) {
            Text("No reviews yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(onClick = onWriteReview) {
                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Write a Review")
            }
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFB400), modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = String.format("%.1f", displayRating),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("($displayCount reviews)", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            // Category ratings bar (if available)
            reviewSummary?.categoryAverages?.let { cats ->
                Spacer(modifier = Modifier.height(14.dp))
                CategoryRatingBars(categoryRatings = cats)
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (isLoadingReviews) {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                }
            } else if (reviews.isNotEmpty()) {
                // Horizontal scrollable review preview cards (max 3)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(reviews.take(3)) { review ->
                        ReviewPreviewCard(review = review)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onWriteReview) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Write a Review")
                }
            }
        }
    }
}

@Composable
private fun CategoryRatingBars(categoryRatings: CategoryRatings) {
    val categories = listOfNotNull(
        categoryRatings.cleanliness?.let { "Cleanliness" to it },
        categoryRatings.accuracy?.let { "Accuracy" to it },
        categoryRatings.communication?.let { "Communication" to it },
        categoryRatings.location?.let { "Location" to it },
        categoryRatings.checkIn?.let { "Check-in" to it },
        categoryRatings.value?.let { "Value" to it }
    )

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        categories.forEach { (label, value) ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.width(100.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                LinearProgressIndicator(
                    progress = { (value / 5.0).toFloat().coerceIn(0f, 1f) },
                    modifier = Modifier
                        .weight(1f)
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = Color(0xFFFFB400),
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = String.format("%.1f", value),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun ReviewPreviewCard(review: ReviewModel) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.width(280.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Avatar
                if (!review.userAvatarUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = review.userAvatarUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                    )
                } else {
                    val initials = review.userName.trim().split(" ")
                        .filter { it.isNotBlank() }
                        .take(2)
                        .mapNotNull { it.firstOrNull()?.toString() }
                        .joinToString("")
                        .uppercase()
                        .ifBlank { "G" }
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            initials,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = review.userName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        repeat(5) { idx ->
                            Icon(
                                Icons.Default.Star,
                                contentDescription = null,
                                tint = if (idx < review.rating.toInt()) Color(0xFFFFB400)
                                else MaterialTheme.colorScheme.outlineVariant,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                        review.createdAt?.let { date ->
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = formatReviewDate(date),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
            if (review.comment.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = review.comment,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun ReviewCard(review: ReviewModel) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (!review.userAvatarUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = review.userAvatarUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                    )
                } else {
                    val initials = review.userName.trim().split(" ")
                        .filter { it.isNotBlank() }
                        .take(2)
                        .mapNotNull { it.firstOrNull()?.toString() }
                        .joinToString("")
                        .uppercase()
                        .ifBlank { "G" }
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            initials,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = review.userName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        repeat(5) { idx ->
                            Icon(
                                Icons.Default.Star,
                                contentDescription = null,
                                tint = if (idx < review.rating.toInt()) Color(0xFFFFB400)
                                else MaterialTheme.colorScheme.outlineVariant,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = String.format("%.1f", review.rating),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                review.createdAt?.let { date ->
                    Text(
                        text = formatReviewDate(date),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (review.comment.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = review.comment,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
private fun ReviewSummaryHeader(summary: ReviewSummary) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Default.Star,
                contentDescription = null,
                tint = Color(0xFFFFB400),
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = String.format("%.1f", summary.averageRating),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "${summary.totalCount} reviews",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Rating distribution bars
        if (summary.ratingDistribution.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                for (star in 5 downTo 1) {
                    val count = summary.ratingDistribution[star] ?: 0
                    val fraction = if (summary.totalCount > 0) count.toFloat() / summary.totalCount else 0f
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "$star",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.width(16.dp)
                        )
                        Icon(
                            Icons.Default.Star,
                            contentDescription = null,
                            tint = Color(0xFFFFB400),
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        LinearProgressIndicator(
                            progress = { fraction },
                            modifier = Modifier
                                .weight(1f)
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = Color(0xFFFFB400),
                            trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "$count",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.width(24.dp)
                        )
                    }
                }
            }
        }

        // Category averages
        summary.categoryAverages?.let { cats ->
            Spacer(modifier = Modifier.height(14.dp))
            CategoryRatingBars(categoryRatings = cats)
        }
    }
}

@Composable
private fun WriteReviewSheet(
    isSubmitting: Boolean,
    onClose: () -> Unit,
    onSubmit: (rating: Double, comment: String, categoryRatings: CategoryRatings?) -> Unit
) {
    var rating by remember { mutableDoubleStateOf(0.0) }
    var comment by remember { mutableStateOf("") }
    var cleanlinessRating by remember { mutableDoubleStateOf(0.0) }
    var accuracyRating by remember { mutableDoubleStateOf(0.0) }
    var communicationRating by remember { mutableDoubleStateOf(0.0) }
    var locationRating by remember { mutableDoubleStateOf(0.0) }
    var checkInRating by remember { mutableDoubleStateOf(0.0) }
    var valueRating by remember { mutableDoubleStateOf(0.0) }

    BottomSheetHeader(title = "Write a Review", onClose = onClose)

    Column(modifier = Modifier.padding(16.dp)) {
        // Overall rating
        Text("Overall Rating", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(8.dp))
        StarRatingInput(
            rating = rating,
            onRatingChanged = { rating = it },
            starSize = 36.dp
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Category ratings
        Text("Rate Categories", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(12.dp))

        CategoryRatingInput("Cleanliness", cleanlinessRating) { cleanlinessRating = it }
        CategoryRatingInput("Accuracy", accuracyRating) { accuracyRating = it }
        CategoryRatingInput("Communication", communicationRating) { communicationRating = it }
        CategoryRatingInput("Location", locationRating) { locationRating = it }
        CategoryRatingInput("Check-in", checkInRating) { checkInRating = it }
        CategoryRatingInput("Value", valueRating) { valueRating = it }

        Spacer(modifier = Modifier.height(16.dp))

        // Comment
        Text("Your Review", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = comment,
            onValueChange = { comment = it },
            label = { Text("Share your experience...") },
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp),
            maxLines = 5
        )

        Spacer(modifier = Modifier.height(16.dp))

        val hasCategoryRatings = listOf(
            cleanlinessRating, accuracyRating, communicationRating,
            locationRating, checkInRating, valueRating
        ).any { it > 0 }

        Button(
            onClick = {
                val catRatings = if (hasCategoryRatings) CategoryRatings(
                    cleanliness = cleanlinessRating.takeIf { it > 0 },
                    accuracy = accuracyRating.takeIf { it > 0 },
                    communication = communicationRating.takeIf { it > 0 },
                    location = locationRating.takeIf { it > 0 },
                    checkIn = checkInRating.takeIf { it > 0 },
                    value = valueRating.takeIf { it > 0 }
                ) else null
                onSubmit(rating, comment, catRatings)
            },
            enabled = rating > 0 && comment.isNotBlank() && !isSubmitting,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (isSubmitting) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text("Submit Review")
        }
    }
}

@Composable
private fun StarRatingInput(
    rating: Double,
    onRatingChanged: (Double) -> Unit,
    starSize: androidx.compose.ui.unit.Dp = 28.dp
) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        for (star in 1..5) {
            Icon(
                Icons.Default.Star,
                contentDescription = "Rate $star stars",
                tint = if (star <= rating.toInt()) Color(0xFFFFB400)
                else MaterialTheme.colorScheme.outlineVariant,
                modifier = Modifier
                    .size(starSize)
                    .clickable { onRatingChanged(star.toDouble()) }
            )
        }
    }
}

@Composable
private fun CategoryRatingInput(
    label: String,
    rating: Double,
    onRatingChanged: (Double) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.width(110.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        StarRatingInput(
            rating = rating,
            onRatingChanged = onRatingChanged,
            starSize = 22.dp
        )
    }
}

private fun formatReviewDate(dateString: String): String {
    return try {
        val instant = Instant.parse(dateString)
        val localDate = instant.atZone(ZoneId.systemDefault()).toLocalDate()
        localDate.format(DateTimeFormatter.ofPattern("MMM d, yyyy"))
    } catch (_: Exception) {
        try {
            val localDate = LocalDate.parse(dateString.take(10))
            localDate.format(DateTimeFormatter.ofPattern("MMM d, yyyy"))
        } catch (_: Exception) {
            dateString.take(10)
        }
    }
}

@Composable
private fun SectionLocation(
    location: ListingLocation?,
    mapCoordinate: LatLng?,
    isGeocoding: Boolean,
    onOpenInMaps: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Location", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.weight(1f))
            TextButton(onClick = onOpenInMaps, enabled = location?.fullAddress != null || location?.cityState != null) {
                Text("Open in Maps")
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        MapPreviewCard(
            mapCoordinate = mapCoordinate,
            isGeocoding = isGeocoding
        )

        location?.fullAddress?.let { addr ->
            Spacer(modifier = Modifier.height(10.dp))
            Text(addr, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun MapPreviewCard(
    mapCoordinate: LatLng?,
    isGeocoding: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        val mapsKey = stringResource(R.string.google_maps_key)
        val mapsEnabled = mapsKey.isNotBlank()

        if (mapCoordinate != null && mapsEnabled) {
            val cameraPositionState = rememberCameraPositionState()
            androidx.compose.runtime.LaunchedEffect(mapCoordinate.latitude, mapCoordinate.longitude) {
                cameraPositionState.position = CameraPosition.fromLatLngZoom(mapCoordinate, 15f)
            }
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState
            ) {
                Marker(
                    state = MarkerState(position = mapCoordinate),
                    title = "Location"
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.LocationOn, contentDescription = "Location", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = when {
                            isGeocoding -> "Finding location…"
                            !mapsEnabled -> "Map preview unavailable"
                            else -> "Location not found"
                        },
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun InlineErrorBanner(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = message,
                modifier = Modifier.weight(1f),
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            Spacer(modifier = Modifier.width(8.dp))
            TextButton(onClick = onRetry) {
                Text("Retry", color = MaterialTheme.colorScheme.onErrorContainer)
            }
        }
    }
}

@Composable
private fun FullErrorState(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Something went wrong", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(8.dp))
        Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(18.dp))
        Button(onClick = onRetry) { Text("Try Again") }
    }
}

@Composable
private fun ListingDetailSkeleton(modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(330.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant)
        )
        Spacer(modifier = Modifier.height(18.dp))
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            SkeletonLine(widthFraction = 0.75f, height = 28.dp)
            Spacer(modifier = Modifier.height(12.dp))
            SkeletonLine(widthFraction = 0.55f, height = 18.dp)
            Spacer(modifier = Modifier.height(18.dp))
            SkeletonLine(widthFraction = 1f, height = 90.dp)
            Spacer(modifier = Modifier.height(18.dp))
            SkeletonLine(widthFraction = 1f, height = 130.dp)
        }
    }
}

@Composable
private fun SkeletonLine(widthFraction: Float, height: androidx.compose.ui.unit.Dp) {
    Box(
        modifier = Modifier
            .fillMaxWidth(widthFraction)
            .height(height)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
    )
}

@Composable
private fun SectionCancellationPolicy(
    cancellationPolicy: CancellationPolicy?,
    modifier: Modifier = Modifier
) {
    val policy = cancellationPolicy ?: CancellationPolicy()
    Column(modifier = modifier) {
        Text(
            "Cancellation Policy",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(10.dp))
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF10B981).copy(alpha = 0.08f)
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = Color(0xFF10B981),
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = when (policy.type) {
                            "flexible" -> "Flexible"
                            "moderate" -> "Moderate"
                            "strict" -> "Strict"
                            else -> "Standard"
                        },
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF10B981)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = policy.displayText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionPricingBreakdown(
    pricing: ListingPricing?,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            "Price Details",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(10.dp))
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                val rate = pricing?.hourlyFrom ?: pricing?.basePrice
                val symbol = when ((pricing?.currency ?: "USD").uppercase()) {
                    "USD" -> "$"
                    else -> "$"
                }
                val freq = pricing?.frequencyLabel?.lowercase()
                    ?: if (pricing?.hourlyFrom != null) "hr" else "night"

                if (rate != null) {
                    PricingRow(
                        label = "Base rate",
                        value = "$symbol${rate.toLong()}/$freq"
                    )
                }

                pricing?.cleaningFee?.let { fee ->
                    Spacer(modifier = Modifier.height(8.dp))
                    PricingRow(
                        label = "Cleaning fee",
                        value = "$symbol${fee.toLong()}"
                    )
                }

                pricing?.serviceFee?.let { fee ->
                    Spacer(modifier = Modifier.height(8.dp))
                    PricingRow(
                        label = "Service fee",
                        value = "$symbol${fee.toLong()}"
                    )
                }

                pricing?.weeklyDiscountPercent?.let { discount ->
                    if (discount > 0) {
                        Spacer(modifier = Modifier.height(8.dp))
                        PricingRow(
                            label = "Weekly discount",
                            value = "-$discount%",
                            valueColor = Color(0xFF10B981)
                        )
                    }
                }

                pricing?.monthlyDiscountPercent?.let { discount ->
                    if (discount > 0) {
                        Spacer(modifier = Modifier.height(8.dp))
                        PricingRow(
                            label = "Monthly discount",
                            value = "-$discount%",
                            valueColor = Color(0xFF10B981)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "Taxes & fees",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "Calculated at checkout",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun PricingRow(
    label: String,
    value: String,
    valueColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = valueColor
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProposalSheet(
    listingTitle: String,
    hostName: String?,
    onClose: () -> Unit,
    onSend: () -> Unit
) {
    var message by remember { mutableStateOf("") }
    var proposedPrice by remember { mutableStateOf("") }
    var proposedDuration by remember { mutableStateOf("") }

    BottomSheetHeader(title = "Send Proposal", onClose = onClose)

    Column(modifier = Modifier.padding(16.dp)) {
        Text(
            text = "Send a proposal to ${hostName ?: "the host"} for \"$listingTitle\"",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text("Your offer", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = proposedPrice,
            onValueChange = { proposedPrice = it },
            label = { Text("Proposed price (e.g. \$25/hr)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = proposedDuration,
            onValueChange = { proposedDuration = it },
            label = { Text("Duration (e.g. 3 hours, 1 week)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text("Message", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = message,
            onValueChange = { message = it },
            label = { Text("Describe your needs, special requests...") },
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp),
            maxLines = 5
        )

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            )
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "The host will review your proposal and respond through the messaging system.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onSend,
            enabled = message.isNotBlank(),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Send Proposal")
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Property Details Row (bedrooms, beds, bathrooms, guests)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun PropertyDetailsRow(
    propertyType: String?,
    maxGuests: Int?,
    bedrooms: Int?,
    beds: Int?,
    bathrooms: Int?,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        // Property type header
        propertyType?.takeIf { it.isNotBlank() }?.let {
            Text(
                text = it.replaceFirstChar { c -> c.uppercase() },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        // Details chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            maxGuests?.let {
                PropertyDetailItem(
                    icon = Icons.Default.Group,
                    value = "$it",
                    label = if (it == 1) "guest" else "guests"
                )
            }
            bedrooms?.let {
                PropertyDetailItem(
                    icon = Icons.Default.Home,
                    value = "$it",
                    label = if (it == 1) "bedroom" else "bedrooms"
                )
            }
            beds?.let {
                PropertyDetailItem(
                    icon = Icons.Default.Bed,
                    value = "$it",
                    label = if (it == 1) "bed" else "beds"
                )
            }
            bathrooms?.let {
                PropertyDetailItem(
                    icon = Icons.Default.Bathtub,
                    value = "$it",
                    label = if (it == 1) "bath" else "baths"
                )
            }
        }
    }
}

@Composable
private fun PropertyDetailItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: String,
    label: String
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = "$value $label",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// House Rules Section
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SectionHouseRules(
    houseRules: List<String>,
    checkInTime: String?,
    checkOutTime: String?,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Default.Rule,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("House Rules", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        }
        Spacer(modifier = Modifier.height(12.dp))

        // Check-in / Check-out times
        if (checkInTime != null || checkOutTime != null) {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    checkInTime?.let {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.AccessTime, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Check-in", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(it, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    checkOutTime?.let {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.AccessTime, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Check-out", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(it, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
            if (houseRules.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
            }
        }

        // Rules list
        houseRules.forEach { rule ->
            Row(
                modifier = Modifier.padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("•", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.width(8.dp))
                Text(rule, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Safety Features Section
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SectionSafetyFeatures(
    features: List<String>,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Default.Security,
                contentDescription = null,
                tint = Color(0xFF10B981),
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Safety & Property", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        }
        Spacer(modifier = Modifier.height(12.dp))

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            features.forEach { feature ->
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = Color(0xFF10B981).copy(alpha = 0.08f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF10B981),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = feature,
                            style = MaterialTheme.typography.labelLarge,
                            color = Color(0xFF10B981)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BottomSheetHeader(title: String, onClose: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.weight(1f))
            TextButton(onClick = onClose) { Text("Close") }
        }
        HorizontalDivider()
    }
}

