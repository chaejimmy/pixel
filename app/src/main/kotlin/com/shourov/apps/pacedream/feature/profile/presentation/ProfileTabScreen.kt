package com.shourov.apps.pacedream.feature.profile.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
    isHostMode: Boolean = false
) {
    val viewModel: ProfileTabViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val menuSections = remember {
        listOf(
            ProfileMenuSection(
                title = "Account",
                items = listOf(
                    ProfileMenuItem("Edit Profile", PaceDreamIcons.Person, onEditProfileClick),
                    ProfileMenuItem("Payment Methods", PaceDreamIcons.Payment, {}),
                    ProfileMenuItem("Addresses", PaceDreamIcons.LocationOn, {}),
                    ProfileMenuItem("Notifications", PaceDreamIcons.Notifications, {})
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
                modifier = Modifier.fillMaxSize()
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
                modifier = Modifier.fillMaxSize()
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(PaceDreamColors.Background),
                    contentPadding = PaddingValues(bottom = PaceDreamSpacing.XXXL)
                ) {
                    // Profile Header
                    item {
                        ProfileHeader(
                            name = state.user.displayName,
                            email = state.user.email ?: "",
                            memberSince = "",
                            onEditClick = onEditProfileClick
                        )
                    }

                    // Host Mode Toggle
                    item {
                        Spacer(modifier = Modifier.height(PaceDreamSpacing.LG))
                        HostModeToggle(
                            isHostMode = isHostMode,
                            onSwitchToHostMode = onSwitchToHostMode,
                            onSwitchToGuestMode = onSwitchToGuestMode
                        )
                    }

                    // Menu Sections
                    items(menuSections) { section ->
                        Spacer(modifier = Modifier.height(PaceDreamSpacing.LG))

                        ProfileMenuSection(
                            section = section,
                            modifier = Modifier.padding(horizontal = PaceDreamSpacing.LG)
                        )
                    }

                    // Logout Button
                    item {
                        Spacer(modifier = Modifier.height(PaceDreamSpacing.XL))

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = PaceDreamSpacing.LG)
                                .clickable {
                                    viewModel.signOut()
                                    onLogoutClick()
                                },
                            colors = CardDefaults.cardColors(containerColor = PaceDreamColors.Error.copy(alpha = 0.1f)),
                            elevation = CardDefaults.cardElevation(defaultElevation = PaceDreamElevation.SM)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(PaceDreamSpacing.LG),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = PaceDreamIcons.ExitToApp,
                                    contentDescription = "Logout",
                                    tint = PaceDreamColors.Error,
                                    modifier = Modifier.size(20.dp)
                                )

                                Spacer(modifier = Modifier.width(PaceDreamSpacing.SM))

                                Text(
                                    text = "Sign Out",
                                    style = PaceDreamTypography.Body,
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
}

@Composable
private fun RequiresAuthState(
    onSignIn: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PaceDreamColors.Background)
            .padding(PaceDreamSpacing.XL),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = PaceDreamIcons.Lock,
                contentDescription = null,
                tint = PaceDreamColors.TextSecondary,
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(PaceDreamSpacing.MD))
            Text(
                text = "Sign in to view your profile",
                style = PaceDreamTypography.Title3,
                color = PaceDreamColors.TextPrimary
            )
            Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))
            Text(
                text = "Manage bookings, favorites, and account settings",
                style = PaceDreamTypography.Body,
                color = PaceDreamColors.TextSecondary
            )
            Spacer(modifier = Modifier.height(PaceDreamSpacing.LG))
            Button(
                onClick = onSignIn,
                colors = ButtonDefaults.buttonColors(containerColor = PaceDreamColors.Primary),
                modifier = Modifier.fillMaxWidth(0.6f)
            ) {
                Text("Sign in / Create account", style = PaceDreamTypography.Headline)
            }
        }
    }
}

@Composable
fun ProfileHeader(
    name: String,
    email: String,
    memberSince: String,
    onEditClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(PaceDreamSpacing.LG),
        colors = CardDefaults.cardColors(containerColor = PaceDreamColors.Card),
        elevation = CardDefaults.cardElevation(defaultElevation = PaceDreamElevation.SM)
    ) {
        Column(
            modifier = Modifier.padding(PaceDreamSpacing.LG),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Avatar
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(PaceDreamColors.Primary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = name.firstOrNull()?.uppercase() ?: "?",
                    style = PaceDreamTypography.LargeTitle,
                    color = PaceDreamColors.Primary,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(PaceDreamSpacing.MD))
            
            Text(
                text = name,
                style = PaceDreamTypography.Title2,
                color = PaceDreamColors.TextPrimary,
                fontWeight = FontWeight.Bold
            )
            
            Text(
                text = email,
                style = PaceDreamTypography.Body,
                color = PaceDreamColors.TextSecondary
            )
            
            Text(
                text = memberSince,
                style = PaceDreamTypography.Caption,
                color = PaceDreamColors.TextSecondary
            )
            
            Spacer(modifier = Modifier.height(PaceDreamSpacing.MD))
            
            OutlinedButton(
                onClick = onEditClick,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = PaceDreamColors.Primary
                ),
                border = ButtonDefaults.outlinedButtonBorder.copy(
                    brush = androidx.compose.ui.graphics.SolidColor(PaceDreamColors.Primary)
                )
            ) {
                Icon(
                    imageVector = PaceDreamIcons.Edit,
                    contentDescription = "Edit",
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(PaceDreamSpacing.XS))
                Text("Edit Profile")
            }
        }
    }
}

@Composable
fun UserStatCard(
    stat: UserStatData
) {
    Card(
        modifier = Modifier
            .width(100.dp)
            .clip(RoundedCornerShape(PaceDreamRadius.LG)),
        colors = CardDefaults.cardColors(containerColor = PaceDreamColors.Card),
        elevation = CardDefaults.cardElevation(defaultElevation = PaceDreamElevation.SM)
    ) {
        Column(
            modifier = Modifier.padding(PaceDreamSpacing.MD),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = stat.icon,
                contentDescription = stat.title,
                tint = PaceDreamColors.Primary,
                modifier = Modifier.size(24.dp)
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
                style = PaceDreamTypography.Caption,
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
            style = PaceDreamTypography.Callout,
            color = PaceDreamColors.TextSecondary,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = PaceDreamSpacing.SM)
        )
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = PaceDreamColors.Card),
            elevation = CardDefaults.cardElevation(defaultElevation = PaceDreamElevation.SM)
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
            .padding(PaceDreamSpacing.MD),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = item.icon,
            contentDescription = item.title,
            tint = PaceDreamColors.TextSecondary,
            modifier = Modifier.size(20.dp)
        )
        
        Spacer(modifier = Modifier.width(PaceDreamSpacing.MD))
        
        Text(
            text = item.title,
            style = PaceDreamTypography.Body,
            color = PaceDreamColors.TextPrimary,
            modifier = Modifier.weight(1f)
        )
        
        Icon(
            imageVector = PaceDreamIcons.ChevronRight,
            contentDescription = "Navigate",
            tint = PaceDreamColors.TextSecondary,
            modifier = Modifier.size(16.dp)
        )
    }
    
    if (showDivider) {
        HorizontalDivider(
            color = PaceDreamColors.Border,
            thickness = 0.5.dp,
            modifier = Modifier.padding(start = 48.dp)
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
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = PaceDreamSpacing.LG),
        colors = CardDefaults.cardColors(
            containerColor = if (isHostMode) 
                PaceDreamColors.Primary.copy(alpha = 0.1f) 
            else 
                PaceDreamColors.Info.copy(alpha = 0.1f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = PaceDreamElevation.SM)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(PaceDreamSpacing.LG),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (isHostMode) PaceDreamIcons.Home else PaceDreamIcons.Person,
                    contentDescription = null,
                    tint = if (isHostMode) PaceDreamColors.Primary else PaceDreamColors.Info,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(PaceDreamSpacing.MD))
                Column {
                    Text(
                        text = if (isHostMode) "Host Mode" else "Guest Mode",
                        style = PaceDreamTypography.Headline,
                        color = if (isHostMode) PaceDreamColors.Primary else PaceDreamColors.Info,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = if (isHostMode) "Manage your listings and bookings" else "Book amazing properties",
                        style = PaceDreamTypography.Caption,
                        color = PaceDreamColors.TextSecondary
                    )
                }
            }
            
            Button(
                onClick = if (isHostMode) onSwitchToGuestMode else onSwitchToHostMode,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isHostMode) PaceDreamColors.Info else PaceDreamColors.Primary
                )
            ) {
                Text(
                    text = if (isHostMode) "Switch to Guest" else "Switch to Host",
                    style = PaceDreamTypography.Caption,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
