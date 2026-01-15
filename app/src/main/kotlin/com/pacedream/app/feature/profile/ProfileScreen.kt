package com.pacedream.app.feature.profile

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage

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
    onHelpClick: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profile") }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp)
        ) {
            if (!uiState.isLoggedIn) {
                // Logged out state
                item {
                    LoggedOutSection(onLoginClick = onLoginClick)
                }
            } else {
                // User profile header
                item {
                    UserProfileHeader(
                        userName = uiState.userName,
                        userEmail = uiState.userEmail,
                        userAvatar = uiState.userAvatar,
                        onEditClick = onEditProfileClick
                    )
                }
                
                item { Spacer(modifier = Modifier.height(24.dp)) }
                
                // Host mode toggle
                item {
                    HostModeCard(
                        isHostMode = uiState.isHostMode,
                        onToggle = { viewModel.toggleHostMode() },
                        onHostDashboard = onHostModeClick
                    )
                }
                
                item { Spacer(modifier = Modifier.height(16.dp)) }
                
                // Identity Verification (if logged in)
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        ProfileMenuItem(
                            icon = Icons.Default.VerifiedUser,
                            title = "Identity Verification",
                            onClick = onIdentityVerificationClick
                        )
                    }
                }
                
                item { Spacer(modifier = Modifier.height(16.dp)) }
                
                // Menu items
                item {
                    ProfileMenuSection(
                        onSettingsClick = onSettingsClick,
                        onHelpClick = onHelpClick,
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
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Person,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Sign in to view your profile",
            style = MaterialTheme.typography.titleMedium
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Manage your bookings, favorites, and more",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Button(
            onClick = onLoginClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Sign In")
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
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar
        AsyncImage(
            model = userAvatar,
            contentDescription = userName,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
        )
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = userName,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            
            userEmail?.let { email ->
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = email,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        IconButton(onClick = onEditClick) {
            Icon(
                imageVector = Icons.Default.Edit,
                contentDescription = "Edit profile"
            )
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
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Host Mode",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = if (isHostMode) "You're hosting" else "Switch to hosting",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Switch(
                    checked = isHostMode,
                    onCheckedChange = { onToggle() }
                )
            }
            
            if (isHostMode) {
                Spacer(modifier = Modifier.height(12.dp))
                
                Button(
                    onClick = onHostDashboard,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Home, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Go to Host Dashboard")
                }
            }
        }
    }
}

@Composable
private fun ProfileMenuSection(
    onSettingsClick: () -> Unit,
    onHelpClick: () -> Unit,
    onLogoutClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            ProfileMenuItem(
                icon = Icons.Default.Settings,
                title = "Settings",
                onClick = onSettingsClick
            )
            
            HorizontalDivider()
            
            ProfileMenuItem(
                icon = Icons.AutoMirrored.Filled.Help,
                title = "Help & Support",
                onClick = onHelpClick
            )
            
            HorizontalDivider()
            
            ProfileMenuItem(
                icon = Icons.Default.Info,
                title = "About",
                onClick = { /* TODO */ }
            )
            
            HorizontalDivider()
            
            ProfileMenuItem(
                icon = Icons.AutoMirrored.Filled.Logout,
                title = "Sign Out",
                onClick = onLogoutClick,
                tint = MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
private fun ProfileMenuItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    onClick: () -> Unit,
    tint: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint
        )
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = tint,
            modifier = Modifier.weight(1f)
        )
        
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}


