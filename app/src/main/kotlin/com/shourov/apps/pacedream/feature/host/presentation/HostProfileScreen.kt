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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.pacedream.common.composables.theme.*

/**
 * HostProfileScreen — iOS HostProfileView parity.
 *
 * Structure (matching iOS List sections):
 * 1. Host identity card (avatar, name, email, "Edit photo")
 * 2. Stats section (Listings count, Booked This Month)
 * 3. Host tools section (Listings, Bookings, Inbox, Earnings)
 * 4. Settings section (Account settings, Personal information, Payment & payout)
 * 5. Switch to Guest Mode
 * 6. Sign Out
 *
 * Uses green host accent (PaceDreamColors.Success) instead of purple primary.
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

    // Host accent color — iOS uses PaceDreamDesignSystem.Colors.primary which renders green in host context
    val hostAccent = PaceDreamColors.Success

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Host Profile",
                        style = PaceDreamTypography.Title1,
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    TextButton(onClick = onEditProfileClick) {
                        Text(
                            "Edit",
                            style = PaceDreamTypography.Callout.copy(
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = hostAccent
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
                        hostAccent = hostAccent,
                        onEditPhotoClick = onEditPhotoClick
                    )
                }

                // ── Section 2: Stats ───────────────────────────────────
                item {
                    SectionHeader(title = "Stats")
                }
                item {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            HostKPIChip(
                                title = "Listings",
                                value = "${uiState.activeListingsCount}",
                                icon = PaceDreamIcons.Home,
                                accent = hostAccent
                            )
                        }
                        item {
                            HostKPIChip(
                                title = "Booked This Month",
                                value = formatHostCurrency(uiState.monthlyEarnings),
                                icon = PaceDreamIcons.AttachMoney,
                                accent = hostAccent
                            )
                        }
                    }
                }

                // ── Section 3: Host Tools ──────────────────────────────
                item {
                    SectionHeader(title = "Host tools")
                }
                item {
                    HostToolsCard(
                        hostAccent = hostAccent,
                        onListingsClick = onListingsClick,
                        onBookingsClick = onBookingsClick,
                        onInboxClick = onInboxClick,
                        onEarningsClick = onEarningsClick
                    )
                }

                // ── Section 4: Settings ────────────────────────────────
                item {
                    SectionHeader(title = "Settings")
                }
                item {
                    SettingsCard(
                        hostAccent = hostAccent,
                        onAccountSettingsClick = onAccountSettingsClick,
                        onPersonalInfoClick = onPersonalInfoClick,
                        onPaymentPayoutClick = onPaymentPayoutClick
                    )
                }

                // ── Section 5: Switch to Guest Mode ────────────────────
                item {
                    SwitchModeRow(
                        hostAccent = hostAccent,
                        onClick = {
                            viewModel.switchToGuestMode()
                            onSwitchToGuestMode()
                        }
                    )
                }

                // ── Section 6: Sign Out ────────────────────────────────
                item {
                    SignOutRow(
                        onClick = { showLogoutDialog = true }
                    )
                }

                // Bottom spacer
                item {
                    Spacer(modifier = Modifier.height(PaceDreamSpacing.LG))
                }
            }
        }
    }

    // Logout confirmation dialog (iOS parity: "Sign out?" / "You can sign back in anytime.")
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = {
                Text(
                    "Sign out?",
                    style = PaceDreamTypography.Title3
                )
            },
            text = {
                Text(
                    "You can sign back in anytime.",
                    style = PaceDreamTypography.Body,
                    color = PaceDreamColors.TextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutDialog = false
                        viewModel.logout()
                        onLoggedOut()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PaceDreamColors.Error
                    ),
                    shape = RoundedCornerShape(PaceDreamRadius.MD)
                ) {
                    Text("Sign Out", style = PaceDreamTypography.Button)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showLogoutDialog = false }
                ) {
                    Text(
                        "Cancel",
                        color = PaceDreamColors.TextPrimary
                    )
                }
            },
            shape = RoundedCornerShape(PaceDreamRadius.LG),
            containerColor = PaceDreamColors.Card
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Host Identity Card (iOS parity: hostIdentity)
// Avatar (60dp circle) + Name + Email + "Edit photo"
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun HostIdentityCard(
    userName: String,
    userEmail: String?,
    userAvatar: String?,
    initials: String,
    hostAccent: Color,
    onEditPhotoClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(PaceDreamRadius.LG),
        colors = CardDefaults.cardColors(containerColor = PaceDreamColors.Card),
        elevation = CardDefaults.cardElevation(defaultElevation = PaceDreamElevation.XS)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = PaceDreamSpacing.MD, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Avatar circle (iOS: 60×60)
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(CircleShape)
                    .background(PaceDreamColors.Gray100),
                contentAlignment = Alignment.Center
            ) {
                if (userAvatar != null) {
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
                        color = hostAccent
                    )
                }
            }

            // Name + email
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = userName.ifEmpty { "Host" },
                    style = PaceDreamTypography.Callout.copy(
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    ),
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

            // "Edit photo" (iOS parity: PhotosPicker text)
            Text(
                text = "Edit photo",
                style = PaceDreamTypography.Caption.copy(
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                ),
                color = hostAccent,
                modifier = Modifier.clickable(onClick = onEditPhotoClick)
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// KPI Chip (iOS parity: KPIChip — icon top-left, value bold 22sp, title 12sp)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun HostKPIChip(
    title: String,
    value: String,
    icon: ImageVector,
    accent: Color
) {
    Card(
        modifier = Modifier.width(160.dp),
        shape = RoundedCornerShape(PaceDreamRadius.LG),
        colors = CardDefaults.cardColors(containerColor = PaceDreamColors.Card),
        elevation = CardDefaults.cardElevation(defaultElevation = PaceDreamElevation.SM)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(PaceDreamIconSize.MD)
            )
            Text(
                text = value,
                style = PaceDreamTypography.Title2.copy(
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = PaceDreamColors.TextPrimary
            )
            Text(
                text = title,
                style = PaceDreamTypography.Caption.copy(
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                ),
                color = PaceDreamColors.TextSecondary
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Host Tools Card (iOS parity: Listings / Bookings / Inbox / Earnings)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun HostToolsCard(
    hostAccent: Color,
    onListingsClick: () -> Unit,
    onBookingsClick: () -> Unit,
    onInboxClick: () -> Unit,
    onEarningsClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(PaceDreamRadius.LG),
        colors = CardDefaults.cardColors(containerColor = PaceDreamColors.Card),
        elevation = CardDefaults.cardElevation(defaultElevation = PaceDreamElevation.XS)
    ) {
        Column {
            HostProfileRow(
                icon = PaceDreamIcons.Home,
                title = "Listings",
                iconTint = hostAccent,
                onClick = onListingsClick
            )
            RowDivider()
            HostProfileRow(
                icon = PaceDreamIcons.CalendarToday,
                title = "Bookings",
                iconTint = hostAccent,
                onClick = onBookingsClick
            )
            RowDivider()
            HostProfileRow(
                icon = PaceDreamIcons.Mail,
                title = "Inbox",
                iconTint = hostAccent,
                onClick = onInboxClick
            )
            RowDivider()
            HostProfileRow(
                icon = PaceDreamIcons.AttachMoney,
                title = "Earnings",
                iconTint = hostAccent,
                onClick = onEarningsClick
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Settings Card (iOS parity: Account settings / Personal info / Payment & payout)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SettingsCard(
    hostAccent: Color,
    onAccountSettingsClick: () -> Unit,
    onPersonalInfoClick: () -> Unit,
    onPaymentPayoutClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(PaceDreamRadius.LG),
        colors = CardDefaults.cardColors(containerColor = PaceDreamColors.Card),
        elevation = CardDefaults.cardElevation(defaultElevation = PaceDreamElevation.XS)
    ) {
        Column {
            HostProfileRow(
                icon = PaceDreamIcons.Settings,
                title = "Account settings",
                iconTint = PaceDreamColors.TextSecondary,
                onClick = onAccountSettingsClick
            )
            RowDivider()
            HostProfileRow(
                icon = PaceDreamIcons.Person,
                title = "Personal information",
                iconTint = PaceDreamColors.TextSecondary,
                onClick = onPersonalInfoClick
            )
            RowDivider()
            HostProfileRow(
                icon = PaceDreamIcons.CreditCard,
                title = "Payment & payout",
                iconTint = PaceDreamColors.TextSecondary,
                onClick = onPaymentPayoutClick
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Switch to Guest Mode (iOS parity: green-accent row)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SwitchModeRow(
    hostAccent: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(PaceDreamRadius.LG),
        colors = CardDefaults.cardColors(containerColor = PaceDreamColors.Card),
        elevation = CardDefaults.cardElevation(defaultElevation = PaceDreamElevation.XS)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = PaceDreamSpacing.MD, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = PaceDreamIcons.SwapHoriz,
                contentDescription = null,
                tint = hostAccent,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(PaceDreamSpacing.MD))
            Text(
                text = "Switch to Guest Mode",
                style = PaceDreamTypography.Callout,
                color = hostAccent,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = PaceDreamIcons.ChevronRight,
                contentDescription = null,
                tint = PaceDreamColors.TextTertiary,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Sign Out (iOS parity: destructive red row)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SignOutRow(onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(PaceDreamRadius.LG),
        colors = CardDefaults.cardColors(containerColor = PaceDreamColors.Card),
        elevation = CardDefaults.cardElevation(defaultElevation = PaceDreamElevation.XS)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = PaceDreamSpacing.MD, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = PaceDreamIcons.ExitToApp,
                contentDescription = null,
                tint = PaceDreamColors.Error,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(PaceDreamSpacing.MD))
            Text(
                text = "Sign Out",
                style = PaceDreamTypography.Callout,
                color = PaceDreamColors.Error,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Shared components
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = PaceDreamTypography.Subheadline.copy(
            fontWeight = FontWeight.SemiBold
        ),
        color = PaceDreamColors.TextSecondary,
        modifier = Modifier.padding(top = PaceDreamSpacing.SM, bottom = PaceDreamSpacing.XS)
    )
}

@Composable
private fun HostProfileRow(
    icon: ImageVector,
    title: String,
    iconTint: Color,
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
            contentDescription = title,
            tint = iconTint,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(PaceDreamSpacing.MD))
        Text(
            text = title,
            style = PaceDreamTypography.Callout,
            color = PaceDreamColors.TextPrimary,
            modifier = Modifier.weight(1f)
        )
        Icon(
            imageVector = PaceDreamIcons.ChevronRight,
            contentDescription = null,
            tint = PaceDreamColors.TextTertiary,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun RowDivider() {
    HorizontalDivider(
        color = PaceDreamColors.Border,
        modifier = Modifier.padding(start = 56.dp)
    )
}

private fun formatHostCurrency(amount: Double): String {
    return if (amount == 0.0) "$0" else "$${String.format("%,.0f", amount)}"
}
