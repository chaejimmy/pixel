@file:OptIn(ExperimentalMaterial3Api::class)
package com.pacedream.app.feature.verification

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import android.graphics.BitmapFactory
import android.net.Uri
import coil.compose.AsyncImage
import com.shourov.apps.pacedream.core.network.model.verification.VerificationStatusResponse

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IDVerificationContent(
    status: VerificationStatusResponse.VerificationStatusData.IDStatus?,
    onSubmitted: () -> Unit,
    viewModel: IDVerificationViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    // Image picker launchers
    val frontImagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.uploadImage(it, isFront = true) }
    }
    
    val backImagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.uploadImage(it, isFront = false) }
    }
    
    LaunchedEffect(uiState.isSubmitted) {
        if (uiState.isSubmitted) {
            onSubmitted()
        }
    }
    
    Column(
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // ID Type Picker
        IDTypePicker(
            selectedType = uiState.idType,
            onTypeSelected = { viewModel.updateIdType(it) },
            enabled = !uiState.isPending
        )
        
        // Front Image
        ImageUploadSection(
            title = "Front Image *",
            imageUri = uiState.frontImageUri,
            imageUrl = status?.frontImage?.url,
            onImageSelected = { frontImagePicker.launch("image/*") },
            isUploading = uiState.isUploadingFront,
            isPending = uiState.isPending,
            context = context
        )
        
        // Back Image
        ImageUploadSection(
            title = "Back Image *",
            imageUri = uiState.backImageUri,
            imageUrl = status?.backImage?.url,
            onImageSelected = { backImagePicker.launch("image/*") },
            isUploading = uiState.isUploadingBack,
            isPending = uiState.isPending,
            context = context
        )
        
        // Photo Tips
        PhotoTipsCard()
        
        // Rejection Reason
        status?.rejectionReason?.let { reason ->
            RejectionReasonCard(reason)
        }
        
        // Submit Button
        if (!uiState.isPending) {
            Button(
                onClick = { viewModel.submitVerification() },
                enabled = uiState.canSubmit && !uiState.isSubmitting,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (uiState.isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Submit for Review")
                }
            }
        } else {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Schedule, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Pending Review")
                }
            }
        }
        
        uiState.error?.let { error ->
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
fun IDTypePicker(
    selectedType: String,
    onTypeSelected: (String) -> Unit,
    enabled: Boolean
) {
    val idTypes = listOf(
        "Driver's License" to "DRIVER_LICENSE",
        "Passport" to "PASSPORT",
        "National ID" to "NATIONAL_ID"
    )
    
    Column {
        Text(
            text = "ID Type",
            style = MaterialTheme.typography.titleMedium
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        var expanded by remember { mutableStateOf(false) }
        
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { if (enabled) expanded = it }
        ) {
            OutlinedTextField(
                value = idTypes.find { it.second == selectedType }?.first ?: "",
                onValueChange = {},
                readOnly = true,
                enabled = enabled,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor()
            )
            
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                for ((label, value) in idTypes) {
                    DropdownMenuItem(
                        text = { Text(label) },
                        onClick = {
                            onTypeSelected(value)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun ImageUploadSection(
    title: String,
    imageUri: Uri?,
    imageUrl: String?,
    onImageSelected: () -> Unit,
    isUploading: Boolean,
    isPending: Boolean,
    context: android.content.Context
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        when {
            imageUri != null -> {
                // Show local image
                val bitmap = remember(imageUri) {
                    context.contentResolver.openInputStream(imageUri)?.use {
                        BitmapFactory.decodeStream(it)
                    }
                }
                
                bitmap?.let {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Image(
                            bitmap = it.asImageBitmap(),
                            contentDescription = title,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
                
                if (!isPending) {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = onImageSelected,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Change Image")
                    }
                }
            }
            imageUrl != null -> {
                // Load from URL
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            }
            else -> {
                ImagePickerButton(
                    text = "Upload $title",
                    onClick = onImageSelected,
                    enabled = !isUploading && !isPending
                )
                
                if (isUploading) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Uploading...")
                    }
                }
            }
        }
    }
}

@Composable
fun ImagePickerButton(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .fillMaxWidth()
            .height(150.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                Icons.Default.AddPhotoAlternate,
                contentDescription = null,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(text)
        }
    }
}

@Composable
fun PhotoTipsCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Photo Tips:",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
            )
            TipItem("Ensure all text is clearly visible")
            TipItem("Use good lighting and avoid shadows")
            TipItem("Make sure the entire ID is in the frame")
            TipItem("Avoid blurry or low-quality images")
        }
    }
}

@Composable
fun TipItem(text: String) {
    Row {
        Text("• ", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
fun RejectionReasonCard(reason: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Rejection Reason:",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onErrorContainer,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = reason,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
        }
    }
}
