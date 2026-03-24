package com.shourov.apps.pacedream.feature.host.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import com.pacedream.common.icon.PaceDreamIcons
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pacedream.common.composables.theme.*

/**
 * Host Post Screen — iOS PostStartView parity.
 *
 * Hub for listing creation with:
 * - Hero header with gradient
 * - Host stats section
 * - Category selection grid (Spaces, Items, Services)
 * - Quick actions (Create, Manage, Analytics)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HostPostScreen(
    viewModel: HostDashboardViewModel = hiltViewModel(),
    onCreateListingClick: () -> Unit = {},
    onCreateListingWithType: (String) -> Unit = {},
    onMyListingsClick: () -> Unit = {},
    onAnalyticsClick: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Host Dashboard",
                        style = PaceDreamTypography.Title1,
                        color = PaceDreamColors.TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    TextButton(onClick = onMyListingsClick) {
                        Text(
                            text = "My Listings",
                            style = PaceDreamTypography.Callout,
                            color = PaceDreamColors.Primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PaceDreamColors.Background)
            )
        },
        containerColor = PaceDreamColors.Background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Hero Header
            PostHeroHeader(userName = uiState.userName)

            // Host Stats
            PostStatsSection(
                activeListings = uiState.activeListingsCount,
                monthlyEarnings = uiState.monthlyEarnings,
                upcomingBookings = uiState.upcomingBookingsCount
            )

            // Category Selection Grid
            PostCategoryGrid(onCategoryClick = onCreateListingWithType)

            // Quick Actions
            PostQuickActions(
                onCreateClick = onCreateListingClick,
                onManageClick = onMyListingsClick,
                onAnalyticsClick = onAnalyticsClick
            )

            Spacer(modifier = Modifier.height(28.dp))
        }
    }
}

// ── Hero Header ─────────────────────────────────────────────────

@Composable
private fun PostHeroHeader(userName: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = PaceDreamSpacing.MD)
            .padding(top = 12.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        PaceDreamColors.Primary.copy(alpha = 0.8f),
                        PaceDreamColors.Primary.copy(alpha = 0.6f)
                    )
                )
            )
            .height(200.dp),
        contentAlignment = Alignment.BottomStart
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .padding(bottom = 20.dp)
        ) {
            val firstName = userName.split(" ").firstOrNull()?.ifBlank { "Host" } ?: "Host"
            Text(
                text = "Welcome back, $firstName!",
                style = PaceDreamTypography.Title2.copy(fontSize = 24.sp),
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Create amazing listings and grow your hosting business",
                style = PaceDreamTypography.Callout,
                color = Color.White.copy(alpha = 0.95f),
                fontWeight = FontWeight.Medium
            )
        }
    }
}

// ── Stats Section ───────────────────────────────────────────────

@Composable
private fun PostStatsSection(
    activeListings: Int,
    monthlyEarnings: Double,
    upcomingBookings: Int
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = PaceDreamSpacing.MD),
        horizontalArrangement = Arrangement.spacedBy(PaceDreamSpacing.MD)
    ) {
        PostStatCard(
            title = "Active Listings",
            value = "$activeListings",
            icon = PaceDreamIcons.Home,
            color = PaceDreamColors.Success,
            modifier = Modifier.weight(1f)
        )
        PostStatCard(
            title = "This Month",
            value = "$${String.format("%.0f", monthlyEarnings)}",
            icon = PaceDreamIcons.AttachMoney,
            color = PaceDreamColors.Info,
            modifier = Modifier.weight(1f)
        )
        PostStatCard(
            title = "Bookings",
            value = "$upcomingBookings",
            icon = PaceDreamIcons.CalendarToday,
            color = PaceDreamColors.Warning,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun PostStatCard(
    title: String,
    value: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = PaceDreamColors.Card),
        elevation = CardDefaults.cardElevation(defaultElevation = PaceDreamElevation.XS),
        shape = RoundedCornerShape(PaceDreamRadius.MD)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(PaceDreamSpacing.MD)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))
            Text(
                text = value,
                style = PaceDreamTypography.Title3.copy(fontSize = 20.sp),
                color = PaceDreamColors.TextPrimary,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = title,
                style = PaceDreamTypography.Caption,
                color = PaceDreamColors.TextSecondary,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// ── Category Selection Grid ─────────────────────────────────────

private data class ResourceCardData(
    val id: String,
    val resourceKind: String,
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val color: Color
)

@Composable
private fun PostCategoryGrid(onCategoryClick: (String) -> Unit) {
    val categories = remember {
        listOf(
            ResourceCardData("spaces", "share", "Spaces", "Share spaces like rooms, parking, studios", PaceDreamIcons.Home, PaceDreamColors.Primary),
            ResourceCardData("items", "borrow", "Items", "Rent out gear, tools, electronics", PaceDreamIcons.ShoppingBag, PaceDreamColors.Info),
            ResourceCardData("services", "share", "Services", "Offer help, skills, or experiences", PaceDreamIcons.Build, PaceDreamColors.Success)
        )
    }

    Column(
        modifier = Modifier.padding(horizontal = PaceDreamSpacing.MD),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "What would you like to list?",
            style = PaceDreamTypography.Title2,
            color = PaceDreamColors.TextPrimary,
            fontWeight = FontWeight.Bold
        )

        categories.forEach { card ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                onClick = { onCategoryClick(card.resourceKind) },
                colors = CardDefaults.cardColors(containerColor = PaceDreamColors.Card),
                elevation = CardDefaults.cardElevation(defaultElevation = PaceDreamElevation.XS),
                shape = RoundedCornerShape(PaceDreamRadius.LG)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(PaceDreamSpacing.MD),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(card.color.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = card.icon,
                            contentDescription = null,
                            tint = card.color,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = card.title,
                            style = PaceDreamTypography.Callout,
                            color = PaceDreamColors.TextPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = card.subtitle,
                            style = PaceDreamTypography.Caption,
                            color = PaceDreamColors.TextSecondary,
                            fontWeight = FontWeight.Medium,
                            maxLines = 2
                        )
                    }

                    Icon(
                        imageVector = PaceDreamIcons.ChevronRight,
                        contentDescription = null,
                        tint = PaceDreamColors.TextSecondary,
                        modifier = Modifier.size(PaceDreamIconSize.XS)
                    )
                }
            }
        }
    }
}

// ── Quick Actions ───────────────────────────────────────────────

@Composable
private fun PostQuickActions(
    onCreateClick: () -> Unit,
    onManageClick: () -> Unit,
    onAnalyticsClick: () -> Unit
) {
    Column(
        modifier = Modifier.padding(horizontal = PaceDreamSpacing.MD),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Create New Listing - primary CTA
        Button(
            onClick = onCreateClick,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = PaceDreamColors.Primary),
            shape = RoundedCornerShape(PaceDreamRadius.MD),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Icon(
                imageVector = PaceDreamIcons.AddCircle,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "Create New Listing",
                style = PaceDreamTypography.Callout,
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = PaceDreamIcons.ChevronRight,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(14.dp)
            )
        }

        // Manage My Listings
        Button(
            onClick = onManageClick,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = PaceDreamColors.Primary),
            shape = RoundedCornerShape(PaceDreamRadius.MD),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Icon(
                imageVector = PaceDreamIcons.ListIcon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "Manage My Listings",
                style = PaceDreamTypography.Callout,
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = PaceDreamIcons.ChevronRight,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(14.dp)
            )
        }

        // View Analytics - outline style
        Button(
            onClick = onAnalyticsClick,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = PaceDreamColors.Primary.copy(alpha = 0.1f)),
            shape = RoundedCornerShape(PaceDreamRadius.MD),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
        ) {
            Icon(
                imageVector = PaceDreamIcons.Analytics,
                contentDescription = null,
                tint = PaceDreamColors.Primary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "View Analytics",
                style = PaceDreamTypography.Callout,
                color = PaceDreamColors.Primary,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = PaceDreamIcons.ChevronRight,
                contentDescription = null,
                tint = PaceDreamColors.Primary,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}
