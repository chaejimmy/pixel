package com.shourov.apps.pacedream.feature.host.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import coil.compose.AsyncImage
import com.pacedream.common.composables.theme.*
import com.pacedream.common.icon.PaceDreamIcons
import com.shourov.apps.pacedream.feature.host.data.HostRepository
import com.shourov.apps.pacedream.feature.host.data.SessionType
import com.shourov.apps.pacedream.model.Property
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Host-side listing preview screen. Shows the host's own listing
 * (including pending/under-review) without any guest-mode dependencies.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HostListingPreviewScreen(
    listingId: String,
    onBackClick: () -> Unit,
    onEditClick: ((String) -> Unit)? = null,
    viewModel: HostListingPreviewViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(listingId) {
        viewModel.load(listingId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Listing Preview", style = PaceDreamTypography.Headline) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(PaceDreamIcons.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // Capture the callback in a local val so Kotlin can smart-
                    // cast it as non-null inside the lambda.  Without this the
                    // compiler rejects `onEditClick(targetId)` even though the
                    // null check above proves it is non-null — function params
                    // are tracked across lambdas only via a locally stable ref.
                    val editHandler = onEditClick
                    if (editHandler != null) {
                        val targetId = uiState.listing?.id?.takeIf { it.isNotBlank() }
                            ?: listingId
                        IconButton(
                            onClick = { editHandler(targetId) },
                            enabled = !uiState.isLoading && uiState.listing != null &&
                                targetId.isNotBlank(),
                        ) {
                            Icon(
                                PaceDreamIcons.Edit,
                                contentDescription = "Edit listing",
                                tint = PaceDreamColors.HostAccent,
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = PaceDreamColors.Background
                )
            )
        },
        containerColor = PaceDreamColors.Background
    ) { padding ->
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = PaceDreamColors.HostAccent)
                }
            }

            uiState.error != null -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            uiState.error ?: "Failed to load listing",
                            color = PaceDreamColors.TextSecondary
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = { viewModel.load(listingId) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = PaceDreamColors.HostAccent
                            )
                        ) {
                            Text("Retry")
                        }
                    }
                }
            }

            uiState.listing != null -> {
                val listing = uiState.listing ?: return@Scaffold
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(bottom = 32.dp)
                ) {
                    // Hero image
                    if (listing.images.isNotEmpty()) {
                        item {
                            AsyncImage(
                                model = listing.images.first(),
                                contentDescription = listing.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(240.dp)
                            )
                        }
                    }

                    // Under Review banner
                    if (listing.isPendingReview) {
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp, vertical = 12.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFFFFA500).copy(alpha = 0.10f))
                                    .border(
                                        1.dp,
                                        Color(0xFFFFA500).copy(alpha = 0.25f),
                                        RoundedCornerShape(12.dp)
                                    )
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    PaceDreamIcons.Schedule,
                                    contentDescription = null,
                                    tint = Color(0xFFFFA500),
                                    modifier = Modifier.size(20.dp)
                                )
                                Column {
                                    Text(
                                        "Under Review",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = PaceDreamColors.TextPrimary
                                    )
                                    Text(
                                        "Your listing is being reviewed and will be visible to guests once approved.",
                                        fontSize = 12.sp,
                                        color = PaceDreamColors.TextSecondary
                                    )
                                }
                            }
                        }
                    }

                    // Title + status
                    item {
                        Column(
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)
                        ) {
                            Text(
                                listing.title.ifBlank { "Untitled listing" },
                                style = PaceDreamTypography.Title2,
                                fontWeight = FontWeight.Bold,
                                color = PaceDreamColors.TextPrimary
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            // Delivery-mode badge — surfaces the host's
                            // session-type choice so guests immediately
                            // know whether the listing is remote, in
                            // person, or both.  Legacy listings with no
                            // session type skip the badge entirely.
                            val deliveryLabel = SessionType
                                .fromValue(listing.sessionType)
                                ?.displayLabel
                            if (deliveryLabel != null) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(50))
                                        .background(PaceDreamColors.HostAccent.copy(alpha = 0.12f))
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        deliveryLabel,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = PaceDreamColors.HostAccent
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                            }

                            // Location — hidden for online-only service
                            // listings so we don't render a stray
                            // "city, state" line pulled from cached
                            // form state that no longer applies.
                            val hideAddress = SessionType
                                .fromValue(listing.sessionType) == SessionType.ONLINE
                            val location = if (hideAddress) "" else listOfNotNull(
                                listing.location.city.takeIf { it.isNotBlank() },
                                listing.location.state.takeIf { it.isNotBlank() },
                                listing.location.country.takeIf { it.isNotBlank() }
                            ).joinToString(", ")
                            if (location.isNotBlank()) {
                                Text(
                                    location,
                                    style = PaceDreamTypography.Subheadline,
                                    color = PaceDreamColors.TextSecondary
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                            }

                            // Price
                            if (listing.pricing.basePrice > 0) {
                                Text(
                                    "$${listing.pricing.basePrice.toInt()}/${listing.pricing.unit.ifBlank { "hr" }}",
                                    style = PaceDreamTypography.Title3,
                                    fontWeight = FontWeight.Bold,
                                    color = PaceDreamColors.HostAccent
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                            }

                            // Status badge
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(
                                        when {
                                            listing.isPendingReview -> PaceDreamColors.Warning.copy(alpha = 0.15f)
                                            listing.isActiveStatus -> PaceDreamColors.HostAccent.copy(alpha = 0.15f)
                                            else -> Color.Gray.copy(alpha = 0.15f)
                                        }
                                    )
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    listing.displayStatus,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = when {
                                        listing.isPendingReview -> PaceDreamColors.Warning
                                        listing.isActiveStatus -> PaceDreamColors.HostAccent
                                        else -> PaceDreamColors.TextSecondary
                                    }
                                )
                            }
                        }
                    }

                    // Description
                    if (listing.description.isNotBlank()) {
                        item {
                            Column(
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    "Description",
                                    style = PaceDreamTypography.Callout,
                                    fontWeight = FontWeight.SemiBold,
                                    color = PaceDreamColors.TextPrimary
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    listing.description,
                                    style = PaceDreamTypography.Subheadline,
                                    color = PaceDreamColors.TextSecondary
                                )
                            }
                        }
                    }

                    // All images gallery
                    if (listing.images.size > 1) {
                        item {
                            Column(
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    "Photos (${listing.images.size})",
                                    style = PaceDreamTypography.Callout,
                                    fontWeight = FontWeight.SemiBold,
                                    color = PaceDreamColors.TextPrimary
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                listing.images.forEach { imageUrl ->
                                    AsyncImage(
                                        model = imageUrl,
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(200.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                }
                            }
                        }
                    }

                    // Listing ID
                    item {
                        Text(
                            "Listing ID: $listingId",
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                            style = PaceDreamTypography.Caption,
                            color = PaceDreamColors.TextTertiary
                        )
                    }
                }
            }
        }
    }
}

@HiltViewModel
class HostListingPreviewViewModel @Inject constructor(
    private val hostRepository: HostRepository
) : ViewModel() {

    data class UiState(
        val listing: Property? = null,
        val isLoading: Boolean = false,
        val error: String? = null
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    fun load(listingId: String) {
        if (listingId.isBlank()) {
            _uiState.value = UiState(error = "No listing ID")
            return
        }
        viewModelScope.launch {
            _uiState.value = UiState(isLoading = true)
            // Find the listing from the host's own listings (includes pending)
            hostRepository.getHostListings()
                .onSuccess { listings ->
                    val match = listings.firstOrNull { it.id == listingId }
                    if (match != null) {
                        _uiState.value = UiState(listing = match)
                    } else {
                        _uiState.value = UiState(error = "Listing not found. It may still be processing.")
                    }
                }
                .onFailure { e ->
                    _uiState.value = UiState(error = com.pacedream.common.util.UserFacingErrorMapper.forLoadProperties(e))
                }
        }
    }
}
