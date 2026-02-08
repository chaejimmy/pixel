package com.shourov.apps.pacedream.feature.host.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.pacedream.common.composables.components.*
import com.pacedream.common.composables.theme.*
import com.shourov.apps.pacedream.feature.host.domain.HostModeManager
import com.shourov.apps.pacedream.feature.host.navigation.HostNavigationGraph
import com.shourov.apps.pacedream.feature.host.presentation.components.HostBottomNavigation
import com.shourov.apps.pacedream.feature.host.presentation.components.HostModeBanner

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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Host Mode Banner
            HostModeBanner(
                isHostMode = true,
                onSwitchToGuest = onSwitchToGuestMode,
                onSwitchToHost = { /* Already in host mode */ },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(PaceDreamSpacing.MD)
            )
            
            // Host Navigation
            NavHost(
                navController = navController,
                startDestination = "host_dashboard",
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 80.dp) // Account for banner
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
                        PaceDreamColors.Primary.copy(alpha = 0.9f)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(PaceDreamSpacing.XXXL),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Host Icon
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Home,
                    contentDescription = "Host",
                    tint = Color.White,
                    modifier = Modifier.size(60.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(PaceDreamSpacing.XXXL))
            
            Text(
                text = "Welcome to Host Mode",
                style = PaceDreamTypography.Title1,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(PaceDreamSpacing.LG))
            
            Text(
                text = "Turn your space into income. List your property, manage bookings, and start earning with PaceDream.",
                style = PaceDreamTypography.Body,
                color = Color.White.copy(alpha = 0.9f),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(PaceDreamSpacing.XXXL))
            
            // Action Buttons
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(PaceDreamSpacing.MD)
            ) {
                Button(
                    onClick = onStartHosting,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Start Hosting",
                        style = PaceDreamTypography.Headline,
                        color = PaceDreamColors.Primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                
                OutlinedButton(
                    onClick = onContinueAsGuest,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Continue as Guest",
                        style = PaceDreamTypography.Headline,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
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
                .padding(PaceDreamSpacing.XXXL),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Choose Your Mode",
                style = PaceDreamTypography.Title1,
                color = PaceDreamColors.TextPrimary,
                fontWeight = FontWeight.Bold,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(PaceDreamSpacing.LG))
            
            Text(
                text = "Switch between guest and host modes to access different features",
                style = PaceDreamTypography.Body,
                color = PaceDreamColors.TextSecondary,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(PaceDreamSpacing.XXXL))
            
            // Mode Toggle Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(PaceDreamRadius.LG)),
                colors = CardDefaults.cardColors(containerColor = PaceDreamColors.Card),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(
                    modifier = Modifier.padding(PaceDreamSpacing.LG)
                ) {
                    // Guest Mode
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(PaceDreamSpacing.MD),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "Guest Mode",
                                tint = if (!isHostMode) PaceDreamColors.Primary else PaceDreamColors.TextSecondary,
                                modifier = Modifier.size(24.dp)
                            )
                            
                            Spacer(modifier = Modifier.width(PaceDreamSpacing.MD))
                            
                            Column {
                                Text(
                                    text = "Guest Mode",
                                    style = PaceDreamTypography.Headline,
                                    color = if (!isHostMode) PaceDreamColors.Primary else PaceDreamColors.TextPrimary,
                                    fontWeight = FontWeight.SemiBold
                                )
                                
                                Text(
                                    text = "Book amazing stays",
                                    style = PaceDreamTypography.Caption,
                                    color = PaceDreamColors.TextSecondary
                                )
                            }
                        }
                        
                        RadioButton(
                            selected = !isHostMode,
                            onClick = { onToggleHostMode(false) },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = PaceDreamColors.Primary
                            )
                        )
                    }
                    
                    Divider(color = PaceDreamColors.Border, thickness = 1.dp)
                    
                    // Host Mode
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(PaceDreamSpacing.MD),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Home,
                                contentDescription = "Host Mode",
                                tint = if (isHostMode) PaceDreamColors.Primary else PaceDreamColors.TextSecondary,
                                modifier = Modifier.size(24.dp)
                            )
                            
                            Spacer(modifier = Modifier.width(PaceDreamSpacing.MD))
                            
                            Column {
                                Text(
                                    text = "Host Mode",
                                    style = PaceDreamTypography.Headline,
                                    color = if (isHostMode) PaceDreamColors.Primary else PaceDreamColors.TextPrimary,
                                    fontWeight = FontWeight.SemiBold
                                )
                                
                                Text(
                                    text = "Manage your properties",
                                    style = PaceDreamTypography.Caption,
                                    color = PaceDreamColors.TextSecondary
                                )
                            }
                        }
                        
                        RadioButton(
                            selected = isHostMode,
                            onClick = { onToggleHostMode(true) },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = PaceDreamColors.Primary
                            )
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(PaceDreamSpacing.XXXL))
            
            Button(
                onClick = onContinue,
                colors = ButtonDefaults.buttonColors(containerColor = PaceDreamColors.Primary),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Continue",
                    style = PaceDreamTypography.Headline,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
