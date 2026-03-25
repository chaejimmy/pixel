package com.pacedream.app.feature.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import com.pacedream.common.icon.PaceDreamIcons
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.pacedream.common.composables.theme.PaceDreamColors
import com.pacedream.common.composables.theme.PaceDreamRadius
import com.pacedream.common.composables.theme.PaceDreamSpacing
import com.pacedream.common.composables.theme.PaceDreamTypography

/**
 * ProfileScreen - iOS parity
 *
 * Structure matches iOS ProfileView.swift:
 * 1. Profile card (avatar, name, email, stat pills, identity)
 * 2. Quick action chips (Edit profile, Bookings, Favorites, Host Mode)
 * 3. Create listing CTA
 * 4. Switch to Host Mode CTA
 * 5. Settings shortcuts section (Account settings, Notifications & preferences)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel = hiltViewModel(),
    onLoginClick: () -> Unit,
    onHostModeClick: () -> Unit,
    onEditProfileClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onIdentityVerificationClick: () -> Unit = {},
    onHelpClick: () -> Unit = {},
    onAboutClick: () -> Unit = {},
    onMyListsClick: () -> Unit = {},
    onBookingsClick: () -> Unit = {},
    onFavoritesClick: () -> Unit = {},
    onCreateListingClick: () -> Unit = onHostModeClick,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

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
                    // Settings gear button (iOS parity: gearshape in upper-right)
                    if (uiState.isLoggedIn) {
                        IconButton(onClick = onSettingsClick) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(
                                        PaceDreamColors.Card.copy(alpha = 0.9f),
                                        CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = PaceDreamIcons.Settings,
                                    contentDescription = "Settings",
                                    modifier = Modifier.size(16.dp),
                                    tint = PaceDreamColors.TextPrimary
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        containerColor = Color.Transparent
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            PaceDreamColors.Primary.copy(alpha = 0.06f),
                            PaceDreamColors.Primary.copy(alpha = 0.03f),
                            PaceDreamColors.Background
                        )
                    )
                )
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(PaceDreamSpacing.MD),
                verticalArrangement = Arrangement.spacedBy(PaceDreamSpacing.MD)
            ) {
                if (!uiState.isLoggedIn) {
                    item {
                        LoggedOutSection(
                            onLoginClick = onLoginClick,
                            onCreateListingClick = onCreateListingClick,
                        )
                    }
                } else {
                    // 1. Profile card with stats and identity (iOS parity)
                    item {
                        UserProfileHeader(
                            userName = uiState.userName,
                            userEmail = uiState.userEmail,
                            userAvatar = uiState.userAvatar,
                            bookingsCount = uiState.bookingsCount,
                            wishlistCount = uiState.wishlistCount,
                            identityStatus = uiState.identityStatus
                        )
                    }

                    // 2. Quick action chips
                    item {
                        QuickActionsRow(
                            onEditProfile = onEditProfileClick,
                            onBookings = onBookingsClick,
                            onWishlist = onFavoritesClick,
                            onHostMode = onHostModeClick
                        )
                    }

                    // 3. Host mode entry point (single, compact)
                    item {
                        HostModeRow(onClick = onHostModeClick)
                    }

                    // 4. Settings shortcuts section
                    item {
                        SettingsShortcutsSection(onSettingsClick = onSettingsClick)
                    }
                }
            }

            // Loading indicator overlay (iOS parity)
            if (uiState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = padding.calculateTopPadding() + 8.dp)
                        .size(32.dp),
                    color = PaceDreamColors.Primary,
                    strokeWidth = 3.dp
                )
            }
        }
    }
}

@Composable
private fun LoggedOutSection(
    onLoginClick: () -> Unit,
    onCreateListingClick: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = PaceDreamSpacing.XL),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = PaceDreamIcons.Shield,
            contentDescription = "Profile",
            modifier = Modifier.size(64.dp),
            tint = PaceDreamColors.Primary
        )

        Spacer(modifier = Modifier.height(PaceDreamSpacing.MD))

        Text(
            text = "Sign in to manage your profile",
            style = PaceDreamTypography.Title3.copy(fontWeight = FontWeight.Bold),
            color = PaceDreamColors.TextPrimary
        )

        Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))

        Text(
            text = "Access your account settings, favorites, and host mode.",
            style = PaceDreamTypography.Body,
            color = PaceDreamColors.TextSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 40.dp)
        )

        Spacer(modifier = Modifier.height(PaceDreamSpacing.LG))

        Button(
            onClick = onLoginClick,
            modifier = Modifier
                .widthIn(max = 320.dp)
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = PaceDreamColors.Primary
            ),
            contentPadding = PaddingValues(vertical = 14.dp)
        ) {
            Text(
                "Sign in / Create account",
                style = PaceDreamTypography.Button.copy(fontWeight = FontWeight.Bold)
            )
        }

        Spacer(modifier = Modifier.height(PaceDreamSpacing.SM2))

        OutlinedButton(
            onClick = onCreateListingClick,
            modifier = Modifier
                .widthIn(max = 320.dp)
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                PaceDreamColors.Primary.copy(alpha = 0.30f)
            ),
            contentPadding = PaddingValues(vertical = 14.dp)
        ) {
            Text(
                "Create listing",
                style = PaceDreamTypography.Button.copy(
                    fontWeight = FontWeight.Bold,
                    color = PaceDreamColors.Primary
                )
            )
        }
    }
}

@Composable
private fun UserProfileHeader(
    userName: String,
    userEmail: String?,
    userAvatar: String?,
    bookingsCount: Int = 0,
    wishlistCount: Int = 0,
    identityStatus: String? = null
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(20.dp),
                ambientColor = Color.Black.copy(alpha = 0.06f)
            ),
        shape = RoundedCornerShape(20.dp),
        color = PaceDreamColors.Card
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Avatar + Name row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Avatar with white stroke (iOS parity: 72x72)
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .shadow(4.dp, CircleShape)
                        .border(2.dp, Color.White, CircleShape)
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
                        val initials = userName.split(" ")
                            .take(2)
                            .mapNotNull { it.firstOrNull()?.uppercase() }
                            .joinToString("")
                        Text(
                            text = initials.ifEmpty { "U" },
                            style = PaceDreamTypography.Title3.copy(
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = PaceDreamColors.TextPrimary
                        )
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = userName.ifEmpty { "Your account" },
                        style = PaceDreamTypography.Title3.copy(
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = PaceDreamColors.TextPrimary
                    )

                    userEmail?.let { email ->
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = email,
                            style = PaceDreamTypography.Caption,
                            color = PaceDreamColors.TextSecondary,
                            maxLines = 1
                        )
                    }
                }
            }

            // Stats pills (iOS parity: bookings + wishlist count in capsule)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatPill(value = "$bookingsCount", label = "Bookings")
                StatPill(value = "$wishlistCount", label = "Favorites")
            }

            // Identity badge (iOS parity)
            if (!identityStatus.isNullOrEmpty()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = PaceDreamIcons.VerifiedUser,
                        contentDescription = null,
                        tint = PaceDreamColors.Success,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Identity: $identityStatus",
                        style = PaceDreamTypography.Caption.copy(
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = PaceDreamColors.TextSecondary
                    )
                }
            }
        }
    }
}

@Composable
private fun StatPill(value: String, label: String) {
    Surface(
        shape = RoundedCornerShape(PaceDreamRadius.Round),
        color = Color.Black.copy(alpha = 0.04f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = value,
                style = PaceDreamTypography.Subheadline.copy(
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                ),
                color = PaceDreamColors.TextPrimary
            )
            Text(
                text = label,
                style = PaceDreamTypography.Caption.copy(fontSize = 13.sp),
                color = PaceDreamColors.TextSecondary
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Quick Actions Row (iOS parity: Edit profile, Bookings, Favorites, Host Mode)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun QuickActionsRow(
    onEditProfile: () -> Unit,
    onBookings: () -> Unit,
    onWishlist: () -> Unit,
    onHostMode: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        ActionChip(title = "Edit profile", icon = PaceDreamIcons.Edit, onClick = onEditProfile)
        ActionChip(title = "Bookings", icon = PaceDreamIcons.DateRange, onClick = onBookings)
        ActionChip(title = "Favorites", icon = PaceDreamIcons.FavoriteBorder, onClick = onWishlist)
        ActionChip(title = "Host Mode", icon = PaceDreamIcons.Group, onClick = onHostMode)
    }
}

@Composable
private fun ActionChip(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(PaceDreamRadius.Round),
        color = PaceDreamColors.Card,
        border = androidx.compose.foundation.BorderStroke(
            0.5.dp,
            PaceDreamColors.Gray200
        ),
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = PaceDreamColors.Primary,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = title,
                style = PaceDreamTypography.Caption.copy(fontWeight = FontWeight.SemiBold),
                color = PaceDreamColors.TextPrimary,
                maxLines = 1
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// CTA Buttons (iOS parity: gradient background with arrow)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun HostModeRow(onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = PaceDreamColors.Card),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        PaceDreamColors.Primary.copy(alpha = 0.10f),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = PaceDreamIcons.Home,
                    contentDescription = null,
                    tint = PaceDreamColors.Primary,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Switch to Host Mode",
                    style = PaceDreamTypography.Callout.copy(fontWeight = FontWeight.SemiBold),
                    color = PaceDreamColors.TextPrimary
                )
                Text(
                    text = "Manage listings, bookings, and earnings",
                    style = PaceDreamTypography.Caption,
                    color = PaceDreamColors.TextSecondary
                )
            }

            Icon(
                imageVector = PaceDreamIcons.ChevronRight,
                contentDescription = null,
                tint = PaceDreamColors.TextTertiary,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Settings Shortcuts Section (iOS parity: "Settings" header + 2 rows in card)
// Matches iOS settingsShortcuts: Account settings + Notifications & preferences
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SettingsShortcutsSection(onSettingsClick: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = "Settings",
            style = PaceDreamTypography.Title3.copy(fontWeight = FontWeight.Bold),
            color = PaceDreamColors.TextPrimary
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = PaceDreamColors.Card),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column {
                SettingsShortcutRow(
                    icon = PaceDreamIcons.Settings,
                    title = "Account settings",
                    onClick = onSettingsClick
                )

                HorizontalDivider(
                    color = PaceDreamColors.Border,
                    modifier = Modifier.padding(start = 52.dp)
                )

                SettingsShortcutRow(
                    icon = PaceDreamIcons.Notifications,
                    title = "Notifications & preferences",
                    onClick = onSettingsClick
                )
            }
        }
    }
}

@Composable
private fun SettingsShortcutRow(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = PaceDreamColors.Primary,
            modifier = Modifier.size(20.dp)
        )

        Spacer(modifier = Modifier.width(12.dp))

        Text(
            text = title,
            style = PaceDreamTypography.Callout.copy(fontWeight = FontWeight.SemiBold),
            color = PaceDreamColors.TextPrimary,
            modifier = Modifier.weight(1f)
        )

        Icon(
            imageVector = PaceDreamIcons.ChevronRight,
            contentDescription = null,
            tint = PaceDreamColors.TextTertiary,
            modifier = Modifier.size(14.dp)
        )
    }
}
