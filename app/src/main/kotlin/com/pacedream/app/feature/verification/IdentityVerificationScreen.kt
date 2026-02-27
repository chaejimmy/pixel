package com.pacedream.app.feature.verification

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import com.pacedream.common.icon.PaceDreamIcons
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import android.graphics.BitmapFactory
import android.net.Uri
import coil.compose.AsyncImage

/**
 * IdentityVerificationScreen - Main screen for ID verification
 * Shows phone and ID verification status and allows users to complete verification
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IdentityVerificationScreen(
    viewModel: IdentityVerificationViewModel = hiltViewModel(),
    onBackClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    LaunchedEffect(Unit) {
        viewModel.loadStatus()
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Identity Verification") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = PaceDreamIcons.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Overall Verification Level Badge
            item {
                LevelBadge(level = uiState.verificationLevel)
            }
            
            // Phone Verification Section
            item {
                VerificationSection(
                    title = "Phone Verification",
                    status = if (uiState.isPhoneVerified) "verified" else null,
                    isVerified = uiState.isPhoneVerified
                ) {
                    if (!uiState.isPhoneVerified) {
                        PhoneVerificationContent(
                            phoneNumber = uiState.phoneNumber,
                            onVerified = { viewModel.loadStatus() }
                        )
                    } else {
                        VerifiedStatusCard(
                            text = "Phone number verified",
                            phoneNumber = uiState.phoneNumber
                        )
                    }
                }
            }
            
            // ID Verification Section
            item {
                VerificationSection(
                    title = "ID Verification",
                    status = uiState.idStatus?.status,
                    isVerified = uiState.isIDVerified
                ) {
                    IDVerificationContent(
                        status = uiState.idStatus,
                        onSubmitted = { viewModel.loadStatus() }
                    )
                }
            }
        }
    }
}

@Composable
fun LevelBadge(level: Int) {
    val (text, color, bgColor) = when (level) {
        3 -> Triple("Fully Verified", MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.primaryContainer)
        2 -> Triple("ID Verified", MaterialTheme.colorScheme.secondary, MaterialTheme.colorScheme.secondaryContainer)
        1 -> Triple("Phone Verified", MaterialTheme.colorScheme.tertiary, MaterialTheme.colorScheme.tertiaryContainer)
        else -> Triple("Not Verified", MaterialTheme.colorScheme.onSurfaceVariant, MaterialTheme.colorScheme.surfaceVariant)
    }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = bgColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                PaceDreamIcons.VerifiedUser,
                contentDescription = null,
                tint = color
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium,
                color = color,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun VerificationSection(
    title: String,
    status: String?,
    isVerified: Boolean,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                
                StatusBadge(
                    text = when {
                        isVerified -> "Verified"
                        status == "submitted" -> "Pending"
                        else -> "Not Verified"
                    },
                    color = when {
                        isVerified -> MaterialTheme.colorScheme.primary
                        status == "submitted" -> MaterialTheme.colorScheme.tertiary
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
            
            content()
        }
    }
}

@Composable
fun StatusBadge(text: String, color: androidx.compose.ui.graphics.Color) {
    Surface(
        color = color.copy(alpha = 0.2f),
        shape = MaterialTheme.shapes.small
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun VerifiedStatusCard(text: String, phoneNumber: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                PaceDreamIcons.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                if (phoneNumber.isNotEmpty()) {
                    Text(
                        text = phoneNumber,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
