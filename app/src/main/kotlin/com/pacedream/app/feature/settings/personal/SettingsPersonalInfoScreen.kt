package com.pacedream.app.feature.settings.personal

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import com.pacedream.common.icon.PaceDreamIcons
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
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
fun SettingsPersonalInfoScreen(
    onBackClick: () -> Unit,
    viewModel: SettingsPersonalInfoViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(uiState.errorMessage, uiState.successMessage) {
        val message = uiState.errorMessage ?: uiState.successMessage
        if (!message.isNullOrBlank()) {
            scope.launch {
                snackbarHostState.showSnackbar(message)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Personal Information",
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
        if (uiState.isLoading && uiState.firstName.isEmpty() && uiState.lastName.isEmpty() && uiState.email.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator(color = PaceDreamColors.Primary)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(PaceDreamSpacing.MD),
                verticalArrangement = Arrangement.spacedBy(PaceDreamSpacing.MD)
            ) {
                // Name section
                SectionLabel("Name")
                Card(
                    shape = RoundedCornerShape(PaceDreamRadius.LG),
                    colors = CardDefaults.cardColors(containerColor = PaceDreamColors.Card),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(PaceDreamSpacing.MD),
                        verticalArrangement = Arrangement.spacedBy(PaceDreamSpacing.SM)
                    ) {
                        StyledTextField(
                            value = uiState.firstName,
                            onValueChange = viewModel::onFirstNameChange,
                            label = "First name",
                            keyboardType = KeyboardType.Text
                        )
                        StyledTextField(
                            value = uiState.lastName,
                            onValueChange = viewModel::onLastNameChange,
                            label = "Last name",
                            keyboardType = KeyboardType.Text
                        )
                    }
                }

                // Contact section
                SectionLabel("Contact")
                Card(
                    shape = RoundedCornerShape(PaceDreamRadius.LG),
                    colors = CardDefaults.cardColors(containerColor = PaceDreamColors.Card),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(PaceDreamSpacing.MD),
                        verticalArrangement = Arrangement.spacedBy(PaceDreamSpacing.SM)
                    ) {
                        StyledTextField(
                            value = uiState.email,
                            onValueChange = viewModel::onEmailChange,
                            label = "Email address",
                            keyboardType = KeyboardType.Email
                        )
                        StyledTextField(
                            value = uiState.phoneNumber,
                            onValueChange = viewModel::onPhoneChange,
                            label = "Phone number",
                            keyboardType = KeyboardType.Phone
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                Button(
                    onClick = { viewModel.save() },
                    enabled = !uiState.isLoading,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(PaceDreamRadius.MD),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PaceDreamColors.Primary
                    ),
                    contentPadding = ButtonDefaults.ContentPadding.let {
                        androidx.compose.foundation.layout.PaddingValues(vertical = 14.dp)
                    }
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
                    Text("Save Changes", style = PaceDreamTypography.Button)
                }

                Spacer(modifier = Modifier.height(PaceDreamSpacing.MD))
            }
        }
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
private fun StyledTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    keyboardType: KeyboardType = KeyboardType.Text
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
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(PaceDreamRadius.MD),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
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
