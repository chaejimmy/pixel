package com.shourov.apps.pacedream.feature.profile.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import com.pacedream.common.icon.PaceDreamIcons
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.pacedream.common.composables.theme.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileTabScreen(
    onShowAuthSheet: () -> Unit,
    onEditProfileClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onHelpClick: () -> Unit = {},
    onFaqClick: () -> Unit = {},
    onAboutClick: () -> Unit = {},
    onPrivacyPolicyClick: () -> Unit = {},
    onTermsOfServiceClick: () -> Unit = {},
    onLogoutClick: () -> Unit = {},
    onSwitchToHostMode: () -> Unit = {},
    onSwitchToGuestMode: () -> Unit = {},
    onCreateListingClick: () -> Unit = {},
    isHostMode: Boolean = false,
    onReviewsClick: () -> Unit = {},
    onTripPlannerClick: () -> Unit = {},
    onBidsClick: () -> Unit = {},
    onDestinationsClick: () -> Unit = {},
    onBookingsClick: () -> Unit = {},
    onWishlistClick: () -> Unit = {},
    onNotificationsClick: () -> Unit = {}
) {
    val viewModel: ProfileTabViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val bookingsCount by viewModel.bookingsCount.collectAsStateWithLifecycle()
    val wishlistCount by viewModel.wishlistCount.collectAsStateWithLifecycle()
    val verificationStatus by viewModel.verificationStatus.collectAsStateWithLifecycle()
    val memberSince by viewModel.memberSince.collectAsStateWithLifecycle()

    when (val state = uiState) {
        is ProfileTabUiState.Locked -> {
            RequiresAuthState(onSignIn = onShowAuthSheet)
        }
        is ProfileTabUiState.Loading -> {
            PullToRefreshBox(
                isRefreshing = state.refreshing,
                onRefresh = { viewModel.refresh() },
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(PaceDreamColors.Background),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = PaceDreamColors.Primary)
                }
            }
        }
        is ProfileTabUiState.Authenticated -> {
            PullToRefreshBox(
                isRefreshing = state.refreshing,
                onRefresh = { viewModel.refresh() },
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(PaceDreamColors.Background),
                    contentPadding = PaddingValues(bottom = PaceDreamSpacing.XXXL)
                ) {
                    // Profile Header — iOS: HStack with avatar + name/email side-by-side
                    item {
                        ProfileHeader(
                            name = state.user.displayName,
                            email = state.user.email ?: "",
                            memberSince = memberSince,
                            onEditClick = onEditProfileClick,
                            verificationStatus = verificationStatus
                        )
                    }

                    // Primary actions - elevated, not buried in lists
                    item {
                        Spacer(modifier = Modifier.height(PaceDreamSpacing.MD))
                        PrimaryActionsRow(
                            onBookings = onBookingsClick,
                            onWishlist = onWishlistClick,
                            onBids = onBidsClick,
                            bookingsCount = bookingsCount,
                            wishlistCount = wishlistCount
                        )
                    }

                    // Host mode entry / Create a Listing
                    item {
                        Spacer(modifier = Modifier.height(PaceDreamSpacing.MD))
                        if (isHostMode) {
                            HostModeEntry(
                                isHostMode = true,
                                onSwitchToHostMode = onSwitchToHostMode,
                                onSwitchToGuestMode = onSwitchToGuestMode,
                                onCreateListingClick = onCreateListingClick
                            )
                        } else {
                            // Guest mode: show two separate actions
                            CreateListingEntry(onClick = onCreateListingClick)
                            Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))
                            SwitchToHostModeEntry(onClick = onSwitchToHostMode)
                        }
                    }

                    // Menu Sections — iOS: grouped List rows with NavigationLinks
                    item {
                        Spacer(modifier = Modifier.height(PaceDreamSpacing.LG))
                        ProfileMenuSection(
                            section = ProfileMenuSection(
                                title = "Account",
                                items = listOf(
                                    ProfileMenuItem("Edit Profile", PaceDreamIcons.Person, onEditProfileClick),
                                    ProfileMenuItem("My Reviews", PaceDreamIcons.Star, onReviewsClick),
                                    ProfileMenuItem("Notifications", PaceDreamIcons.Notifications, onNotificationsClick)
                                )
                            ),
                            modifier = Modifier.padding(horizontal = PaceDreamSpacing.MD)
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(PaceDreamSpacing.MD))
                        ProfileMenuSection(
                            section = ProfileMenuSection(
                                title = "Explore",
                                items = listOf(
                                    ProfileMenuItem("Trip Planner", PaceDreamIcons.Map, onTripPlannerClick),
                                    ProfileMenuItem("Destinations", PaceDreamIcons.LocationOn, onDestinationsClick)
                                )
                            ),
                            modifier = Modifier.padding(horizontal = PaceDreamSpacing.MD)
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(PaceDreamSpacing.MD))
                        ProfileMenuSection(
                            section = ProfileMenuSection(
                                title = "Support",
                                items = listOf(
                                    ProfileMenuItem("Help Center", PaceDreamIcons.Help, onHelpClick),
                                    ProfileMenuItem("FAQ", PaceDreamIcons.QuestionAnswer, onFaqClick)
                                )
                            ),
                            modifier = Modifier.padding(horizontal = PaceDreamSpacing.MD)
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(PaceDreamSpacing.MD))
                        ProfileMenuSection(
                            section = ProfileMenuSection(
                                title = "App",
                                items = listOf(
                                    ProfileMenuItem("Settings", PaceDreamIcons.Settings, onSettingsClick),
                                    ProfileMenuItem("About", PaceDreamIcons.Info, onAboutClick),
                                    ProfileMenuItem("Privacy Policy", PaceDreamIcons.PrivacyTip, onPrivacyPolicyClick),
                                    ProfileMenuItem("Terms of Service", PaceDreamIcons.Description, onTermsOfServiceClick)
                                )
                            ),
                            modifier = Modifier.padding(horizontal = PaceDreamSpacing.MD)
                        )
                    }

                    // Sign Out — iOS: plain destructive button in a section
                    item {
                        Spacer(modifier = Modifier.height(PaceDreamSpacing.LG))

                        TextButton(
                            onClick = {
                                viewModel.signOut()
                                onLogoutClick()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = PaceDreamSpacing.MD),
                            contentPadding = PaddingValues(vertical = 12.dp)
                        ) {
                            Icon(
                                imageVector = PaceDreamIcons.ExitToApp,
                                contentDescription = "Sign Out",
                                tint = PaceDreamColors.Error,
                                modifier = Modifier.size(PaceDreamIconSize.SM)
                            )
                            Spacer(modifier = Modifier.width(PaceDreamSpacing.SM))
                            Text(
                                text = "Sign Out",
                                style = PaceDreamTypography.Subheadline,
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

@Composable
private fun RequiresAuthState(
    onSignIn: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PaceDreamColors.Background)
            .statusBarsPadding(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(PaceDreamSpacing.XL)
        ) {
            Icon(
                imageVector = PaceDreamIcons.Lock,
                contentDescription = null,
                tint = PaceDreamColors.TextSecondary.copy(alpha = 0.5f),
                modifier = Modifier.size(48.dp)
            )

            Spacer(modifier = Modifier.height(PaceDreamSpacing.LG))

            Text(
                text = "Sign in to view your profile",
                style = PaceDreamTypography.Title2,
                color = PaceDreamColors.TextPrimary,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))

            Text(
                text = "Manage bookings, favorites, and account settings",
                style = PaceDreamTypography.Subheadline,
                color = PaceDreamColors.TextSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(0.75f)
            )

            Spacer(modifier = Modifier.height(PaceDreamSpacing.XL))

            Button(
                onClick = onSignIn,
                colors = ButtonDefaults.buttonColors(containerColor = PaceDreamColors.Primary),
                shape = RoundedCornerShape(PaceDreamRadius.Round),
                modifier = Modifier.fillMaxWidth(0.7f),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                Text(
                    "Sign in / Create account",
                    style = PaceDreamTypography.Subheadline,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

// ── Profile Header ────────────────────────────────────────────
// iOS: HStack with 60pt avatar circle + VStack(name, email) side-by-side
// No gradient banner, no overlapping layout. Clean horizontal row.

@Composable
fun ProfileHeader(
    name: String,
    email: String,
    memberSince: String,
    onEditClick: () -> Unit,
    bookingsCount: Int = 0,
    wishlistCount: Int = 0,
    verificationStatus: VerificationStatus = VerificationStatus()
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = PaceDreamSpacing.MD)
            .padding(top = PaceDreamSpacing.MD)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar — iOS: 60pt circle with initials
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(CircleShape)
                    .background(PaceDreamColors.Gray100),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = name.firstOrNull()?.uppercase() ?: "?",
                    style = PaceDreamTypography.Headline.copy(fontSize = 18.sp),
                    color = PaceDreamColors.Primary,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name.ifBlank { "Guest" },
                    style = PaceDreamTypography.Callout,
                    color = PaceDreamColors.TextPrimary,
                    fontWeight = FontWeight.Bold
                )
                if (email.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = email,
                        style = PaceDreamTypography.Footnote,
                        color = PaceDreamColors.TextSecondary,
                        fontWeight = FontWeight.Medium
                    )
                }
                if (memberSince.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = memberSince,
                        style = PaceDreamTypography.Caption,
                        color = PaceDreamColors.TextTertiary
                    )
                }
            }

            // Edit button — iOS: "Edit" text button in toolbar
            TextButton(onClick = onEditClick) {
                Text(
                    "Edit",
                    style = PaceDreamTypography.Subheadline.copy(fontSize = 13.sp),
                    color = PaceDreamColors.Primary,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // Verification badges — iOS: inline pills
        if (verificationStatus.hasAnyVerification) {
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(PaceDreamSpacing.SM),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (verificationStatus.identityVerified) {
                    VerificationBadge(
                        icon = PaceDreamIcons.VerifiedUser,
                        label = "ID Verified",
                        color = PaceDreamColors.Success
                    )
                }
                if (verificationStatus.phoneVerified) {
                    VerificationBadge(
                        icon = PaceDreamIcons.Phone,
                        label = "Phone Verified",
                        color = PaceDreamColors.Info
                    )
                }
            }
        }
    }
}

/** Verification badge pill — iOS: StatusBadge capsule style */
@Composable
private fun VerificationBadge(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    color: Color
) {
    Text(
        text = label,
        style = PaceDreamTypography.Caption.copy(fontWeight = FontWeight.Bold),
        color = color,
        modifier = Modifier
            .background(color.copy(alpha = 0.12f), shape = RoundedCornerShape(PaceDreamRadius.Round))
            .padding(horizontal = 10.dp, vertical = 6.dp)
    )
}

/** Primary action cards - bookings, wishlist, bids with counts */
@Composable
private fun PrimaryActionsRow(
    onBookings: () -> Unit,
    onWishlist: () -> Unit,
    onBids: () -> Unit,
    bookingsCount: Int,
    wishlistCount: Int
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = PaceDreamSpacing.MD),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        PrimaryActionCard(
            icon = PaceDreamIcons.CalendarToday,
            label = "Bookings",
            count = bookingsCount,
            onClick = onBookings,
            modifier = Modifier.weight(1f)
        )
        PrimaryActionCard(
            icon = PaceDreamIcons.Favorite,
            label = "Favorites",
            count = wishlistCount,
            onClick = onWishlist,
            modifier = Modifier.weight(1f)
        )
        PrimaryActionCard(
            icon = PaceDreamIcons.Gavel,
            label = "Bids",
            count = null,
            onClick = onBids,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun PrimaryActionCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    count: Int?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        onClick = onClick,
        shape = RoundedCornerShape(PaceDreamRadius.LG),
        colors = CardDefaults.cardColors(containerColor = PaceDreamColors.Card),
        elevation = CardDefaults.cardElevation(defaultElevation = PaceDreamElevation.XS)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = PaceDreamColors.Primary,
                modifier = Modifier.size(PaceDreamIconSize.MD)
            )
            Spacer(modifier = Modifier.height(PaceDreamSpacing.XS))
            if (count != null && count > 0) {
                Text(
                    text = "$count",
                    style = PaceDreamTypography.Headline,
                    color = PaceDreamColors.TextPrimary,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                text = label,
                style = PaceDreamTypography.Caption,
                color = PaceDreamColors.TextSecondary,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

/**
 * Unified host mode entry card.
 *
 * iOS parity: When the user is in guest mode, show "Switch to Host Mode"
 * (not "Start Hosting") to match the iOS ProfileView hostCTA.
 * When the user is in host mode, show "Switch to Guest Mode".
 */
/** Create a Listing CTA — shown in guest mode, prominent primary card */
@Composable
private fun CreateListingEntry(onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = PaceDreamSpacing.MD),
        onClick = onClick,
        shape = RoundedCornerShape(PaceDreamRadius.LG),
        colors = CardDefaults.cardColors(containerColor = PaceDreamColors.Primary),
        elevation = CardDefaults.cardElevation(defaultElevation = PaceDreamElevation.SM)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = PaceDreamIcons.Add,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(PaceDreamIconSize.SM)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Create a Listing",
                    style = PaceDreamTypography.Subheadline,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Share your space, items, or services",
                    style = PaceDreamTypography.Caption,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }
            Icon(
                imageVector = PaceDreamIcons.ChevronRight,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.7f),
                modifier = Modifier.size(PaceDreamIconSize.SM)
            )
        }
    }
}

/** Switch to Host Mode — shown in guest mode, secondary subtle card */
@Composable
private fun SwitchToHostModeEntry(onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = PaceDreamSpacing.MD),
        onClick = onClick,
        shape = RoundedCornerShape(PaceDreamRadius.LG),
        colors = CardDefaults.cardColors(containerColor = PaceDreamColors.Card),
        elevation = CardDefaults.cardElevation(defaultElevation = PaceDreamElevation.XS)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(PaceDreamColors.Primary.copy(alpha = 0.08f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = PaceDreamIcons.Home,
                    contentDescription = null,
                    tint = PaceDreamColors.Primary,
                    modifier = Modifier.size(PaceDreamIconSize.SM)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Switch to Host Mode",
                    style = PaceDreamTypography.Subheadline,
                    color = PaceDreamColors.TextPrimary,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Manage your listings and bookings",
                    style = PaceDreamTypography.Caption,
                    color = PaceDreamColors.TextSecondary
                )
            }
            Icon(
                imageVector = PaceDreamIcons.ChevronRight,
                contentDescription = null,
                tint = PaceDreamColors.TextTertiary,
                modifier = Modifier.size(PaceDreamIconSize.SM)
            )
        }
    }
}

/** Host mode: switch back to guest — shown only in host mode */
@Composable
private fun HostModeEntry(
    isHostMode: Boolean,
    onSwitchToHostMode: () -> Unit,
    onSwitchToGuestMode: () -> Unit,
    onCreateListingClick: () -> Unit = {}
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = PaceDreamSpacing.MD),
        onClick = onSwitchToGuestMode,
        shape = RoundedCornerShape(PaceDreamRadius.LG),
        colors = CardDefaults.cardColors(
            containerColor = PaceDreamColors.Primary.copy(alpha = 0.06f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(PaceDreamColors.Primary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = PaceDreamIcons.Person,
                    contentDescription = null,
                    tint = PaceDreamColors.Primary,
                    modifier = Modifier.size(PaceDreamIconSize.SM)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Switch to Guest Mode",
                    style = PaceDreamTypography.Subheadline,
                    color = PaceDreamColors.Primary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Browse and book spaces",
                    style = PaceDreamTypography.Caption,
                    color = PaceDreamColors.TextSecondary
                )
            }
            Icon(
                imageVector = PaceDreamIcons.ChevronRight,
                contentDescription = null,
                tint = PaceDreamColors.TextTertiary,
                modifier = Modifier.size(PaceDreamIconSize.SM)
            )
        }
    }
}

@Composable
fun ProfileMenuSection(
    section: ProfileMenuSection,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = section.title.uppercase(),
            style = PaceDreamTypography.Caption.copy(fontWeight = FontWeight.SemiBold, letterSpacing = 0.5.sp),
            color = PaceDreamColors.TextTertiary,
            modifier = Modifier.padding(
                start = PaceDreamSpacing.XS,
                bottom = PaceDreamSpacing.SM
            )
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(PaceDreamRadius.LG),
            colors = CardDefaults.cardColors(containerColor = PaceDreamColors.Card),
            elevation = CardDefaults.cardElevation(defaultElevation = PaceDreamElevation.XS)
        ) {
            Column {
                section.items.forEachIndexed { index, item ->
                    ProfileMenuRow(
                        item = item,
                        showDivider = index < section.items.size - 1
                    )
                }
            }
        }
    }
}

@Composable
fun ProfileMenuRow(
    item: ProfileMenuItem,
    showDivider: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = item.onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = item.icon,
            contentDescription = item.title,
            tint = PaceDreamColors.Primary,
            modifier = Modifier.size(PaceDreamIconSize.SM)
        )

        Spacer(modifier = Modifier.width(14.dp))

        Text(
            text = item.title,
            style = PaceDreamTypography.Callout,
            color = PaceDreamColors.TextPrimary,
            modifier = Modifier.weight(1f)
        )

        Icon(
            imageVector = PaceDreamIcons.ChevronRight,
            contentDescription = "Navigate",
            tint = PaceDreamColors.TextTertiary,
            modifier = Modifier.size(PaceDreamIconSize.XS)
        )
    }

    if (showDivider) {
        HorizontalDivider(
            color = PaceDreamColors.Border,
            thickness = 0.5.dp,
            modifier = Modifier.padding(start = 45.dp)
        )
    }
}

data class UserStatData(
    val title: String,
    val value: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

data class ProfileMenuSection(
    val title: String,
    val items: List<ProfileMenuItem>
)

data class ProfileMenuItem(
    val title: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val onClick: () -> Unit
)
