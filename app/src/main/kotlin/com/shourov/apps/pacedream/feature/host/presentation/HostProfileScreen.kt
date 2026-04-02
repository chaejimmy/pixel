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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.pacedream.common.composables.theme.*
import com.shourov.apps.pacedream.feature.host.presentation.components.*

/**
 * HostProfileScreen — iOS HostProfileView parity.
 *
 * Structure (matching iOS List sections):
 * 1. Host identity card (avatar, name, email, "Edit photo")
 * 2. Stats section (Listings count, Booked This Month)
 * 3. Host tools section (Listings, Bookings, Inbox, Earnings)
 * 4. Settings section (Account settings, Personal information, Earnings & payouts)
 * 5. Switch to Guest Mode
 * 6. Sign Out
 *
 * Uses HostAccent (PaceDreamColors.HostAccent = Success #10B981) for host actions.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HostProfileScreen(
    viewModel: HostProfileViewModel = hiltViewModel(),
    onEditProfileClick: () -> Unit = {},
    onEditPhotoClick: () -> Unit = {},
    onListingsClick: () -> Unit = {},
    onBookingsClick: () -> Unit = {},
    onInboxClick: () -> Unit = {},
    onEarningsClick: () -> Unit = {},
    onAccountSettingsClick: () -> Unit = {},
    onPersonalInfoClick: () -> Unit = {},
    onPaymentPayoutClick: () -> Unit = {},
    onSwitchToGuestMode: () -> Unit = {},
    onLoggedOut: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showLogoutDialog by remember { mutableStateOf(false) }

    // iOS parity: refresh when screen becomes visible (returning from listing creation etc.)
    val lifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current
    androidx.compose.runtime.DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                viewModel.refresh()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Profile",
                        style = PaceDreamTypography.Title1,
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    TextButton(onClick = onEditProfileClick) {
                        Text(
                            "Edit",
                            style = PaceDreamTypography.Callout.copy(fontWeight = FontWeight.SemiBold),
                            color = PaceDreamColors.HostAccent
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = PaceDreamColors.Background
                )
            )
        },
        containerColor = PaceDreamColors.Surface
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = uiState.isLoading,
            onRefresh = { viewModel.refresh() },
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    horizontal = PaceDreamSpacing.MD,
                    vertical = PaceDreamSpacing.SM
                ),
                verticalArrangement = Arrangement.spacedBy(PaceDreamSpacing.MD)
            ) {
                // ── Section 1: Host Identity Card ──────────────────────
                item {
                    HostIdentityCard(
                        userName = uiState.userName,
                        userEmail = uiState.userEmail,
                        userAvatar = uiState.userAvatar,
                        initials = uiState.initials,
                        onEditPhotoClick = onEditPhotoClick
                    )
                }

                // ── Section 2: Stats ───────────────────────────────────
                item {
                    HostSectionHeader(title = "Stats")
                }
                item {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            HostKpiChip(
                                title = "Bookings",
                                value = "${uiState.totalBookingsCount}",
                                icon = PaceDreamIcons.CalendarToday
                            )
                        }
                        item {
                            HostKpiChip(
                                title = "Pending",
                                value = "${uiState.pendingRequestsCount}",
                                icon = PaceDreamIcons.Schedule,
                                valueColor = if (uiState.pendingRequestsCount > 0) PaceDreamColors.Warning else null
                            )
                        }
                        item {
                            HostKpiChip(
                                title = "Active",
                                value = "${uiState.activeListingsCount}",
                                icon = PaceDreamIcons.Home
                            )
                        }
                    }
                }

                // ── Section 3: Host Tools ──────────────────────────────
                item {
                    HostSectionHeader(title = "Host tools")
                }
                item {
                    HostGroupedCard {
                        HostProfileRow(
                            icon = PaceDreamIcons.Home,
                            title = "Listings",
                            onClick = onListingsClick
                        )
                        HostRowDivider()
                        HostProfileRow(
                            icon = PaceDreamIcons.CalendarToday,
                            title = "Bookings",
                            onClick = onBookingsClick
                        )
                        HostRowDivider()
                        HostProfileRow(
                            icon = PaceDreamIcons.Mail,
                            title = "Inbox",
                            onClick = onInboxClick
                        )
                        HostRowDivider()
                        HostProfileRow(
                            icon = PaceDreamIcons.AttachMoney,
                            title = "Earnings",
                            onClick = onEarningsClick
                        )
                    }
                }

                // ── Section 4: Settings ────────────────────────────────
                item {
                    HostSectionHeader(title = "Settings")
                }
                item {
                    HostGroupedCard {
                        HostProfileRow(
                            icon = PaceDreamIcons.Settings,
                            title = "Account settings",
                            iconTint = PaceDreamColors.TextSecondary,
                            onClick = onAccountSettingsClick
                        )
                        HostRowDivider()
                        HostProfileRow(
                            icon = PaceDreamIcons.Person,
                            title = "Personal information",
                            iconTint = PaceDreamColors.TextSecondary,
                            onClick = onPersonalInfoClick
                        )
                        HostRowDivider()
                        HostProfileRow(
                            icon = PaceDreamIcons.CreditCard,
                            title = "Earnings & payouts",
                            iconTint = PaceDreamColors.TextSecondary,
                            onClick = onPaymentPayoutClick
                        )
                    }
                }

                // ── Section 5: Switch to Guest Mode ────────────────────
                item {
                    HostSwitchModeRow(
                        onClick = {
                            viewModel.switchToGuestMode()
                            onSwitchToGuestMode()
                        }
                    )
                }

                // ── Section 6: Sign Out ────────────────────────────────
                item {
                    HostSignOutRow(onClick = { showLogoutDialog = true })
                }

                // Bottom spacer
                item {
                    Spacer(modifier = Modifier.height(PaceDreamSpacing.LG))
                }
            }
        }
    }

    if (showLogoutDialog) {
        HostSignOutDialog(
            onConfirm = {
                showLogoutDialog = false
                viewModel.logout()
                onLoggedOut()
            },
            onDismiss = { showLogoutDialog = false }
        )
    }
}

// ── Host Identity Card ──────────────────────────────────────────

@Composable
private fun HostIdentityCard(
    userName: String,
    userEmail: String?,
    userAvatar: String?,
    initials: String,
    onEditPhotoClick: () -> Unit
) {
    HostGroupedCard {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = PaceDreamSpacing.MD, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Avatar circle (iOS: 60x60)
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(CircleShape)
                    .background(PaceDreamColors.Gray100),
                contentAlignment = Alignment.Center
            ) {
                if (userAvatar?.takeIf { it.isNotBlank() } != null) {
                    AsyncImage(
                        model = userAvatar,
                        contentDescription = userName,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                    )
                } else {
                    Text(
                        text = initials,
                        style = PaceDreamTypography.Title3.copy(
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = PaceDreamColors.HostAccent
                    )
                }
            }

            // Name + email
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = userName.ifEmpty { "Host" },
                    style = PaceDreamTypography.Callout.copy(fontWeight = FontWeight.Bold),
                    color = PaceDreamColors.TextPrimary
                )
                userEmail?.let { email ->
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = email,
                        style = PaceDreamTypography.Caption.copy(
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        color = PaceDreamColors.TextSecondary,
                        maxLines = 1
                    )
                }
            }

            // "Edit photo"
            Text(
                text = "Edit photo",
                style = PaceDreamTypography.Caption.copy(
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                ),
                color = PaceDreamColors.HostAccent,
                modifier = Modifier.clickable(onClick = onEditPhotoClick)
            )
        }
    }
}

private fun formatHostCurrency(amount: Double): String {
    return if (amount == 0.0) "$0" else "$${String.format("%,.0f", amount)}"
}
