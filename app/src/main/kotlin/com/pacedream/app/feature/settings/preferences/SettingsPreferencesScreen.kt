package com.pacedream.app.feature.settings.preferences

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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
fun SettingsPreferencesScreen(
    onBackClick: () -> Unit,
    viewModel: SettingsPreferencesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Supported options - these are UI-level constants for the dropdown menus.
    // The selected value is saved/loaded from the backend via SettingsPreferencesViewModel.
    // TODO: Fetch available options from the backend if a /preferences/options endpoint is added.
    val languageOptions = listOf("English", "Spanish", "French")
    val currencyOptions = listOf("USD", "EUR", "GBP")
    val timezoneOptions = listOf("UTC", "America/Los_Angeles", "Europe/London")

    var languageExpanded by remember { mutableStateOf(false) }
    var currencyExpanded by remember { mutableStateOf(false) }
    var timezoneExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.errorMessage, uiState.successMessage) {
        val message = uiState.errorMessage ?: uiState.successMessage
        if (!message.isNullOrBlank()) {
            scope.launch { snackbarHostState.showSnackbar(message) }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Preferences",
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
        if (uiState.isLoading && uiState.language.isEmpty()) {
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
                // Language & Region section
                SectionLabel("Language & Region")
                Card(
                    shape = RoundedCornerShape(PaceDreamRadius.LG),
                    colors = CardDefaults.cardColors(containerColor = PaceDreamColors.Card),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(PaceDreamSpacing.MD),
                        verticalArrangement = Arrangement.spacedBy(PaceDreamSpacing.SM)
                    ) {
                        StyledDropdown(
                            value = uiState.language,
                            label = "Language",
                            options = languageOptions,
                            expanded = languageExpanded,
                            onExpandedChange = { languageExpanded = it },
                            onOptionSelected = {
                                viewModel.onLanguageChange(it)
                                languageExpanded = false
                            }
                        )

                        StyledDropdown(
                            value = uiState.currency,
                            label = "Currency",
                            options = currencyOptions,
                            expanded = currencyExpanded,
                            onExpandedChange = { currencyExpanded = it },
                            onOptionSelected = {
                                viewModel.onCurrencyChange(it)
                                currencyExpanded = false
                            }
                        )

                        StyledDropdown(
                            value = uiState.timezone,
                            label = "Timezone",
                            options = timezoneOptions,
                            expanded = timezoneExpanded,
                            onExpandedChange = { timezoneExpanded = it },
                            onOptionSelected = {
                                viewModel.onTimezoneChange(it)
                                timezoneExpanded = false
                            }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StyledDropdown(
    value: String,
    label: String,
    options: List<String>,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onOptionSelected: (String) -> Unit
) {
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { onExpandedChange(!expanded) }
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            label = {
                Text(
                    label,
                    style = PaceDreamTypography.Caption,
                    color = PaceDreamColors.TextSecondary
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),
            shape = RoundedCornerShape(PaceDreamRadius.MD),
            textStyle = PaceDreamTypography.Body,
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = PaceDreamColors.Primary,
                unfocusedBorderColor = PaceDreamColors.Border,
                focusedLabelColor = PaceDreamColors.Primary,
                cursorColor = PaceDreamColors.Primary
            )
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { onExpandedChange(false) }
        ) {
            for (option in options) {
                DropdownMenuItem(
                    text = {
                        Text(
                            option,
                            style = PaceDreamTypography.Body,
                            color = if (option == value) PaceDreamColors.Primary else PaceDreamColors.TextPrimary
                        )
                    },
                    onClick = { onOptionSelected(option) }
                )
            }
        }
    }
}
