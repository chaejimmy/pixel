package com.pacedream.app.feature.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
 * ProfileScreen – Refactored guest profile hub.
 *
 * Layout (logged-in):
 *   1. Profile header card (avatar, name, email, identity badge, edit button)
 *   2. "Your Activity" section  – Bookings / Favorites rows + Host Mode row
 *   3. "Settings & Support" section – Account settings / Notifications / Help
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
    onNotificationsClick: () -> Unit = onSettingsClick,
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
                contentPadding = PaddingValues(
                    start = PaceDreamSpacing.MD,
                    end = PaceDreamSpacing.MD,
                    top = PaceDreamSpacing.SM,
                    bottom = PaceDreamSpacing.XXXL
                ),
                verticalArrangement = Arrangement.spacedBy(PaceDreamSpacing.LG)
            ) {
                if (!uiState.isLoggedIn) {
                    item {
                        LoggedOutSection(
                            onLoginClick = onLoginClick,
                            onCreateListingClick = onCreateListingClick,
                        )
                    }
                } else {
                    // ── 1. Profile header ────────────────────────────
                    item {
                        UserProfileHeader(
                            userName = uiState.userName,
                            userEmail = uiState.userEmail,
                            userAvatar = uiState.userAvatar,
                            identityStatus = uiState.identityStatus,
                            onEditProfile = onEditProfileClick
                        )
                    }

                    // ── 2. Your Activity ─────────────────────────────
                    item {
                        SectionGroup(title = "Your Activity") {
                            SectionRow(
                                icon = PaceDreamIcons.DateRange,
                                title = "Bookings",
                                trailing = when {
                                    uiState.bookingsCountFailed -> "—"
                                    uiState.bookingsCount > 0 -> "${uiState.bookingsCount}"
                                    else -> null
                                },
                                onClick = onBookingsClick
                            )
                            SectionDivider()
                            SectionRow(
                                icon = PaceDreamIcons.FavoriteBorder,
                                title = "Favorites",
                                trailing = when {
                                    uiState.wishlistCountFailed -> "—"
                                    uiState.wishlistCount > 0 -> "${uiState.wishlistCount}"
                                    else -> null
                                },
                                onClick = onFavoritesClick
                            )
                            SectionDivider()
                            SectionRow(
                                icon = PaceDreamIcons.Home,
                                title = "Host Mode",
                                subtitle = "Manage listings & earnings",
                                onClick = onHostModeClick
                            )
                        }
                    }

                    // ── 3. Settings & Support ────────────────────────
                    item {
                        SectionGroup(title = "Settings & Support") {
                            SectionRow(
                                icon = PaceDreamIcons.Settings,
                                title = "Account settings",
                                onClick = onSettingsClick
                            )
                            SectionDivider()
                            SectionRow(
                                icon = PaceDreamIcons.Notifications,
                                title = "Notifications",
                                onClick = onNotificationsClick
                            )
                            SectionDivider()
                            SectionRow(
                                icon = PaceDreamIcons.Help,
                                title = "Help",
                                onClick = onHelpClick
                            )
                        }
                    }
                }
            }

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

// ─────────────────────────────────────────────────────────────────────────────
// Logged-out state
// ─────────────────────────────────────────────────────────────────────────────

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
            shape = RoundedCornerShape(PaceDreamRadius.LG),
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
            shape = RoundedCornerShape(PaceDreamRadius.LG),
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

// ─────────────────────────────────────────────────────────────────────────────
// Profile header card
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun UserProfileHeader(
    userName: String,
    userEmail: String?,
    userAvatar: String?,
    identityStatus: String? = null,
    onEditProfile: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 4.dp,
                shape = RoundedCornerShape(PaceDreamRadius.XL),
                ambientColor = Color.Black.copy(alpha = 0.04f)
            ),
        shape = RoundedCornerShape(PaceDreamRadius.XL),
        color = PaceDreamColors.Card
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .shadow(2.dp, CircleShape)
                    .border(2.dp, Color.White, CircleShape)
                    .clip(CircleShape)
                    .background(PaceDreamColors.Gray100),
                contentAlignment = Alignment.Center
            ) {
                if (!userAvatar.isNullOrBlank()) {
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
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = PaceDreamColors.TextPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = userName.ifEmpty { "Your account" },
                    style = PaceDreamTypography.Callout.copy(
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = PaceDreamColors.TextPrimary
                )

                userEmail?.let { email ->
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = email,
                        style = PaceDreamTypography.Caption,
                        color = PaceDreamColors.TextSecondary,
                        maxLines = 1
                    )
                }

                if (!identityStatus.isNullOrEmpty()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = PaceDreamIcons.VerifiedUser,
                            contentDescription = null,
                            tint = PaceDreamColors.Success,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = identityStatus,
                            style = PaceDreamTypography.Caption.copy(
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = PaceDreamColors.Success
                        )
                    }
                }
            }

            // Edit button
            TextButton(onClick = onEditProfile) {
                Text(
                    "Edit",
                    style = PaceDreamTypography.Subheadline.copy(fontSize = 13.sp),
                    color = PaceDreamColors.Primary,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Reusable section group (header + card with rows)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SectionGroup(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            style = PaceDreamTypography.Footnote.copy(
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.3.sp
            ),
            color = PaceDreamColors.TextSecondary,
            modifier = Modifier.padding(start = 4.dp)
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(PaceDreamRadius.LG),
            colors = CardDefaults.cardColors(containerColor = PaceDreamColors.Card),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(content = content)
        }
    }
}

@Composable
private fun SectionDivider() {
    HorizontalDivider(
        color = PaceDreamColors.Border,
        thickness = 0.5.dp,
        modifier = Modifier.padding(start = 52.dp)
    )
}

@Composable
private fun SectionRow(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    trailing: String? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = PaceDreamColors.TextSecondary,
            modifier = Modifier.size(20.dp)
        )

        Spacer(modifier = Modifier.width(PaceDreamSpacing.SM2))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = PaceDreamTypography.Callout.copy(fontWeight = FontWeight.Medium),
                color = PaceDreamColors.TextPrimary
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = PaceDreamTypography.Caption,
                    color = PaceDreamColors.TextTertiary
                )
            }
        }

        if (trailing != null) {
            Text(
                text = trailing,
                style = PaceDreamTypography.Footnote.copy(fontWeight = FontWeight.SemiBold),
                color = PaceDreamColors.TextSecondary,
                modifier = Modifier.padding(end = 4.dp)
            )
        }

        Icon(
            imageVector = PaceDreamIcons.ChevronRight,
            contentDescription = null,
            tint = PaceDreamColors.TextTertiary,
            modifier = Modifier.size(16.dp)
        )
    }
}
