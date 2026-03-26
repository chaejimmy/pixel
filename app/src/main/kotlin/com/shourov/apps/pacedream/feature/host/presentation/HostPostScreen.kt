package com.shourov.apps.pacedream.feature.host.presentation

import androidx.compose.foundation.background
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
import com.shourov.apps.pacedream.feature.host.presentation.components.*

/**
 * Host Post Screen — iOS PostStartView parity.
 *
 * Hub for listing creation with:
 * - Hero header with gradient (HostAccent tinted)
 * - Host stats section (unified KPI chips)
 * - Category selection grid
 * - Quick actions
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
                        style = PaceDreamTypography.Title1.copy(fontWeight = FontWeight.Bold),
                        color = PaceDreamColors.TextPrimary
                    )
                },
                actions = {
                    TextButton(onClick = onMyListingsClick) {
                        Text(
                            text = "My Listings",
                            style = PaceDreamTypography.Callout.copy(fontWeight = FontWeight.SemiBold),
                            color = PaceDreamColors.HostAccent
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

            // Host Stats — unified KPI chips
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
            .clip(RoundedCornerShape(PaceDreamRadius.LG))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        PaceDreamColors.HostAccent.copy(alpha = 0.8f),
                        PaceDreamColors.HostAccent.copy(alpha = 0.6f)
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
        horizontalArrangement = Arrangement.spacedBy(PaceDreamSpacing.SM)
    ) {
        PostStatCard(
            title = "Active Listings",
            value = "$activeListings",
            icon = PaceDreamIcons.Home,
            modifier = Modifier.weight(1f)
        )
        PostStatCard(
            title = "This Month",
            value = "$${String.format("%.0f", monthlyEarnings)}",
            icon = PaceDreamIcons.AttachMoney,
            modifier = Modifier.weight(1f)
        )
        PostStatCard(
            title = "Bookings",
            value = "$upcomingBookings",
            icon = PaceDreamIcons.CalendarToday,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun PostStatCard(
    title: String,
    value: String,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = PaceDreamColors.Card),
        elevation = CardDefaults.cardElevation(defaultElevation = PaceDreamElevation.XS),
        shape = RoundedCornerShape(PaceDreamRadius.LG)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = PaceDreamColors.HostAccent,
                modifier = Modifier.size(PaceDreamIconSize.SM)
            )
            Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))
            Text(
                text = value,
                style = PaceDreamTypography.Title3,
                color = PaceDreamColors.TextPrimary,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = title,
                style = PaceDreamTypography.Caption.copy(fontWeight = FontWeight.SemiBold),
                color = PaceDreamColors.TextSecondary,
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
            ResourceCardData("spaces", "share", "Spaces", "Share spaces like rooms, parking, studios", PaceDreamIcons.Home, PaceDreamColors.HostAccent),
            ResourceCardData("items", "borrow", "Items", "Rent out gear, tools, electronics", PaceDreamIcons.ShoppingBag, PaceDreamColors.HostAccent),
            ResourceCardData("services", "share", "Services", "Offer help, skills, or experiences", PaceDreamIcons.Build, PaceDreamColors.HostAccent)
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
                            .background(card.color.copy(alpha = 0.10f)),
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
                            style = PaceDreamTypography.Callout.copy(fontWeight = FontWeight.SemiBold),
                            color = PaceDreamColors.TextPrimary
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
                        tint = PaceDreamColors.TextTertiary,
                        modifier = Modifier.size(18.dp)
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
        HostFullWidthButton(
            icon = PaceDreamIcons.AddCircle,
            title = "Create New Listing",
            onClick = onCreateClick
        )

        HostFullWidthButton(
            icon = PaceDreamIcons.ListIcon,
            title = "Manage My Listings",
            onClick = onManageClick
        )

        // View Analytics - outline/secondary style
        HostFullWidthButton(
            icon = PaceDreamIcons.Analytics,
            title = "View Analytics",
            onClick = onAnalyticsClick,
            containerColor = PaceDreamColors.HostAccent.copy(alpha = 0.1f),
            contentColor = PaceDreamColors.HostAccent
        )
    }
}
