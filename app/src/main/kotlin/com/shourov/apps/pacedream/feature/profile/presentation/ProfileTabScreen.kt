package com.shourov.apps.pacedream.feature.profile.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.pacedream.common.composables.components.*
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
    // iOS/Web parity: new feature navigation callbacks
    onReviewsClick: () -> Unit = {},
    onBlogClick: () -> Unit = {},
    onTripPlannerClick: () -> Unit = {},
    onSplitBookingsClick: () -> Unit = {},
    onBidsClick: () -> Unit = {},
    onDestinationsClick: () -> Unit = {},
    // iOS parity: navigate to bookings/wishlist from quick actions
    onBookingsClick: () -> Unit = {},
    onWishlistClick: () -> Unit = {},
    onNotificationsClick: () -> Unit = {}
) {
    val viewModel: ProfileTabViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val bookingsCount by viewModel.bookingsCount.collectAsStateWithLifecycle()
    val wishlistCount by viewModel.wishlistCount.collectAsStateWithLifecycle()

    val menuSections = remember {
        listOf(
            ProfileMenuSection(
                title = "Account",
                items = listOf(
                    ProfileMenuItem("Edit Profile", PaceDreamIcons.Person, onEditProfileClick),
                    ProfileMenuItem("My Reviews", PaceDreamIcons.Star, onReviewsClick),
                    ProfileMenuItem("Payment Methods", PaceDreamIcons.Payment, {}),
                    ProfileMenuItem("Addresses", PaceDreamIcons.LocationOn, {}),
                    ProfileMenuItem("Notifications", PaceDreamIcons.Notifications, onNotificationsClick)
                )
            ),
            ProfileMenuSection(
                title = "Explore",
                items = listOf(
                    ProfileMenuItem("Trip Planner", PaceDreamIcons.Map, onTripPlannerClick),
                    ProfileMenuItem("Destinations", PaceDreamIcons.LocationOn, onDestinationsClick),
                    ProfileMenuItem("Blog", PaceDreamIcons.Article, onBlogClick),
                    ProfileMenuItem("My Bids", PaceDreamIcons.Gavel, onBidsClick),
                    ProfileMenuItem("Split Bookings", PaceDreamIcons.Group, onSplitBookingsClick)
                )
            ),
            ProfileMenuSection(
                title = "Support",
                items = listOf(
                    ProfileMenuItem("Help Center", PaceDreamIcons.Help, onHelpClick),
                    ProfileMenuItem("FAQ", PaceDreamIcons.QuestionAnswer, onFaqClick),
                    ProfileMenuItem("Contact Us", PaceDreamIcons.Email, {}),
                    ProfileMenuItem("Report a Problem", PaceDreamIcons.Report, {})
                )
            ),
            ProfileMenuSection(
                title = "App",
                items = listOf(
                    ProfileMenuItem("Settings", PaceDreamIcons.Settings, onSettingsClick),
                    ProfileMenuItem("About", PaceDreamIcons.Info, onAboutClick),
                    ProfileMenuItem("Privacy Policy", PaceDreamIcons.PrivacyTip, onPrivacyPolicyClick),
                    ProfileMenuItem("Terms of Service", PaceDreamIcons.Description, onTermsOfServiceClick)
                )
            )
        )
    }

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
                    // Profile Header with stats (iOS parity)
                    item {
                        ProfileHeader(
                            name = state.user.displayName,
                            email = state.user.email ?: "",
                            memberSince = "",
                            onEditClick = onEditProfileClick,
                            bookingsCount = bookingsCount,
                            wishlistCount = wishlistCount
                        )
                    }

                    // iOS parity: Quick action chips
                    item {
                        Spacer(modifier = Modifier.height(PaceDreamSpacing.MD))
                        QuickActionChips(
                            onEditProfile = onEditProfileClick,
                            onBookings = onBookingsClick,
                            onWishlist = onWishlistClick,
                            onHostMode = onSwitchToHostMode,
                            onBids = onBidsClick
                        )
                    }

                    // iOS parity: Create a listing CTA
                    item {
                        Spacer(modifier = Modifier.height(PaceDreamSpacing.MD))
                        CreateListingCTA(onClick = onSwitchToHostMode)
                    }

                    // Host Mode Toggle
                    item {
                        Spacer(modifier = Modifier.height(PaceDreamSpacing.MD))
                        HostModeToggle(
                            isHostMode = isHostMode,
                            onSwitchToHostMode = onSwitchToHostMode,
                            onSwitchToGuestMode = onSwitchToGuestMode
                        )
                    }

                    // Menu Sections
                    items(menuSections) { section ->
                        Spacer(modifier = Modifier.height(PaceDreamSpacing.MD))

                        ProfileMenuSection(
                            section = section,
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
    wishlistCount: Int = 0
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
                .height(80.dp)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            PaceDreamColors.Primary.copy(alpha = 0.12f),
                            PaceDreamColors.Primary.copy(alpha = 0.03f)
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .offset(y = (-40).dp)
                .padding(horizontal = PaceDreamSpacing.MD),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(PaceDreamColors.Primary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = name.firstOrNull()?.uppercase() ?: "?",
                    style = PaceDreamTypography.LargeTitle,
                    color = PaceDreamColors.Primary,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))

            Text(
                text = name,
                style = PaceDreamTypography.Title2,
                color = PaceDreamColors.TextPrimary,
                fontWeight = FontWeight.Bold
            )

            if (email.isNotBlank()) {
                Text(
                    text = email,
                    style = PaceDreamTypography.Caption,
                    color = PaceDreamColors.TextSecondary
                )
            }

            if (memberSince.isNotBlank()) {
                Text(
                    text = memberSince,
                    style = PaceDreamTypography.Caption2,
                    color = PaceDreamColors.TextTertiary
                )
            }

            // iOS parity: stat pills showing bookings & wishlist count
            Spacer(modifier = Modifier.height(PaceDreamSpacing.MD))

            Row(
                horizontalArrangement = Arrangement.spacedBy(PaceDreamSpacing.SM),
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatPill(
                    icon = PaceDreamIcons.CalendarToday,
                    label = "$bookingsCount Bookings"
                )
                StatPill(
                    icon = PaceDreamIcons.Favorite,
                    label = "$wishlistCount Wishlist"
                )
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

/** iOS parity: small pill showing an icon + label (e.g., "3 Bookings") */
@Composable
private fun StatPill(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String
) {
    Surface(
        shape = RoundedCornerShape(PaceDreamRadius.Round),
        color = PaceDreamColors.Primary.copy(alpha = 0.08f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = PaceDreamSpacing.SM, vertical = PaceDreamSpacing.XS),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(PaceDreamSpacing.XS)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = PaceDreamColors.Primary,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = label,
                style = PaceDreamTypography.Caption2,
                color = PaceDreamColors.Primary,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

/** iOS parity: horizontally scrolling quick action chips */
@Composable
private fun QuickActionChips(
    onEditProfile: () -> Unit,
    onBookings: () -> Unit,
    onWishlist: () -> Unit,
    onHostMode: () -> Unit,
    onBids: () -> Unit
) {
    val chips = remember {
        listOf(
            Triple(PaceDreamIcons.Edit, "Edit profile", onEditProfile),
            Triple(PaceDreamIcons.CalendarToday, "Bookings", onBookings),
            Triple(PaceDreamIcons.Favorite, "Wishlist", onWishlist),
            Triple(PaceDreamIcons.Home, "Host Mode", onHostMode),
            Triple(PaceDreamIcons.Gavel, "My Bids", onBids)
        )
    }

    androidx.compose.foundation.lazy.LazyRow(
        contentPadding = PaddingValues(horizontal = PaceDreamSpacing.MD),
        horizontalArrangement = Arrangement.spacedBy(PaceDreamSpacing.SM)
    ) {
        items(chips.size) { index ->
            val (icon, label, onClick) = chips[index]
            AssistChip(
                onClick = onClick,
                label = {
                    Text(
                        label,
                        style = PaceDreamTypography.Caption,
                        fontWeight = FontWeight.Medium
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = icon,
                        contentDescription = label,
                        modifier = Modifier.size(16.dp)
                    )
                },
                shape = RoundedCornerShape(PaceDreamRadius.Round),
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = PaceDreamColors.Card,
                    labelColor = PaceDreamColors.TextPrimary,
                    leadingIconContentColor = PaceDreamColors.Primary
                ),
                border = AssistChipDefaults.assistChipBorder(
                    borderColor = PaceDreamColors.Border
                )
            )
        }
    }
}

/** iOS parity: prominent "Create a listing" CTA with gradient background */
@Composable
private fun CreateListingCTA(onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = PaceDreamSpacing.MD)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(PaceDreamRadius.LG),
        elevation = CardDefaults.cardElevation(defaultElevation = PaceDreamElevation.SM)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            PaceDreamColors.Primary,
                            PaceDreamColors.Primary.copy(alpha = 0.8f)
                        )
                    )
                )
                .padding(PaceDreamSpacing.MD)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = PaceDreamIcons.Add,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(PaceDreamIconSize.MD)
                    )
                }
                Spacer(modifier = Modifier.width(PaceDreamSpacing.MD))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Create a listing",
                        style = PaceDreamTypography.Callout,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Start hosting and earning",
                        style = PaceDreamTypography.Caption2,
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
}

@Composable
fun UserStatCard(
    stat: UserStatData
) {
    Card(
        modifier = Modifier.width(100.dp),
        shape = RoundedCornerShape(PaceDreamRadius.LG),
        colors = CardDefaults.cardColors(containerColor = PaceDreamColors.Card),
        elevation = CardDefaults.cardElevation(defaultElevation = PaceDreamElevation.XS)
    ) {
        Column(
            modifier = Modifier.padding(PaceDreamSpacing.MD),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = stat.icon,
                contentDescription = stat.title,
                tint = PaceDreamColors.Primary,
                modifier = Modifier.size(PaceDreamIconSize.MD)
            )

            Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))

            Text(
                text = stat.value,
                style = PaceDreamTypography.Title2,
                color = PaceDreamColors.TextPrimary,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = stat.title,
                style = PaceDreamTypography.Caption2,
                color = PaceDreamColors.TextSecondary
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

@Composable
fun HostModeToggle(
    isHostMode: Boolean,
    onSwitchToHostMode: () -> Unit,
    onSwitchToGuestMode: () -> Unit
) {
    val accentColor = if (isHostMode) PaceDreamColors.Primary else PaceDreamColors.Info

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = PaceDreamSpacing.MD),
        shape = RoundedCornerShape(PaceDreamRadius.LG),
        colors = CardDefaults.cardColors(
            containerColor = accentColor.copy(alpha = 0.06f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = PaceDreamElevation.XS)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(PaceDreamSpacing.MD),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isHostMode) PaceDreamIcons.Home else PaceDreamIcons.Person,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(PaceDreamIconSize.SM)
                    )
                }
                Spacer(modifier = Modifier.width(PaceDreamSpacing.SM))
                Column {
                    Text(
                        text = if (isHostMode) "Host Mode" else "Guest Mode",
                        style = PaceDreamTypography.Callout,
                        color = accentColor,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = if (isHostMode) "Manage your listings and bookings" else "Book amazing properties",
                        style = PaceDreamTypography.Caption2,
                        color = PaceDreamColors.TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.width(PaceDreamSpacing.SM))

            Button(
                onClick = if (isHostMode) onSwitchToGuestMode else onSwitchToHostMode,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isHostMode) PaceDreamColors.Info else PaceDreamColors.Primary
                ),
                shape = RoundedCornerShape(PaceDreamRadius.Round),
                contentPadding = PaddingValues(horizontal = PaceDreamSpacing.MD, vertical = PaceDreamSpacing.SM)
            ) {
                Text(
                    text = if (isHostMode) "Guest" else "Host",
                    style = PaceDreamTypography.Caption,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
