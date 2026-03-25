package com.shourov.apps.pacedream.feature.host.presentation

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pacedream.common.composables.theme.PaceDreamButtonHeight
import com.pacedream.common.composables.theme.PaceDreamColors
import com.pacedream.common.composables.theme.PaceDreamElevation
import com.pacedream.common.composables.theme.PaceDreamIconSize
import com.pacedream.common.composables.theme.PaceDreamRadius
import com.pacedream.common.composables.theme.PaceDreamSpacing
import com.pacedream.common.composables.theme.PaceDreamTypography
import com.pacedream.common.icon.PaceDreamIcons
import com.shourov.apps.pacedream.feature.host.data.HostRepository
import com.shourov.apps.pacedream.model.PricingUnit
import com.shourov.apps.pacedream.model.Property
import com.shourov.apps.pacedream.model.PropertyLocation
import com.shourov.apps.pacedream.model.PropertyPricing
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

// ── UI State ────────────────────────────────────────────────────────────────
data class EditListingUiState(
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val error: String? = null,
    val saveSuccess: Boolean = false,
    val listingId: String = "",
    val title: String = "",
    val description: String = "",
    val basePrice: String = "",
    val pricingUnit: PricingUnit = PricingUnit.HOUR,
    val address: String = "",
    val city: String = "",
    val state: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val amenities: List<String> = emptyList(),
    val images: List<String> = emptyList(),
    val isAvailable: Boolean = true,
    val propertyType: String = "",
    val bedrooms: Int = 0,
    val bathrooms: Int = 0,
    val maxGuests: Int = 1,
) {
    val hasChanges: Boolean get() = title.isNotBlank()
}

// ── ViewModel ───────────────────────────────────────────────────────────────
@HiltViewModel
class EditListingViewModel @Inject constructor(
    private val hostRepository: HostRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(EditListingUiState())
    val uiState: StateFlow<EditListingUiState> = _uiState.asStateFlow()

    fun loadListing(listingId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null, listingId = listingId)
            hostRepository.getHostListings()
                .onSuccess { listings ->
                    val listing = listings.firstOrNull { it.id == listingId }
                    if (listing != null) {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            title = listing.title,
                            description = listing.description,
                            basePrice = if (listing.pricing.basePrice > 0)
                                listing.pricing.basePrice.toBigDecimal().stripTrailingZeros().toPlainString()
                            else "",
                            pricingUnit = PricingUnit.fromValue(listing.pricing.unit),
                            address = listing.location.address,
                            city = listing.location.city,
                            state = listing.location.state,
                            latitude = listing.location.latitude,
                            longitude = listing.location.longitude,
                            amenities = listing.amenities,
                            images = listing.images,
                            isAvailable = listing.isAvailable,
                            propertyType = listing.propertyType,
                            bedrooms = listing.bedrooms,
                            bathrooms = listing.bathrooms,
                            maxGuests = listing.maxGuests,
                        )
                    } else {
                        _uiState.value = _uiState.value.copy(isLoading = false, error = "Listing not found.")
                    }
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false, error = e.message ?: "Failed to load listing."
                    )
                }
        }
    }

    fun updateTitle(value: String) { _uiState.value = _uiState.value.copy(title = value) }
    fun updateDescription(value: String) { _uiState.value = _uiState.value.copy(description = value) }
    fun updatePricingUnit(unit: PricingUnit) { _uiState.value = _uiState.value.copy(pricingUnit = unit) }
    fun updateAddress(value: String) { _uiState.value = _uiState.value.copy(address = value) }
    fun updateCity(value: String) { _uiState.value = _uiState.value.copy(city = value) }
    fun updateState(value: String) { _uiState.value = _uiState.value.copy(state = value) }
    fun updateAvailability(isAvailable: Boolean) { _uiState.value = _uiState.value.copy(isAvailable = isAvailable) }
    fun clearError() { _uiState.value = _uiState.value.copy(error = null) }
    fun clearSaveSuccess() { _uiState.value = _uiState.value.copy(saveSuccess = false) }

    fun updateBasePrice(value: String) {
        val cleaned = value.replace(Regex("[^0-9.]"), "")
        val parts = cleaned.split(".")
        val sanitized = if (parts.size > 1) "${parts[0]}.${parts[1].take(2)}" else cleaned
        _uiState.value = _uiState.value.copy(basePrice = sanitized)
    }

    fun toggleAmenity(amenity: String) {
        val current = _uiState.value.amenities.toMutableList()
        if (current.contains(amenity)) current.remove(amenity) else current.add(amenity)
        _uiState.value = _uiState.value.copy(amenities = current)
    }

    fun removeImage(index: Int) {
        val current = _uiState.value.images.toMutableList()
        if (index in current.indices) {
            current.removeAt(index)
            _uiState.value = _uiState.value.copy(images = current)
        }
    }

    fun addImageUrls(urls: List<String>) {
        val current = _uiState.value.images.toMutableList()
        current.addAll(urls)
        _uiState.value = _uiState.value.copy(images = current)
    }

    fun saveListing() {
        val s = _uiState.value
        if (s.title.isBlank()) { _uiState.value = s.copy(error = "Title is required."); return }
        val price = s.basePrice.toDoubleOrNull() ?: 0.0
        if (price <= 0) { _uiState.value = s.copy(error = "Price must be greater than 0."); return }

        viewModelScope.launch {
            _uiState.value = s.copy(isSaving = true, error = null)
            val updated = Property(
                id = s.listingId, title = s.title, description = s.description,
                location = PropertyLocation(
                    address = s.address, city = s.city, state = s.state,
                    latitude = s.latitude, longitude = s.longitude,
                ),
                pricing = PropertyPricing(
                    basePrice = price, unit = s.pricingUnit.value,
                    pricingType = s.pricingUnit.backendPricingType, currency = "USD",
                ),
                amenities = s.amenities, images = s.images, isAvailable = s.isAvailable,
                propertyType = s.propertyType, bedrooms = s.bedrooms,
                bathrooms = s.bathrooms, maxGuests = s.maxGuests,
            )
            hostRepository.updateListing(s.listingId, updated)
                .onSuccess { _uiState.value = _uiState.value.copy(isSaving = false, saveSuccess = true) }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(
                        isSaving = false, error = e.message ?: "Failed to save changes."
                    )
                }
        }
    }
}

// ── Screen Composable ───────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditListingScreen(
    listingId: String,
    viewModel: EditListingViewModel,
    onBackClick: () -> Unit = {},
    onSaveSuccess: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(listingId) { viewModel.loadListing(listingId) }
    LaunchedEffect(uiState.saveSuccess) {
        if (uiState.saveSuccess) {
            snackbarHostState.showSnackbar("Listing updated successfully.")
            viewModel.clearSaveSuccess()
            onSaveSuccess()
        }
    }
    LaunchedEffect(uiState.error) {
        uiState.error?.let { snackbarHostState.showSnackbar(it); viewModel.clearError() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Listing", style = PaceDreamTypography.Headline) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(PaceDreamIcons.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (!uiState.isLoading) {
                        TextButton(
                            onClick = { viewModel.saveListing() },
                            enabled = !uiState.isSaving && uiState.hasChanges,
                        ) {
                            if (uiState.isSaving) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    color = PaceDreamColors.HostAccent, strokeWidth = 2.dp,
                                )
                            } else {
                                Text("Save", style = PaceDreamTypography.Button,
                                    color = if (uiState.hasChanges) PaceDreamColors.HostAccent
                                    else PaceDreamColors.TextTertiary)
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PaceDreamColors.Background),
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = PaceDreamColors.Background,
    ) { padding ->
        when {
            uiState.isLoading -> {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = PaceDreamColors.HostAccent)
                        Spacer(Modifier.height(PaceDreamSpacing.MD))
                        Text("Loading listing...", style = PaceDreamTypography.Body,
                            color = PaceDreamColors.TextSecondary)
                    }
                }
            }
            uiState.error != null && uiState.title.isBlank() -> {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(PaceDreamIcons.ErrorOutline, contentDescription = null,
                            tint = PaceDreamColors.Error, modifier = Modifier.size(PaceDreamIconSize.XXL))
                        Spacer(Modifier.height(PaceDreamSpacing.MD))
                        Text(uiState.error ?: "Something went wrong.",
                            style = PaceDreamTypography.Body, color = PaceDreamColors.TextSecondary)
                        Spacer(Modifier.height(PaceDreamSpacing.LG))
                        Button(
                            onClick = { viewModel.loadListing(listingId) },
                            colors = ButtonDefaults.buttonColors(containerColor = PaceDreamColors.HostAccent),
                            shape = RoundedCornerShape(PaceDreamRadius.LG),
                        ) { Text("Retry", style = PaceDreamTypography.Button) }
                    }
                }
            }
            else -> EditListingForm(
                uiState = uiState,
                onTitleChange = viewModel::updateTitle,
                onDescriptionChange = viewModel::updateDescription,
                onBasePriceChange = viewModel::updateBasePrice,
                onPricingUnitChange = viewModel::updatePricingUnit,
                onAddressChange = viewModel::updateAddress,
                onCityChange = viewModel::updateCity,
                onStateChange = viewModel::updateState,
                onToggleAmenity = viewModel::toggleAmenity,
                onAvailabilityChange = viewModel::updateAvailability,
                onRemoveImage = viewModel::removeImage,
                onAddImageUrls = viewModel::addImageUrls,
                onSave = viewModel::saveListing,
                modifier = Modifier.padding(padding),
            )
        }
    }
}

// ── Form ────────────────────────────────────────────────────────────────────

@Composable
private fun EditListingForm(
    uiState: EditListingUiState,
    onTitleChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onBasePriceChange: (String) -> Unit,
    onPricingUnitChange: (PricingUnit) -> Unit,
    onAddressChange: (String) -> Unit,
    onCityChange: (String) -> Unit,
    onStateChange: (String) -> Unit,
    onToggleAmenity: (String) -> Unit,
    onAvailabilityChange: (Boolean) -> Unit,
    onRemoveImage: (Int) -> Unit,
    onAddImageUrls: (List<String>) -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Image picker for adding new photos
    val newImageUris = remember { mutableStateListOf<Uri>() }
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (uris.isNotEmpty()) {
            newImageUris.addAll(uris)
        }
    }
    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .padding(horizontal = PaceDreamSpacing.LG)
            .padding(top = PaceDreamSpacing.SM, bottom = PaceDreamSpacing.XL),
    ) {
        // Basics
        CollapsibleSection("Basics", PaceDreamIcons.Edit, defaultExpanded = true) {
            OutlinedTextField(
                value = uiState.title, onValueChange = onTitleChange,
                label = { Text("Title", style = PaceDreamTypography.Callout) },
                modifier = Modifier.fillMaxWidth(), singleLine = true,
                shape = RoundedCornerShape(PaceDreamRadius.MD), colors = fieldColors(),
            )
            Spacer(Modifier.height(PaceDreamSpacing.MD))
            OutlinedTextField(
                value = uiState.description, onValueChange = onDescriptionChange,
                label = { Text("Description", style = PaceDreamTypography.Callout) },
                modifier = Modifier.fillMaxWidth(), minLines = 3, maxLines = 8,
                shape = RoundedCornerShape(PaceDreamRadius.MD), colors = fieldColors(),
            )
        }
        Spacer(Modifier.height(PaceDreamSpacing.MD))

        // Pricing
        CollapsibleSection("Pricing", PaceDreamIcons.AttachMoney) {
            EditPricingUnitSelector(uiState.pricingUnit, onPricingUnitChange)
            Spacer(Modifier.height(PaceDreamSpacing.MD))
            OutlinedTextField(
                value = uiState.basePrice, onValueChange = onBasePriceChange,
                label = { Text("Price", style = PaceDreamTypography.Callout) },
                placeholder = { Text("0.00", style = PaceDreamTypography.Body, color = PaceDreamColors.TextSecondary) },
                modifier = Modifier.fillMaxWidth(), singleLine = true,
                leadingIcon = { Text("$", style = PaceDreamTypography.Title3,
                    color = PaceDreamColors.TextSecondary, fontWeight = FontWeight.SemiBold) },
                trailingIcon = { Text("/${uiState.pricingUnit.shortLabel}",
                    style = PaceDreamTypography.Callout, color = PaceDreamColors.TextTertiary) },
                shape = RoundedCornerShape(PaceDreamRadius.MD), colors = fieldColors(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            )
        }
        Spacer(Modifier.height(PaceDreamSpacing.MD))

        // Location
        CollapsibleSection("Location", PaceDreamIcons.LocationOn) {
            OutlinedTextField(
                value = uiState.address, onValueChange = onAddressChange,
                label = { Text("Address", style = PaceDreamTypography.Callout) },
                modifier = Modifier.fillMaxWidth(), singleLine = true,
                shape = RoundedCornerShape(PaceDreamRadius.MD), colors = fieldColors(),
            )
            Spacer(Modifier.height(PaceDreamSpacing.MD))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(PaceDreamSpacing.SM)) {
                OutlinedTextField(
                    value = uiState.city, onValueChange = onCityChange,
                    label = { Text("City", style = PaceDreamTypography.Callout) },
                    modifier = Modifier.weight(1f), singleLine = true,
                    shape = RoundedCornerShape(PaceDreamRadius.MD), colors = fieldColors(),
                )
                OutlinedTextField(
                    value = uiState.state, onValueChange = onStateChange,
                    label = { Text("State", style = PaceDreamTypography.Callout) },
                    modifier = Modifier.weight(1f), singleLine = true,
                    shape = RoundedCornerShape(PaceDreamRadius.MD), colors = fieldColors(),
                )
            }
        }
        Spacer(Modifier.height(PaceDreamSpacing.MD))

        // Images – iOS parity: show actual thumbnails with remove, working add button
        CollapsibleSection(
            "Images" + if (uiState.images.isNotEmpty() || newImageUris.isNotEmpty())
                " (${uiState.images.size + newImageUris.size})" else "",
            PaceDreamIcons.CameraAlt,
        ) {
            if (uiState.images.isEmpty() && newImageUris.isEmpty()) {
                Text("No images yet. Tap + to add photos.",
                    style = PaceDreamTypography.Callout, color = PaceDreamColors.TextSecondary)
            }
            Spacer(Modifier.height(PaceDreamSpacing.SM))
            Row(
                horizontalArrangement = Arrangement.spacedBy(PaceDreamSpacing.SM),
                modifier = Modifier.horizontalScroll(rememberScrollState()),
            ) {
                // Show existing uploaded image thumbnails
                uiState.images.forEachIndexed { index, url ->
                    Box(modifier = Modifier.size(92.dp)) {
                        AsyncImage(
                            model = url,
                            contentDescription = "Photo ${index + 1}",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(PaceDreamRadius.MD)),
                        )
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .size(22.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.6f))
                                .clickable { onRemoveImage(index) },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                PaceDreamIcons.Close, contentDescription = "Remove",
                                tint = Color.White, modifier = Modifier.size(12.dp),
                            )
                        }
                    }
                }
                // Show newly selected (not yet uploaded) image URIs
                newImageUris.forEachIndexed { index, uri ->
                    Box(modifier = Modifier.size(92.dp)) {
                        AsyncImage(
                            model = uri,
                            contentDescription = "New photo ${index + 1}",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(PaceDreamRadius.MD)),
                        )
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .size(22.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.6f))
                                .clickable { newImageUris.removeAt(index) },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                PaceDreamIcons.Close, contentDescription = "Remove",
                                tint = Color.White, modifier = Modifier.size(12.dp),
                            )
                        }
                    }
                }
                // Add photo button
                if (uiState.images.size + newImageUris.size < 10) {
                    EditPhotoPlaceholder(onClick = { imagePickerLauncher.launch("image/*") })
                }
            }
        }
        Spacer(Modifier.height(PaceDreamSpacing.MD))

        // Amenities – use subcategory-specific amenities matching iOS parity
        CollapsibleSection("Amenities", PaceDreamIcons.CheckCircle) {
            EditAmenitiesChips(uiState.amenities, onToggleAmenity, uiState.propertyType)
        }
        Spacer(Modifier.height(PaceDreamSpacing.MD))

        // Availability
        CollapsibleSection("Availability", PaceDreamIcons.Schedule) {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Available for booking", style = PaceDreamTypography.Body,
                        color = PaceDreamColors.TextPrimary)
                    Text(
                        if (uiState.isAvailable) "Your listing is visible to guests"
                        else "Your listing is hidden from search",
                        style = PaceDreamTypography.Caption, color = PaceDreamColors.TextSecondary,
                    )
                }
                Switch(
                    checked = uiState.isAvailable, onCheckedChange = onAvailabilityChange,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White, checkedTrackColor = PaceDreamColors.Success,
                        uncheckedThumbColor = Color.White, uncheckedTrackColor = PaceDreamColors.Gray300,
                    ),
                )
            }
        }
        Spacer(Modifier.height(PaceDreamSpacing.XXL))

        // Save button
        Button(
            onClick = onSave,
            modifier = Modifier.fillMaxWidth().height(PaceDreamButtonHeight.LG),
            enabled = !uiState.isSaving && uiState.hasChanges,
            colors = ButtonDefaults.buttonColors(containerColor = PaceDreamColors.HostAccent),
            shape = RoundedCornerShape(PaceDreamRadius.LG),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
        ) {
            if (uiState.isSaving) {
                CircularProgressIndicator(Modifier.size(20.dp), Color.White, strokeWidth = 2.dp)
                Spacer(Modifier.width(PaceDreamSpacing.SM))
            }
            Text(if (uiState.isSaving) "Saving..." else "Save Changes", style = PaceDreamTypography.Button)
        }
        Spacer(Modifier.height(PaceDreamSpacing.LG))
    }
}

// ── Collapsible Section ─────────────────────────────────────────────────────

@Composable
private fun CollapsibleSection(
    title: String, icon: ImageVector, defaultExpanded: Boolean = false,
    content: @Composable () -> Unit,
) {
    var expanded by remember { mutableStateOf(defaultExpanded) }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(PaceDreamRadius.LG),
        colors = CardDefaults.cardColors(containerColor = PaceDreamColors.Card),
        elevation = CardDefaults.cardElevation(defaultElevation = PaceDreamElevation.XS),
    ) {
        Column(Modifier.fillMaxWidth()) {
            Row(
                Modifier.fillMaxWidth().clickable { expanded = !expanded }.padding(PaceDreamSpacing.MD),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier.size(36.dp).clip(CircleShape)
                        .background(PaceDreamColors.HostAccent.copy(alpha = 0.08f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(icon, contentDescription = null, tint = PaceDreamColors.HostAccent,
                        modifier = Modifier.size(PaceDreamIconSize.SM))
                }
                Spacer(Modifier.width(PaceDreamSpacing.SM))
                Text(title, style = PaceDreamTypography.Headline,
                    color = PaceDreamColors.TextPrimary, modifier = Modifier.weight(1f))
                Icon(PaceDreamIcons.ExpandMore,
                    contentDescription = if (expanded) "Collapse" else "Expand",
                    tint = PaceDreamColors.TextSecondary,
                    modifier = Modifier.size(PaceDreamIconSize.MD).rotate(if (expanded) 180f else 0f))
            }
            AnimatedVisibility(expanded, enter = expandVertically(), exit = shrinkVertically()) {
                Column(Modifier.fillMaxWidth().padding(
                    start = PaceDreamSpacing.MD, end = PaceDreamSpacing.MD, bottom = PaceDreamSpacing.MD,
                )) { content() }
            }
        }
    }
}

// ── Pricing Unit Selector ───────────────────────────────────────────────────

@Composable
private fun EditPricingUnitSelector(selectedUnit: PricingUnit, onUnitSelected: (PricingUnit) -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(PaceDreamRadius.MD))
            .background(PaceDreamColors.HostAccent.copy(alpha = 0.06f)).padding(PaceDreamSpacing.XS),
        horizontalArrangement = Arrangement.spacedBy(PaceDreamSpacing.XS),
    ) {
        PricingUnit.entries.forEach { unit ->
            val sel = selectedUnit == unit
            Box(
                Modifier.weight(1f).clip(RoundedCornerShape(PaceDreamRadius.SM))
                    .background(if (sel) Color.White else Color.Transparent)
                    .clickable { onUnitSelected(unit) }.padding(vertical = PaceDreamSpacing.SM),
                contentAlignment = Alignment.Center,
            ) {
                Text(unit.displayLabel, style = PaceDreamTypography.Callout,
                    fontWeight = if (sel) FontWeight.SemiBold else FontWeight.Medium,
                    color = if (sel) PaceDreamColors.HostAccent else PaceDreamColors.TextSecondary)
            }
        }
    }
}

// ── Amenities Chips ─────────────────────────────────────────────────────────

private val COMMON_AMENITIES = listOf(
    "WiFi", "AC", "Parking", "Accessible", "Clean", "Towels", "Showers",
    "Lockers", "Security", "24/7 Access", "EV Charging", "Kitchen", "TV", "Quiet", "Charger",
)

/**
 * Subcategory-specific amenities matching iOS parity (Amenity.swift).
 * Falls back to COMMON_AMENITIES if no matching subcategory found.
 */
private fun getSubcategoryAmenities(propertyType: String): List<String> {
    return when (propertyType.lowercase()) {
        "restroom" -> listOf("Toilet Paper", "Hand Soap", "Hand Towels", "Air Freshener", "Paper Towels")
        "nap_pod" -> listOf("Noise Cancellation", "Soundproof Walls", "White Noise Machine", "Earplugs",
            "Calming Music", "Reclining Chair", "Blanket", "Pillow", "Eye Mask", "Temperature Control")
        "meeting_room" -> listOf("WiFi", "Power Outlets", "Projector", "Whiteboard", "Monitor/TV", "Desk Space", "Chairs")
        "gym" -> listOf("Exercise Equipment", "Locker Room", "Showers", "Water Fountain", "Towel Service", "Air Conditioning")
        "parking" -> listOf("Covered Parking", "Security Camera", "EV Charging", "Gated Access", "Well Lit", "24/7 Access")
        "storage_space" -> listOf("Climate Controlled", "Security Camera", "Gated Access", "24/7 Access", "Ground Floor", "Drive-Up Access")
        "wifi" -> listOf("High Speed", "Unlimited Data", "Router Included", "5G Support", "Password Protected")
        "camera" -> listOf("Lens Included", "Extra Batteries", "Memory Card", "Camera Bag", "Tripod", "Filters", "Remote Control")
        "tech" -> listOf("Charger Included", "Case/Cover", "Screen Protector", "Headphones", "Warranty", "Extra Cable")
        "sports_gear" -> listOf("Adjustable Straps", "Carrying Case", "Extra Batteries", "Charger", "Quick Release")
        "instrument" -> listOf("Case/Bag", "Strap Included", "Tuner", "Extra Strings", "Metronome", "Stand")
        "tools" -> listOf("Carrying Case", "Safety Gear", "Extra Blades", "Charger", "Manual Included", "Extension Cord")
        "micromobility" -> listOf("Helmet Included", "Lock Included", "Charger", "Lights", "Bell", "Basket")
        else -> COMMON_AMENITIES
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EditAmenitiesChips(selected: List<String>, onToggle: (String) -> Unit, propertyType: String = "") {
    val subcategoryAmenities = if (propertyType.isNotBlank()) getSubcategoryAmenities(propertyType) else COMMON_AMENITIES
    val all = (subcategoryAmenities + selected).distinct()
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(PaceDreamSpacing.SM),
        verticalArrangement = Arrangement.spacedBy(PaceDreamSpacing.SM),
    ) {
        all.forEach { name ->
            val isOn = selected.contains(name)
            FilterChip(
                selected = isOn, onClick = { onToggle(name) },
                label = { Text(name, style = PaceDreamTypography.Callout) },
                leadingIcon = if (isOn) {
                    { Icon(PaceDreamIcons.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                } else null,
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = PaceDreamColors.HostAccent,
                    selectedLabelColor = Color.White, selectedLeadingIconColor = Color.White,
                    containerColor = PaceDreamColors.Card, labelColor = PaceDreamColors.TextPrimary,
                ),
                shape = RoundedCornerShape(PaceDreamRadius.Round),
                border = FilterChipDefaults.filterChipBorder(
                    borderColor = PaceDreamColors.Border, selectedBorderColor = PaceDreamColors.HostAccent,
                    enabled = true, selected = isOn,
                ),
            )
        }
    }
}

// ── Photo Placeholder ───────────────────────────────────────────────────────

@Composable
private fun EditPhotoPlaceholder(onClick: () -> Unit) {
    Card(
        modifier = Modifier.size(100.dp).clickable(onClick = onClick),
        shape = RoundedCornerShape(PaceDreamRadius.MD),
        colors = CardDefaults.cardColors(containerColor = PaceDreamColors.Gray100),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Icon(PaceDreamIcons.Add, contentDescription = "Add photo",
                tint = PaceDreamColors.TextSecondary, modifier = Modifier.size(28.dp))
        }
    }
}

// ── Shared TextField Colors ─────────────────────────────────────────────────

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = PaceDreamColors.HostAccent, unfocusedBorderColor = PaceDreamColors.Border,
)
