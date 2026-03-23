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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
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
    isHostMode: Boolean = false,
    onReviewsClick: () -> Unit = {},
    onBlogClick: () -> Unit = {},
    onTripPlannerClick: () -> Unit = {},
    onSplitBookingsClick: () -> Unit = {},
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
                    // Profile Header with verification badges
                    item {
                        ProfileHeader(
                            name = state.user.displayName,
                            email = state.user.email ?: "",
                            memberSince = memberSince,
                            onEditClick = onEditProfileClick,
                            bookingsCount = bookingsCount,
                            wishlistCount = wishlistCount,
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

                    // Host mode entry - single clear CTA instead of toggle + CTA
                    item {
                        Spacer(modifier = Modifier.height(PaceDreamSpacing.MD))
                        HostModeEntry(
                            isHostMode = isHostMode,
                            onSwitchToHostMode = onSwitchToHostMode,
                            onSwitchToGuestMode = onSwitchToGuestMode
                        )
                    }

                    // Menu Sections - grouped with clear visual hierarchy
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
                                    ProfileMenuItem("Destinations", PaceDreamIcons.LocationOn, onDestinationsClick),
                                    ProfileMenuItem("Blog", PaceDreamIcons.Article, onBlogClick),
                                    ProfileMenuItem("Split Bookings", PaceDreamIcons.Group, onSplitBookingsClick)
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

                    // Logout Button
                    item {
                        Spacer(modifier = Modifier.height(PaceDreamSpacing.XL))

                        OutlinedButton(
                            onClick = {
                                viewModel.signOut()
                                onLogoutClick()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = PaceDreamSpacing.MD),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = PaceDreamColors.Error
                            ),
                            border = ButtonDefaults.outlinedButtonBorder.copy(
                                brush = androidx.compose.ui.graphics.SolidColor(PaceDreamColors.Error.copy(alpha = 0.5f))
                            ),
                            shape = RoundedCornerShape(PaceDreamRadius.LG),
                            contentPadding = PaddingValues(vertical = PaceDreamSpacing.MD)
                        ) {
                            Icon(
                                imageVector = PaceDreamIcons.ExitToApp,
                                contentDescription = "Logout",
                                tint = PaceDreamColors.Error,
                                modifier = Modifier.size(PaceDreamIconSize.SM)
                            )
                            Spacer(modifier = Modifier.width(PaceDreamSpacing.SM))
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
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .clip(CircleShape)
                    .background(PaceDreamColors.Primary.copy(alpha = 0.08f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = PaceDreamIcons.Lock,
                    contentDescription = null,
                    tint = PaceDreamColors.Primary,
                    modifier = Modifier.size(PaceDreamIconSize.XL)
                )
            }

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
                style = PaceDreamTypography.Callout,
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
                contentPadding = PaddingValues(vertical = PaceDreamSpacing.MD)
            ) {
                Text(
                    "Sign in / Create account",
                    style = PaceDreamTypography.Callout,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

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
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = PaceDreamSpacing.MD)
            .padding(top = PaceDreamSpacing.MD),
        shape = RoundedCornerShape(PaceDreamRadius.LG),
        colors = CardDefaults.cardColors(containerColor = PaceDreamColors.Card),
        elevation = CardDefaults.cardElevation(defaultElevation = PaceDreamElevation.XS)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            PaceDreamColors.Primary.copy(alpha = 0.10f),
                            PaceDreamColors.Primary.copy(alpha = 0.02f)
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .offset(y = (-36).dp)
                .padding(horizontal = PaceDreamSpacing.MD),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(PaceDreamColors.Primary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = name.firstOrNull()?.uppercase() ?: "?",
                    style = PaceDreamTypography.Title1,
                    color = PaceDreamColors.Primary,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))

            Text(
                text = name,
                style = PaceDreamTypography.Title3,
                color = PaceDreamColors.TextPrimary,
                fontWeight = FontWeight.Bold
            )

            if (email.isNotBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = email,
                    style = PaceDreamTypography.Caption,
                    color = PaceDreamColors.TextSecondary
                )
            }

            if (memberSince.isNotBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = memberSince,
                    style = PaceDreamTypography.Caption2,
                    color = PaceDreamColors.TextTertiary
                )
            }

            // Verification badges (website parity)
            if (verificationStatus.hasAnyVerification) {
                Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))
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

            Spacer(modifier = Modifier.height(PaceDreamSpacing.MD))

            OutlinedButton(
                onClick = onEditClick,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = PaceDreamColors.Primary
                ),
                border = ButtonDefaults.outlinedButtonBorder.copy(
                    brush = androidx.compose.ui.graphics.SolidColor(PaceDreamColors.Primary)
                ),
                shape = RoundedCornerShape(PaceDreamRadius.Round),
                contentPadding = PaddingValues(horizontal = PaceDreamSpacing.LG, vertical = PaceDreamSpacing.SM)
            ) {
                Icon(
                    imageVector = PaceDreamIcons.Edit,
                    contentDescription = "Edit",
                    modifier = Modifier.size(PaceDreamIconSize.XS)
                )
                Spacer(modifier = Modifier.width(PaceDreamSpacing.XS))
                Text(
                    "Edit Profile",
                    style = PaceDreamTypography.Caption,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))
        }
    }
}

/** Verification badge pill */
@Composable
private fun VerificationBadge(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    color: Color
) {
    Surface(
        shape = RoundedCornerShape(PaceDreamRadius.Round),
        color = color.copy(alpha = 0.10f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = PaceDreamSpacing.SM, vertical = PaceDreamSpacing.XS),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(PaceDreamSpacing.XS)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(12.dp)
            )
            Text(
                text = label,
                style = PaceDreamTypography.Caption2,
                color = color,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
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
        horizontalArrangement = Arrangement.spacedBy(PaceDreamSpacing.SM)
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
            label = "Wishlist",
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
                .padding(PaceDreamSpacing.MD),
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
                style = PaceDreamTypography.Caption2,
                color = PaceDreamColors.TextSecondary,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

/** Unified host mode entry - replaces the old toggle + CTA combo */
@Composable
private fun HostModeEntry(
    isHostMode: Boolean,
    onSwitchToHostMode: () -> Unit,
    onSwitchToGuestMode: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = PaceDreamSpacing.MD),
        onClick = if (isHostMode) onSwitchToGuestMode else onSwitchToHostMode,
        shape = RoundedCornerShape(PaceDreamRadius.LG),
        colors = CardDefaults.cardColors(
            containerColor = if (isHostMode)
                PaceDreamColors.Primary.copy(alpha = 0.06f)
            else
                PaceDreamColors.Primary
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isHostMode) 0.dp else PaceDreamElevation.SM)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(PaceDreamSpacing.MD),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        if (isHostMode) PaceDreamColors.Primary.copy(alpha = 0.12f)
                        else Color.White.copy(alpha = 0.2f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isHostMode) PaceDreamIcons.Person else PaceDreamIcons.Add,
                    contentDescription = null,
                    tint = if (isHostMode) PaceDreamColors.Primary else Color.White,
                    modifier = Modifier.size(PaceDreamIconSize.SM)
                )
            }
            Spacer(modifier = Modifier.width(PaceDreamSpacing.MD))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isHostMode) "Switch to Guest Mode" else "Start Hosting",
                    style = PaceDreamTypography.Callout,
                    color = if (isHostMode) PaceDreamColors.Primary else Color.White,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (isHostMode) "Browse and book spaces"
                    else "List your space and start earning",
                    style = PaceDreamTypography.Caption2,
                    color = if (isHostMode) PaceDreamColors.TextSecondary
                    else Color.White.copy(alpha = 0.8f)
                )
            }
            Icon(
                imageVector = PaceDreamIcons.ChevronRight,
                contentDescription = null,
                tint = if (isHostMode) PaceDreamColors.TextTertiary
                else Color.White.copy(alpha = 0.7f),
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
            text = section.title,
            style = PaceDreamTypography.Caption,
            color = PaceDreamColors.TextTertiary,
            fontWeight = FontWeight.SemiBold,
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
            .padding(horizontal = PaceDreamSpacing.MD, vertical = PaceDreamSpacing.SM),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(PaceDreamRadius.SM))
                .background(PaceDreamColors.Primary.copy(alpha = 0.08f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = item.title,
                tint = PaceDreamColors.Primary,
                modifier = Modifier.size(PaceDreamIconSize.XS)
            )
        }

        Spacer(modifier = Modifier.width(PaceDreamSpacing.MD))

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
            modifier = Modifier.padding(start = 56.dp)
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
