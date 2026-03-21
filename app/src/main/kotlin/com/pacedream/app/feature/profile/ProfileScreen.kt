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
 * ProfileScreen - User profile with Guest/Host mode toggle
 *
 * iOS Parity:
 * - Guest mode: show profile, bookings, favorites, settings
 * - Host mode: switch to host dashboard
 * - Persisted mode preference (SharedPreferences)
 * - Logout confirmation dialog
 * - Verified badge when identity is verified
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
    onFavoritesClick: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showLogoutDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Profile",
                        style = PaceDreamTypography.Title2
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = PaceDreamColors.Background
                )
            )
        },
        containerColor = PaceDreamColors.Background
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(PaceDreamSpacing.MD),
            verticalArrangement = Arrangement.spacedBy(PaceDreamSpacing.MD)
        ) {
            if (!uiState.isLoggedIn) {
                item {
                    LoggedOutSection(onLoginClick = onLoginClick)
                }
            } else {
                item {
                    UserProfileHeader(
                        userName = uiState.userName,
                        userEmail = uiState.userEmail,
                        userAvatar = uiState.userAvatar,
                        onEditClick = onEditProfileClick
                    )
                }

                // Quick action chips (iOS parity: horizontal scrollable chips)
                item {
                    QuickActionsRow(
                        onEditProfile = onEditProfileClick,
                        onBookings = onBookingsClick,
                        onWishlist = onFavoritesClick,
                        onHostMode = onHostModeClick,
                        onMyLists = onMyListsClick
                    )
                }

                // Create listing CTA (iOS parity: gradient button)
                item {
                    CreateListingCTA(onClick = onHostModeClick)
                }

                // Host mode CTA (iOS parity: gradient button)
                item {
                    HostModeCTA(onClick = onHostModeClick)
                }

                // Verification section
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(PaceDreamRadius.LG),
                        colors = CardDefaults.cardColors(containerColor = PaceDreamColors.Card),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        ProfileMenuItem(
                            icon = PaceDreamIcons.VerifiedUser,
                            title = "Identity Verification",
                            subtitle = "Verify your identity for a trusted experience",
                            onClick = onIdentityVerificationClick
                        )
                    }
                }

                // Settings and support section
                item {
                    ProfileMenuSection(
                        onSettingsClick = onSettingsClick,
                        onHelpClick = onHelpClick,
                        onAboutClick = onAboutClick,
                        onLogoutClick = { showLogoutDialog = true }
                    )
                }
            }
        }
    }

    // Logout confirmation dialog
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = {
                Text(
                    "Sign Out",
                    style = PaceDreamTypography.Title3
                )
            },
            text = {
                Text(
                    "Are you sure you want to sign out of your account?",
                    style = PaceDreamTypography.Body,
                    color = PaceDreamColors.TextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutDialog = false
                        viewModel.logout()
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
        // Lock shield icon (iOS parity)
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
            text = "Access your account settings, wishlist, and host mode.",
            style = PaceDreamTypography.Body,
            color = PaceDreamColors.TextSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 40.dp)
        )

        Spacer(modifier = Modifier.height(PaceDreamSpacing.LG))

        // Primary button: Sign in / Create account (iOS parity)
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

        // Secondary button: Create a listing (iOS parity)
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
                "Create a listing",
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
    onEditClick: () -> Unit,
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
            )
            .clickable(onClick = onEditClick),
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
                // Avatar with white stroke (iOS parity)
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
                        Icon(
                            imageVector = PaceDreamIcons.Person,
                            contentDescription = userName,
                            modifier = Modifier.size(36.dp),
                            tint = PaceDreamColors.TextSecondary
                        )
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = userName,
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

            // Stats pills (iOS parity)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatPill(value = "$bookingsCount", label = "Bookings")
                StatPill(value = "$wishlistCount", label = "Wishlist")
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
        color = PaceDreamColors.Primary.copy(alpha = 0.08f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = value,
                style = PaceDreamTypography.Caption.copy(fontWeight = FontWeight.Bold),
                color = PaceDreamColors.Primary
            )
            Text(
                text = label,
                style = PaceDreamTypography.Caption,
                color = PaceDreamColors.TextSecondary
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Quick Actions Row (iOS parity: horizontal scrollable action chips)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun QuickActionsRow(
    onEditProfile: () -> Unit,
    onBookings: () -> Unit,
    onWishlist: () -> Unit,
    onHostMode: () -> Unit,
    onMyLists: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        ActionChip(title = "Edit profile", icon = PaceDreamIcons.Edit, onClick = onEditProfile)
        ActionChip(title = "Bookings", icon = PaceDreamIcons.DateRange, onClick = onBookings)
        ActionChip(title = "Wishlist", icon = PaceDreamIcons.FavoriteBorder, onClick = onWishlist)
        ActionChip(title = "Host Mode", icon = PaceDreamIcons.Group, onClick = onHostMode)
        ActionChip(title = "My Lists", icon = PaceDreamIcons.ListIcon, onClick = onMyLists)
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
private fun CreateListingCTA(onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        color = Color.Transparent,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            PaceDreamColors.Primary,
                            PaceDreamColors.Primary.copy(alpha = 0.78f)
                        )
                    ),
                    RoundedCornerShape(20.dp)
                )
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = PaceDreamIcons.AddCircle,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Create a listing",
                    style = PaceDreamTypography.Headline.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = Color.White
                )
                Text(
                    text = "Start hosting in minutes",
                    style = PaceDreamTypography.Caption.copy(fontWeight = FontWeight.SemiBold),
                    color = Color.White.copy(alpha = 0.9f)
                )
            }
            Icon(
                imageVector = PaceDreamIcons.ArrowForward,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.95f),
                modifier = Modifier.size(13.dp)
            )
        }
    }
}

@Composable
private fun HostModeCTA(onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        color = Color.Transparent,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            PaceDreamColors.Primary,
                            PaceDreamColors.Primary.copy(alpha = 0.80f)
                        )
                    ),
                    RoundedCornerShape(20.dp)
                )
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = PaceDreamIcons.Home,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Switch to Host Mode",
                    style = PaceDreamTypography.Headline.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = Color.White
                )
                Text(
                    text = "Manage listings and bookings",
                    style = PaceDreamTypography.Caption.copy(fontWeight = FontWeight.SemiBold),
                    color = Color.White.copy(alpha = 0.9f)
                )
            }
            Icon(
                imageVector = PaceDreamIcons.ArrowForward,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.95f),
                modifier = Modifier.size(13.dp)
            )
        }
    }
}

@Composable
private fun ProfileMenuSection(
    onSettingsClick: () -> Unit,
    onHelpClick: () -> Unit,
    onAboutClick: () -> Unit,
    onLogoutClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(PaceDreamRadius.LG),
        colors = CardDefaults.cardColors(containerColor = PaceDreamColors.Card),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column {
            ProfileMenuItem(
                icon = PaceDreamIcons.Settings,
                title = "Settings",
                subtitle = "Account preferences and privacy",
                onClick = onSettingsClick
            )

            HorizontalDivider(
                color = PaceDreamColors.Border,
                modifier = Modifier.padding(horizontal = PaceDreamSpacing.MD)
            )

            ProfileMenuItem(
                icon = PaceDreamIcons.Help,
                title = "Help & Support",
                subtitle = "Get help with your account",
                onClick = onHelpClick
            )

            HorizontalDivider(
                color = PaceDreamColors.Border,
                modifier = Modifier.padding(horizontal = PaceDreamSpacing.MD)
            )

            ProfileMenuItem(
                icon = PaceDreamIcons.Info,
                title = "About",
                subtitle = "App information and legal",
                onClick = onAboutClick
            )

            HorizontalDivider(
                color = PaceDreamColors.Border,
                modifier = Modifier.padding(horizontal = PaceDreamSpacing.MD)
            )

            ProfileMenuItem(
                icon = PaceDreamIcons.ExitToApp,
                title = "Sign Out",
                onClick = onLogoutClick,
                tint = PaceDreamColors.Error
            )
        }
    }
}

@Composable
private fun ProfileMenuItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit,
    tint: androidx.compose.ui.graphics.Color = PaceDreamColors.TextPrimary
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
            tint = if (tint == PaceDreamColors.Error) tint else PaceDreamColors.Primary,
            modifier = Modifier.size(24.dp)
        )

        Spacer(modifier = Modifier.width(PaceDreamSpacing.MD))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = PaceDreamTypography.Callout,
                color = tint
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = PaceDreamTypography.Caption,
                    color = PaceDreamColors.TextSecondary
                )
            }
        }

        Icon(
            imageVector = PaceDreamIcons.ChevronRight,
            contentDescription = null,
            tint = PaceDreamColors.TextTertiary,
            modifier = Modifier.size(18.dp)
        )
    }
}
