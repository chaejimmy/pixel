package com.shourov.apps.pacedream.feature.host.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.pacedream.common.composables.components.*
import com.pacedream.common.composables.theme.*
import com.shourov.apps.pacedream.feature.host.domain.HostModeManager
import com.shourov.apps.pacedream.feature.host.navigation.HostNavigationGraph
import com.shourov.apps.pacedream.feature.host.presentation.components.HostBottomNavigation

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HostModeScreen(
    hostModeManager: HostModeManager,
    onSwitchToGuestMode: () -> Unit,
    onNavigateToProperty: (String) -> Unit = {},
    onNavigateToBooking: (String) -> Unit = {},
    onNavigateToAddListing: () -> Unit = {},
    onNavigateToEditListing: (String) -> Unit = {},
    onNavigateToAnalytics: () -> Unit = {},
    onNavigateToWithdraw: () -> Unit = {}
) {
    val navController = rememberNavController()
    var currentRoute by remember { mutableStateOf("host_dashboard") }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = PaceDreamColors.Background,
        bottomBar = {
            HostBottomNavigation(
                currentRoute = currentRoute,
                onNavigate = { route ->
                    currentRoute = route
                    navController.navigate(route) {
                        popUpTo("host_dashboard") {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = "host_dashboard",
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            HostNavigationGraph(
                navController = navController,
                onNavigateToProperty = onNavigateToProperty,
                onNavigateToBooking = onNavigateToBooking,
                onNavigateToAddListing = onNavigateToAddListing,
                onNavigateToEditListing = onNavigateToEditListing,
                onNavigateToAnalytics = onNavigateToAnalytics,
                onNavigateToWithdraw = onNavigateToWithdraw
            )
        }
    }
}

@Composable
fun HostModeWelcomeScreen(
    onStartHosting: () -> Unit,
    onContinueAsGuest: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        PaceDreamColors.Primary,
                        PaceDreamColors.Primary.copy(alpha = 0.85f),
                        PaceDreamColors.Primary.copy(alpha = 0.7f)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = PaceDreamSpacing.XL)
                .padding(bottom = PaceDreamSpacing.XXXL),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Glowing icon container
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = PaceDreamIcons.Home,
                        contentDescription = "Host",
                        tint = Color.White,
                        modifier = Modifier.size(40.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(PaceDreamSpacing.XXXL))

            Text(
                text = "Welcome to\nHost Mode",
                style = PaceDreamTypography.LargeTitle,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                lineHeight = 40.sp
            )

            Spacer(modifier = Modifier.height(PaceDreamSpacing.MD))

            Text(
                text = "Turn your space into income. List your property, manage bookings, and start earning with PaceDream.",
                style = PaceDreamTypography.Body,
                color = Color.White.copy(alpha = 0.85f),
                textAlign = TextAlign.Center,
                lineHeight = 24.sp
            )

            Spacer(modifier = Modifier.height(PaceDreamSpacing.XXXL))

            // Feature highlights
            Column(
                verticalArrangement = Arrangement.spacedBy(PaceDreamSpacing.MD)
            ) {
                WelcomeFeatureRow(
                    icon = PaceDreamIcons.Home,
                    text = "List any space - rooms, parking, gear & more"
                )
                WelcomeFeatureRow(
                    icon = PaceDreamIcons.CalendarToday,
                    text = "Manage bookings and availability easily"
                )
                WelcomeFeatureRow(
                    icon = PaceDreamIcons.AttachMoney,
                    text = "Earn money with secure Stripe payouts"
                )
            }

            Spacer(modifier = Modifier.height(PaceDreamSpacing.XXXL))

            // Action Buttons
            Button(
                onClick = onStartHosting,
                colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                shape = RoundedCornerShape(PaceDreamRadius.MD),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(PaceDreamButtonHeight.LG)
            ) {
                Text(
                    text = "Start Hosting",
                    style = PaceDreamTypography.Button,
                    color = PaceDreamColors.Primary
                )
            }

            Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))

            OutlinedButton(
                onClick = onContinueAsGuest,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                border = ButtonDefaults.outlinedButtonBorder.copy(
                    brush = androidx.compose.ui.graphics.SolidColor(Color.White.copy(alpha = 0.6f))
                ),
                shape = RoundedCornerShape(PaceDreamRadius.MD),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(PaceDreamButtonHeight.LG)
            ) {
                Text(
                    text = "Continue as Guest",
                    style = PaceDreamTypography.Button,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
private fun WelcomeFeatureRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(PaceDreamRadius.MD))
            .background(Color.White.copy(alpha = 0.1f))
            .padding(horizontal = PaceDreamSpacing.MD, vertical = PaceDreamSpacing.SM)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.9f),
            modifier = Modifier.size(PaceDreamIconSize.MD)
        )
        Spacer(modifier = Modifier.width(PaceDreamSpacing.MD))
        Text(
            text = text,
            style = PaceDreamTypography.Callout,
            color = Color.White.copy(alpha = 0.9f)
        )
    }
}

@Composable
fun HostModeToggleScreen(
    isHostMode: Boolean,
    onToggleHostMode: (Boolean) -> Unit,
    onContinue: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PaceDreamColors.Background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = PaceDreamSpacing.XL)
                .padding(vertical = PaceDreamSpacing.XXXL),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Choose Your Mode",
                style = PaceDreamTypography.Title1,
                color = PaceDreamColors.TextPrimary,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))

            Text(
                text = "Switch between guest and host modes to access different features",
                style = PaceDreamTypography.Body,
                color = PaceDreamColors.TextSecondary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(PaceDreamSpacing.XXXL))

            // Mode cards
            Column(
                verticalArrangement = Arrangement.spacedBy(PaceDreamSpacing.MD)
            ) {
                ModeSelectionCard(
                    icon = PaceDreamIcons.Person,
                    title = "Guest Mode",
                    subtitle = "Book amazing stays and experiences",
                    isSelected = !isHostMode,
                    accentColor = PaceDreamColors.Info,
                    onClick = { onToggleHostMode(false) }
                )

                ModeSelectionCard(
                    icon = PaceDreamIcons.Home,
                    title = "Host Mode",
                    subtitle = "Manage your properties and earnings",
                    isSelected = isHostMode,
                    accentColor = PaceDreamColors.Primary,
                    onClick = { onToggleHostMode(true) }
                )
            }

            Spacer(modifier = Modifier.height(PaceDreamSpacing.XXXL))

            Button(
                onClick = onContinue,
                colors = ButtonDefaults.buttonColors(containerColor = PaceDreamColors.Primary),
                shape = RoundedCornerShape(PaceDreamRadius.MD),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(PaceDreamButtonHeight.LG)
            ) {
                Text(
                    text = "Continue",
                    style = PaceDreamTypography.Button,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
private fun ModeSelectionCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    isSelected: Boolean,
    accentColor: Color,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(PaceDreamRadius.LG),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) accentColor.copy(alpha = 0.08f) else PaceDreamColors.Card
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) PaceDreamElevation.SM else PaceDreamElevation.None),
        border = if (isSelected) {
            androidx.compose.foundation.BorderStroke(2.dp, accentColor)
        } else {
            androidx.compose.foundation.BorderStroke(1.dp, PaceDreamColors.Border)
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(PaceDreamSpacing.LG),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = if (isSelected) 0.15f else 0.08f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = accentColor,
                    modifier = Modifier.size(PaceDreamIconSize.MD)
                )
            }

            Spacer(modifier = Modifier.width(PaceDreamSpacing.MD))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = PaceDreamTypography.Headline,
                    color = if (isSelected) accentColor else PaceDreamColors.TextPrimary,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = subtitle,
                    style = PaceDreamTypography.Caption,
                    color = PaceDreamColors.TextSecondary
                )
            }

            RadioButton(
                selected = isSelected,
                onClick = onClick,
                colors = RadioButtonDefaults.colors(
                    selectedColor = accentColor,
                    unselectedColor = PaceDreamColors.TextSecondary
                )
            )
        }
    }
}
