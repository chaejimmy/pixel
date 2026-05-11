@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.shourov.apps.pacedream.feature.wanted.presentation

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.pacedream.common.composables.theme.PaceDreamColors
import com.pacedream.common.composables.theme.PaceDreamRadius
import com.pacedream.common.composables.theme.PaceDreamSpacing
import com.pacedream.common.composables.theme.PaceDreamTypography
import com.pacedream.common.icon.PaceDreamIcons
import com.shourov.apps.pacedream.feature.wanted.model.WantedCategoriesByType
import com.shourov.apps.pacedream.feature.wanted.model.WantedCategoryOption
import com.shourov.apps.pacedream.feature.wanted.model.WantedType

/**
 * "Post a Request" form (web parity for `POST /v1/requests`).
 *
 * The web product uses a 3-step wizard. On a phone the steps don't add much
 * value — the form fits in a single scroll — so we collapse to a single
 * screen with a strong "Type" picker at the top and a sticky submit CTA.
 *
 * Loading / error / success states are explicit; backend errors are mapped
 * to friendly copy in [CreateRequestViewModel].
 */
@Composable
fun CreateRequestScreen(
    onBack: () -> Unit,
    onCreated: (String) -> Unit,
    viewModel: CreateRequestViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(state.createdId) {
        state.createdId?.let { onCreated(it) }
    }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
    ) { uri: Uri? ->
        val value = uri?.toString()
        viewModel.update { it.copy(imageUrl = value) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Post a Request",
                        style = PaceDreamTypography.Headline,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = PaceDreamColors.Background,
                ),
            )
        },
        containerColor = PaceDreamColors.Background,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding(),
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = PaceDreamSpacing.MD)
                    .padding(top = PaceDreamSpacing.SM, bottom = PaceDreamSpacing.LG),
                verticalArrangement = Arrangement.spacedBy(PaceDreamSpacing.SM2),
            ) {
                Text(
                    text = "Tell providers what you need and they'll send offers.",
                    style = PaceDreamTypography.Footnote,
                    color = PaceDreamColors.TextSecondary,
                )

                SectionLabel("Type")
                TypePicker(
                    selected = state.form.type,
                    onSelect = viewModel::selectType,
                )

                SectionLabel("Category")
                CategoryDropdown(
                    type = state.form.type,
                    selectedKey = state.form.category,
                    onSelect = { opt -> viewModel.update { it.copy(category = opt.key) } },
                )

                SectionLabel("Title")
                OutlinedTextField(
                    value = state.form.title,
                    onValueChange = { v -> viewModel.update { it.copy(title = v) } },
                    placeholder = { Text("e.g. Need a covered parking spot in SF") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction = ImeAction.Next,
                    ),
                    colors = pdTextFieldColors(),
                    shape = RoundedCornerShape(PaceDreamRadius.MD),
                    modifier = Modifier.fillMaxWidth(),
                )

                SectionLabel("Description")
                OutlinedTextField(
                    value = state.form.description,
                    onValueChange = { v -> viewModel.update { it.copy(description = v) } },
                    placeholder = {
                        Text("Anything providers should know — dates, condition, must-haves.")
                    },
                    minLines = 3,
                    maxLines = 6,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                    ),
                    colors = pdTextFieldColors(),
                    shape = RoundedCornerShape(PaceDreamRadius.MD),
                    modifier = Modifier.fillMaxWidth(),
                )

                SectionLabel("Where? (optional)")
                OutlinedTextField(
                    value = state.form.locationCity,
                    onValueChange = { v -> viewModel.update { it.copy(locationCity = v) } },
                    placeholder = { Text("City") },
                    singleLine = true,
                    colors = pdTextFieldColors(),
                    shape = RoundedCornerShape(PaceDreamRadius.MD),
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(PaceDreamSpacing.SM)) {
                    OutlinedTextField(
                        value = state.form.locationState,
                        onValueChange = { v -> viewModel.update { it.copy(locationState = v) } },
                        placeholder = { Text("State / Region") },
                        singleLine = true,
                        colors = pdTextFieldColors(),
                        shape = RoundedCornerShape(PaceDreamRadius.MD),
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = state.form.locationCountry,
                        onValueChange = { v -> viewModel.update { it.copy(locationCountry = v) } },
                        placeholder = { Text("Country") },
                        singleLine = true,
                        colors = pdTextFieldColors(),
                        shape = RoundedCornerShape(PaceDreamRadius.MD),
                        modifier = Modifier.weight(1f),
                    )
                }

                SectionLabel("When? (optional)")
                OutlinedTextField(
                    value = state.form.date,
                    onValueChange = { v -> viewModel.update { it.copy(date = v) } },
                    placeholder = { Text("e.g. 2026-07-01 or Sat 10:00 AM") },
                    singleLine = true,
                    colors = pdTextFieldColors(),
                    shape = RoundedCornerShape(PaceDreamRadius.MD),
                    modifier = Modifier.fillMaxWidth(),
                )

                SectionLabel("Budget (optional)")
                OutlinedTextField(
                    value = state.form.budget,
                    onValueChange = { v ->
                        viewModel.update {
                            it.copy(budget = v.filter { c -> c.isDigit() || c == '.' || c == ',' })
                        }
                    },
                    placeholder = { Text("Leave blank for negotiable") },
                    prefix = { Text("$") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    colors = pdTextFieldColors(),
                    shape = RoundedCornerShape(PaceDreamRadius.MD),
                    modifier = Modifier.fillMaxWidth(),
                )

                SectionLabel("Cover image (optional)")
                ImagePickerRow(
                    imageUri = state.form.imageUrl,
                    onPick = { imagePicker.launch("image/*") },
                    onClear = { viewModel.update { it.copy(imageUrl = null) } },
                )

                state.error?.let { msg ->
                    InlineErrorBanner(msg)
                }
            }

            // Sticky bottom CTA so users on small screens can always reach
            // the submit button without scrolling past every field first.
            HorizontalDivider(color = PaceDreamColors.Border, thickness = 0.5.dp)
            Button(
                onClick = viewModel::submit,
                enabled = !state.submitting,
                shape = RoundedCornerShape(PaceDreamRadius.MD),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PaceDreamColors.Primary,
                    contentColor = PaceDreamColors.OnPrimary,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = PaceDreamSpacing.MD,
                        vertical = PaceDreamSpacing.SM,
                    ),
            ) {
                if (state.submitting) {
                    CircularProgressIndicator(
                        strokeWidth = 2.dp,
                        color = PaceDreamColors.OnPrimary,
                        modifier = Modifier.size(18.dp),
                    )
                } else {
                    Text(
                        text = "Post Request",
                        style = PaceDreamTypography.Headline,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}

// ============================================================================
// Type picker
// ============================================================================

@Composable
private fun TypePicker(
    selected: WantedType,
    onSelect: (WantedType) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(PaceDreamSpacing.SM)) {
        WantedType.entries.forEach { type ->
            val isSelected = selected == type
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(PaceDreamRadius.MD))
                    .clickable { onSelect(type) }
                    .background(
                        if (isSelected) PaceDreamColors.Primary.copy(alpha = 0.10f)
                        else PaceDreamColors.Surface
                    )
                    .padding(PaceDreamSpacing.SM2),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(
                            color = if (isSelected) PaceDreamColors.Primary.copy(alpha = 0.20f)
                            else PaceDreamColors.TextSecondary.copy(alpha = 0.15f),
                            shape = CircleShape,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = when (type) {
                            WantedType.Space -> PaceDreamIcons.Home
                            WantedType.Item -> PaceDreamIcons.Bookmark
                            WantedType.Service -> PaceDreamIcons.Help
                        },
                        contentDescription = null,
                        tint = if (isSelected) PaceDreamColors.Primary else PaceDreamColors.TextSecondary,
                        modifier = Modifier.size(16.dp),
                    )
                }
                Spacer(Modifier.width(PaceDreamSpacing.SM2))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = type.label,
                        style = PaceDreamTypography.Headline,
                        color = PaceDreamColors.TextPrimary,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = type.subtitle,
                        style = PaceDreamTypography.Caption,
                        color = PaceDreamColors.TextSecondary,
                    )
                }
                if (isSelected) {
                    Icon(
                        imageVector = PaceDreamIcons.CheckCircle,
                        contentDescription = "Selected",
                        tint = PaceDreamColors.Primary,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }
    }
}

// ============================================================================
// Category dropdown
// ============================================================================

@Composable
private fun CategoryDropdown(
    type: WantedType,
    selectedKey: String,
    onSelect: (WantedCategoryOption) -> Unit,
) {
    val options = WantedCategoriesByType[type].orEmpty()
    val selectedLabel = options.firstOrNull { it.key == selectedKey }?.label
        ?: options.firstOrNull()?.label
        ?: "Other"

    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = Modifier.fillMaxWidth(),
    ) {
        OutlinedTextField(
            value = selectedLabel,
            onValueChange = {},
            readOnly = true,
            singleLine = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            colors = pdTextFieldColors(),
            shape = RoundedCornerShape(PaceDreamRadius.MD),
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth(),
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(PaceDreamColors.Background),
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = option.label,
                            style = PaceDreamTypography.Body,
                            color = PaceDreamColors.TextPrimary,
                        )
                    },
                    onClick = {
                        onSelect(option)
                        expanded = false
                    },
                )
            }
        }
    }
}

// ============================================================================
// Image picker
// ============================================================================

@Composable
private fun ImagePickerRow(
    imageUri: String?,
    onPick: () -> Unit,
    onClear: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(PaceDreamSpacing.SM2),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .height(72.dp)
                .weight(1f)
                .clip(RoundedCornerShape(PaceDreamRadius.MD))
                .background(PaceDreamColors.Surface),
            contentAlignment = Alignment.Center,
        ) {
            if (imageUri != null) {
                AsyncImage(
                    model = imageUri,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Icon(
                    imageVector = Icons.Filled.Image,
                    contentDescription = null,
                    tint = PaceDreamColors.TextSecondary,
                )
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(PaceDreamSpacing.XS)) {
            OutlinedButton(
                onClick = onPick,
                shape = RoundedCornerShape(PaceDreamRadius.MD),
            ) {
                Text(if (imageUri == null) "Add image" else "Replace")
            }
            if (imageUri != null) {
                OutlinedButton(
                    onClick = onClear,
                    shape = RoundedCornerShape(PaceDreamRadius.MD),
                ) {
                    Text("Remove")
                }
            }
        }
    }
}

// ============================================================================
// Shared building blocks
// ============================================================================

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = PaceDreamTypography.Caption,
        color = PaceDreamColors.TextSecondary,
        fontWeight = FontWeight.SemiBold,
    )
}

@Composable
private fun InlineErrorBanner(message: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                PaceDreamColors.Warning.copy(alpha = 0.12f),
                shape = RoundedCornerShape(PaceDreamRadius.MD),
            )
            .padding(PaceDreamSpacing.SM2),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(
            imageVector = PaceDreamIcons.Warning,
            contentDescription = null,
            tint = PaceDreamColors.Warning,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(PaceDreamSpacing.SM))
        Text(
            text = message,
            style = PaceDreamTypography.Footnote,
            color = PaceDreamColors.TextPrimary,
        )
    }
}

@Composable
private fun pdTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = PaceDreamColors.Primary,
    unfocusedBorderColor = PaceDreamColors.Border,
    focusedTextColor = PaceDreamColors.TextPrimary,
    unfocusedTextColor = PaceDreamColors.TextPrimary,
    focusedPlaceholderColor = PaceDreamColors.TextSecondary,
    unfocusedPlaceholderColor = PaceDreamColors.TextSecondary,
    cursorColor = PaceDreamColors.Primary,
)
