package com.shourov.apps.pacedream.feature.host.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import com.pacedream.common.icon.PaceDreamIcons
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.pacedream.common.composables.theme.*

/**
 * Host Profile Screen — iOS parity.
 *
 * Dedicated host profile matching iOS HostProfileView.swift structure:
 * - Host identity (avatar, name, email, edit photo)
 * - Stats (listings, booked this month)
 * - Host tools (Listings, Bookings, Inbox, Earnings)
 * - Settings (Account settings, Personal information, Payment & payout)
 * - Switch to Guest Mode
 * - Sign Out
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HostProfileScreen(
    viewModel: HostProfileViewModel = hiltViewModel(),
    onListingsClick: () -> Unit = {},
    onBookingsClick: () -> Unit = {},
    onInboxClick: () -> Unit = {},
    onEarningsClick: () -> Unit = {},
    onAccountSettingsClick: () -> Unit = {},
    onPersonalInfoClick: () -> Unit = {},
    onPaymentPayoutClick: () -> Unit = {},
    onSwitchToGuestMode: () -> Unit = {},
    onEditProfileClick: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Host Profile",
                        style = PaceDreamTypography.Title1,
                        color = PaceDreamColors.TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    TextButton(onClick = onEditProfileClick) {
                        Text(
                            text = "Edit",
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
        PullToRefreshBox(
            isRefreshing = uiState.isLoading,
            onRefresh = { viewModel.refreshData() },
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 28.dp)
            ) {
                // Inline error
                uiState.error?.let { err ->
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = PaceDreamSpacing.MD, vertical = PaceDreamSpacing.XS)
                                .clip(RoundedCornerShape(12.dp))
                                .background(PaceDreamColors.Warning.copy(alpha = 0.12f))
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = PaceDreamIcons.Warning,
                                contentDescription = null,
                                tint = PaceDreamColors.Warning,
                                modifier = Modifier.size(PaceDreamIconSize.SM)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = err,
                                style = PaceDreamTypography.Subheadline,
                                color = PaceDreamColors.TextPrimary,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // Host Identity Section
                item {
                    HostIdentitySection(
                        avatarUrl = uiState.avatarUrl,
                        fullName = uiState.fullName,
                        email = uiState.email,
                        initials = uiState.initials,
                        onEditPhotoClick = onEditProfileClick
                    )
                }

                // Stats Section
                item {
                    ProfileSectionHeader(title = "Stats")
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = PaceDreamSpacing.MD),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.padding(top = 6.dp, bottom = 6.dp)
                    ) {
                        item {
                            ProfileKPIChip(
                                title = "Listings",
                                value = "${uiState.activeListingsCount}",
                                icon = PaceDreamIcons.Home
                            )
                        }
                        item {
                            ProfileKPIChip(
                                title = "Booked This Month",
                                value = "$${String.format("%.0f", uiState.monthlyEarnings)}",
                                icon = PaceDreamIcons.AttachMoney
                            )
                        }
                    }
                }

                // Host Tools Section
                item {
                    ProfileSectionHeader(title = "Host tools")
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = PaceDreamSpacing.MD),
                        shape = RoundedCornerShape(PaceDreamRadius.LG),
                        colors = CardDefaults.cardColors(containerColor = PaceDreamColors.Card),
                        elevation = CardDefaults.cardElevation(defaultElevation = PaceDreamElevation.XS)
                    ) {
                        Column {
                            ProfileToolRow(
                                icon = PaceDreamIcons.Home,
                                title = "Listings",
                                onClick = onListingsClick
                            )
                            ProfileDivider()
                            ProfileToolRow(
                                icon = PaceDreamIcons.CalendarToday,
                                title = "Bookings",
                                onClick = onBookingsClick
                            )
                            ProfileDivider()
                            ProfileToolRow(
                                icon = PaceDreamIcons.Mail,
                                title = "Inbox",
                                onClick = onInboxClick
                            )
                            ProfileDivider()
                            ProfileToolRow(
                                icon = PaceDreamIcons.AttachMoney,
                                title = "Earnings",
                                onClick = onEarningsClick
                            )
                        }
                    }
                }

                // Settings Section
                item {
                    ProfileSectionHeader(title = "Settings")
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = PaceDreamSpacing.MD),
                        shape = RoundedCornerShape(PaceDreamRadius.LG),
                        colors = CardDefaults.cardColors(containerColor = PaceDreamColors.Card),
                        elevation = CardDefaults.cardElevation(defaultElevation = PaceDreamElevation.XS)
                    ) {
                        Column {
                            ProfileToolRow(
                                icon = PaceDreamIcons.Settings,
                                title = "Account settings",
                                onClick = onAccountSettingsClick
                            )
                            ProfileDivider()
                            ProfileToolRow(
                                icon = PaceDreamIcons.Person,
                                title = "Personal information",
                                onClick = onPersonalInfoClick
                            )
                            ProfileDivider()
                            ProfileToolRow(
                                icon = PaceDreamIcons.CreditCard,
                                title = "Payment & payout",
                                onClick = onPaymentPayoutClick
                            )
                        }
                    }
                }

                // Switch to Guest Mode
                item {
                    Spacer(modifier = Modifier.height(PaceDreamSpacing.MD))
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = PaceDreamSpacing.MD),
                        shape = RoundedCornerShape(PaceDreamRadius.LG),
                        colors = CardDefaults.cardColors(containerColor = PaceDreamColors.Card),
                        elevation = CardDefaults.cardElevation(defaultElevation = PaceDreamElevation.XS)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(onClick = onSwitchToGuestMode)
                                .padding(horizontal = PaceDreamSpacing.MD, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = PaceDreamIcons.SwapHoriz,
                                contentDescription = null,
                                tint = PaceDreamColors.Primary,
                                modifier = Modifier.size(PaceDreamIconSize.SM)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Switch to Guest Mode",
                                style = PaceDreamTypography.Callout,
                                color = PaceDreamColors.Primary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                // Sign Out
                item {
                    Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = PaceDreamSpacing.MD),
                        shape = RoundedCornerShape(PaceDreamRadius.LG),
                        colors = CardDefaults.cardColors(containerColor = PaceDreamColors.Card),
                        elevation = CardDefaults.cardElevation(defaultElevation = PaceDreamElevation.XS)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(onClick = { viewModel.signOut(onSwitchToGuestMode) })
                                .padding(horizontal = PaceDreamSpacing.MD, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = PaceDreamIcons.ExitToApp,
                                contentDescription = null,
                                tint = PaceDreamColors.Error,
                                modifier = Modifier.size(PaceDreamIconSize.SM)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Sign Out",
                                style = PaceDreamTypography.Callout,
                                color = PaceDreamColors.Error,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}

// ── Host Identity Section ──────────────────────────────────────

@Composable
private fun HostIdentitySection(
    avatarUrl: String?,
    fullName: String,
    email: String,
    initials: String,
    onEditPhotoClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = PaceDreamSpacing.MD)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(CircleShape)
                .background(PaceDreamColors.Gray100),
            contentAlignment = Alignment.Center
        ) {
            if (!avatarUrl.isNullOrBlank()) {
                AsyncImage(
                    model = avatarUrl,
                    contentDescription = "Profile photo",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Text(
                    text = initials,
                    style = PaceDreamTypography.Headline.copy(fontSize = 18.sp),
                    color = PaceDreamColors.Primary,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = fullName.ifBlank { "Host" },
                style = PaceDreamTypography.Callout.copy(fontSize = 16.sp),
                color = PaceDreamColors.TextPrimary,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = email.ifBlank { "—" },
                style = PaceDreamTypography.Footnote,
                color = PaceDreamColors.TextSecondary,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        TextButton(onClick = onEditPhotoClick) {
            Text(
                text = "Edit photo",
                style = PaceDreamTypography.Footnote,
                color = PaceDreamColors.Primary,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

// ── Section Header ─────────────────────────────────────────────

@Composable
private fun ProfileSectionHeader(title: String) {
    Text(
        text = title,
        style = PaceDreamTypography.Caption,
        color = PaceDreamColors.TextTertiary,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(
            start = PaceDreamSpacing.MD + PaceDreamSpacing.XS,
            top = PaceDreamSpacing.MD,
            bottom = PaceDreamSpacing.SM
        )
    )
}

// ── Tool / Settings Row ────────────────────────────────────────

@Composable
private fun ProfileToolRow(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = PaceDreamSpacing.MD, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = PaceDreamColors.TextSecondary,
            modifier = Modifier.size(PaceDreamIconSize.SM)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = title,
            style = PaceDreamTypography.Callout,
            color = PaceDreamColors.TextPrimary,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )
        Icon(
            imageVector = PaceDreamIcons.ChevronRight,
            contentDescription = null,
            tint = PaceDreamColors.TextTertiary,
            modifier = Modifier.size(PaceDreamIconSize.XS)
        )
    }
}

// ── Divider ────────────────────────────────────────────────────

@Composable
private fun ProfileDivider() {
    HorizontalDivider(
        color = PaceDreamColors.Border,
        thickness = 0.5.dp,
        modifier = Modifier.padding(start = 48.dp)
    )
}

// ── KPI Chip ───────────────────────────────────────────────────

@Composable
private fun ProfileKPIChip(
    title: String,
    value: String,
    icon: ImageVector
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = PaceDreamColors.Card),
        elevation = CardDefaults.cardElevation(defaultElevation = PaceDreamElevation.XS),
        shape = RoundedCornerShape(PaceDreamRadius.LG)
    ) {
        Column(
            modifier = Modifier
                .width(160.dp)
                .padding(14.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = PaceDreamColors.Primary,
                modifier = Modifier.size(PaceDreamIconSize.SM)
            )
            Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))
            Text(
                text = value,
                style = PaceDreamTypography.Title2,
                color = PaceDreamColors.TextPrimary,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = title,
                style = PaceDreamTypography.Caption.copy(fontWeight = FontWeight.SemiBold),
                color = PaceDreamColors.TextSecondary
            )
        }
    }
}
