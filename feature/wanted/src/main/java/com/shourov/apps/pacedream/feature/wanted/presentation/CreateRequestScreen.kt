@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.shourov.apps.pacedream.feature.wanted.presentation

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
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
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.pacedream.common.composables.theme.PaceDreamColors
import com.pacedream.common.composables.theme.PaceDreamRadius
import com.pacedream.common.composables.theme.PaceDreamSpacing
import com.pacedream.common.composables.theme.PaceDreamTypography
import com.pacedream.common.icon.PaceDreamIcons
import com.shourov.apps.pacedream.feature.wanted.model.CreateRequestUiState
import com.shourov.apps.pacedream.feature.wanted.model.SelectedPlace
import com.shourov.apps.pacedream.feature.wanted.model.WantedCategoryOption
import com.shourov.apps.pacedream.feature.wanted.model.WantedType
import com.shourov.apps.pacedream.feature.wanted.presentation.components.LocationPickerSheet
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

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
    var showLocationSheet by remember { mutableStateOf(false) }
    val locationSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LaunchedEffect(state.createdId) {
        state.createdId?.let { onCreated(it) }
    }

    // Android Photo Picker (PickVisualMedia) instead of the legacy
    // any-file document picker: it's the platform standard, scoped to
    // photos, and needs no READ_EXTERNAL_STORAGE / READ_MEDIA_IMAGES
    // permission — the system picker hands back a single grant.
    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri: Uri? ->
        // The picker returns a local content:// URI that's only valid on
        // this device. We hand it to the ViewModel which uploads the
        // bytes to object storage and persists the returned public URL.
        uri?.let(viewModel::uploadCoverImage)
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
                    options = state.categoriesByType[state.form.type].orEmpty(),
                    selectedKey = state.form.category,
                    onSelect = { opt -> viewModel.update { it.copy(category = opt.key) } },
                )

                SectionLabel("Title")
                OutlinedTextField(
                    value = state.form.title,
                    onValueChange = { v -> viewModel.update { it.copy(title = v) } },
                    placeholder = { Text("e.g. Need a covered parking spot in SF") },
                    singleLine = true,
                    isError = state.fieldErrors.titleError != null,
                    supportingText = {
                        FieldSupportingText(
                            error = state.fieldErrors.titleError,
                            current = state.form.title.length,
                            max = CreateRequestUiState.TITLE_MAX_LENGTH,
                        )
                    },
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
                    isError = state.fieldErrors.descriptionError != null,
                    supportingText = {
                        FieldSupportingText(
                            error = state.fieldErrors.descriptionError,
                            current = state.form.description.length,
                            max = CreateRequestUiState.DESCRIPTION_MAX_LENGTH,
                        )
                    },
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                    ),
                    colors = pdTextFieldColors(),
                    shape = RoundedCornerShape(PaceDreamRadius.MD),
                    modifier = Modifier.fillMaxWidth(),
                )

                SectionLabel("Where?")
                LocationField(
                    selected = state.form.location,
                    error = state.fieldErrors.locationError,
                    onClick = { showLocationSheet = true },
                    onClear = { viewModel.update { it.copy(location = null) } },
                )

                SectionLabel("When? (optional)")
                DateRangeField(
                    startDate = state.form.startDate,
                    endDate = state.form.endDate,
                    onChange = { start, end ->
                        viewModel.update { it.copy(startDate = start, endDate = end) }
                    },
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
                    isError = state.fieldErrors.budgetError != null,
                    supportingText = {
                        state.fieldErrors.budgetError?.let { err ->
                            Text(
                                text = err,
                                style = PaceDreamTypography.Caption,
                                color = PaceDreamColors.Error,
                            )
                        }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    colors = pdTextFieldColors(),
                    shape = RoundedCornerShape(PaceDreamRadius.MD),
                    modifier = Modifier.fillMaxWidth(),
                )

                SectionLabel("Cover image (optional)")
                ImagePickerRow(
                    imageUri = state.form.imageUrl,
                    uploading = state.uploading,
                    onPick = {
                        imagePicker.launch(
                            PickVisualMediaRequest(
                                ActivityResultContracts.PickVisualMedia.ImageOnly,
                            ),
                        )
                    },
                    onClear = viewModel::clearCoverImage,
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
                enabled = !state.submitting &&
                    !state.uploading &&
                    state.fieldErrors.isEmpty() &&
                    state.requiredFieldsPresent,
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

    if (showLocationSheet) {
        LocationPickerSheet(
            sheetState = locationSheetState,
            onDismiss = { showLocationSheet = false },
            onPlaceSelected = { place ->
                viewModel.update { it.copy(location = place) }
                showLocationSheet = false
            },
        )
    }
}

// ============================================================================
// Location field — collapses three legacy text fields into a single tap target
// that opens the autocomplete sheet. The read-only label shows "City, Region,
// Country" so the user can verify the pick at a glance.
// ============================================================================

@Composable
private fun LocationField(
    selected: SelectedPlace?,
    error: String?,
    onClick: () -> Unit,
    onClear: () -> Unit,
) {
    val shape = RoundedCornerShape(PaceDreamRadius.MD)
    val borderModifier = if (error != null) {
        Modifier.border(1.dp, PaceDreamColors.Error, shape)
    } else {
        Modifier
    }
    Column(verticalArrangement = Arrangement.spacedBy(PaceDreamSpacing.XS)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(PaceDreamColors.Surface)
                .then(borderModifier)
                .clickable(onClick = onClick)
                .padding(
                    horizontal = PaceDreamSpacing.SM2,
                    vertical = PaceDreamSpacing.SM2,
                ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = PaceDreamIcons.LocationOn,
                contentDescription = null,
                tint = when {
                    error != null -> PaceDreamColors.Error
                    selected != null -> PaceDreamColors.Primary
                    else -> PaceDreamColors.TextSecondary
                },
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(PaceDreamSpacing.SM))
            Text(
                text = selected?.displayLine?.takeIf { it.isNotBlank() }
                    ?: "Add a location",
                style = PaceDreamTypography.Body,
                color = if (selected != null) PaceDreamColors.TextPrimary
                else PaceDreamColors.TextSecondary,
                modifier = Modifier.weight(1f),
            )
            if (selected != null) {
                TextButton(onClick = onClear) {
                    Text("Clear", color = PaceDreamColors.Primary)
                }
            }
        }
        if (error != null) {
            Text(
                text = error,
                style = PaceDreamTypography.Caption,
                color = PaceDreamColors.Error,
            )
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
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(PaceDreamSpacing.SM),
        ) {
            WantedType.entries.forEach { type ->
                TypeTile(
                    type = type,
                    selected = selected == type,
                    onClick = { onSelect(type) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
        Text(
            text = selected.subtitle,
            style = PaceDreamTypography.Caption,
            color = PaceDreamColors.TextSecondary,
        )
    }
}

@Composable
private fun TypeTile(
    type: WantedType,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(PaceDreamRadius.MD)
    val borderModifier = if (selected) {
        Modifier.border(1.5.dp, PaceDreamColors.Primary, shape)
    } else {
        Modifier
    }
    Box(
        modifier = modifier
            .height(72.dp)
            .clip(shape)
            .background(
                if (selected) PaceDreamColors.Primary.copy(alpha = 0.10f)
                else PaceDreamColors.Surface
            )
            .then(borderModifier)
            .selectable(
                selected = selected,
                role = Role.RadioButton,
                onClick = onClick,
            )
            .semantics { contentDescription = type.label }
            .padding(PaceDreamSpacing.XS),
    ) {
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(PaceDreamSpacing.XS),
        ) {
            Icon(
                imageVector = when (type) {
                    WantedType.Space -> PaceDreamIcons.Home
                    WantedType.Item -> PaceDreamIcons.Bookmark
                    WantedType.Service -> PaceDreamIcons.Help
                },
                contentDescription = null,
                tint = if (selected) PaceDreamColors.Primary else PaceDreamColors.TextSecondary,
                modifier = Modifier.size(24.dp),
            )
            Text(
                text = type.label,
                style = PaceDreamTypography.Headline,
                color = if (selected) PaceDreamColors.Primary else PaceDreamColors.TextPrimary,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (selected) {
            Icon(
                imageVector = PaceDreamIcons.CheckCircle,
                contentDescription = null,
                tint = PaceDreamColors.Primary,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(12.dp),
            )
        }
    }
}

// ============================================================================
// Category dropdown
// ============================================================================

@Composable
private fun CategoryDropdown(
    options: List<WantedCategoryOption>,
    selectedKey: String,
    onSelect: (WantedCategoryOption) -> Unit,
) {
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
// Date range picker
//
// `startDate` / `endDate` are stored on the form as UTC-midnight epoch
// millis so they round-trip through `rememberDateRangePickerState` without
// timezone drift. Wire serialization happens in the ViewModel — this
// composable only handles selection + display.
// ============================================================================

@Composable
private fun DateRangeField(
    startDate: Long?,
    endDate: Long?,
    onChange: (Long?, Long?) -> Unit,
) {
    var dialogOpen by remember { mutableStateOf(false) }
    val locale = Locale.getDefault()
    val label = remember(startDate, endDate, locale) {
        formatDateRangeLabel(startDate, endDate, locale)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(PaceDreamRadius.MD))
            .background(PaceDreamColors.Surface)
            .clickable { dialogOpen = true }
            .padding(
                horizontal = PaceDreamSpacing.SM2,
                vertical = PaceDreamSpacing.SM2,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label ?: "Any date",
            style = PaceDreamTypography.Body,
            color = if (label == null) PaceDreamColors.TextSecondary
            else PaceDreamColors.TextPrimary,
            modifier = Modifier.weight(1f),
        )
        if (startDate != null) {
            TextButton(
                onClick = { onChange(null, null) },
            ) {
                Text("Clear", color = PaceDreamColors.Primary)
            }
        }
    }

    if (dialogOpen) {
        val todayUtcMillis = remember {
            LocalDate.now(ZoneOffset.UTC).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        }
        val pickerState = rememberDateRangePickerState(
            initialSelectedStartDateMillis = startDate,
            initialSelectedEndDateMillis = endDate,
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean =
                    utcTimeMillis >= todayUtcMillis
            },
        )
        DatePickerDialog(
            onDismissRequest = { dialogOpen = false },
            confirmButton = {
                TextButton(
                    enabled = pickerState.selectedStartDateMillis != null,
                    onClick = {
                        onChange(
                            pickerState.selectedStartDateMillis,
                            pickerState.selectedEndDateMillis,
                        )
                        dialogOpen = false
                    },
                ) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { dialogOpen = false }) { Text("Cancel") }
            },
            colors = DatePickerDefaults.colors(),
        ) {
            DateRangePicker(
                state = pickerState,
                showModeToggle = false,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

private fun formatDateRangeLabel(
    startMillis: Long?,
    endMillis: Long?,
    locale: Locale,
): String? {
    if (startMillis == null) return null
    val formatter = DateTimeFormatter
        .ofLocalizedDate(FormatStyle.MEDIUM)
        .withLocale(locale)
    val startLabel = formatter.format(
        Instant.ofEpochMilli(startMillis).atZone(ZoneOffset.UTC).toLocalDate()
    )
    val endLabel = endMillis?.takeIf { it != startMillis }?.let {
        formatter.format(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate())
    }
    return if (endLabel != null) "$startLabel — $endLabel" else startLabel
}

// ============================================================================
// Image picker
// ============================================================================

@Composable
private fun ImagePickerRow(
    imageUri: String?,
    uploading: Boolean,
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
            when {
                uploading -> {
                    CircularProgressIndicator(
                        strokeWidth = 2.dp,
                        color = PaceDreamColors.Primary,
                        modifier = Modifier.size(20.dp),
                    )
                }
                imageUri != null -> {
                    // 72dp decorative thumbnail — bounded by the parent Box and
                    // crossfaded per the E-03 Coil convention. Stays
                    // contentDescription = null because the labelled
                    // "Replace"/"Remove" controls beside it already name it.
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(imageUri)
                            .crossfade(true)
                            .build(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
                else -> {
                    Icon(
                        imageVector = Icons.Filled.Image,
                        contentDescription = null,
                        tint = PaceDreamColors.TextSecondary,
                    )
                }
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(PaceDreamSpacing.XS)) {
            OutlinedButton(
                onClick = onPick,
                enabled = !uploading,
                shape = RoundedCornerShape(PaceDreamRadius.MD),
            ) {
                Text(
                    when {
                        uploading -> "Uploading…"
                        imageUri == null -> "Add image"
                        else -> "Replace"
                    }
                )
            }
            if (imageUri != null && !uploading) {
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

/**
 * Renders the per-field error if present, otherwise a soft "{n}/{max}"
 * counter. Splitting into one composable keeps the call sites tidy and
 * guarantees we never show both pieces of text at once.
 */
@Composable
private fun FieldSupportingText(error: String?, current: Int, max: Int) {
    if (error != null) {
        Text(
            text = error,
            style = PaceDreamTypography.Caption,
            color = PaceDreamColors.Error,
        )
    } else {
        Text(
            text = "$current/$max",
            style = PaceDreamTypography.Caption,
            color = PaceDreamColors.TextSecondary,
        )
    }
}

@Composable
private fun InlineErrorBanner(message: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                PaceDreamColors.Error.copy(alpha = 0.12f),
                shape = RoundedCornerShape(PaceDreamRadius.MD),
            )
            .padding(PaceDreamSpacing.SM2),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(
            imageVector = PaceDreamIcons.Error,
            contentDescription = null,
            tint = PaceDreamColors.Error,
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
