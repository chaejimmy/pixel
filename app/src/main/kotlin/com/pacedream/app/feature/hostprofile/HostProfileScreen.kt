package com.pacedream.app.feature.hostprofile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
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

private val HostProfileMaxWidth = 600.dp
private val GridBreakpoint = 600.dp

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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.TopCenter,
        ) {
            when (uiState) {
                HostProfileUiState.Loading -> CenteredLoading()
                HostProfileUiState.NotFound -> NotFoundState(onBackClick = onBackClick)
                is HostProfileUiState.Error -> ErrorState(message = uiState.message, onRetry = onRetry)
                is HostProfileUiState.Content -> HostProfileContent(
                    state = uiState,
                    onMessageHost = onMessageHost,
                    onListingClick = onListingClick,
                )
            }
        }
    }
}

@Composable
private fun CenteredLoading() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
private fun NotFoundState(onBackClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(PaceDreamSpacing.LG),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Host not found.",
            style = MaterialTheme.typography.titleMedium,
        )
        Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))
        Text(
            text = "This host profile is no longer available.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(PaceDreamSpacing.MD))
        OutlinedButton(onClick = onBackClick) { Text("Go back") }
    }
}

@Composable
private fun ErrorState(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(PaceDreamSpacing.LG),
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

@Composable
private fun HostProfileContent(
    state: HostProfileUiState.Content,
    onMessageHost: () -> Unit,
    onListingClick: (HostListingSummary) -> Unit,
) {
    val host = state.host
    BoxWithConstraints(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter,
    ) {
        val columns = if (maxWidth >= GridBreakpoint) 2 else 1
        LazyVerticalGrid(
            columns = GridCells.Fixed(columns),
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = HostProfileMaxWidth),
            contentPadding = PaddingValues(
                horizontal = PaceDreamSpacing.MD,
                vertical = PaceDreamSpacing.MD,
            ),
            horizontalArrangement = Arrangement.spacedBy(PaceDreamSpacing.MD),
            verticalArrangement = Arrangement.spacedBy(PaceDreamSpacing.MD),
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                HeaderSection(
                    host = host,
                    isContactingHost = state.isContactingHost,
                    onMessageHost = onMessageHost,
                )
            }
            item(span = { GridItemSpan(maxLineSpan) }) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
            item(span = { GridItemSpan(maxLineSpan) }) { AboutSection(bio = host.bio) }
            item(span = { GridItemSpan(maxLineSpan) }) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
            item(span = { GridItemSpan(maxLineSpan) }) {
                ListingsSectionTitle(firstName = host.firstName ?: host.name?.split(" ")?.firstOrNull())
            }
            if (host.listings.isEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) { EmptyListings() }
            } else {
                items(host.listings, key = { it.id }) { listing ->
                    HostListingCard(
                        listing = listing,
                        onClick = { onListingClick(listing) },
                    )
                }
            }
            item(span = { GridItemSpan(maxLineSpan) }) {
                Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))
                ContactCard(
                    hostName = host.firstName ?: host.name,
                    isLoading = state.isContactingHost,
                    onMessageHost = onMessageHost,
                )
            }
        }
    }
}

@Composable
private fun HeaderSection(
    host: HostProfileModel,
    isContactingHost: Boolean,
    onMessageHost: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = PaceDreamSpacing.SM),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        HostAvatar(avatarUrl = host.avatarUrl, name = host.name, size = 104)
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
        host.location?.display?.let { display ->
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
                    text = display,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (host.hasInlineChips) {
            Spacer(modifier = Modifier.height(PaceDreamSpacing.SM2))
            HeaderChips(host = host)
        }
        Spacer(modifier = Modifier.height(PaceDreamSpacing.MD))
        Button(
            onClick = onMessageHost,
            enabled = !isContactingHost,
            shape = RoundedCornerShape(PaceDreamRadius.MD),
        ) {
            if (isContactingHost) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
                Spacer(modifier = Modifier.width(PaceDreamSpacing.SM))
            }
            Text(messageButtonLabel(host.firstName ?: host.name))
        }
    }
}

@Composable
private fun HeaderChips(host: HostProfileModel) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(PaceDreamSpacing.SM)) {
            host.rating?.let { rating ->
                Chip(
                    icon = PaceDreamIcons.Star,
                    text = buildString {
                        append(String.format("%.1f", rating))
                        host.reviewCount?.takeIf { it > 0 }?.let { append(" · $it") }
                    },
                )
            }
            host.joinedAt?.takeIf { it.isNotBlank() }?.let { joined ->
                Chip(text = "Joined ${formatJoinedYear(joined)}")
            }
            host.verifiedBadges.firstOrNull()?.let { firstBadge ->
                Chip(
                    icon = PaceDreamIcons.CheckCircle,
                    text = if (host.verifiedBadges.size == 1) {
                        firstBadge.replaceFirstChar { it.uppercase() } + " verified"
                    } else {
                        "Verified · ${host.verifiedBadges.size}"
                    },
                )
            }
        }
    }
}

@Composable
private fun Chip(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
) {
    Surface(
        shape = RoundedCornerShape(PaceDreamRadius.Round),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = PaceDreamSpacing.SM,
                vertical = PaceDreamSpacing.XS,
            ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (icon != null) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(14.dp),
                )
                Spacer(modifier = Modifier.width(PaceDreamSpacing.XS))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
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
private fun AboutSection(bio: String?) {
    Column(modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = PaceDreamSpacing.SM)) {
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
private fun ListingsSectionTitle(firstName: String?) {
    val name = firstName?.takeIf { it.isNotBlank() } ?: "this host"
    Text(
        text = "Listings by $name",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(vertical = PaceDreamSpacing.SM),
    )
}

@Composable
private fun EmptyListings() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = PaceDreamSpacing.LG),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "This host doesn't have any listings yet.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun HostListingCard(
    listing: HostListingSummary,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(PaceDreamRadius.LG),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 11f)
                .clip(RoundedCornerShape(
                    topStart = PaceDreamRadius.LG,
                    topEnd = PaceDreamRadius.LG,
                ))
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
        Column(modifier = Modifier.padding(PaceDreamSpacing.SM2)) {
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
                Spacer(modifier = Modifier.height(PaceDreamSpacing.XS))
                Text(
                    text = price,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
    }
}

@Composable
private fun ContactCard(
    hostName: String?,
    isLoading: Boolean,
    onMessageHost: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(PaceDreamRadius.LG),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(PaceDreamSpacing.MD)) {
            Text(
                text = "Have a question?",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(PaceDreamSpacing.XS))
            Text(
                text = "Send a message to learn more about availability, the space, or anything else.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(PaceDreamSpacing.SM2))
            OutlinedButton(
                onClick = onMessageHost,
                enabled = !isLoading,
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                    )
                    Spacer(modifier = Modifier.width(PaceDreamSpacing.SM))
                }
                Text(messageButtonLabel(hostName))
            }
        }
    }
}

private fun messageButtonLabel(name: String?): String {
    val first = name?.split(" ")?.firstOrNull()?.takeIf { it.isNotBlank() }
    return if (first != null) "Message $first" else "Message Host"
}

private val HostProfileModel.hasInlineChips: Boolean
    get() = rating != null ||
        !joinedAt.isNullOrBlank() ||
        verifiedBadges.isNotEmpty()

private fun formatJoinedYear(raw: String): String {
    val year = raw.take(4)
    return if (year.length == 4 && year.all { it.isDigit() }) year else raw
}
