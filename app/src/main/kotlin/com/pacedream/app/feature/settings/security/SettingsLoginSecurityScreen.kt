package com.pacedream.app.feature.settings.security

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import com.pacedream.common.icon.PaceDreamIcons
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pacedream.common.composables.theme.PaceDreamColors
import com.pacedream.common.composables.theme.PaceDreamRadius
import com.pacedream.common.composables.theme.PaceDreamSpacing
import com.pacedream.common.composables.theme.PaceDreamTypography
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsLoginSecurityScreen(
    onBackClick: () -> Unit,
    onAccountDeactivated: () -> Unit,
    viewModel: SettingsLoginSecurityViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val showDeactivateDialog = remember { mutableStateOf(false) }
    val showDeleteDialog = remember { mutableStateOf(false) }
    val deleteConfirmText = remember { mutableStateOf("") }

    LaunchedEffect(uiState.errorMessage, uiState.successMessage, uiState.deactivateSuccess, uiState.deleteSuccess) {
        uiState.errorMessage?.let { msg ->
            scope.launch { snackbarHostState.showSnackbar(msg) }
        }
        uiState.successMessage?.let { msg ->
            scope.launch { snackbarHostState.showSnackbar(msg) }
        }
        if (uiState.deactivateSuccess || uiState.deleteSuccess) {
            onAccountDeactivated()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Login & Security",
                        style = PaceDreamTypography.Headline
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = PaceDreamIcons.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = PaceDreamColors.Background
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = PaceDreamColors.Background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(PaceDreamSpacing.MD),
            verticalArrangement = Arrangement.spacedBy(PaceDreamSpacing.MD)
        ) {
            // Password section
            SectionLabel("Password")
            Card(
                shape = RoundedCornerShape(PaceDreamRadius.LG),
                colors = CardDefaults.cardColors(containerColor = PaceDreamColors.Card),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier.padding(PaceDreamSpacing.MD),
                    verticalArrangement = Arrangement.spacedBy(PaceDreamSpacing.SM)
                ) {
                    PasswordField(
                        value = uiState.currentPassword,
                        onValueChange = viewModel::onCurrentPasswordChange,
                        label = "Current password"
                    )
                    PasswordField(
                        value = uiState.newPassword,
                        onValueChange = viewModel::onNewPasswordChange,
                        label = "New password"
                    )
                    PasswordField(
                        value = uiState.confirmPassword,
                        onValueChange = viewModel::onConfirmPasswordChange,
                        label = "Confirm new password"
                    )

                    Spacer(modifier = Modifier.height(PaceDreamSpacing.XS))

                    Button(
                        onClick = { viewModel.changePassword() },
                        enabled = !uiState.isLoading,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(PaceDreamRadius.MD),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PaceDreamColors.Primary
                        ),
                        contentPadding = PaddingValues(vertical = 14.dp)
                    ) {
                        if (uiState.isLoading) {
                            CircularProgressIndicator(
                                strokeWidth = 2.dp,
                                modifier = Modifier
                                    .padding(end = 8.dp)
                                    .size(18.dp),
                                color = PaceDreamColors.OnPrimary
                            )
                        }
                        Text("Update Password", style = PaceDreamTypography.Button)
                    }
                }
            }

            Spacer(modifier = Modifier.height(PaceDreamSpacing.SM))

            // Danger zone section
            SectionLabel("Danger Zone")
            Card(
                shape = RoundedCornerShape(PaceDreamRadius.LG),
                colors = CardDefaults.cardColors(containerColor = PaceDreamColors.Card),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier.padding(PaceDreamSpacing.MD),
                    verticalArrangement = Arrangement.spacedBy(PaceDreamSpacing.MD)
                ) {
                    // Deactivate
                    Column(verticalArrangement = Arrangement.spacedBy(PaceDreamSpacing.SM)) {
                        Text(
                            text = "Deactivate Account",
                            style = PaceDreamTypography.Headline,
                            color = PaceDreamColors.TextPrimary
                        )
                        Text(
                            text = "Temporarily disable your account. Your data will be preserved and you can reactivate later.",
                            style = PaceDreamTypography.Caption,
                            color = PaceDreamColors.TextSecondary
                        )
                        OutlinedButton(
                            onClick = { showDeactivateDialog.value = true },
                            enabled = !uiState.isLoading,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(PaceDreamRadius.MD),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = PaceDreamColors.Warning
                            )
                        ) {
                            Text("Deactivate Account", style = PaceDreamTypography.Button)
                        }
                    }

                    Spacer(modifier = Modifier.height(PaceDreamSpacing.XS))

                    // Delete
                    Column(verticalArrangement = Arrangement.spacedBy(PaceDreamSpacing.SM)) {
                        Text(
                            text = "Delete Account",
                            style = PaceDreamTypography.Headline,
                            color = PaceDreamColors.Error
                        )
                        Text(
                            text = "Permanently delete your account and all associated data. This action cannot be undone.",
                            style = PaceDreamTypography.Caption,
                            color = PaceDreamColors.TextSecondary
                        )
                        Button(
                            onClick = {
                                deleteConfirmText.value = ""
                                showDeleteDialog.value = true
                            },
                            enabled = !uiState.isLoading,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = PaceDreamColors.Error
                            ),
                            shape = RoundedCornerShape(PaceDreamRadius.MD),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Delete Account", style = PaceDreamTypography.Button)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(PaceDreamSpacing.XL))
        }
    }

    // Deactivate confirmation dialog
    if (showDeactivateDialog.value) {
        AlertDialog(
            onDismissRequest = { showDeactivateDialog.value = false },
            title = {
                Text(
                    "Deactivate account?",
                    style = PaceDreamTypography.Title3
                )
            },
            text = {
                Text(
                    "Are you sure you want to deactivate your account? You will be logged out and your account will be temporarily disabled.",
                    style = PaceDreamTypography.Body,
                    color = PaceDreamColors.TextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeactivateDialog.value = false
                        viewModel.deactivateAccount()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PaceDreamColors.Warning
                    ),
                    shape = RoundedCornerShape(PaceDreamRadius.MD)
                ) {
                    Text("Deactivate", style = PaceDreamTypography.Button)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeactivateDialog.value = false }) {
                    Text("Cancel", color = PaceDreamColors.TextPrimary)
                }
            },
            shape = RoundedCornerShape(PaceDreamRadius.LG),
            containerColor = PaceDreamColors.Card
        )
    }

    // Delete confirmation dialog
    if (showDeleteDialog.value) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog.value = false },
            title = {
                Text(
                    "Delete Account Permanently",
                    style = PaceDreamTypography.Title3,
                    color = PaceDreamColors.Error
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(PaceDreamSpacing.SM)) {
                    Text(
                        "This will permanently delete all your data including:",
                        style = PaceDreamTypography.Body,
                        color = PaceDreamColors.TextPrimary
                    )
                    Text(
                        "\u2022 Profile and personal information\n" +
                        "\u2022 All listings and bookings\n" +
                        "\u2022 Messages and conversations\n" +
                        "\u2022 Reviews and ratings\n" +
                        "\u2022 Wishlists and notifications",
                        style = PaceDreamTypography.Caption,
                        color = PaceDreamColors.TextSecondary
                    )
                    Text(
                        "Type DELETE to confirm:",
                        style = PaceDreamTypography.Headline,
                        color = PaceDreamColors.TextPrimary
                    )
                    OutlinedTextField(
                        value = deleteConfirmText.value,
                        onValueChange = { deleteConfirmText.value = it },
                        placeholder = {
                            Text(
                                "DELETE",
                                color = PaceDreamColors.TextTertiary
                            )
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(PaceDreamRadius.MD),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PaceDreamColors.Error,
                            unfocusedBorderColor = PaceDreamColors.Border,
                            cursorColor = PaceDreamColors.Error
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteDialog.value = false
                        viewModel.deleteAccount()
                    },
                    enabled = deleteConfirmText.value == "DELETE",
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PaceDreamColors.Error
                    ),
                    shape = RoundedCornerShape(PaceDreamRadius.MD)
                ) {
                    Text("Delete Forever", style = PaceDreamTypography.Button)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog.value = false }) {
                    Text("Cancel", color = PaceDreamColors.TextPrimary)
                }
            },
            shape = RoundedCornerShape(PaceDreamRadius.LG),
            containerColor = PaceDreamColors.Card
        )
    }
}

@Composable
private fun SectionLabel(title: String) {
    Text(
        text = title.uppercase(),
        style = PaceDreamTypography.Caption,
        color = PaceDreamColors.TextTertiary,
        modifier = Modifier.padding(start = PaceDreamSpacing.XS)
    )
}

@Composable
private fun PasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = {
            Text(
                label,
                style = PaceDreamTypography.Caption,
                color = PaceDreamColors.TextSecondary
            )
        },
        visualTransformation = PasswordVisualTransformation(),
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(PaceDreamRadius.MD),
        textStyle = PaceDreamTypography.Body,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = PaceDreamColors.Primary,
            unfocusedBorderColor = PaceDreamColors.Border,
            focusedLabelColor = PaceDreamColors.Primary,
            cursorColor = PaceDreamColors.Primary
        ),
        singleLine = true
    )
}
