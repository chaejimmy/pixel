package com.pacedream.app.feature.profile

import androidx.compose.foundation.background
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
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
    onMyListsClick: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

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

                item {
                    HostModeCard(
                        isHostMode = uiState.isHostMode,
                        onToggle = { viewModel.toggleHostMode() },
                        onHostDashboard = onHostModeClick
                    )
                }

                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(PaceDreamRadius.LG),
                        colors = CardDefaults.cardColors(containerColor = PaceDreamColors.Card),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        ProfileMenuItem(
                            icon = PaceDreamIcons.VerifiedUser,
                            title = "Identity Verification",
                            subtitle = "Verify your identity for a trusted experience",
                            onClick = onIdentityVerificationClick
                        )
                    }
                }

                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(PaceDreamRadius.LG),
                        colors = CardDefaults.cardColors(containerColor = PaceDreamColors.Card),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        ProfileMenuItem(
                            icon = PaceDreamIcons.ListIcon,
                            title = "My Lists",
                            subtitle = "Create and manage your curated lists",
                            onClick = onMyListsClick
                        )
                    }
                }

                item {
                    ProfileMenuSection(
                        onSettingsClick = onSettingsClick,
                        onHelpClick = onHelpClick,
                        onAboutClick = onAboutClick,
                        onLogoutClick = { viewModel.logout() }
                    )
                }
            }
        }
    }
}

@Composable
private fun LoggedOutSection(onLoginClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = PaceDreamSpacing.XL),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(96.dp)
                .clip(CircleShape)
                .background(PaceDreamColors.Gray100),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = PaceDreamIcons.Person,
                contentDescription = "Profile",
                modifier = Modifier.size(48.dp),
                tint = PaceDreamColors.TextSecondary
            )
        }

        Spacer(modifier = Modifier.height(PaceDreamSpacing.LG))

        Text(
            text = "Sign in to view your profile",
            style = PaceDreamTypography.Title3,
            color = PaceDreamColors.TextPrimary
        )

        Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))

        Text(
            text = "Manage your bookings, favorites, and more",
            style = PaceDreamTypography.Body,
            color = PaceDreamColors.TextSecondary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(PaceDreamSpacing.LG))

        Button(
            onClick = onLoginClick,
            modifier = Modifier.fillMaxWidth(0.7f),
            shape = RoundedCornerShape(PaceDreamRadius.MD),
            colors = ButtonDefaults.buttonColors(
                containerColor = PaceDreamColors.Primary
            ),
            contentPadding = PaddingValues(vertical = 14.dp)
        ) {
            Text(
                "Sign In",
                style = PaceDreamTypography.Button
            )
        }
    }
}

@Composable
private fun UserProfileHeader(
    userName: String,
    userEmail: String?,
    userAvatar: String?,
    onEditClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(PaceDreamRadius.LG),
        colors = CardDefaults.cardColors(containerColor = PaceDreamColors.Card),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(PaceDreamSpacing.MD),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
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

            Spacer(modifier = Modifier.width(PaceDreamSpacing.MD))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = userName,
                    style = PaceDreamTypography.Title3,
                    color = PaceDreamColors.TextPrimary
                )

                userEmail?.let { email ->
                    Spacer(modifier = Modifier.height(PaceDreamSpacing.XS))
                    Text(
                        text = email,
                        style = PaceDreamTypography.Callout,
                        color = PaceDreamColors.TextSecondary
                    )
                }
            }

            IconButton(onClick = onEditClick) {
                Icon(
                    imageVector = PaceDreamIcons.Edit,
                    contentDescription = "Edit profile",
                    tint = PaceDreamColors.Primary
                )
            }
        }
    }
}

@Composable
private fun HostModeCard(
    isHostMode: Boolean,
    onToggle: () -> Unit,
    onHostDashboard: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(PaceDreamRadius.LG),
        colors = CardDefaults.cardColors(containerColor = PaceDreamColors.Card),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(PaceDreamSpacing.MD)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Host Mode",
                        style = PaceDreamTypography.Headline,
                        color = PaceDreamColors.TextPrimary
                    )
                    Text(
                        text = if (isHostMode) "You're hosting" else "Switch to hosting",
                        style = PaceDreamTypography.Callout,
                        color = PaceDreamColors.TextSecondary
                    )
                }

                Switch(
                    checked = isHostMode,
                    onCheckedChange = { onToggle() },
                    colors = SwitchDefaults.colors(
                        checkedTrackColor = PaceDreamColors.Primary,
                        checkedThumbColor = PaceDreamColors.OnPrimary
                    )
                )
            }

            if (isHostMode) {
                Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))

                Button(
                    onClick = onHostDashboard,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(PaceDreamRadius.MD),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PaceDreamColors.Primary
                    ),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    Icon(PaceDreamIcons.Home, contentDescription = null)
                    Spacer(modifier = Modifier.width(PaceDreamSpacing.SM))
                    Text("Go to Host Dashboard", style = PaceDreamTypography.Button)
                }
            }
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
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            ProfileMenuItem(
                icon = PaceDreamIcons.Settings,
                title = "Settings",
                subtitle = "Account preferences and privacy",
                onClick = onSettingsClick
            )

            HorizontalDivider(color = PaceDreamColors.Border)

            ProfileMenuItem(
                icon = PaceDreamIcons.Help,
                title = "Help & Support",
                subtitle = "Get help with your account",
                onClick = onHelpClick
            )

            HorizontalDivider(color = PaceDreamColors.Border)

            ProfileMenuItem(
                icon = PaceDreamIcons.Info,
                title = "About",
                subtitle = "App information and legal",
                onClick = onAboutClick
            )

            HorizontalDivider(color = PaceDreamColors.Border)

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
            imageVector = PaceDreamIcons.ArrowForward,
            contentDescription = null,
            tint = PaceDreamColors.TextTertiary,
            modifier = Modifier.size(18.dp)
        )
    }
}
