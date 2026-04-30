package com.pacedream.app.feature.hostprofile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.pacedream.common.composables.theme.PaceDreamRadius
import com.pacedream.common.composables.theme.PaceDreamSpacing
import com.pacedream.common.icon.PaceDreamIcons

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HostProfileScreen(
    uiState: HostProfileUiState,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    onBackClick: () -> Unit,
    onMessageHost: () -> Unit,
    onListingClick: (HostListingSummary) -> Unit,
    onRetry: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Host", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(PaceDreamIcons.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        when {
            uiState.isLoading && uiState.host == null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }
            uiState.host == null && uiState.errorMessage != null -> {
                ErrorState(
                    message = uiState.errorMessage,
                    onRetry = onRetry,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                )
            }
            uiState.host != null -> {
                HostProfileContent(
                    host = uiState.host,
                    isContactingHost = uiState.isContactingHost,
                    contentPadding = padding,
                    onMessageHost = onMessageHost,
                    onListingClick = onListingClick,
                )
            }
        }
    }
}

@Composable
private fun HostProfileContent(
    host: HostProfileModel,
    isContactingHost: Boolean,
    contentPadding: PaddingValues,
    onMessageHost: () -> Unit,
    onListingClick: (HostListingSummary) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            top = contentPadding.calculateTopPadding(),
            bottom = contentPadding.calculateBottomPadding() + PaceDreamSpacing.LG,
        ),
    ) {
        item { HeaderSection(host = host) }

        item { HorizontalDivider(modifier = Modifier.padding(horizontal = PaceDreamSpacing.MD)) }

        // Optional trust signals — render only when at least one real value exists.
        if (host.hasTrustSignals) {
            item { TrustSignalsRow(host = host) }
            item { HorizontalDivider(modifier = Modifier.padding(horizontal = PaceDreamSpacing.MD)) }
        }

        item { AboutSection(bio = host.bio) }

        item { HorizontalDivider(modifier = Modifier.padding(horizontal = PaceDreamSpacing.MD)) }

        item { ListingsSectionHeader(count = host.listings.size) }
        if (host.listings.isEmpty()) {
            item { EmptyListings() }
        } else {
            items(host.listings, key = { it.id }) { listing ->
                HostListingRow(
                    listing = listing,
                    onClick = { onListingClick(listing) },
                    modifier = Modifier.padding(
                        horizontal = PaceDreamSpacing.MD,
                        vertical = PaceDreamSpacing.SM,
                    ),
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(PaceDreamSpacing.LG))
            ContactCta(
                hostName = host.name,
                isLoading = isContactingHost,
                onMessageHost = onMessageHost,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = PaceDreamSpacing.MD),
            )
        }
    }
}

@Composable
private fun HeaderSection(host: HostProfileModel) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(PaceDreamSpacing.MD),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        HostAvatar(
            avatarUrl = host.avatarUrl,
            name = host.name,
            size = 96,
        )
        Spacer(modifier = Modifier.height(PaceDreamSpacing.SM2))
        Text(
            text = host.name?.takeIf { it.isNotBlank() } ?: "Host",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = "Host on PaceDream",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        host.location?.takeIf { it.isNotBlank() }?.let { loc ->
            Spacer(modifier = Modifier.height(PaceDreamSpacing.XS))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    PaceDreamIcons.LocationOn,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(modifier = Modifier.width(PaceDreamSpacing.XS))
                Text(
                    text = loc,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun HostAvatar(avatarUrl: String?, name: String?, size: Int) {
    val initials = (name ?: "Host")
        .trim()
        .split(" ")
        .filter { it.isNotBlank() }
        .take(2)
        .mapNotNull { it.firstOrNull()?.toString() }
        .joinToString("")
        .uppercase()
        .ifBlank { "H" }

    if (!avatarUrl.isNullOrBlank()) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(avatarUrl)
                .crossfade(200)
                .build(),
            contentDescription = null,
            modifier = Modifier
                .size(size.dp)
                .clip(CircleShape),
        )
    } else {
        Box(
            modifier = Modifier
                .size(size.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = initials,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun TrustSignalsRow(host: HostProfileModel) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(PaceDreamSpacing.MD),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        host.rating?.let { rating ->
            TrustStat(
                value = String.format("%.1f", rating),
                label = host.reviewCount?.let { "$it reviews" } ?: "Rating",
            )
        }
        host.completedBookings?.takeIf { it > 0 }?.let { bookings ->
            TrustStat(value = bookings.toString(), label = "Bookings")
        }
        host.responseTime?.takeIf { it.isNotBlank() }?.let { time ->
            TrustStat(value = time, label = "Response time")
        }
        host.joinedAt?.takeIf { it.isNotBlank() }?.let { joined ->
            TrustStat(value = formatJoinedYear(joined), label = "Joined")
        }
    }
    if (host.verifiedBadges.isNotEmpty()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = PaceDreamSpacing.MD, vertical = PaceDreamSpacing.SM),
            horizontalArrangement = Arrangement.spacedBy(PaceDreamSpacing.SM),
        ) {
            host.verifiedBadges.take(4).forEach { badge ->
                Surface(
                    shape = RoundedCornerShape(PaceDreamRadius.Round),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                ) {
                    Row(
                        modifier = Modifier.padding(
                            horizontal = PaceDreamSpacing.SM,
                            vertical = PaceDreamSpacing.XS,
                        ),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            PaceDreamIcons.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp),
                        )
                        Spacer(modifier = Modifier.width(PaceDreamSpacing.XS))
                        Text(
                            text = badge.replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TrustStat(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun AboutSection(bio: String?) {
    Column(modifier = Modifier
        .fillMaxWidth()
        .padding(PaceDreamSpacing.MD)) {
        Text(
            text = "About",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))
        val text = bio?.takeIf { it.isNotBlank() }
            ?: "This host has not added a bio yet."
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = if (bio.isNullOrBlank())
                MaterialTheme.colorScheme.onSurfaceVariant
            else MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun ListingsSectionHeader(count: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = PaceDreamSpacing.MD, vertical = PaceDreamSpacing.MD),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "Listings",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        if (count > 0) {
            Spacer(modifier = Modifier.width(PaceDreamSpacing.SM))
            Text(
                text = "($count)",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun EmptyListings() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = PaceDreamSpacing.MD, vertical = PaceDreamSpacing.LG),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "No listings to show yet.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun HostListingRow(
    listing: HostListingSummary,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(PaceDreamRadius.LG),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Row(
            modifier = Modifier.padding(PaceDreamSpacing.SM2),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(PaceDreamRadius.MD))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center,
            ) {
                if (!listing.imageUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(listing.imageUrl)
                            .crossfade(200)
                            .build(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    Icon(
                        PaceDreamIcons.Apartment,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(modifier = Modifier.width(PaceDreamSpacing.SM2))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = listing.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                listing.location?.takeIf { it.isNotBlank() }?.let { loc ->
                    Text(
                        text = loc,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                listing.priceLabel?.takeIf { it.isNotBlank() }?.let { price ->
                    Text(
                        text = price,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
        }
    }
}

@Composable
private fun ContactCta(
    hostName: String?,
    isLoading: Boolean,
    onMessageHost: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Button(
        onClick = onMessageHost,
        enabled = !isLoading,
        modifier = modifier,
        shape = RoundedCornerShape(PaceDreamRadius.MD),
        colors = ButtonDefaults.buttonColors(),
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.onPrimary,
            )
            Spacer(modifier = Modifier.width(PaceDreamSpacing.SM))
        }
        val label = hostName?.split(" ")?.firstOrNull()?.takeIf { it.isNotBlank() }
        Text(if (label != null) "Message $label" else "Message Host")
    }
}

@Composable
private fun ErrorState(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(PaceDreamSpacing.LG),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(PaceDreamSpacing.MD))
        OutlinedButton(onClick = onRetry) { Text("Try again") }
    }
}

private val HostProfileModel.hasTrustSignals: Boolean
    get() = rating != null ||
        (completedBookings != null && completedBookings > 0) ||
        !responseTime.isNullOrBlank() ||
        !joinedAt.isNullOrBlank() ||
        verifiedBadges.isNotEmpty()

private fun formatJoinedYear(raw: String): String {
    // "2023-09-12T..." or "2023-09-12" → "2023". Otherwise return raw.
    val year = raw.take(4)
    return if (year.length == 4 && year.all { it.isDigit() }) year else raw
}

