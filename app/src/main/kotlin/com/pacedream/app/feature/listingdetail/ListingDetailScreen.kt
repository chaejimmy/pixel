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
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.compose.ui.platform.LocalContext
import com.pacedream.common.composables.theme.*
import com.google.android.gms.maps.model.LatLng
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
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
    var showReportSheet by remember { mutableStateOf(false) }

    val listing = uiState.listing

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            BookingBar(
                pricingLabel = listing?.pricing?.displayPrimary,
                available = listing?.available,
                onReserveClick = { showReserveSheet = true }
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

                    // Under Review banner for pending listings (host self-view)
                    if (listing?.isPendingReview == true) {
                        item {
                            androidx.compose.foundation.layout.Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp, vertical = 8.dp)
                                    .background(
                                        androidx.compose.ui.graphics.Color(0xFFFFA500).copy(alpha = 0.10f),
                                        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
                                    )
                                    .border(
                                        1.dp,
                                        androidx.compose.ui.graphics.Color(0xFFFFA500).copy(alpha = 0.25f),
                                        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
                                    )
                                    .padding(14.dp),
                                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(10.dp)
                            ) {
                                androidx.compose.material3.Icon(
                                    imageVector = com.pacedream.common.icon.PaceDreamIcons.Schedule,
                                    contentDescription = null,
                                    tint = androidx.compose.ui.graphics.Color(0xFFFFA500),
                                    modifier = Modifier.size(20.dp)
                                )
                                androidx.compose.foundation.layout.Column {
                                    androidx.compose.material3.Text(
                                        text = "Under Review",
                                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                    androidx.compose.material3.Text(
                                        text = "This listing is being reviewed and will be visible to guests once approved.",
                                        fontSize = 12.sp,
                                        color = androidx.compose.ui.graphics.Color.Gray
                                    )
                                }
                            }
                        }
                    }

                    if (uiState.inlineErrorMessage != null) {
                        item {
                            InlineErrorBanner(
                                message = uiState.inlineErrorMessage,
                                onRetry = onRetry,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp, vertical = 12.dp)
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
                            propertyType = listing?.propertyType,
                            instantBook = listing?.instantBook,
                            available = listing?.available,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 16.dp)
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
                                    .padding(horizontal = 20.dp, vertical = 4.dp)
                            )
                        }
                        item { HorizontalDivider(modifier = Modifier.padding(horizontal = PaceDreamSpacing.MD)) }
                    }

                    item {
                        HostCard(
                            host = listing?.host,
                            onContact = onContactHost,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = PaceDreamSpacing.MD)
                        )
                    }

                    // About section — only show when description is available
                    if (!listing?.description.isNullOrBlank()) {
                        item {
                            SectionAbout(
                                description = listing?.description,
                                category = listing?.category,
                                onReadMore = { showAboutSheet = true },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp, vertical = 20.dp)
                            )
                        }
                        item { HorizontalDivider(modifier = Modifier.padding(horizontal = PaceDreamSpacing.MD)) }
                    }

                    // Amenities section — only show when amenities exist
                    if (listing?.amenities.orEmpty().isNotEmpty()) {
                        item {
                            SectionAmenities(
                                amenities = listing?.amenities.orEmpty(),
                                onSeeAll = { showAmenitiesSheet = true },
                                category = listing?.category,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp, vertical = 20.dp)
                            )
                        }
                    }

                    // House Rules Section
                    if (listing != null && (listing.houseRules.isNotEmpty() || listing.checkInTime != null || listing.checkOutTime != null)) {
                        item { HorizontalDivider(modifier = Modifier.padding(horizontal = PaceDreamSpacing.MD)) }
                        item {
                            SectionHouseRules(
                                houseRules = listing.houseRules,
                                checkInTime = listing.checkInTime,
                                checkOutTime = listing.checkOutTime,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp, vertical = 20.dp)
                            )
                        }
                    }

                    // Safety Features Section
                    if (listing != null && listing.safetyFeatures.isNotEmpty()) {
                        item { HorizontalDivider(modifier = Modifier.padding(horizontal = PaceDreamSpacing.MD)) }
                        item {
                            SectionSafetyFeatures(
                                features = listing.safetyFeatures,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp, vertical = 20.dp)
                            )
                        }
                    }

                    item { HorizontalDivider(modifier = Modifier.padding(horizontal = PaceDreamSpacing.MD)) }

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
                                .padding(horizontal = 20.dp, vertical = 20.dp)
                        )
                    }

                    item { HorizontalDivider(modifier = Modifier.padding(horizontal = PaceDreamSpacing.MD)) }

                    // Cancellation Policy Section (Web parity)
                    item {
                        SectionCancellationPolicy(
                            cancellationPolicy = listing?.cancellationPolicy,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 20.dp)
                        )
                    }

                    item { HorizontalDivider(modifier = Modifier.padding(horizontal = PaceDreamSpacing.MD)) }

                    // Pricing Breakdown Section (Web parity: cleaning fee, weekly discount)
                    item {
                        SectionPricingBreakdown(
                            pricing = listing?.pricing,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 20.dp)
                        )
                    }

                    // Split Listing Info (website parity: shows cost splitting, slots, deadline)
                    if (listing?.shareType?.uppercase() == "SPLIT" && listing.totalCost != null) {
                        item { HorizontalDivider(modifier = Modifier.padding(horizontal = PaceDreamSpacing.MD)) }
                        item {
                            SectionSplitInfo(
                                totalCost = listing.totalCost,
                                slotsTotal = listing.slotsTotal,
                                slotsFilled = listing.slotsFilled,
                                splitStatus = listing.splitStatus,
                                deadlineAt = listing.deadlineAt,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp, vertical = 20.dp)
                            )
                        }
                    }

                    item { HorizontalDivider(modifier = Modifier.padding(horizontal = PaceDreamSpacing.MD)) }

                    item {
                        SectionLocation(
                            location = listing?.location,
                            mapCoordinate = uiState.mapCoordinate,
                            isGeocoding = uiState.isGeocoding,
                            onOpenInMaps = onOpenInMaps,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 20.dp)
                        )
                    }

                    // Report Listing (iOS/Web parity)
                    item { HorizontalDivider(modifier = Modifier.padding(horizontal = PaceDreamSpacing.MD)) }
                    item {
                        SectionReportListing(
                            onReportClick = { showReportSheet = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 20.dp)
                        )
                    }
                }
            }
        }

        // Report Listing Bottom Sheet (iOS/Web parity)
        if (showReportSheet) {
            ReportListingSheet(
                listingId = listing?.id ?: "",
                listingTitle = listing?.title ?: "Listing",
                onDismiss = { showReportSheet = false },
                onReportSubmitted = { showReportSheet = false }
            )
        }

        if (showAboutSheet) {
            val aboutTitle = when (listing?.category?.lowercase()) {
                "gear", "item", "items" -> "About this item"
                "service", "services", "split-stay" -> "About this service"
                else -> "About this space"
            }
            val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            ModalBottomSheet(
                onDismissRequest = { showAboutSheet = false },
                sheetState = sheetState
            ) {
                BottomSheetHeader(title = aboutTitle, onClose = { showAboutSheet = false })
                Text(
                    text = listing?.description?.trim().orEmpty(),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(20.dp)
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
                val highlightsTitle = when (listing?.category?.lowercase()) {
                    "car", "vehicle" -> "Vehicle Features"
                    "gear", "equipment" -> "Included Items"
                    "studio", "office" -> "Workspace Features"
                    else -> "Highlights"
                }
                BottomSheetHeader(title = highlightsTitle, onClose = { showAmenitiesSheet = false })
                Column(modifier = Modifier.padding(20.dp)) {
                    listing?.amenities.orEmpty().forEach { amenity ->
                        Row(
                            modifier = Modifier.padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                PaceDreamIcons.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(text = amenity, style = MaterialTheme.typography.bodyLarge)
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
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                if (uiState.reviewsLoadFailed) {
                                    // Subtle error state — distinct from "truly no reviews".
                                    Icon(
                                        PaceDreamIcons.Error,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(28.dp)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        "Couldn't load reviews",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        "Please try again in a moment.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    TextButton(onClick = { onLoadReviews() }) {
                                        Text("Retry")
                                    }
                                } else {
                                    Icon(
                                        PaceDreamIcons.Star,
                                        contentDescription = null,
                                        tint = Color(0xFFFFB400),
                                        modifier = Modifier.size(28.dp)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        "No reviews yet",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        "Be the first to share your experience!",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
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
                    hourlyFrom = listing?.pricing?.hourlyFrom ?: listing?.pricing?.basePrice,
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
    val durationOptions = listOf(30, 60, 90, 120) // iOS parity: 4 duration options
    var selectedDuration by remember { mutableStateOf(60) }
    var selectedDate by remember { mutableStateOf<LocalDate?>(LocalDate.now()) }
    var selectedSlotStart by remember { mutableStateOf<LocalTime?>(null) }
    var guests by remember { mutableStateOf(1) }

    val today = remember { LocalDate.now() }
    val next7Days = remember { (0L..6L).map { today.plusDays(it) } }

    // Generate 30-min time slots from 8 AM to 10 PM
    val timeSlots = remember(selectedDate, selectedDuration) {
        if (selectedDate == null) emptyList()
        else {
            val now = LocalTime.now()
            val isToday = selectedDate == LocalDate.now()
            val slots = mutableListOf<LocalTime>()
            var slot = LocalTime.of(8, 0)
            val lastSlotEnd = LocalTime.of(22, 0)
            while (true) {
                val slotEnd = slot.plusMinutes(selectedDuration.toLong())
                if (slotEnd.isAfter(lastSlotEnd) || slotEnd.isBefore(slot)) break
                if (!isToday || slot.isAfter(now)) {
                    slots.add(slot)
                }
                slot = slot.plusMinutes(30)
            }
            slots
        }
    }

    val selectedEnd = selectedSlotStart?.plusMinutes(selectedDuration.toLong())
    val currencySymbol = when ((currency ?: "USD").uppercase()) {
        "USD" -> "$"; "EUR" -> "€"; "GBP" -> "£"; "INR" -> "₹"
        "AED" -> "د.إ"; "CAD" -> "CA$"; "AUD" -> "A$"; else -> "$"
    }

    val subtotal = if (selectedSlotStart != null && hourlyFrom != null) {
        hourlyFrom * selectedDuration / 60.0
    } else null
    val taxes = subtotal?.let { it * 0.20 }
    val total = if (subtotal != null && taxes != null) subtotal + taxes else null

    val canConfirm = selectedDate != null && selectedSlotStart != null

    BottomSheetHeader(title = "Reserve", onClose = onClose)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
    ) {
        // Duration selection chips
        Text("Duration", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(10.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(durationOptions) { duration ->
                val isSelected = duration == selectedDuration
                val label = when {
                    duration < 60 -> "${duration} min"
                    duration % 60 == 0 -> "${duration / 60} hr"
                    else -> "${duration / 60}h ${duration % 60}m"
                }
                Surface(
                    onClick = {
                        selectedDuration = duration
                        selectedSlotStart = null // reset slot on duration change
                    },
                    shape = RoundedCornerShape(12.dp),
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = if (isSelected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Text(
                        text = label,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 7-day date strip
        Text("Date", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(10.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(next7Days) { date ->
                val isSelected = date == selectedDate
                val isToday = date == today
                val dayLabel = date.dayOfWeek.name.take(3).lowercase()
                    .replaceFirstChar { it.uppercase() }
                val dateNum = date.dayOfMonth.toString()
                Surface(
                    onClick = {
                        selectedDate = date
                        selectedSlotStart = null // reset slot on date change
                    },
                    shape = RoundedCornerShape(12.dp),
                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                    border = when {
                        isSelected -> null
                        isToday -> BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
                        else -> BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    }
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = dayLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = dateNum,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Time slots grid
        Text("Available times", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(10.dp))

        if (selectedDate == null) {
            Text(
                "Select a date to see available times",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium
            )
        } else if (timeSlots.isEmpty()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(vertical = 12.dp)
            ) {
                Icon(
                    imageVector = PaceDreamIcons.Schedule,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "No available times for this day",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        } else {
            // 2-column grid of time slot buttons (iOS parity)
            val rows = timeSlots.chunked(2)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                rows.forEach { rowSlots ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        rowSlots.forEach { slot ->
                            val isSelected = slot == selectedSlotStart
                            val label = slot.format(DateTimeFormatter.ofPattern("h:mm a"))
                            Surface(
                                onClick = { selectedSlotStart = slot },
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                border = if (isSelected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.padding(vertical = 12.dp)
                                ) {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                        // Fill remaining columns if row is incomplete
                        repeat(2 - rowSlots.size) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }

        // Price breakdown card (shows when a time slot is selected)
        if (subtotal != null && taxes != null && total != null && selectedSlotStart != null && selectedEnd != null) {
            Spacer(modifier = Modifier.height(18.dp))
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    val durationLabel = when {
                        selectedDuration < 60 -> "$selectedDuration min"
                        selectedDuration % 60 == 0 -> "${selectedDuration / 60} hr"
                        else -> "${selectedDuration / 60}h ${selectedDuration % 60}m"
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("$durationLabel × ${currencySymbol}${hourlyFrom?.let { trimTrailingZeros(it) } ?: ""}/hr", style = MaterialTheme.typography.bodyMedium)
                        Text("${currencySymbol}${trimTrailingZeros(subtotal)}", style = MaterialTheme.typography.bodyMedium)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        val startLabel = selectedSlotStart?.format(DateTimeFormatter.ofPattern("h:mm a")) ?: "—"
                        val endLabel = selectedEnd?.format(DateTimeFormatter.ofPattern("h:mm a")) ?: "—"
                        val timeRange = "$startLabel – $endLabel"
                        Text(timeRange, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Taxes & fees", style = MaterialTheme.typography.bodyMedium)
                        Text("${currencySymbol}${trimTrailingZeros(taxes)}", style = MaterialTheme.typography.bodyMedium)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Total", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("${currencySymbol}${trimTrailingZeros(total)}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Guests
        Spacer(modifier = Modifier.height(24.dp))
        Text("Guests", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
                onClick = { guests = (guests - 1).coerceAtLeast(1) },
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Text("−", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
                }
            }
            Spacer(modifier = Modifier.width(20.dp))
            Text(
                "$guests",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.width(20.dp))
            Surface(
                onClick = { guests = (guests + 1).coerceAtMost(20) },
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Text("+", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
                }
            }
        }

        // Booking summary card
        if (canConfirm && selectedSlotStart != null && selectedEnd != null) {
            Spacer(modifier = Modifier.height(24.dp))
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Booking summary",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    val dayLabel = selectedDate?.dayOfWeek?.name?.lowercase()
                        ?.replaceFirstChar { it.uppercase() } ?: ""
                    val monthDay = selectedDate?.format(DateTimeFormatter.ofPattern("MMM d")) ?: ""
                    val timeRange = "${selectedSlotStart?.format(DateTimeFormatter.ofPattern("h:mm a")) ?: "—"} – ${selectedEnd?.format(DateTimeFormatter.ofPattern("h:mm a")) ?: "—"}"
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("$dayLabel, $monthDay", style = MaterialTheme.typography.bodyMedium)
                        Text(timeRange, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("$guests guest${if (guests != 1) "s" else ""}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        val durationLabel = when {
                            selectedDuration < 60 -> "$selectedDuration min"
                            selectedDuration % 60 == 0 -> "${selectedDuration / 60} hr"
                            else -> "${selectedDuration / 60}h ${selectedDuration % 60}m"
                        }
                        Text(durationLabel, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (total != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Total", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text("${currencySymbol}${trimTrailingZeros(total)}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Confirm button
        Button(
            onClick = {
                val d = selectedDate ?: return@Button
                val start = selectedSlotStart ?: return@Button
                val end = selectedEnd ?: return@Button
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
                        totalAmountEstimate = total
                    )
                )
            },
            enabled = canConfirm,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            Text(
                text = if (total != null) "Confirm and Pay (${currencySymbol}${trimTrailingZeros(total)})"
                       else "Select a time slot",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "You won't be charged yet",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth(),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Spacer(modifier = Modifier.height(20.dp))
    }
}

private fun trimTrailingZeros(value: Double): String {
    val asLong = value.toLong()
    return if (value == asLong.toDouble()) asLong.toString()
    else "%.2f".format(value).trimEnd('0').trimEnd('.')
}

@Composable
private fun BookingBar(
    pricingLabel: String?,
    available: Boolean? = null,
    onReserveClick: () -> Unit
) {
    val isAvailable = available != false
    Surface(
        shadowElevation = 12.dp,
        color = PaceDreamColors.Surface,
        tonalElevation = 0.dp
    ) {
        Column {
            HorizontalDivider(
                thickness = 0.5.dp,
                color = PaceDreamColors.BorderLight
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = PaceDreamSpacing.MD, vertical = 14.dp)
                    .padding(WindowInsets.navigationBars.asPaddingValues()),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = pricingLabel ?: "Select time",
                        style = PaceDreamTypography.Title3.copy(
                            fontFamily = paceDreamFontFamily,
                            fontWeight = FontWeight.Bold
                        ),
                        color = PaceDreamColors.TextPrimary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "You won't be charged yet",
                        style = PaceDreamTypography.Caption.copy(
                            fontFamily = paceDreamFontFamily
                        ),
                        color = PaceDreamColors.TextSecondary
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Button(
                    onClick = onReserveClick,
                    enabled = isAvailable,
                    shape = RoundedCornerShape(PaceDreamRadius.MD),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PaceDreamColors.Primary,
                        disabledContainerColor = PaceDreamColors.Gray300
                    ),
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 14.dp)
                ) {
                    Text(
                        "Reserve",
                        style = PaceDreamTypography.Headline.copy(
                            fontFamily = paceDreamFontFamily,
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = Color.White
                    )
                }
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
                    imageVector = PaceDreamIcons.ArrowBack,
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
                    Icon(PaceDreamIcons.Share, contentDescription = "Share", tint = Color.White)
                }
            }
            Surface(shape = CircleShape, color = Color.Black.copy(alpha = 0.35f)) {
                IconButton(onClick = onToggleFavorite) {
                    Icon(
                        imageVector = if (isFavorite) PaceDreamIcons.Favorite else PaceDreamIcons.FavoriteBorder,
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
    propertyType: String? = null,
    instantBook: Boolean? = null,
    available: Boolean? = null,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = title,
            style = PaceDreamTypography.Title2.copy(
                fontFamily = paceDreamDisplayFontFamily,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = (-0.5).sp
            ),
            color = PaceDreamColors.TextPrimary
        )
        Spacer(modifier = Modifier.height(10.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            val hasRealRating = rating != null && rating > 0.0 && reviewCount != null && reviewCount > 0
            if (hasRealRating) {
                Icon(
                    imageVector = PaceDreamIcons.Star,
                    contentDescription = null,
                    tint = Color(0xFFFFB400),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = String.format("%.1f", rating ?: 0.0),
                    style = PaceDreamTypography.Subheadline.copy(
                        fontFamily = paceDreamFontFamily,
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = PaceDreamColors.TextPrimary
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "(${reviewCount ?: 0} ${if (reviewCount == 1) "review" else "reviews"})",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                // "New" badge instead of awkward "0.0 (0)" or empty star
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = "New",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
            }
            // Format cityState properly — ensure space after comma
            cityState?.let { raw ->
                val formatted = raw.replace(Regex(",(?!\\s)"), ", ")
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "· $formatted",
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

        // Property type badge + Instant Book badge (iOS parity)
        if (propertyType != null || instantBook == true || available != null) {
            Spacer(modifier = Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                propertyType?.takeIf { it.isNotBlank() }?.let { type ->
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                PaceDreamIcons.Home,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = type.replaceFirstChar { it.uppercase() },
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                if (instantBook == true) {
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = Color(0xFFFFF3E0)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                PaceDreamIcons.Bolt,
                                contentDescription = null,
                                tint = Color(0xFFFF9800),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Instant Book",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFFFF9800)
                            )
                        }
                    }
                }

                available?.let { isAvailable ->
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = if (isAvailable) Color(0xFF10B981).copy(alpha = 0.1f) else Color(0xFFEF4444).copy(alpha = 0.1f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(if (isAvailable) Color(0xFF10B981) else Color(0xFFEF4444))
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isAvailable) "Available" else "Unavailable",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Medium,
                                color = if (isAvailable) Color(0xFF10B981) else Color(0xFFEF4444)
                            )
                        }
                    }
                }
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
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(host?.avatarUrl)
                                .crossfade(200)
                                .size(coil.size.Size(96, 96))
                                .build(),
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
                                PaceDreamIcons.Star,
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
                                PaceDreamIcons.Verified,
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
                                    PaceDreamIcons.CheckCircle,
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
    category: String? = null,
    modifier: Modifier = Modifier
) {
    val sectionTitle = when (category?.lowercase()) {
        "car", "vehicle" -> "About this vehicle"
        "gear", "equipment", "item", "items" -> "About this item"
        "studio", "office" -> "About this workspace"
        "service", "services", "split-stay" -> "About this service"
        else -> "About this space"
    }

    Column(modifier = modifier) {
        Text(sectionTitle, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(10.dp))
        if (!description.isNullOrBlank()) {
            Text(
                text = description.trim(),
                maxLines = 5,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
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
    category: String? = null,
    modifier: Modifier = Modifier
) {
    val sectionTitle = when (category?.lowercase()) {
        "car", "vehicle" -> "What this vehicle offers"
        "gear", "equipment" -> "What's included"
        "studio", "office" -> "Workspace features"
        else -> "What this place offers"
    }

    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(sectionTitle, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.weight(1f))
            if (amenities.isNotEmpty()) {
                TextButton(onClick = onSeeAll) {
                    Text("See all")
                }
            }
        }
        Spacer(modifier = Modifier.height(10.dp))

        if (amenities.isNotEmpty()) {
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

        if (displayRating == null || displayCount == null || displayCount == 0 || displayRating == 0.0) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        PaceDreamIcons.Star,
                        contentDescription = null,
                        tint = Color(0xFFFFB400),
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "No reviews yet",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Be the first to share your experience",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = onWriteReview) {
                        Icon(PaceDreamIcons.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Write a Review")
                    }
                }
            }
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(PaceDreamIcons.Star, contentDescription = null, tint = Color(0xFFFFB400), modifier = Modifier.size(18.dp))
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
                    Icon(PaceDreamIcons.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
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
                                PaceDreamIcons.Star,
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
                                PaceDreamIcons.Star,
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
                PaceDreamIcons.Star,
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
                            PaceDreamIcons.Star,
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

    BottomSheetHeader(title = "Leave a Review", onClose = onClose)

    Column(
        modifier = Modifier.padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        // Prompt
        Text(
            "How was your experience?",
            style = PaceDreamTypography.Title3,
            fontWeight = FontWeight.SemiBold,
            color = PaceDreamColors.TextPrimary
        )
        Spacer(modifier = Modifier.height(16.dp))

        // Star rating — larger, centered
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
            content = {
                Spacer(Modifier.weight(1f))
                for (star in 1..5) {
                    Icon(
                        if (star <= rating.toInt()) PaceDreamIcons.Star else PaceDreamIcons.StarOutlined,
                        contentDescription = "Rate $star star${if (star > 1) "s" else ""}",
                        tint = if (star <= rating.toInt()) PaceDreamColors.StarRating
                        else PaceDreamColors.TextTertiary,
                        modifier = Modifier
                            .size(44.dp)
                            .clickable { rating = star.toDouble() }
                    )
                }
                Spacer(Modifier.weight(1f))
            }
        )

        // Rating label
        val ratingLabel = when (rating.toInt()) {
            1 -> "Poor"
            2 -> "Fair"
            3 -> "Good"
            4 -> "Very Good"
            5 -> "Excellent"
            else -> ""
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = ratingLabel,
            style = PaceDreamTypography.Callout,
            fontWeight = FontWeight.Medium,
            color = if (rating > 0) PaceDreamColors.Primary else Color.Transparent,
            modifier = Modifier.height(24.dp)
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Comment (optional)
        Text(
            "Write a review (optional)",
            style = PaceDreamTypography.Body,
            fontWeight = FontWeight.Medium,
            color = PaceDreamColors.TextPrimary,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = comment,
            onValueChange = { if (it.length <= 2000) comment = it },
            placeholder = { Text("Share your experience...", color = PaceDreamColors.TextTertiary) },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 100.dp, max = 140.dp),
            textStyle = PaceDreamTypography.Body.copy(color = PaceDreamColors.TextPrimary),
            shape = RoundedCornerShape(PaceDreamRadius.MD),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = PaceDreamColors.Primary,
                unfocusedBorderColor = PaceDreamColors.Divider,
                cursorColor = PaceDreamColors.Primary
            ),
            maxLines = 5
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                "Your review helps others make better decisions.",
                style = PaceDreamTypography.Caption,
                color = PaceDreamColors.TextTertiary,
                modifier = Modifier.weight(1f)
            )
            Text(
                "${comment.length}/2000",
                style = PaceDreamTypography.Caption,
                color = PaceDreamColors.TextTertiary
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Submit button
        Button(
            onClick = { onSubmit(rating, comment.trim(), null) },
            enabled = rating > 0 && !isSubmitting,
            modifier = Modifier
                .fillMaxWidth()
                .height(PaceDreamButtonHeight.MD),
            colors = ButtonDefaults.buttonColors(
                containerColor = PaceDreamColors.Primary,
                disabledContainerColor = PaceDreamColors.Divider
            ),
            shape = RoundedCornerShape(PaceDreamRadius.MD)
        ) {
            if (isSubmitting) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Submitting...",
                    style = PaceDreamTypography.Button,
                    color = Color.White
                )
            } else {
                Text(
                    "Submit Review",
                    style = PaceDreamTypography.Button,
                    fontWeight = FontWeight.SemiBold,
                    color = if (rating > 0) Color.White else PaceDreamColors.TextTertiary
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
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
                if (star <= rating.toInt()) PaceDreamIcons.Star else PaceDreamIcons.StarOutlined,
                contentDescription = "Rate $star stars",
                tint = if (star <= rating.toInt()) PaceDreamColors.StarRating
                else PaceDreamColors.TextTertiary,
                modifier = Modifier
                    .size(starSize)
                    .clickable { onRatingChanged(star.toDouble()) }
            )
        }
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
private fun SectionSplitInfo(
    totalCost: Double?,
    slotsTotal: Int?,
    slotsFilled: Int?,
    splitStatus: String?,
    deadlineAt: String?,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            "Split Details",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(10.dp))
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                if (totalCost != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Total Cost", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("$${String.format("%,.0f", totalCost)}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    }
                    if (slotsTotal != null && slotsTotal > 0) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Per Person", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("$${String.format("%,.0f", totalCost / slotsTotal)}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
                if (slotsTotal != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Slots", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("${slotsFilled ?: 0} / $slotsTotal filled", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { if (slotsTotal > 0) ((slotsFilled ?: 0).toFloat() / slotsTotal.toFloat()).coerceIn(0f, 1f) else 0f },
                        modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }
                if (splitStatus != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Status", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = when (splitStatus.uppercase()) {
                                "OPEN" -> Color(0xFF10B981).copy(alpha = 0.1f)
                                "MATCHED" -> MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                "CLOSED" -> MaterialTheme.colorScheme.error.copy(alpha = 0.1f)
                                else -> MaterialTheme.colorScheme.surfaceVariant
                            }
                        ) {
                            Text(
                                splitStatus.replaceFirstChar { it.uppercase() },
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = when (splitStatus.uppercase()) {
                                    "OPEN" -> Color(0xFF10B981)
                                    "MATCHED" -> MaterialTheme.colorScheme.primary
                                    "CLOSED" -> MaterialTheme.colorScheme.error
                                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                        }
                    }
                }
                if (deadlineAt != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Deadline", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(deadlineAt.take(10), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
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
            Text("Where you'll be", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
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
            // Use Maps Static API – works with any key that has Static Maps enabled,
            // avoids the Maps SDK for Android dependency on the API key restrictions.
            val lat = mapCoordinate.latitude
            val lng = mapCoordinate.longitude
            val staticMapUrl = "https://maps.googleapis.com/maps/api/staticmap" +
                "?center=$lat,$lng" +
                "&zoom=15" +
                "&size=600x300" +
                "&scale=2" +
                "&markers=color:red%7C$lat,$lng" +
                "&key=$mapsKey"

            val context = LocalContext.current
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(staticMapUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = "Map showing location",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else if (mapCoordinate != null && !mapsEnabled) {
            // Static map fallback using OpenStreetMap embed when no Google Maps key
            Box(modifier = Modifier.fillMaxSize()) {
                val lat = mapCoordinate.latitude
                val lng = mapCoordinate.longitude
                // Build a small bounding box around the coordinate for the embed
                val delta = 0.005
                val bbox = "${lng - delta},${lat - delta},${lng + delta},${lat + delta}"
                val embedUrl = "https://www.openstreetmap.org/export/embed.html" +
                    "?bbox=$bbox&layer=mapnik&marker=$lat,$lng"
                val context = LocalContext.current
                androidx.compose.ui.viewinterop.AndroidView(
                    factory = {
                        android.webkit.WebView(context).apply {
                            settings.javaScriptEnabled = true
                            settings.loadWithOverviewMode = true
                            settings.useWideViewPort = true
                            setBackgroundColor(android.graphics.Color.TRANSPARENT)
                            loadUrl(embedUrl)
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        RoundedCornerShape(16.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(24.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        modifier = Modifier.size(56.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            Icon(
                                PaceDreamIcons.LocationOn,
                                contentDescription = "Location",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = if (isGeocoding) "Finding location…" else "Exact location shared after booking",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (!isGeocoding) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "General area shown on map",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
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
        Column(modifier = Modifier.padding(horizontal = PaceDreamSpacing.MD)) {
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
    // Do not fabricate a default policy when the backend omits one.
    val policy = cancellationPolicy ?: return
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
                    PaceDreamIcons.CheckCircle,
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
                    icon = PaceDreamIcons.Group,
                    value = "$it",
                    label = if (it == 1) "guest" else "guests"
                )
            }
            bedrooms?.let {
                PropertyDetailItem(
                    icon = PaceDreamIcons.Home,
                    value = "$it",
                    label = if (it == 1) "bedroom" else "bedrooms"
                )
            }
            beds?.let {
                PropertyDetailItem(
                    icon = PaceDreamIcons.Bed,
                    value = "$it",
                    label = if (it == 1) "bed" else "beds"
                )
            }
            bathrooms?.let {
                PropertyDetailItem(
                    icon = PaceDreamIcons.Bathtub,
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
                PaceDreamIcons.Rule,
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
                            Icon(PaceDreamIcons.AccessTime, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Check-in", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(it, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    checkOutTime?.let {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(PaceDreamIcons.AccessTime, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
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
                PaceDreamIcons.Security,
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
                            PaceDreamIcons.CheckCircle,
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
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.weight(1f))
            TextButton(onClick = onClose) { Text("Close") }
        }
        HorizontalDivider()
    }
}

// ── Report Listing (iOS/Web parity) ─────────────────────────────

@Composable
private fun SectionReportListing(
    onReportClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                PaceDreamIcons.Flag,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                "Report this listing",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clickable { onReportClick() }
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            "If something looks wrong with this listing, let us know.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReportListingSheet(
    listingId: String,
    listingTitle: String,
    onDismiss: () -> Unit,
    onReportSubmitted: () -> Unit
) {
    val reportReasons = listOf(
        "Spam or scam",
        "Offensive or inappropriate content",
        "Harassment or bullying",
        "Misleading or fraudulent listing",
        "Safety concern",
        "Impersonation",
        "Other"
    )

    var selectedReason by remember { mutableStateOf<String?>(null) }
    var details by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = PaceDreamSpacing.MD)
                .padding(bottom = 32.dp)
        ) {
            Text(
                "Report Listing",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Why are you reporting \"$listingTitle\"?",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(16.dp))

            reportReasons.forEach { reason ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { selectedReason = reason }
                        .padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = selectedReason == reason,
                        onClick = { selectedReason = reason }
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(reason, style = MaterialTheme.typography.bodyMedium)
                }
            }

            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = details,
                onValueChange = { if (it.length <= 5000) details = it },
                label = { Text("Additional details (optional)") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp),
                shape = RoundedCornerShape(12.dp),
                maxLines = 5
            )

            Spacer(Modifier.height(20.dp))
            Button(
                onClick = {
                    isSubmitting = true
                    onReportSubmitted()
                },
                enabled = selectedReason != null && !isSubmitting,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                shape = RoundedCornerShape(24.dp)
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White
                    )
                } else {
                    Text("Submit Report", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
