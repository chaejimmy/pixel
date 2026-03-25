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
 * ProfileScreen - Clean profile hub
 *
 * Grouped into clear sections:
 * 1. Profile header (avatar, name, email, identity badge)
 * 2. Account section (Edit Profile, Identity Verification)
 * 3. Activity section (Bookings, Favorites)
 * 4. Settings & Support section (Account settings, Notifications, Help, Host mode)
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
                    // 1. Profile header
                    item {
                        UserProfileHeader(
                            userName = uiState.userName,
                            userEmail = uiState.userEmail,
                            userAvatar = uiState.userAvatar,
                            identityStatus = uiState.identityStatus,
                            onEditProfileClick = onEditProfileClick
                        )
                    }

                    // 2. Account section
                    item {
                        ProfileSection(title = "Account") {
                            ProfileRow(
                                icon = PaceDreamIcons.Person,
                                title = "Edit profile",
                                onClick = onEditProfileClick
                            )
                            ProfileDivider()
                            ProfileRow(
                                icon = PaceDreamIcons.VerifiedUser,
                                title = "Identity verification",
                                subtitle = uiState.identityStatus,
                                onClick = onIdentityVerificationClick
                            )
                        }
                    }

                    // 3. Activity section
                    item {
                        ProfileSection(title = "Activity") {
                            ProfileRow(
                                icon = PaceDreamIcons.DateRange,
                                title = "Bookings",
                                trailingText = "${uiState.bookingsCount}",
                                onClick = onBookingsClick
                            )
                            ProfileDivider()
                            ProfileRow(
                                icon = PaceDreamIcons.FavoriteBorder,
                                title = "Favorites",
                                trailingText = "${uiState.wishlistCount}",
                                onClick = onFavoritesClick
                            )
                        }
                    }

                    // 4. Settings & Support section
                    item {
                        ProfileSection(title = "Settings & Support") {
                            ProfileRow(
                                icon = PaceDreamIcons.Settings,
                                title = "Account settings",
                                onClick = onSettingsClick
                            )
                            ProfileDivider()
                            ProfileRow(
                                icon = PaceDreamIcons.Notifications,
                                title = "Notifications & preferences",
                                onClick = onSettingsClick
                            )
                            ProfileDivider()
                            ProfileRow(
                                icon = PaceDreamIcons.QuestionAnswer,
                                title = "Help & support",
                                onClick = onHelpClick
                            )
                        }
                    }

                    // 5. Host mode — visible but not overpowering
                    item {
                        HostModeRow(onClick = onHostModeClick)
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

// ─────────────────────────────────────────────────────────────────────────────
// Profile header — avatar, name, email, identity badge, edit tap target
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun UserProfileHeader(
    userName: String,
    userEmail: String?,
    userAvatar: String?,
    identityStatus: String? = null,
    onEditProfileClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onEditProfileClick)
            .padding(vertical = PaceDreamSpacing.SM),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(PaceDreamSpacing.MD)
    ) {
        // Avatar
        Box(
            modifier = Modifier
                .size(64.dp)
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
                        fontSize = 20.sp,
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
                    fontSize = 20.sp,
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
                Spacer(modifier = Modifier.height(4.dp))
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
                        style = PaceDreamTypography.Caption,
                        color = PaceDreamColors.Success
                    )
                }
            }
        }

        Icon(
            imageVector = PaceDreamIcons.ChevronRight,
            contentDescription = null,
            tint = PaceDreamColors.TextTertiary,
            modifier = Modifier.size(16.dp)
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Reusable section: title + grouped card of rows
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ProfileSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(PaceDreamSpacing.SM)) {
        Text(
            text = title,
            style = PaceDreamTypography.Subheadline.copy(
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp
            ),
            color = PaceDreamColors.TextSecondary,
            modifier = Modifier.padding(start = PaceDreamSpacing.XS)
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(PaceDreamRadius.LG),
            colors = CardDefaults.cardColors(containerColor = PaceDreamColors.Card),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(content = content)
        }
    }
}

@Composable
private fun ProfileRow(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    trailingText: String? = null,
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
            modifier = Modifier.size(20.dp)
        )

        Spacer(modifier = Modifier.width(PaceDreamSpacing.SM2))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = PaceDreamTypography.Body,
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

        if (trailingText != null) {
            Text(
                text = trailingText,
                style = PaceDreamTypography.Subheadline,
                color = PaceDreamColors.TextTertiary,
                modifier = Modifier.padding(end = PaceDreamSpacing.SM)
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

@Composable
private fun ProfileDivider() {
    HorizontalDivider(
        color = PaceDreamColors.Border,
        modifier = Modifier.padding(start = 48.dp)
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// Host mode entry — subtle outlined row, not a dominant CTA
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun HostModeRow(onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(PaceDreamRadius.LG),
        color = PaceDreamColors.Primary.copy(alpha = 0.05f),
        border = androidx.compose.foundation.BorderStroke(
            0.5.dp,
            PaceDreamColors.Primary.copy(alpha = 0.15f)
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = PaceDreamSpacing.MD, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = PaceDreamIcons.Home,
                contentDescription = null,
                tint = PaceDreamColors.Primary,
                modifier = Modifier.size(20.dp)
            )

            Spacer(modifier = Modifier.width(PaceDreamSpacing.SM2))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Switch to Host Mode",
                    style = PaceDreamTypography.Body.copy(fontWeight = FontWeight.Medium),
                    color = PaceDreamColors.TextPrimary
                )
                Text(
                    text = "Manage listings & earnings",
                    style = PaceDreamTypography.Caption,
                    color = PaceDreamColors.TextTertiary
                )
            }

            Icon(
                imageVector = PaceDreamIcons.ChevronRight,
                contentDescription = null,
                tint = PaceDreamColors.Primary.copy(alpha = 0.6f),
                modifier = Modifier.size(14.dp)
            )
        }
    }
}
